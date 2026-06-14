import request from './request'
import type { PageResult } from './user'

/** 相关方类型。 */
export type PartyType = 'PERSON' | 'ORGANIZATION'

// ---- 人员 ----

export interface PersonItem {
  id: string
  name: string
  gender: number
  idCard?: string
  contact?: string
  status: number
  remark?: string
  createdAt: string
}

export interface PersonParams {
  name: string
  gender?: number
  idCard?: string
  contact?: string
  status?: number
  remark?: string
}

export function pagePersons(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<unknown, PageResult<PersonItem>>('/party/persons', { params })
}

export function createPerson(data: PersonParams) {
  return request.post<unknown, string>('/party/persons', data)
}

export function updatePerson(id: string, data: PersonParams) {
  return request.put<unknown, void>(`/party/persons/${id}`, data)
}

export function deletePerson(id: string) {
  return request.delete<unknown, void>(`/party/persons/${id}`)
}

// ---- 组织/单位 ----

export interface OrganizationItem {
  id: string
  name: string
  orgType?: string
  taxNo?: string
  registeredCapital?: string
  establishedDate?: string
  legalPerson?: string
  regAddress?: string
  businessScope?: string
  status: number
  remark?: string
  createdAt: string
}

export interface OrganizationParams {
  name: string
  orgType?: string
  taxNo?: string
  registeredCapital?: string
  establishedDate?: string
  legalPerson?: string
  regAddress?: string
  businessScope?: string
  status?: number
  remark?: string
}

export function pageOrganizations(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<unknown, PageResult<OrganizationItem>>('/party/organizations', { params })
}

/** 已入库的组织类型（去重），供输入补全。 */
export function listOrganizationTypes() {
  return request.get<unknown, string[]>('/party/organizations/types')
}

export function createOrganization(data: OrganizationParams) {
  return request.post<unknown, string>('/party/organizations', data)
}

export function updateOrganization(id: string, data: OrganizationParams) {
  return request.put<unknown, void>(`/party/organizations/${id}`, data)
}

export function deleteOrganization(id: string) {
  return request.delete<unknown, void>(`/party/organizations/${id}`)
}

// ---- 账户 ----

export interface AccountItem {
  id: string
  partyId: string
  accountName: string
  accountNo: string
  bankName?: string
  bankBranch?: string
  status: number
  remark?: string
  createdAt: string
}

export interface AccountParams {
  accountName: string
  accountNo: string
  bankName?: string
  bankBranch?: string
  status?: number
  remark?: string
}

export function listAccounts(partyId: string) {
  return request.get<unknown, AccountItem[]>(`/party/${partyId}/accounts`)
}

export function createAccount(partyId: string, data: AccountParams) {
  return request.post<unknown, string>(`/party/${partyId}/accounts`, data)
}

export function updateAccount(id: string, data: AccountParams) {
  return request.put<unknown, void>(`/party/accounts/${id}`, data)
}

export function deleteAccount(id: string) {
  return request.delete<unknown, void>(`/party/accounts/${id}`)
}

export function listBankNames() {
  return request.get<unknown, string[]>(`/party/accounts/bank-names`)
}

// ---- 资质 ----

export interface QualificationItem {
  id: string
  partyId: string
  qualType: string
  qualName: string
  qualLevel?: string
  qualNo?: string
  issuingAuthority?: string
  issueDate?: string
  expiryDate?: string
  fileId?: string
  fileName?: string
  fileContentType?: string
  status: number
  remark?: string
  createdAt: string
}

export interface QualificationParams {
  qualType: string
  qualName: string
  qualLevel?: string
  qualNo?: string
  issuingAuthority?: string
  issueDate?: string
  expiryDate?: string
  status?: number
  remark?: string
}

export function listQualifications(partyId: string) {
  return request.get<unknown, QualificationItem[]>(`/party/${partyId}/qualifications`)
}

export function createQualification(partyId: string, data: QualificationParams) {
  return request.post<unknown, string>(`/party/${partyId}/qualifications`, data)
}

export function updateQualification(id: string, data: QualificationParams) {
  return request.put<unknown, void>(`/party/qualifications/${id}`, data)
}

export function deleteQualification(id: string) {
  return request.delete<unknown, void>(`/party/qualifications/${id}`)
}

export function listQualTypes() {
  return request.get<unknown, string[]>(`/party/qualifications/qual-types`)
}

export function listQualLevels() {
  return request.get<unknown, string[]>(`/party/qualifications/qual-levels`)
}

export function uploadQualificationFile(id: string, file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<unknown, string>(`/party/qualifications/${id}/file`, fd)
}
