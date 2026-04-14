import { defineStore } from 'pinia'
import { ref } from 'vue'
import { checkAgreement, signAgreement, type AgreementCheckVO, type AgreementItem } from '../api/agreement'

export const useAppStore = defineStore('app', () => {
  const showAgreement = ref(false)
  const pendingAgreements = ref<AgreementItem[]>([])
  const agreementBlockMode = ref(false)
  const loading = ref(false)

  async function checkAgreementStatus() {
    try {
      const result = await checkAgreement()
      if (result.needConsent && result.agreements && result.agreements.length > 0) {
        pendingAgreements.value = result.agreements
        agreementBlockMode.value = result.block
        showAgreement.value = true
      }
    } catch (e) {
      console.error('Failed to check agreement:', e)
    }
  }

  function setAgreementCheckFromLogin(checkVO: AgreementCheckVO | null | undefined) {
    if (checkVO && checkVO.needConsent && checkVO.agreements && checkVO.agreements.length > 0) {
      pendingAgreements.value = checkVO.agreements
      agreementBlockMode.value = checkVO.block
      showAgreement.value = true
    }
  }

  async function acceptAgreement() {
    if (pendingAgreements.value.length === 0) return
    try {
      for (const item of pendingAgreements.value) {
        await signAgreement({ agreementType: item.type, versionId: item.versionId })
      }
      showAgreement.value = false
      pendingAgreements.value = []
    } catch (e) {
      console.error('Failed to sign agreement:', e)
      throw e
    }
  }

  function dismissAgreement() {
    if (!agreementBlockMode.value) {
      showAgreement.value = false
      pendingAgreements.value = []
    }
  }

  return {
    showAgreement,
    pendingAgreements,
    agreementBlockMode,
    loading,
    checkAgreementStatus,
    setAgreementCheckFromLogin,
    acceptAgreement,
    dismissAgreement,
  }
})
