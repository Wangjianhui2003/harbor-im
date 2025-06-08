import {CMD_TYPE} from "../common/enums.js";

var websock = null;
let rec; //断线重连后，延迟5秒重新创建WebSocket连接  rec用来存储延迟请求的代码
let isConnect = false; //连接标识 避免重复连接
let connectCallBack = null;
let messageCallBack = null;
let closeCallBack = null

/**
 * 连接
 */
const connect = (wsUrl,accessToken) => {
    try {
        if (isConnect) {
            return;
        }
        console.log("准备连接WebSocket");
        websock = new WebSocket(wsUrl);

        //打开后回调函数
        websock.onopen = () => {
            console.log('WebSocket Opened')
            isConnect = true;
            // 发送登录命令
            let loginInfo = {
                cmd: CMD_TYPE.LOGIN,
                data: {
                    accessToken: accessToken,
                }
            }
            websock.send(JSON.stringify(loginInfo));
        }

        //收到消息
        websock.onmessage = (e) => {
            let sendInfo = JSON.parse(e.data)
            if (sendInfo.cmd === CMD_TYPE.LOGIN) {
                //成功登录
                //开始心跳
                heartCheck.start()
                // 连接后回调函数
                console.log('WebSocket登录成功')
                connectCallBack && connectCallBack();
            }else if (sendInfo.cmd === CMD_TYPE.HEARTBEAT) {
                //重置心跳
                heartCheck.reset()
            }else {
                //收到消息
                console.log("收到消息:", sendInfo);
                messageCallBack && messageCallBack(sendInfo.cmd, sendInfo.data)
            }
        }

        //连接关闭回调
        websock.onclose = (e) => {
            console.log('WebSocket连接关闭')
            isConnect = false; //断开后修改标识
            closeCallBack && closeCallBack(e);
        }

    }catch (error) {
        console.log('ws尝试连接失败',error);
        reconnect(wsUrl, accessToken); //如果无法连接上webSocket 那么重新连接！可能会因为服务器重新部署，或者短暂断网等导致无法创建连接
    }
}

/**
 * 重连
 */
const reconnect = (wsUrl,accessToken) => {
    console.log("尝试重新连接");
    if (isConnect) {
        //如果已经连上就不在重连了
        return;
    }
    rec && clearTimeout(rec);
    rec = setTimeout(function () { // 延迟5秒重连  避免过多次过频繁请求重连
        connect(wsUrl, accessToken);
    }, 5000)
}

/**
 * 关闭连接
 */
const close = (code) => {
    websock && websock.close(code);
}

/**
 * 心跳
 */
const heartCheck = {
    timeout: 10000, //10s心跳
    timeoutObj: null, //延时发送消息对象（启动心跳新建这个对象，收到消息后重置对象）
    //发送心跳
    start: function() {
        if(isConnect){
            console.log("send heartbeat");
            let heartbeat = {
                cmd: CMD_TYPE.HEARTBEAT,
                data: {}
            }
            websock.send(JSON.stringify(heartbeat));
        }
    },
    //重置心跳
    reset: function() {
        clearTimeout(this.timeoutObj);
        this.timeoutObj = setTimeout(function () {
            heartCheck.start()
        },this.timeout)
    }
}

/**
 * ws发送消息
 */
const sendMessage = (agentData) => {
    if(websock.readyState === WebSocket.OPEN) {
        websock.send(JSON.stringify(agentData));
    }else if(websock.readyState === WebSocket.CONNECTING){
        //延迟发
        setTimeout(function (){
            sendMessage(agentData)
        }, 1000)
    }else{
        setTimeout(function (){
            sendMessage(agentData)
        },1000)
    }
}

/**
 * 设置回调函数
 */
let onConnect = (callback) => {
    connectCallBack = callback;
}

let onMessage = (callback) => {
    messageCallBack = callback;
}

let onClose = (callback) => {
    closeCallBack = callback;
}

export {
    connect,
    reconnect,
    close,
    sendMessage,
    onConnect,
    onMessage,
    onClose,
}