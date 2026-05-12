const KEY = 'throttlr_jwt'

export const getToken = (): string | null => localStorage.getItem(KEY)
export const setToken = (token: string): void => localStorage.setItem(KEY, token)
export const clearToken = (): void => localStorage.removeItem(KEY)
