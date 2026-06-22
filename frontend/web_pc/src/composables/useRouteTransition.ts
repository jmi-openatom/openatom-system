type RouteTransitionOptions = {
  enterY?: number
  leaveY?: number
  enterDuration?: number
  leaveDuration?: number
}

const activeAnimations = new WeakMap<Element, Animation[]>()
const runningAnimations = new Set<Animation>()
const animationFallbacks = new WeakMap<Element, number>()

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
}

function cancelAnimations(element: Element) {
  const fallback = animationFallbacks.get(element)
  if (fallback) window.clearTimeout(fallback)
  animationFallbacks.delete(element)
  activeAnimations.get(element)?.forEach((animation) => animation.cancel())
  activeAnimations.delete(element)
}

function animate(
  element: Element,
  keyframes: Keyframe[],
  options: KeyframeAnimationOptions,
  done: () => void,
) {
  cancelAnimations(element)
  if (typeof element.animate !== 'function') {
    done()
    return
  }

  let completed = false
  const finish = () => {
    if (completed) return
    completed = true
    const fallback = animationFallbacks.get(element)
    if (fallback) window.clearTimeout(fallback)
    animationFallbacks.delete(element)
    activeAnimations.delete(element)
    done()
  }

  let animation: Animation
  try {
    animation = element.animate(keyframes, options)
  } catch (_error) {
    finish()
    return
  }

  runningAnimations.add(animation)
  activeAnimations.set(element, [animation])
  const duration =
    typeof options.duration === 'number' ? options.duration : Number(options.duration || 0)
  animationFallbacks.set(
    element,
    window.setTimeout(
      () => {
        animation.cancel()
        runningAnimations.delete(animation)
        finish()
      },
      Math.max(100, duration + 180),
    ),
  )
  animation.finished
    .catch(() => undefined)
    .finally(() => {
      runningAnimations.delete(animation)
      finish()
    })
}

export function useRouteTransition(options: RouteTransitionOptions = {}) {
  const enterY = options.enterY ?? 22
  const leaveY = options.leaveY ?? -10
  const enterDuration = options.enterDuration ?? 220
  const leaveDuration = options.leaveDuration ?? 120

  function beforeEnter(element: Element) {
    const htmlElement = element as HTMLElement
    htmlElement.style.opacity = '0'
    htmlElement.style.transform = prefersReducedMotion() ? 'none' : `translate3d(0, ${enterY}px, 0)`
  }

  function enter(element: Element, done: () => void) {
    const htmlElement = element as HTMLElement
    const reducedMotion = prefersReducedMotion()
    animate(
      element,
      [
        {
          opacity: Number(htmlElement.style.opacity || 0),
          transform: htmlElement.style.transform || 'none',
        },
        { opacity: 1, transform: 'translate3d(0, 0, 0)' },
      ],
      {
        duration: reducedMotion ? 1 : enterDuration,
        easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
        fill: 'forwards',
      },
      () => {
        htmlElement.style.opacity = ''
        htmlElement.style.transform = ''
        done()
      },
    )
  }

  function leave(element: Element, done: () => void) {
    const reducedMotion = prefersReducedMotion()
    animate(
      element,
      [
        { opacity: 1, transform: 'translate3d(0, 0, 0)' },
        {
          opacity: 0,
          transform: reducedMotion ? 'translate3d(0, 0, 0)' : `translate3d(0, ${leaveY}px, 0)`,
        },
      ],
      {
        duration: reducedMotion ? 1 : leaveDuration,
        easing: 'cubic-bezier(0.4, 0, 1, 1)',
        fill: 'forwards',
      },
      done,
    )
  }

  function kill() {
    runningAnimations.forEach((animation) => animation.cancel())
    runningAnimations.clear()
  }

  return {
    beforeEnter,
    enter,
    leave,
    kill,
  }
}
