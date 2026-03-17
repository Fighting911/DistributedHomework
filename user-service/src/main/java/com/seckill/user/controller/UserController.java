package com.seckill.user.controller;

import com.seckill.user.common.Result;
import com.seckill.user.dto.LoginRequest;
import com.seckill.user.dto.RegisterRequest;
import com.seckill.user.entity.User;
import com.seckill.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * POST /api/user/register
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> data = userService.register(request);
        return Result.success("注册成功", data);
    }

    /**
     * 用户登录
     * POST /api/user/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.success("登录成功", data);
    }

    /**
     * 查询用户信息
     * GET /api/user/info/{userId}
     */
    @GetMapping("/info/{userId}")
    public Result<User> getUserInfo(@PathVariable Long userId) {
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    /**
     * 健康检查 + 实例标识（JMeter压测验证负载均衡用）
     * GET /api/user/ping
     */
    @GetMapping("/ping")
    public Result<String> ping(
            @org.springframework.beans.factory.annotation.Value("${instance.id:unknown}") String instanceId) {
        return Result.success("pong from " + instanceId);
    }
}
