# 规章制度页面视觉验证

- source visual truth:
  - `/var/folders/9r/pmbkyp5s08dcmd8smh6k65yw0000gn/T/codex-clipboard-b6c61fe6-1aac-47fc-8248-b9964621c544.png`
  - `/var/folders/9r/pmbkyp5s08dcmd8smh6k65yw0000gn/T/codex-clipboard-e8b93653-cc0c-4243-b0dc-0cac51c18c6c.png`
- implementation screenshots:
  - `/var/folders/9r/pmbkyp5s08dcmd8smh6k65yw0000gn/T/openatom-regulations-qa/regulations-mermaid.png`
  - `/var/folders/9r/pmbkyp5s08dcmd8smh6k65yw0000gn/T/openatom-regulations-qa/regulations-toc.png`
- viewport: `1280 x 720`
- state: 浅色模式；Mermaid 组织架构图；规章长文第四章激活

## Full-view comparison evidence

已分别打开参考图和本地实现截图。实现中 Mermaid 节点中文文字已显示，目录按 Markdown 二级、三级标题分层，并在长文滚动时保持侧栏可见。

## Focused region comparison evidence

已分别检查 Mermaid 节点区域与目录区域。浏览器安全策略阻止创建包含两张本地截图的数据页，因此无法完成同一画布中的并排视觉比较。

## Findings

- Mermaid 图形包含 1 个 SVG、57 个文本分段；中文标签颜色可见且不透明。
- 目录点击会更新 URL 锚点并滚动到对应标题。
- 当前章节使用蓝色文字、浅蓝背景和左侧标记，三级标题按参考图缩进。
- 目录侧栏在长文滚动时固定于页头下方，内部内容可滚动。
- 未发现独立检查可见的 P0、P1 或 P2 问题。

## Patches made

- Mermaid 流程图改用原生 SVG 标签并显式设置中文字体和文字颜色。
- Markdown 标题生成稳定且可去重的锚点。
- 制度详情页自动生成章节目录，支持点击跳转、滚动高亮和链接锚点。
- 修正长目录吸顶、内部滚动和移动端回落布局。

## Final result

final result: blocked

Blocker: required side-by-side comparison input could not be created because the browser rejected the local data URL under its security policy.
