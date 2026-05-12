import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listRules, createRule, updateRule, deleteRule } from '@/api/rules'
import type { CreateRuleRequest } from '@/types'

export function useRules(appId: string, page: number) {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['rules', appId] })

  const query = useQuery({
    queryKey: ['rules', appId, page],
    queryFn: () => listRules(appId, page),
  })

  const create = useMutation({
    mutationFn: (body: CreateRuleRequest) => createRule(appId, body),
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ clientId, body }: { clientId: string; body: CreateRuleRequest }) =>
      updateRule(appId, clientId, body),
    onSuccess: invalidate,
  })

  const remove = useMutation({
    mutationFn: (clientId: string) => deleteRule(appId, clientId),
    onSuccess: invalidate,
  })

  return {
    rules: query.data,
    isLoading: query.isLoading,
    createRule: create.mutateAsync,
    createPending: create.isPending,
    updateRule: update.mutateAsync,
    updatePending: update.isPending,
    deleteRule: remove.mutateAsync,
    deletePending: remove.isPending,
  }
}
