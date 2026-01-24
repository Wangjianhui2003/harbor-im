package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.annotation.RepeatSubmit;
import com.jianhui.project.harbor.platform.dto.response.GroupInviteRespDTO;
import com.jianhui.project.harbor.platform.dto.response.GroupMemberRespDTO;
import com.jianhui.project.harbor.platform.dto.response.GroupRespDTO;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "群聊")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @RepeatSubmit
    @Operation(summary = "创建群聊", description = "创建群聊")
    @PostMapping("/create")
    public Result<GroupRespDTO> createGroup(@Valid @RequestBody GroupRespDTO vo) {
        return Results.success(groupService.createGroup(vo));
    }

    @RepeatSubmit
    @Operation(summary = "修改群聊信息", description = "修改群聊信息")
    @PutMapping("/modify")
    public Result<GroupRespDTO> modifyGroup(@Valid @RequestBody GroupRespDTO vo) {
        return Results.success(groupService.modifyGroup(vo));
    }

    @RepeatSubmit
    @Operation(summary = "解散群聊", description = "解散群聊")
    @DeleteMapping("/delete/{groupId}")
    public Result deleteGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return Results.success();
    }


    @Operation(summary = "查询群聊", description = "查询单个群聊信息")
    @GetMapping("/find/{groupId}")
    public Result<GroupRespDTO> findGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        return Results.success(groupService.findById(groupId));
    }

    @Operation(summary = "查询群聊列表", description = "查询群聊列表")
    @GetMapping("/list")
    public Result<List<GroupRespDTO>> findGroups() {
        return Results.success(groupService.findGroups());
    }

    @RepeatSubmit
    @Operation(summary = "邀请进群", description = "邀请好友进群")
    @PostMapping("/invite")
    public Result invite(@Valid @RequestBody GroupInviteRespDTO vo) {
        groupService.invite(vo);
        return Results.success();
    }

    @Operation(summary = "查询群聊成员", description = "查询群聊成员")
    @GetMapping("/members/{groupId}")
    public Result<List<GroupMemberRespDTO>> findGroupMembers(
            @NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        return Results.success(groupService.findGroupMembers(groupId));
    }

    @RepeatSubmit
    @Operation(summary = "退出群聊", description = "退出群聊")
    @DeleteMapping("/quit/{groupId}")
    public Result quitGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        groupService.quitGroup(groupId);
        return Results.success();
    }

    @RepeatSubmit
    @Operation(summary = "踢出群聊", description = "将用户踢出群聊")
    @DeleteMapping("/kick/{groupId}")
    public Result kickGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId,
                            @NotNull(message = "用户id不能为空") @RequestParam Long userId) {
        groupService.kickFromGroup(groupId, userId);
        return Results.success();
    }

    @RepeatSubmit
    @Operation(summary = "搜索群聊", description = "通过id搜索群聊(用于加入)")
    @GetMapping("/search")
    public Result<GroupRespDTO> searchGroups(@RequestParam(required = true) Long groupId) {
        GroupRespDTO groupRespDTO = groupService.searchById(groupId);
        return Results.success(groupRespDTO);
    }

    @Operation(summary = "获取管理的群组", description = "获取当前用户管理的群组ID列表（群主或管理员）")
    @GetMapping("/managed")
    public Result<List<Long>> getManagedGroupIds() {
        return Results.success(groupService.getManagedGroupIds());
    }

    @RepeatSubmit
    @Operation(summary = "设置管理员", description = "设置或移除群管理员")
    @PutMapping("/admin/{groupId}")
    public Result<Void> setGroupAdmin(
            @NotNull(message = "群聊id不能为空") @PathVariable Long groupId,
            @NotNull(message = "用户id不能为空") @RequestParam Long userId,
            @NotNull(message = "是否管理员不能为空") @RequestParam Boolean isAdmin) {
        groupService.setGroupAdmin(groupId, userId, isAdmin);
        return Results.success();
    }
}

