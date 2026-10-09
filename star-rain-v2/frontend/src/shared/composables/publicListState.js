export function publicPage(value) { const n = Number(value); return Number.isSafeInteger(n) && n > 0 && n <= 100000 ? n : 1 }
export function publicPageSize(value, defaultSize = 12, sizes = [12, 24, 48]) { const n = Number(value); return sizes.includes(n) ? n : defaultSize }
export function sizeQuery(query, size, defaultSize) { return { ...query, page: undefined, pageSize: size === defaultSize ? undefined : String(size) } }
