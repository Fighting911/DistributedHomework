package com.seckill.user.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(400, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleBusiness(RuntimeException e) {
        String msg = e.getMessage();
        if (msg == null) return Result.error(500, "服务器内部错误");
        if (msg.contains("用户名已存在"))      return Result.error(409, msg);
        if (msg.contains("用户名或密码错误"))   return Result.error(401, msg);
        if (msg.contains("账号已被禁用"))       return Result.error(403, msg);
        if (msg.contains("用户不存在"))         return Result.error(404, msg);
        return Result.error(500, "服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleGeneral(Exception e) {
        return Result.error(500, "服务器内部错误");
    }
}

