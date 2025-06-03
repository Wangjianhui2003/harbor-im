import {defineStore} from "pinia";
import useUserStore from "./userStore.js";
import localForage from "localforage";
import {CHATINFO_TYPE, MESSAGE_STATUS, MESSAGE_TYPE} from "../common/enums.js";

/* 为了加速拉取离线消息效率，拉取时消息暂时存储到cacheChats,等
待所有离线消息拉取完成后，再统一渲染*/
let cacheChats = [];

//chat结构
// chat = {
//     targetId: chatInfo.targetId,
//     type: chatInfo.type,
//     showName: chatInfo.showName,
//     headImage: chatInfo.headImage,
//     lastContent: "",
//     lastSendTime: new Date().getTime(),
//     unreadCount: 0,
//     messages: [],
//     atMe: false,
//     atAll: false,
//     stored: false,
//     delete: false
// }

/**
 * key,chatKey,chatsData结构
 *
 * key:chats-userId(当前用户id) -> chatsData
 * chatKey: key-chat.type-targetId  例如:chats-23-PrivateMsg-12 -> chat
 *
 * chatsData = {
 * privateMsgMaxId: state.privateMsgMaxId,
 * groupMsgMaxId: state.groupMsgMaxId,
 * chatKeys: chatKeys
 * }
 *
 *  key -> chatsData (chatKeys)
 *  chatKey -> chat
 *
 */

const useChatStore = defineStore("chatStore", {
    state: () => ({
        activeChat: null,
        privateMsgMaxId: 0,
        groupMsgMaxId: 0,
        loadingPrivateMsg: false,
        loadingGroupMsg: false,
        chats: []
    }),
    actions: {
        //加载chat数据
        loadChats() {
            return new Promise((resolve, reject) => {
                const userStore = useUserStore();
                let userId = userStore.userInfo.id
                let key = 'chats-' + userId
                localForage.getItem(key).then((chatsData) => {
                    if (!chatsData) {
                        //没有chatData数据，不加载
                        console.log("没有历史chats数据")
                        resolve()
                    } else if (chatsData.chatKeys) {
                        console.log("加载chats")
                        //有chatKeys，读取chat
                        const promises = []
                        //用key到localForage取
                        chatsData.chatKeys.forEach(chatKey => {
                            console.log(chatKey)
                            promises.push(localForage.getItem(chatKey))
                        })
                        //过滤，init
                        Promise.all(promises).then(chats => {
                            chatsData.chats = chats.filter(o => o)
                            this.initChat(chatsData)
                            resolve()
                        })
                    }
                }).catch(err => {
                    console.log('消息加载失败', err)
                    reject()
                })
            })
        },
        //初始化
        initChat(chatsData) {
            this.chats = [];
            this.privateMsgMaxId = chatsData.privateMsgMaxId || 0;
            this.groupMsgMaxId = chatsData.groupMsgMaxId || 0;
            cacheChats = chatsData.chats || [];
            // 防止图片一直处在加载中状态
            cacheChats.forEach((chat) => {
                chat.messages.forEach((msg) => {
                    if (msg.loadStatus == "loading") {
                        msg.loadStatus = "fail"
                    }
                })
            })
        },
        //开始一个chat会话
        openChat(chatInfo) {
            let chats = this.findChats;
            let chat = null;
            //从store里取chat更新(移动到头部)
            for (let idx in chats) {
                //目标id和聊天类型相同
                if (chats[idx].type == chatInfo.type && chats[idx].targetId == chatInfo.targetId) {
                    chat = chats[idx]
                    this.moveTop(idx)
                    break
                }
            }
            //没有，新建一个会话
            if (chat == null) {
                chat = {
                    targetId: chatInfo.targetId,
                    type: chatInfo.type,
                    showName: chatInfo.showName,
                    headImage: chatInfo.headImage,
                    lastContent: "",
                    lastSendTime: new Date().getTime(),
                    unreadCount: 0,
                    messages: [],
                    atMe: false,
                    atAll: false,
                    stored: false,
                    delete: false
                };
                chats.unshift(chat);
            }
        },
        //会话移动到顶部
        moveTop(idx) {
            // 加载中不移动，很耗性能
            if (this.isLoading) {
                return;
            }
            if (idx > 0) {
                let chats = this.findChats;
                let chat = chats[idx];
                chats.splice(idx, 1)
                chats.unshift(chat)
                chat.stored = false
                chat.lastSendTime = new Date().getTime()
                this.saveToStorage()
            }
        },
        //选择一个chat
        activateChat(idx) {
            let chats = this.findChats
            this.activeChat = chats[idx]
        },
        //将内存里数据(而且stored状态为false)保存到数据库
        saveToStorage() {
            // 加载中不保存，防止卡顿
            if (this.isLoading) return
            const userStore = useUserStore()
            let userId = userStore.userInfo.id
            let key = 'chats-' + userId
            let chatKeys = []
            this.chats.forEach(chat => {
                let chatKey = `${key}-${chat.type}-${chat.targetId}`
                if (!chat.stored) {
                    if (chat.delete) {
                        localForage.removeItem(chatKey);
                    } else {
                        localForage.setItem(chatKey, JSON.parse(JSON.stringify(chat)));
                    }
                    chat.stored = true;
                }
                if (!chat.delete) {
                    chatKeys.push(chatKey);
                }
            })
            // 会话核心信息
            let chatsData = {
                privateMsgMaxId: this.privateMsgMaxId,
                groupMsgMaxId: this.groupMsgMaxId,
                chatKeys: chatKeys
            }
            localForage.setItem(key, chatsData)
            // 清理已删除的会话
            this.chats = this.chats.filter(chat => !chat.delete)
        },

        insertMessage(msgInfo, chatInfo) {
            let type = chatInfo.type
            //记录消息的最大id
            if (msgInfo.id && type === "PRIVATE" && msgInfo.id > this.privateMsgMaxId) {
                this.privateMsgMaxId = msgInfo.id;
            }
            if (msgInfo.id && type === "GROUP" && msgInfo.id > this.groupMsgMaxId) {
                this.groupMsgMaxId = msgInfo.id;
            }
            // 如果是已存在消息，则覆盖旧的消息数据
            let chat = this.findChat(chatInfo);
            let message = this.findMessage(chat, chatInfo)
            if (message) {
                Object.assign(message, msgInfo);
                chat.stored = false;
                this.saveToStorage()
                return
            }
            if (msgInfo.type == MESSAGE_TYPE.TEXT) {
                chat.lastContent = msgInfo.content;
            }
            chat.lastSendTime = msgInfo.sendTime;
            chat.sendNickname = msgInfo.sendNickname;
            // TODO:完善，insertMessage
            // 根据id顺序插入，防止消息乱序
            let insertPos = chat.messages.length;
            if (msgInfo.id && msgInfo.id > 0) {
                for (let idx in chat.messages) {
                    if (chat.messages[idx].id && msgInfo.id < chat.messages[idx].id) {
                        insertPos = idx;
                        console.log(`消息出现乱序,位置:${chat.messages.length},修正至:${insertPos}`);
                        break;
                    }
                }
            }
            chat.messages.splice(insertPos, 0, msgInfo)
            chat.stored = false;
            this.saveToStorage()
        },
        //将cacheChats刷新到chats
        refreshChat(){
            //没有缓存，不刷新
            if(!cacheChats){
                return
            }
            //按最新时间排序（在前）
            cacheChats.sort((chat1, chat2) => {
                return chat2.lastSendTime - chat1.lastSendTime;
            })
            // 将消息一次性装载回来
            this.chats = cacheChats;
            // 清空缓存
            cacheChats = null;
            this.saveToStorage()
        },
        //isLoading=true,现在在拉取离线消息，isLoading=false，现在没有在拉取离线消息，可以刷新到chats
        setLoadingPrivateMsgState(loading){
            this.loadingPrivateMsg = loading
            if(!this.isLoading){
                this.refreshChat()
            }
        },
        setLoadingGroupMsgState(loading){
            this.loadingPrivateMsg = loading
            if(!this.isLoading){
                this.refreshChat()
            }
        },
        //移除chat
        removeChat(idx) {
            let chats = this.findChats;
            if (chats[idx] === this.activeChat) {
                this.activeChat = null
            }
            chats[idx].delete = true;
            chats[idx].stored = false;
            this.saveToStorage()
        },
        //移除私聊消息
        removePrivateChat(friendId){
            let chats = this.findChats;
            for (let idx in chats) {
                if(chats[idx].type == CHATINFO_TYPE.PRIVATE && chats[idx].targetId == friendId){
                    this.removeChat(idx)
                    break;
                }
            }
        },
        //移除群组消息
        removeGroupChat(groupId){
            let chats = this.findChats;
            for (let idx in chats) {
                if(chats[idx].type == CHATINFO_TYPE.GROUP && chats[idx].targetId == groupId){
                    this.removeChat(idx)
                    break;
                }
            }
        },
        updateChatFromFriend(friend){
            let chat = this.findChatByFriendId(friend.id);
            // 更新会话中的群名和头像
            if (chat && (chat.headImage != friend.headImage ||
                chat.showName != friend.friendNickname)) {
                chat.headImage = friend.headImage;
                chat.showName = friend.friendNickname;
                chat.stored = false;
                this.saveToStorage()
            }
        },
        clear() {
            cacheChats = []
            this.chats = [];
            this.activeChat = null;
        },
        //清除未读状态(未读消息数，at等)
        resetUnread(chatInfo){
            let chats = this.findChats
            for (let idx in chats) {
                if(chats[idx].type == chatInfo.type && chats[idx].targetId == chatInfo.targetId){
                    chats[idx].unreadCount = 0;
                    chats[idx].atMe = false;
                    chats[idx].atAll = false;
                    chats[idx].stored = false;
                    this.saveToStorage()
                    break;
                }
            }
        },
        recallMsg(msgInfo,chatInfo){
            let chat = chatStore.findChat(chatInfo);
            if (!chat) return;
            //要撤回的消息id
            let id = msgInfo.content;
            //群聊和私聊撤回标识
            let name = msgInfo.selfSend ? '你' : chat.type == 'PRIVATE' ? '对方' : msgInfo.sendNickname;
            for (let idx in chat.messages){
                let m = chat.messages[idx]
                if(m.id && m.id == id){
                    // 改造成一条提示消息
                    m.status = MESSAGE_STATUS.RECALL;
                    m.content = name + "撤回了一条消息";
                    m.type = MESSAGE_TYPE.TIP_TEXT
                    // 会话列表变化
                    chat.lastContent = m.content;
                    chat.lastSendTime = msgInfo.sendTime;
                    chat.sendNickname = '';
                    if (!msgInfo.selfSend && msgInfo.status != MESSAGE_STATUS.READED) {
                        chat.unreadCount++;
                    }
                }
                // 被引用的消息也要撤回
                if (m.quoteMessage && m.quoteMessage.id == msgInfo.id) {
                    m.quoteMessage.content = "引用内容已撤回";
                    m.quoteMessage.status = MESSAGE_STATUS.RECALL;
                    m.quoteMessage.type = MESSAGE_TYPE.TIP_TEXT
                }
            }
            chat.stored = false;
            this.saveToStorage()
        },
        //加载群信息后，如果头像和showGroupName变了要重新加载
        updateChatFromGroup(group){
            let chat = this.findChatByGroupId(group.id)
            if (chat && (chat.headImage != group.headImageThumb || chat.showName != group.showGroupName)) {
                // 更新会话中的群名称和头像
                chat.headImage = group.headImageThumb;
                chat.showName = group.showGroupName;
                chat.stored = false;
                this.saveToStorage()
            }
        }
    },
    getters: {
        isLoading(state) {
            return state.loadingGroupMsg || state.loadingPrivateMsg
        },
        //cacheChats或已加载完成的chats
        findChats(state) {
            if (cacheChats && this.isLoading) {
                return cacheChats;
            }
            return state.chats;
        },
        //找到对应信息
        findMessage(state) {
            return (chat, msgInfo) => {
                if (!chat) return null;
                for (let msg in chat.msgInfo) {
                    // 通过id判断
                    if (msgInfo.id && chat.messages[idx].id == msgInfo.id) {
                        return chat.messages[idx];
                    }
                    // 正在发送中的消息可能没有id,只有tmpId
                    if (msgInfo.tmpId && chat.messages[idx].tmpId &&
                        chat.messages[idx].tmpId == msgInfo.tmpId) {
                        return chat.messages[idx];
                    }
                }
            }
        },
        findChat(state) {
            return (chat) => {
                let chats = this.findChats
                let idx = this.findChatIdx(chat)
                return chats[idx]
            }
        },
        //找到chat在chats下标
        findChatIdx(state) {
            return (chat) => {
                let chats = this.findChats
                for (let idx in chats) {
                    if (chats[idx].type == chat.type &&
                        chats[idx].targetId === chat.targetId) {
                        chat = chats[idx];
                        return idx;
                    }
                }
            }
        },
        //根据friendId找chat
        findChatByFriendId(state) {
            return (friendId) => {
                let chats = this.findChats
                return chats.find(chat => chat.type == 'PRIVATE' && chat.targetId == friendId)
            }
        },
        //根据groupId找chat
        findChatByGroupId(state) {
            return (groupId) => {
                let chats = this.findChats
                return chats.find(chat => chat.type == 'GROUP' && chat.targetId == groupId)
            }
        }
    }
})

export default useChatStore;