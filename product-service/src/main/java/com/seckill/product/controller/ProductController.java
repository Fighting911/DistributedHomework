package com.seckill.product.controller;

import com.seckill.product.common.Result;
import com.seckill.product.entity.Product;
import com.seckill.product.service.impl.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductServiceImpl productService;

    @Value("${instance.id:unknown}")
    private String instanceId;

    /**
     * 商品详情（Redis缓存，处理三大缓存问题）
     * GET /api/product/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                return Result.error(404, "商品不存在");
            }
            return Result.success(product);
        } catch (Exception e) {
            return Result.error(500, "服务器错误: " + e.getMessage());
        }
    }

    /**
     * 商品列表
     * GET /api/product/list
     */
    @GetMapping("/list")
    public Result<List<Product>> listProducts() {
        return Result.success(productService.listProducts());
    }

    /**
     * 健康检查 + 实例标识（JMeter压测时验证负载均衡用）
     * GET /api/product/ping
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong from " + instanceId);
    }
}
