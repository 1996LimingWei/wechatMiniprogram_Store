import { http } from "@/utils/http";

export interface HealthItem {
  component: string;
  status: string;
  message: string;
  lastCheckTime: string;
}

export interface MetricItem {
  code: string;
  label: string;
  value: number;
  unit: string;
  level: string;
}

export interface AlertItem {
  id: number;
  alertType: string;
  level: string;
  title: string;
  message: string;
  businessRef: string;
  currentValue: number;
  thresholdValue: number;
  status: number;
  firstTriggerTime: string;
  lastTriggerTime: string;
  triggerCount: number;
}

export interface JobItem {
  jobName: string;
  lastStatus: string;
  lastMessage: string;
  processedCount: number;
  successCount: number;
  failureCount: number;
  consecutiveFailures: number;
  lastRunTime: string;
}

export interface TraceItem {
  time: string;
  type: string;
  ref: string;
  status: string;
  message: string;
}

export interface OrderTrace {
  orderId: number;
  orderSn: string;
  userId: number;
  orderStatus: number;
  payStatus: number;
  paySn: string;
  afterSaleSn: string;
  providerRefundNo: string;
  tradeLogs: TraceItem[];
  payLogs: TraceItem[];
  auditLogs: TraceItem[];
}

export interface ObservabilitySummary {
  health: HealthItem[];
  metrics: MetricItem[];
  alerts: AlertItem[];
  jobs: JobItem[];
  orderTrace?: OrderTrace | null;
}

export const getObservabilitySummary = (params: Record<string, unknown>) =>
  http.get<ObservabilitySummary, Record<string, unknown>>(
    "/admin-api/trade/observability/summary",
    { params }
  );

export const getOrderTrace = (orderSn: string) =>
  http.get<OrderTrace, { orderSn: string }>(
    "/admin-api/trade/observability/order-trace",
    { params: { orderSn } }
  );
