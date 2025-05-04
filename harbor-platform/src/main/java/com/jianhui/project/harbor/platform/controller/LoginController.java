package com.jianhui.project.harbor.platform.controller;

import com.jianhui.project.harbor.platform.pojo.req.LoginReq;
import com.jianhui.project.harbor.platform.pojo.req.RegisterReq;
import com.jianhui.project.harbor.platform.pojo.resp.CaptchaResp;
import com.jianhui.project.harbor.platform.pojo.resp.LoginResp;
import com.jianhui.project.harbor.platform.result.Result;
import com.jianhui.project.harbor.platform.result.Results;
import com.jianhui.project.harbor.platform.service.UserService;
import com.pig4cloud.captcha.ArithmeticCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

import static com.jianhui.project.harbor.platform.constant.RedisConstant.CHECK_CODE_EXPIRE_TIME;
import static com.jianhui.project.harbor.platform.constant.RedisConstant.CHECK_CODE_PREFIX;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final RedisTemplate redisTemplate;

    @GetMapping("/captcha")
    public Result<CaptchaResp> checkCode() {
        // 生成验证码
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String base64 = captcha.toBase64();
        String code = captcha.text();
        // 生成验证码key
        String checkCodeKey = UUID.randomUUID().toString();
        // 将验证码存入redis
        redisTemplate.opsForValue().set(CHECK_CODE_PREFIX + checkCodeKey, code, Duration.ofSeconds(CHECK_CODE_EXPIRE_TIME));
        return Results.success(CaptchaResp.builder()
                .captchaPic(base64)
                .captchaKey(checkCodeKey)
                .build());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq loginReq) {
        LoginResp vo = userService.login(loginReq);
        return Results.success(vo);
    }

    @PutMapping("/refreshToken")
    @Operation(summary = "刷新token", description = "用refreshtoken换取新的token")
    public Result refreshToken(@RequestHeader("refreshToken") String refreshToken) {
        LoginResp vo = userService.refreshToken(refreshToken);
        return Results.success(vo);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册")
    public Result register(@Valid @RequestBody RegisterReq dto) {
        userService.register(dto);
        return Results.success();
    }
}
