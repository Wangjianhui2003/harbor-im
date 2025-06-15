//摄像头类,用来获取和操控媒体流

class IMCamera {
    constructor() {
        this.stream = null
    }

    //媒体是否可用
    isEnable() {
        return !!navigator && !!navigator.mediaDevices && !!navigator.mediaDevices.getUserMedia;
    }

    //关闭流
    close(){
        //停止媒体流
        if (this.stream){
            this.stream.getTracks().forEach((track)=>{
                track.stop()
            })
        }
    }

    //打开音视频
    async openVideo(){
        //之前有流关闭
        if(this.stream){
            this.close()
        }
        //约束
        let constraints = {
            video: true,
            audio: {
                //去回音
                echoCancellation: true,
                //降噪
                noiseSuppression: true,
            }
        }

        try {
            const stream = await navigator.mediaDevices.getUserMedia(constraints)
            this.stream = stream;
            return stream
        } catch (err){
            console.log(err,"摄像头未能正常打开")
            throw {
                code: 0,
                message: "摄像头未能正常打开"
            }
        }
    }

    //打开音频
    openAudio(){
        return new Promise((resolve,reject)=>{
            if(this.stream){
                this.close()
            }
            let constraints = {
                video: false,
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                }
            }
            navigator.mediaDevices.getUserMedia(constraints).then(stream => {
                this.stream = stream;
            }).catch(err=>{
                console.log(err,"音频未能打开")
                reject({
                    code: 0,
                    message: "音频未能打开"
                })
            })
        })
    }
}

export default IMCamera;


