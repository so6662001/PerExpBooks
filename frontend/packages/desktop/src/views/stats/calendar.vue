<template>
  <div class="page-container stats-calendar">
    <div class="page-header">
      <h2>费用日历</h2>
      <div class="header-controls">
        <el-select v-model="year" style="width: 100px" @change="loadData">
          <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
        </el-select>
        <el-select v-model="month" style="width: 90px; margin-left: 8px" @change="loadData">
          <el-option v-for="m in 12" :key="m" :label="`${m}月`" :value="m" />
        </el-select>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <div class="chart-container calendar-grid-wrapper">
          <h3 class="chart-title">{{ year }}年{{ month }}月 费用日历</h3>
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
                <span v-if="cell.amount > 0" class="day-amount">¥{{ cell.amount >= 1000 ? `${(cell.amount / 1000).toFixed(1)}k` : cell.amount.toFixed(0) }}</span>
                <span v-if="cell.count > 0" class="day-count">{{ cell.count }}笔</span>
              </template>
            </div>
          </div>
          <div class="legend">
            <span class="legend-label">少</span>
            <span class="legend-block" style="background: rgba(0,122,255,0.15)" />
            <span class="legend-block" style="background: rgba(0,122,255,0.35)" />
            <span class="legend-block" style="background: rgba(0,122,255,0.55)" />
            <span class="legend-block" style="background: rgba(0,122,255,0.75)" />
            <span class="legend-label">多</span>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>{{ selectedDay ? selectedDay.date : '选择日期查看详情' }}</span>
          </template>
          <div v-if="selectedDay" class="detail-content">
            <div class="detail-row">
              <span class="detail-label">费用笔数</span>
              <span class="detail-value">{{ selectedDay.count }} 笔</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">费用总额</span>
              <span class="detail-value amount">{{ formatAmount(selectedDay.amount) }}</span>
            </div>
          </div>
          <div v-else class="detail-empty">
            点击日历中的日期查看当天费用详情
          </div>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>本月汇总</span>
          </template>
          <div class="detail-content">
            <div class="detail-row">
              <span class="detail-label">总笔数</span>
              <span class="detail-value">{{ totalCount }} 笔</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">总金额</span>
              <span class="detail-value amount">{{ formatAmount(totalAmount) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">有支出天数</span>
              <span class="detail-value">{{ activeDays }} 天</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">日均支出</span>
              <span class="detail-value">{{ activeDays > 0 ? formatAmount(totalAmount / activeDays) : '¥0.00' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { getExpenseCalendar, formatAmount, type CalendarDayVO } from '@qianku/shared'

const now = new Date()
const currentYear = now.getFullYear()
const year = ref(currentYear)
const month = ref(now.getMonth() + 1)
const yearOptions = computed(() => Array.from({ length: 5 }, (_, i) => currentYear - i))

const calendarData = ref<CalendarDayVO[]>([])
const selectedDay = ref<(CalendarDayVO & { day: number }) | null>(null)

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const calendarGrid = computed(() => {
  const firstDay = new Date(year.value, month.value - 1, 1).getDay()
  const daysInMonth = new Date(year.value, month.value, 0).getDate()
  const dataMap = new Map<number, CalendarDayVO>()
  calendarData.value.forEach(d => {
    const day = new Date(d.date).getDate()
    dataMap.set(day, d)
  })

  const grid: (CalendarDayVO & { day: number } | null)[] = []
  for (let i = 0; i < firstDay; i++) grid.push(null)
  for (let d = 1; d <= daysInMonth; d++) {
    const data = dataMap.get(d)
    grid.push({
      day: d,
      date: data?.date || `${year.value}-${String(month.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`,
      amount: data?.amount || 0,
      count: data?.count || 0,
    })
  }
  return grid
})

const maxAmount = computed(() => Math.max(...calendarData.value.map(d => d.amount), 1))
const totalAmount = computed(() => calendarData.value.reduce((s, d) => s + d.amount, 0))
const totalCount = computed(() => calendarData.value.reduce((s, d) => s + d.count, 0))
const activeDays = computed(() => calendarData.value.filter(d => d.amount > 0).length)

function getHeatColor(amount: number): string {
  if (amount <= 0) return 'transparent'
  const ratio = Math.min(amount / maxAmount.value, 1)
  const alpha = 0.15 + ratio * 0.7
  return `rgba(0, 122, 255, ${alpha})`
}

async function loadData() {
  selectedDay.value = null
  try {
    calendarData.value = await getExpenseCalendar({ year: year.value, month: month.value })
  } catch {
    calendarData.value = []
  }
}

function onDayClick(cell: any) {
  if (!cell || cell.amount <= 0) {
    selectedDay.value = null
    return
  }
  selectedDay.value = cell
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.stats-calendar {
  .header-controls {
    display: flex;
  }

  .chart-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 16px;
    color: #1d1d1f;
  }

  .calendar-grid-wrapper {
    padding: 20px;
  }

  .calendar-header {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    text-align: center;
    font-size: 13px;
    color: #86868b;
    padding-bottom: 10px;
    border-bottom: 1px solid #e5e5e5;
    margin-bottom: 6px;
  }

  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
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
    min-height: 60px;

    &:hover {
      opacity: 0.8;
    }

    &.selected {
      outline: 2px solid #007AFF;
    }

    .day-num {
      font-size: 14px;
      font-weight: 500;
    }

    .day-amount {
      font-size: 10px;
      color: #007AFF;
      font-weight: 600;
      margin-top: 2px;
    }

    .day-count {
      font-size: 9px;
      color: #86868b;
    }
  }

  .legend {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 16px;

    .legend-label {
      font-size: 12px;
      color: #86868b;
    }

    .legend-block {
      width: 20px;
      height: 20px;
      border-radius: 4px;
    }
  }

  .detail-content {
    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 10px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .detail-label {
        color: #86868b;
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

  .detail-empty {
    text-align: center;
    color: #86868b;
    padding: 30px 0;
    font-size: 14px;
  }
}
</style>
