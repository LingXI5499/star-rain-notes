import { nextTick, onBeforeUnmount, onMounted, type Ref } from 'vue'

/**
 * Lightweight IntersectionObserver reveal for sections/cards.
 * Adds `.is-revealed` to `[data-reveal]` elements when they enter the viewport;
 * de-dupes per element (unobserve after reveal). Reduced-motion is handled in
 * CSS (public-theme.css). No dependencies, per spec §7.3–7.4.
 */
export function useReveal(root?: Ref<HTMLElement | null>, selector = '[data-reveal]') {
  let observer: IntersectionObserver | null = null

  const stop = () => {
    observer?.disconnect()
    observer = null
  }

  const refresh = async () => {
    await nextTick()
    const scope = root?.value ?? document
    const nodes = scope.querySelectorAll<HTMLElement>(selector)
    if (!nodes.length || typeof IntersectionObserver === 'undefined') return
    observer ??= new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed')
            observer?.unobserve(entry.target)
          }
        }
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    )
    nodes.forEach((node) => {
      if (!node.classList.contains('is-revealed')) observer!.observe(node)
    })
  }

  onMounted(refresh)

  onBeforeUnmount(stop)
  return { refresh }
}
