package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.WebRTCPrivateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "单人通话")
@RestController
@RequestMapping("/webrtc/private")
@RequiredArgsConstructor
public class WebRTCPrivateController {

    private final WebRTCPrivateService webRTCPrivateService;

    @Operation(summary = "呼叫视频通话")
    @PostMapping("/call")
    public Result call(@RequestParam Long userId, @RequestParam(defaultValue = "VIDEO") String mode,
                       @RequestBody String offer) {
        webRTCPrivateService.call(userId, mode, offer);
        return Results.success();
    }

    @Operation(summary = "接受视频通话")
    @PostMapping("/accept")
    public Result accept(@RequestParam Long userId, @RequestBody String answer) {
        webRTCPrivateService.accept(userId, answer);
        return Results.success();
    }

    @Operation(summary = "拒绝视频通话")
    @PostMapping("/reject")
    public Result reject(@RequestParam Long userId) {
        webRTCPrivateService.reject(userId);
        return Results.success();
    }

    @Operation(summary = "取消呼叫")
    @PostMapping("/cancel")
    public Result cancel(@RequestParam("userId") Long userId) {
        webRTCPrivateService.cancel(userId);
        return Results.success();
    }

    @Operation(summary = "呼叫失败")
    @PostMapping("/failed")
    public Result failed(@RequestParam Long userId, @RequestParam String reason) {
        webRTCPrivateService.failed(userId, reason);
        return Results.success();
    }

    @Operation(summary = "挂断")
    @PostMapping("/hangup")
    public Result hangup(@RequestParam Long userId) {
        webRTCPrivateService.hangup(userId);
        return Results.success();
    }

    @PostMapping("/candidate")
    @Operation(summary = "同步candidate")
    public Result candidate(@RequestParam Long userId, @RequestBody String candidate) {
        webRTCPrivateService.candidate(userId, candidate);
        return Results.success();
    }

    @Operation(summary = "心跳")
    @PostMapping("/heartbeat")
    public Result heartbeat(@RequestParam Long userId) {
        webRTCPrivateService.heartbeat(userId);
        return Results.success();
    }
}
