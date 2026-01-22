package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.annotation.RepeatSubmit;
import com.jianhui.project.harbor.platform.dao.entity.RequestFriendDO;
import com.jianhui.project.harbor.platform.dao.entity.RequestGroupDO;
import com.jianhui.project.harbor.platform.dto.request.AddFriendReqDTO;
import com.jianhui.project.harbor.platform.dto.request.AddGroupReqDTO;
import com.jianhui.project.harbor.platform.dto.request.DealFriendReqDTO;
import com.jianhui.project.harbor.platform.dto.request.DealGroupReqDTO;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.RequestFriendService;
import com.jianhui.project.harbor.platform.service.RequestGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 添加好友/群聊请求控制器
 *
 * @author wangj
 */
@Tag(name = "添加请求")
@RestController
@RequestMapping("/add")
@RequiredArgsConstructor
public class AddController {

    private final RequestFriendService requestFriendService;
    private final RequestGroupService requestGroupService;

    @RepeatSubmit
    @PostMapping("/friend")
    @Operation(summary = "发送好友请求", description = "根据对方addType决定：0直接添加，1发送请求，2禁止")
    public Result<Void> addFriend(@Valid @RequestBody AddFriendReqDTO dto) {
        requestFriendService.addFriend(dto);
        return Results.success();
    }

    @RepeatSubmit
    @PostMapping("/friend/deal")
    @Operation(summary = "处理好友请求", description = "同意或拒绝好友请求")
    public Result<Void> dealFriendRequest(@Valid @RequestBody DealFriendReqDTO dto) {
        requestFriendService.dealFriendRequest(dto);
        return Results.success();
    }

    @GetMapping("/friend/sent")
    @Operation(summary = "我发送的好友请求", description = "查询当前用户发送的好友请求列表")
    public Result<List<RequestFriendDO>> getSentFriendRequests() {
        return Results.success(requestFriendService.findSentRequests());
    }

    @GetMapping("/friend/received")
    @Operation(summary = "我收到的好友请求", description = "查询当前用户收到的好友请求列表")
    public Result<List<RequestFriendDO>> getReceivedFriendRequests() {
        return Results.success(requestFriendService.findReceivedRequests());
    }

    @RepeatSubmit
    @PostMapping("/group")
    @Operation(summary = "发送群组请求", description = "根据群组joinType决定：0直接加入，1发送请求，2禁止")
    public Result<Void> addGroup(@Valid @RequestBody AddGroupReqDTO dto) {
        requestGroupService.addGroup(dto);
        return Results.success();
    }

    @RepeatSubmit
    @PostMapping("/group/deal")
    @Operation(summary = "处理群组请求", description = "群主或管理员同意或拒绝加群请求")
    public Result<Void> dealGroupRequest(@Valid @RequestBody DealGroupReqDTO dto) {
        requestGroupService.dealGroupRequest(dto);
        return Results.success();
    }

    @GetMapping("/group/sent")
    @Operation(summary = "我发送的群组请求", description = "查询当前用户发送的加群请求列表")
    public Result<List<RequestGroupDO>> getSentGroupRequests() {
        return Results.success(requestGroupService.findSentRequests());
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "群组加入请求", description = "查询指定群组的加入请求列表")
    public Result<List<RequestGroupDO>> getGroupRequests(
            @NotNull(message = "群组ID不能为空") @PathVariable Long groupId) {
        return Results.success(requestGroupService.findByGroupId(groupId));
    }

    @PostMapping("/group/list")
    @Operation(summary = "批量查询群组请求", description = "根据群组ID列表查询加入请求")
    public Result<List<RequestGroupDO>> findRequestsByGroupIds(@RequestBody List<Long> groupIds) {
        return Results.success(requestGroupService.findByGroupIds(groupIds));
    }
}
