<template>
  <div class="orders-page">
    <div class="page-header">
      <div class="header-left">
        <h1>订单管理</h1>
        <p class="subtitle">查看和管理所有用户订单</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"><ShoppingCart /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
              <div class="stat-label">总订单数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"><Clock /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingOrders || 0 }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"><Calendar /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayOrders || 0 }}</div>
              <div class="stat-label">今日订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)"><Money /></el-icon>
            <div class="stat-info">
              <div class="stat-value">¥{{ stats.todayRevenue?.toFixed(2) || '0.00' }}</div>
              <div class="stat-label">今日收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="订单状态">
          <el-select
            v-model="filterForm.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in statusList"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="订单号/联系人/电话"
            clearable
            style="width: 200px"
            :prefix-icon="Search"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 批量操作 -->
    <el-card shadow="never" class="batch-card">
      <div class="batch-actions">
        <el-checkbox v-model="checkAll" @change="handleCheckAllChange">
          全选
        </el-checkbox>

        <el-button
          type="primary"
          :disabled="selectedIds.length === 0"
          @click="showBatchStatusDialog = true"
        >
          <el-icon><Edit /></el-icon>
          批量更新状态 ({{ selectedIds.length }})
        </el-button>
      </div>
    </el-card>

    <!-- 订单列表 -->
    <el-card shadow="never">
      <el-table
        ref="tableRef"
        :data="orders"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        row-key="id"
      >
        <el-table-column type="selection" width="50" />

        <el-table-column prop="orderNumber" label="订单号" width="180">
          <template #default="{ row }">
            <span class="order-number">{{ row.orderNumber }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="contactName" label="联系人" width="100">
          <template #default="{ row }">
            {{ row.contactName || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="contactPhone" label="联系电话" width="130">
          <template #default="{ row }">
            {{ row.contactPhone || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="deliveryAddress" label="配送地址" min-width="200">
          <template #default="{ row }">
            {{ row.deliveryAddress || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="下单时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">
              详情
            </el-button>
            <el-button type="success" link size="small" @click="showUpdateStatus(row)">
              更新状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="filterForm.page"
          v-model:page-size="filterForm.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="订单详情" width="600px">
      <div class="order-detail" v-if="currentOrder">
        <div class="detail-header">
          <span class="order-no">{{ currentOrder.orderNumber }}</span>
          <el-tag :type="getStatusType(currentOrder.status)" size="small">
            {{ currentOrder.statusText }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单金额">
            <span class="amount">¥{{ currentOrder.totalAmount?.toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ formatTime(currentOrder.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="联系人">
            {{ currentOrder.contactName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ currentOrder.contactPhone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">
            {{ currentOrder.deliveryAddress || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ currentOrder.remark || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="items-section">
          <h4>订单商品</h4>
          <el-table :data="currentOrder.items || []" size="small">
            <el-table-column prop="menuName" label="商品名称" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="unitPrice" label="单价" width="100">
              <template #default="{ row }">
                ¥{{ row.unitPrice?.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="subtotal" label="小计" width="100">
              <template #default="{ row }">
                <span class="amount">¥{{ row.subtotal?.toFixed(2) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 更新状态对话框 -->
    <el-dialog v-model="showStatusDialog" title="更新订单状态" width="400px">
      <el-form :model="statusForm" label-width="80px">
        <el-form-item label="订单号">
          <span>{{ statusForm.orderNumber }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(statusForm.currentStatus)" size="small">
            {{ statusForm.currentStatusText }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.newStatus" style="width: 100%">
            <el-option
              v-for="item in statusList"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStatusDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateStatus" :loading="statusLoading">
          确认更新
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量更新状态对话框 -->
    <el-dialog v-model="showBatchStatusDialog" title="批量更新订单状态" width="400px">
      <el-form :model="batchStatusForm" label-width="80px">
        <el-form-item label="已选择">
          <span>{{ selectedIds.length }} 个订单</span>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="batchStatusForm.status" style="width: 100%">
            <el-option
              v-for="item in statusList"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchStatusDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBatchUpdateStatus" :loading="batchStatusLoading">
          确认更新
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ShoppingCart, Clock, Calendar, Money, Refresh, Search, Edit
} from '@element-plus/icons-vue'
import {
  getOrders, getOrderById, getOrderStats, updateOrderStatus,
  batchUpdateOrderStatus, getOrderStatuses
} from '@/api/orders'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')

const tableRef = ref(null)

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const stats = ref({})
const statusList = ref([])
const selectedIds = ref([])
const checkAll = ref(false)

const showDetailDialog = ref(false)
const showStatusDialog = ref(false)
const showBatchStatusDialog = ref(false)
const currentOrder = ref(null)
const statusLoading = ref(false)
const batchStatusLoading = ref(false)

const filterForm = reactive({
  status: '',
  keyword: '',
  page: 1,
  size: 20
})

const statusForm = reactive({
  id: null,
  orderNumber: '',
  currentStatus: '',
  currentStatusText: '',
  newStatus: ''
})

const batchStatusForm = reactive({
  status: ''
})

// 状态列表
const statusOptions = [
  { value: 'pending', label: '待处理', type: 'warning' },
  { value: 'confirmed', label: '已确认', type: 'info' },
  { value: 'preparing', label: '准备中', type: 'warning' },
  { value: 'delivering', label: '配送中', type: 'primary' },
  { value: 'completed', label: '已完成', type: 'success' },
  { value: 'cancelled', label: '已取消', type: 'danger' }
]

// 格式化时间
function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 获取状态类型
function getStatusType(status) {
  const item = statusOptions.find(s => s.value === status)
  return item ? item.type : 'info'
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: filterForm.page,
      size: filterForm.size
    }
    if (filterForm.status) params.status = filterForm.status
    if (filterForm.keyword) params.keyword = filterForm.keyword

    const res = await getOrders(params)
    const data = res.data?.data || {}
    orders.value = data.records || []
    total.value = data.total || 0

    // 加载统计数据
    const statsRes = await getOrderStats()
    stats.value = statsRes.data?.data || {}

    // 加载状态列表
    const statusRes = await getOrderStatuses()
    const statuses = statusRes.data?.data || []
    statusList.value = statusOptions.filter(s => statuses.includes(s.value))
  } catch (e) {
    ElMessage.error('加载数据失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  filterForm.page = 1
  loadData()
}

// 重置筛选
function resetFilter() {
  filterForm.status = ''
  filterForm.keyword = ''
  filterForm.page = 1
  loadData()
}

// 分页
function handleSizeChange() {
  filterForm.page = 1
  loadData()
}

function handlePageChange() {
  loadData()
}

// 选择变化
function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id)
  checkAll.value = selection.length === orders.value.length && orders.value.length > 0
}

// 全选
function handleCheckAllChange(val) {
  if (val) {
    tableRef.value?.toggleAllSelection()
  } else {
    tableRef.value?.clearSelection()
  }
}

// 显示详情
async function showDetail(row) {
  try {
    const res = await getOrderById(row.id)
    currentOrder.value = res.data?.data || row
    showDetailDialog.value = true
  } catch (e) {
    currentOrder.value = row
    showDetailDialog.value = true
  }
}

// 显示更新状态对话框
function showUpdateStatus(row) {
  statusForm.id = row.id
  statusForm.orderNumber = row.orderNumber
  statusForm.currentStatus = row.status
  statusForm.currentStatusText = row.statusText
  statusForm.newStatus = row.status
  showStatusDialog.value = true
}

// 更新状态
async function handleUpdateStatus() {
  if (!statusForm.newStatus || statusForm.newStatus === statusForm.currentStatus) {
    ElMessage.warning('请选择不同的状态')
    return
  }

  statusLoading.value = true
  try {
    await updateOrderStatus(statusForm.id, statusForm.newStatus)
    ElMessage.success('状态更新成功')
    showStatusDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('状态更新失败')
  } finally {
    statusLoading.value = false
  }
}

// 批量更新状态
async function handleBatchUpdateStatus() {
  if (!batchStatusForm.status) {
    ElMessage.warning('请选择状态')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedIds.value.length} 个订单更新为「${statusOptions.find(s => s.value === batchStatusForm.status)?.label}」状态吗？`,
      '批量更新状态',
      { type: 'warning' }
    )

    batchStatusLoading.value = true
    await batchUpdateOrderStatus(selectedIds.value, batchStatusForm.status)
    ElMessage.success('批量更新成功')
    showBatchStatusDialog.value = false
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量更新失败')
    }
  } finally {
    batchStatusLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.orders-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;

    .header-left {
      h1 {
        font-size: 20px;
        font-weight: 600;
        color: #303133;
        margin: 0;
      }

      .subtitle {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .stats-row {
    margin-bottom: 20px;

    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;

        .stat-icon {
          font-size: 32px;
          color: #fff;
          width: 56px;
          height: 56px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 16px;
        }

        .stat-info {
          .stat-value {
            font-size: 24px;
            font-weight: bold;
            color: #303133;
          }

          .stat-label {
            font-size: 13px;
            color: #909399;
          }
        }
      }
    }
  }

  .filter-card {
    margin-bottom: 16px;
  }

  .batch-card {
    margin-bottom: 16px;

    .batch-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .order-number {
    font-family: monospace;
    font-size: 13px;
    color: #606266;
  }

  .amount {
    color: #f56c6c;
    font-weight: 600;
  }
}

.order-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .order-no {
      font-family: monospace;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .items-section {
    margin-top: 20px;

    h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #303133;
    }
  }
}
</style>
