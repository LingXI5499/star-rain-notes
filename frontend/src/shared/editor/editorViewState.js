export function outlinePreferenceKey(context) {
  return `star-rain-editor-outline:${context?.accountId ?? 'guest'}:${context?.module || 'general'}:${context?.field || 'body'}`
}
export function readOutlinePreference(key,fallback) {
  try{const value=localStorage.getItem(key);return value==='show'?true:value==='hide'?false:fallback}catch{return fallback}
}
export function saveOutlinePreference(key,visible){try{localStorage.setItem(key,visible?'show':'hide')}catch{}}

export function readLayoutPreference(key) {
  try {
    const value = JSON.parse(localStorage.getItem(`${key}:layout`))
    return {
      outlineWidth: Number.isFinite(value?.outlineWidth) ? Math.max(160, Math.min(480, value.outlineWidth)) : 224,
      canvasWidth: Number.isFinite(value?.canvasWidth) && value.canvasWidth >= 480 ? Math.min(1600, value.canvasWidth) : 0,
    }
  } catch { return { outlineWidth: 224, canvasWidth: 0 } }
}
export function saveLayoutPreference(key, value) {
  try { localStorage.setItem(`${key}:layout`, JSON.stringify(value)) } catch {}
}

// One scan and one deferred update for both counters; it never delays document input or saving.
export function countEditorText(value){
  const source=String(value ?? '');let words=0,lines=source?1:0
  for(const match of source.matchAll(/[\u4e00-\u9fff\u3400-\u4dbf]|[A-Za-z0-9_'-]+|\n/g)){
    if(match[0]==='\n')lines++;else words++
  }
  return {words,lines}
}
