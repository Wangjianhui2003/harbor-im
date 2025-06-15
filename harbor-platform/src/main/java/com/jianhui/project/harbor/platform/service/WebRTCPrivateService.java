package com.jianhui.project.harbor.platform.service;

/**
 * webrtc 通信服务
 */
public interface WebRTCPrivateService {

    /**
     * 发起方调用
     * @param uid 接收方userId
     * @param mode 模式(VIDEO,VOICE)
     * @param offer offer
     */
    void call(Long uid, String mode,String offer);

    /**
     * 接收方调用
     * @param uid 发起方userId
     * @param answer answer
     */
    void accept(Long uid,  String answer);

    /**
     * 拒绝通话，接收方调用
     * @param uid 发送方userId
     */
    void reject(Long uid);

    /**
     * 取消通话（还没有接通时）
     * @param uid 接收方userId
     */
    void cancel(Long uid);

    /**
     * 通话失败
     * @param uid 对方userId
     * @param reason 原因
     */
    void failed(Long uid, String reason);

    /**
     * 挂断通信，双方都可以调用
     * @param uid 对方userId
     */
    void hangup(Long uid);

    /**
     * 发送candidate信息
     * @param uid 对方userId
     * @param candidate candidate信息
     */
    void candidate(Long uid, String candidate);

    /**
     * 心跳 每15秒一次延续会话 会话超时时间60s
     * @param uid 对方userId
     */
    void heartbeat(Long uid);

}
