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
                title: "订单管理"
            }
        },
        {
            path: "/trade/after-sale",
            name: "AfterSaleList",
            component: () => import("@/views/after-sale/index.vue"),
            meta: {
                title: "售后管理"
            }
        }
    ]
} satisfies RouteConfigsTable;
