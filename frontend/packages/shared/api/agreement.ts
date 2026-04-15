import { get, post } from './request'

export interface AgreementItem {
  type: string
  versionId: string
  versionCode: string
  changeSummary: string
  changeLevel: string
}

export interface AgreementCheckVO {
  needConsent: boolean
  block: boolean
  agreements: AgreementItem[]
}

export interface AgreementVersionVO {
  id: string
  type: string
  versionCode: string
  title: string
  content: string
  changeSummary: string
  changeLevel: string
  effectiveAt: string
  createdAt: string
}

export interface AgreementSignDTO {
  agreementType: string
  versionId: string
}

export const checkAgreement = () =>
  get<AgreementCheckVO>('/agreement/check')

export const signAgreement = (data: AgreementSignDTO) =>
  post('/agreement/sign', data)

export const getCurrentAgreement = (type: string) =>
  get<AgreementVersionVO>(`/agreement/current/${type}`)

export const getAgreementHistory = (type: string) =>
  get<AgreementVersionVO[]>(`/agreement/history/${type}`)

export const getAgreementVersion = (id: string) =>
  get<AgreementVersionVO>(`/agreement/version/${id}`)
