// ============================================================
// Model Layer: HTTP request wrapper
// Unified request/response interceptor for uni.request
// ============================================================

const BASE_URL = '' // Replace with your API base URL

const request = (options = {}) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...options.header
      },
      timeout: options.timeout || 15000,
      success: (res) => {
        const { statusCode, data } = res
        if (statusCode === 200) {
          // Adapt to your backend response format
          if (data.code === 0 || data.code === 200) {
            resolve(data.data ?? data)
          } else {
            uni.showToast({ title: data.message || '请求失败', icon: 'none' })
            reject(data)
          }
        } else if (statusCode === 401) {
          uni.showToast({ title: '登录已过期', icon: 'none' })
          reject(res)
        } else {
          uni.showToast({ title: `请求错误 ${statusCode}`, icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

// Convenience methods
const get = (url, data, opts) => request({ ...opts, url, method: 'GET', data })
const post = (url, data, opts) => request({ ...opts, url, method: 'POST', data })
const put = (url, data, opts) => request({ ...opts, url, method: 'PUT', data })
const del = (url, data, opts) => request({ ...opts, url, method: 'DELETE', data })
const patch = (url, data, opts) => {request({ ...opts, url, method: 'PATCH', data })}

export { get, post, put, del }
export default request
