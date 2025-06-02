
// 创建群聊
import http from "./http.js";

export const createGroup = (data) => {
    return http({
        url: '/group/create',
        method: 'POST',
        data
    })
}

// 修改群聊信息
export const modifyGroup = (data) => {
    return http({
        url: '/group/modify',
        method: 'PUT',
        data
    })
}

// 解散群聊
export const deleteGroup = (groupId) => {
    return http({
        url: `/group/delete/${groupId}`,
        method: 'DELETE'
    })
}

// 查询单个群聊
export const findGroup = (groupId) => {
    return http({
        url: `/group/find/${groupId}`,
        method: 'GET'
    })
}

// 查询群聊列表
export const findGroups = () => {
    return http({
        url: '/group/list',
        method: 'GET'
    })
}

// 邀请进群
export const inviteToGroup = (data) => {
    return http({
        url: '/group/invite',
        method: 'POST',
        data
    })
}

// 查询群聊成员
export const findGroupMembers = (groupId) => {
    return http({
        url: `/group/members/${groupId}`,
        method: 'GET'
    })
}

// 退出群聊
export const quitGroup = (groupId) => {
    return http({
        url: `/group/quit/${groupId}`,
        method: 'DELETE'
    })
}

// 踢出群聊
export const kickGroup = (groupId, userId) => {
    return http({
        url: `/group/kick/${groupId}`,
        method: 'DELETE',
        params: { userId }
    })
}
