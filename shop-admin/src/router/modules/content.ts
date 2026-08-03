const Layout = () => import("@/layout/index.vue");

export default {
    path: "/content",
    name: "Content",
    component: Layout,
    redirect: "/content/banner",
    meta: {
        icon: "ep/picture",
        title: "内容管理",
        rank: 3
    },
    children: [
        {
            path: "/content/banner",
            name: "ContentBanner",
            component: () => import("@/views/content/banner/index.vue"),
            meta: {
                title: "Banner 管理"
            }
        },
        {
            path: "/content/channel",
            name: "ContentChannel",
            component: () => import("@/views/content/channel/index.vue"),
            meta: {
                title: "频道管理"
            }
        },
        {
            path: "/content/brand",
            name: "ContentBrand",
            component: () => import("@/views/content/brand/index.vue"),
            meta: {
                title: "品牌管理"
            }
        },
        {
            path: "/content/topic",
            name: "ContentTopic",
            component: () => import("@/views/content/topic/index.vue"),
            meta: {
                title: "专题管理"
            }
        }
    ]
} satisfies RouteConfigsTable;
