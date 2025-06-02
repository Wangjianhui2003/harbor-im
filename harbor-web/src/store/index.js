import {defineStore} from "pinia";
import useUserStore from "./userStore.js";
import useFriendStore from "./friendStore.js";
import useChatStore from "./chatStore.js";
import chatStore from "./chatStore.js";
import {useGroupStore} from "./groupStore.js";

// Main store to load all data
const useMainStore = defineStore('mainStore', {
    actions:{
        async loadAll(){
            const userStore = useUserStore()
            const friendStore = useFriendStore()
            const chatStore = useChatStore()
            const groupStore = useGroupStore()

            return userStore.loadUser().then(() => {
                const promises = [];
                promises.push(friendStore.loadFriend())
                promises.push(chatStore.loadChats())
                promises.push(groupStore.loadGroups())
                return Promise.all(promises)
            })
        },
        clearAll(){

        }
    }
})

export default useMainStore