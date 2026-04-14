import { get, post } from './request'

export interface AgreementVO {
  id: string
  version: string
  title: string
  content: string
  changeSummary: string
  blockMode: boolean
  effectiveAt: string
}

export interface AgreementCheckVO {
  needConfirm: boolean
  agreement: AgreementVO | null
}

export const checkAgreement = () =>
  get<AgreementCheckVO>('/agreement/check')

export const confirmAgreement = (agreementId: string) =>
  post('/agreement/confirm', { agreementId })

export const getLatestAgreement = () =>
  get<AgreementVO>('/agreement/latest')
