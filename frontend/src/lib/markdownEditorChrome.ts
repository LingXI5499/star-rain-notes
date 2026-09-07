/**
 * Admin Vditor chrome helpers.
 *
 * Vditor outline jumps use window.scrollTo when height === "auto", but the
 * admin shell scrolls .admin-shell__content — so height must be numeric.
 * Fullscreen also cannot outrank the sidebar stacking context (z-index 30),
 * so we flag <html> and hide admin chrome via CSS.
 */

/** Prefer a tall editor; never below 640 so IR stays scrollable. */
export function resolveVditorEditorHeight(viewportHeight: number): number {
  return Math.max(640, viewportHeight - 280)
}

export function setVditorFullscreenActive(active: boolean): void {
  document.documentElement.classList.toggle('is-vditor-fullscreen', active)
}
