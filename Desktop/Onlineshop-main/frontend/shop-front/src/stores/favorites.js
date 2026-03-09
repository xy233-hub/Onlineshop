// src/stores/favorites.js
import { defineStore } from 'pinia'

export const useFavoritesStore = defineStore('favorites', {
  state: () => ({
    // 收藏商品列表
    favoritesList: [],
    // 加载状态
    loading: false
  }),

  getters: {
    // 获取收藏商品数量
    favoritesCount: (state) => state.favoritesList.length,
    // 检查商品是否已收藏
    isFavorited: (state) => (productId) => {
      return state.favoritesList.some(item => item.product_id === productId)
    }
  },

  actions: {
    // 从本地存储加载收藏列表
    loadFromLocalStorage() {
      try {
        const stored = localStorage.getItem('user_favorites')
        if (stored) {
          this.favoritesList = JSON.parse(stored)
        }
      } catch (e) {
        console.error('加载收藏列表失败', e)
        this.favoritesList = []
      }
    },

    // 保存收藏列表到本地存储
    saveToLocalStorage() {
      try {
        localStorage.setItem('user_favorites', JSON.stringify(this.favoritesList))
      } catch (e) {
        console.error('保存收藏列表失败', e)
      }
    },

    // 添加商品到收藏
    addToFavorites(product) {
      // 检查商品是否已收藏
      if (this.isFavorited(product.product_id)) {
        return false // 已收藏，不重复添加
      }

      // 添加到收藏列表
      this.favoritesList.push({
        product_id: product.product_id,
        product_name: product.product_name,
        price: product.price,
        image_url: product.image_url || product.product_image,
        stock_quantity: product.stock_quantity,
        product_status: product.product_status,
        favorited_at: new Date().toISOString()
      })

      // 保存到本地存储
      this.saveToLocalStorage()
      return true
    },

    // 从收藏中移除商品
    removeFromFavorites(productId) {
      const index = this.favoritesList.findIndex(item => item.product_id === productId)
      if (index !== -1) {
        this.favoritesList.splice(index, 1)
        this.saveToLocalStorage()
        return true
      }
      return false
    },

    // 切换商品收藏状态
    toggleFavorite(product) {
      if (this.isFavorited(product.product_id)) {
        return this.removeFromFavorites(product.product_id)
      } else {
        return this.addToFavorites(product)
      }
    },

    // 清空收藏列表
    clearFavorites() {
      this.favoritesList = []
      this.saveToLocalStorage()
    }
  }
})
