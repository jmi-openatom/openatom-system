package edu.jmi.openatom.mail.service;

/**
 * Branded email shell (main-site visual style) used for automatically
 * generated broadcast mails. Table-based with inline styles for client
 * compatibility; the club logo is served from the mail web origin.
 */
public final class BroadcastEmailTemplate {
  private static final String LOGO_URL = "https://mail.jmi-openatom.cn/logo.png";
  private static final String FONT =
      "-apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif";

  private BroadcastEmailTemplate() {}

  public static String wrap(String contentHtml, String subject) {
    String safeSubject = escape(subject);
    String safeContent = contentHtml == null ? "" : contentHtml;
    return ""
        + "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"/>"
        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>"
        + "<style>"
        + ".b-content{font-family:" + FONT + ";color:#1d1d1f;font-size:15px;line-height:1.8;word-break:break-word}"
        + ".b-content p{margin:0 0 14px}"
        + ".b-content h1,.b-content h2,.b-content h3{color:#1d1d1f;line-height:1.35;margin:20px 0 10px}"
        + ".b-content h2{font-size:20px}.b-content h3{font-size:17px}"
        + ".b-content ul,.b-content ol{margin:0 0 14px;padding-left:22px}"
        + ".b-content li{margin:4px 0}"
        + ".b-content a{color:#1677ff;text-decoration:underline}"
        + ".b-content blockquote{margin:14px 0;padding-left:14px;border-left:3px solid #d2d2d7;color:#6e6e73}"
        + ".b-content img{max-width:100%;border-radius:8px}"
        + "</style></head>"
        + "<body style=\"margin:0;padding:0;background:#f5f5f7;\">"
        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:#f5f5f7;\"><tr><td align=\"center\" style=\"padding:28px 14px;\">"
        + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:640px;\">"
        + "<tr><td style=\"padding:0 0 18px;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>"
        + "<td style=\"vertical-align:middle;padding-right:10px;\"><img src=\"" + LOGO_URL
        + "\" width=\"40\" height=\"40\" alt=\"开放原子开源社团\" style=\"display:block;border-radius:10px;\"/></td>"
        + "<td style=\"vertical-align:middle;line-height:1.2;\"><div style=\"font-size:15px;font-weight:650;color:#1d1d1f;\">开放原子开源社团</div>"
        + "<div style=\"font-size:12px;color:#6e6e73;letter-spacing:0.05em;\">" + safeSubject + "</div></td>"
        + "</tr></table></td></tr>"
        + "<tr><td style=\"padding:0 0 18px;\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-radius:14px;overflow:hidden;\"><tr><td align=\"center\" style=\"background:#0a4b78;padding:22px 24px;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.02em;\">"
        + safeSubject + "</td></tr></table></td></tr>"
        + "<tr><td style=\"background:#ffffff;border:1px solid #e8e8ed;border-radius:16px;padding:28px 28px 12px;\">"
        + "<div class=\"b-content\">" + safeContent + "</div></td></tr>"
        + "<tr><td align=\"center\" style=\"padding:22px 10px 6px;color:#86868b;font-family:" + FONT
        + ";font-size:12px;line-height:1.8;\">江苏海事职业技术学院 · 开放原子开源社团<br/>本邮件由社团邮箱系统自动发送</td></tr>"
        + "</table></td></tr></table></body></html>";
  }

  private static String escape(String value) {
    if (value == null) return "";
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
