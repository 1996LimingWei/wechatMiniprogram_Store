package com.shop.module.product.vo;

import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import lombok.Data;

import java.util.List;

@Data
public class ProductSaveReqVO {

    private ProductSpuDO spu;
    private List<ProductSkuDO> skus;
}
