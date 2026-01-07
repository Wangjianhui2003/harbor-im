package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.annotation.RepeatSubmit;
import com.jianhui.project.harbor.platform.entity.RequestFriend;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.RequestFriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "好友请求")
@RestController
@RequestMapping("/add")
@RequiredArgsConstructor
public class RequestFriendController {

    private final RequestFriendService requestFriendService;

    @RepeatSubmit
    @PostMapping("/friend")
    @Operation(summary = "发送添加好友请求", description = "发送添加好友请求")
    public Result<Void> addFriendRequest(@Valid @RequestBody RequestFriend requestFriend) {
        requestFriendService.addFriendRequest(requestFriend);
        return Results.success();
    }

    @RepeatSubmit
    @PostMapping("/friend/deal")
    @Operation(summary = "处理添加好友请求", description = "处理添加好友请求，1:同意 2:拒绝")
    public Result<Void> dealFriendRequest(@Valid @RequestBody RequestFriend requestFriend) {
        requestFriendService.dealFriendRequest(requestFriend);
        return Results.success();
    }

    @GetMapping("/friend/sent")
    @Operation(summary = "查询我发送的好友请求列表", description = "查询我发送的好友请求列表")
    public Result<List<RequestFriend>> findSentRequests() {
        return Results.success(requestFriendService.findSentRequests());
    }

    @GetMapping("/friend/received")
    @Operation(summary = "查询我接收的好友请求列表", description = "查询我接收的好友请求列表")
    public Result<List<RequestFriend>> findReceivedRequests() {
        return Results.success(requestFriendService.findReceivedRequests());
    }
}
