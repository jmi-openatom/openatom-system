import DOMPurify from 'dompurify'

/** Strips scripts, event handlers and other active content from email HTML. */
export function sanitizeEmailHtml(html: string): string {
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['iframe', 'object', 'embed', 'form', 'input', 'button'],
    ADD_ATTR: ['target'],
  })
}
