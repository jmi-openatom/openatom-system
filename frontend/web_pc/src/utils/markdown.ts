import MarkdownIt from 'markdown-it'
import type Token from 'markdown-it/lib/token.mjs'

export interface MarkdownHeading {
  id: string
  text: string
  level: number
}

interface MarkdownRenderEnv {
  headingSlugs?: Map<string, number>
}

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false,
})

const safeUrlPattern = /^(https?:\/\/|\/(?!\/)|data:image\/(?:png|jpeg|jpg|gif|webp);base64,)/i

markdown.validateLink = (url: string): boolean => safeUrlPattern.test(url.trim())

const defaultLinkOpen =
  markdown.renderer.rules.link_open ||
  ((tokens, index, options, _env, self) => self.renderToken(tokens, index, options))

markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  const token = tokens[index]
  const targetIndex = token.attrIndex('target')
  const relIndex = token.attrIndex('rel')

  if (targetIndex < 0) token.attrPush(['target', '_blank'])
  else token.attrs![targetIndex][1] = '_blank'

  if (relIndex < 0) token.attrPush(['rel', 'noopener noreferrer'])
  else token.attrs![relIndex][1] = 'noopener noreferrer'

  return defaultLinkOpen(tokens, index, options, env, self)
}

const defaultImage =
  markdown.renderer.rules.image ||
  ((tokens, index, options, _env, self) => self.renderToken(tokens, index, options))

markdown.renderer.rules.image = (tokens, index, options, env, self) => {
  const token = tokens[index]
  if (token.attrIndex('loading') < 0) token.attrPush(['loading', 'lazy'])
  if (token.attrIndex('referrerpolicy') < 0) token.attrPush(['referrerpolicy', 'no-referrer'])
  return defaultImage(tokens, index, options, env, self)
}

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

function uniqueHeadingId(text: string, slugs: Map<string, number>): string {
  const base = slugifyHeading(text)
  const count = (slugs.get(base) || 0) + 1
  slugs.set(base, count)
  return count === 1 ? base : `${base}-${count}`
}

const defaultHeadingOpen =
  markdown.renderer.rules.heading_open ||
  ((tokens, index, options, _env, self) => self.renderToken(tokens, index, options))

markdown.renderer.rules.heading_open = (tokens, index, options, env, self) => {
  const renderEnv = env as MarkdownRenderEnv
  const slugs = renderEnv.headingSlugs || new Map<string, number>()
  renderEnv.headingSlugs = slugs
  tokens[index].attrSet('id', uniqueHeadingId(headingText(tokens[index + 1]), slugs))
  return defaultHeadingOpen(tokens, index, options, env, self)
}

export function renderMarkdown(value = ''): string {
  return markdown.render(String(value || ''), { headingSlugs: new Map<string, number>() })
}

export function extractMarkdownHeadings(value = ''): MarkdownHeading[] {
  const tokens = markdown.parse(String(value || ''), {})
  const slugs = new Map<string, number>()
  const headings: MarkdownHeading[] = []

  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return
    const text = headingText(tokens[index + 1])
    if (!text) return
    headings.push({
      id: uniqueHeadingId(text, slugs),
      text,
      level: Number(token.tag.slice(1)) || 1,
    })
  })

  return headings
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
