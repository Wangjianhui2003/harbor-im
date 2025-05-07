package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.pojo.req.UserUpdateReq;
import com.jianhui.project.harbor.platform.pojo.resp.OnlineTerminalResp;
import com.jianhui.project.harbor.platform.pojo.resp.UserInfoResp;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户相关")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/terminal/online")
    @Operation(summary = "判断用户哪个终端在线", description = "返回在线的用户id的终端集合")
    public Result<List<OnlineTerminalResp>> getOnlineTerminal(@NotNull @RequestParam("userIds") String userIds) {
        return Results.success(userService.getOnlineTerminals(userIds));
    }


    @GetMapping("/self")
    @Operation(summary = "获取当前用户信息", description = "获取当前用户信息")
    public Result<UserInfoResp> findSelfInfo() {
        UserSession session = SessionContext.getSession();
        User user = userService.getById(session.getUserId());
        UserInfoResp userInfoResp= BeanUtils.copyProperties(user, UserInfoResp.class);
        return Results.success(userInfoResp);
    }

    @GetMapping("/find/{id}")
    @Operation(summary = "查找用户", description = "根据id查找用户")
    public Result<UserInfoResp> findById(@NotNull @PathVariable("id") Long id) {
        return Results.success(userService.findUserById(id));
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户信息", description = "修改用户信息，仅允许修改登录用户信息")
    public Result update(@Valid @RequestBody UserUpdateReq userUpdateReq) {
        userService.updateUserInfo(userUpdateReq);
        return Results.success();
    }

    @GetMapping("/findByName")
    @Operation(summary = "查找用户", description = "根据用户名或昵称查找用户")
    public Result<List<UserInfoResp>> findByName(@RequestParam String name) {
        return Results.success(userService.findUserByName(name));
    }
}

