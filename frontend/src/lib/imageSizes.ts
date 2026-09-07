/** Shared responsive-image sizes hints for portfolio surfaces. */
export type ImageSurface = 'hero' | 'gallery' | 'thumb' | 'list'

export function imageSizes(surface: ImageSurface): string {
  switch (surface) {
    case 'hero':
      return '(max-width: 900px) 100vw, min(1360px, 92vw)'
    case 'gallery':
      return '(max-width: 720px) 100vw, min(1100px, 90vw)'
    case 'thumb':
      return '96px'
    case 'list':
      return '(max-width: 600px) 100vw, 180px'
  }
}
