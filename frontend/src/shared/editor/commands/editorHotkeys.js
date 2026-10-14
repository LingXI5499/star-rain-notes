export const defaultHeadingKeys = Object.fromEntries(Array.from({ length: 7 }, (_, level) => [`heading${level}`, `Alt+${level}`]))
export function keyChord(event) {
  const key = /^Digit[0-6]$/.test(event.code) ? event.code.slice(-1) : event.key.length === 1 ? event.key.toUpperCase() : event.key
  return [event.ctrlKey && 'Ctrl', event.altKey && 'Alt', event.metaKey && 'Meta', event.shiftKey && 'Shift', key].filter(Boolean).join('+')
}
export function headingHotkey(event, keys = defaultHeadingKeys, composing = false) {
  if (composing || event.isComposing || event.keyCode === 229 || event.repeat || event.getModifierState?.('AltGraph')) return null
  if (event.ctrlKey && event.altKey) return null
  const entry = Object.entries(keys).find(([, key]) => key.toUpperCase() === keyChord(event).toUpperCase())
  return entry ? Number(entry[0].slice(7)) : null
}
export function validateHeadingKeys(keys) {
  const values = Object.values(keys)
  if (values.length !== 7 || new Set(values.map(value => value.toUpperCase())).size !== 7) return '七个操作必须配置不同的快捷键。'
  if (values.some(value => !/^(Alt\+[0-9A-Z]|Ctrl\+Shift\+[A-Z])$/i.test(value))) return '仅支持 Alt+数字/字母或 Ctrl+Shift+字母。'
  if (values.some(value => /^(Alt\+[DFE]|Ctrl\+Shift\+[BJNOPTW])$/i.test(value))) return '该组合可能是浏览器保留键，请选择其它键。'
  return ''
}
