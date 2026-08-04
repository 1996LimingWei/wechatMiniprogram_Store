const Layout = () => import("@/layout/index.vue");

export default {
    path: "/product",
    name: "Product",
    component: Layout,
    redirect: "/product/spu",
    meta: {
        icon: "ep/goods",
        title: "商品管理",
        rank: 1
    },
    children: [
        {
            path: "/product/spu",
            name: "ProductList",
            component: () => import("@/views/product/spu/index.vue"),
            meta: {
                title: "商品列表"
            }
        },
        {
            path: "/product/category",
            name: "ProductCategory",
            component: () => import("@/views/product/category/index.vue"),
            meta: {
                title: "分类管理"
            }
        },
        {
            path: "/product/spu-form",
            name: "ProductCreate",
            component: () => import("@/views/product/spu-form/index.vue"),
            meta: {
                title: "新增商品",
                showLink: false
            }
        },
        {
            path: "/product/spu-form/:id",
            name: "ProductEdit",
            component: () => import("@/views/product/spu-form/index.vue"),
            meta: {
                title: "编辑商品",
                showLink: false
            }
        }
    ]
} satisfies RouteConfigsTable;
