package com.jianhui.project.harbor.platform.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.client.IMClient;
import com.jianhui.project.harbor.common.enums.IMTerminalType;
import com.jianhui.project.harbor.common.util.JwtUtil;
import com.jianhui.project.harbor.platform.config.props.JwtProperties;
import com.jianhui.project.harbor.platform.dao.entity.User;
import com.jianhui.project.harbor.platform.dao.mapper.FriendMapper;
import com.jianhui.project.harbor.platform.dao.mapper.GroupMapper;
import com.jianhui.project.harbor.platform.dao.mapper.GroupMemberMapper;
import com.jianhui.project.harbor.platform.dao.mapper.UserMapper;
import com.jianhui.project.harbor.platform.dto.request.LoginReqDTO;
import com.jianhui.project.harbor.platform.dto.request.ModifyPwdDTO;
import com.jianhui.project.harbor.platform.dto.request.RegisterReqDTO;
import com.jianhui.project.harbor.platform.dto.response.LoginRespDTO;
import com.jianhui.project.harbor.platform.dto.response.OnlineTerminalRespDTO;
import com.jianhui.project.harbor.platform.dto.response.UserRespDTO;
import com.jianhui.project.harbor.platform.enums.ResultCode;
import com.jianhui.project.harbor.platform.exception.GlobalException;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.session.SessionContext;
import com.jianhui.project.harbor.platform.session.UserSession;
import com.jianhui.project.harbor.platform.util.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.jianhui.project.harbor.platform.constant.RedisKey.CHECK_CODE_PREFIX;


@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final FriendMapper friendMapper;
    private final GroupMapper groupMapper;
    private final IMClient imClient;
    private final GroupMemberMapper groupMemberMapper;

    @Override
    public void register(RegisterReqDTO registerReqDTO) {
        try {
            //验证码
            String captcha = redisTemplate.opsForValue().
                    get(CHECK_CODE_PREFIX + registerReqDTO.getCaptchaKey());
            if (!registerReqDTO.getCaptcha().equalsIgnoreCase(captcha)) {
                throw new GlobalException("注册验证码错误");
            }
            //username是否已经被注册
            User user = userMapper.getByUsername(registerReqDTO.getUsername());
            if (user != null) {
                throw new GlobalException(ResultCode.USERNAME_ALREADY_REGISTER);
            }
            user = BeanUtils.copyProperties(registerReqDTO, User.class);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            this.save(user);
            log.info("注册用户，用户id:{},用户名:{},昵称:{}", user.getId(), registerReqDTO.getUsername(), registerReqDTO.getNickname());
        } finally {
            //删除验证码,防止猜验证码
            redisTemplate.delete(CHECK_CODE_PREFIX + registerReqDTO.getCaptchaKey());
        }
    }

    @Override
    public LoginRespDTO login(LoginReqDTO loginReqDTO) {
        try {
            // 校验验证码
            String captcha = redisTemplate.opsForValue().
                    get(CHECK_CODE_PREFIX + loginReqDTO.getCaptchaKey());
            if (!loginReqDTO.getCaptcha().equalsIgnoreCase(captcha)) {
                throw new GlobalException("注册验证码错误");
            }
            // 查询用户
            User user = userMapper.getByUsername(loginReqDTO.getUsername());
            if (Objects.isNull(user)) {
                throw new GlobalException("用户不存在");
            }
            // 是否被封禁
            if (user.getIsBanned().equals(1)) {
                String tip = String.format("您的账号因'%s'已被管理员封禁,请联系客服!", user.getReason());
                throw new GlobalException(tip);
            }
            // 校验密码
            if (!passwordEncoder.matches(loginReqDTO.getPassword(), user.getPassword())) {
                throw new GlobalException(ResultCode.PASSWOR_ERROR);
            }
            // 生成token
            UserSession session = BeanUtils.copyProperties(user, UserSession.class);
            session.setUserId(user.getId());
            session.setTerminal(loginReqDTO.getTerminal());
            String strJson = JSON.toJSONString(session);

            String accessToken = JwtUtil.sign(user.getId(), strJson, jwtProperties.getAccessTokenExpireIn(),
                    jwtProperties.getAccessTokenSecret());
            String refreshToken = JwtUtil.sign(user.getId(), strJson, jwtProperties.getRefreshTokenExpireIn(),
                    jwtProperties.getRefreshTokenSecret());

            return LoginRespDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(jwtProperties.getAccessTokenExpireIn())
                    .refreshTokenExpiresIn(jwtProperties.getRefreshTokenExpireIn()).build();
        } finally {
            //删除验证码,防止猜验证码
            redisTemplate.delete(CHECK_CODE_PREFIX + loginReqDTO.getCaptchaKey());
        }
    }

    @Override
    public LoginRespDTO refreshToken(String refreshToken) {
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
        //被ban
        if (user.getIsBanned().equals(1)) {
            String tip = String.format("您的账号因'%s'被管理员封禁,请联系客服!", user.getReason());
            throw new GlobalException(tip);
        }
        String accessToken =
                JwtUtil.sign(userId, strJson, jwtProperties.getAccessTokenExpireIn(), jwtProperties.getAccessTokenSecret());
        String newRefreshToken = JwtUtil.sign(userId, strJson, jwtProperties.getRefreshTokenExpireIn(),
                jwtProperties.getRefreshTokenSecret());
        LoginRespDTO loginRespDTO = LoginRespDTO.builder().build();
        loginRespDTO.setAccessToken(accessToken);
        loginRespDTO.setAccessTokenExpiresIn(jwtProperties.getAccessTokenExpireIn());
        loginRespDTO.setRefreshToken(newRefreshToken);
        loginRespDTO.setRefreshTokenExpiresIn(jwtProperties.getRefreshTokenExpireIn());
        return loginRespDTO;
    }

    @Override
    public List<OnlineTerminalRespDTO> getOnlineTerminals(String userIds) {
        List<Long> ids = Arrays.stream(userIds.split(",")).map(Long::parseLong).collect(Collectors.toList());
        Map<Long, List<IMTerminalType>> terminalMap = imClient.getOnlineTerminal(ids);
        // 组装vo
        List<OnlineTerminalRespDTO> vos = new LinkedList<>();
        terminalMap.forEach((userId, types) -> {
            List<Integer> terminals = types.stream().map(IMTerminalType::code).collect(Collectors.toList());
            vos.add(new OnlineTerminalRespDTO(userId, terminals));
        });
        return vos;
    }

    @Override
    public UserRespDTO findUserById(Long id) {
        User user = this.getById(id);
        if (Objects.isNull(user)) {
            throw new GlobalException("用户不存在");
        }
        UserRespDTO userRespDTO = BeanUtils.copyProperties(user, UserRespDTO.class);
        userRespDTO.setOnline(imClient.isOnline(id));
        return userRespDTO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserInfo(UserRespDTO userRespDTO) {
        UserSession session = SessionContext.getSession();
        if (!session.getUserId().equals(userRespDTO.getId())) {
            throw new GlobalException("不能修改其他用户信息");
        }
        User user = this.getById(userRespDTO.getId());
        if (Objects.isNull(user)) {
            throw new GlobalException("用户不存在");
        }
        //修改群聊和好友的昵称和头像
        if (!Objects.equals(user.getNickname(), userRespDTO.getNickname())
                || !Objects.equals(user.getHeadImageThumb(), userRespDTO.getHeadImageThumb())) {
            friendMapper.updateFriendNicknameAndFriendHeadImageByFriendId(userRespDTO.getNickname(), userRespDTO.getHeadImage(), userRespDTO.getId());
            groupMemberMapper.updateUserNicknameAndHeadImageByUserId(userRespDTO.getNickname(), userRespDTO.getHeadImageThumb(), userRespDTO.getId());
        }
        BeanUtils.copyProperties(userRespDTO, user);
        this.updateById(user);
        log.info("用户信息更新，用户:{}}", user);
    }

    @Override
    public List<UserRespDTO> findUserByName(String name) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.like(User::getUsername, name).or().like(User::getUsername, name).last("limit 20");
        List<User> users = this.list(queryWrapper);
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<Long> onlineUserIds = imClient.getOnlineUser(userIds);
        //TODO:获取在线用户
        return users.stream().map(u -> {
            UserRespDTO userRespDTO = BeanUtils.copyProperties(u, UserRespDTO.class);
            userRespDTO.setOnline(onlineUserIds.contains(u.getId()));
            return userRespDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void modifyPassword(ModifyPwdDTO dto) {

    }
}




