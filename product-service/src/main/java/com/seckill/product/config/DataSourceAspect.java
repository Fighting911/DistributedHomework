package com.seckill.product.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class DataSourceAspect {

    @Around("@annotation(com.seckill.product.config.ReadOnly)")
    public Object readOnly(ProceedingJoinPoint pjp) throws Throwable {
        DataSourceContextHolder.setSlave();
        try {
            return pjp.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
