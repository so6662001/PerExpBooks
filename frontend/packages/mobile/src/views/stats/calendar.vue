<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getExpenseCalendar, formatAmount } from '@qianku/shared'
import type { CalendarDayVO } from '@qianku/shared'

defineOptions({ name: 'StatsCalendar' })

const router = useRouter()
const loading = ref(true)
const calendarData = ref<CalendarDayVO[]>([])
const selectedDay = ref<CalendarCell | null>(null)

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)

const showYearPicker = ref(false)
const currentYear = now.getFullYear()
const yearMonthColumns = computed(() => [
  Array.from({ length: 5 }, (_, i) => ({
    text: `${currentYear - i}年`,
    value: currentYear - i,
  })),
  Array.from({ length: 12 }, (_, i) => ({
    text: `${i + 1}月`,
    value: i + 1,
  })),
])

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

interface CalendarCell {
  day: number
  date: string
  amount: number
  count: number
}

const calendarGrid = computed(() => {
  const firstDay = new Date(year.value, month.value - 1, 1).getDay()
  const daysInMonth = new Date(year.value, month.value, 0).getDate()
  const dataMap = new Map<number, CalendarDayVO>()
  calendarData.value.forEach(d => {
    const day = new Date(d.date).getDate()
    dataMap.set(day, d)
  })

  const grid: (CalendarCell | null)[] = []
  for (let i = 0; i < firstDay; i++) grid.push(null)
  for (let d = 1; d <= daysInMonth; d++) {
    const data = dataMap.get(d)
    grid.push({
      day: d,
      date: data?.date || '',
      amount: data?.amount || 0,
      count: data?.count || 0,
    })
  }
  return grid
})

const maxAmount = computed(() => {
  return Math.max(...calendarData.value.map(d => d.amount), 1)
})

function getHeatColor(amount: number): string {
  if (amount <= 0) return 'transparent'
  const ratio = Math.min(amount / maxAmount.value, 1)
  const alpha = 0.15 + ratio * 0.7
  return `rgba(0, 122, 255, ${alpha})`
}

onMounted(() => {
  loadData()
})

watch([year, month], () => {
  loadData()
})

async function loadData() {
  loading.value = true
  selectedDay.value = null
  try {
    calendarData.value = await getExpenseCalendar({ year: year.value, month: month.value })
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onDayClick(cell: any) {
  if (!cell || cell.amount <= 0) {
    selectedDay.value = null
    return
  }
  selectedDay.value = cell
}

function onYearMonthConfirm({ selectedOptions }: any) {
  year.value = selectedOptions[0]?.value || currentYear
  month.value = selectedOptions[1]?.value || 1
  showYearPicker.value = false
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="费用日历" left-arrow @click-left="router.back()" />

    <div class="month-selector card">
      <van-field
        :model-value="`${year}年${month}月`"
        is-link
        readonly
        label="月份"
        @click="showYearPicker = true"
      />
    </div>

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="calendar-section card">
        <div class="calendar-header">
          <span v-for="wd in weekDays" :key="wd" class="week-day">{{ wd }}</span>
        </div>
        <div class="calendar-grid">
          <div
            v-for="(cell, index) in calendarGrid"
            :key="index"
            class="calendar-cell"
            :class="{ active: cell && cell.amount > 0, selected: selectedDay?.day === cell?.day }"
            :style="cell ? { background: getHeatColor(cell.amount) } : {}"
            @click="onDayClick(cell)"
          >
            <template v-if="cell">
              <span class="day-num">{{ cell.day }}</span>
              <span v-if="cell.amount > 0" class="day-amount">{{ cell.amount >= 1000 ? `${(cell.amount / 1000).toFixed(0)}k` : cell.amount.toFixed(0) }}</span>
            </template>
          </div>
        </div>
      </div>

      <div v-if="selectedDay" class="detail-section card">
        <h3 class="detail-title">{{ selectedDay.date }} 费用详情</h3>
        <div class="detail-row">
          <span class="detail-label">费用笔数</span>
          <span class="detail-value">{{ selectedDay.count }} 笔</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">费用金额</span>
          <span class="detail-value amount">{{ formatAmount(selectedDay.amount) }}</span>
        </div>
      </div>

      <div class="legend card">
        <span class="legend-label">少</span>
        <span class="legend-block" style="background: rgba(0,122,255,0.15)" />
        <span class="legend-block" style="background: rgba(0,122,255,0.35)" />
        <span class="legend-block" style="background: rgba(0,122,255,0.55)" />
        <span class="legend-block" style="background: rgba(0,122,255,0.75)" />
        <span class="legend-label">多</span>
      </div>
    </template>

    <van-popup v-model:show="showYearPicker" position="bottom" round>
      <van-picker
        :columns="yearMonthColumns"
        @confirm="onYearMonthConfirm"
        @cancel="showYearPicker = false"
      />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.month-selector {
  margin-top: 0;
}

.calendar-section {
  .calendar-header {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    text-align: center;
    font-size: 12px;
    color: var(--color-text-secondary);
    padding-bottom: 8px;
    border-bottom: 0.5px solid var(--color-divider);
    margin-bottom: 4px;
  }

  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 3px;
  }

  .calendar-cell {
    aspect-ratio: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &.active {
      cursor: pointer;
    }

    &.selected {
      outline: 2px solid #007AFF;
    }

    .day-num {
      font-size: 13px;
      font-weight: 500;
    }

    .day-amount {
      font-size: 9px;
      color: #007AFF;
      font-weight: 600;
      margin-top: 1px;
    }
  }
}

.detail-section {
  .detail-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .detail-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 0.5px solid var(--color-divider);

    &:last-child { border-bottom: none; }

    .detail-label {
      color: var(--color-text-secondary);
      font-size: 14px;
    }

    .detail-value {
      font-size: 14px;
      font-weight: 500;

      &.amount {
        color: #007AFF;
        font-weight: 600;
      }
    }
  }
}

.legend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;

  .legend-label {
    font-size: 12px;
    color: var(--color-text-secondary);
  }

  .legend-block {
    width: 16px;
    height: 16px;
    border-radius: 3px;
  }
}
</style>
