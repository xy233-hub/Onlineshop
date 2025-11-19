import { defineStore } from 'pinia'

export const useSellerStore = defineStore('seller', {
  state: () => ({
    seller: null,
    token: localStorage.getItem('seller_token') || null
  }),
  
  actions: {
    login(token, sellerInfo) {
      this.token = token
      this.seller = sellerInfo
      localStorage.setItem('seller_token', token)
    },
    
    logout() {
      this.token = null
      this.seller = null
      localStorage.removeItem('seller_token')
    },
    
    updatePassword() {
      // 密码更新逻辑
    }
  }
})