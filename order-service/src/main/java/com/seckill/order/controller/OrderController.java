package com.seckill.order.controller;

import com.seckill.order.common.Result;
import com.seckill.order.service.impl.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderServiceImpl orderService;

    /** GET /api/order/{orderNo} */
    @GetMapping("/{orderNo}")
    public Result<?> getOrder(@PathVariable String orderNo) {
        var order = orderService.getByOrderNo(orderNo);
        return order != null ? Result.success(order) : Result.error(404, "订单不存在");
    }

    /** GET /api/order/user/{userId} */
    @GetMapping("/user/{userId}")
    public Result<?> getUserOrders(@PathVariable Long userId) {
        return Result.success(orderService.getByUserId(userId));
    }

    /** POST /api/order/{orderNo}/pay */
    @PostMapping("/{orderNo}/pay")
    public Result<?> payOrder(@PathVariable String orderNo) {
        try {
            orderService.payOrder(orderNo);
            return Result.success("支付成功");
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
