package com.seckill.order.mapper;

import com.seckill.order.entity.Order;
import com.seckill.order.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    void insertOrder(Order order);
    Order selectByOrderNo(String orderNo);
    List<Order> selectByUserId(Long userId);
    int updateStatus(@Param("orderNo") String orderNo, @Param("status") int status);

    void insertOutbox(OutboxMessage msg);
    List<OutboxMessage> selectPendingOutbox();
    int markOutboxSent(Long id);
    int incrementOutboxRetry(Long id);
}
