package edu.jmi.openatom.server.openatomsystem.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Markdown-to-HTML renderer for email bodies. Supports headings,
 * blockquotes, unordered/ordered lists, paragraphs, line breaks, bold,
 * inline code and emphasis. Everything else is HTML-escaped.
 */
public final class EmailMarkdown {
  private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.*)$");
  private static final Pattern QUOTE = Pattern.compile("^>\\s?(.*)$");
  private static final Pattern UL_ITEM = Pattern.compile("^[-*]\\s+(.*)$");
  private static final Pattern OL_ITEM = Pattern.compile("^\\d+[.、]\\s+(.*)$");
  private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
  private static final Pattern CODE = Pattern.compile("`([^`]+)`");
  private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)");

  private EmailMarkdown() {}

  public static String render(String source) {
    if (source == null) {
      return "";
    }
    StringBuilder html = new StringBuilder();
    boolean inUl = false;
    boolean inOl = false;
    boolean inP = false;
    for (String raw : source.split("\\R", -1)) {
      String line = raw.stripTrailing();
      String text = line.stripLeading();
      if (text.isBlank()) {
        closeAll(html, inUl, inOl, inP);
        inUl = false;
        inOl = false;
        inP = false;
        continue;
      }
      Matcher heading = HEADING.matcher(text);
      if (heading.matches()) {
        closeAll(html, inUl, inOl, inP);
        inUl = false;
        inOl = false;
        inP = false;
        int level = heading.group(1).length() + 1;
        html.append("<h").append(level).append('>')
            .append(inline(heading.group(2)))
            .append("</h").append(level).append('>');
        continue;
      }
      Matcher quote = QUOTE.matcher(text);
      if (quote.matches()) {
        closeAll(html, inUl, inOl, inP);
        inUl = false;
        inOl = false;
        inP = false;
        html.append("<blockquote>").append(inline(quote.group(1))).append("</blockquote>");
        continue;
      }
      Matcher ulItem = UL_ITEM.matcher(text);
      if (ulItem.matches()) {
        if (!inUl) {
          closeParagraph(html, inP);
          inP = false;
          html.append("<ul>");
          inUl = true;
          inOl = false;
        }
        html.append("<li>").append(inline(ulItem.group(1))).append("</li>");
        continue;
      }
      Matcher olItem = OL_ITEM.matcher(text);
      if (olItem.matches()) {
        if (!inOl) {
          closeParagraph(html, inP);
          inP = false;
          html.append("<ol>");
          inOl = true;
          inUl = false;
        }
        html.append("<li>").append(inline(olItem.group(1))).append("</li>");
        continue;
      }
      if (inUl || inOl) {
        closeLists(html, inUl, inOl);
        inUl = false;
        inOl = false;
      }
      if (!inP) {
        html.append("<p>");
        inP = true;
      } else {
        html.append("<br/>");
      }
      html.append(inline(text));
    }
    closeAll(html, inUl, inOl, inP);
    return html.toString();
  }

  private static String inline(String source) {
    String escaped =
        source.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    escaped = BOLD.matcher(escaped).replaceAll("<strong>$1</strong>");
    escaped = CODE.matcher(escaped).replaceAll("<code>$1</code>");
    escaped = ITALIC.matcher(escaped).replaceAll("<em>$1</em>");
    return escaped;
  }

  private static void closeAll(StringBuilder html, boolean inUl, boolean inOl, boolean inP) {
    closeLists(html, inUl, inOl);
    closeParagraph(html, inP);
  }

  private static void closeLists(StringBuilder html, boolean inUl, boolean inOl) {
    if (inUl) {
      html.append("</ul>");
    }
    if (inOl) {
      html.append("</ol>");
    }
  }

  private static void closeParagraph(StringBuilder html, boolean inP) {
    if (inP) {
      html.append("</p>");
    }
  }
}
