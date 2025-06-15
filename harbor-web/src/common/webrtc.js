/**
 * webRTC实现类
 */

class WebRTC{

    constructor() {
        this.configuration = {};
        this.stream = null;
        this.peerConnection = null;
    }

    //是否支持webrtc接口
    isEnable() {
        window.RTCPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window
            .mozRTCPeerConnection;
        window.RTCSessionDescription = window.RTCSessionDescription || window.webkitRTCSessionDescription || window
            .mozRTCSessionDescription;
        window.RTCIceCandidate = window.RTCIceCandidate || window.webkitRTCIceCandidate || window
            .mozRTCIceCandidate;
        return !!window.RTCPeerConnection;
    }

    //初始化PeerConnection配置
    init(configuration) {
        this.configuration = configuration;
    }

    //设置PeerConnection ontrack事件的回调
    setupPeerConnection(callback) {
        this.peerConnection = new RTCPeerConnection(this.configuration)
        this.peerConnection.ontrack = (event) => {
            callback(event.streams[0])
        }
    }

    //设置（重置）流和track
    setStream(stream) {
        // 原来有流，移除
        if (this.stream) {
            //TODO：removeStream api已经被废弃,通过sender来移除
            let senders = this.peerConnection.getSenders();
            senders.forEach((sender) => {
                this.peerConnection.removeTrack(sender)
            })
        }
        if (stream) {
            stream.getTracks().forEach((track) => {
                this.peerConnection.addTrack(track,stream)
            })
        }
        this.stream = stream
    }

    //添加监听器
    //并设置 当Ice获取到候选地址后的 回调函数 对candidate进行操作
    onIceCandidate(callback) {
        //监听器
        this.peerConnection.onicecandidate = event => {
            if (event.candidate) {
                callback(event.candidate)
            }
        }
    }

    //添加监听器
    //并设置 当ICE连接发生变化事的 回调函数 传入state
    onICEStateChange(callback) {
      this.peerConnection.oniceconnectionstatechange = (event) => {
          let state = event.target.iceConnectionState
          console.log("ICE连接发生变化:" + state)
          callback(state)
      }
    }

    //创建本地sdp信息
    createOffers() {
        return new Promise((resolve, reject) => {
            //offer参数
            const offerParam = {
                offerToReceiveAudio: 1,
                offerToReceiveVideo: 1
            }
            // 创建本地sdp信息
            this.peerConnection.createOffer(offerParam).then((offer) => {
                //设置
                this.peerConnection.setLocalDescription(offer)
                resolve(offer)
            }).catch(err => {
                reject(err)
            })
        })
    }

    /**
     * 接收offer
     * @param offer 发起方发来的offer
     */
    createAnswers(offer) {
        return new Promise((resolve, reject) => {
            //设置远端sdp
            this.setRemoteSDP(offer)
            //创建本地sdp
            const offerParam = {
                offerToReceiveAudio: 1,
                offerToReceiveVideo: 1
            }
            this.peerConnection.createAnswer(offerParam).then((answer) => {
                this.peerConnection.setLocalDescription(answer)
                resolve(answer)
            }).catch(err => {
                reject(err)
            })
        })
    }

    //设置远程SDP信息
    setRemoteSDP(sdp) {
       this.peerConnection.setRemoteDescription(sdp)
    }

    //添加candidate信息
    addICECandidate(candidate) {
        this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
    }

    //关闭RTC连接
    close(){
        if(this.peerConnection) {
            this.peerConnection.close();
            this.peerConnection.onicecandidate = null;
            this.peerConnection.oniceconnectionstatechange = null;
            this.peerConnection.onaddstream = null;
            this.peerConnection = null;
        }
    }
}

export default WebRTC