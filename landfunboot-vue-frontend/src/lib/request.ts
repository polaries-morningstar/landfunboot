import axios, { type AxiosRequestConfig } from 'axios'
import { toast } from '@/components/ui/toast/use-toast'

export interface RequestOptions {
    method?: string
    body?: BodyInit | object
    params?: Record<string, string | number | boolean | undefined>
    headers?: Record<string, string>
}

const instance = axios.create({
    baseURL: '',
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' }
})

instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = token
    }
    if (config.headers['Content-Type'] === 'application/json' && config.data instanceof FormData) {
        delete config.headers['Content-Type']
    }
    return config
})

instance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            const requestUrl: string = error.response.config?.url || ''
            const isLoginRequest = requestUrl.includes('/auth/login')
            const isOnLoginPage = window.location.pathname.endsWith('/login')

            // 仅在非登录页、非登录接口的 401 时才重定向到登录页
            if (!isLoginRequest && !isOnLoginPage) {
                localStorage.removeItem('token')
                window.location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
    const { method = 'GET', body, params, headers } = options
    const config: AxiosRequestConfig = {
        url,
        method: method as any,
        params,
        headers
    }
    if (body !== undefined) {
        config.data = body instanceof FormData ? body : body
    }
    try {
        const response = await instance.request(config)
        const res = response.data
        if (res && res.code !== undefined && res.code !== 200) {
            const msg = res.message || res.msg || '请求失败'
            toast({
                title: '错误',
                description: msg,
                variant: 'destructive'
            })
            throw new Error(msg)
        }
        return (res?.data ?? res) as T
    } catch (error: any) {
        // Only show toast for real HTTP errors (axios error); avoid duplicate when we threw in try above
        const isAxiosError = error.response != null
        const msg =
            error.response?.data?.message ||
            error.response?.data?.msg ||
            error.message ||
            '请求失败'
        if (isAxiosError) {
            toast({
                title: '错误',
                description: msg,
                variant: 'destructive'
            })
        }
        throw error
    }
}
