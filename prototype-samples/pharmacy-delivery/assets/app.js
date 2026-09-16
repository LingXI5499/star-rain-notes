(() => {
  const $ = (selector, scope = document) => scope.querySelector(selector)
  const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)]
  const toastBox = $('[data-toast-box]')
  let toastTimer
  const toast = (message) => {
    if (!toastBox) return
    toastBox.textContent = message
    toastBox.classList.add('show')
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => toastBox.classList.remove('show'), 2200)
  }

  $('[data-menu]')?.addEventListener('click', () => $('[data-nav]')?.classList.toggle('open'))
  $$('[data-toast]').forEach((button) => button.addEventListener('click', () => toast(button.dataset.toast)))

  let cartCount = Number(localStorage.getItem('prototype-cart-count') || 2)
  const updateCart = () => $$('[data-cart-count]').forEach((node) => { node.textContent = cartCount })
  updateCart()
  $$('[data-add]').forEach((button) => button.addEventListener('click', () => {
    cartCount += 1
    localStorage.setItem('prototype-cart-count', cartCount)
    updateCart()
    toast('已加入购物车')
    button.textContent = '✓'
    setTimeout(() => { button.textContent = '＋' }, 900)
  }))

  const products = $$('[data-category]')
  const filterButtons = $$('[data-filter]')
  const search = $('[data-search]')
  let category = 'all'
  const filterProducts = () => {
    const keyword = search?.value.trim().toLowerCase() || ''
    let visible = 0
    products.forEach((card) => {
      const matchCategory = category === 'all' || card.dataset.category === category
      const matchKeyword = !keyword || (card.dataset.name || '').toLowerCase().includes(keyword)
      card.hidden = !(matchCategory && matchKeyword)
      if (!card.hidden) visible += 1
    })
    const empty = $('[data-empty]')
    if (empty) empty.hidden = visible !== 0
  }
  filterButtons.forEach((button) => button.addEventListener('click', () => {
    filterButtons.forEach((item) => item.classList.remove('active'))
    button.classList.add('active')
    category = button.dataset.filter
    filterProducts()
  }))
  search?.addEventListener('input', filterProducts)

  $$('[data-qty]').forEach((button) => button.addEventListener('click', () => {
    const value = $('b', button.parentElement)
    value.textContent = Math.max(1, Number(value.textContent) + Number(button.dataset.qty))
  }))
  $$('[data-remove]').forEach((button) => button.addEventListener('click', () => {
    button.closest('.cart-item')?.remove()
    cartCount = Math.max(0, cartCount - 1)
    localStorage.setItem('prototype-cart-count', cartCount)
    updateCart()
    toast('商品已移除')
  }))
})()
