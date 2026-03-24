<template>
  <el-dialog
    v-model="dialogVisible"
    title="物流轨迹查询"
    width="600px"
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <div v-if="!logisticsData" class="empty-logistics">
        <el-empty description="暂无物流信息" />
      </div>
      <div v-else class="logistics-container">
        <div class="logistics-header">
          <div class="order-info">
            <div class="order-id">订单编号：{{ logisticsData.purchase_id }}</div>
            <div class="order-status">订单状态：{{ logisticsData.purchase_status }}</div>
          </div>
          <div v-if="logisticsData.logistics_info" class="logistics-info">
            <div class="provider">
              <span class="label">物流公司：</span>
              <span class="value">{{ logisticsData.logistics_info.provider_name }}</span>
            </div>
            <div class="tracking-no">
              <span class="label">物流单号：</span>
              <span class="value">{{ logisticsData.logistics_info.tracking_no }}</span>
            </div>
            <div class="shipped-at">
              <span class="label">发货时间：</span>
              <span class="value">{{ formatTime(logisticsData.logistics_info.shipped_at) }}</span>
            </div>
          </div>
        </div>

        <div v-if="logisticsData.tracks && logisticsData.tracks.length > 0" class="tracks-list">
          <h4>物流轨迹</h4>
          <div class="track-item" v-for="(track, index) in logisticsData.tracks" :key="track.track_id">
            <div class="track-line" :class="{ 'last': index === logisticsData.tracks.length - 1 }"></div>
            <div class="track-content">
              <div class="track-time">{{ formatTime(track.track_time) }}</div>
              <div class="track-status">{{ track.track_status }}</div>
              <div class="track-location">{{ track.track_location }}</div>
              <div class="track-desc">{{ track.track_content }}</div>
            </div>
          </div>
        </div>
        <div v-else class="no-tracks">
          <el-empty description="暂无物流轨迹信息" />
        </div>
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  modelValue: Boolean,
  orderId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['update:modelValue']);

const dialogVisible = ref(false);
const loading = ref(false);
const logisticsData = ref(null);

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val;
  if (val && props.orderId) {
    fetchLogisticsData();
  }
});

watch(dialogVisible, (val) => {
  emit('update:modelValue', val);
});

const fetchLogisticsData = async () => {
  if (!props.orderId) return;

  loading.value = true;
  try {
    const response = await fetch(`/api/customers/orders/${props.orderId}/logistics`, {
      method: 'GET',
      headers: {
        'Authorization': localStorage.getItem('customer_token')
      }
    });

    const result = await response.json();
    if (result.code === 200) {
      logisticsData.value = result.data;
    } else {
      ElMessage.error(result.message || '获取物流信息失败');
      logisticsData.value = null;
    }
  } catch (error) {
    console.error('获取物流信息失败:', error);
    ElMessage.error('网络错误，请重试');
    logisticsData.value = null;
  } finally {
    loading.value = false;
  }
};

const formatTime = (t) => {
  if (!t) return '';
  try { return new Date(t).toLocaleString() } catch (e) { return t }
};

onMounted(() => {
  if (dialogVisible.value && props.orderId) {
    fetchLogisticsData();
  }
});
</script>

<style scoped>
.logistics-container {
  padding: 10px 0;
}

.logistics-header {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.order-info {
  margin-bottom: 10px;
}

.order-id {
  font-weight: bold;
  margin-bottom: 5px;
}

.logistics-info {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 10px;
}

.logistics-info .label {
  color: #666;
}

.logistics-info .value {
  font-weight: 500;
}

.tracks-list {
  margin-top: 20px;
}

.tracks-list h4 {
  margin-bottom: 15px;
  color: #333;
}

.track-item {
  display: flex;
  margin-bottom: 20px;
  position: relative;
}

.track-line {
  width: 2px;
  background-color: #e4e7ed;
  position: absolute;
  left: 10px;
  top: 0;
  bottom: -20px;
}

.track-line.last {
  display: none;
}

.track-content {
  margin-left: 30px;
  flex: 1;
}

.track-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.track-status {
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.track-location {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.track-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
}

.empty-logistics,
.no-tracks {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>