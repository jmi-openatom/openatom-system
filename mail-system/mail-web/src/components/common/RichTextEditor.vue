<template>
  <div class="rich-editor">
    <div v-if="editor" class="rich-editor__toolbar" role="toolbar" aria-label="文本格式">
      <button :disabled="!editor.can().undo()" aria-label="撤销" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.undo())">
        <Undo2 :size="16" />
      </button>
      <button :disabled="!editor.can().redo()" aria-label="重做" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.redo())">
        <Redo2 :size="16" />
      </button>
      <span class="tool-sep" aria-hidden="true"></span>
      <button :class="{ active: editor.isActive('bold') }" aria-label="加粗" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleBold())">
        <Bold :size="16" />
      </button>
      <button :class="{ active: editor.isActive('italic') }" aria-label="斜体" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleItalic())">
        <Italic :size="16" />
      </button>
      <button :class="{ active: editor.isActive('underline') }" aria-label="下划线" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleUnderline())">
        <UnderlineIcon :size="16" />
      </button>
      <button :class="{ active: editor.isActive('strike') }" aria-label="删除线" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleStrike())">
        <Strikethrough :size="16" />
      </button>
      <span class="tool-sep" aria-hidden="true"></span>
      <button :class="{ active: editor.isActive('heading', { level: 2 }) }" aria-label="二级标题" class="tool tool--label" type="button" @mousedown.prevent @click="exec((c) => c.toggleHeading({ level: 2 }))">
        H2
      </button>
      <button :class="{ active: editor.isActive('heading', { level: 3 }) }" aria-label="三级标题" class="tool tool--label" type="button" @mousedown.prevent @click="exec((c) => c.toggleHeading({ level: 3 }))">
        H3
      </button>
      <span class="tool-sep" aria-hidden="true"></span>
      <button :class="{ active: editor.isActive('bulletList') }" aria-label="无序列表" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleBulletList())">
        <List :size="16" />
      </button>
      <button :class="{ active: editor.isActive('orderedList') }" aria-label="有序列表" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleOrderedList())">
        <ListOrdered :size="16" />
      </button>
      <button :class="{ active: editor.isActive('blockquote') }" aria-label="引用" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleBlockquote())">
        <Quote :size="16" />
      </button>
      <span class="tool-sep" aria-hidden="true"></span>
      <button :class="{ active: editor.isActive('code') }" aria-label="行内代码" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleCode())">
        <Code :size="16" />
      </button>
      <button :class="{ active: editor.isActive('codeBlock') }" aria-label="代码块" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.toggleCodeBlock())">
        <SquareCode :size="16" />
      </button>
      <span class="tool-sep" aria-hidden="true"></span>
      <button :class="{ active: editor.isActive('link') }" aria-label="插入链接" class="tool" type="button" @mousedown.prevent @click="setLink">
        <LinkIcon :size="16" />
      </button>
      <button :disabled="!editor.isActive('link')" aria-label="移除链接" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.unsetLink())">
        <Link2Off :size="16" />
      </button>
      <span class="tool-sep" aria-hidden="true"></span>
      <button :class="{ active: editor.isActive({ textAlign: 'left' }) }" aria-label="左对齐" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.setTextAlign('left'))">
        <AlignLeft :size="16" />
      </button>
      <button :class="{ active: editor.isActive({ textAlign: 'center' }) }" aria-label="居中" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.setTextAlign('center'))">
        <AlignCenter :size="16" />
      </button>
      <button :class="{ active: editor.isActive({ textAlign: 'right' }) }" aria-label="右对齐" class="tool" type="button" @mousedown.prevent @click="exec((c) => c.setTextAlign('right'))">
        <AlignRight :size="16" />
      </button>
    </div>
    <EditorContent :editor="editor" class="rich-editor__content" />
  </div>
</template>

<script lang="ts" setup>
import { useEditor, EditorContent, type ChainedCommands } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'
import Placeholder from '@tiptap/extension-placeholder'
import TextAlign from '@tiptap/extension-text-align'
import {
  AlignCenter, AlignLeft, AlignRight, Bold, Code, Italic, Link as LinkIcon,
  Link2Off, List, ListOrdered, Quote, Redo2, SquareCode, Strikethrough, Underline as UnderlineIcon, Undo2,
} from 'lucide-vue-next'

const props = defineProps<{ modelValue: string; placeholder?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const editor = useEditor({
  content: props.modelValue || '<p></p>',
  extensions: [
    StarterKit,
    Underline,
    Link.configure({ openOnClick: false, autolink: true, defaultProtocol: 'https' }),
    Placeholder.configure({ placeholder: props.placeholder || '写点什么…' }),
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
  ],
  onUpdate: ({ editor: instance }) => emit('update:modelValue', instance.getHTML()),
})

function exec(fn: (chain: ChainedCommands) => ChainedCommands) {
  const instance = editor.value
  if (!instance) return
  const { from, to } = instance.state.selection
  fn(instance.chain().focus().setTextSelection({ from, to })).run()
}

function setLink() {
  const instance = editor.value
  if (!instance) return
  const previous = instance.getAttributes('link').href as string | undefined
  const url = window.prompt('链接地址', previous ?? 'https://')
  if (url === null) return
  if (!url.trim()) {
    instance.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  instance.chain().focus().extendMarkRange('link').setLink({ href: url.trim() }).run()
}
</script>