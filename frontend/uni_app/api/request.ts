import {clearSession, getToken} from '@/utils/auth'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.jmi-openatom.cn/api/v1'

// ---------- types ----------

interface BackendResponse<T = unknown> {
    code: number
    message?: string
    data?: T
}

interface RequestConfig {
    url: string
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
    data?: Record<string, unknown> | null
    params?: Record<string, unknown>
    header?: Record<string, string>
    timeout?: number
    responseType?: string
}

interface GetConfig {
    params?: Record<string, unknown>
    responseType?: string
}

// ---------- query builder ----------

function buildQuery(params?: Record<string, unknown>): string {
    if (!params) return ''
    const parts = Object.entries(params)
        .filter(([, v]) => v !== undefined && v !== null)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    return parts.length ? `?${parts.join('&')}` : ''
}

// ---------- request interceptor ----------

function requestInterceptor(config: RequestConfig): RequestConfig {
    const token = getToken()
    if (!token) return config
    return {
        ...config,
        header: {
            ...config.header,
            jmiopenatom: token,
            Authorization: `Bearer ${token}`,
        },
    }
}

// ---------- core ----------

function request<T = unknown>(config: RequestConfig): Promise<T> {
    const finalConfig = requestInterceptor(config)
    const queryString = buildQuery(finalConfig.params)

    return new Promise((resolve, reject) => {
        uni.request({
            url: BASE_URL + finalConfig.url + queryString,
            method: finalConfig.method || 'GET' as any,
            data: finalConfig.data || {},
            header: {
                'Content-Type': 'application/json',
                ...finalConfig.header,
            },
            timeout: finalConfig.timeout || 15000,
            responseType: (finalConfig.responseType || 'text') as any,
            success: (res) => {
                const {statusCode} = res
                const body = res.data as BackendResponse<T>

                if (statusCode === 401) {
                    clearSession()
                    uni.showToast({title: '登录已过期，请重新登录', icon: 'none'})
                    uni.redirectTo({url: '/pages/login/index'})
                    reject(res)
                    return
                }

                if (statusCode !== 200) {
                    const msg = (body && body.message) || `请求错误 ${statusCode}`
                    uni.showToast({title: msg, icon: 'none'})
                    reject(res)
                    return
                }

                if (body && typeof body.code !== 'undefined' && body.code !== 0) {
                    uni.showToast({title: body.message || '操作失败', icon: 'none'})
                    reject(body)
                    return
                }

                resolve(
                    body && Object.prototype.hasOwnProperty.call(body, 'data')
                        ? (body.data as T)
                        : (body as unknown as T),
                )
            },
            fail: (err) => {
                uni.showToast({title: '网络异常', icon: 'none'})
                reject(err)
            },
        })
    })
}

// ---------- convenience methods (matching axios API) ----------

const service = {
    get<T = unknown>(url: string, config?: GetConfig): Promise<T> {
        return request<T>({
            url,
            method: 'GET',
            params: config?.params,
            responseType: config?.responseType,
        })
    },
    post<T = unknown>(url: string, data?: Record<string, unknown>): Promise<T> {
        return request<T>({url, method: 'POST', data})
    },
    put<T = unknown>(url: string, data?: Record<string, unknown>): Promise<T> {
        return request<T>({url, method: 'PUT', data})
    },
    patch<T = unknown>(
        url: string,
        data?: Record<string, unknown> | null,
        config?: GetConfig,
    ): Promise<T> {
        return request<T>({url, method: 'PATCH', data, params: config?.params})
    },
    delete<T = unknown>(url: string, config?: GetConfig): Promise<T> {
        return request<T>({url, method: 'DELETE', params: config?.params})
    },
}

export type {GetConfig, RequestConfig}
export default service
