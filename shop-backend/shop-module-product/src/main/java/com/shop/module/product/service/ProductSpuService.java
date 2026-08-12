package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ErrorCode;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSpuService {

    private final ProductSpuMapper productSpuMapper;
    private final ProductAdminService productAdminService;

    public PageResult<ProductSpuDO> getSpuPage(PageParam pageParam, Long categoryId) {
        LambdaQueryWrapper<ProductSpuDO> wrapper = new LambdaQueryWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getStatus, 1)
                .eq(categoryId != null, ProductSpuDO::getCategoryId, categoryId)
                .orderByDesc(ProductSpuDO::getSort);
        return productSpuMapper.selectPage(pageParam, wrapper);
    }

    public ProductSpuDO getSpuDetail(Long id) {
        ProductSpuDO spu = productSpuMapper.selectById(id);
        if (spu == null) {
            throw new ServerException(ErrorCode.PRODUCT_NOT_EXISTS);
        }
        return spu;
    }

    public PageResult<ProductSpuDO> getAdminSpuPage(PageParam pageParam, String name, Long categoryId, Integer status) {
        LambdaQueryWrapper<ProductSpuDO> wrapper = new LambdaQueryWrapper<ProductSpuDO>()
                .like(name != null && !name.isBlank(), ProductSpuDO::getName, name)
                .eq(categoryId != null, ProductSpuDO::getCategoryId, categoryId)
                .eq(status != null, ProductSpuDO::getStatus, status)
                .orderByDesc(ProductSpuDO::getCreateTime);
        return productSpuMapper.selectPage(pageParam, wrapper);
    }

    public void createSpu(ProductSpuDO spu) {
        createSpu(spu, 0L);
    }

    public void createSpu(ProductSpuDO spu, Long adminId) {
        productAdminService.saveProduct(spu, null, adminId, "商品创建初始化库存");
    }

    public void updateSpu(ProductSpuDO spu) {
        productAdminService.updateSpu(spu);
    }

    public void deleteSpu(Long id) {
        productAdminService.deleteProduct(id);
    }
}
