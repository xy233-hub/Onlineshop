<template>
  <div class="promotion-management">
    <el-card>
      <template #header>
        <div class="promotion-header">
          <span>优惠营销管理</span>
          <el-button type="primary" @click="openCreateDialog">创建促销活动</el-button>
        </div>
      </template>

      <div class="filter-row">
        <el-select
            v-model="filters.status"
            clearable
            placeholder="按状态筛选"
            style="width: 180px"
            @change="fetchPromotions(1)"
        >
          <el-option label="草稿" value="DRAFT" />
          <el-option label="进行中" value="ACTIVE" />
          <el-option label="已结束" value="ENDED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>

        <el-select
            v-model="filters.promotion_type"
            clearable
            placeholder="按类型筛选"
            style="width: 180px"
            @change="fetchPromotions(1)"
        >
          <el-option v-for="rule in ruleDefinitions" :key="rule.rule_type" :label="rule.name" :value="rule.rule_type" />
        </el-select>
      </div>

      <el-table :data="promotions" v-loading="loading" style="width: 100%">
        <el-table-column prop="promotion_id" label="活动ID" width="100" />
        <el-table-column prop="promotion_name" label="活动名称" min-width="180" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ promotionTypeText(row.promotion_type) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ promotionStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="discount_value" label="优惠值" width="100" />
        <el-table-column prop="start_time" label="开始时间" width="180" />
        <el-table-column prop="end_time" label="结束时间" width="180" />
        <el-table-column prop="affected_products_count" label="影响商品" width="100" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)" :disabled="row.status !== 'DRAFT'">编辑</el-button>
            <el-button size="small" type="success" @click="activate(row)" :disabled="row.status === 'ACTIVE'">激活</el-button>
            <el-button size="small" type="warning" @click="endPromotion(row)" :disabled="row.status !== 'ACTIVE'">结束</el-button>
            <el-button
                size="small"
                type="danger"
                @click="cancelPromotion(row)"
                :disabled="!(row.status === 'DRAFT' || row.status === 'ACTIVE')"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="活动名称">
          <el-input v-model="form.promotion_name" placeholder="请输入活动名称" />
        </el-form-item>

        <el-form-item label="活动类型">
          <el-select v-model="form.promotion_type" style="width: 100%" @change="handleTypeChange">
            <el-option v-for="rule in ruleDefinitions" :key="rule.rule_type" :label="rule.name" :value="rule.rule_type" />
          </el-select>
        </el-form-item>

        <el-form-item label="活动描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>

        <el-form-item label="开始时间">
          <el-date-picker
              v-model="form.start_time"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间">
          <el-date-picker
              v-model="form.end_time"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="折扣率" v-if="form.promotion_type === 'DISCOUNT'">
          <el-input-number v-model="form.discount_value" :min="0.01" :max="1" :step="0.01" :precision="2" />
          <span class="inline-hint">0.80 表示 8 折</span>
        </el-form-item>

        <template v-else-if="form.promotion_type === 'FULL_REDUCTION'">
          <el-form-item label="减免金额">
            <el-input-number v-model="form.discount_value" :min="0.01" :step="1" :precision="2" />
          </el-form-item>
          <el-form-item label="最低消费金额">
            <el-input-number v-model="form.min_purchase_amount" :min="0" :step="1" :precision="2" />
          </el-form-item>
          <el-form-item label="最大优惠金额">
            <el-input-number v-model="form.max_discount_amount" :min="0" :step="1" :precision="2" />
          </el-form-item>
        </template>

        <template v-else>
           <!-- 兼容扩展的 CUSTOM 或其他规则，默认提供一个折扣数值输入以便满足基本参数 -->
           <el-form-item label="配置值">
             <el-input-number v-model="form.discount_value" :min="0" :step="1" :precision="2" />
             <span class="inline-hint">请输入数值配置，具体功能视所选规则决定</span>
           </el-form-item>
        </template>

        <el-form-item label="适用范围">
          <el-radio-group v-model="form.applicable_scope">
            <el-radio label="ALL">全部商品</el-radio>
            <el-radio label="CATEGORY">指定品类</el-radio>
            <el-radio label="PRODUCT">指定商品</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.applicable_scope === 'CATEGORY'" label="目标品类">
          <el-select v-model="form.target_ids" multiple filterable style="width: 100%" placeholder="请选择品类">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.applicable_scope === 'PRODUCT'" label="目标商品">
          <el-select v-model="form.target_ids" multiple filterable style="width: 100%" placeholder="请选择商品">
            <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :step="1" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePromotion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { promotionAPI, sellerProductAPI, categoryAPI } from '@/api'

const defaultForm = () => ({
  promotion_id: null,
  promotion_name: '',
  promotion_type: 'DISCOUNT',
  description: '',
  start_time: '',
  end_time: '',
  discount_value: 0.8,
  min_purchase_amount: 0,
  max_discount_amount: null,
  applicable_scope: 'ALL',
  target_ids: [],
  priority: 0,
  rules: []
})

export default {
  name: 'PromotionManagement',
  data() {
    return {
      loading: false,
      promotions: [],
      categories: [],
      products: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      filters: {
        status: '',
        promotion_type: ''
      },
      dialogVisible: false,
      dialogTitle: '创建促销活动',
      form: defaultForm(),
      ruleDefinitions: []
    }
  },
  async mounted() {
    await Promise.all([this.fetchCategories(), this.fetchProducts(), this.fetchRuleDefinitions()])
    this.fetchPromotions(1)
  },
  methods: {
    promotionTypeText(type) {
      if (!type) return type;
      const matched = this.ruleDefinitions.find(r => r.rule_type === type);
      if (matched) return matched.name;
      return { DISCOUNT: '限时折扣', FULL_REDUCTION: '满减', COUPON: '优惠券', FLASH_SALE: '秒杀' }[type] || type
    },
    handleTypeChange(val) {
      if (val === 'DISCOUNT') {
        this.form.discount_value = 0.8;
      } else if (val === 'FULL_REDUCTION') {
        this.form.discount_value = 0;
        this.form.min_purchase_amount = 0;
        this.form.max_discount_amount = null;
      }
    },
    promotionStatusText(status) {
      return { DRAFT: '草稿', ACTIVE: '进行中', ENDED: '已结束', CANCELLED: '已取消' }[status] || status
    },
    statusTagType(status) {
      return { DRAFT: 'info', ACTIVE: 'success', ENDED: 'warning', CANCELLED: 'danger' }[status] || 'info'
    },
    async fetchPromotions(page = this.currentPage) {
      this.loading = true
      this.currentPage = page
      try {
        const params = {
          page: this.currentPage,
          size: this.pageSize,
          status: this.filters.status || undefined,
          promotion_type: this.filters.promotion_type || undefined
        }
        const res = await promotionAPI.getPromotions(params)
        const data = res?.data?.data || {}
        this.promotions = Array.isArray(data.items) ? data.items : []
        this.total = Number(data.total || 0)
      } catch (e) {
        ElMessage.error(e?.response?.data?.message || '获取促销活动失败')
      } finally {
        this.loading = false
      }
    },
    async fetchRuleDefinitions() {
      try {
        const res = await promotionAPI.getRuleDefinitions()
        this.ruleDefinitions = Array.isArray(res?.data?.data) ? res.data.data : []
      } catch (e) {
        console.error('Failed to fetch rule definitions', e);
        this.ruleDefinitions = [
          { rule_type: 'DISCOUNT', name: '限时折扣' },
          { rule_type: 'FULL_REDUCTION', name: '满减' }
        ]
      }
    },
    async fetchCategories() {
      try {
        const res = await categoryAPI.getSellerCategories({ page: 1, size: 200 })
        const data = res?.data?.data
        const items = Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : [])
        this.categories = items.map(item => ({ id: item.category_id, name: item.category_name }))
      } catch (e) {
        this.categories = []
      }
    },
    async fetchProducts() {
      try {
        const res = await sellerProductAPI.getProducts({ page: 1, size: 200 })
        const data = res?.data?.data
        const items = Array.isArray(data?.items) ? data.items : (Array.isArray(data) ? data : [])
        this.products = items.map(item => ({ id: item.product_id, name: item.product_name }))
      } catch (e) {
        this.products = []
      }
    },
    openCreateDialog() {
      this.dialogTitle = '创建促销活动'
      this.form = defaultForm()
      this.dialogVisible = true
    },
    async openEditDialog(row) {
      try {
        const res = await promotionAPI.getPromotionDetail(row.promotion_id)
        const detail = res?.data?.data
        if (!detail) {
          ElMessage.error('活动详情不存在')
          return
        }

        this.dialogTitle = '编辑促销活动'
        this.form = {
          promotion_id: detail.promotion_id,
          promotion_name: detail.promotion_name,
          promotion_type: detail.promotion_type,
          description: detail.description || '',
          start_time: detail.start_time,
          end_time: detail.end_time,
          discount_value: Number(detail.discount_value || 0),
          min_purchase_amount: detail.min_purchase_amount == null ? 0 : Number(detail.min_purchase_amount),
          max_discount_amount: detail.max_discount_amount == null ? null : Number(detail.max_discount_amount),
          applicable_scope: detail.applicable_scope || 'ALL',
          target_ids: Array.isArray(detail.target_ids) ? detail.target_ids : [],
          priority: Number(detail.priority || 0),
          rules: Array.isArray(detail.rules) ? detail.rules : []
        }
        this.dialogVisible = true
      } catch (e) {
        ElMessage.error(e?.response?.data?.message || '获取活动详情失败')
      }
    },
    buildPayload() {
      return {
        promotion_name: this.form.promotion_name,
        promotion_type: this.form.promotion_type,
        description: this.form.description,
        start_time: this.form.start_time,
        end_time: this.form.end_time,
        discount_value: Number(this.form.discount_value),
        min_purchase_amount: this.form.promotion_type === 'FULL_REDUCTION' ? Number(this.form.min_purchase_amount || 0) : null,
        max_discount_amount: this.form.promotion_type === 'FULL_REDUCTION'
            ? (this.form.max_discount_amount == null ? null : Number(this.form.max_discount_amount))
            : null,
        applicable_scope: this.form.applicable_scope,
        target_ids: ['CATEGORY', 'PRODUCT'].includes(this.form.applicable_scope) ? this.form.target_ids : [],
        priority: Number(this.form.priority || 0),
        rules: Array.isArray(this.form.rules) ? this.form.rules : []
      }
    },
    validateForm() {
      if (!this.form.promotion_name) return '活动名称必填'
      if (!this.form.start_time || !this.form.end_time) return '开始时间和结束时间必填'
      if (new Date(this.form.start_time).getTime() >= new Date(this.form.end_time).getTime()) return '开始时间必须早于结束时间'
      if (this.form.promotion_type === 'DISCOUNT' && !(this.form.discount_value > 0 && this.form.discount_value <= 1)) return '折扣活动 discount_value 需在 0~1 之间'
      if (this.form.promotion_type === 'FULL_REDUCTION' && !(this.form.discount_value > 0)) return '满减活动减免金额必须大于 0'
      if (['CATEGORY', 'PRODUCT'].includes(this.form.applicable_scope) && (!Array.isArray(this.form.target_ids) || this.form.target_ids.length === 0)) {
        return '指定范围活动必须选择目标ID'
      }
      return null
    },
    async savePromotion() {
      const err = this.validateForm()
      if (err) {
        ElMessage.warning(err)
        return
      }
      try {
        const payload = this.buildPayload()
        if (this.form.promotion_id) {
          await promotionAPI.updatePromotion(this.form.promotion_id, payload)
        } else {
          await promotionAPI.createPromotion(payload)
        }
        this.dialogVisible = false
        ElMessage.success('保存成功')
        await this.fetchPromotions(this.currentPage)
      } catch (e) {
        ElMessage.error(e?.response?.data?.message || '保存失败')
      }
    },
    async activate(row) {
      try {
        await ElMessageBox.confirm('确认激活该促销活动吗？', '提示', { type: 'warning' })
        await promotionAPI.activatePromotion(row.promotion_id)
        ElMessage.success('激活成功')
        await this.fetchPromotions(this.currentPage)
      } catch (e) {
        if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '激活失败')
      }
    },
    async endPromotion(row) {
      try {
        await ElMessageBox.confirm('确认结束该促销活动吗？', '提示', { type: 'warning' })
        await promotionAPI.endPromotion(row.promotion_id, { reason: 'seller manual end' })
        ElMessage.success('结束成功')
        await this.fetchPromotions(this.currentPage)
      } catch (e) {
        if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '结束失败')
      }
    },
    async cancelPromotion(row) {
      try {
        await ElMessageBox.confirm('确认取消该促销活动吗？', '提示', { type: 'warning' })
        await promotionAPI.cancelPromotion(row.promotion_id, 'seller cancel')
        ElMessage.success('取消成功')
        await this.fetchPromotions(this.currentPage)
      } catch (e) {
        if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '取消失败')
      }
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.fetchPromotions(1)
    },
    handleCurrentChange(page) {
      this.fetchPromotions(page)
    }
  }
}
</script>

<style scoped>
.promotion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.inline-hint {
  margin-left: 10px;
  color: #909399;
}
</style>

