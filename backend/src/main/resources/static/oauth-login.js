const form = document.querySelector('#login-form')
const message = document.querySelector('#message')
const submit = document.querySelector('#submit')
const username = document.querySelector('#username')
const password = document.querySelector('#password')

const contextPath = window.location.pathname.replace(/\/oauth\/login$/, '')
const returnTo = new URLSearchParams(window.location.search).get('return_to')

function safeReturnTarget() {
  if (!returnTo) return null
  try {
    const target = new URL(returnTo, window.location.origin)
    const expectedPath = `${contextPath}/oauth/authorize`
    if (target.origin !== window.location.origin || target.pathname !== expectedPath) return null
    return target.toString()
  } catch (_error) {
    return null
  }
}

const target = safeReturnTarget()
if (!target) {
  message.textContent = '授权请求无效，请返回应用后重新登录。'
  submit.disabled = true
}

form.addEventListener('submit', async (event) => {
  event.preventDefault()
  message.textContent = ''

  if (!username.value.trim() || !password.value) {
    message.textContent = '请输入用户名和密码。'
    return
  }

  submit.disabled = true
  submit.textContent = '正在登录…'
  try {
    const response = await fetch(`${contextPath}/oauth/session/login`, {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value.trim(), password: password.value }),
    })
    const body = await response.json()
    if (!response.ok || body.code !== 0) {
      throw new Error(body.message || '登录失败')
    }
    window.location.assign(target)
  } catch (error) {
    message.textContent = error instanceof Error ? error.message : '登录失败，请稍后重试。'
    submit.disabled = false
    submit.textContent = '登录并授权'
  }
})
