<template>
  <div class="feedback-page">
    <div class="page-header">
      <h1>意见反馈管理</h1>
      <p class="subtitle">查看和管理用户提交的反馈意见</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row" v-loading="statsLoading">
      <el-col :span="6">
        <div class="stat-card stat-total">
          <div class="stat-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalFeedback || 0 }}</div>
            <div class="stat-label">反馈总数</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-rating">
          <div class="stat-icon">
            <el-icon><Star /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.averageRating || 0 }}</div>
            <div class="stat-label">平均评分</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-good">
          <div class="stat-icon">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.goodFeedback || 0 }}</div>
            <div class="stat-label">好评 (4-5 星)</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-poor">
          <div class="stat-icon">
            <el-icon><CircleClose /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.poorFeedback || 0 }}</div>
            <div class="stat-label">差评 (1-2 星)</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select
            v-model="filterRating"
            placeholder="筛选评分"
            clearable
            style="width: 150px"
            @change="filterFeedback"
          >
            <el-option label="5 星 - 非常满意" :value="5" />
            <el-option label="4 星 - 满意" :value="4" />
            <el-option label="3 星 - 一般" :value="3" />
            <el-option label="2 星 - 不满意" :value="2" />
            <el-option label="1 星 - 非常不满意" :value="1" />
          </el-select>

          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户 ID 或内容"
            clearable
            style="width: 300px; margin-left: 16px"
            @input="filterFeedback"
          />
        </div>

        <div class="toolbar-right">
          <el-button
            type="danger"
            plain
            :disabled="selectedIds.length === 0"
            @click="batchDelete"
          >
            批量删除
          </el-button>
          <el-button
            type="danger"
            plain
            @click="clearAll"
          >
            清空所有
          </el-button>
          <el-button
            type="primary"
            @click="loadFeedback"
          >
            刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 反馈列表 -->
    <el-card class="table-card" v-loading="loading">
      <el-table
        :data="filteredFeedback"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="userId" label="用户 ID" width="120" />
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <div class="rating-display">
              <span v-for="i in 5" :key="i" :class="{ active: i <= row.rating }">
                {{ i <= row.rating ? '⭐' : '☆' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="comment" label="详细意见" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              @click="deleteFeedbackItem(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredFeedback.length"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Star, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { getFeedbackList, getFeedbackStats, deleteFeedback, batchDeleteFeedback, clearAllFeedback } from '@/api/feedback'

const loading = ref(false)
const statsLoading = ref(false)
const feedbackList = ref([])
const filteredFeedback = ref([])
const selectedIds = ref([])

const filterRating = ref(null)
const searchKeyword = ref('')

const currentPage = ref(1)
const pageSize = ref(20)

const stats = ref({
  totalFeedback: 0,
  averageRating: 0,
  goodFeedback: 0,
  poorFeedback: 0
})

// 加载反馈列表
const loadFeedback = async () => {
  loading.value = true
  try {
    const res = await getFeedbackList()
    feedbackList.value = res.data?.data || []
    filterFeedback()
  } catch (e) {
    console.error('加载反馈失败:', e)
    ElMessage.error('加载反馈失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getFeedbackStats()
    const data = res.data?.data || {}
    stats.value.averageRating = data.averageRating || 0

    // 计算统计数据
    const allFeedback = feedbackList.value
    stats.value.totalFeedback = allFeedback.length
    stats.value.goodFeedback = allFeedback.filter(f => f.rating >= 4).length
    stats.value.poorFeedback = allFeedback.filter(f => f.rating <= 2).length
  } catch (e) {
    console.error('加载统计失败:', e)
  } finally {
    statsLoading.value = false
  }
}

// 筛选反馈
const filterFeedback = () => {
  let result = [...feedbackList.value]

  // 按评分筛选
  if (filterRating.value) {
    result = result.filter(f => f.rating === filterRating.value)
  }

  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(f =>
      String(f.userId ?? '').toLowerCase().includes(keyword) ||
      f.question?.toLowerCase().includes(keyword) ||
      f.comment?.toLowerCase().includes(keyword)
    )
  }

  filteredFeedback.value = result
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(s => s.id)
}

// 删除反馈
const deleteFeedbackItem = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条反馈吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteFeedback(id)
    ElMessage.success('删除成功')
    loadFeedback()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条反馈吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await batchDeleteFeedback(selectedIds.value)
    ElMessage.success('批量删除成功')
    loadFeedback()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败:', e)
      ElMessage.error('批量删除失败')
    }
  }
}

// 清空所有
const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有反馈吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    await clearAllFeedback()
    ElMessage.success('清空成功')
    loadFeedback()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('清空失败:', e)
      ElMessage.error('清空失败')
    }
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {
  // 分页逻辑已在前端实现
}

onMounted(() => {
  loadFeedback()
  loadStats()
})

// 暴露删除方法给模板
defineExpose({
  deleteFeedbackItem
})
</script>

<style lang="scss" scoped>
.feedback-page {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;

    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }

    .subtitle {
      font-size: 14px;
      color: #909399;
      margin: 8px 0 0;
    }
  }

  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    align-items: center;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      font-size: 24px;
      color: #fff;
    }

    &.stat-total .stat-icon {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &.stat-rating .stat-icon {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    }

    &.stat-good .stat-icon {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }

    &.stat-poor .stat-icon {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }

    .stat-content {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .toolbar-card {
    margin-bottom: 20px;

    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .toolbar-left {
        display: flex;
        align-items: center;
      }

      .toolbar-right {
        display: flex;
        gap: 12px;
      }
    }
  }

  .table-card {
    .pagination-wrap {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .rating-display {
    .active {
      color: #ffd700;
    }

    span:not(.active) {
      color: #dcdfe6;
    }
  }
}
</style>
