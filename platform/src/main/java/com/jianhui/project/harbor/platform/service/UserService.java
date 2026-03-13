package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.dao.entity.User;
import com.jianhui.project.harbor.platform.dto.request.LoginReqDTO;
import com.jianhui.project.harbor.platform.dto.request.ModifyPwdDTO;
import com.jianhui.project.harbor.platform.dto.request.RegisterReqDTO;
import com.jianhui.project.harbor.platform.dto.response.LoginRespDTO;
import com.jianhui.project.harbor.platform.dto.response.OnlineTerminalRespDTO;
import com.jianhui.project.harbor.platform.dto.response.UserRespDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface UserService extends IService<User> {

    void register(RegisterReqDTO registerReqDTO);

    LoginRespDTO login(LoginReqDTO loginReqDTO);

    /**
     * 用refreshToken换取新 token
     */
    LoginRespDTO refreshToken(String refreshToken);

    /**
     * 获取用户在线的终端类型
     */
    List<OnlineTerminalRespDTO> getOnlineTerminals(@NotNull String userIds);

    /**
     * 根据用户昵id查询用户以及在线状态
     */
    UserRespDTO findUserById(@NotNull Long id);

    /**
     * 更新用户信息，好友昵称和群聊昵称等冗余信息也会更新
     */
    void updateUserInfo(@Valid UserRespDTO userRespDTO);

    /**
     * 根据用户昵称查询用户，最多返回20条数据
     */
    List<UserRespDTO> findUserByName(String name);

    /**
     * 修改用户密码
     */
    void modifyPassword(@Valid ModifyPwdDTO dto);
}
