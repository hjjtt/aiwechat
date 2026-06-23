<template>
  <div class="menus-page">
    <div class="page-header">
      <div class="header-left">
        <h1>菜单管理</h1>
        <p class="subtitle">管理外卖菜品信息</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="success" :icon="Plus" @click="showAddDialog">添加菜品</el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"><Food /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalItems || 0 }}</div>
              <div class="stat-label">菜品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)"><CircleCheck /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.availableItems || 0 }}</div>
              <div class="stat-label">已上架</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"><CircleClose /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.soldOutItems || 0 }}</div>
              <div class="stat-label">已下架</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"><Grid /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalCategories || 0 }}</div>
              <div class="stat-label">分类数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="分类">
          <el-select
            v-model="filterForm.category"
            placeholder="全部分类"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in categoryList"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="菜品名称/描述"
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
          type="success"
          :disabled="selectedIds.length === 0"
          @click="handleBatchAvailable(true)"
        >
          <el-icon><CircleCheck /></el-icon>
          批量上架 ({{ selectedIds.length }})
        </el-button>

        <el-button
          type="warning"
          :disabled="selectedIds.length === 0"
          @click="handleBatchAvailable(false)"
        >
          <el-icon><CircleClose /></el-icon>
          批量下架 ({{ selectedIds.length }})
        </el-button>

        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>
    </el-card>

    <!-- 菜单列表 -->
    <el-card shadow="never">
      <el-table
        ref="tableRef"
        :data="menus"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        row-key="id"
      >
        <el-table-column type="selection" width="50" />

        <el-table-column prop="name" label="菜品名称" min-width="150">
          <template #default="{ row }">
            <div class="item-name">
              <span class="name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span class="description">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="salesCount" label="销量" width="80">
          <template #default="{ row }">
            {{ row.salesCount || 0 }}
          </template>
        </el-table-column>

        <el-table-column prop="isAvailable" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isAvailable === 1 ? 'success' : 'info'" size="small">
              {{ row.isAvailable === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">
              编辑
            </el-button>
            <el-button
              :type="row.isAvailable === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleAvailability(row)"
            >
              {{ row.isAvailable === 1 ? '下架' : '上架' }}
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

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="showFormDialog"
      :title="isEdit ? '编辑菜品' : '添加菜品'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入菜品名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in categoryOptions"
              :key="item"
              :label="item"
              :value="item"
            />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="formData.price"
            :min="0"
            :max="9999"
            :precision="2"
            :step="1"
            style="width: 100%"
          >
            <template #prefix>¥</template>
          </el-input-number>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入菜品描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="上架状态">
          <el-switch
            v-model="formData.isAvailable"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ isEdit ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Food, CircleCheck, CircleClose, Grid, Refresh, Plus, Search, Delete
} from '@element-plus/icons-vue'
import {
  getMenus, getMenuById, getMenuStats, getMenuCategories,
  addMenu, updateMenu, deleteMenu, updateMenuAvailability,
  batchUpdateMenuAvailability
} from '@/api/menus'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')

const tableRef = ref(null)
const formRef = ref(null)

const loading = ref(false)
const submitLoading = ref(false)
const menus = ref([])
const total = ref(0)
const stats = ref({})
const categoryList = ref([])
const categoryOptions = ref([])
const selectedIds = ref([])
const checkAll = ref(false)

const showFormDialog = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const filterForm = reactive({
  category: '',
  keyword: '',
  page: 1,
  size: 20
})

const formData = reactive({
  name: '',
  category: '',
  price: 0,
  description: '',
  isAvailable: 1
})

const formRules = {
  name: [
    { required: true, message: '请输入菜品名称', trigger: 'blur' },
    { min: 1, max: 50, message: '名称长度在1-50个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格必须大于0', trigger: 'blur' }
  ]
}

// 默认分类选项
const defaultCategories = ['主食', '热菜', '凉菜', '汤类', '饮品', '小吃', '甜点']

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
    if (filterForm.category) params.category = filterForm.category
    if (filterForm.keyword) params.keyword = filterForm.keyword

    const res = await getMenus(params)
    const data = res.data?.data || {}
    menus.value = data.records || []
    total.value = data.total || 0

    // 加载统计数据
    const statsRes = await getMenuStats()
    stats.value = statsRes.data?.data || {}

    // 加载分类列表
    const catRes = await getMenuCategories()
    const cats = catRes.data?.data || []
    categoryList.value = cats
    // 合并默认分类和数据库分类
    categoryOptions.value = [...new Set([...defaultCategories, ...cats])]
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
  filterForm.category = ''
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
  checkAll.value = selection.length === menus.value.length && menus.value.length > 0
}

// 全选
function handleCheckAllChange(val) {
  if (val) {
    tableRef.value?.toggleAllSelection()
  } else {
    tableRef.value?.clearSelection()
  }
}

// 显示添加对话框
function showAddDialog() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  showFormDialog.value = true
}

// 显示编辑对话框
async function showEditDialog(row) {
  isEdit.value = true
  currentId.value = row.id
  try {
    const res = await getMenuById(row.id)
    const data = res.data?.data || row
    Object.assign(formData, {
      name: data.name || '',
      category: data.category || '',
      price: data.price ? Number(data.price) : 0,
      description: data.description || '',
      isAvailable: data.isAvailable ?? 1
    })
    showFormDialog.value = true
  } catch (e) {
    ElMessage.error('获取菜品信息失败')
  }
}

// 重置表单
function resetForm() {
  formData.name = ''
  formData.category = ''
  formData.price = 0
  formData.description = ''
  formData.isAvailable = 1
  formRef.value?.clearValidate()
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const data = {
      name: formData.name,
      category: formData.category,
      price: formData.price,
      description: formData.description,
      isAvailable: formData.isAvailable
    }

    if (isEdit.value) {
      await updateMenu(currentId.value, data)
      ElMessage.success('更新成功')
    } else {
      await addMenu(data)
      ElMessage.success('添加成功')
    }

    showFormDialog.value = false
    loadData()
  } catch (e) {
    if (e !== false) {
      ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除菜品「${row.name}」吗？`,
      '删除确认',
      { type: 'warning' }
    )

    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换上架状态
async function toggleAvailability(row) {
  const newStatus = row.isAvailable === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '上架' : '下架'

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}菜品「${row.name}」吗？`,
      '操作确认',
      { type: 'warning' }
    )

    await updateMenuAvailability(row.id, newStatus === 1)
    ElMessage.success(`${actionText}成功`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 批量上架/下架
async function handleBatchAvailable(available) {
  const actionText = available ? '上架' : '下架'

  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedIds.value.length} 个菜品${actionText}吗？`,
      '批量操作确认',
      { type: 'warning' }
    )

    await batchUpdateMenuAvailability(selectedIds.value, available)
    ElMessage.success(`成功${actionText} ${selectedIds.value.length} 个菜品`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量操作失败')
    }
  }
}

// 批量删除
async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个菜品吗？此操作不可恢复！`,
      '批量删除确认',
      { type: 'danger' }
    )

    for (const id of selectedIds.value) {
      await deleteMenu(id)
    }
    ElMessage.success(`成功删除 ${selectedIds.value.length} 个菜品`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.menus-page {
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

  .item-name {
    .name {
      font-weight: 500;
      color: #303133;
    }
  }

  .price {
    color: #f56c6c;
    font-weight: 600;
  }

  .description {
    color: #606266;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
    max-width: 200px;
  }
}
</style>
