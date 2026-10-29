import { importEditorImage, prepareEditorImage, uploadMedia } from '../../modules/media/api/mediaApi'
import { IMAGE_MAX_BYTES, validateEditorImage } from '../../modules/media/support/upload'
import { markdownImages, imageEdit } from './commands/editorDocument'

/** One insertion always resolves to a registered public asset, including library selections. */
export async function prepareImageUrl(value, { signal } = {}) {
  const url = new URL(value, window.location.origin)
  const id = url.origin === window.location.origin && url.pathname.match(/^\/api\/media\/assets\/(\d+)\/content$/)?.[1]
  if (id) return prepareEditorImage(id)
  if (!['http:', 'https:', 'data:', 'blob:'].includes(url.protocol)) throw new Error('请通过上传或拖拽插入本地图片。')
  const external = ['http:', 'https:'].includes(url.protocol) && url.origin !== window.location.origin
  let response
  try {
    response = await fetch(url.href, {
      signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(10000)]) : AbortSignal.timeout(10000),
      credentials: url.origin === window.location.origin ? 'same-origin' : 'omit',
    })
    if (!response.ok && external) throw new Error('Image host unavailable')
  } catch (cause) {
    if (signal?.aborted || !external) throw cause
    // Servers without browser CORS support use the bounded server-side importer.
    return importEditorImage(url.href, { signal })
  }
  if (!response.ok) throw new Error('图片无法读取，请直接上传原图。')
  if (Number(response.headers.get('content-length')) > IMAGE_MAX_BYTES) throw new Error('图片不能超过 20 MB。')
  const blob = await response.blob()
  const extensions = { 'image/png':'png', 'image/jpeg':'jpg', 'image/gif':'gif', 'image/webp':'webp', 'image/bmp':'bmp', 'image/tiff':'tiff', 'image/avif':'avif' }
  const extension = extensions[blob.type.toLowerCase()]
  if (!extension) throw new Error('请选择支持的图片格式。')
  const file = new File([blob], `inserted-image.${extension}`, { type: blob.type })
  const invalid = validateEditorImage(file)
  if (invalid) throw new Error(invalid)
  return uploadMedia(file, 'PUBLIC', { signal })
}

export async function prepareImageMarkdown(source, options = {}) {
  const images = markdownImages(source), urls = new Map()
  for (const image of images) {
    if (!urls.has(image.url)) urls.set(image.url, await prepareImageUrl(image.url, options))
  }
  for (const image of images.reverse()) {
    const asset = urls.get(image.url)
    const edit = imageEdit(image, { ...image, url: asset.contentUrl })
    source = source.slice(0, edit.start) + edit.replacement + source.slice(edit.end)
  }
  return source
}
