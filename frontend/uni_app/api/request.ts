import {clearSession, currentPageUrl, getToken, loginUrl} from '@/utils/auth'

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
    retry?: number
    cache?: boolean
}

interface GetConfig {
    params?: Record<string, unknown>
    responseType?: string
    retry?: number
    cache?: boolean
}

const responseCache = new Map<string, unknown>()
let lastToast = ''
let lastToastAt = 0

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

function showToast(message: string): void {
    const now = Date.now()
    if (message === lastToast && now - lastToastAt < 1800) return
    lastToast = message
    lastToastAt = now
    uni.showToast({title: message, icon: 'none'})
}

// ---------- core ----------

function request<T = unknown>(config: RequestConfig): Promise<T> {
    const finalConfig = requestInterceptor(config)
    const queryString = buildQuery(finalConfig.params)
    const method = finalConfig.method || 'GET'
    const maxRetries = method === 'GET' ? (finalConfig.retry ?? 1) : 0
    const cacheEnabled = method === 'GET' && finalConfig.cache !== false
    const cacheKey = `${getToken() || 'anonymous'}:${method}:${finalConfig.url}${queryString}`

    return new Promise((resolve, reject) => {
        const fallbackOrReject = (error: unknown, message: string) => {
            if (cacheEnabled && responseCache.has(cacheKey)) {
                showToast('网络较弱，已显示最近数据')
                resolve(responseCache.get(cacheKey) as T)
                return
            }
            showToast(message)
            reject(error)
        }

        const run = (attempt: number) => {
            uni.request({
                url: BASE_URL + finalConfig.url + queryString,
                method: method as any,
                data: finalConfig.data || {},
                header: {
                    'Content-Type': 'application/json',
                    ...finalConfig.header,
                },
                timeout: finalConfig.timeout ?? 20000,
                responseType: (finalConfig.responseType || 'text') as any,
                success: (res) => {
                    const {statusCode} = res
                    const body = res.data as BackendResponse<T>

                    if (statusCode === 401) {
                        const redirect = currentPageUrl()
                        responseCache.clear()
                        clearSession()
                        showToast('登录已过期，请重新登录')
                        uni.redirectTo({url: loginUrl(redirect)})
                        reject(res)
                        return
                    }

                    if (
                        method === 'GET' &&
                        attempt < maxRetries &&
                        [408, 425, 429, 500, 502, 503, 504].includes(statusCode)
                    ) {
                        setTimeout(() => run(attempt + 1), 500 * 2 ** attempt)
                        return
                    }

                    if (statusCode < 200 || statusCode >= 300) {
                        const msg = (body && body.message) || `请求错误 ${statusCode}`
                        fallbackOrReject(res, msg)
                        return
                    }

                    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
                        showToast(body.message || '操作失败')
                        reject(body)
                        return
                    }

                    const result =
                        body && Object.prototype.hasOwnProperty.call(body, 'data')
                            ? (body.data as T)
                            : (body as unknown as T)
                    if (cacheEnabled) responseCache.set(cacheKey, result)
                    resolve(result)
                },
                fail: (err) => {
                    if (method === 'GET' && attempt < maxRetries) {
                        setTimeout(() => run(attempt + 1), 500 * 2 ** attempt)
                        return
                    }
                    const timedOut = /timeout/i.test(err.errMsg || '')
                    fallbackOrReject(err, timedOut ? '网络响应超时，请重试' : '网络异常，请检查连接')
                },
            })
        }

        run(0)
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
            retry: config?.retry,
            cache: config?.cache,
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
