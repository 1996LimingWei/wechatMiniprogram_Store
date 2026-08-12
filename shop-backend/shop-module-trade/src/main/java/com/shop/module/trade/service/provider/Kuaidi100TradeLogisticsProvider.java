package com.shop.module.trade.service.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Kuaidi100TradeLogisticsProvider implements TradeLogisticsProvider {

    private static final URI QUERY_URI = URI.create("https://poll.kuaidi100.com/poll/query.do");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final String customer;
    private final String key;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public Kuaidi100TradeLogisticsProvider(
            ObjectMapper objectMapper,
            @Value("${trade.logistics.kuaidi100.customer:}") String customer,
            @Value("${trade.logistics.kuaidi100.key:}") String key) {
        this.objectMapper = objectMapper;
        this.customer = customer == null ? "" : customer.trim();
        this.key = key == null ? "" : key.trim();
    }

    @Override
    public String type() {
        return "kuaidi100";
    }

    @Override
    public List<LogisticsTrace> query(LogisticsQuery query) {
        validateConfiguration();
        if (query.logisticsCode() == null || query.logisticsCode().isBlank()
                || query.logisticsNo() == null || !query.logisticsNo().matches("[A-Za-z0-9-]{6,32}")) {
            throw new ServerException(400, "物流公司编码或物流单号不正确");
        }
        try {
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("com", query.logisticsCode());
            param.put("num", query.logisticsNo());
            if (query.receiverMobile() != null && !query.receiverMobile().isBlank()) {
                param.put("phone", query.receiverMobile());
            }
            param.put("resultv2", "1");
            param.put("show", "0");
            param.put("order", "desc");
            param.put("lang", "zh");
            String paramJson = objectMapper.writeValueAsString(param);
            String body = form("customer", customer)
                    + "&" + form("sign", md5Upper(paramJson + key + customer))
                    + "&" + form("signType", "MD5")
                    + "&" + form("param", paramJson);
            HttpRequest request = HttpRequest.newBuilder(QUERY_URI)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServerException(502, "物流供应商请求失败");
            }
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
            if (!"200".equals(String.valueOf(result.getOrDefault("status", "")))) {
                throw new ServerException(502,
                        "物流供应商查询失败: " + String.valueOf(result.getOrDefault("message", "未知错误")));
            }
            Object rawData = result.get("data");
            if (!(rawData instanceof List<?> data)) return List.of();
            List<LogisticsTrace> traces = new ArrayList<>();
            for (Object item : data) {
                if (!(item instanceof Map<?, ?> value)) continue;
                Object rawText = value.get("context");
                Object rawTime = value.get("ftime") == null ? value.get("time") : value.get("ftime");
                String text = rawText == null ? "" : String.valueOf(rawText).trim();
                String time = rawTime == null ? "" : String.valueOf(rawTime).trim();
                if (text.isEmpty()) continue;
                traces.add(new LogisticsTrace(parseTime(time), text));
            }
            return List.copyOf(traces);
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(502, "物流供应商暂时不可用");
        }
    }

    public void validateConfiguration() {
        if (customer.isBlank() || key.isBlank()) {
            throw new ServerException(503, "快递100企业账号尚未配置");
        }
    }

    private String form(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String md5Upper(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    private LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value, TIME_FORMATTER);
        } catch (Exception exception) {
            return null;
        }
    }
}
