<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getTripDetail, updateTrip } from '@qianku/shared'

defineOptions({ name: 'TripEdit' })

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  destination: '',
  startDate: '',
  endDate: '',
  purpose: '',
  budget: undefined as number | undefined,
  remark: '',
})

const showStartPicker = ref(false)
const showEndPicker = ref(false)

const canSubmit = computed(() =>
  form.value.destination && form.value.startDate && form.value.endDate && form.value.purpose,
)

onMounted(async () => {
  const id = route.params.id as string
  try {
    const trip = await getTripDetail(id)
    form.value.destination = trip.destination
    form.value.startDate = trip.startDate
    form.value.endDate = trip.endDate
    form.value.purpose = trip.purpose
    form.value.budget = trip.budget || undefined
    form.value.remark = trip.remark || ''
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

function onStartDateConfirm({ selectedValues }: any) {
  form.value.startDate = selectedValues.join('-')
  showStartPicker.value = false
}

function onEndDateConfirm({ selectedValues }: any) {
  form.value.endDate = selectedValues.join('-')
  showEndPicker.value = false
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const id = route.params.id as string
    await updateTrip(id, form.value)
    showToast({ message: '更新成功', type: 'success' })
    router.back()
  } catch (e: any) {
    showToast(e.message || '更新失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="编辑出差" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="form-section card">
        <van-field
          v-model="form.destination"
          label="出差地点"
          placeholder="请输入目的地"
          required
        />
        <van-field
          v-model="form.startDate"
          is-link
          readonly
          label="开始日期"
          placeholder="选择开始日期"
          required
          @click="showStartPicker = true"
        />
        <van-field
          v-model="form.endDate"
          is-link
          readonly
          label="结束日期"
          placeholder="选择结束日期"
          required
          @click="showEndPicker = true"
        />
        <van-field
          v-model="form.purpose"
          label="出差事由"
          placeholder="请输入出差目的"
          type="textarea"
          rows="2"
          autosize
          required
        />
        <van-field
          v-model.number="form.budget"
          label="预算(元)"
          type="number"
          placeholder="可选填写预算"
        />
        <van-field
          v-model="form.remark"
          label="备注"
          placeholder="可选填写备注"
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
          :loading="submitting"
          @click="handleSubmit"
        >
          保存修改
        </van-button>
      </div>
    </template>

    <van-popup v-model:show="showStartPicker" position="bottom" round>
      <van-date-picker @confirm="onStartDateConfirm" @cancel="showStartPicker = false" />
    </van-popup>
    <van-popup v-model:show="showEndPicker" position="bottom" round>
      <van-date-picker @confirm="onEndDateConfirm" @cancel="showEndPicker = false" />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.form-section {
  margin-top: 12px;
}

.submit-bar {
  padding: 24px 16px;
}
</style>
