import {defineStore} from "pinia";
import useUserStore from "./userStore.js";
import useFriendStore from "./friendStore.js";
import useChatStore from "./chatStore.js";
import chatStore from "./chatStore.js";
import {useGroupStore} from "./groupStore.js";
import {useWebRTCStore} from "./webRTCStore.js";

// Main store to load all data
const useMainStore = defineStore('mainStore', {
    actions:{
        async loadAll(){
            const userStore = useUserStore()
            const friendStore = useFriendStore()
            const chatStore = useChatStore()
            const groupStore = useGroupStore()
            const webRTCStore = useWebRTCStore()

            return userStore.loadUser().then(() => {
                const promises = [];
                promises.push(friendStore.loadFriend())
                promises.push(chatStore.loadChats())
                promises.push(groupStore.loadGroups())
                promises.push(webRTCStore.loadConfig())
                return Promise.all(promises)
            })
        },
        clearAll(){

        }
    }
})

export default useMainStore