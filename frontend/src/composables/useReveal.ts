import { onBeforeUnmount, onMounted } from 'vue'

/**
 * Lightweight IntersectionObserver reveal for sections/cards.
 * Adds `.is-revealed` to `[data-reveal]` elements when they enter the viewport;
 * de-dupes per element (unobserve after reveal). Reduced-motion is handled in
 * CSS (public-theme.css). No dependencies, per spec §7.3–7.4.
 */
export function useReveal(selector = '[data-reveal]') {
  let observer: IntersectionObserver | null = null
  let nodes: NodeListOf<HTMLElement> = document.querySelectorAll<HTMLElement>(selector)

  const stop = () => {
    observer?.disconnect()
    observer = null
  }

  onMounted(() => {
    nodes = document.querySelectorAll<HTMLElement>(selector)
    if (!nodes.length || typeof IntersectionObserver === 'undefined') return
    observer = new IntersectionObserver(
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
    nodes.forEach((node) => observer!.observe(node))
  })

  onBeforeUnmount(stop)
}
