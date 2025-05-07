package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.pojo.req.LoginReq;
import com.jianhui.project.harbor.platform.pojo.req.RegisterReq;
import com.jianhui.project.harbor.platform.pojo.req.UserUpdateReq;
import com.jianhui.project.harbor.platform.pojo.resp.LoginResp;
import com.jianhui.project.harbor.platform.pojo.resp.OnlineTerminalResp;
import com.jianhui.project.harbor.platform.pojo.resp.UserInfoResp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
* @author wjh2
* @description 针对表【t_user(用户)】的数据库操作Service
*/
public interface UserService extends IService<User> {

    void register(RegisterReq registerReq);

    LoginResp login(LoginReq loginReq);

    LoginResp refreshToken(String refreshToken);

    List<OnlineTerminalResp> getOnlineTerminals(@NotNull String userIds);

    UserInfoResp findUserById(@NotNull Long id);

    void updateUserInfo(@Valid UserUpdateReq userUpdateReq);

    List<UserInfoResp> findUserByName(String name);
}
