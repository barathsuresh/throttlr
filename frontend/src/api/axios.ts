import axios from 'axios'
import { getToken, clearToken } from '@/lib/token'
import { navigateTo } from '@/lib/navigate'

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://127.0.0.1:8080'

const instance = axios.create({
  baseURL: API_BASE_URL,
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      navigateTo('/login')
    }
    return Promise.reject(error)
  }
)

export default instance
