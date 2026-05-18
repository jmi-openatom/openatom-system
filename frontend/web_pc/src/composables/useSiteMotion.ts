import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { nextTick, onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

gsap.registerPlugin(ScrollTrigger)

const PAGE_HEAD_SELECTOR = '.site-page-hero__copy'
const PAGE_REVEAL_SELECTOR = '.site-reveal'

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function disableCssAnimation(element: HTMLElement) {
  element.style.animation = 'none'
}

export function useSiteShellMotion(
  shellRef: Ref<HTMLElement | undefined>,
  mainRef: Ref<HTMLElement | undefined>,
  isHomeRoute: Readonly<Ref<boolean>>,
) {
  const animatedHeads = new WeakSet<HTMLElement>()
  const animatedReveals = new WeakSet<HTMLElement>()

  let motionContext: gsap.Context | undefined
  let observer: MutationObserver | undefined
  let refreshFrame = 0

  function animatePageHead(element: HTMLElement, index: number) {
    if (animatedHeads.has(element) || isHomeRoute.value) return

    animatedHeads.add(element)
    disableCssAnimation(element)

    gsap.from(element, {
      y: 18,
      opacity: 0,
      duration: 0.56,
      delay: Math.min(index, 3) * 0.04,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: element,
        start: 'top 94%',
        once: true,
      },
    })
  }

  function animateReveal(element: HTMLElement, index: number) {
    if (animatedReveals.has(element) || isHomeRoute.value) return

    animatedReveals.add(element)
    disableCssAnimation(element)

    gsap.from(element, {
      y: 18,
      opacity: 0,
      duration: 0.52,
      delay: (index % 4) * 0.03,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: element,
        start: 'top 92%',
        once: true,
      },
    })
  }

  function refreshTargets() {
    const main = mainRef.value
    if (!main) return

    if (!prefersReducedMotion()) {
      main
        .querySelectorAll<HTMLElement>(PAGE_HEAD_SELECTOR)
        .forEach((element, index) => animatePageHead(element, index))

      main
        .querySelectorAll<HTMLElement>(PAGE_REVEAL_SELECTOR)
        .forEach((element, index) => animateReveal(element, index))
    }

    ScrollTrigger.refresh()
  }

  function queueRefresh() {
    if (refreshFrame) return
    refreshFrame = window.requestAnimationFrame(() => {
      refreshFrame = 0
      refreshTargets()
    })
  }

  onMounted(async () => {
    await nextTick()

    const shell = shellRef.value
    const main = mainRef.value
    if (!shell || !main) return

    motionContext = gsap.context(() => undefined, shell)
    refreshTargets()

    observer = new MutationObserver(queueRefresh)
    observer.observe(main, {
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
    observer?.disconnect()
    motionContext?.revert()
  })
}
