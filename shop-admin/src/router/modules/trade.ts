const Layout = () => import("@/layout/index.vue");

export default {
    path: "/trade",
    name: "Trade",
    component: Layout,
    redirect: "/trade/order",
    meta: {
        icon: "ep/list",
        title: "交易管理",
        rank: 2
    },
    children: [
        {
            path: "/trade/order",
            name: "OrderList",
            component: () => import("@/views/order/index.vue"),
            meta: {
                title: "订单管理",
                permissions: ["trade:manage", "trade:order-read"]
            }
        },
        {
            path: "/trade/after-sale",
            name: "AfterSaleList",
            component: () => import("@/views/after-sale/index.vue"),
            meta: {
                title: "售后管理",
                permissions: ["trade:manage", "trade:after-sale-read"]
            }
        },
        {
            path: "/trade/payment",
            name: "PaymentExceptionWorkbench",
            component: () => import("@/views/payment/index.vue"),
            meta: {
                title: "支付异常",
                permissions: ["trade:manage", "trade:payment-read"]
            }
        },
        {
            path: "/trade/refund",
            name: "RefundExceptionWorkbench",
            component: () => import("@/views/refund/index.vue"),
            meta: {
                title: "退款异常",
                permissions: ["trade:manage", "trade:refund-read", "trade:after-sale-read"]
            }
        },
        {
            path: "/trade/reconcile",
            name: "ReconcileWorkbench",
            component: () => import("@/views/reconcile/index.vue"),
            meta: {
                title: "日终对账",
                permissions: ["trade:manage", "trade:reconcile-read"]
            }
        },
        {
            path: "/trade/observability",
            name: "TradeObservability",
            component: () => import("@/views/observability/index.vue"),
            meta: {
                title: "运行监控",
                permissions: ["trade:manage", "trade:observability-read"]
            }
        }
    ]
} satisfies RouteConfigsTable;
