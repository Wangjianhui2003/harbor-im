package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.pojo.dto.GroupMessageDTO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMessageVO;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.GroupMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "群聊消息")
@RestController
@RequestMapping("/message/group")
@RequiredArgsConstructor
public class GroupMsgController {

    private final GroupMessageService groupMessageService;

    @PostMapping("/send")
    @Operation(summary = "发送群聊消息", description = "发送群聊消息")
    public Result<GroupMessageVO> sendMessage(@Valid @RequestBody GroupMessageDTO dto) {
        return Results.success(groupMessageService.sendMessage(dto));
    }

    @DeleteMapping("/recall/{id}")
    @Operation(summary = "撤回消息", description = "撤回群聊消息")
    public Result<GroupMessageVO> recallMessage(@NotNull(message = "消息id不能为空") @PathVariable Long id) {
        return Results.success(groupMessageService.recallMessage(id));
    }

    @GetMapping("/pullOfflineMessage")
    @Operation(summary = "拉取离线消息", description = "拉取离线消息,消息将通过webscoket异步推送")
    public Result pullOfflineMessage(@RequestParam Long minId) {
        groupMessageService.pullOfflineMessage(minId);
        return Results.success();
    }

    @PutMapping("/readed")
    @Operation(summary = "消息已读", description = "将群聊中的消息状态置为已读")
    public Result readedMessage(@RequestParam Long groupId) {
        groupMessageService.readedMessage(groupId);
        return Results.success();
    }

    @GetMapping("/findReadedUsers")
    @Operation(summary = "获取已读用户id", description = "获取消息已读用户列表")
    public Result<List<Long>> findReadedUsers(@RequestParam Long groupId,
                                              @RequestParam Long messageId) {
        return Results.success(groupMessageService.findReadedUsers(groupId, messageId));
    }

    @GetMapping("/history")
    @Operation(summary = "查询聊天记录", description = "查询聊天记录")
    public Result<List<GroupMessageVO>> findHistoryMessage(@NotNull(message = "群聊id不能为空") @RequestParam Long groupId,
                                                      @NotNull(message = "页码不能为空") @RequestParam Long page,
                                                      @NotNull(message = "size不能为空") @RequestParam Long size) {
        return Results.success(groupMessageService.findHistoryMessage(groupId, page, size));
    }
}

