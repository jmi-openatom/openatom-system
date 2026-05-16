import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { nextTick, onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

gsap.registerPlugin(ScrollTrigger)

const PAGE_HEAD_SELECTOR = [
  '.activities-hero__inner > div',
  '.detail-hero .container',
  '.toolbar',
  '.section-head',
  '.notifications-hero__copy',
  '.notifications-hero__stats',
  '.scan-heading',
].join(',')

const PAGE_REVEAL_SELECTOR = [
  '.activity-row',
  '.campaign-item',
  '.apply-grid > *',
  '.progress-card',
  '.profile-grid > *',
  '.calendar-shell > *',
  '.notifications-workspace > *',
  '.notification-item',
  '.markdown-body',
  '.signup-panel',
  '.scan-panel',
  '.status-card',
  '.leave-card',
  '.setting-panel',
  '.history-panel',
  '.club-card',
].join(',')

const INTERACTIVE_CARD_SELECTOR = [
  '.signal-card',
  '.metric-node',
  '.activity-card',
  '.award-card',
  '.activity-row',
  '.campaign-item',
  '.apply-note',
  '.progress-card',
  '.profile-grid .el-card',
  '.week-card',
  '.notification-item',
  '.notification-summary',
  '.markdown-body',
  '.signup-panel',
  '.scan-panel',
  '.status-card',
  '.leave-card',
  '.setting-panel',
  '.history-panel',
  '.club-card',
].join(',')

const MAGNETIC_SELECTOR = [
  '.hero__actions .el-button',
  '.site-header__actions .el-button',
  '.site-nav a',
].join(',')

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function supportsPrecisePointer() {
  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
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
  const interactiveCards = new WeakSet<HTMLElement>()
  const magneticTargets = new WeakSet<HTMLElement>()
  const cleanupTasks: Array<() => void> = []

  let motionContext: gsap.Context | undefined
  let observer: MutationObserver | undefined
  let refreshFrame = 0

  function bindInteractiveCard(element: HTMLElement) {
    if (interactiveCards.has(element)) return

    interactiveCards.add(element)
    element.classList.add('motion-card')

    if (prefersReducedMotion() || !supportsPrecisePointer()) return

    gsap.set(element, {
      transformPerspective: 1000,
      transformStyle: 'preserve-3d',
    })

    const rotateXTo = gsap.quickTo(element, 'rotateX', {
      duration: 0.32,
      ease: 'power3.out',
    })
    const rotateYTo = gsap.quickTo(element, 'rotateY', {
      duration: 0.32,
      ease: 'power3.out',
    })
    const yTo = gsap.quickTo(element, 'y', {
      duration: 0.32,
      ease: 'power3.out',
    })

    const handlePointerEnter = () => {
      gsap.to(element, {
        boxShadow: '0 24px 72px rgba(15, 23, 42, 0.16)',
        duration: 0.28,
        ease: 'power2.out',
      })
      yTo(-8)
    }

    const handlePointerMove = (event: PointerEvent) => {
      const rect = element.getBoundingClientRect()
      const localX = (event.clientX - rect.left) / rect.width - 0.5
      const localY = (event.clientY - rect.top) / rect.height - 0.5

      rotateXTo(localY * -10)
      rotateYTo(localX * 10)
      element.style.setProperty('--motion-x', `${(localX + 0.5) * 100}%`)
      element.style.setProperty('--motion-y', `${(localY + 0.5) * 100}%`)
    }

    const handlePointerLeave = () => {
      rotateXTo(0)
      rotateYTo(0)
      yTo(0)
      gsap.to(element, {
        boxShadow: '0 0 0 rgba(15, 23, 42, 0)',
        duration: 0.3,
        ease: 'power2.out',
      })
    }

    element.addEventListener('pointerenter', handlePointerEnter)
    element.addEventListener('pointermove', handlePointerMove)
    element.addEventListener('pointerleave', handlePointerLeave)

    cleanupTasks.push(() => {
      element.removeEventListener('pointerenter', handlePointerEnter)
      element.removeEventListener('pointermove', handlePointerMove)
      element.removeEventListener('pointerleave', handlePointerLeave)
    })
  }

  function bindMagneticTarget(element: HTMLElement) {
    if (magneticTargets.has(element)) return

    magneticTargets.add(element)
    element.classList.add('motion-magnetic')

    if (prefersReducedMotion() || !supportsPrecisePointer()) return

    const xTo = gsap.quickTo(element, 'x', {
      duration: 0.28,
      ease: 'power3.out',
    })
    const yTo = gsap.quickTo(element, 'y', {
      duration: 0.28,
      ease: 'power3.out',
    })

    const handlePointerMove = (event: PointerEvent) => {
      const rect = element.getBoundingClientRect()
      const localX = event.clientX - rect.left - rect.width / 2
      const localY = event.clientY - rect.top - rect.height / 2

      xTo(localX * 0.18)
      yTo(localY * 0.18)
    }

    const handlePointerLeave = () => {
      xTo(0)
      yTo(0)
    }

    element.addEventListener('pointermove', handlePointerMove)
    element.addEventListener('pointerleave', handlePointerLeave)

    cleanupTasks.push(() => {
      element.removeEventListener('pointermove', handlePointerMove)
      element.removeEventListener('pointerleave', handlePointerLeave)
    })
  }

  function animatePageHead(element: HTMLElement, index: number) {
    if (animatedHeads.has(element) || isHomeRoute.value) return

    animatedHeads.add(element)
    disableCssAnimation(element)

    gsap.from(element, {
      y: 32,
      opacity: 0,
      filter: 'blur(12px)',
      duration: 0.82,
      delay: Math.min(index, 4) * 0.05,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: element,
        start: 'top 92%',
        once: true,
      },
    })
  }

  function animateReveal(element: HTMLElement, index: number) {
    if (animatedReveals.has(element) || isHomeRoute.value) return

    animatedReveals.add(element)
    disableCssAnimation(element)

    gsap.from(element, {
      y: 38,
      opacity: 0,
      rotateX: -4,
      scale: 0.985,
      filter: 'blur(10px)',
      transformOrigin: '50% 100%',
      duration: 0.78,
      delay: (index % 5) * 0.045,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: element,
        start: 'top 90%',
        once: true,
      },
    })
  }

  function refreshTargets() {
    const shell = shellRef.value
    const main = mainRef.value
    if (!shell || !main) return

    main
      .querySelectorAll<HTMLElement>(INTERACTIVE_CARD_SELECTOR)
      .forEach((element) => bindInteractiveCard(element))

    shell
      .querySelectorAll<HTMLElement>(MAGNETIC_SELECTOR)
      .forEach((element) => bindMagneticTarget(element))

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
    cleanupTasks.forEach((cleanup) => cleanup())
    motionContext?.revert()
  })
}
