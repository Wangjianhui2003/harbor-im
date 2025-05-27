import {defineStore} from "pinia";
import useUserStore from "./userStore.js";
import useFriendStore from "./friendStore.js";
import useChatStore from "./chatStore.js";
import chatStore from "./chatStore.js";

// Main store to load all data
const useMainStore = defineStore('mainStore', {
    actions:{
        async loadAll(){
            const userStore = useUserStore()
            const friendStore = useFriendStore()
            const chatStore = useChatStore()

            return userStore.loadUser().then(() => {
                const promises = [];
                promises.push(friendStore.loadFriend())
                promises.push(chatStore.loadChats())
                return Promise.all(promises)
            })
        },
        clearAll(){
            useUserStore().clear()
            useFriendStore().clear()
            chatStore().clear()
        }
    }
})

export default useMainStore