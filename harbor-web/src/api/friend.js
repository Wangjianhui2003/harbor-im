
import http from "./http.js";

//添加好友
const addFriend = async (friendId) => {
    return http({
        url: '/friend/add',
        method: 'post',
        params: { friendId }
    })
}

const getFriendList = async () => {
    return http({
        url: '/friend/list',
        method: 'get'
    })
}

export { addFriend, getFriendList }