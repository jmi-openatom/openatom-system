FROM node:22-alpine AS build
WORKDIR /app
COPY mail-system/mail-web/package.json mail-system/mail-web/pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile
COPY mail-system/mail-web/ ./
RUN pnpm build

FROM nginx:1.27-alpine
COPY mail-system/mail-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
