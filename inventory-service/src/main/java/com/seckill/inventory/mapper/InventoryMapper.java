package com.seckill.inventory.mapper;

import com.seckill.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryMapper {

    Inventory selectByProductId(Long productId);

    int decreaseStock(@Param("productId") Long productId,
                      @Param("quantity") int quantity,
                      @Param("version") int version);

    int increaseStock(@Param("productId") Long productId,
                      @Param("quantity") int quantity);

    int countSeckillRecord(@Param("userId") Long userId,
                           @Param("productId") Long productId);

    void insertSeckillRecord(@Param("userId") Long userId,
                             @Param("productId") Long productId,
                             @Param("orderNo") String orderNo);
}
