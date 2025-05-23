import http from "./http.js";

const login = (loginData) => {
    return http({
        url: '/login',
        method: 'post',
        data: loginData
    })
}

const register = (registerData) => {
    return http({
        url: '/register',
        method: 'post',
        data: registerData
    })
}

const getCaptcha = () => {
    return http({
        url: '/captcha',
        method: 'get'
    })
}

//通过用户名或昵称查找用户
const findUserByName = ( name ) => {
    return http({
        url: '/user/findByName',
        method: 'get',
        params: { name }
    })
}

const getSelfInfo = () => {
    return http({
        url: '/user/self',
        method: 'get'
    })
}

const getUserOnlineStatus = (userIds) => {
    return http({
        url: '/user/terminal/online',
        method: 'get',
        params: { userIds: userIds.join(',') }
    })
}

const getUserInfo = (id) => {
    return http({
        url: `/user/find/${id}`,
        method: 'get',
    })
}

export {login, register,getCaptcha,findUserByName,getSelfInfo,getUserOnlineStatus,getUserInfo}