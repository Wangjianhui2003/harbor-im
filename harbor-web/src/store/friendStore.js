import {defineStore} from "pinia";
import {getFriendList} from "../api/friend.js";
import {getUserOnlineStatus} from "../api/user.js";
import {TERMINAL_TYPE} from "../common/enums.js";

// 好友列表
const useFriendStore = defineStore('friendStore', {
    state: () => ({
        friends: [],
        timer: null
    }),
    actions: {
        setFriends(friends){
            // 设置好友列表 初始化3个参数
            friends.forEach((f) => {
                f.online = false;
                f.onlineWeb = false;
                f.onlineApp = false;
            })
            this.friends = friends;
        },
        updateFriend(friend){
            // 更新好友信息
            this.friends.forEach((f, index) => {
                if (f.id == friend.id) {
                    // 拷贝属性
                    let online = this.friends[index].online;
                    Object.assign(this.friends[index], friend);
                    this.friends[index].online = online;
                }
            })
        },
        removeFriend(id){
            // 删除好友 delete标记
            this.friends.filter(f => f.id == id).forEach(f => f.deleted = true);
        },
        addFriend(friend){
            // 如果已经存在，则更新
            if (this.friends.some((f) => f.id == friend.id)) {
                this.updateFriend(friend)
            } else {
                this.friends.unshift(friend);
            }
        },
        refreshOnlineStatus(){
            //过滤掉已删除的好友
            let userIds = this.friends.filter((f) => !f.deleted).map((f) => f.id);
            if (userIds.length == 0) {
                return;
            }
            // 拉取在线状态
            getUserOnlineStatus(userIds).then((onlineTerminals) => {
                this.setOnlineStatus(onlineTerminals)
            })
            // 30s后重新拉取
            this.timer && clearTimeout(this.timer);
            this.timer = setTimeout(() => {
                this.refreshOnlineStatus()
            },1000 * 30)
        },
        setOnlineStatus(onlineTerminals){
            // 设置在线状态
            this.friends.forEach((f) => {
                let userTerminal = onlineTerminals.find((o) => f.id == o.userId);
                if (userTerminal) {
                    f.online = true;
                    f.onlineWeb = userTerminal.terminals.indexOf(TERMINAL_TYPE.WEB) >= 0
                    f.onlineApp = userTerminal.terminals.indexOf(TERMINAL_TYPE.APP) >= 0
                } else {
                    f.online = false;
                    f.onlineWeb = false;
                    f.onlineApp = false;
                }
            })
            // 在线的在前面
            this.friends.sort((f1, f2) => {
                if (f1.online && !f2.online) {
                    return -1;
                }
                if (f2.online && !f1.online) {
                    return 1;
                }
                return 0;
            });
        },
        clear() {
            // 清空好友列表
            this.timer && clearTimeout(this.timer);
            this.friends = [];
            this.timer = null;
        },
        async loadFriend(){
            // 加载好友列表
            return new Promise((resolve,reject) => {
                getFriendList().then((friends) => {
                    this.setFriends(friends);
                    // 拉取在线状态并设置定时任务
                    this.refreshOnlineStatus();
                    resolve()
                }).catch(()=>{
                    reject()
                })
            })
        }
    },
    getters: {
        //判断和该id是否是好友
        isFriend(state){
            return (userId) => state.friends.filter(f => !f.deleted).some(f => f.id == userId)
        },
        //根据id查找friend信息
        findFriend(state){
            return (userId) => state.friends.find(f => f.id == userId)
        }
    }
})

export default useFriendStore