export function mediaImage(url, width = 720) {
  if (!url || !/^(?:https?:\/\/[^/]+)?\/api\/media\/assets\/\d+\/content$/.test(url)) return url
  return url.replace(/\/content$/, `/thumbnail?width=${width}`)
}
export function mediaImageSet(url) {
  return [360, 720, 1080].map(width => `${mediaImage(url, width)} ${width}w`).join(', ')
}
