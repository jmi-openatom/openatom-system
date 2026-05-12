# Build stage
FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_APP_VERSION
ENV VITE_APP_VERSION=$VITE_APP_VERSION

# Pin pnpm to a Node 20 compatible release that matches the lockfile generation.
RUN corepack enable && corepack prepare pnpm@9.0.0 --activate

# Copy lockfile and package.json for dependency fetching
COPY pnpm-lock.yaml package.json ./

# Install dependencies (utilizing layer caching)
RUN pnpm install --frozen-lockfile || pnpm install

# Copy source code and build
COPY . .
RUN pnpm run build

# Run stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
