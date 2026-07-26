<template>
  <div class="points-trend">
    <div
      v-if="chartRows.length"
      ref="chartElement"
      class="points-trend__canvas"
      role="img"
      :aria-label="chartSummary"
    />
    <div v-else class="points-trend__empty">
      <TrendCharts aria-hidden="true" />
      <strong>还没有积分走势</strong>
      <span>完成签到或参与活动后，这里会展示余额变化。</span>
    </div>

    <details v-if="chartRows.length" class="points-trend__table">
      <summary>查看趋势数据表</summary>
      <div class="points-trend__table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">时间</th>
              <th scope="col">变动</th>
              <th scope="col">余额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in chartRows" :key="row.id">
              <td>{{ row.fullDate }}</td>
              <td :class="row.delta >= 0 ? 'is-positive' : 'is-negative'">
                {{ row.delta > 0 ? '+' : '' }}{{ formatPoints(row.delta) }}
              </td>
              <td>{{ formatPoints(row.balance) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </details>
  </div>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'
import { TrendCharts } from '@element-plus/icons-vue'
import { LineChart } from 'echarts/charts'
import { AriaComponent, GridComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

use([LineChart, GridComponent, TooltipComponent, AriaComponent, CanvasRenderer])

type PointTransaction = {
  id?: string | number
  delta?: number
  balanceAfter?: number
  createdAt?: string
}

type ChartRow = {
  id: string | number
  shortDate: string
  fullDate: string
  delta: number
  balance: number
}

const props = withDefaults(
  defineProps<{
    transactions?: PointTransaction[]
    currentBalance?: number
  }>(),
  {
    transactions: () => [],
    currentBalance: 0,
  },
)

const chartElement = ref<HTMLElement | null>(null)
const { resolvedTheme } = useTheme()
const prefersReducedMotion =
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const shortDateFormatter = new Intl.DateTimeFormat('zh-CN', {
  month: 'numeric',
  day: 'numeric',
})

const chartRows = computed<ChartRow[]>(() => {
  const transactions = [...props.transactions]
    .filter((item) => item.createdAt)
    .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())
    .slice(0, 30)

  let fallbackBalance = Number(props.currentBalance || 0)
  const descendingRows = transactions.map((item, index) => {
    const delta = Number(item.delta || 0)
    const balanceAfter = Number.isFinite(Number(item.balanceAfter))
      ? Number(item.balanceAfter)
      : fallbackBalance
    fallbackBalance = balanceAfter - delta
    const date = new Date(item.createdAt || '')

    return {
      id: item.id ?? `${item.createdAt}-${index}`,
      shortDate: shortDateFormatter.format(date),
      fullDate: dateTimeFormatter.format(date),
      delta,
      balance: balanceAfter,
    }
  })

  return descendingRows.reverse()
})

const chartSummary = computed(() => {
  if (!chartRows.value.length) return '暂无积分趋势数据'
  const last = chartRows.value.at(-1)
  const netChange = chartRows.value.reduce((total, row) => total + row.delta, 0)
  const direction =
    netChange > 0
      ? `合计增加 ${formatPoints(netChange)}`
      : netChange < 0
        ? `合计减少 ${formatPoints(Math.abs(netChange))}`
        : '保持不变'
  return `最近 ${chartRows.value.length} 笔积分记录，当前余额 ${formatPoints(last?.balance || 0)}，${direction}。`
})

function formatPoints(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}

function cssVariable(name: string, fallback: string) {
  if (typeof document === 'undefined') return fallback
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function ensureChart() {
  if (!chartElement.value || !chartRows.value.length) return
  if (chart) {
    renderChart()
    return
  }

  chart = init(chartElement.value)
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
  renderChart()
}

function renderChart() {
  if (!chart || !chartRows.value.length) return

  const textColor = cssVariable('--oa-text', resolvedTheme.value === 'dark' ? '#f5f5f7' : '#1d1d1f')
  const mutedColor = cssVariable(
    '--oa-muted',
    resolvedTheme.value === 'dark' ? '#aeaeb2' : '#6e6e73',
  )
  const borderColor = cssVariable('--oa-border', 'rgba(127, 127, 127, 0.2)')
  const surfaceColor = cssVariable(
    '--oa-elevated-bg',
    resolvedTheme.value === 'dark' ? '#1c1c1e' : '#ffffff',
  )
  const lineColor = resolvedTheme.value === 'dark' ? '#f5f5f7' : '#1d1d1f'
  const areaStart =
    resolvedTheme.value === 'dark' ? 'rgba(245, 245, 247, 0.22)' : 'rgba(29, 29, 31, 0.16)'
  const areaEnd =
    resolvedTheme.value === 'dark' ? 'rgba(245, 245, 247, 0.01)' : 'rgba(29, 29, 31, 0.01)'

  chart.setOption(
    {
      animation: !prefersReducedMotion,
      animationDuration: 360,
      animationEasing: 'cubicOut',
      aria: {
        enabled: true,
        description: chartSummary.value,
      },
      grid: {
        left: 12,
        right: 16,
        top: 26,
        bottom: 8,
        containLabel: true,
      },
      tooltip: {
        trigger: 'axis',
        appendToBody: true,
        backgroundColor: surfaceColor,
        borderColor,
        borderWidth: 1,
        padding: [10, 12],
        textStyle: {
          color: textColor,
          fontFamily: cssVariable('--oa-font-family', 'system-ui, sans-serif'),
        },
        axisPointer: {
          type: 'line',
          lineStyle: {
            color: mutedColor,
            type: 'dashed',
          },
        },
        valueFormatter: (value: unknown) => `${formatPoints(Number(value || 0))} 积分`,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: chartRows.value.map((row) => row.shortDate),
        axisTick: { show: false },
        axisLine: { lineStyle: { color: borderColor } },
        axisLabel: {
          color: mutedColor,
          fontSize: 12,
          hideOverlap: true,
          margin: 14,
        },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitNumber: 4,
        axisLabel: {
          color: mutedColor,
          fontSize: 12,
          formatter: (value: number) => formatPoints(value),
        },
        splitLine: {
          lineStyle: {
            color: borderColor,
            type: 'dashed',
            opacity: 0.72,
          },
        },
      },
      series: [
        {
          name: '积分余额',
          type: 'line',
          data: chartRows.value.map((row) => row.balance),
          smooth: 0.28,
          showSymbol: chartRows.value.length < 8,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: {
            color: lineColor,
            width: 3,
            cap: 'round',
          },
          itemStyle: {
            color: lineColor,
            borderColor: surfaceColor,
            borderWidth: 2,
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: areaStart },
                { offset: 1, color: areaEnd },
              ],
            },
          },
          emphasis: {
            focus: 'series',
            scale: true,
          },
        },
      ],
    },
    true,
  )
}

watch(
  chartRows,
  async (rows) => {
    if (!rows.length) {
      chart?.clear()
      return
    }
    await nextTick()
    ensureChart()
  },
  { deep: true },
)

watch(resolvedTheme, () => {
  nextTick(renderChart)
})

onMounted(async () => {
  await nextTick()
  ensureChart()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.points-trend {
  min-width: 0;
}

.points-trend__canvas {
  width: 100%;
  height: clamp(260px, 31vw, 340px);
}

.points-trend__empty {
  display: grid;
  min-height: 280px;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: var(--space-8);
  border: 1px dashed var(--oa-border);
  border-radius: var(--radius-lg);
  color: var(--oa-muted);
  text-align: center;
}

.points-trend__empty svg {
  width: 32px;
  height: 32px;
}

.points-trend__empty strong {
  color: var(--oa-text);
  font-size: var(--font-size-lg);
}

.points-trend__empty span {
  max-width: 360px;
  line-height: 1.6;
}

.points-trend__table {
  margin-top: var(--space-3);
  color: var(--oa-muted);
  font-size: var(--font-size-sm);
}

.points-trend__table summary {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  cursor: pointer;
  color: var(--oa-text-soft);
  font-weight: var(--font-weight-medium);
}

.points-trend__table summary:focus-visible {
  border-radius: var(--radius-sm);
  outline: 2px solid var(--oa-text);
  outline-offset: 3px;
}

.points-trend__table-wrap {
  overflow-x: auto;
  max-height: 280px;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-md);
}

.points-trend__table table {
  width: 100%;
  border-collapse: collapse;
  font-variant-numeric: tabular-nums;
}

.points-trend__table th,
.points-trend__table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--oa-divider);
  text-align: left;
  white-space: nowrap;
}

.points-trend__table th {
  position: sticky;
  top: 0;
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
  font-weight: var(--font-weight-semibold);
}

.points-trend__table td.is-positive {
  color: var(--color-success);
}

.points-trend__table td.is-negative {
  color: var(--color-danger);
}

@media (max-width: 640px) {
  .points-trend__canvas {
    height: 260px;
  }
}
</style>
