package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.dto.request.LoginReqDTO;
import com.jianhui.project.harbor.platform.dto.request.ModifyPwdDTO;
import com.jianhui.project.harbor.platform.dto.request.RegisterReqDTO;
import com.jianhui.project.harbor.platform.dto.response.CaptchaRespDTO;
import com.jianhui.project.harbor.platform.dto.response.LoginRespDTO;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.UserService;
import com.pig4cloud.captcha.ArithmeticCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

import static com.jianhui.project.harbor.platform.constant.RedisKey.CHECK_CODE_EXPIRE_TIME;
import static com.jianhui.project.harbor.platform.constant.RedisKey.CHECK_CODE_PREFIX;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/captcha")
    public Result<CaptchaRespDTO> checkCode() {
        // 生成验证码
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String base64 = captcha.toBase64();
        String code = captcha.text();
        // 生成验证码key
        String checkCodeKey = UUID.randomUUID().toString();
        // 将验证码存入redis
        redisTemplate.opsForValue().set(CHECK_CODE_PREFIX + checkCodeKey, code, Duration.ofSeconds(CHECK_CODE_EXPIRE_TIME));
        log.info("验证码key: {}, 验证码: {}", checkCodeKey, code);
        return Results.success(CaptchaRespDTO.builder()
                .captchaPic(base64)
                .captchaKey(checkCodeKey)
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录")
    public Result<LoginRespDTO> login(@Valid @RequestBody LoginReqDTO loginReqDTO) {
        LoginRespDTO vo = userService.login(loginReqDTO);
        return Results.success(vo);
    }

    @PutMapping("/refreshToken")
    @Operation(summary = "刷新token", description = "用refreshtoken换取新的token")
    public Result refreshToken(@RequestHeader("refreshToken") String refreshToken) {
        LoginRespDTO vo = userService.refreshToken(refreshToken);
        return Results.success(vo);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册")
    public Result register(@Valid @RequestBody RegisterReqDTO dto) {
        userService.register(dto);
        return Results.success();
    }

    @PutMapping("/modifyPwd")
    @Operation(summary = "修改密码", description = "修改用户密码")
    public Result modifyPassword(@Valid @RequestBody ModifyPwdDTO dto) {
        userService.modifyPassword(dto);
        return Results.success();
    }
}
