//摄像头类,用来获取和操控媒体流

class IMCamera {
  constructor() {
    this.stream = null;
  }

  //媒体是否可用
  isEnable() {
    return (
      !!navigator &&
      !!navigator.mediaDevices &&
      !!navigator.mediaDevices.getUserMedia
    );
  }

  //关闭流
  close() {
    //停止媒体流
    if (this.stream) {
      this.stream.getTracks().forEach((track) => {
        track.stop();
      });
    }
    this.stream = null;
  }

  //打开音视频
  async openVideo() {
    //之前有流关闭
    if (this.stream) {
      this.close();
    }
    //约束
    let constraints = {
      video: true,
      audio: {
        //去回音
        echoCancellation: true,
        //降噪
        noiseSuppression: true,
      },
    };

    try {
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      this.stream = stream;
      return stream;
    } catch (err) {
      console.log(err, "摄像头未能正常打开");
      throw {
        code: 0,
        message: "摄像头未能正常打开",
      };
    }
  }

  //打开音频
  async openAudio() {
    if (this.stream) {
      this.close();
    }
    try {
      let constraints = {
        video: false,
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
        },
      };
      let stream = await navigator.mediaDevices.getUserMedia(constraints);
      this.stream = stream;
      return stream;
    } catch (err) {
      console.log("未能获取麦克风stream流");
      throw {
        code: 0,
        message: "未能获取麦克风stream流",
      };
    }
  }
}

export default IMCamera;
