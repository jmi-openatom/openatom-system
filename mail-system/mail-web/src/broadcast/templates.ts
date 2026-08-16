// Email broadcast templates matching the main-site visual language:
// Apple-minimal white cards on #F5F5F7, deep blue #0A4B78, link blue
// #1677FF, text #1D1D1F / secondary #6E6E73, plus the club logo.
// Table-based with inline styles for maximum client compatibility.

export interface BroadcastTemplate {
  key: string
  name: string
  description: string
  wrap(contentHtml: string, subject: string, logoUrl: string): string
}

const FONT =
  "-apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif"

function shell(
  contentHtml: string,
  subject: string,
  options: { accent: string; banner?: string; footerNote?: string },
) {
  return (
    `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"/>` +
    `<meta name="viewport" content="width=device-width, initial-scale=1.0"/>` +
    `<meta name="x-apple-disable-message-reformatting"/>` +
    `<title></title><style>` +
    `.b-content { font-family: ${FONT}; color: #1d1d1f; font-size: 15px; line-height: 1.8; word-break: break-word; }` +
    `.b-content p { margin: 0 0 14px; }` +
    `.b-content h1, .b-content h2, .b-content h3 { color: #1d1d1f; line-height: 1.35; margin: 22px 0 10px; }` +
    `.b-content h2 { font-size: 20px; } .b-content h3 { font-size: 17px; }` +
    `.b-content ul, .b-content ol { margin: 0 0 14px; padding-left: 22px; }` +
    `.b-content li { margin: 4px 0; }` +
    `.b-content a { color: #1677ff; text-decoration: underline; }` +
    `.b-content blockquote { margin: 14px 0; padding-left: 14px; border-left: 3px solid #d2d2d7; color: #6e6e73; }` +
    `.b-content pre { margin: 14px 0; padding: 12px 14px; border-radius: 10px; background: #f5f5f7; font-size: 13px; overflow: auto; }` +
    `.b-content code { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.9em; }` +
    `</style></head>` +
    `<body style="margin:0;padding:0;background:#f5f5f7;">` +
    `<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#f5f5f7;"><tr><td align="center" style="padding:28px 14px;">` +
    `<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width:640px;">` +
    // header with logo
    `<tr><td style="padding:0 0 18px;">` +
    `<table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%"><tr>` +
    `<td align="left" style="vertical-align:middle;">` +
    `<table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>` +
    `<td style="vertical-align:middle;padding-right:10px;"><img src="__LOGO__" width="40" height="40" alt="开放原子开源社团" style="display:block;border-radius:10px;"/></td>` +
    `<td style="vertical-align:middle;line-height:1.2;"><div style="font-size:15px;font-weight:650;color:#1d1d1f;">开放原子开源社团</div><div style="font-size:12px;color:#6e6e73;letter-spacing:0.05em;">${subject}</div></td>` +
    `</tr></table></td></tr></table>` +
    `</td></tr>` +
    (options.banner
      ? `<tr><td style="padding:0 0 18px;"><table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="border-radius:14px;overflow:hidden;"><tr><td align="center" style="background:${options.accent};padding:26px 24px;color:#ffffff;font-size:21px;font-weight:700;letter-spacing:0.02em;">${options.banner}</td></tr></table></td></tr>`
      : '') +
    // card with content
    `<tr><td style="background:#ffffff;border:1px solid #e8e8ed;border-radius:16px;padding:30px 30px 14px;">` +
    `<div class="b-content">${contentHtml}</div>` +
    `</td></tr>` +
    // footer
    `<tr><td align="center" style="padding:22px 10px 6px;color:#86868b;font-family:${FONT};font-size:12px;line-height:1.8;">` +
    `江苏海事职业技术学院 · 开放原子开源社团<br/>` +
    `${options.footerNote ?? '本邮件由社团邮箱系统发送'}` +
    `</td></tr>` +
    `</table></td></tr></table></body></html>`
  )
}

function withLogo(html: string, logoUrl: string): string {
  return html.replace('__LOGO__', logoUrl)
}

export const broadcastTemplates: BroadcastTemplate[] = [
  {
    key: 'default',
    name: '简约通知',
    description: '白色卡片 + 极简排版，适合日常通知',
    wrap: (contentHtml, subject, logoUrl) =>
      withLogo(
        shell(contentHtml, subject, { accent: '#0A4B78', footerNote: '社团日常通知 · 请勿直接回复本邮件' }),
        logoUrl,
      ),
  },
  {
    key: 'activity',
    name: '活动邀请',
    description: '顶部品牌色横幅，适合活动报名邀请',
    wrap: (contentHtml, subject, logoUrl) =>
      withLogo(
        shell(contentHtml, subject, {
          accent: '#0A4B78',
          banner: '社团活动 · 邀请函',
          footerNote: '活动报名请以正文链接为准',
        }),
        logoUrl,
      ),
  },
  {
    key: 'recruitment',
    name: '招新宣传',
    description: '深蓝横幅 + 醒目号召，适合纳新季',
    wrap: (contentHtml, subject, logoUrl) =>
      withLogo(
        shell(contentHtml, subject, {
          accent: '#0A4B78',
          banner: '开放原子开源社团 · 招新进行中',
          footerNote: '加入我们，一起玩转开源',
        }),
        logoUrl,
      ),
  },
  {
    key: 'announcement',
    name: '重要公告',
    description: '强调色横幅，适合重要通知',
    wrap: (contentHtml, subject, logoUrl) =>
      withLogo(
        shell(contentHtml, subject, {
          accent: '#1677FF',
          banner: '重要公告',
          footerNote: '本邮件为系统公告，请及时查收',
        }),
        logoUrl,
      ),
  },
]

export function defaultTemplate(): BroadcastTemplate {
  return broadcastTemplates[0]
}
