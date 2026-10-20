# 独立 Actions 部署

工作流：`.github/workflows/member-guide.yml`，名称 **OpenAtom Member Guide CI/CD**。

- 推送到 `main`，且修改 `member-guide/**` 或该 workflow：检查、构建并发布指南。
- Pull Request 修改上述路径：只检查与构建，不访问部署环境。
- 在 GitHub Actions 页面手动 Run workflow：选择 `main` 执行部署；其他分支仅构建。
- 主站 `deploy.yml` 已忽略这两个指南路径。包含主站其他文件的混合提交仍按主站原有规则执行。

构建不需要安装 npm 依赖。CI 生成全部步骤图、核对图文对应关系，测试发布器，再制作 SHA-256 清单。服务器验证完整文件集后，将独立 `current` 符号链接原子切换到新版本。整个过程不执行主站 `git reset`、Docker Compose 或 Nginx 重载。

## 复用配置

复用 GitHub `SERVER` Environment 中的 `SERVER_HOST`、`SERVER_USER`、`SERVER_PASSWORD`；`SERVER_PORT` 可选，默认 22。现有服务器需要 Python 3。若已配置 `SERVER_FINGERPRINT`，SSH 与上传步骤会用它核对主机。

可选 Environment Variables：

| 名称 | 默认值 / 用途 |
| --- | --- |
| `MEMBER_GUIDE_DOMAIN` | `guide.jmi-openatom.cn`，用于部署标识与发布链接 |
| `MEMBER_GUIDE_DEPLOY_PATH` | `/www/wwwroot/openatom-member-guide`，必须是指南专用绝对目录 |
| `MEMBER_GUIDE_HEALTHCHECK_URL` | 默认留空；域名配置好后，设为 `https://guide.jmi-openatom.cn/`，开启外网 HTTP 检查 |

域名变量只设置发布标识和 workflow 链接，不会替你修改 DNS 或创建宝塔站点。

## 首次上线：DNS 与宝塔

1. 添加 DNS 记录，主机名 `guide`，指向这台部署服务器的公网 IP。
2. 创建 `guide.jmi-openatom.cn` 的静态站点，把根目录设为：

   ```text
   /www/wwwroot/openatom-member-guide/current
   ```

   第一次 Actions 发布成功后会生成这个目录链接。不要将部署根目录设为现有主站或其他站点目录。

3. 在宝塔为该站点申请并启用 HTTPS。页面是纯静态文件，不需要反向代理到本地 4180，也不需要 Node 常驻进程。
4. 在站点配置中保留 `include mime.types` 或相应的 MIME 设置，确保 `.js` 以 JavaScript MIME 类型返回。可参考 `nginx/guide.jmi-openatom.cn.conf` 的 `root` 与缓存规则；已在宝塔创建站点时，不要额外安装重复的 server block。
5. 打开站点确认页面与步骤图片可读，再设置 `MEMBER_GUIDE_HEALTHCHECK_URL`。之后的发布会检查公网 HTTP；未配置时，Actions 只证明静态文件已经发布到服务器，不证明公网域名可访问。

## 文件布局

```text
/www/wwwroot/openatom-member-guide/
  .member-guide-site.json
  current -> releases/<commit>-<run-id>-<attempt>
  releases/
    <commit>-<run-id>-<attempt>/
      index.html
      release.json
      assets/
      ...
  incoming/
    <commit>-<run-id>-<attempt>/
      manifest.json
      publish.py
```

公网根目录只包含构建结果，不包含发布器、源码维护脚本或校验清单。旧版本继续保留，校验失败时当前站点不切换；旧工作流晚于新版本完成时，会拒绝用旧版本覆盖新版本。

`release.json` 包含提交 SHA 与工作流序号，可用于核对当前发布版本。重新运行 workflow 会使用新的 attempt ID。

## 本地验证

```sh
npm run build
python3 -m unittest discover -s deploy/tests -v
```

发布器测试覆盖文件校验失败保留旧站、额外文件、路径越界、符号链接、旧版本覆盖保护、站点目录与域名冲突，以及正常原子切换。仅使用临时目录，不会操作服务器。
