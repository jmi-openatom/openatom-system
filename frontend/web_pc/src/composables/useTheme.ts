import { computed, readonly, ref } from 'vue'

export type ThemePreference = 'system' | 'light' | 'dark'
export type ResolvedTheme = 'light' | 'dark'

const THEME_STORAGE_KEY = 'openatom_theme_preference'

const preference = ref<ThemePreference>('system')
const systemTheme = ref<ResolvedTheme>('light')

let initialized = false
let mediaQuery: MediaQueryList | undefined

const resolvedTheme = computed<ResolvedTheme>(() => {
  return preference.value === 'system' ? systemTheme.value : preference.value
})

const isDark = computed(() => resolvedTheme.value === 'dark')

function isThemePreference(value: string | null): value is ThemePreference {
  return value === 'system' || value === 'light' || value === 'dark'
}

function preferredSystemTheme(): ResolvedTheme {
  if (typeof window === 'undefined' || !window.matchMedia) return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme() {
  if (typeof document === 'undefined') return

  const nextTheme = resolvedTheme.value
  const root = document.documentElement

  root.classList.toggle('dark', nextTheme === 'dark')
  root.dataset.theme = nextTheme
  root.style.colorScheme = nextTheme
}

function readThemePreference(): ThemePreference {
  if (typeof window === 'undefined') return 'system'

  const storedPreference = window.localStorage.getItem(THEME_STORAGE_KEY)
  return isThemePreference(storedPreference) ? storedPreference : 'system'
}

export function initTheme() {
  if (typeof window === 'undefined' || initialized) return

  initialized = true
  systemTheme.value = preferredSystemTheme()
  preference.value = readThemePreference()
  applyTheme()

  mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const handleSystemThemeChange = (event: MediaQueryListEvent) => {
    systemTheme.value = event.matches ? 'dark' : 'light'
    applyTheme()
  }

  mediaQuery.addEventListener?.('change', handleSystemThemeChange)
}

export function useTheme() {
  initTheme()

  function setThemePreference(nextPreference: ThemePreference) {
    preference.value = nextPreference
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(THEME_STORAGE_KEY, nextPreference)
    }
    applyTheme()
  }

  function toggleTheme() {
    setThemePreference(isDark.value ? 'light' : 'dark')
  }

  return {
    preference: readonly(preference),
    resolvedTheme,
    isDark,
    setThemePreference,
    toggleTheme,
  }
}
