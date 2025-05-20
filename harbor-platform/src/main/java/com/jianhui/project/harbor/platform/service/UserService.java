package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.pojo.dto.ModifyPwdDTO;
import com.jianhui.project.harbor.platform.pojo.req.LoginReq;
import com.jianhui.project.harbor.platform.pojo.req.RegisterReq;
import com.jianhui.project.harbor.platform.pojo.req.UserUpdateReq;
import com.jianhui.project.harbor.platform.pojo.resp.LoginResp;
import com.jianhui.project.harbor.platform.pojo.resp.UserVO;
import com.jianhui.project.harbor.platform.pojo.vo.OnlineTerminalVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface UserService extends IService<User> {

    void register(RegisterReq registerReq);

    LoginResp login(LoginReq loginReq);

    /**
     * 用refreshToken换取新 token
     */
    LoginResp refreshToken(String refreshToken);

    /**
     * 获取用户在线的终端类型
     */
    List<OnlineTerminalVO> getOnlineTerminals(@NotNull String userIds);

    /**
     * 根据用户昵id查询用户以及在线状态
     */
    UserVO findUserById(@NotNull Long id);

    /**
     * 更新用户信息，好友昵称和群聊昵称等冗余信息也会更新
     */
    void updateUserInfo(@Valid UserUpdateReq userUpdateReq);

    /**
     * 根据用户昵称查询用户，最多返回20条数据
     */
    List<UserVO> findUserByName(String name);

    /**
     * 修改用户密码
     */
    void modifyPassword(@Valid ModifyPwdDTO dto);
}
