const API_BASE = localStorage.getItem('lab_api_base') || 'http://localhost:8090/api'
const state = {
  token: localStorage.getItem('lab_token') || '',
  user: JSON.parse(localStorage.getItem('lab_user') || 'null'),
  activeAdminTab: 'overview',
  problem: null,
  submissions: [],
  scoreLogs: [],
  notices: [],
  dashboard: null,
  users: [],
  problems: [],
  adminSubmissions: [],
  checkins: [],
  adminNotices: [],
  settings: null,
  code: '',
  language: 'cpp',
  message: '',
  blurSubmitted: false,
}

const app = document.querySelector('#app')

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (state.token) headers.Authorization = `Bearer ${state.token}`
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || (body.code !== undefined && body.code !== 0)) {
    throw new Error(body.message || response.statusText || '请求失败')
  }
  return body.data
}

function saveSession(payload) {
  state.token = payload.token
  state.user = payload.user
  localStorage.setItem('lab_token', state.token)
  localStorage.setItem('lab_user', JSON.stringify(state.user))
}

function logout() {
  localStorage.removeItem('lab_token')
  localStorage.removeItem('lab_user')
  state.token = ''
  state.user = null
  render()
}

async function login(role) {
  try {
    const isAdmin = role === 'admin'
    const payload = await api('/auth/cms-callback', {
      method: 'POST',
      body: JSON.stringify({
        clubUserId: isAdmin ? 10001 : 20001,
        username: isAdmin ? 'coach' : 'student',
        realName: isAdmin ? '实验室主教练' : '实验室学生',
        isLabMember: true,
      }),
    })
    saveSession(payload)
    await loadStudent()
    if (isAdmin) await loadAdmin()
  } catch (error) {
    state.message = error.message
  }
  render()
}

async function loginBlocked() {
  try {
    await api('/auth/cms-callback', {
      method: 'POST',
      body: JSON.stringify({
        clubUserId: 30001,
        username: 'visitor',
        realName: '非实验室成员',
        isLabMember: false,
      }),
    })
  } catch (error) {
    state.message = error.message
    render()
  }
}

async function loadStudent() {
  if (!state.token) return
  const [user, problem, submissions, scoreLogs, notices] = await Promise.all([
    api('/lab/me'),
    api('/lab/problems/today'),
    api('/lab/submissions'),
    api('/lab/score/logs'),
    api('/lab/notices'),
  ])
  state.user = user
  state.problem = problem
  state.submissions = submissions
  state.scoreLogs = scoreLogs
  state.notices = notices
  localStorage.setItem('lab_user', JSON.stringify(user))
}

async function loadAdmin() {
  if (!state.user || state.user.labRole < 1) return
  const [dashboard, users, problems, adminSubmissions, checkins, adminNotices, settings] =
    await Promise.all([
      api('/admin/dashboard'),
      api('/admin/users'),
      api('/admin/problems'),
      api('/admin/submissions'),
      api('/admin/checkins'),
      api('/admin/notices'),
      api('/admin/settings'),
    ])
  Object.assign(state, { dashboard, users, problems, adminSubmissions, checkins, adminNotices, settings })
}

async function submit(auto = false) {
  if (!state.problem || !state.code.trim()) return
  try {
    const result = await api(`/lab/submissions?problemId=${state.problem.id}`, {
      method: 'POST',
      body: JSON.stringify({ language: state.language, code: state.code }),
    })
    state.message = `${result.status}: ${result.judgeMessage}`
    await loadStudent()
  } catch (error) {
    state.message = error.message
  }
  if (auto && state.message.includes('AC')) state.message = `页面失去焦点，系统已自动交卷：${state.message}`
  render()
}

async function onsiteCheckin() {
  try {
    await api('/lab/checkins/onsite', { method: 'POST' })
    state.message = '现场签到已记录'
    await loadStudent()
  } catch (error) {
    state.message = error.message
  }
  render()
}

async function adminAction(action) {
  try {
    if (action === 'generate') {
      await api('/admin/problems/generate', { method: 'POST' })
      state.message = '今日题目已生成'
    }
    if (action === 'closeDay') {
      const result = await api('/admin/checkins/close-day', { method: 'POST' })
      state.message = `旷课结算完成，新增 ${result.created} 条`
    }
    if (action === 'notice') {
      await api('/admin/notices', {
        method: 'POST',
        body: JSON.stringify({
          title: '实验室通知',
          content: '请关注今日训练和签到状态。',
          type: 'system',
        }),
      })
      state.message = '通知已发送'
    }
    await loadAdmin()
    await loadStudent()
  } catch (error) {
    state.message = error.message
  }
  render()
}

function handleBlurSubmit() {
  if (!state.token || state.blurSubmitted || !state.code.trim()) return
  state.blurSubmitted = true
  submit(true)
}

window.addEventListener('blur', handleBlurSubmit)
document.addEventListener('visibilitychange', () => {
  if (document.hidden) handleBlurSubmit()
})

function renderLogin() {
  app.innerHTML = `
    <main class="login">
      <section class="panel login-card">
        <div class="brand">
          <span class="brand-mark">LMS</span>
          <div>
            <strong>算法实验室管理系统</strong>
            <p class="muted">独立网站，通过 CMS OAuth 获取账号和实验室成员资格。</p>
          </div>
        </div>
        <div class="form-grid" style="margin-top:18px">
          <button data-login="student">模拟 CMS OAuth 登录学生</button>
          <button data-login="admin" class="secondary">模拟 CMS OAuth 登录主教练</button>
          <button data-login="blocked" class="danger">模拟非实验室成员被拦截</button>
          ${state.message ? `<p class="error">${escapeHtml(state.message)}</p>` : ''}
        </div>
      </section>
    </main>
  `
  app.querySelector('[data-login="student"]').onclick = () => login('student')
  app.querySelector('[data-login="admin"]').onclick = () => login('admin')
  app.querySelector('[data-login="blocked"]').onclick = loginBlocked
}

function render() {
  if (!state.token) {
    renderLogin()
    return
  }
  const isAdmin = state.user?.labRole >= 1
  app.innerHTML = `
    <div class="shell">
      <header class="topbar">
        <div class="topbar-inner">
          <div class="brand">
            <span class="brand-mark">LMS</span>
            <div>
              <strong>算法实验室</strong>
              <div class="muted">CMS OAuth / LMS 独立业务闭环</div>
            </div>
          </div>
          <nav class="nav">
            ${isAdmin ? '<button class="secondary" data-admin-refresh>后台刷新</button>' : ''}
            <button class="ghost" data-logout>退出</button>
          </nav>
        </div>
      </header>

      <main class="container">
        <section class="hero">
          <div class="hero-grid">
            <div>
              <span class="eyebrow">Lab Management System</span>
              <h1>每日训练、考勤和信誉分</h1>
              <p class="muted">当前用户：${escapeHtml(state.user?.realName || state.user?.username || '-')} · ${roleText(state.user?.labRole)}</p>
            </div>
            <div class="metrics">
              <article class="metric"><span>信誉分</span><strong>${state.user?.reputationScore ?? '-'}</strong></article>
              <article class="metric"><span>今日题</span><strong>${state.problem?.title ? '已发布' : '-'}</strong></article>
              <article class="metric"><span>提交数</span><strong>${state.submissions.length}</strong></article>
            </div>
          </div>
        </section>

        ${state.message ? `<p class="panel">${escapeHtml(state.message)}</p>` : ''}
        ${renderStudent()}
        ${isAdmin ? renderAdmin() : ''}
      </main>
    </div>
  `
  bindStudent()
  if (isAdmin) bindAdmin()
  app.querySelector('[data-logout]').onclick = logout
  const refresh = app.querySelector('[data-admin-refresh]')
  if (refresh) refresh.onclick = async () => {
    await loadAdmin()
    render()
  }
}

function renderStudent() {
  return `
    <section class="main-grid">
      <article class="panel">
        <div class="panel-head">
          <h2>${escapeHtml(state.problem?.title || '今日题目')}</h2>
          <span class="tag">${escapeHtml(state.problem?.difficulty || 'easy')}</span>
        </div>
        <div class="problem-body">${markdownLite(state.problem?.descriptionMd || '')}</div>
        <div class="case-grid">
          ${(state.problem?.cases || [])
            .filter((item) => item.sampleFlag)
            .map((item) => `<div><strong>样例输入</strong><pre>${escapeHtml(item.inputData)}</pre><strong>样例输出</strong><pre>${escapeHtml(item.expectedOutput)}</pre></div>`)
            .join('')}
        </div>
        <div class="form-grid">
          <div class="row">
            <select data-language>
              <option value="cpp">C++</option>
              <option value="java">Java</option>
              <option value="python">Python</option>
            </select>
            <button data-submit>提交评测</button>
          </div>
          <textarea data-code placeholder="输入代码；页面失去焦点会自动提交一次。">${escapeHtml(state.code)}</textarea>
        </div>
      </article>

      <aside class="side-stack">
        <article class="panel">
          <div class="panel-head">
            <h3>签到</h3>
            <span class="tag warning">${todayCheckinText()}</span>
          </div>
          <p class="muted">AC 今日题会自动签到；现场签到会按后台时间规则判定正常或迟到。</p>
          <button data-onsite class="secondary">现场签到</button>
        </article>
        <article class="panel">
          <div class="panel-head"><h3>通知</h3></div>
          <div class="list">${state.notices.map(renderNotice).join('') || '<p class="muted">暂无通知</p>'}</div>
        </article>
      </aside>
    </section>

    <section class="history-grid">
      <article class="panel">
        <h3>提交记录</h3>
        ${table(['题目', '语言', '状态', '时间'], state.submissions.map((item) => ['#' + item.problemId, item.language, item.status, formatTime(item.createdAt)]))}
      </article>
      <article class="panel">
        <h3>信誉流水</h3>
        ${table(['日期', '考勤', '信誉分', '同步'], state.scoreLogs.map((item) => [item.checkinDate, item.attendanceStatusText, item.localScoreChange, item.clubSyncStatusText]))}
      </article>
    </section>
  `
}

function renderAdmin() {
  return `
    <section class="panel">
      <div class="panel-head">
        <h2>实验室后台</h2>
        <div class="row">
          <button data-admin-action="generate" class="secondary">生成今日题</button>
          <button data-admin-action="closeDay" class="secondary">结算旷课</button>
          <button data-admin-action="notice" class="secondary">发送通知</button>
        </div>
      </div>
      <div class="metrics">
        <article class="metric"><span>成员</span><strong>${state.dashboard?.activeMemberCount ?? 0}</strong></article>
        <article class="metric"><span>题目</span><strong>${state.dashboard?.problemCount ?? 0}</strong></article>
        <article class="metric"><span>今日 AC</span><strong>${state.dashboard?.todayAcceptedCount ?? 0}</strong></article>
      </div>
      <div class="tabs">
        ${['overview', 'users', 'problems', 'submissions', 'checkins', 'notices']
          .map((tab) => `<button data-tab="${tab}" class="${state.activeAdminTab === tab ? 'active' : ''}">${tabLabel(tab)}</button>`)
          .join('')}
      </div>
      ${renderAdminTab()}
    </section>
  `
}

function renderAdminTab() {
  if (state.activeAdminTab === 'users') {
    return table(['ID', 'CMS用户', '姓名', '身份', '信誉分', '状态'], state.users.map((u) => [u.id, u.clubUserId, u.realName || u.username, roleText(u.labRole), u.reputationScore, u.active ? '启用' : '停用']))
  }
  if (state.activeAdminTab === 'problems') {
    return table(['ID', '标题', '日期', '状态'], state.problems.map((p) => [p.id, p.title, p.publishDate, p.status]))
  }
  if (state.activeAdminTab === 'submissions') {
    return table(['ID', '题目', '成员', '语言', '状态'], state.adminSubmissions.map((s) => [s.id, s.problemId, s.labUserId, s.language, s.status]))
  }
  if (state.activeAdminTab === 'checkins') {
    return table(['ID', '成员', '日期', '考勤', '信誉分', '同步'], state.checkins.map((c) => [c.id, c.labUserId, c.checkinDate, c.attendanceStatusText, c.localScoreChange, c.clubSyncStatusText]))
  }
  if (state.activeAdminTab === 'notices') {
    return table(['ID', '接收人', '标题', '类型'], state.adminNotices.map((n) => [n.id, n.receiverLabUserId || '全员', n.title, n.type]))
  }
  return table(['指标', '数值'], Object.entries(state.dashboard || {}))
}

function bindStudent() {
  const code = app.querySelector('[data-code]')
  const language = app.querySelector('[data-language]')
  if (code) code.oninput = (event) => (state.code = event.target.value)
  if (language) {
    language.value = state.language
    language.onchange = (event) => (state.language = event.target.value)
  }
  app.querySelector('[data-submit]').onclick = () => submit(false)
  app.querySelector('[data-onsite]').onclick = onsiteCheckin
}

function bindAdmin() {
  app.querySelectorAll('[data-admin-action]').forEach((button) => {
    button.onclick = () => adminAction(button.dataset.adminAction)
  })
  app.querySelectorAll('[data-tab]').forEach((button) => {
    button.onclick = () => {
      state.activeAdminTab = button.dataset.tab
      render()
    }
  })
}

function renderNotice(item) {
  return `<article class="list-item"><strong>${escapeHtml(item.title)}</strong><p>${escapeHtml(item.content)}</p><p class="muted">${formatTime(item.createdAt)}</p></article>`
}

function table(headers, rows) {
  return `<table><thead><tr>${headers.map((h) => `<th>${escapeHtml(String(h))}</th>`).join('')}</tr></thead><tbody>${rows
    .map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(String(cell ?? '-'))}</td>`).join('')}</tr>`)
    .join('')}</tbody></table>`
}

function todayCheckinText() {
  const today = new Date().toISOString().slice(0, 10)
  const item = state.scoreLogs.find((log) => log.checkinDate === today)
  return item ? item.attendanceStatusText : '未签到'
}

function tabLabel(tab) {
  return {
    overview: '总览',
    users: '成员',
    problems: '题目',
    submissions: '提交',
    checkins: '考勤',
    notices: '通知',
  }[tab]
}

function roleText(role = 0) {
  return role >= 2 ? '主教练' : role >= 1 ? '助教' : '学生'
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

function markdownLite(value) {
  return escapeHtml(value)
    .replace(/^### (.*)$/gm, '<h3>$1</h3>')
    .replace(/^## (.*)$/gm, '<h2>$1</h2>')
    .replace(/^# (.*)$/gm, '<h1>$1</h1>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

if (state.token) {
  loadStudent()
    .then(loadAdmin)
    .catch((error) => {
      state.message = error.message
      logout()
    })
    .finally(render)
} else {
  render()
}
