import { nextTick, onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

const PAGE_HEAD_SELECTOR = '.site-page-hero__copy'
const PAGE_REVEAL_SELECTOR = '.site-reveal'

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

export function useSiteShellMotion(
  shellRef: Ref<HTMLElement | undefined>,
  mainRef: Ref<HTMLElement | undefined>,
  isHomeRoute: Readonly<Ref<boolean>>,
) {
  let observer: IntersectionObserver | undefined
  let mutationObserver: MutationObserver | undefined
  let refreshFrame = 0

  function observeTargets() {
    const main = mainRef.value
    if (!main || isHomeRoute.value || prefersReducedMotion()) return

    main
      .querySelectorAll<HTMLElement>(`${PAGE_HEAD_SELECTOR}, ${PAGE_REVEAL_SELECTOR}`)
      .forEach((element) => {
        if (element.dataset.siteMotionObserved === '1') return
        element.dataset.siteMotionObserved = '1'
        element.classList.add('site-motion-pending')
        observer?.observe(element)
      })
  }

  function queueRefresh() {
    if (refreshFrame) return
    refreshFrame = window.requestAnimationFrame(() => {
      refreshFrame = 0
      observeTargets()
    })
  }

  onMounted(async () => {
    await nextTick()

    const shell = shellRef.value
    const main = mainRef.value
    if (!shell || !main || prefersReducedMotion()) return

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          const element = entry.target as HTMLElement
          element.classList.add('site-motion-visible')
          observer?.unobserve(element)
        })
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.08 },
    )

    observeTargets()

    mutationObserver = new MutationObserver(queueRefresh)
    mutationObserver.observe(main, {
      childList: true,
      subtree: true,
    })
  })

  watch(isHomeRoute, async () => {
    await nextTick()
    queueRefresh()
  })

  onBeforeUnmount(() => {
    if (refreshFrame) {
      window.cancelAnimationFrame(refreshFrame)
    }
    mutationObserver?.disconnect()
    observer?.disconnect()
  })
}
