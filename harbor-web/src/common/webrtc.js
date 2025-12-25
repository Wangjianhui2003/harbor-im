/**
 * webRTC实现类
 */

class WebRTC {
  constructor() {
    this.configuration = {};
    this.stream = null;
    this.peerConnection = null;
  }

  //是否支持webrtc接口
  isEnable() {
    window.RTCPeerConnection =
      window.RTCPeerConnection ||
      window.webkitRTCPeerConnection ||
      window.mozRTCPeerConnection;
    window.RTCSessionDescription =
      window.RTCSessionDescription ||
      window.webkitRTCSessionDescription ||
      window.mozRTCSessionDescription;
    window.RTCIceCandidate =
      window.RTCIceCandidate ||
      window.webkitRTCIceCandidate ||
      window.mozRTCIceCandidate;
    return !!window.RTCPeerConnection;
  }

  //初始化PeerConnection配置
  init(configuration) {
    this.configuration = configuration;
  }

  //设置PeerConnection ontrack事件的回调
  setupPeerConnection(callback) {
    this.peerConnection = new RTCPeerConnection(this.configuration);
    this.peerConnection.ontrack = (event) => {
      callback(event.streams[0]);
    };
  }

  //设置（重置）流和track
  setStream(stream) {
    // 原来有流，移除
    if (this.stream) {
      console.log("移除stream");
      //TODO：removeStream api已经被废弃,通过sender来移除
      let senders = this.peerConnection.getSenders();
      senders.forEach((sender) => {
        this.peerConnection.removeTrack(sender);
      });
    }
    console.log("stream:", stream);
    if (stream) {
      stream.getTracks().forEach((track) => {
        console.log("添加track");
        this.peerConnection.addTrack(track, stream);
      });
    }
    this.stream = stream;
  }

  //添加监听器
  //并设置 当Ice获取到候选地址后的 回调函数 对candidate进行操作
  onIceCandidate(callback) {
    //监听器
    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        callback(event.candidate);
      }
    };
  }

  //添加监听器
  //并设置 当ICE连接发生变化事的 回调函数 传入state
  onICEStateChange(callback) {
    this.peerConnection.oniceconnectionstatechange = (event) => {
      let state = event.target.iceConnectionState;
      console.log("ICE连接发生变化:" + state);
      callback(state);
    };
  }

  //创建本地sdp信息
  createOffers() {
    return new Promise((resolve, reject) => {
      //offer参数
      const offerParam = {
        offerToReceiveAudio: 1,
        offerToReceiveVideo: 1,
      };
      // 创建本地sdp信息
      this.peerConnection
        .createOffer(offerParam)
        .then((offer) => {
          //设置
          console.log("offer:", offer);
          this.peerConnection.setLocalDescription(offer);
          resolve(offer);
        })
        .catch((err) => {
          reject(err);
        });
    });
  }

  /**
   * 接收offer
   * @param offer 发起方发来的offer
   */
  async createAnswers(offer) {
    console.log("收到offer，生成answer,offer:offer", offer);
    //设置远端sdp
    await this.setRemoteSDP(offer);
    console.log("remote sdp", this.peerConnection.remoteDescription);
    //创建本地sdp
    const offerParam = {
      offerToReceiveAudio: 1,
      offerToReceiveVideo: 1,
    };
    try {
      let answer = await this.peerConnection.createAnswer(offerParam);
      this.peerConnection.setLocalDescription(answer);
      return answer;
    } catch (err) {
      console.error(err);
    }
  }

  //设置远程SDP信息
  setRemoteSDP(sdpObject) {
    return this.peerConnection.setRemoteDescription(sdpObject);
  }

  //添加candidate信息
  addICECandidate(candidate) {
    this.peerConnection
      .addIceCandidate(new RTCIceCandidate(candidate))
      .catch((err) => console.log(err));
  }

  //关闭RTC连接
  close() {
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection.onicecandidate = null;
      this.peerConnection.oniceconnectionstatechange = null;
      this.peerConnection.ontrack = null;
      this.peerConnection = null;
    }
  }
}

export default WebRTC;
