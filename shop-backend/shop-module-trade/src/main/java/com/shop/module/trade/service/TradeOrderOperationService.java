package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import com.shop.module.trade.vo.BatchShipReqVO;
import com.shop.module.trade.vo.BatchShipResultVO;
import com.shop.module.trade.vo.DeliveryNoteRespVO;
import com.shop.module.trade.vo.OrderRemarkReqVO;
import com.shop.module.trade.vo.PickingListRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TradeOrderOperationService {

    private static final int MAX_EXPORT_ROWS = 5000;
    private static final int MAX_BATCH_ROWS = 200;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> LOGISTICS_CODES = Set.of(
            "shunfeng", "zhongtong", "yuantong", "yunda", "jtexpress", "shentong", "jd", "ems");
    private static final Set<String> EXPORT_FIELDS = Set.of(
            "orderSn", "userId", "status", "payStatus", "mobile", "createTimeStart", "createTimeEnd");

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    private final TradeLogisticsService tradeLogisticsService;
    private final TradeOrderLogService tradeOrderLogService;
    private final JdbcTemplate jdbcTemplate;

    public byte[] exportOrders(Long adminId, Map<String, Object> request) {
        boolean includeSensitive = canExportSensitive(adminId);
        QueryParts query = buildExportQuery(request);
        List<ExportRow> rows = jdbcTemplate.query("""
                SELECT o.order_sn, o.user_id, o.status, o.pay_status, o.consignee, o.mobile, o.full_region, o.address,
                       o.goods_price, o.freight_price, o.coupon_price, o.actual_price, o.create_time,
                       i.goods_name, i.sku_id, i.spec_name, i.count, i.price, i.total_price,
                       l.logistics_company, l.logistics_no
                """ + query.fromWhere() + """
                 ORDER BY o.create_time DESC, o.id DESC, i.id ASC
                 LIMIT ?
                """, (rs, index) -> new ExportRow(
                rs.getString("order_sn"),
                rs.getLong("user_id"),
                rs.getInt("status"),
                rs.getInt("pay_status"),
                rs.getString("consignee"),
                rs.getString("mobile"),
                rs.getString("full_region"),
                rs.getString("address"),
                rs.getInt("goods_price"),
                rs.getInt("freight_price"),
                rs.getInt("coupon_price"),
                rs.getInt("actual_price"),
                toTime(rs.getTimestamp("create_time")),
                rs.getString("goods_name"),
                rs.getLong("sku_id"),
                rs.getString("spec_name"),
                rs.getInt("count"),
                rs.getInt("price"),
                rs.getInt("total_price"),
                rs.getString("logistics_company"),
                rs.getString("logistics_no")
        ), appendLimit(query.args()));

        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvLine(csv, List.of("订单号", "用户ID", "商品", "SKU ID", "规格", "数量", "单价", "小计",
                "收货人", "手机号", "地址", "商品金额", "运费", "优惠", "实付金额", "支付状态", "发货状态",
                "物流公司", "物流单号", "下单时间"));
        for (ExportRow row : rows) {
            appendCsvLine(csv, List.of(
                    row.orderSn(), String.valueOf(row.userId()), row.goodsName(), String.valueOf(row.skuId()),
                    blankToDefault(row.specName(), "默认规格"), String.valueOf(row.count()),
                    TradeMoneyUtils.formatYuan(row.price()), TradeMoneyUtils.formatYuan(row.totalPrice()),
                    row.consignee(), includeSensitive ? row.mobile() : maskMobile(row.mobile()),
                    includeSensitive ? fullAddress(row) : maskAddress(fullAddress(row)),
                    TradeMoneyUtils.formatYuan(row.goodsPrice()), TradeMoneyUtils.formatYuan(row.freightPrice()),
                    TradeMoneyUtils.formatYuan(row.couponPrice()), TradeMoneyUtils.formatYuan(row.actualPrice()),
                    payStatusText(row.payStatus()), orderStatusText(row.status()),
                    blankToDefault(row.logisticsCompany(), ""), blankToDefault(row.logisticsNo(), ""), row.createTime()));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] batchShipTemplate() {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvLine(csv, List.of("订单号", "物流公司", "物流编码", "物流单号", "内部备注"));
        appendCsvLine(csv, List.of("202608160001", "顺丰速运", "shunfeng", "SF1234567890", "请放门卫"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchShipResultVO batchShip(Long adminId, BatchShipReqVO request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new ServerException(400, "请上传批量发货 CSV 内容");
        }
        List<List<String>> csvRows = parseCsv(request.getContent());
        if (csvRows.size() <= 1) {
            throw new ServerException(400, "批量发货文件没有数据行");
        }
        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        BatchShipResultVO result = new BatchShipResultVO();
        result.setDryRun(dryRun);
        int limit = Math.min(csvRows.size() - 1, MAX_BATCH_ROWS);
        for (int i = 1; i <= limit; i++) {
            List<String> row = csvRows.get(i);
            if (row.stream().allMatch(value -> value == null || value.isBlank())) continue;
            BatchShipResultVO.Row item = new BatchShipResultVO.Row();
            item.setRowNo(i + 1);
            item.setOrderSn(cell(row, 0));
            try {
                processBatchShipRow(adminId, row, dryRun);
                item.setSuccess(true);
                item.setMessage(dryRun ? "校验通过" : "发货成功");
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception exception) {
                item.setSuccess(false);
                item.setMessage(exception.getMessage());
                result.setFailedCount(result.getFailedCount() + 1);
            }
            result.getRows().add(item);
            result.setTotalCount(result.getTotalCount() + 1);
        }
        if (csvRows.size() - 1 > MAX_BATCH_ROWS) {
            throw new ServerException(400, "单次最多导入 " + MAX_BATCH_ROWS + " 行发货数据");
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAdminRemark(Long adminId, OrderRemarkReqVO request) {
        if (request == null || request.getOrderId() == null || request.getOrderId() <= 0) {
            throw new ServerException(400, "订单不存在");
        }
        String remark = request.getRemark() == null ? "" : request.getRemark().trim();
        if (remark.length() > 200) {
            throw new ServerException(400, "内部备注最多 200 字");
        }
        TradeOrderDO order = getOrder(request.getOrderId());
        tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, request.getOrderId())
                .set(TradeOrderDO::getAdminRemark, remark));
        order.setAdminRemark(remark);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_ADMIN, adminId,
                "UPDATE_ADMIN_REMARK", order.getStatus(), order.getStatus(), "更新客服内部备注");
    }

    public DeliveryNoteRespVO getDeliveryNote(Long orderId) {
        TradeOrderDO order = getOrder(orderId);
        DeliveryNoteRespVO note = new DeliveryNoteRespVO();
        note.setOrderId(order.getId());
        note.setOrderSn(order.getOrderSn());
        note.setConsignee(order.getConsignee());
        note.setMobile(order.getMobile());
        note.setFullAddress((blankToDefault(order.getFullRegion(), "") + blankToDefault(order.getAddress(), "")).trim());
        note.setGoodsPrice(TradeMoneyUtils.formatYuan(order.getGoodsPrice()));
        note.setFreightPrice(TradeMoneyUtils.formatYuan(order.getFreightPrice()));
        note.setCouponPrice(TradeMoneyUtils.formatYuan(order.getCouponPrice()));
        note.setActualPrice(TradeMoneyUtils.formatYuan(order.getActualPrice()));
        note.setAdminRemark(order.getAdminRemark());
        TradeOrderLogisticsDO logistics = getLogistics(order.getId());
        if (logistics != null) {
            note.setLogisticsCompany(logistics.getLogisticsCompany());
            note.setLogisticsNo(logistics.getLogisticsNo());
            note.setDeliveryTime(formatTime(logistics.getDeliveryTime()));
        }
        note.setItems(getItems(orderId).stream().map(this::toDeliveryItem).toList());
        return note;
    }

    public PickingListRespVO getPickingList(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new ServerException(400, "请选择要打印的订单");
        }
        List<Long> distinctOrderIds = orderIds.stream().filter(id -> id != null && id > 0).distinct().limit(100).toList();
        if (distinctOrderIds.isEmpty()) {
            throw new ServerException(400, "请选择要打印的订单");
        }
        List<TradeOrderDO> orders = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrderDO>()
                .in(TradeOrderDO::getId, distinctOrderIds)
                .orderByDesc(TradeOrderDO::getCreateTime));
        Map<Long, String> orderSnMap = new LinkedHashMap<>();
        for (TradeOrderDO order : orders) {
            orderSnMap.put(order.getId(), order.getOrderSn());
        }
        if (orderSnMap.isEmpty()) {
            PickingListRespVO empty = new PickingListRespVO();
            empty.setOrderCount(0);
            empty.setItemCount(0);
            return empty;
        }
        List<TradeOrderItemDO> items = tradeOrderItemMapper.selectList(new LambdaQueryWrapper<TradeOrderItemDO>()
                .in(TradeOrderItemDO::getOrderId, orderSnMap.keySet())
                .orderByAsc(TradeOrderItemDO::getSkuId));
        Map<Long, PickingListRespVO.Item> bySku = new LinkedHashMap<>();
        int itemCount = 0;
        for (TradeOrderItemDO item : items) {
            itemCount += item.getCount() == null ? 0 : item.getCount();
            PickingListRespVO.Item aggregate = bySku.computeIfAbsent(item.getSkuId(), ignored -> {
                PickingListRespVO.Item created = new PickingListRespVO.Item();
                created.setSpuId(item.getSpuId());
                created.setSkuId(item.getSkuId());
                created.setGoodsName(item.getGoodsName());
                created.setSpecName(item.getSpecName());
                created.setCount(0);
                return created;
            });
            aggregate.setCount(aggregate.getCount() + (item.getCount() == null ? 0 : item.getCount()));
            String orderSn = orderSnMap.get(item.getOrderId());
            if (orderSn != null && !aggregate.getOrderSns().contains(orderSn)) {
                aggregate.getOrderSns().add(orderSn);
            }
        }
        PickingListRespVO result = new PickingListRespVO();
        result.setOrderCount(orderSnMap.size());
        result.setItemCount(itemCount);
        result.setItems(new ArrayList<>(bySku.values()));
        return result;
    }

    private void processBatchShipRow(Long adminId, List<String> row, boolean dryRun) {
        String orderSn = cell(row, 0);
        String company = cell(row, 1);
        String code = cell(row, 2);
        String logisticsNo = cell(row, 3);
        String remark = cell(row, 4);
        if (orderSn.isBlank()) throw new ServerException(400, "订单号不能为空");
        TradeOrderDO order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getOrderSn, orderSn)
                .last("LIMIT 1"));
        if (order == null) throw new ServerException(400, "订单不存在");
        if (order.getStatus() == null || order.getStatus() != 1
                || !Integer.valueOf(TradeOrderPayStatus.PAID).equals(order.getPayStatus())) {
            throw new ServerException(400, "当前订单不是待发货状态");
        }
        if (company.length() < 2 || company.length() > 64) throw new ServerException(400, "物流公司长度应为 2 至 64 个字符");
        if (!LOGISTICS_CODES.contains(code)) throw new ServerException(400, "物流编码不支持");
        if (!logisticsNo.matches("[A-Za-z0-9-]{6,32}")) throw new ServerException(400, "物流单号仅支持 6 至 32 位字母、数字或连字符");
        if (!dryRun) {
            tradeLogisticsService.adminShip(adminId, order.getId(), Map.of(
                    "logisticsCompany", company, "logisticsCode", code, "logisticsNo", logisticsNo));
            if (!remark.isBlank()) {
                OrderRemarkReqVO remarkReq = new OrderRemarkReqVO();
                remarkReq.setOrderId(order.getId());
                remarkReq.setRemark(remark);
                updateAdminRemark(adminId, remarkReq);
            }
        }
    }

    private QueryParts buildExportQuery(Map<String, Object> request) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
                 FROM trade_order o
                 JOIN trade_order_item i ON i.order_id = o.id AND i.deleted = b'0'
                 LEFT JOIN trade_order_logistics l ON l.order_id = o.id AND l.deleted = b'0'
                WHERE o.deleted = b'0'
                """);
        for (String key : request == null ? Set.<String>of() : request.keySet()) {
            if (!EXPORT_FIELDS.contains(key)) continue;
            Object value = request.get(key);
            if (value == null || String.valueOf(value).isBlank() || "all".equals(String.valueOf(value))) continue;
            switch (key) {
                case "orderSn" -> { where.append(" AND o.order_sn = ?"); args.add(String.valueOf(value).trim()); }
                case "userId" -> { where.append(" AND o.user_id = ?"); args.add(Long.parseLong(String.valueOf(value))); }
                case "status" -> { where.append(" AND o.status = ?"); args.add(Integer.parseInt(String.valueOf(value))); }
                case "payStatus" -> { where.append(" AND o.pay_status = ?"); args.add(Integer.parseInt(String.valueOf(value))); }
                case "mobile" -> { where.append(" AND o.mobile LIKE ?"); args.add(String.valueOf(value).trim() + "%"); }
                case "createTimeStart" -> { where.append(" AND o.create_time >= ?"); args.add(LocalDateTime.parse(String.valueOf(value), TIME_FORMATTER)); }
                case "createTimeEnd" -> { where.append(" AND o.create_time < ?"); args.add(LocalDateTime.parse(String.valueOf(value), TIME_FORMATTER)); }
                default -> { }
            }
        }
        return new QueryParts(where.toString(), args);
    }

    private boolean canExportSensitive(Long adminId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM sys_admin_user_role ur
                  JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = b'0'
                  LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
                  LEFT JOIN sys_permission p ON p.id = rp.permission_id AND p.status = 1 AND p.deleted = b'0'
                 WHERE ur.admin_user_id = ?
                   AND (r.code = 'SUPER_ADMIN' OR p.code = 'trade:order-export-sensitive')
                """, Integer.class, adminId);
        return count != null && count > 0;
    }

    private TradeOrderDO getOrder(Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) throw new ServerException(1404, "订单不存在");
        return order;
    }

    private List<TradeOrderItemDO> getItems(Long orderId) {
        return tradeOrderItemMapper.selectList(new LambdaQueryWrapper<TradeOrderItemDO>()
                .eq(TradeOrderItemDO::getOrderId, orderId)
                .orderByAsc(TradeOrderItemDO::getId));
    }

    private TradeOrderLogisticsDO getLogistics(Long orderId) {
        return tradeOrderLogisticsMapper.selectOne(new LambdaQueryWrapper<TradeOrderLogisticsDO>()
                .eq(TradeOrderLogisticsDO::getOrderId, orderId)
                .orderByDesc(TradeOrderLogisticsDO::getUpdateTime)
                .last("LIMIT 1"));
    }

    private DeliveryNoteRespVO.Item toDeliveryItem(TradeOrderItemDO item) {
        DeliveryNoteRespVO.Item result = new DeliveryNoteRespVO.Item();
        result.setSkuId(item.getSkuId());
        result.setGoodsName(item.getGoodsName());
        result.setSpecName(item.getSpecName());
        result.setRetailPrice(TradeMoneyUtils.formatYuan(item.getPrice()));
        result.setCount(item.getCount());
        result.setTotalPrice(TradeMoneyUtils.formatYuan(item.getTotalPrice()));
        return result;
    }

    private Object[] appendLimit(List<Object> args) {
        List<Object> values = new ArrayList<>(args);
        values.add(MAX_EXPORT_ROWS);
        return values.toArray();
    }

    private void appendCsvLine(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) csv.append(',');
            String value = values.get(i) == null ? "" : values.get(i);
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = content.startsWith("\uFEFF") ? 1 : 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                current.add(cell.toString().trim()); cell.setLength(0);
            } else if (ch == '\n') {
                current.add(cell.toString().trim()); cell.setLength(0);
                rows.add(current); current = new ArrayList<>();
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }
        current.add(cell.toString().trim());
        rows.add(current);
        return rows;
    }

    private String cell(List<String> row, int index) {
        return index < row.size() && row.get(index) != null ? row.get(index).trim() : "";
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) return "";
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private String maskAddress(String address) {
        if (address == null || address.isBlank()) return "";
        return address.length() <= 8 ? "已脱敏" : address.substring(0, 8) + "****";
    }

    private String fullAddress(ExportRow row) {
        return (blankToDefault(row.fullRegion(), "") + blankToDefault(row.address(), "")).trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    private String toTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(TIME_FORMATTER);
    }

    private String payStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case TradeOrderPayStatus.UNPAID -> "未支付";
            case TradeOrderPayStatus.PAID -> "已支付";
            case TradeOrderPayStatus.REFUNDED -> "已退款";
            default -> "未知";
        };
    }

    private String orderStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "退款中";
            default -> "未知";
        };
    }

    private record QueryParts(String fromWhere, List<Object> args) {
    }

    private record ExportRow(String orderSn, Long userId, Integer status, Integer payStatus,
                             String consignee, String mobile, String fullRegion, String address,
                             Integer goodsPrice, Integer freightPrice, Integer couponPrice, Integer actualPrice,
                             String createTime, String goodsName, Long skuId, String specName,
                             Integer count, Integer price, Integer totalPrice,
                             String logisticsCompany, String logisticsNo) {
    }
}
