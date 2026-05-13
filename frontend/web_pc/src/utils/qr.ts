import QRCode from '@/vendor/qrcode-browser.js'

export function qrSvgDataUrl(text: string): string {
  const qr = QRCode.create(text, {
    errorCorrectionLevel: 'H',
  })
  const cell = 12
  const quiet = 4
  const size = qr.modules.size
  const total = (size + quiet * 2) * cell
  const rects: string[] = []
  qr.modules.data.forEach((dark: number, index: number) => {
    if (!dark) return
    const x = (index % size + quiet) * cell
    const y = (Math.floor(index / size) + quiet) * cell
    rects.push(`<rect x="${x}" y="${y}" width="${cell}" height="${cell}"/>`)
  })
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${total} ${total}" shape-rendering="crispEdges"><rect width="100%" height="100%" fill="#fff"/><g fill="#000">${rects.join('')}</g></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
