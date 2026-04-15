<template>
  <router-view />
  <AgreementDialog />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAppStore, setAgreementRequiredHandler } from '@qianku/shared'
import AgreementDialog from '@/components/AgreementDialog.vue'

const appStore = useAppStore()

onMounted(() => {
  setAgreementRequiredHandler(() => {
    appStore.checkAgreementStatus()
  })
  const token = localStorage.getItem('token')
  if (token) {
    appStore.checkAgreementStatus()
  }
})
</script>
