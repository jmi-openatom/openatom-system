const VERSION = 6
const SIZE = VERSION * 4 + 17
const DATA_CODEWORDS = 136
const ECC_CODEWORDS_PER_BLOCK = 18
const ALIGNMENT_POSITIONS = [6, 34]
const FORMAT_BITS_L_MASK_0 = 0x77c4

const EXP = new Array<number>(512)
const LOG = new Array<number>(256)

let value = 1
for (let i = 0; i < 255; i++) {
  EXP[i] = value
  LOG[value] = i
  value <<= 1
  if (value & 0x100) value ^= 0x11d
}
for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255]

function gfMul(a: number, b: number): number {
  if (a === 0 || b === 0) return 0
  return EXP[LOG[a] + LOG[b]]
}

function generatorPoly(degree: number): number[] {
  let result = [1]
  for (let i = 0; i < degree; i++) {
    const next = new Array(result.length + 1).fill(0)
    for (let j = 0; j < result.length; j++) {
      next[j] ^= result[j]
      next[j + 1] ^= gfMul(result[j], EXP[i])
    }
    result = next
  }
  return result
}

function reedSolomon(data: number[], degree: number): number[] {
  const generator = generatorPoly(degree)
  const result = new Array(degree).fill(0)
  for (const dataByte of data) {
    const factor = dataByte ^ result[0]
    result.shift()
    result.push(0)
    for (let i = 0; i < degree; i++) {
      result[i] ^= gfMul(generator[i + 1], factor)
    }
  }
  return result
}

class BitBuffer {
  bits: number[] = []

  push(value: number, length: number) {
    for (let i = length - 1; i >= 0; i--) this.bits.push((value >>> i) & 1)
  }

  toCodewords(): number[] {
    const result: number[] = []
    for (let i = 0; i < this.bits.length; i += 8) {
      let byte = 0
      for (let j = 0; j < 8; j++) byte = (byte << 1) | (this.bits[i + j] || 0)
      result.push(byte)
    }
    return result
  }
}

function createDataCodewords(text: string): number[] {
  const bytes = Array.from(new TextEncoder().encode(text))
  if (bytes.length > 133) throw new Error('二维码内容过长')
  const buffer = new BitBuffer()
  buffer.push(0b0100, 4)
  buffer.push(bytes.length, 16)
  for (const byte of bytes) buffer.push(byte, 8)
  const maxBits = DATA_CODEWORDS * 8
  buffer.push(0, Math.min(4, maxBits - buffer.bits.length))
  while (buffer.bits.length % 8) buffer.push(0, 1)
  const codewords = buffer.toCodewords()
  for (let pad = 0xec; codewords.length < DATA_CODEWORDS; pad = pad === 0xec ? 0x11 : 0xec) {
    codewords.push(pad)
  }
  return codewords
}

function createFinalCodewords(dataCodewords: number[]): number[] {
  const blockSizes = [68, 68]
  const blocks = blockSizes.map((size, index) => {
    const start = blockSizes.slice(0, index).reduce((sum, item) => sum + item, 0)
    const data = dataCodewords.slice(start, start + size)
    return { data, ecc: reedSolomon(data, ECC_CODEWORDS_PER_BLOCK) }
  })
  const result: number[] = []
  for (let i = 0; i < 69; i++) {
    for (const block of blocks) if (i < block.data.length) result.push(block.data[i])
  }
  for (let i = 0; i < ECC_CODEWORDS_PER_BLOCK; i++) {
    for (const block of blocks) result.push(block.ecc[i])
  }
  return result
}

function createMatrix(): { modules: boolean[][]; reserved: boolean[][] } {
  const modules = Array.from({ length: SIZE }, () => new Array<boolean>(SIZE).fill(false))
  const reserved = Array.from({ length: SIZE }, () => new Array<boolean>(SIZE).fill(false))
  const set = (row: number, col: number, dark: boolean, reserve = true) => {
    if (row < 0 || col < 0 || row >= SIZE || col >= SIZE) return
    modules[row][col] = dark
    if (reserve) reserved[row][col] = true
  }

  const finder = (row: number, col: number) => {
    for (let y = -1; y <= 7; y++) {
      for (let x = -1; x <= 7; x++) {
        const r = row + y
        const c = col + x
        const dark =
          x >= 0 &&
          x <= 6 &&
          y >= 0 &&
          y <= 6 &&
          (x === 0 || x === 6 || y === 0 || y === 6 || (x >= 2 && x <= 4 && y >= 2 && y <= 4))
        set(r, c, dark)
      }
    }
  }

  finder(0, 0)
  finder(0, SIZE - 7)
  finder(SIZE - 7, 0)

  for (let i = 8; i < SIZE - 8; i++) {
    set(6, i, i % 2 === 0)
    set(i, 6, i % 2 === 0)
  }

  for (const row of ALIGNMENT_POSITIONS) {
    for (const col of ALIGNMENT_POSITIONS) {
      if ((row === 6 && col === 6) || (row === 6 && col === 50) || (row === 50 && col === 6)) continue
      for (let y = -2; y <= 2; y++) {
        for (let x = -2; x <= 2; x++) {
          set(row + y, col + x, Math.max(Math.abs(x), Math.abs(y)) !== 1)
        }
      }
    }
  }

  set(SIZE - 8, 8, true)
  reserveFormatAreas(reserved)
  if (VERSION >= 7) drawVersionInfo(modules, reserved)
  return { modules, reserved }
}

function reserveFormatAreas(reserved: boolean[][]) {
  for (let i = 0; i < 9; i++) {
    reserved[8][i] = true
    reserved[i][8] = true
  }
  for (let i = 0; i < 8; i++) {
    reserved[8][SIZE - 1 - i] = true
    reserved[SIZE - 1 - i][8] = true
  }
}

function versionBits(): number {
  let rem = VERSION
  for (let i = 0; i < 12; i++) {
    rem <<= 1
    if ((rem & 0x1000) !== 0) rem ^= 0x1f25
  }
  return (VERSION << 12) | rem
}

function drawVersionInfo(modules: boolean[][], reserved: boolean[][]) {
  const bits = versionBits()
  for (let i = 0; i < 18; i++) {
    const dark = ((bits >>> i) & 1) === 1
    const row = Math.floor(i / 3)
    const col = i % 3 + SIZE - 11
    modules[row][col] = dark
    reserved[row][col] = true
    modules[col][row] = dark
    reserved[col][row] = true
  }
}

function drawData(modules: boolean[][], reserved: boolean[][], codewords: number[]) {
  const bits = codewords.flatMap((byte) =>
    Array.from({ length: 8 }, (_, index) => (byte >>> (7 - index)) & 1),
  )
  let bitIndex = 0
  let upward = true
  for (let col = SIZE - 1; col > 0; col -= 2) {
    if (col === 6) col--
    for (let i = 0; i < SIZE; i++) {
      const row = upward ? SIZE - 1 - i : i
      for (let offset = 0; offset < 2; offset++) {
        const c = col - offset
        if (reserved[row][c]) continue
        const masked = ((bits[bitIndex++] || 0) === 1) !== ((row + c) % 2 === 0)
        modules[row][c] = masked
      }
    }
    upward = !upward
  }
}

function drawFormatInfo(modules: boolean[][]) {
  for (let i = 0; i <= 5; i++) modules[8][i] = ((FORMAT_BITS_L_MASK_0 >>> i) & 1) === 1
  modules[8][7] = ((FORMAT_BITS_L_MASK_0 >>> 6) & 1) === 1
  modules[8][8] = ((FORMAT_BITS_L_MASK_0 >>> 7) & 1) === 1
  modules[7][8] = ((FORMAT_BITS_L_MASK_0 >>> 8) & 1) === 1
  for (let i = 9; i < 15; i++) modules[14 - i][8] = ((FORMAT_BITS_L_MASK_0 >>> i) & 1) === 1

  for (let i = 0; i < 8; i++) modules[SIZE - 1 - i][8] = ((FORMAT_BITS_L_MASK_0 >>> i) & 1) === 1
  for (let i = 8; i < 15; i++) modules[8][SIZE - 15 + i] = ((FORMAT_BITS_L_MASK_0 >>> i) & 1) === 1
  modules[SIZE - 8][8] = true
}

export function qrSvgDataUrl(text: string): string {
  const { modules, reserved } = createMatrix()
  drawData(modules, reserved, createFinalCodewords(createDataCodewords(text)))
  drawFormatInfo(modules)
  const cell = 8
  const quiet = 4
  const total = (SIZE + quiet * 2) * cell
  const rects: string[] = []
  for (let row = 0; row < SIZE; row++) {
    for (let col = 0; col < SIZE; col++) {
      if (modules[row][col]) {
        rects.push(`<rect x="${(col + quiet) * cell}" y="${(row + quiet) * cell}" width="${cell}" height="${cell}"/>`)
      }
    }
  }
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${total} ${total}" shape-rendering="crispEdges"><rect width="100%" height="100%" fill="#fff"/><g fill="#0f172a">${rects.join('')}</g></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
