package com.jianhui.project.harbor.platform.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.common.util.JwtUtil;
import com.jianhui.project.harbor.platform.config.props.JwtProperties;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.enums.ResultCode;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.mapper.UserMapper;
import com.jianhui.project.harbor.platform.pojo.req.LoginReq;
import com.jianhui.project.harbor.platform.pojo.req.RegisterReq;
import com.jianhui.project.harbor.platform.pojo.resp.LoginResp;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.jianhui.project.harbor.platform.constant.RedisConstant.CHECK_CODE_PREFIX;


@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public void register(RegisterReq registerReq) {
        try {
            String captcha = redisTemplate.opsForValue().
                    get(CHECK_CODE_PREFIX + registerReq.getCaptchaKey());
            if (!registerReq.getCaptcha().equalsIgnoreCase(captcha)) {
                throw new GlobalException("注册验证码错误");
            }
            User user = userMapper.getByUsername(registerReq.getUsername());
            if (user != null) {
                throw new GlobalException(ResultCode.USERNAME_ALREADY_REGISTER);
            }
            user = BeanUtils.copyProperties(registerReq, User.class);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            this.save(user);
            log.info("注册用户，用户id:{},用户名:{},昵称:{}", user.getId(),registerReq.getUsername(),registerReq.getNickname());
        } finally {
            //删除验证码,防止猜验证码
            redisTemplate.delete(CHECK_CODE_PREFIX + registerReq.getCaptchaKey());
        }
    }

    @Override
    public LoginResp login(LoginReq loginReq) {
        try {
            // 校验验证码
            String captcha = redisTemplate.opsForValue().
                    get(CHECK_CODE_PREFIX + loginReq.getCaptchaKey());
            if (!loginReq.getCaptcha().equalsIgnoreCase(captcha)) {
                throw new GlobalException("注册验证码错误");
            }
            // 查询用户
            User user = userMapper.getByUsername(loginReq.getUsername());
            if (Objects.isNull(user)) {
                throw new GlobalException("用户不存在");
            }
            // 是否被封禁
            if (user.getIsBanned().equals(1)) {
                String tip = String.format("您的账号因'%s'已被管理员封禁,请联系客服!",user.getReason());
                throw new GlobalException(tip);
            }
            // 校验密码
            if (!passwordEncoder.matches(loginReq.getPassword(), user.getPassword())) {
                throw new GlobalException(ResultCode.PASSWOR_ERROR);
            }
            // 生成token
            UserSession session = BeanUtils.copyProperties(user, UserSession.class);
            session.setUserId(user.getId());
            session.setTerminal(loginReq.getTerminal());
            String strJson = JSON.toJSONString(session);
            String accessToken = JwtUtil.sign(user.getId(), strJson, jwtProperties.getAccessTokenExpireIn(),
                    jwtProperties.getAccessTokenSecret());
            String refreshToken = JwtUtil.sign(user.getId(), strJson, jwtProperties.getRefreshTokenExpireIn(),
                    jwtProperties.getRefreshTokenSecret());

            return LoginResp.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(jwtProperties.getAccessTokenExpireIn())
                    .refreshTokenExpiresIn(jwtProperties.getRefreshTokenExpireIn()).build();
        } finally {
            //删除验证码,防止猜验证码
            redisTemplate.delete(CHECK_CODE_PREFIX + loginReq.getCaptchaKey());
        }
    }

    @Override
    public LoginResp refreshToken(String refreshToken) {
        //验证 token
        if (!JwtUtil.checkSign(refreshToken, jwtProperties.getRefreshTokenSecret())) {
            throw new GlobalException("您的登录信息已过期，请重新登录");
        }
        String strJson = JwtUtil.getInfo(refreshToken);
        Long userId = JwtUtil.getUserId(refreshToken);
        User user = this.getById(userId);
        if (Objects.isNull(user)) {
            throw new GlobalException("用户不存在");
        }
        if (user.getIsBanned().equals(1)) {
            String tip = String.format("您的账号因'%s'被管理员封禁,请联系客服!",user.getReason());
            throw new GlobalException(tip);
        }
        String accessToken =
                JwtUtil.sign(userId, strJson, jwtProperties.getAccessTokenExpireIn(), jwtProperties.getAccessTokenSecret());
        String newRefreshToken = JwtUtil.sign(userId, strJson, jwtProperties.getRefreshTokenExpireIn(),
                jwtProperties.getRefreshTokenSecret());
        LoginResp vo = LoginResp.builder().build();
        vo.setAccessToken(accessToken);
        vo.setAccessTokenExpiresIn(jwtProperties.getAccessTokenExpireIn());
        vo.setRefreshToken(newRefreshToken);
        vo.setRefreshTokenExpiresIn(jwtProperties.getRefreshTokenExpireIn());
        return vo;
    }
}




