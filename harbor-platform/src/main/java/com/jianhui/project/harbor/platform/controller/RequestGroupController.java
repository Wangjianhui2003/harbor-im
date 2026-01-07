package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.annotation.RepeatSubmit;
import com.jianhui.project.harbor.platform.entity.RequestGroup;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.RequestGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "群聊请求")
@RestController
@RequestMapping("/add")
@RequiredArgsConstructor
public class RequestGroupController {

    private final RequestGroupService requestGroupService;

    @RepeatSubmit
    @PostMapping("/group")
    @Operation(summary = "发送添加群组请求", description = "发送添加群组请求")
    public Result<Void> addGroupRequest(@Valid @RequestBody RequestGroup requestGroup) {
        requestGroupService.addGroupRequest(requestGroup);
        return Results.success();
    }

    @RepeatSubmit
    @PostMapping("/group/deal")
    @Operation(summary = "处理添加群组请求", description = "处理添加群组请求，1:同意 2:拒绝")
    public Result<Void> dealGroupRequest(@Valid @RequestBody RequestGroup requestGroup) {
        requestGroupService.dealGroupRequest(requestGroup);
        return Results.success();
    }

    @GetMapping("/group/sent")
    @Operation(summary = "查询我发送的群组请求列表", description = "查询我发送的群组请求列表")
    public Result<List<RequestGroup>> findSentRequests() {
        return Results.success(requestGroupService.findSentRequests());
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "查询指定群组的请求列表", description = "查询指定群组的请求列表（用于群主/管理员查看）")
    public Result<List<RequestGroup>> findGroupRequests(@PathVariable("groupId") Long groupId) {
        return Results.success(requestGroupService.findGroupRequests(groupId));
    }
}
