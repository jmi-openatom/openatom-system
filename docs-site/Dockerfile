# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app

# 启用 pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# 允许 pnpm 构建脚本（esbuild 等 native 依赖需要）
RUN pnpm config set dangerouslyAllowAllBuilds true

# 先复制依赖文件，利用 Docker 层缓存
COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile 2>/dev/null || pnpm install

# 复制源码并构建
COPY . .
RUN pnpm run build

# ---- Serve stage ----
FROM nginx:alpine AS serve
LABEL maintainer="OpenAtom System"
LABEL description="OpenAtom System 开发文档站"

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=build /app/.vuepress/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
