import { reactive } from "vue";
import type { FormRules } from "element-plus";
import { adminPasswordFits } from "@/utils/auth";

/** 登录校验与后端 BCrypt 字节上限一致，支持合法的长初始密码和历史密码。 */
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
