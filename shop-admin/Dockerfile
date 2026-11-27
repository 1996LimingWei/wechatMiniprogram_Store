# 构建型：容器内执行 pnpm install + vite build，内存峰值超过 2G。
# 仅适用于内存 4G 以上的开发机或 CI，禁止在 2 核 1.9G 的生产服务器上使用
# （会吃干内存转入 swap 抖动，导致 SSH 与宝塔面板全部无响应，只能强制重启）。
# 生产环境请用 Dockerfile.prebuilt：开发机先 pnpm build，容器内只 COPY dist。
FROM node:22.17.0-alpine as build-stage

WORKDIR /app
RUN corepack enable
RUN corepack prepare pnpm@10.18.3 --activate

RUN npm config set registry https://registry.npmmirror.com

COPY .npmrc package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM nginx:stable-alpine as production-stage

COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY default.conf.template /etc/nginx/templates/default.conf.template

ENV BACKEND_URL=http://shop-backend:8085

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
