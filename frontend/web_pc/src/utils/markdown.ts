import MarkdownIt from 'markdown-it'
import type Token from 'markdown-it/lib/token.mjs'

export interface MarkdownHeading {
  id: string
  text: string
  level: number
}

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false,
})

function headingText(token?: Token): string {
  if (!token) return ''
  if (!token.children?.length) return markdownToPlainText(token.content)

  return token.children
    .filter((child) => ['text', 'code_inline', 'image'].includes(child.type))
    .map((child) => child.content)
    .join('')
    .trim()
}

function slugifyHeading(text: string): string {
  return (
    text
      .normalize('NFKC')
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '-')
      .replace(/[^\p{Letter}\p{Number}_-]/gu, '')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '') || 'section'
  )
}

function uniqueHeadingId(text: string, slugs: Set<string>, prefix = ''): string {
  const base = slugifyHeading(text)
  let id = base
  let count = 1
  while (slugs.has(id)) id = `${base}-${++count}`
  slugs.add(id)
  return prefix ? `${prefix}-${id}` : id
}

function assignHeadingIds(tokens: Token[], prefix: string): MarkdownHeading[] {
  const slugs = new Set<string>()
  const headings: MarkdownHeading[] = []

  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return
    const text = headingText(tokens[index + 1])
    if (!text) return
    const id = uniqueHeadingId(text, slugs, prefix)
    token.attrSet('id', id)
    headings.push({
      id,
      text,
      level: Number(token.tag.slice(1)) || 1,
    })
  })

  return headings
}

export function extractMarkdownHeadings(value = '', prefix = ''): MarkdownHeading[] {
  return assignHeadingIds(markdown.parse(String(value || ''), {}), prefix)
}

/** Use the same IDs for the rendered document and its table of contents. */
export function withMarkdownHeadingIds(md: MarkdownIt, prefix = ''): MarkdownIt {
  md.core.ruler.after('inline', 'openatom_heading_ids', (state) => {
    const headings = assignHeadingIds(state.tokens, prefix)
    if (!prefix) return
    const localLinks = new Map(
      headings.map((heading) => [heading.id.slice(prefix.length + 1), heading.id]),
    )
    for (const token of state.tokens) {
      for (const child of token.children || []) {
        if (child.type !== 'link_open') continue
        const href = child.attrGet('href')
        if (!href?.startsWith('#')) continue
        try {
          const target = localLinks.get(decodeURIComponent(href.slice(1)))
          if (target) child.attrSet('href', `#${encodeURIComponent(target)}`)
        } catch {
          // Preserve malformed links as authored rather than failing the whole document.
        }
      }
    }
  })
  return md
}

export function markdownToPlainText(value = ''): string {
  return String(value || '')
    .replace(/```[^\n]*\n?([\s\S]*?)```/g, '$1')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/^\s*[-*+]\s+/gm, '')
    .replace(/^\s*\d+\.\s+/gm, '')
    .replace(/[*_~]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}
