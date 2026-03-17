package com.seckill.product.mapper;

import com.seckill.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ProductMapper {
    Product selectByIdWithStock(Long id);
    List<Product> selectAllWithStock();
}
