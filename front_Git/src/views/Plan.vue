<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI 健康计划生成</span>
          <el-button type="primary" :loading="loading" @click="generatePlan">
            <el-icon><MagicStick /></el-icon> 生成本周计划
          </el-button>
        </div>
      </template>

      <div v-if="!planContent" class="empty-tip">
        <el-icon :size="60" color="#909399"><Document /></el-icon>
        <p>点击上方按钮，AI将为您生成个性化的一周饮食与运动计划</p>
        <p style="color:#909399;font-size:14px">计划会结合您的健康档案和历史体重变化进行智能调整</p>
      </div>

      <div v-else>
        <el-alert
          title="计划已根据您的档案和历史体重数据生成"
          type="success"
          :closable="false"
          style="margin-bottom:20px"
        />
        <div class="plan-content" v-html="renderedPlan"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { marked } from 'marked'

const planContent = ref('')
const loading = ref(false)

const renderedPlan = computed(() => {
  return planContent.value ? marked.parse(planContent.value) : ''
})

const generatePlan = async () => {
  loading.value = true
  planContent.value = ''
  try {
    const res = await request.get('/plan/generate')
    if (res.code === 200) {
      planContent.value = res.data
      ElMessage.success('计划生成成功')
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  } catch (e) {
    ElMessage.error('服务异常，请检查网络')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-tip {
  text-align: center;
  padding: 60px 0;
  color: #606266;
}
.empty-tip p {
  margin-top: 20px;
}
.plan-content {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  line-height: 1.8;
}
.plan-content :deep(h1) { color: #409EFF; border-bottom: 2px solid #409EFF; padding-bottom: 10px; }
.plan-content :deep(h2) { color: #67C23A; margin-top: 20px; }
.plan-content :deep(h3) { color: #E6A23C; }
.plan-content :deep(ul) { padding-left: 20px; }
.plan-content :deep(li) { margin: 8px 0; }
.plan-content :deep(strong) { color: #303133; }
</style>
