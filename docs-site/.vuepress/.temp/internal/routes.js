export const redirects = JSON.parse("{}")

export const routes = Object.fromEntries([
  ["/", { loader: () => import(/* webpackChunkName: "index.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/README.md"), meta: {"title":"开发文档"} }],
  ["/api/permissions.html", { loader: () => import(/* webpackChunkName: "api_permissions.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/api/permissions.md"), meta: {"title":"API 权限清单"} }],
  ["/bot/overview.html", { loader: () => import(/* webpackChunkName: "bot_overview.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/bot/overview.md"), meta: {"title":"QQ 机器人系统"} }],
  ["/backend/architecture.html", { loader: () => import(/* webpackChunkName: "backend_architecture.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/backend/architecture.md"), meta: {"title":"后端架构概览"} }],
  ["/backend/auth.html", { loader: () => import(/* webpackChunkName: "backend_auth.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/backend/auth.md"), meta: {"title":"认证与权限"} }],
  ["/backend/config.html", { loader: () => import(/* webpackChunkName: "backend_config.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/backend/config.md"), meta: {"title":"配置说明"} }],
  ["/backend/conventions.html", { loader: () => import(/* webpackChunkName: "backend_conventions.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/backend/conventions.md"), meta: {"title":"后端开发规范"} }],
  ["/backend/flyway.html", { loader: () => import(/* webpackChunkName: "backend_flyway.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/backend/flyway.md"), meta: {"title":"数据库迁移（Flyway）"} }],
  ["/backend/structure.html", { loader: () => import(/* webpackChunkName: "backend_structure.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/backend/structure.md"), meta: {"title":"后端项目结构"} }],
  ["/frontend/api.html", { loader: () => import(/* webpackChunkName: "frontend_api.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/frontend/api.md"), meta: {"title":"API 请求层"} }],
  ["/frontend/architecture.html", { loader: () => import(/* webpackChunkName: "frontend_architecture.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/frontend/architecture.md"), meta: {"title":"前端架构概览"} }],
  ["/frontend/components.html", { loader: () => import(/* webpackChunkName: "frontend_components.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/frontend/components.md"), meta: {"title":"组件库"} }],
  ["/frontend/router.html", { loader: () => import(/* webpackChunkName: "frontend_router.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/frontend/router.md"), meta: {"title":"路由与权限"} }],
  ["/frontend/structure.html", { loader: () => import(/* webpackChunkName: "frontend_structure.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/frontend/structure.md"), meta: {"title":"前端项目结构"} }],
  ["/frontend/uniapp.html", { loader: () => import(/* webpackChunkName: "frontend_uniapp.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/frontend/uniapp.md"), meta: {"title":"UniApp 微信小程序"} }],
  ["/guide/faq.html", { loader: () => import(/* webpackChunkName: "guide_faq.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/guide/faq.md"), meta: {"title":"常见问题"} }],
  ["/guide/getting-started.html", { loader: () => import(/* webpackChunkName: "guide_getting-started.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/guide/getting-started.md"), meta: {"title":"快速开始"} }],
  ["/guide/introduction.html", { loader: () => import(/* webpackChunkName: "guide_introduction.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/guide/introduction.md"), meta: {"title":"项目介绍"} }],
  ["/guide/project-structure.html", { loader: () => import(/* webpackChunkName: "guide_project-structure.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/guide/project-structure.md"), meta: {"title":"项目结构详解"} }],
  ["/lms/overview.html", { loader: () => import(/* webpackChunkName: "lms_overview.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/lms/overview.md"), meta: {"title":"实验室管理系统（LMS）"} }],
  ["/database/tables.html", { loader: () => import(/* webpackChunkName: "database_tables.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/database/tables.md"), meta: {"title":"数据库表结构"} }],
  ["/deploy/cicd.html", { loader: () => import(/* webpackChunkName: "deploy_cicd.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/deploy/cicd.md"), meta: {"title":"CI/CD 持续集成与部署"} }],
  ["/deploy/docker.html", { loader: () => import(/* webpackChunkName: "deploy_docker.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/deploy/docker.md"), meta: {"title":"Docker 部署"} }],
  ["/deploy/env.html", { loader: () => import(/* webpackChunkName: "deploy_env.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/deploy/env.md"), meta: {"title":"环境变量"} }],
  ["/deploy/nginx.html", { loader: () => import(/* webpackChunkName: "deploy_nginx.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/deploy/nginx.md"), meta: {"title":"Nginx 反向代理"} }],
  ["/404.html", { loader: () => import(/* webpackChunkName: "404.html" */"/Users/ariven/1-jmiopenatom/openatom-system/docs-site/.vuepress/.temp/pages/404.html.vue"), meta: {"title":""} }],
]);

if (import.meta.webpackHot) {
  import.meta.webpackHot.accept()
  __VUE_HMR_RUNTIME__.updateRoutes?.(routes)
  __VUE_HMR_RUNTIME__.updateRedirects?.(redirects)
}

if (import.meta.hot) {
  import.meta.hot.accept((m) => {
    __VUE_HMR_RUNTIME__.updateRoutes?.(m.routes)
    __VUE_HMR_RUNTIME__.updateRedirects?.(m.redirects)
  })
}
