import { postAiStream } from '@/api'
import { ref } from 'vue'

export interface AiStreamHandlers {
  onDelta?: (content: string) => void
  onPhase?: (message: string) => void
  onEvent?: (event: string, data: any) => void
}

/**
 * AI 流式请求封装：统一管理 streaming 状态、阶段提示、Abort 中断与错误事件。
 *
 * 错误处理约定：流内 error 事件会抛出 Error（消息来自后端），由调用方 catch；
 * 用户手动中断（abort）不抛错。
 */
export function useAiStream() {
  const streaming = ref(false)
  const phase = ref('')
  const controller = ref<AbortController | null>(null)

  function abort() {
    controller.value?.abort()
    controller.value = null
    streaming.value = false
    phase.value = ''
  }

  async function run(
    path: string,
    body: Record<string, unknown>,
    handlers: AiStreamHandlers = {},
  ): Promise<void> {
    const ctrl = new AbortController()
    controller.value = ctrl
    streaming.value = true
    try {
      await postAiStream(
        path,
        body,
        ({ event, data }) => {
          if (event === 'phase') {
            const message = data?.message || ''
            phase.value = message
            handlers.onPhase?.(message)
          } else if (event === 'delta') {
            handlers.onDelta?.(data?.content || '')
          } else if (event === 'error') {
            throw new Error(data?.message || 'AI 流式输出失败')
          } else {
            handlers.onEvent?.(event, data)
          }
        },
        ctrl.signal,
      )
    } finally {
      if (controller.value === ctrl) controller.value = null
      streaming.value = false
    }
  }

  return { streaming, phase, run, abort }
}