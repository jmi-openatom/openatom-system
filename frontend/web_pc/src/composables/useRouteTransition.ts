import gsap from 'gsap'

type RouteTransitionOptions = {
  enterY?: number
  leaveY?: number
  enterDuration?: number
  leaveDuration?: number
}

export function useRouteTransition(options: RouteTransitionOptions = {}) {
  const enterY = options.enterY ?? 22
  const leaveY = options.leaveY ?? -10
  const enterDuration = options.enterDuration ?? 0.34
  const leaveDuration = options.leaveDuration ?? 0.18

  let activeTween: gsap.core.Tween | undefined

  function prefersReducedMotion() {
    return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  }

  function kill() {
    activeTween?.kill()
    activeTween = undefined
  }

  function beforeEnter(element: Element) {
    if (prefersReducedMotion()) {
      gsap.set(element, { autoAlpha: 0 })
      return
    }

    gsap.set(element, {
      autoAlpha: 0,
      y: enterY,
      scale: 0.992,
      transformOrigin: '50% 0%',
      willChange: 'transform, opacity',
    })
  }

  function enter(element: Element, done: () => void) {
    kill()

    activeTween = gsap.to(element, {
      autoAlpha: 1,
      y: 0,
      scale: 1,
      duration: prefersReducedMotion() ? 0.12 : enterDuration,
      ease: 'power3.out',
      overwrite: 'auto',
      clearProps: 'transform,opacity,visibility,willChange',
      onComplete: done,
    })
  }

  function leave(element: Element, done: () => void) {
    kill()

    activeTween = gsap.to(element, {
      autoAlpha: 0,
      y: prefersReducedMotion() ? 0 : leaveY,
      scale: prefersReducedMotion() ? 1 : 0.996,
      duration: prefersReducedMotion() ? 0.1 : leaveDuration,
      ease: 'power2.in',
      overwrite: 'auto',
      onComplete: done,
    })
  }

  return {
    beforeEnter,
    enter,
    leave,
    kill,
  }
}
