<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>历史计划记录</span>
        </div>
      </template>
      <el-empty v-if="!plans.length" description="暂无历史计划" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="plan in plans"
          :key="plan.id"
          :timestamp="plan.generatedAt"
          placement="top"
        >
          <el-card>
            <div class="plan-preview" v-html="renderMarkdown(plan.content.substring(0, 200) + '...')" />
            <el-button type="primary" text @click="showDetail(plan)">查看完整计划</el-button>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog v-model="dialogVisible" title="计划详情" width="70%">
      <div class="plan-detail" v-html="renderMarkdown(currentPlan.content)" />
      <div style="margin-top:20px;padding:15px;background:#f5f7fa;border-radius:8px">
        <h4>生成时的体重上下文</h4>
        <p style="color:#606266;font-size:14px">{{ currentPlan.weightContext || '无记录' }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const plans = ref([])
const dialogVisible = ref(false)
const currentPlan = ref({})

const loadPlans = async () => {
  try {
    const res = await request.get('/plan/history')
    if (res.code === 200) {
      plans.value = res.data || []
    }
  } catch (e) {}
}

const showDetail = (plan) => {
  currentPlan.value = plan
  dialogVisible.value = true
}

const renderMarkdown = (text) => {
  // 简单Markdown渲染（实际项目中可用marked库）
  return text
    .replace(/### (.*)/g, '<h3>$1</h3>')
    .replace(/## (.*)/g, '<h2>$1</h2>')
    .replace(/# (.*)/g, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/- (.*)/g, '<li>$1</li>')
    .replace(/\n/g, '<br>')
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.plan-preview {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 10px;
}
.plan-detail {
  line-height: 1.8;
}
.plan-detail :deep(h1) { color: #409EFF; border-bottom: 2px solid #409EFF; padding-bottom: 10px; }
.plan-detail :deep(h2) { color: #67C23A; margin-top: 20px; }
.plan-detail :deep(h3) { color: #E6A23C; }
.plan-detail :deep(strong) { color: #303133; }
</style>
