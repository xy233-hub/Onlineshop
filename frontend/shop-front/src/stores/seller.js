import { defineStore } from 'pinia'

const parseJSON = (raw) => {
    if (!raw) return null
    try { return JSON.parse(raw) } catch { return null }
}

const getStoredSeller = () => {
    return parseJSON(localStorage.getItem('seller_info'))
}

export const useSellerStore = defineStore('seller', {
  state: () => ({
    seller: getStoredSeller(),
    token: localStorage.getItem('seller_token') || null
  }),

  getters: {
    isLoggedIn: (state) => {
      const token = typeof state.token === 'string' ? state.token.trim() : ''
      return !!token
    }
  },
  
  actions: {
    login(token, sellerInfo) {
      this.token = token
      this.seller = sellerInfo
      localStorage.setItem('seller_token', token)
      if (sellerInfo) {
        try { localStorage.setItem('seller_info', JSON.stringify(sellerInfo)) } catch (e) {}
      }
    },
    
    logout() {
      this.token = null
      this.seller = null
      localStorage.removeItem('seller_token')
      localStorage.removeItem('seller_info')
    },

    setSellerInfo(info) {
      this.seller = info
      if (info) {
        try { localStorage.setItem('seller_info', JSON.stringify(info)) } catch (e) {}
      }
    },
    
    updatePassword() {
    }
  }
})