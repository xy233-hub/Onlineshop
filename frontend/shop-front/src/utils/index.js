// 格式化时间
export const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 格式化价格
export const formatPrice = (price) => {
  return '¥' + parseFloat(price).toFixed(2)
}

// 防抖函数
export const debounce = (func, wait) => {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

// 验证手机号
export const validatePhone = (phone) => {
  const reg = /^1[3-9]\d{9}$|^(0\d{2,3}-?)?\d{7,8}$/
  return reg.test(phone)
}

// 验证邮箱
export const validateEmail = (email) => {
  const reg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return reg.test(email)
}       