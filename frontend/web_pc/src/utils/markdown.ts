const escapeHtml = (value = ''): string =>
  String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const safeUrl = (value = ''): string => {
  const url = value.replaceAll('&amp;', '&').trim()
  if (/^(https?:\/\/|\/(?!\/)|data:image\/(?:png|jpeg|jpg|gif|webp);base64,)/i.test(url)) {
    return escapeHtml(url)
  }
  return '#'
}

const inline = (value: string): string => {
  let output = escapeHtml(value)
  output = output.replace(/`([^`]+?)`/g, '<code>$1</code>')
  output = output.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
  output = output.replace(/\*([^*]+?)\*/g, '<em>$1</em>')
  output = output.replace(
    /!\[([^\]]*)\]\(([^)\s]+)(?:\s+&quot;(.+?)&quot;)?\)/g,
    (_match, alt, url, title) =>
      `<img src="${safeUrl(url)}" alt="${alt}"${title ? ` title="${title}"` : ''} loading="lazy" />`,
  )
  output = output.replace(
    /\[([^\]]+)\]\(([^)\s]+)(?:\s+&quot;(.+?)&quot;)?\)/g,
    (_match, label, url, title) =>
      `<a href="${safeUrl(url)}" target="_blank" rel="noopener noreferrer"${
        title ? ` title="${title}"` : ''
      }>${label}</a>`,
  )
  return output
}

const splitTableRow = (line: string): string[] =>
  line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => cell.trim())

const isTableDivider = (line = ''): boolean =>
  /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line)

export function renderMarkdown(markdown = ''): string {
  const lines = String(markdown).split(/\r?\n/)
  const html: string[] = []
  let listType: 'ul' | 'ol' | '' = ''
  let inCode = false
  let codeLanguage = ''
  let codeLines: string[] = []

  const closeList = (): void => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = ''
    }
  }

  const closeCode = (): void => {
    const languageClass = codeLanguage ? ` class="language-${escapeHtml(codeLanguage)}"` : ''
    html.push(`<pre><code${languageClass}>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
    codeLines = []
    codeLanguage = ''
    inCode = false
  }

  const openList = (type: 'ul' | 'ol'): void => {
    if (listType === type) return
    closeList()
    html.push(`<${type}>`)
    listType = type
  }

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    const trimmed = line.trim()

    if (inCode) {
      if (trimmed.startsWith('```')) closeCode()
      else codeLines.push(line)
      continue
    }

    if (trimmed.startsWith('```')) {
      closeList()
      inCode = true
      codeLanguage = trimmed.slice(3).trim().split(/\s+/)[0] || ''
      codeLines = []
      continue
    }

    if (!trimmed) {
      closeList()
      continue
    }

    if (index + 1 < lines.length && trimmed.includes('|') && isTableDivider(lines[index + 1])) {
      closeList()
      const headers = splitTableRow(line)
      index += 2
      const rows: string[][] = []
      while (index < lines.length && lines[index].trim().includes('|')) {
        rows.push(splitTableRow(lines[index]))
        index += 1
      }
      index -= 1
      html.push('<table><thead><tr>')
      headers.forEach((header) => html.push(`<th>${inline(header)}</th>`))
      html.push('</tr></thead><tbody>')
      rows.forEach((row) => {
        html.push('<tr>')
        headers.forEach((_header, cellIndex) => {
          html.push(`<td>${inline(row[cellIndex] || '')}</td>`)
        })
        html.push('</tr>')
      })
      html.push('</tbody></table>')
      continue
    }

    const heading = /^(#{1,6})\s+(.+)$/.exec(trimmed)
    if (heading) {
      closeList()
      const level = heading[1].length
      html.push(`<h${level}>${inline(heading[2])}</h${level}>`)
      continue
    }

    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      closeList()
      html.push('<hr />')
      continue
    }

    if (trimmed.startsWith('>')) {
      closeList()
      const quoteLines: string[] = []
      while (index < lines.length && lines[index].trim().startsWith('>')) {
        quoteLines.push(lines[index].trim().replace(/^>\s?/, ''))
        index += 1
      }
      index -= 1
      html.push(`<blockquote>${quoteLines.map((quote) => `<p>${inline(quote)}</p>`).join('')}</blockquote>`)
      continue
    }

    const unordered = /^[-*+]\s+(.+)$/.exec(trimmed)
    if (unordered) {
      openList('ul')
      html.push(`<li>${inline(unordered[1])}</li>`)
      continue
    }

    const ordered = /^\d+[.)]\s+(.+)$/.exec(trimmed)
    if (ordered) {
      openList('ol')
      html.push(`<li>${inline(ordered[1])}</li>`)
      continue
    }

    closeList()
    html.push(`<p>${inline(line)}</p>`)
  }

  if (inCode) closeCode()
  closeList()
  return html.join('')
}
