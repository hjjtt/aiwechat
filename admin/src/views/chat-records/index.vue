<template>
  <div class="chat-records-page">
    <div class="page-header">
      <div>
        <h1>聊天记录管理</h1>
        <p>查看、筛选和清理用户与 AI 的对话记录。</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="loadPageData">
        刷新数据
      </el-button>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon success"><ChatDotRound /></el-icon>
            <div>
              <div class="stat-value">{{ stats.totalRecords }}</div>
              <div class="stat-label">总聊天记录数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon primary"><User /></el-icon>
            <div>
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">活跃用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon warning"><Collection /></el-icon>
            <div>
              <div class="stat-value">{{ selectedIds.length }}</div>
              <div class="stat-label">当前选中记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="用户 ID">
          <el-select
            v-model="filterForm.userId"
            placeholder="全部用户"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="id in userIds"
              :key="id"
              :label="id"
              :value="id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索问题或回答内容"
            clearable
            style="width: 260px"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-checkbox v-model="checkAll" @change="handleCheckAllChange">
          全选当前页
        </el-checkbox>

        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除（{{ selectedIds.length }}）
        </el-button>

        <el-button type="warning" @click="showCleanDialog = true">
          <el-icon><Clock /></el-icon>
          按天数清理
        </el-button>

        <el-button type="danger" plain @click="showClearAllDialog = true">
          <el-icon><Warning /></el-icon>
          清空全部记录
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="records"
        row-key="id"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column prop="userId" label="用户 ID" width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ row.userId || '-' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'user' ? 'primary' : 'success'" size="small">
              {{ row.role === 'user' ? '用户' : 'AI 助手' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="对话内容" min-width="360">
          <template #default="{ row }">
            <div class="content-cell">
              <div class="content-line">
                <span class="content-label">{{ row.role === 'user' ? '问：' : '答：' }}</span>
                <span class="content-text">
                  {{ getContentText(row) }}
                </span>
              </div>
              <div v-if="row.sessionId" class="session-id">
                会话：{{ row.sessionId }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
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

    <el-dialog v-model="showCleanDialog" title="按天数清理聊天记录" width="420px">
      <el-form :model="cleanForm" label-width="90px">
        <el-form-item label="保留天数">
          <el-input-number v-model="cleanForm.days" :min="1" :max="365" />
          <span class="dialog-tip">删除该天数之前的记录</span>
        </el-form-item>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          description="清理后不可恢复，请确认后再执行。"
        />
      </el-form>
      <template #footer>
        <el-button @click="showCleanDialog = false">取消</el-button>
        <el-button type="primary" :loading="cleanLoading" @click="handleCleanByDays">
          确认清理
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showClearAllDialog" title="清空全部聊天记录" width="420px">
      <el-alert
        type="error"
        :closable="false"
        show-icon
        title="此操作会删除所有聊天记录"
        description="请输入“确认清空”后继续，该操作不可恢复。"
      />
      <el-form class="confirm-form">
        <el-form-item label="确认内容">
          <el-input v-model="confirmText" placeholder="请输入：确认清空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showClearAllDialog = false">取消</el-button>
        <el-button
          type="danger"
          :loading="clearLoading"
          :disabled="confirmText !== '确认清空'"
          @click="handleClearAll"
        >
          确认清空
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Clock,
  Collection,
  Delete,
  Refresh,
  Search,
  User,
  Warning
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  batchDeleteChatRecords,
  cleanChatRecordsByDays,
  clearAllChatRecords,
  deleteChatRecord,
  getAllUserIds,
  getChatRecords,
  getChatStats
} from '@/api/chat-records'

const tableRef = ref(null)

const loading = ref(false)
const cleanLoading = ref(false)
const clearLoading = ref(false)
const records = ref([])
const total = ref(0)
const userIds = ref([])
const selectedIds = ref([])
const checkAll = ref(false)
const showCleanDialog = ref(false)
const showClearAllDialog = ref(false)
const confirmText = ref('')

const stats = reactive({
  totalRecords: 0,
  totalUsers: 0
})

const filterForm = reactive({
  userId: '',
  keyword: '',
  page: 1,
  size: 20
})

const cleanForm = reactive({
  days: 30
})

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

function truncateText(text, maxLength = 120) {
  if (!text) {
    return '-'
  }
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

function getContentText(row) {
  const content = row.role === 'user' ? row.question : row.answer
  return truncateText(content)
}

async function loadTableData() {
  const params = {
    page: filterForm.page,
    size: filterForm.size
  }

  if (filterForm.userId) {
    params.userId = filterForm.userId
  }

  if (filterForm.keyword) {
    params.keyword = filterForm.keyword
  }

  const response = await getChatRecords(params)
  records.value = response.data?.data?.records || []
  total.value = response.data?.data?.total || 0
}

async function loadStatsData() {
  const [statsResponse, usersResponse] = await Promise.all([
    getChatStats(),
    getAllUserIds()
  ])

  stats.totalRecords = statsResponse.data?.data?.totalRecords || 0
  stats.totalUsers = statsResponse.data?.data?.totalUsers || 0
  userIds.value = usersResponse.data?.data || []
}

async function loadPageData() {
  loading.value = true
  try {
    await Promise.all([loadTableData(), loadStatsData()])
  } catch (error) {
    console.error('加载聊天记录页面失败:', error)
    ElMessage.error('加载聊天记录数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filterForm.page = 1
  loadPageData()
}

function resetFilter() {
  filterForm.userId = ''
  filterForm.keyword = ''
  filterForm.page = 1
  loadPageData()
}

function handleSizeChange() {
  filterForm.page = 1
  loadPageData()
}

function handlePageChange() {
  loadTableData().catch((error) => {
    console.error('分页加载失败:', error)
    ElMessage.error('加载聊天记录失败')
  })
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map((item) => item.id)
  checkAll.value = selection.length > 0 && selection.length === records.value.length
}

function handleCheckAllChange(value) {
  if (value) {
    tableRef.value?.toggleAllSelection()
    return
  }
  tableRef.value?.clearSelection()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除这条聊天记录吗？', '确认删除', {
      type: 'warning'
    })
    await deleteChatRecord(row.id)
    ElMessage.success('删除成功')
    await loadPageData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除聊天记录失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条聊天记录吗？`,
      '批量删除',
      { type: 'warning' }
    )
    await batchDeleteChatRecords(selectedIds.value)
    ElMessage.success('批量删除成功')
    await loadPageData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

async function handleCleanByDays() {
  try {
    await ElMessageBox.confirm(
      `确定清理 ${cleanForm.days} 天之前的聊天记录吗？`,
      '确认清理',
      { type: 'warning' }
    )
    cleanLoading.value = true
    await cleanChatRecordsByDays(cleanForm.days)
    ElMessage.success(`已清理 ${cleanForm.days} 天之前的记录`)
    showCleanDialog.value = false
    await loadPageData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('按天数清理失败:', error)
      ElMessage.error('清理失败')
    }
  } finally {
    cleanLoading.value = false
  }
}

async function handleClearAll() {
  try {
    clearLoading.value = true
    await clearAllChatRecords()
    ElMessage.success('已清空全部聊天记录')
    showClearAllDialog.value = false
    confirmText.value = ''
    await loadPageData()
  } catch (error) {
    console.error('清空聊天记录失败:', error)
    ElMessage.error('清空失败')
  } finally {
    clearLoading.value = false
  }
}

onMounted(() => {
  loadPageData()
})
</script>

<style lang="scss" scoped>
.chat-records-page {
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;

    h1 {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #303133;
    }

    p {
      margin: 6px 0 0;
      color: #909399;
      font-size: 14px;
    }
  }

  .stats-row,
  .filter-card,
  .toolbar-card {
    margin-bottom: 16px;
  }

  .stat-card {
    height: 100%;
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .stat-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 14px;
    font-size: 24px;
    color: #fff;

    &.success {
      background: linear-gradient(135deg, #34c759, #17a34a);
    }

    &.primary {
      background: linear-gradient(135deg, #409eff, #2563eb);
    }

    &.warning {
      background: linear-gradient(135deg, #f59e0b, #ea580c);
    }
  }

  .stat-value {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
    line-height: 1.2;
  }

  .stat-label {
    margin-top: 4px;
    color: #909399;
    font-size: 13px;
  }

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .content-cell {
    line-height: 1.7;
  }

  .content-line {
    display: flex;
    align-items: flex-start;
    gap: 6px;
  }

  .content-label {
    flex-shrink: 0;
    font-weight: 600;
    color: #303133;
  }

  .content-text {
    color: #606266;
    word-break: break-word;
  }

  .session-id {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
  }

  .dialog-tip {
    margin-left: 8px;
    color: #909399;
    font-size: 13px;
  }

  .confirm-form {
    margin-top: 16px;
  }
}
</style>
