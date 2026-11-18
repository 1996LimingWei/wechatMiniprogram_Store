package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.MemberAddressService;
import com.shop.module.trade.service.RegionCatalog;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppAddressController {

    private final MemberAddressService memberAddressService;

    @RequestMapping("/app-api/address/list")
    public CommonResult<List<Map<String, Object>>> list() {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        return CommonResult.success(memberAddressService.getAddressList(userId));
    }

    @RequestMapping("/app-api/address/detail")
    public CommonResult<Map<String, Object>> detail(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params,
                                                     @RequestParam(value = "id", required = false) Long paramId) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long id = paramId != null ? paramId : TradeRequestUtils.getLong(request, "id", 0L);
        return CommonResult.success(memberAddressService.getAddressDetail(userId, id));
    }

    @RequestMapping("/app-api/address/save")
    public CommonResult<Boolean> save(@RequestBody(required = false) String rawBody,
                                      @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        memberAddressService.saveAddress(userId, TradeRequestUtils.parse(rawBody, params));
        return CommonResult.success(true);
    }

    @RequestMapping("/app-api/address/delete")
    public CommonResult<Boolean> delete(@RequestBody(required = false) String rawBody,
                                        @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        memberAddressService.deleteAddress(userId, TradeRequestUtils.getLong(request, "id", 0L));
        return CommonResult.success(true);
    }

    @RequestMapping("/app-api/region/list")
    public CommonResult<List<Map<String, Object>>> regionList(@RequestBody(required = false) String rawBody,
                                                               @RequestParam Map<String, Object> params,
                                                               @RequestParam(value = "parentId", required = false) Long parentId) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long id = parentId != null ? parentId : TradeRequestUtils.getLong(request, "parentId", 1L);
        return CommonResult.success(RegionCatalog.children(id));
    }

}
