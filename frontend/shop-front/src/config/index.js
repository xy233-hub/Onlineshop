// 应用配置
export const APP_CONFIG = {
  appName: '在线购物系统',
  version: '1.0.0',
  apiTimeout: 10000,
  pageSize: 10
}

// 商品状态映射
export const PRODUCT_STATUS = {
  online: { text: '在售', type: 'success' },
  frozen: { text: '冻结', type: 'warning' },
  sold: { text: '已售', type: 'info' }
}

// 订单状态映射
export const ORDER_STATUS = {
  pending: { text: '待处理', type: 'warning' },
  success: { text: '成功', type: 'success' },
  failed: { text: '失败', type: 'danger' }
}