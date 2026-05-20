# OpenAtom Bot Deployment

机器人随根目录 `docker-compose.yml` 一起部署，CI/CD 在 `main` 分支推送后会启动以下服务：

- `astrbot`：AstrBot 主服务，默认映射 `${ASTRBOT_PORT:-6185}:6185`
- `napcat`：NapCat QQ 适配器，默认映射 `${NAPCAT_PORT:-6099}:6099`

## 服务器使用

部署完成后访问 AstrBot 面板：

```text
http://服务器IP:6185
```

首次使用时在 AstrBot 面板里配置模型和 NapCat/aiocqhttp 平台，然后在 QQ 里使用：

```text
/oa help
```

OpenAtom 插件会随仓库发布到：

```text
astrbot/data/plugins/astrbot_plugin_openatom_api
```

插件默认通过 Docker 内网访问后端：

```text
http://backend:8921/api/v1
```

## QQ 绑定

用户在 Web 端个人中心生成绑定码，然后向机器人发送：

```text
/oa bind-qq 绑定码
```

绑定码 10 分钟有效，绑定成功后立即失效。

## 环境变量

可在服务器 `.env` 中覆盖：

```text
ASTRBOT_PORT=6185
NAPCAT_PORT=6099
NAPCAT_UID=1000
NAPCAT_GID=1000
```
