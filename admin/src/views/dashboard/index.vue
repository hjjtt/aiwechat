<template>
  <div class="dashboard">
    <div class="page-header">
      <h1>数据概览</h1>
      <p class="subtitle">实时监控系统数据状态</p>
    </div>

    <el-row :gutter="20" class="stats-row" v-loading="loading">
      <el-col :span="6">
        <div class="stat-card stat-chat">
          <div class="stat-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalRecords }}</div>
            <div class="stat-label">聊天记录总数</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-users">
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-orders">
          <div class="stat-icon">
            <el-icon><List /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.todayOrders }}</div>
            <div class="stat-label">今日订单</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-menus">
          <div class="stat-icon">
            <el-icon><Food /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalMenus }}</div>
            <div class="stat-label">菜单数量</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-feedback">
          <div class="stat-icon">
            <el-icon><ChatLineRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.averageRating }}</div>
            <div class="stat-label">平均评分</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card class="action-card">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>

          <el-row :gutter="16">
            <el-col :span="6">
              <div class="action-item" @click="$router.push('/chat-records')">
                <el-icon class="action-icon"><ChatDotRound /></el-icon>
                <span class="action-text">聊天记录管理</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="action-item" @click="$router.push('/orders')">
                <el-icon class="action-icon"><List /></el-icon>
                <span class="action-text">订单管理</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="action-item" @click="$router.push('/menus')">
                <el-icon class="action-icon"><Food /></el-icon>
                <span class="action-text">菜单管理</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="action-item" @click="$router.push('/users')">
                <el-icon class="action-icon"><User /></el-icon>
                <span class="action-text">用户管理</span>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="action-item" @click="$router.push('/feedback')">
                <el-icon class="action-icon"><ChatLineRound /></el-icon>
                <span class="action-text">意见反馈</span>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ChatDotRound, User, List, Food, ChatLineRound } from '@element-plus/icons-vue'
import { getChatStats } from '@/api/chat-records'
import { getOrderStats } from '@/api/orders'
import { getMenuStats } from '@/api/menus'
import { getFeedbackStats } from '@/api/feedback'

const emptyStats = () => ({
  totalRecords: 0,
  totalUsers: 0,
  todayOrders: 0,
  totalMenus: 0,
  averageRating: 0
})

const stats = ref(emptyStats())
const loading = ref(false)

onMounted(async () => {
  loading.value = true

  try {
    const [chatRes, orderRes, menuRes, feedbackRes] = await Promise.all([
      getChatStats(),
      getOrderStats(),
      getMenuStats(),
      getFeedbackStats()
    ])

    const chatData = chatRes.data?.data || {}
    const orderData = orderRes.data?.data || {}
    const menuData = menuRes.data?.data || {}
    const feedbackData = feedbackRes.data?.data || {}

    stats.value = {
      totalRecords: chatData.totalRecords || 0,
      totalUsers: chatData.totalUsers || 0,
      todayOrders: orderData.todayOrders || 0,
      totalMenus: menuData.totalItems || 0,
      averageRating: feedbackData.averageRating || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    stats.value = emptyStats()
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.dashboard {
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

    &.stat-chat .stat-icon {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &.stat-users .stat-icon {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    }

    &.stat-orders .stat-icon {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }

    &.stat-menus .stat-icon {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }

    &.stat-feedback .stat-icon {
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

  .quick-actions {
    .action-card {
      .card-header {
        font-weight: 600;
      }

      .action-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 24px;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.3s;
        background: #f5f7fa;

        &:hover {
          background: #07c160;
          color: #fff;

          .action-icon,
          .action-text {
            color: #fff;
          }
        }

        .action-icon {
          font-size: 32px;
          color: #07c160;
          margin-bottom: 12px;
          transition: color 0.3s;
        }

        .action-text {
          font-size: 14px;
          color: #606266;
          transition: color 0.3s;
        }
      }
    }
  }
}
</style>
