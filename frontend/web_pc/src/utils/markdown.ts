import MarkdownIt from 'markdown-it'

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

export function renderMarkdown(value = ''): string {
  return markdown.render(String(value || ''))
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
