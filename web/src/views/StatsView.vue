<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseIcon from '@/components/BaseIcon.vue'
import BarChart from '@/components/BarChart.vue'
import { fetchStatsApi } from '@/api/stats'
import { formatCompact, formatDayLabel, formatNumber } from '@/utils/format'
import type { AdminStats } from '@/types'

const stats = ref<AdminStats | null>(null)
const loading = ref(false)
const loadError = ref(false)

const dailyCalls = computed(() =>
  (stats.value?.daily ?? []).map((item) => ({ label: formatDayLabel(item.date), value: item.calls })),
)
const dailyTokens = computed(() =>
  (stats.value?.daily ?? []).map((item) => ({ label: formatDayLabel(item.date), value: item.tokens })),
)
const topUsers = computed(() => stats.value?.topUsers ?? [])
const totalCalls = computed(() => stats.value?.totalCalls ?? 0)
const totalTokens = computed(() => stats.value?.totalTokens ?? 0)
const averageTokens = computed(() =>
  totalCalls.value > 0 ? Math.round(totalTokens.value / totalCalls.value) : 0,
)
const peakCalls = computed(() => Math.max(0, ...dailyCalls.value.map((item) => item.value)))

async function load(): Promise<void> {
  loading.value = true
  loadError.value = false
  try {
    stats.value = await fetchStatsApi()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-shell">
      <header class="page-header">
        <div>
          <h1 class="page-title">调用统计</h1>
          <p class="page-subtitle">模型调用、Token 消耗与用户排行，数据来自调用日志。</p>
        </div>
        <div class="page-actions">
          <el-button size="default" :loading="loading" @click="load">
            <BaseIcon name="refresh" :size="14" />
            刷新
          </el-button>
        </div>
      </header>

      <el-alert
        v-if="loadError"
        class="stats-alert"
        type="error"
        :closable="false"
        title="统计加载失败"
        description="请确认后端已在 http://localhost:8080 启动，并且当前账号有权限访问 /api/admin/stats。"
        show-icon
      />

      <div v-loading="loading" class="stats-body">
        <div class="stat-grid">
          <article class="stat-card">
            <div class="stat-icon"><BaseIcon name="chart" :size="18" /></div>
            <span class="stat-label">总调用次数</span>
            <strong class="stat-value">{{ formatNumber(totalCalls) }}</strong>
            <span class="stat-hint">累计模型调用（次）</span>
          </article>

          <article class="stat-card">
            <div class="stat-icon violet"><BaseIcon name="sparkles" :size="18" /></div>
            <span class="stat-label">总 Token 消耗</span>
            <strong class="stat-value">{{ formatCompact(totalTokens) }}</strong>
            <span class="stat-hint">{{ formatNumber(totalTokens) }} tokens</span>
          </article>

          <article class="stat-card">
            <div class="stat-icon teal"><BaseIcon name="clock" :size="18" /></div>
            <span class="stat-label">平均每次 Token</span>
            <strong class="stat-value">{{ formatNumber(averageTokens) }}</strong>
            <span class="stat-hint">总 Token ÷ 总调用次数</span>
          </article>

          <article class="stat-card">
            <div class="stat-icon amber"><BaseIcon name="user" :size="18" /></div>
            <span class="stat-label">单日调用峰值</span>
            <strong class="stat-value">{{ formatNumber(peakCalls) }}</strong>
            <span class="stat-hint">{{ dailyCalls.length }} 天有调用记录</span>
          </article>
        </div>

        <div class="chart-grid">
          <section class="chart-card surface-card">
            <header class="card-head">
              <div>
                <h2 class="card-title">每日调用次数</h2>
                <p class="card-sub">按自然日聚合</p>
              </div>
              <span class="card-badge">次</span>
            </header>
            <BarChart :items="dailyCalls" color="#4f46e5" unit=" 次" empty-text="还没有按天的调用数据" />
          </section>

          <section class="chart-card surface-card">
            <header class="card-head">
              <div>
                <h2 class="card-title">每日 Token 消耗</h2>
                <p class="card-sub">输入 + 输出合计</p>
              </div>
              <span class="card-badge violet">tokens</span>
            </header>
            <BarChart :items="dailyTokens" color="#8b5cf6" unit=" tokens" empty-text="还没有按天的 Token 数据" />
          </section>
        </div>

        <section class="table-card surface-card">
          <header class="card-head">
            <div>
              <h2 class="card-title">调用 Top 用户</h2>
              <p class="card-sub">按调用次数降序，最多展示前 10 位</p>
            </div>
            <span class="card-badge">{{ topUsers.length }} 位</span>
          </header>

          <el-table
            :data="topUsers"
            class="top-table"
            stripe
            empty-text="暂无调用记录"
            :row-style="{ height: '46px' }"
          >
            <el-table-column type="index" label="#" width="56" />
            <el-table-column prop="username" label="用户" min-width="180">
              <template #default="{ row }">
                <span class="user-cell">
                  <span class="user-dot">{{ String(row.username).slice(0, 1).toUpperCase() }}</span>
                  {{ row.username }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="calls" label="调用次数" width="140" align="right">
              <template #default="{ row }">
                <span class="mono">{{ formatNumber(row.calls) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="tokens" label="Token 消耗" width="160" align="right">
              <template #default="{ row }">
                <span class="mono">{{ formatNumber(row.tokens) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <p class="stats-foot">
          统计口径：chat_model_call_log；Token 缺失时由后端按字符数估算。
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-alert {
  margin-bottom: 18px;
  border-radius: 12px;
}

.stats-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 320px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 17px 18px 15px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: #fff;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  right: -28px;
  top: -34px;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.09), transparent 70%);
}

.stat-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-bottom: 8px;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary);
}

.stat-icon.violet {
  background: #f3eeff;
  color: #7c3aed;
}

.stat-icon.teal {
  background: #e7f8f4;
  color: #0d9488;
}

.stat-icon.amber {
  background: #fef4e6;
  color: #d97706;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.02em;
  color: var(--text-strong);
  font-variant-numeric: tabular-nums;
}

.stat-hint {
  font-size: 10.5px;
  color: #a7adbd;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.chart-card,
.table-card {
  padding: 17px 18px 15px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-strong);
}

.card-sub {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-muted);
}

.card-badge {
  flex: none;
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 10.5px;
  letter-spacing: 0.02em;
}

.card-badge.violet {
  background: #f7f2ff;
  border-color: #e9ddff;
  color: #7c3aed;
}

.table-card {
  padding-bottom: 8px;
}

.top-table {
  --el-table-border-color: var(--border);
  --el-table-header-bg-color: var(--bg-subtle);
  --el-table-row-hover-bg-color: #fafaff;
  width: 100%;
  font-size: 12.5px;
}

.top-table :deep(th.el-table__cell) {
  font-weight: 600;
  font-size: 11.5px;
  color: var(--text-secondary);
  letter-spacing: 0.02em;
}

.user-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text);
}

.user-dot {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #eef0ff, #e5e0ff);
  color: var(--primary-strong);
  font-size: 10.5px;
  font-weight: 700;
}

.stats-foot {
  font-size: 10.5px;
  color: #a7adbd;
  text-align: center;
  padding-bottom: 6px;
}

@media (max-width: 1080px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
