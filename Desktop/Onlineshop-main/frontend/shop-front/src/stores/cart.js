// src/stores/cart.js
import { defineStore } from 'pinia'
import { cartAPI } from '@/api'
import { useCustomerStore } from './customer'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 购物车商品列表
    cartItems: [],
    // 加载状态
    loading: false
  }),

  getters: {
    // 获取购物车商品数量
    cartCount: (state) => state.cartItems.reduce((total, item) => total + item.quantity, 0),
    // 获取购物车商品总价
    cartTotal: (state) => state.cartItems.reduce((total, item) => {
      return total + (item.product.price * item.quantity)
    }, 0),
    // 检查商品是否已在购物车
    isInCart: (state) => (productId) => {
      return state.cartItems.some(item => item.product.product_id === productId)
    },
    // 根据商品ID获取购物车中的商品
    getCartItem: (state) => (productId) => {
      return state.cartItems.find(item => item.product.product_id === productId)
    }
  },

  actions: {
    // 从本地存储加载购物车列表
    loadFromLocalStorage() {
      try {
        const stored = localStorage.getItem('user_cart')
        if (stored) {
          this.cartItems = JSON.parse(stored)
        }
      } catch (e) {
        console.error('加载购物车列表失败', e)
        this.cartItems = []
      }
    },

    // 保存购物车列表到本地存储
    saveToLocalStorage() {
      try {
        localStorage.setItem('user_cart', JSON.stringify(this.cartItems))
      } catch (e) {
        console.error('保存购物车列表失败', e)
      }
    },

    // 获取购物车列表
    async getCartItems() {
      this.loading = true
      try {
        const response = await cartAPI.getCartItems()
        this.cartItems = response.data || []
        this.saveToLocalStorage()
      } catch (error) {
        console.error('Failed to load cart items:', error)
        this.cartItems = []
        this.loadFromLocalStorage()
      } finally {
        this.loading = false
      }
    },

    // 添加商品到购物车
    async addToCart(product, quantity = 1) {
      const customerStore = useCustomerStore()
      const existingItem = this.cartItems.find(item => item.product.product_id === product.product_id)
      
      if (existingItem) {
        // 如果商品已在购物车中，更新数量
        existingItem.quantity += quantity
        await this.updateCartItem(existingItem.cart_item_id, { quantity: existingItem.quantity })
      } else {
        // 如果商品不在购物车中，添加新商品
        try {
          this.loading = true
          // 调用后端API添加到购物车
          const response = await cartAPI.addToCart({
            customer_id: customerStore.customer?.customer_id,
            product_id: product.product_id,
            quantity: quantity
          })
          
          if (response.data) {
            this.cartItems.push({
              cart_item_id: response.data.cart_item_id,
              product: product,
              quantity: quantity
            })
            this.saveToLocalStorage()
          }
        } catch (error) {
          console.error('Failed to add to cart:', error)
        } finally {
          this.loading = false
        }
      }
    },

    // 从购物车移除商品
    async removeCartItem(cartItemId) {
      try {
        this.loading = true
        await cartAPI.removeCartItem(cartItemId)
        this.cartItems = this.cartItems.filter(item => item.cart_item_id !== cartItemId)
        this.saveToLocalStorage()
        return true
      } catch (error) {
        console.error('从购物车移除商品失败', error)
        return false
      } finally {
        this.loading = false
      }
    },

    // 更新购物车商品数量
    async updateCartItem(cartItemId, data) {
      try {
        this.loading = true
        await cartAPI.updateCartItem(cartItemId, data)
        // 更新本地状态
        const item = this.cartItems.find(item => item.cart_item_id === cartItemId)
        if (item) {
          Object.assign(item, data)
          this.saveToLocalStorage()
        }
        return true
      } catch (error) {
        console.error('Failed to update cart item:', error)
        return false
      } finally {
        this.loading = false
      }
    },

    // 清空购物车
    async clearCart() {
      const customerStore = useCustomerStore()
      try {
        this.loading = true
        // 批量删除购物车商品
        await cartAPI.removeCartItems({
          customer_id: customerStore.customer?.customer_id,
          cart_item_ids: this.cartItems.map(item => item.cart_item_id)
        })
        this.cartItems = []
        this.saveToLocalStorage()
        return true
      } catch (error) {
        console.error('Failed to clear cart:', error)
        // 如果批量删除失败，尝试逐个删除
        for (const item of this.cartItems) {
          await this.removeCartItem(item.cart_item_id)
        }
      } finally {
        this.loading = false
      }
    }
  }
})