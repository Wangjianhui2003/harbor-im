import {defineStore} from "pinia";
import {computed, reactive, ref} from "vue";
import {findGroups} from "../api/group.js";

export const useGroupStore = defineStore("groupStore",() => {
    const groups = ref([])

    //设置群聊
    const setGroups = (groupVOs) => {
        groups.value = groupVOs
    }

    //添加群聊
    const addGroup = (group) => {
        groups.value.unshift(group)
    }

    //逻辑移除
    const removeGroup = (id) => {
        groups.value
            .filter(group => group.id == id)
            .forEach(group => {group.quit = true})
    }

    //更新group
    const updateGroup = (group) => {
        groups.value.forEach((g,idx) => {
            if (g.id == group.id) {
                Object.assign(groups.value[idx],group)
            }
        })
    }

    //加载groups
    const loadGroups = async () => {
        findGroups().then(groups => {
            setGroups(groups)
        }).catch((err) => {
            console.error("加载群组失败", err);
            throw err;
        })
    }

    //根据id查找group
    const findGroup = computed(() => (id) => groups.value.find(g => g.id == id))

    return  {groups,addGroup, removeGroup, updateGroup, loadGroups,findGroup};
})