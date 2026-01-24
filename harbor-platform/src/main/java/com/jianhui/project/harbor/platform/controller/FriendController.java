package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.dto.response.FriendRespDTO;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "好友")
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/list")
    @Operation(summary = "好友列表", description = "获取好友列表")
    public Result<List<FriendRespDTO>> findFriends() {
        return Results.success(friendService.findFriends());
    }


    @GetMapping("/find/{friendId}")
    @Operation(summary = "查找好友信息", description = "查找好友信息")
    public Result<FriendRespDTO> findFriend(@NotNull(message = "好友id不可为空") @PathVariable("friendId") Long friendId) {
        return Results.success(friendService.findFriendInfo(friendId));
    }

    @DeleteMapping("/delete/{friendId}")
    @Operation(summary = "删除好友", description = "解除好友关系")
    public Result<Void> delFriend(@NotNull(message = "好友id不可为空") @PathVariable("friendId") Long friendId) {
        friendService.delFriend(friendId);
        return Results.success();
    }

    @PostMapping("/updateFriendNickName")
    @Operation(summary = "改变好友备注名", description = "改变好友备注名")
    public Result<Void> editFriendRemarkName(@RequestBody FriendRespDTO friendRespDTO) {
        friendService.editFriendRemarkName(friendRespDTO);
        return Results.success();
    }

}
