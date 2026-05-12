import api from './axios'
import type { CreateRuleRequest, RuleResponse, PagedResponse } from '@/types'

export const createRule = (appId: string, body: CreateRuleRequest): Promise<RuleResponse> =>
  api.post<RuleResponse>(`/api/apps/${appId}/rules`, body).then((r) => r.data)

export const listRules = (appId: string, page: number, size = 10): Promise<PagedResponse<RuleResponse>> =>
  api
    .get<PagedResponse<RuleResponse>>(`/api/apps/${appId}/rules`, { params: { page, size } })
    .then((r) => r.data)

export const updateRule = (appId: string, clientId: string, body: CreateRuleRequest): Promise<RuleResponse> =>
  api.put<RuleResponse>(`/api/apps/${appId}/rules/${encodeURIComponent(clientId)}`, body).then((r) => r.data)

export const deleteRule = (appId: string, clientId: string): Promise<void> =>
  api.delete(`/api/apps/${appId}/rules/${encodeURIComponent(clientId)}`).then(() => undefined)
