package com.seckill.user.service;

import com.seckill.user.dto.LoginRequest;
import com.seckill.user.dto.RegisterRequest;
import com.seckill.user.entity.User;

import java.util.Map;

public interface UserService {
    Map<String, Object> register(RegisterRequest request);
    Map<String, Object> login(LoginRequest request);
    User getUserInfo(Long userId);
}
