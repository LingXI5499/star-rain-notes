export function openDateTimePicker(event) {
  const input = event.currentTarget
  if (typeof input.showPicker !== 'function') return
  try {
    input.showPicker()
  } catch {
    input.focus()
  }
}
