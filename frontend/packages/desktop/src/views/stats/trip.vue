<template>
  <div class="page-container stats-trip">
    <div class="page-header">
      <h2>出差统计</h2>
      <el-select v-model="year" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #007AFF">{{ tripSummary.tripCount }}</div>
          <div class="stat-label">出差次数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #5856D6">{{ tripSummary.totalDays }}</div>
          <div class="stat-label">出差天数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #FF9500">{{ formatAmount(tripSummary.totalSubsidy) }}</div>
          <div class="stat-label">总补贴</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #34C759">
            {{ tripSummary.tripCount > 0 ? formatAmount(tripSummary.totalSubsidy / tripSummary.tripCount) : '¥0.00' }}
          </div>
          <div class="stat-label">场均补贴</div>
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>城市分布</span>
      </template>
      <el-table :data="tripSummary.cityDistribution" stripe>
        <el-table-column type="index" width="60" label="排名" />
        <el-table-column prop="city" label="城市" width="150" />
        <el-table-column prop="count" label="出差次数" width="100" align="center" />
        <el-table-column prop="amount" label="补贴金额" align="right" width="150">
          <template #default="{ row }">
            <span class="amount">{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getTripSummary,
  formatAmount,
  type TripSummaryVO,
} from '@qianku/shared'

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = computed(() => Array.from({ length: 5 }, (_, i) => currentYear - i))

const tripSummary = reactive<TripSummaryVO>({
  tripCount: 0,
  totalDays: 0,
  totalSubsidy: 0,
  cityDistribution: [],
})

async function loadData() {
  try {
    const res = await getTripSummary({ year: year.value })
    Object.assign(tripSummary, res)
  } catch {
    tripSummary.tripCount = 0
    tripSummary.totalDays = 0
    tripSummary.totalSubsidy = 0
    tripSummary.cityDistribution = []
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.stats-trip {
  .stat-cards {
    margin-bottom: 16px;
  }

  .amount {
    font-weight: 600;
    color: #1d1d1f;
  }
}
</style>
