import { defineStore } from 'pinia'

const parseJSON = (raw) => {
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch {
        return null
    }
}

const getStoredCustomer = () => {
    const info = parseJSON(localStorage.getItem('customer_info'))
    if (info && typeof info === 'object') return info

    const legacy = parseJSON(localStorage.getItem('customer'))
    if (legacy && typeof legacy === 'object') return legacy

    return null
}

const normalizeCustomerId = (customer) => {
    if (!customer || typeof customer !== 'object') return null
    return customer.customer_id ?? customer.id ?? customer.user_id ?? null
}

export const useCustomerStore = defineStore('customer', {
    state: () => ({
        customer: getStoredCustomer(),
        token: localStorage.getItem('customer_token') || null
    }),
    getters: {
        isLoggedIn: (state) => {
            const token = typeof state.token === 'string' ? state.token.trim() : ''
            return !!token
        },
        customerId: (state) => {
            const fromState = normalizeCustomerId(state.customer)
            if (fromState !== null && fromState !== undefined && String(fromState).trim()) return fromState

            const fromInfo = normalizeCustomerId(getStoredCustomer())
            if (fromInfo !== null && fromInfo !== undefined && String(fromInfo).trim()) return fromInfo

            const legacyId = localStorage.getItem('customer_id')
            return legacyId && legacyId.trim() ? legacyId.trim() : null
        }
    },
    actions: {
        login(token, customerInfo = null) {
            this.token = token
            this.customer = customerInfo
            localStorage.setItem('customer_token', token)
            if (customerInfo) {
                try { localStorage.setItem('customer_info', JSON.stringify(customerInfo)) } catch (e) {}
                const customerId = normalizeCustomerId(customerInfo)
                if (customerId !== null && customerId !== undefined && String(customerId).trim()) {
                    localStorage.setItem('customer_id', String(customerId).trim())
                }
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
            const customerId = normalizeCustomerId(info)
            if (customerId !== null && customerId !== undefined && String(customerId).trim()) {
                localStorage.setItem('customer_id', String(customerId).trim())
            }
        }
    }
})