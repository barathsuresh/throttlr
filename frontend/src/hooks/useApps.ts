import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listApps, createApp, deleteApp } from '@/api/apps'
import type { CreateAppRequest } from '@/types'

export function useApps(page: number) {
  const qc = useQueryClient()

  const query = useQuery({
    queryKey: ['apps', page],
    queryFn: () => listApps(page),
  })

  const create = useMutation({
    mutationFn: (body: CreateAppRequest) => createApp(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['apps'] }),
  })

  const remove = useMutation({
    mutationFn: (appId: string) => deleteApp(appId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['apps'] }),
  })

  return {
    apps: query.data,
    isLoading: query.isLoading,
    createApp: create.mutateAsync,
    createPending: create.isPending,
    deleteApp: remove.mutateAsync,
    deletePending: remove.isPending,
  }
}
