// ViewModel: UI state (theme, toast, global keyboard shortcuts).
import { onBeforeUnmount, onMounted, ref } from 'vue'

const theme = ref<'light' | 'dark'>('light')
const toast = ref('')
const accountMenu = ref(false)
let toastTimer: number | undefined

export function initTheme(): void {
  theme.value = (localStorage.getItem('openatom-theme') as 'light' | 'dark') || 'light'
  applyTheme()
}

export function toggleTheme(): void {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  localStorage.setItem('openatom-theme', theme.value)
  applyTheme()
}

function applyTheme(): void {
  document.documentElement.dataset.theme = theme.value
}

export function showToast(value: string): void {
  toast.value = value
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value = ''
  }, 4000)
}

/** Registers global keyboard shortcuts; returns a disposer for unmount. */
export function useGlobalShortcuts(onSearch: () => void, onEscape: () => void): void {
  function handleKeydown(event: KeyboardEvent) {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault()
      onSearch()
    }
    if (event.key === 'Escape') onEscape()
  }
  onMounted(() => window.addEventListener('keydown', handleKeydown))
  onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
}

export function useUiStore() {
  return { theme, toast, accountMenu, initTheme, toggleTheme, showToast, useGlobalShortcuts }
}
