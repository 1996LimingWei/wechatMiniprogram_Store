const Layout = () => import("@/layout/index.vue");

export default {
    path: "/marketing",
    name: "Marketing",
    component: Layout,
    redirect: "/marketing/coupon",
    meta: {
        icon: "ep/discount",
        title: "营销管理",
        rank: 4,
        permissions: ["marketing:manage", "marketing:read"]
    },
    children: [
        {
            path: "/marketing/coupon",
            name: "MarketingCoupon",
            component: () => import("@/views/marketing/coupon/index.vue"),
            meta: {
                title: "优惠券管理",
                permissions: ["marketing:manage", "marketing:read"]
            }
        },
        {
            path: "/marketing/promotion",
            name: "MarketingPromotion",
            component: () => import("@/views/marketing/promotion/index.vue"),
            meta: {
                title: "满减活动",
                permissions: ["marketing:manage", "marketing:read"]
            }
        },
        {
            path: "/marketing/shipping",
            name: "MarketingShipping",
            component: () => import("@/views/marketing/shipping/index.vue"),
            meta: {
                title: "包邮规则",
                permissions: ["marketing:manage", "marketing:read"]
            }
        }
    ]
} satisfies RouteConfigsTable;
