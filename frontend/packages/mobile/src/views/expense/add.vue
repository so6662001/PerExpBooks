<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { createExpense, listTrips, getCategoryLabel, getCategoryIcon } from '@qianku/shared'
import type { TripVO } from '@qianku/shared'

defineOptions({ name: 'ExpenseAdd' })

const router = useRouter()
const creating = ref(false)
const tripsLoading = ref(false)
const trips = ref<TripVO[]>([])

const categories = [
  { value: 'transport', label: '交通', icon: '🚗' },
  { value: 'accommodation', label: '住宿', icon: '🏨' },
  { value: 'meal', label: '餐饮', icon: '🍽️' },
  { value: 'office', label: '办公', icon: '📎' },
  { value: 'communication', label: '通讯', icon: '📱' },
  { value: 'other', label: '其他', icon: '📋' },
]

const form = ref({
  category: '',
  amount: undefined as number | undefined,
  expenseDate: '',
  tripId: '',
  description: '',
})

const showCategoryPicker = ref(false)
const showDatePicker = ref(false)
const showTripPicker = ref(false)

const categoryText = computed(() => {
  const cat = categories.find(c => c.value === form.value.category)
  return cat ? `${cat.icon} ${cat.label}` : ''
})

const tripText = computed(() => {
  if (!form.value.tripId) return ''
  const trip = trips.value.find(t => t.id === form.value.tripId)
  return trip ? `📍 ${trip.destination}` : ''
})

const canSubmit = computed(() =>
  form.value.category && form.value.amount && form.value.amount > 0 && form.value.expenseDate,
)

const today = new Date()
const defaultDate = [
  String(today.getFullYear()),
  String(today.getMonth() + 1).padStart(2, '0'),
  String(today.getDate()).padStart(2, '0'),
]

onMounted(async () => {
  form.value.expenseDate = defaultDate.join('-')
  tripsLoading.value = true
  try {
    const result = await listTrips({ pageNum: 1, pageSize: 100 })
    trips.value = result.list
  } catch {
    // ignore
  } finally {
    tripsLoading.value = false
  }
})

function onCategoryConfirm({ selectedOptions }: any) {
  form.value.category = selectedOptions[0]?.value || ''
  showCategoryPicker.value = false
}

function onDateConfirm({ selectedValues }: any) {
  form.value.expenseDate = selectedValues.join('-')
  showDatePicker.value = false
}

function onTripConfirm({ selectedOptions }: any) {
  form.value.tripId = selectedOptions[0]?.value || ''
  showTripPicker.value = false
}

function clearTrip() {
  form.value.tripId = ''
}

async function handleSubmit() {
  if (!canSubmit.value) return
  creating.value = true
  try {
    await createExpense({
      category: form.value.category,
      amount: form.value.amount!,
      expenseDate: form.value.expenseDate,
      tripId: form.value.tripId || undefined,
      description: form.value.description,
    })
    showToast({ message: '添加成功', type: 'success' })
    router.back()
  } catch (e: any) {
    showToast(e.message || '添加失败')
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="添加费用" left-arrow @click-left="router.back()" />

    <div class="form-section card">
      <van-field
        :model-value="categoryText"
        is-link
        readonly
        label="费用分类"
        placeholder="请选择分类"
        required
        @click="showCategoryPicker = true"
      />
      <van-field
        v-model.number="form.amount"
        label="金额(元)"
        type="number"
        placeholder="请输入金额"
        required
      >
        <template #left-icon>
          <span style="font-size: 16px; color: #007AFF">¥</span>
        </template>
      </van-field>
      <van-field
        v-model="form.expenseDate"
        is-link
        readonly
        label="费用日期"
        placeholder="选择日期"
        required
        @click="showDatePicker = true"
      />
      <van-field
        :model-value="tripText"
        is-link
        readonly
        label="关联出差"
        placeholder="可选关联出差记录"
        @click="showTripPicker = true"
      >
        <template v-if="form.tripId" #right-icon>
          <van-icon name="clear" @click.stop="clearTrip" />
        </template>
      </van-field>
      <van-field
        v-model="form.description"
        label="描述"
        placeholder="可选填写描述"
        type="textarea"
        rows="2"
        autosize
      />
    </div>

    <div class="submit-bar">
      <van-button
        type="primary"
        block
        round
        size="large"
        :disabled="!canSubmit"
        :loading="creating"
        @click="handleSubmit"
      >
        提交
      </van-button>
    </div>

    <van-popup v-model:show="showCategoryPicker" position="bottom" round>
      <van-picker
        :columns="categories.map(c => ({ text: `${c.icon} ${c.label}`, value: c.value }))"
        @confirm="onCategoryConfirm"
        @cancel="showCategoryPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        :model-value="defaultDate"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showTripPicker" position="bottom" round>
      <van-picker
        :columns="trips.map(t => ({ text: `📍 ${t.destination} (${t.startDate})`, value: t.id }))"
        @confirm="onTripConfirm"
        @cancel="showTripPicker = false"
      />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.form-section {
  margin-top: 12px;
}

.submit-bar {
  padding: 24px 16px;
}
</style>
