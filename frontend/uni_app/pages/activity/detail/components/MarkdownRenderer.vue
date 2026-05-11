<script lang="ts" setup>
import {computed} from 'vue'

const props = defineProps<{
  content?: string
}>()

interface InlineSegment {
  type: 'text' | 'bold' | 'italic' | 'code' | 'strikethrough' | 'link'
  content: string
  url?: string
}

interface Block {
  type: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'p' | 'code' | 'blockquote' | 'li' | 'hr' | 'image' | 'br'
  children?: InlineSegment[]
  content?: string
  alt?: string
  src?: string
}

function parseInline(text: string): InlineSegment[] {
  const segments: InlineSegment[] = []
  let remaining = text

  while (remaining.length) {
    let m

    // bold + italic
    m = remaining.match(/^\*\*\*([^*]+)\*\*\*/)
    if (m) {
      segments.push({type: 'bold', content: m[1]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    // bold
    m = remaining.match(/^\*\*([^*]+)\*\*/)
    if (m) {
      segments.push({type: 'bold', content: m[1]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    // italic
    m = remaining.match(/^\*([^*]+)\*/)
    if (m) {
      segments.push({type: 'italic', content: m[1]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    // strikethrough
    m = remaining.match(/^~~([^~]+)~~/)
    if (m) {
      segments.push({type: 'strikethrough', content: m[1]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    // inline code
    m = remaining.match(/^`([^`]+)`/)
    if (m) {
      segments.push({type: 'code', content: m[1]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    // link
    m = remaining.match(/^\[([^\]]+)\]\(([^)]+)\)/)
    if (m) {
      segments.push({type: 'link', content: m[1], url: m[2]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    // plain text
    m = remaining.match(/^([^!\[*~`\n]+)/)
    if (m) {
      segments.push({type: 'text', content: m[1]})
      remaining = remaining.slice(m[0].length)
      continue
    }
    if (remaining[0] === '\n') {
      segments.push({type: 'text', content: ' '})
      remaining = remaining.slice(1)
      continue
    }
    segments.push({type: 'text', content: remaining[0]})
    remaining = remaining.slice(1)
  }

  return segments
}

function parseBlocks(md: string): Block[] {
  if (!md) return []
  const lines = md.split('\n')
  const blocks: Block[] = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]

    if (!line.trim()) {
      i++
      continue
    }

    // hr
    if (/^(---|\*\*\*|___)\s*$/.test(line.trim())) {
      blocks.push({type: 'hr'})
      i++
      continue
    }

    // code block
    if (line.trim().startsWith('```')) {
      const codeLines: string[] = []
      i++
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i])
        i++
      }
      i++ // skip closing ```
      blocks.push({type: 'code', content: codeLines.join('\n')})
      continue
    }

    // heading
    const hMatch = line.match(/^(#{1,6})\s+(.+)/)
    if (hMatch) {
      blocks.push({type: `h${hMatch[1].length}` as Block['type'], children: parseInline(hMatch[2])})
      i++
      continue
    }

    // blockquote
    if (line.trim().startsWith('> ')) {
      const quoteLines: string[] = []
      while (i < lines.length && lines[i].trim().startsWith('> ')) {
        quoteLines.push(lines[i].trim().slice(2))
        i++
      }
      blocks.push({type: 'blockquote', children: parseInline(quoteLines.join('\n'))})
      continue
    }

    // unordered list
    if (/^[-*]\s+/.test(line.trim())) {
      while (i < lines.length && /^[-*]\s+/.test(lines[i].trim())) {
        const content = lines[i].trim().replace(/^[-*]\s+/, '')
        blocks.push({type: 'li', children: parseInline(content)})
        i++
      }
      continue
    }

    // ordered list
    if (/^\d+\.\s+/.test(line.trim())) {
      while (i < lines.length && /^\d+\.\s+/.test(lines[i].trim())) {
        const content = lines[i].trim().replace(/^\d+\.\s+/, '')
        blocks.push({type: 'li', children: parseInline(content)})
        i++
      }
      continue
    }

    // image block
    const imgMatch = line.match(/^!\[([^\]]*)\]\(([^)]+)\)$/)
    if (imgMatch) {
      blocks.push({type: 'image', alt: imgMatch[1], src: imgMatch[2]})
      i++
      continue
    }

    // paragraph
    const pLines: string[] = []
    while (i < lines.length && lines[i].trim() !== ''
      && !/^(#{1,6}\s|```|> |- |\* |\d+\. |!\[|---|\*\*\*|___)/.test(lines[i] + ' ')) {
      pLines.push(lines[i])
      i++
    }
    if (pLines.length) {
      blocks.push({type: 'p', children: parseInline(pLines.join('\n'))})
    }
  }

  return blocks
}

const blocks = computed(() => {
  if (!props.content) return []
  return parseBlocks(props.content)
})
</script>

<template>
  <view v-if="blocks.length" class="md-root">
    <view v-for="(b, bi) in blocks" :key="bi">
      <view v-if="b.type === 'h1'" class="md-h1">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'h2'" class="md-h2">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'h3'" class="md-h3">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'h4'" class="md-h4">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'h5'" class="md-h5">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'h6'" class="md-h6">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'p'" class="md-p">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'code'" class="md-code-block">
        <text class="md-code-text">{{ b.content }}</text>
      </view>
      <view v-else-if="b.type === 'blockquote'" class="md-blockquote">
        <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
      </view>
      <view v-else-if="b.type === 'li'" class="md-li">
        <text class="md-li-dot">&#x2022;</text>
        <view class="md-li-body">
          <text v-for="(seg, si) in b.children" :key="si" :class="seg.type">{{ seg.content }}</text>
        </view>
      </view>
      <view v-else-if="b.type === 'hr'" class="md-hr" />
      <image v-else-if="b.type === 'image'" :src="b.src" mode="widthFix" class="md-img" />
    </view>
  </view>
  <view v-else class="md-empty">
    <text class="md-empty-text">暂无内容</text>
  </view>
</template>

<style lang="scss" scoped>
.md-root {
  line-height: 1.75;
}

.md-h1 {
  font-size: 40rpx;
  font-weight: 700;
  color: #0f172a;
  margin: 32rpx 0 16rpx;
  line-height: 1.3;
}

.md-h2 {
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
  margin: 28rpx 0 14rpx;
  line-height: 1.35;
}

.md-h3 {
  font-size: 32rpx;
  font-weight: 600;
  color: #1e293b;
  margin: 24rpx 0 12rpx;
  line-height: 1.4;
}

.md-h4 {
  font-size: 28rpx;
  font-weight: 600;
  color: #334155;
  margin: 20rpx 0 10rpx;
}

.md-h5 {
  font-size: 26rpx;
  font-weight: 600;
  color: #475569;
  margin: 16rpx 0 8rpx;
}

.md-h6 {
  font-size: 24rpx;
  font-weight: 600;
  color: #64748b;
  margin: 12rpx 0 8rpx;
}

.md-p {
  font-size: 28rpx;
  color: #334155;
  margin: 8rpx 0;
}

.md-code-block {
  margin: 16rpx 0;
  padding: 20rpx 24rpx;
  border-radius: 12rpx;
  background: #1e293b;
}

.md-code-text {
  font-size: 24rpx;
  font-family: 'Courier New', monospace;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
}

.md-blockquote {
  margin: 16rpx 0;
  padding: 12rpx 20rpx;
  border-left: 6rpx solid #1769e8;
  background: rgba(23, 105, 232, 0.04);
  border-radius: 0 8rpx 8rpx 0;
  font-size: 26rpx;
  color: #475569;
  font-style: italic;
}

.md-li {
  display: flex;
  margin: 6rpx 0;
  padding-left: 12rpx;
  font-size: 28rpx;
  color: #334155;
}

.md-li-dot {
  margin-right: 12rpx;
  color: #1769e8;
  font-weight: 700;
}

.md-li-body {
  flex: 1;
}

.md-hr {
  height: 1rpx;
  margin: 24rpx 0;
  background: rgba(226, 232, 240, 0.8);
}

.md-img {
  max-width: 100%;
  margin: 16rpx 0;
  border-radius: 12rpx;
}

.md-empty {
  padding: 60rpx 0;
  text-align: center;
}

.md-empty-text {
  font-size: 26rpx;
  color: #94a3b8;
}

.text {
  color: inherit;
}

.bold {
  font-weight: 700;
}

.italic {
  font-style: italic;
}

.code {
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
  background: rgba(148, 163, 184, 0.15);
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e74c3c;
}

.strikethrough {
  text-decoration: line-through;
  color: #94a3b8;
}

.link {
  color: #1769e8;
  text-decoration: underline;
}
</style>
