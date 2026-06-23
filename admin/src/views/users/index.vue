<template>
  <div class="users-page">
    <div class="page-header">
      <div class="header-left">
        <h1>用户管理</h1>
        <p class="subtitle">管理注册用户信息</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)"><CircleCheck /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeUsers || 0 }}</div>
              <div class="stat-label">正常用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"><CircleClose /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.bannedUsers || 0 }}</div>
              <div class="stat-label">已禁用</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="状态">
          <el-select
            v-model="filterForm.status"
            placeholder="全部状态"
            clearable
            style="width: 120px"
          >
            <el-option label="正常" value="active" />
            <el-option label="已禁用" value="banned" />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="昵称/OpenID"
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

    <!-- 用户列表 -->
    <el-card shadow="never">
      <el-table
        :data="users"
        v-loading="loading"
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="nickname" label="用户昵称" min-width="150">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="36" :src="row.avatarUrl">
                {{ row.nickname?.charAt(0)?.toUpperCase() || 'U' }}
              </el-avatar>
              <span class="nickname">{{ row.nickname || '未知用户' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="openId" label="OpenID" min-width="200">
          <template #default="{ row }">
            <span class="open-id">{{ row.openId || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="lastLoginAt" label="最后登录" width="170">
          <template #default="{ row }">
            {{ formatTime(row.lastLoginAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 'active' ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, CircleCheck, CircleClose, Refresh, Search
} from '@element-plus/icons-vue'
import {
  getUsers, getUserById, getUserStats, updateUserStatus, deleteUser
} from '@/api/users'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')

const loading = ref(false)
const users = ref([])
const total = ref(0)
const stats = ref({})

const filterForm = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 20
})

// 格式化时间
function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: filterForm.page,
      size: filterForm.size
    }
    if (filterForm.keyword) params.keyword = filterForm.keyword
    if (filterForm.status) params.status = filterForm.status

    const res = await getUsers(params)
    const data = res.data?.data || {}
    users.value = data.records || []
    total.value = data.total || 0

    // 加载统计数据
    const statsRes = await getUserStats()
    stats.value = statsRes.data?.data || {}
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
  filterForm.keyword = ''
  filterForm.status = ''
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

// 切换用户状态
async function toggleStatus(row) {
  const newStatus = row.status === 'active' ? 'banned' : 'active'
  const actionText = newStatus === 'active' ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}用户「${row.nickname}」吗？`,
      '操作确认',
      { type: 'warning' }
    )

    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${actionText}成功`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除用户
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.nickname}」吗？此操作不可恢复！`,
      '删除确认',
      { type: 'danger' }
    )

    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.users-page {
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

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .nickname {
      font-weight: 500;
      color: #303133;
    }
  }

  .open-id {
    font-family: monospace;
    font-size: 12px;
    color: #606266;
  }
}
</style>
