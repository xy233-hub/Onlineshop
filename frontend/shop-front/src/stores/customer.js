import { defineStore } from 'pinia'

export const useCustomerStore = defineStore('customer', {
    state: () => ({
        customer: null,
        token: localStorage.getItem('customer_token') || null
    }),
    actions: {
        login(token, customerInfo = null) {
            this.token = token
            this.customer = customerInfo
            localStorage.setItem('customer_token', token)
            if (customerInfo) {
                try { localStorage.setItem('customer_info', JSON.stringify(customerInfo)) } catch (e) {}
            }
        },
        logout() {
            this.token = null
            this.customer = null
            localStorage.removeItem('customer_token')
            localStorage.removeItem('customer_info')
            localStorage.removeItem('customer')
            localStorage.removeItem('customer_id')
        },
        setCustomerInfo(info) {
            this.customer = info
            try { localStorage.setItem('customer_info', JSON.stringify(info)) } catch (e) {}
        }
    }
})