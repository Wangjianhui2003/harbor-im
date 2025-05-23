import {defineStore} from "pinia";
import useUserStore from "./userStore.js";
import localForage from "localforage";

//chats缓存
let cacheChats = [];
const userStore = useUserStore()

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
 * key:chats-userId(当前用户id)
 * chatKey: key-chat.type-targetId  例如:chats-23-PrivateMsg-12
 *
 // chatsData = {
 // privateMsgMaxId: state.privateMsgMaxId,
 // groupMsgMaxId: state.groupMsgMaxId,
 // chatKeys: chatKeys
 // }
 *
 *  key -> chatsData (chatKeys)
 *  chatKey -> chat
 *
 */

const useChatStore = defineStore("friendStore",{
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
        loadChats(){
            return new Promise((resolve, reject) => {
                const userStore = useUserStore();
                let userId = userStore.userInfo.id
                let key = 'chats-' + userId
                localForage.getItem(key).then((chatsData) => {
                    if(!chatsData){
                        //没有chat数据，不加载
                        resolve()
                    }else if (chatsData.chats){
                        this.initChat(chatsData)
                        resolve()
                    }else if(chatsData.chatKeys){
                        const promises = []
                        //用key到localForage取
                        chatsData.chatKeys.forEach(key => {
                            promises.push(localForage.getItem(key))
                        })
                        //过滤，init
                        Promise.all(promises).then(chats => {
                            chatsData.chats = chats.filter(o => o)
                            this.initChat(chatsData)
                            resolve()
                        })
                    }
                }).catch(err=>{
                    console.log('消息加载失败',err)
                    reject()
                })
            })
        },
        initChat(chatsData){
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
        openChat(chatInfo){
            let chats = this.findChats;
            let chat = null;
            //从store里取chat更新(移动到头部)
            for(let idx in chats){
                //目标id和聊天类型相同
                if(chats[idx].type == chatInfo.type && chats[idx].id == chatInfo.targetId) {
                    chat = chats[idx]
                    this.moveTop(idx)
                    break
                }
            }
            //没有，新建一个会话
            if(chat == null){
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
        moveTop(idx){
            // 加载中不移动，很耗性能
            if(this.isLoading){
                return;
            }
            if(idx > 0){
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
        activateChat(){
            let chat = this.findChats
            this.activeChat = chat
        },
        //将内存里数据(而且stored状态为false)保存到数据库
        saveToStorage(){
            // 加载中不保存，防止卡顿
            if(this.isLoading) return

            let userId = userStore.userInfo.id
            let key = 'chats-' + userId
            let chatKeys = []

            this.chats.forEach(chat => {
                let chatKey = `${key}-${chat.type}-${chat.targetId}`
                if (!chat.stored) {
                    if (chat.delete) {
                        localForage.removeItem(chatKey);
                    } else {
                        localForage.setItem(chatKey, chat);
                    }
                    chat.stored = true;
                }
                if (!chat.delete) {
                    chatKeys.push(chatKey);
                }
                // 会话核心信息
                let chatsData = {
                    privateMsgMaxId: this.privateMsgMaxId,
                    groupMsgMaxId: this.groupMsgMaxId,
                    chatKeys: chatKeys
                }
                localForage.setItem(key, chatsData)
                // 清理已删除的会话
                this.chats = this.chats.filter(chat => !chat.delete)
            })
        },
        clear() {
            cacheChats = []
            this.chats = [];
            this.activeChat = null;
        }
    },
    getters: {
        isLoading(state){
            return state.loadingGroupMsg || state.loadingPrivateMsg
        },
        //cacheChats或已加载完成的chats
        findChats(state){
            if(cacheChats && this.isLoading){
                return cacheChats;
            }
            return state.chats;
        }
    }
})

export default useChatStore;