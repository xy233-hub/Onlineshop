<template>
  <div class="promotion-management">
    <el-card class="promotion-card">
      <template #header>
        <div class="promotion-header">
          <span>优惠营销管理</span>
          <el-button type="primary" @click="handleAddPromotion">添加优惠活动</el-button>
        </div>
      </template>
      
      <el-table :data="promotions" style="width: 100%">
        <el-table-column prop="id" label="活动ID" width="100" />
        <el-table-column prop="name" label="活动名称" />
        <el-table-column prop="type" label="活动类型" width="120">
          <template #default="scope">
            {{ promotionTypeMap[scope.row.type] || scope.row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button 
              v-if="scope.row.status === 'DRAFT'" 
              size="small" 
              @click="handleEditPromotion(scope.row)"
            >
              编辑
            </el-button>
            <el-button 
              v-if="scope.row.status === 'DRAFT' || scope.row.status === 'ENDED'" 
              size="small" 
              type="success"
              @click="handleActivatePromotion(scope.row.id)"
            >
              激活
            </el-button>
            <el-button 
              v-if="scope.row.status === 'ACTIVE'" 
              size="small" 
              type="warning"
              @click="handleEndPromotion(scope.row.id)"
            >
              结束
            </el-button>
            <el-button 
              v-if="scope.row.status === 'DRAFT' || scope.row.status === 'ACTIVE'" 
              size="small" 
              type="danger" 
              @click="handleDeletePromotion(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 优惠活动编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动名称">
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.type" placeholder="请选择活动类型">
            <el-option label="限时折扣" value="DISCOUNT" />
            <el-option label="满减" value="FULL_REDUCTION" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        
        <!-- 折扣设置 -->
        <el-form-item v-if="form.type === 'DISCOUNT'" label="折扣值">
          <el-input-number v-model="form.discountValue" :min="0.1" :max="1" :step="0.01" :precision="2" placeholder="请输入折扣值" />
          <span style="margin-left: 10px">（0.88表示88折）</span>
        </el-form-item>
        
        <!-- 满减设置 -->
        <el-form-item v-if="form.type === 'FULL_REDUCTION'" label="满减规则">
          <el-button type="primary" @click="addFullReductionRule">添加规则</el-button>
          <el-table :data="form.fullReductionRules" style="margin-top: 10px">
            <el-table-column prop="minAmount" label="满金额" width="120">
              <template #default="scope">
                <el-input-number v-model="scope.row.minAmount" :min="0" :step="10" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="reductionAmount" label="减金额" width="120">
              <template #default="scope">
                <el-input-number v-model="scope.row.reductionAmount" :min="0" :step="5" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button size="small" type="danger" @click="removeFullReductionRule(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        
        <el-form-item v-if="form.type === 'FULL_REDUCTION'" label="最高减免">
          <el-input-number v-model="form.maxDiscountAmount" :min="0" :step="10" placeholder="可选" />
        </el-form-item>
        
        <!-- 适用范围 -->
        <el-form-item label="适用范围">
          <el-radio-group v-model="form.applicableScope">
            <el-radio label="ALL">全部商品</el-radio>
            <el-radio label="CATEGORY">指定品类</el-radio>
            <el-radio label="PRODUCT">指定商品</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 选择品类 -->
        <el-form-item v-if="form.applicableScope === 'CATEGORY'" label="选择品类">
          <el-select v-model="form.categoryIds" multiple placeholder="请选择品类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        
        <!-- 选择商品 -->
        <el-form-item v-if="form.applicableScope === 'PRODUCT'" label="选择商品">
          <el-select v-model="form.productIds" multiple placeholder="请选择商品">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSavePromotion">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { sellerProductAPI, categoryAPI, promotionAPI } from '@/api'
export default {
  name: 'PromotionManagement',
  data() {
    return {
      promotions: [],
      categories: [],
      products: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '添加优惠活动',
      form: {
        id: '',
        name: '',
        type: 'DISCOUNT',
        status: 'DRAFT',
        startTime: new Date(),
        endTime: new Date(new Date().getTime() + 7 * 24 * 60 * 60 * 1000),
        discountValue: 0.88,
        minPurchaseAmount: null,
        maxDiscountAmount: null,
        fullReductionRules: [],
        applicableScope: 'ALL',
        categoryIds: [],
        productIds: []
      },
      promotionTypeMap: {
        DISCOUNT: '限时折扣',
        FULL_REDUCTION: '满减'
      }
    }
  },
  async mounted() {
    await this.getPromotions()
    await this.getCategories()
    await this.getProducts()
  },
  watch: {
    'form.type': function(newType) {
      if (newType === 'FULL_REDUCTION' && this.form.fullReductionRules.length === 0) {
        this.addFullReductionRule()
      }
    }
  },
  methods: {
    async getPromotions() {
      try {
        const response = await promotionAPI.getPromotions({
          page: this.currentPage,
          size: this.pageSize
        })
        console.log('获取优惠活动响应:', response)
        
        const data = response?.data?.data
        console.log('响应数据:', data)
        
        if (data && Array.isArray(data.items)) {
          this.promotions = data.items.map(item => ({
            id: item.promotion_id,
            name: item.promotion_name,
            type: item.promotion_type,
            status: item.status,
            startTime: item.start_time,
            endTime: item.end_time,
            discountValue: item.discount_value,
            minPurchaseAmount: item.min_purchase_amount,
            maxDiscountAmount: item.max_discount_amount,
            applicableScope: item.applicable_scope
          }))
          this.total = data.total || 0
          console.log('处理后的促销活动列表:', this.promotions)
        } else {
          this.promotions = []
          this.total = 0
        }
      } catch (error) {
        console.error('获取优惠活动列表失败:', error)
        this.$message.error('获取优惠活动列表失败')
        this.promotions = []
        this.total = 0
      }
    },
    async getCategories() {
      try {
        const response = await categoryAPI.getSellerCategories({ page: 1, size: 100 })
        const data = response?.data?.data
        if (data && Array.isArray(data.items)) {
          this.categories = data.items.map(category => ({
            id: category.category_id,
            name: category.category_name
          }))
        } else {
          this.categories = []
        }
      } catch (error) {
        console.error('获取品类列表失败:', error)
        this.categories = []
      }
    },
    async getProducts() {
      try {
        const response = await sellerProductAPI.getProducts({ page: 1, size: 100 })
        const data = response?.data?.data
        if (data && Array.isArray(data.items)) {
          this.products = data.items.map(product => ({
            id: product.product_id,
            name: product.product_name
          }))
        } else {
          this.products = []
        }
      } catch (error) {
        console.error('获取商品列表失败:', error)
        this.products = []
      }
    },
    handleAddPromotion() {
      this.dialogTitle = '添加优惠活动'
      this.form = {
        id: '',
        name: '',
        type: 'DISCOUNT',
        status: 'DRAFT',
        startTime: new Date(),
        endTime: new Date(new Date().getTime() + 7 * 24 * 60 * 60 * 1000),
        discountValue: 0.88,
        minPurchaseAmount: null,
        maxDiscountAmount: null,
        fullReductionRules: [],
        applicableScope: 'ALL',
        categoryIds: [],
        productIds: []
      }
      this.dialogVisible = true
    },
    handleEditPromotion(promotion) {
      this.dialogTitle = '编辑优惠活动'
      this.form = {
        id: promotion.id,
        name: promotion.name,
        type: promotion.type,
        status: promotion.status,
        startTime: new Date(promotion.startTime),
        endTime: new Date(promotion.endTime),
        discountValue: promotion.discountValue || 0.88,
        minPurchaseAmount: promotion.minPurchaseAmount,
        maxDiscountAmount: promotion.maxDiscountAmount,
        fullReductionRules: [],
        applicableScope: promotion.applicableScope || 'ALL',
        categoryIds: promotion.categoryIds || [],
        productIds: promotion.productIds || []
      }
      this.dialogVisible = true
    },
    handleDeletePromotion(id) {
      this.$confirm('确定要删除这个优惠活动吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await promotionAPI.cancelPromotion(id, '用户删除')
          this.$message.success('删除成功')
          await this.getPromotions()
        } catch (error) {
          console.error('删除失败:', error)
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }).catch(() => {})
    },
    async handleSavePromotion() {
      try {
        if (this.form.type === 'FULL_REDUCTION' && this.form.fullReductionRules.length === 0) {
          this.$message.warning('满减活动请至少添加一条满减规则')
          return
        }

        const targetIds = []
        if (this.form.applicableScope === 'CATEGORY') {
          targetIds.push(...this.form.categoryIds)
        } else if (this.form.applicableScope === 'PRODUCT') {
          targetIds.push(...this.form.productIds)
        }

        const rules = []
        if (this.form.type === 'FULL_REDUCTION' && this.form.fullReductionRules.length > 0) {
          this.form.fullReductionRules.forEach(rule => {
            rules.push({
              rule_type: 'FULL_REDUCTION',
              rule_config: {
                min_purchase_amount: rule.minAmount,
                discount_value: rule.reductionAmount,
                max_discount_amount: this.form.maxDiscountAmount
              }
            })
          })
        }

        const payload = {
          promotion_name: this.form.name,
          promotion_type: this.form.type,
          description: `${this.form.name} - ${this.promotionTypeMap[this.form.type]}`,
          start_time: this.formatDateTime(this.form.startTime),
          end_time: this.formatDateTime(this.form.endTime),
          discount_value: this.form.type === 'DISCOUNT' ? this.form.discountValue : (this.form.fullReductionRules[0]?.reductionAmount || 0),
          min_purchase_amount: this.form.type === 'FULL_REDUCTION' ? (this.form.fullReductionRules[0]?.minAmount || 0) : null,
          max_discount_amount: this.form.maxDiscountAmount,
          applicable_scope: this.form.applicableScope,
          target_ids: targetIds,
          priority: 0,
          rules: rules
        }

        console.log('保存优惠活动 payload:', payload)

        if (this.form.id) {
          await promotionAPI.updatePromotion(this.form.id, payload)
          this.$message.success('更新成功')
        } else {
          await promotionAPI.createPromotion(payload)
          this.$message.success('创建成功')
        }
        
        this.dialogVisible = false
        await this.getPromotions()
      } catch (error) {
        console.error('保存失败:', error)
        console.error('错误详情:', error.response)
        this.$message.error(error.response?.data?.message || '保存失败')
      }
    },
    formatDateTime(date) {
      if (!date) return null
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
    },
    addFullReductionRule() {
      this.form.fullReductionRules.push({ minAmount: 100, reductionAmount: 20 })
    },
    removeFullReductionRule(index) {
      this.form.fullReductionRules.splice(index, 1)
    },
    async handleActivatePromotion(id) {
      this.$confirm('激活后将立即生效，确定要激活此优惠活动吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const loading = this.$loading({
            lock: true,
            text: '正在激活...',
            background: 'rgba(0, 0, 0, 0.7)'
          })
          
          const response = await promotionAPI.activatePromotion(id)
          console.log('激活响应:', response)
          
          loading.close()
          this.$message.success('激活成功')
          
          setTimeout(async () => {
            await this.getPromotions()
            console.log('刷新后的列表:', this.promotions)
          }, 500)
        } catch (error) {
          console.error('激活失败:', error)
          console.error('错误详情:', error.response)
          this.$message.error(error.response?.data?.message || '激活失败')
        }
      }).catch(() => {})
    },
    async handleEndPromotion(id) {
      this.$confirm('结束后优惠活动将不再生效，确定要结束此活动吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await promotionAPI.endPromotion(id)
          this.$message.success('结束成功')
          await this.getPromotions()
        } catch (error) {
          console.error('结束失败:', error)
          this.$message.error(error.response?.data?.message || '结束失败')
        }
      }).catch(() => {})
    },
    getStatusType(status) {
      const typeMap = {
        'DRAFT': 'info',
        'ACTIVE': 'success',
        'ENDED': 'warning',
        'CANCELLED': 'danger'
      }
      return typeMap[status] || 'info'
    },
    getStatusText(status) {
      const textMap = {
        'DRAFT': '草稿',
        'ACTIVE': '生效中',
        'ENDED': '已结束',
        'CANCELLED': '已取消'
      }
      return textMap[status] || status
    },
    async handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      await this.getPromotions()
    },
    async handleCurrentChange(current) {
      this.currentPage = current
      await this.getPromotions()
    }
  }
}
</script>

<style scoped>
.promotion-management {
  padding: 24px;
  min-height: calc(100vh - 160px);
  position: relative;
  z-index: 1;
}

.promotion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.promotion-header span {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #1e293b, #475569);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

:deep(.promotion-card) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 1px solid rgba(224, 230, 237, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

:deep(.promotion-card .el-card__header) {
  background: rgba(248, 250, 252, 0.95);
  border-radius: 20px 20px 0 0;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(224, 230, 237, 0.5);
}

:deep(.promotion-card .el-card__body) {
  padding: 24px;
}

:deep(.promotion-card .el-button--primary) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  border-radius: 12px;
  padding: 10px 24px;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}

:deep(.promotion-card .el-button--primary:hover) {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

:deep(.el-table) {
  border: none;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: rgba(248, 250, 252, 0.95);
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-table td) {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-table__row:hover) {
  background: #f8fafc;
}

:deep(.el-tag) {
  border-radius: 12px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  color: #16a34a;
  border: none;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #2563eb;
  border: none;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #d97706;
  border: none;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #fee2e2, #fecaca);
  color: #dc2626;
  border: none;
}

:deep(.el-table .el-button) {
  padding: 6px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-table .el-button--default) {
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid #e2e8f0;
  color: #475569;
}

:deep(.el-table .el-button--default:hover) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

:deep(.el-table .el-button--success) {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.1), rgba(22, 163, 74, 0.1));
  border: 1px solid rgba(34, 197, 94, 0.2);
  color: #16a34a;
}

:deep(.el-table .el-button--success:hover) {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(22, 163, 74, 0.2));
  border-color: rgba(34, 197, 94, 0.3);
}

:deep(.el-table .el-button--warning) {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.1));
  border: 1px solid rgba(245, 158, 11, 0.2);
  color: #d97706;
}

:deep(.el-table .el-button--warning:hover) {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(217, 119, 6, 0.2));
  border-color: rgba(245, 158, 11, 0.3);
}

:deep(.el-table .el-button--danger) {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(249, 115, 22, 0.1));
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

:deep(.el-table .el-button--danger:hover) {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(249, 115, 22, 0.2));
  border-color: rgba(239, 68, 68, 0.3);
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

:deep(.el-pagination) {
  font-size: 13px;
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  margin: 0 4px;
  padding: 4px 12px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}

:deep(.el-pagination button) {
  border-radius: 8px;
}

:deep(.el-pagination .el-pagination__sizes .el-select .el-input .el-input__inner) {
  border-radius: 8px;
}

:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

:deep(.el-dialog__header) {
  background: rgba(248, 250, 252, 0.95);
  padding: 20px 24px;
  border-bottom: 1px solid rgba(224, 230, 237, 0.5);
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}

:deep(.el-input__inner),
:deep(.el-select__input) {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 12px 16px;
  font-size: 14px;
  transition: all 0.3s ease;
}

:deep(.el-input__inner:focus),
:deep(.el-select__input:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

:deep(.el-radio) {
  margin-right: 20px;
  font-size: 14px;
  color: #334155;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: #3b82f6;
}

:deep(.el-date-editor) {
  width: 100%;
}

:deep(.el-date-editor .el-input__inner) {
  border-radius: 12px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  background: rgba(248, 250, 252, 0.95);
}

:deep(.dialog-footer .el-button) {
  padding: 10px 24px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
}

:deep(.dialog-footer .el-button--default) {
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid #e2e8f0;
  color: #475569;
}

:deep(.dialog-footer .el-button--default:hover) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

:deep(.dialog-footer .el-button--primary) {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

:deep(.dialog-footer .el-button--primary:hover) {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

:deep(.el-input-number) {
  width: 180px;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  border-radius: 0 12px 12px 0;
}

:deep(.el-input-number__decrease) {
  border-radius: 12px 0 0 12px;
}
</style>