import { defineStore } from 'pinia'
import { ref } from 'vue'
import { checkAgreement, confirmAgreement, type AgreementVO } from '../api/agreement'

export const useAppStore = defineStore('app', () => {
  const showAgreement = ref(false)
  const currentAgreement = ref<AgreementVO | null>(null)
  const agreementBlockMode = ref(false)
  const loading = ref(false)

  async function checkAgreementStatus() {
    try {
      const result = await checkAgreement()
      if (result.needConfirm && result.agreement) {
        currentAgreement.value = result.agreement
        agreementBlockMode.value = result.agreement.blockMode
        showAgreement.value = true
      }
    } catch (e) {
      console.error('Failed to check agreement:', e)
    }
  }

  async function acceptAgreement() {
    if (!currentAgreement.value) return
    try {
      await confirmAgreement(currentAgreement.value.id)
      showAgreement.value = false
      currentAgreement.value = null
    } catch (e) {
      console.error('Failed to confirm agreement:', e)
      throw e
    }
  }

  function dismissAgreement() {
    if (!agreementBlockMode.value) {
      showAgreement.value = false
      currentAgreement.value = null
    }
  }

  return {
    showAgreement,
    currentAgreement,
    agreementBlockMode,
    loading,
    checkAgreementStatus,
    acceptAgreement,
    dismissAgreement,
  }
})
