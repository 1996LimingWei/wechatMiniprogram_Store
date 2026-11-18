import { reactive } from "vue";
import type { FormRules } from "element-plus";
import { adminPasswordFits } from "@/utils/auth";

/** 登录校验 */
const loginRules = reactive<FormRules>({
  password: [
    {
      required: true,
      message: "请输入密码",
      trigger: "blur"
    },
    {
      validator: (_rule, value: string) =>
        !value || adminPasswordFits(value) || new Error("密码不能超过 72 字节"),
      trigger: "blur"
    }
  ]
});

export { loginRules };
