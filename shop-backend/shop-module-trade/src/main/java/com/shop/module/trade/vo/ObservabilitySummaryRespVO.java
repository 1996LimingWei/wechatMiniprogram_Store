package com.shop.module.trade.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ObservabilitySummaryRespVO {

    private List<HealthItem> health = new ArrayList<>();
    private List<MetricItem> metrics = new ArrayList<>();
    private List<AlertItem> alerts = new ArrayList<>();
    private List<JobItem> jobs = new ArrayList<>();
    private OrderTrace orderTrace;

    @Data
    public static class HealthItem {
        private String component;
        private String status;
        private String message;
        private String lastCheckTime;
    }

    @Data
    public static class MetricItem {
        private String code;
        private String label;
        private Integer value;
        private String unit;
        private String level;
    }

    @Data
    public static class AlertItem {
        private Long id;
        private String alertType;
        private String level;
        private String title;
        private String message;
        private String businessRef;
        private Integer currentValue;
        private Integer thresholdValue;
        private Integer status;
        private String firstTriggerTime;
        private String lastTriggerTime;
        private Integer triggerCount;
    }

    @Data
    public static class JobItem {
        private String jobName;
        private String lastStatus;
        private String lastMessage;
        private Integer processedCount;
        private Integer successCount;
        private Integer failureCount;
        private Integer consecutiveFailures;
        private String lastRunTime;
    }

    @Data
    public static class OrderTrace {
        private Long orderId;
        private String orderSn;
        private Long userId;
        private Integer orderStatus;
        private Integer payStatus;
        private String paySn;
        private String afterSaleSn;
        private String providerRefundNo;
        private List<TraceItem> tradeLogs = new ArrayList<>();
        private List<TraceItem> payLogs = new ArrayList<>();
        private List<TraceItem> auditLogs = new ArrayList<>();
    }

    @Data
    public static class TraceItem {
        private String time;
        private String type;
        private String ref;
        private String status;
        private String message;
    }
}
