package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.service.TradeLogisticsService;
import com.shop.module.trade.service.TradeOrderOperationService;
import com.shop.module.trade.service.TradeOrderQueryService;
import com.shop.module.trade.service.TradeOrderService;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import com.shop.module.trade.vo.BatchShipReqVO;
import com.shop.module.trade.vo.BatchShipResultVO;
import com.shop.module.trade.vo.DeliveryNoteRespVO;
import com.shop.module.trade.vo.OrderRemarkReqVO;
import com.shop.module.trade.vo.PickingListRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/trade/order")
@RequiredArgsConstructor
public class AdminTradeOrderController {

    private final TradeOrderService tradeOrderService;
    private final TradeOrderQueryService tradeOrderQueryService;
    private final TradeLogisticsService tradeLogisticsService;
    private final TradeOrderOperationService tradeOrderOperationService;

    @GetMapping("/list")
    public CommonResult<PageResult<Map<String, Object>>> list(@RequestParam Map<String, Object> params,
                                                              @RequestParam(value = "page", required = false) Integer page,
                                                              @RequestParam(value = "size", required = false) Integer size) {
        Map<String, Object> request = TradeRequestUtils.parse(null, params);
        int finalPage = page != null ? page : TradeRequestUtils.getInt(request, "page", 1);
        int finalSize = size != null ? size : TradeRequestUtils.getInt(request, "size", 10);
        return CommonResult.success(tradeOrderQueryService.getAdminOrderPage(finalPage, finalSize, request));
    }

    @GetMapping("/detail")
    public CommonResult<Map<String, Object>> detail(@RequestParam Map<String, Object> params,
                                                    @RequestParam(value = "orderId", required = false) Long orderId,
                                                    @RequestParam(value = "id", required = false) Long id) {
        Map<String, Object> request = TradeRequestUtils.parse(null, params);
        Long finalOrderId = orderId != null ? orderId : (id != null ? id : TradeRequestUtils.getLong(request, "orderId", 0L));
        return CommonResult.success(tradeOrderService.getAdminOrderDetail(finalOrderId));
    }

    @PostMapping("/ship")
    public CommonResult<Map<String, Object>> ship(@RequestBody(required = false) String rawBody,
                                                  @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeLogisticsService.adminShip(TradeSecurityUtils.getRequiredUserId(), orderId, request));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "orderSn", required = false) String orderSn,
                                         @RequestParam(value = "userId", required = false) Long userId,
                                         @RequestParam(value = "status", required = false) Integer status,
                                         @RequestParam(value = "payStatus", required = false) Integer payStatus,
                                         @RequestParam(value = "mobile", required = false) String mobile,
                                         @RequestParam(value = "createTimeStart", required = false) String createTimeStart,
                                         @RequestParam(value = "createTimeEnd", required = false) String createTimeEnd) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("orderSn", orderSn);
        request.put("userId", userId);
        request.put("status", status);
        request.put("payStatus", payStatus);
        request.put("mobile", mobile);
        request.put("createTimeStart", createTimeStart);
        request.put("createTimeEnd", createTimeEnd);
        byte[] content = tradeOrderOperationService.exportOrders(TradeSecurityUtils.getRequiredUserId(), request);
        return csvResponse("订单导出.csv", content);
    }

    @GetMapping("/batch-ship/template")
    public ResponseEntity<byte[]> batchShipTemplate() {
        return csvResponse("批量发货模板.csv", tradeOrderOperationService.batchShipTemplate());
    }

    @PostMapping("/batch-ship/import")
    public CommonResult<BatchShipResultVO> batchShip(@RequestBody BatchShipReqVO request) {
        return CommonResult.success(tradeOrderOperationService.batchShip(TradeSecurityUtils.getRequiredUserId(), request));
    }

    @PostMapping("/remark")
    public CommonResult<Boolean> updateRemark(@RequestBody OrderRemarkReqVO request) {
        tradeOrderOperationService.updateAdminRemark(TradeSecurityUtils.getRequiredUserId(), request);
        return CommonResult.success(true);
    }

    @GetMapping("/delivery-note")
    public CommonResult<DeliveryNoteRespVO> deliveryNote(@RequestParam("orderId") Long orderId) {
        return CommonResult.success(tradeOrderOperationService.getDeliveryNote(orderId));
    }

    @GetMapping("/picking-list")
    public CommonResult<PickingListRespVO> pickingList(@RequestParam("orderIds") String orderIds) {
        List<Long> ids = Arrays.stream(orderIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .toList();
        return CommonResult.success(tradeOrderOperationService.getPickingList(ids));
    }

    private ResponseEntity<byte[]> csvResponse(String filename, byte[] content) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }
}
