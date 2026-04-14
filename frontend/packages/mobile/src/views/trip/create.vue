<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { createTrip } from '@qianku/shared'

const router = useRouter()
const creating = ref(false)

const form = ref({
  title: '',
  destination: '',
  startDate: '',
  endDate: '',
  subsidyPerDay: undefined as number | undefined,
  remark: '',
})

const showStartPicker = ref(false)
const showEndPicker = ref(false)

const canSubmit = computed(() =>
  form.value.title && form.value.destination && form.value.startDate && form.value.endDate && form.value.subsidyPerDay,
)

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
  creating.value = true
  try {
    await createTrip({
      title: form.value.title,
      destination: form.value.destination,
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      subsidyPerDay: form.value.subsidyPerDay!,
      remark: form.value.remark,
    })
    showToast({ message: '创建成功', type: 'success' })
    router.back()
  } catch (e: any) {
    showToast(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="创建出差" left-arrow @click-left="router.back()" />

    <div class="form-section card">
      <van-field
        v-model="form.title"
        label="出差标题"
        placeholder="请输入出差标题"
        required
      />
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
        v-model.number="form.subsidyPerDay"
        label="每日补贴(元)"
        type="number"
        placeholder="请输入每日补贴金额"
        required
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
        :loading="creating"
        @click="handleSubmit"
      >
        创建出差
      </van-button>
    </div>

    <van-popup v-model:show="showStartPicker" position="bottom" round>
      <van-date-picker @confirm="onStartDateConfirm" @cancel="showStartPicker = false" />
    </van-popup>
    <van-popup v-model:show="showEndPicker" position="bottom" round>
      <van-date-picker @confirm="onEndDateConfirm" @cancel="showEndPicker = false" />
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
