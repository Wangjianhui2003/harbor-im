import {defineStore} from "pinia";
import useUserStore from "./userStore.js";
import useFriendStore from "./friendStore.js";

// Main store to load all data
const useMainStore = defineStore('mainStore', {
    actions:{
        async loadAll(){
            const userStore = useUserStore()
            const friendStore = useFriendStore()

            await userStore.loadUser()
            await Promise.all([
                friendStore.loadFriend()
            ])
        },
        clearAll(){
            useUserStore().clear()
            useFriendStore().clear()
        }
    }
})

export default useMainStore