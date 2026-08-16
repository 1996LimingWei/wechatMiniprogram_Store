const Layout = () => import("@/layout/index.vue");

export default {
  path: "/system",
  name: "System",
  component: Layout,
  redirect: "/system/admin-user",
  meta: {
    icon: "ep/setting",
    title: "系统管理",
    rank: 5,
    permissions: ["system:admin-user", "system:role", "system:audit"]
  },
  children: [
    {
      path: "/system/admin-user",
      name: "SystemAdminUser",
      component: () => import("@/views/system/admin-user/index.vue"),
      meta: { title: "管理员账号", permissions: ["system:admin-user"] }
    },
    {
      path: "/system/role",
      name: "SystemRole",
      component: () => import("@/views/system/role/index.vue"),
      meta: { title: "角色权限", permissions: ["system:role"] }
    },
    {
      path: "/system/audit",
      name: "SystemAudit",
      component: () => import("@/views/system/audit/index.vue"),
      meta: { title: "审计日志", permissions: ["system:audit"] }
    }
  ]
} satisfies RouteConfigsTable;
