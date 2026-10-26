<script setup lang="ts">
import { computed } from 'vue'
import { formatCompact, formatNumber } from '@/utils/format'

interface BarChartItem {
  label: string
  value: number
}

const props = withDefaults(
  defineProps<{
    items: BarChartItem[]
    color?: string
    unit?: string
    emptyText?: string
  }>(),
  {
    color: '#4f46e5',
    unit: '',
    emptyText: '暂无数据',
  },
)

const maxValue = computed(() => Math.max(...props.items.map((item) => item.value), 1))
const labelStep = computed(() => Math.max(1, Math.ceil(props.items.length / 10)))

function heightPercent(value: number): string {
  if (value <= 0) return '0%'
  return `${Math.max((value / maxValue.value) * 100, 3)}%`
}

function tooltip(item: BarChartItem): string {
  return `${item.label} · ${formatNumber(item.value)}${props.unit}`
}
</script>

<template>
  <div class="bar-chart">
    <div v-if="!items.length" class="chart-empty">{{ emptyText }}</div>
    <template v-else>
      <div class="chart-plot">
        <div v-for="(item, index) in items" :key="`${item.label}-${index}`" class="chart-col" :title="tooltip(item)">
          <div class="chart-bar" :style="{ height: heightPercent(item.value), background: color }" />
        </div>
      </div>
      <div class="chart-labels">
        <span v-for="(item, index) in items" :key="`label-${index}`" class="chart-label">
          {{ index % labelStep === 0 || index === items.length - 1 ? item.label : '' }}
        </span>
      </div>
      <div class="chart-foot">
        <span>峰值 {{ formatCompact(maxValue) }}{{ unit }}</span>
        <span>{{ items.length }} 天</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.bar-chart {
  display: flex;
  flex-direction: column;
  min-height: 178px;
}

.chart-empty {
  display: grid;
  place-items: center;
  flex: 1;
  min-height: 160px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 12.5px;
  background: var(--bg-subtle);
}

.chart-plot {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 152px;
  padding: 0 2px;
  background-image: linear-gradient(to top, rgba(230, 232, 240, 0.9) 1px, transparent 1px);
  background-size: 100% 50%;
  background-position: 0 0;
}

.chart-col {
  flex: 1 1 0;
  display: flex;
  align-items: flex-end;
  height: 100%;
  min-width: 0;
}

.chart-bar {
  width: 100%;
  max-width: 44px;
  margin: 0 auto;
  border-radius: 6px 6px 2px 2px;
  transition: height 0.45s cubic-bezier(0.4, 0, 0.2, 1), filter 0.16s ease;
  opacity: 0.92;
}

.chart-col:hover .chart-bar {
  filter: brightness(1.12);
  opacity: 1;
}

.chart-labels {
  display: flex;
  gap: 6px;
  padding: 7px 2px 0;
}

.chart-label {
  flex: 1 1 0;
  min-width: 0;
  text-align: center;
  font-size: 10.5px;
  color: var(--text-muted);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.chart-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 10.5px;
  color: #a7adbd;
}

@media (max-width: 720px) {
  .chart-plot {
    height: 130px;
  }
}
</style>
