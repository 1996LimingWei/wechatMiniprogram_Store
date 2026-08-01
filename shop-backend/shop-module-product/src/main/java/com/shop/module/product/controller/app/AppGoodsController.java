package com.shop.module.product.controller.app;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.service.ProductCatalogProvider;
import com.shop.module.product.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppGoodsController {
    private final ProductCatalogService catalogService;
    private ProductCatalogProvider provider() { return catalogService.current(); }
    @RequestMapping("/app-api/catalog/index") public CommonResult<Map<String,Object>> catalogIndex(){return CommonResult.success(provider().catalogIndex());}
    @RequestMapping("/app-api/catalog/current") public CommonResult<Map<String,Object>> catalogCurrent(@RequestParam(defaultValue="1") Long id){return CommonResult.success(Map.of("currentCategory",provider().catalog(id)));}
    @RequestMapping("/app-api/catalog/{id}") public CommonResult<Map<String,Object>> catalog(@PathVariable Long id){return CommonResult.success(Map.of("currentCategory",provider().catalog(id)));}
    @RequestMapping("/app-api/goods/category") public CommonResult<Map<String,Object>> category(@RequestParam(defaultValue="1") Long id){return CommonResult.success(provider().goodsCategory(id));}
    @RequestMapping("/app-api/goods/count") public CommonResult<Map<String,Object>> count(){return CommonResult.success(provider().count());}
    @RequestMapping("/app-api/goods/list") public CommonResult<Map<String,Object>> list(@RequestParam(defaultValue="0") Long categoryId,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="0") int isHot,@RequestParam(defaultValue="0") int isNew,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){return CommonResult.success(provider().list(categoryId,keyword,isHot,isNew,page,size));}
    @RequestMapping("/app-api/goods/detail") public CommonResult<Map<String,Object>> detail(@RequestParam Long id){return CommonResult.success(provider().detail(id));}
    @RequestMapping("/app-api/goods/related") public CommonResult<Map<String,Object>> related(@RequestParam Long id){return CommonResult.success(provider().related(id));}
}
