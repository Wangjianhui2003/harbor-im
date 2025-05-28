
import http from "./http.js";

//添加好友
export const addFriend = async (friendId) => {
    return http({
        url: '/friend/add',
        method: 'post',
        params: { friendId }
    })
}

export const getFriendList = async () => {
    return http({
        url: '/friend/list',
        method: 'get'
    })
}

export const removeFriend = async (friendId) => {
    return http({
        url: `/friend/delete/${friendId}`,
        method: 'DELETE',
    })
}
