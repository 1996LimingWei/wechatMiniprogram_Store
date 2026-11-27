import { http } from "@/utils/http";

/** 商家新订单通知配置 */
export interface NotifyConfig {
  enabled: number;
  mailEnabled: number;
  mailRecipients: string;
  smtpHost: string;
  smtpPort: number;
  smtpUsername: string;
  /** 后端返回掩码，回传表示不修改授权码 */
  smtpPassword: string;
  smtpPasswordConfigured: boolean;
  mailFrom: string;
  sslEnabled: number;
  updatedAt?: string;
}

export interface NotifyLog {
  id: number;
  orderId: number;
  orderSn: string;
  channel: string;
  receiver: string;
  success: number;
  message: string;
  createTime: string;
}

export const getNotifyConfig = () => {
  return http.get<NotifyConfig, undefined>("/admin-api/trade/notify-config");
};

export const saveNotifyConfig = (data: Partial<NotifyConfig>) => {
  return http.post<NotifyConfig, typeof data>(
    "/admin-api/trade/notify-config/save",
    { data }
  );
};

/** 使用已保存的配置发送测试邮件 */
export const sendTestNotifyMail = () => {
  return http.post<boolean, undefined>("/admin-api/trade/notify-config/test");
};

export const getNotifyLogs = (params: { pageNo?: number; pageSize?: number }) => {
  return http.get<{ list: NotifyLog[]; total: number }, typeof params>(
    "/admin-api/trade/notify-config/logs",
    { params }
  );
};
