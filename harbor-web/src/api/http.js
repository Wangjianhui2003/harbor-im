import axios from "axios";
import {ElMessage} from "element-plus";

const http = axios.create({
    baseURL: "http://localhost:8100/api",
    timeout: "1000 * 10",
})

http.interceptors.request.use(
    config => {
    let accessToken = sessionStorage.getItem("accessToken");
    if (accessToken) {
        config.headers.accessToken = encodeURIComponent(accessToken);
    }
    return config
    },
    error => {
        return Promise.reject(error)
    }
)

http.interceptors.response.use(async response => {
    //后端成功返回
    if (response.data.code == 200) {
        return response.data.data;
    } else if (response.data.code == 400) {
        location.href = "/"
    //鉴权失败
    } else if (response.data.code == 401) {
        console.log("token失效，尝试重新获取")
        let refreshToken = sessionStorage.getItem("refreshToken");
        if (!refreshToken) {
            location.href = "/";
        }
        // 发送请求, 进行刷新token操作, 获取新的token
        const data = await http({
            method: 'put',
            url: '/refreshToken',
            headers: {
                refreshToken: refreshToken
            }
        }).catch(() => {
            location.href = "/";
        })
        // 保存token
        sessionStorage.setItem("accessToken", data.accessToken);
        sessionStorage.setItem("refreshToken", data.refreshToken);
        // 重新发送刚才的请求
        return http(response.config)
    //其他错误,打印msg
    } else {
        ElMessage({
            message: response.data.message,
            type: 'error',
            duration: 1500,
        })
        return Promise.reject(response.data)
    }
    }, error => {
    if (!error.response) {
        ElMessage({
            message: '网络异常，请检查连接',
            type: 'error',
            duration: 1500,
        });
        return Promise.reject(error);
    }
    switch (error.response.status) {
        case 400:
            ElMessage({
                message: error.response.data,
                type: 'error',
                duration: 1500,
            })
            break
        case 401:
            location.href = "/";
            break
        case 405:
            ElMessage({
                message: 'http请求方式有误',
                type: 'error',
                duration: 1500,
            })
            break
        case 404:
        case 500:
            ElMessage({
                message: '服务器出了点小差，请稍后再试',
                type: 'error',
                duration: 1500,
            })
            break
        case 501:
            ElMessage({
                message: '服务器不支持当前请求所需要的某个功能',
                type: 'error',
                duration: 1500,
            })
            break
    }

    return Promise.reject(error)
})

export default http