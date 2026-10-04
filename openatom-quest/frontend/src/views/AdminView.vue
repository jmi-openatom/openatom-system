<template>
  <div class="admin-dashboard">
    <aside class="admin-sidebar">
      <div class="admin-sidebar__brand"><img src="/logo.png?v=20261005" alt="" /><span><strong>OPENATOM QUEST</strong><small>管理工作台</small></span></div>
      <p class="admin-sidebar__label">工作区</p>
      <nav class="admin-sidebar__nav" aria-label="管理后台导航">
        <button v-for="item in sections" :key="item" type="button" :class="{ active: section === item }" :aria-current="section === item ? 'page' : undefined" @click="section = item">{{ item }}</button>
      </nav>
      <router-link class="admin-sidebar__back" to="/dashboard">← 返回成员工作台</router-link>
    </aside>

    <main class="admin-workspace">
    <header class="admin-page-heading"><div><p class="eyebrow">PLATFORM OPERATIONS</p><h1>{{ section }}</h1><p>管理 Quest 任务、成员成长与平台配置。</p></div><el-button @click="refreshAll">刷新数据</el-button></header>
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="error" icon="error" title="管理数据加载失败" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>

    <template v-else>
      <section v-if="section === '数据概览'" class="admin-section">
        <div class="metric-grid"><article><span>成员总数</span><strong>{{ stats.members?.total || 0 }}</strong><small>近 30 天新增 {{ stats.members?.newIn30Days || 0 }}</small></article><article><span>任务领取</span><strong>{{ stats.tasks?.assignments || 0 }}</strong><small>完成率 {{ stats.tasks?.completionRate || 0 }}%</small></article><article><span>已完成</span><strong>{{ stats.tasks?.passed || 0 }}</strong><small>成员任务已通过</small></article><article class="admin-metric--alert"><span>当前逾期</span><strong>{{ overdueTotal }}</strong><button type="button" @click="showOverdue">查看逾期记录 →</button></article><article><span>待审核</span><strong>{{ stats.reviews?.pending || 0 }}</strong><small>平均 {{ stats.reviews?.averageHours || 0 }} 小时</small></article></div>
        <div class="admin-overview-grid">
          <article class="content-card admin-table-card"><div class="admin-card-title"><h2>需要关注的成员</h2><button type="button" @click="section = '成员进度'">查看全部 →</button></div><div v-if="!atRiskMembers.length" class="admin-empty">暂无逾期成员</div><button v-for="item in atRiskMembers" :key="item.memberId" class="admin-risk-row" type="button" @click="openMemberAssignments(item)"><span><strong>{{ item.nickname || `成员 #${item.memberId}` }}</strong><small>{{ item.email || item.currentLevel }}</small></span><span>{{ item.overdue }} 项逾期 →</span></button></article>
          <article class="content-card admin-table-card"><h2>平台概况</h2><div class="admin-facts"><div><span>OAuth 登录成功率</span><strong>{{ stats.oauth?.successRate || 0 }}%</strong></div><div><span>技术方向</span><strong>{{ stats.directions?.length || 0 }}</strong></div><div><span>活跃成员</span><strong>{{ stats.members?.active || 0 }}</strong></div></div></article>
        </div>
        <article class="content-card admin-table-card"><h2>技术方向人数</h2><div class="direction-bars"><div v-for="item in stats.directions || []" :key="item.name"><span>{{ item.name }}</span><strong>{{ item.memberCount }}</strong></div></div></article>
      </section>

      <section v-else-if="section === '成长路线'" class="admin-section">
        <div class="admin-toolbar"><div><h2>成长路线</h2><p>先创建路线并配置阶段，再发布任务。</p></div><el-button type="primary" @click="routeDialog = true">新建路线</el-button></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>路线</th><th>技术方向</th><th>阶段</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in routes" :key="item.id"><td><strong>{{ item.name }}</strong><small>{{ item.routeKey }}</small></td><td>{{ item.directionName }}</td><td>{{ item.stageCount }}</td><td><span class="status-chip" :data-status="item.status">{{ statusText[item.status] || item.status }}</span></td><td><el-button v-if="item.status === 'DRAFT'" link type="primary" @click="publish(item)">发布</el-button><el-button v-if="item.status !== 'ARCHIVED'" link type="danger" @click="archiveRouteItem(item)">归档</el-button></td></tr></tbody></table></div>
      </section>

      <section v-else-if="section === '任务管理'" class="admin-section">
        <div class="admin-toolbar"><div><h2>任务管理</h2><p>已产生提交的任务只允许归档，历史记录不会删除。</p></div><el-button type="primary" @click="taskDialog = true">新建任务</el-button></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>任务</th><th>阶段</th><th>负责人</th><th>领取 / 通过</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in tasks" :key="item.id"><td><strong>{{ item.title }}</strong><small>{{ item.taskKey }}</small></td><td>{{ item.stageKey || '-' }}</td><td>{{ item.ownerName }}</td><td>{{ item.assignmentCount || 0 }} / {{ item.passedCount || 0 }}</td><td><span class="status-chip" :data-status="item.status">{{ statusText[item.status] || item.status }}</span></td><td><el-button v-if="item.status === 'DRAFT' || item.status === 'OFFLINE'" link type="primary" @click="setTaskStatus(item, 'PUBLISHED')">{{ item.status === 'OFFLINE' ? '重新发布' : '发布' }}</el-button><el-button v-if="item.status === 'PUBLISHED'" link type="primary" @click="openAssignment(item)">分配</el-button><el-button v-if="item.status === 'PUBLISHED'" link @click="setTaskStatus(item, 'OFFLINE')">下架</el-button><el-button v-if="item.status !== 'ARCHIVED'" link type="danger" @click="archiveTask(item)">归档</el-button></td></tr></tbody></table></div>
      </section>

      <section v-else-if="section === '成员进度'" class="admin-section">
        <div class="admin-toolbar"><div><h2>每位成员的任务进度</h2><p>统计所有已领取或已分配任务；到期但尚未被定时任务更新的记录也会按逾期显示。</p></div><el-input v-model="progressSearch" clearable placeholder="搜索成员姓名或邮箱" class="admin-search" /></div>
        <div class="data-table-wrap admin-progress-table"><table class="data-table"><thead><tr><th>成员</th><th>完成进度</th><th>进行中</th><th>待审核</th><th>逾期</th><th>操作</th></tr></thead><tbody><tr v-for="item in filteredProgress" :key="item.memberId"><td><strong>{{ item.nickname || `成员 #${item.memberId}` }}</strong><small>{{ item.email || item.currentLevel }}</small></td><td><div class="admin-progress-cell"><strong>{{ item.passed }} / {{ item.total }}</strong><span class="admin-progress-track"><span :style="{ width: `${item.total ? item.passed / item.total * 100 : 0}%` }"></span></span></div></td><td>{{ item.inProgress }}</td><td>{{ item.pendingReview }}</td><td><span class="status-chip" :data-status="item.overdue ? 'OVERDUE' : ''">{{ item.overdue }}</span></td><td><el-button link type="primary" @click="openMemberAssignments(item)">查看任务</el-button></td></tr><tr v-if="!filteredProgress.length"><td colspan="6" class="admin-empty">没有匹配的成员</td></tr></tbody></table></div>
        <div class="admin-progress-cards"><article v-for="item in filteredProgress" :key="item.memberId" class="content-card"><div class="admin-progress-card__heading"><span><strong>{{ item.nickname || `成员 #${item.memberId}` }}</strong><small>{{ item.email || item.currentLevel }}</small></span><el-button link type="primary" @click="openMemberAssignments(item)">查看任务</el-button></div><div class="admin-progress-card__bar"><strong>{{ item.passed }} / {{ item.total }} 已完成</strong><span class="admin-progress-track"><span :style="{ width: `${item.total ? item.passed / item.total * 100 : 0}%` }"></span></span></div><div class="admin-progress-card__metrics"><span>进行中 {{ item.inProgress }}</span><span>待审核 {{ item.pendingReview }}</span><span :class="{ 'admin-text-warning': item.overdue }">逾期 {{ item.overdue }}</span></div></article><p v-if="!filteredProgress.length" class="admin-empty">没有匹配的成员</p></div>

        <div class="admin-toolbar"><div><h2>任务领取记录</h2><p>按成员、任务和状态筛选，逾期任务可由管理员设置新期限后手动重启。</p></div><el-button :loading="assignmentsLoading" @click="loadAssignments">刷新记录</el-button></div>
        <div class="admin-filters"><el-select v-model="assignmentFilters.memberId" clearable filterable placeholder="全部成员" aria-label="按成员筛选" @change="resetAssignmentPage"><el-option v-for="item in members" :key="item.id" :label="item.nickname || `成员 #${item.id}`" :value="item.id" /></el-select><el-select v-model="assignmentFilters.taskId" clearable filterable placeholder="全部任务" aria-label="按任务筛选" @change="resetAssignmentPage"><el-option v-for="item in tasks" :key="item.id" :label="item.title" :value="item.id" /></el-select><el-select v-model="assignmentFilters.status" aria-label="按状态筛选" @change="resetAssignmentPage"><el-option v-for="item in assignmentStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select><el-button @click="clearAssignmentFilters">重置筛选</el-button><span class="admin-filters__count">共 {{ assignmentTotal }} 条记录</span></div>
        <el-skeleton v-if="assignmentsLoading" :rows="5" animated />
        <el-result v-else-if="assignmentsError" icon="error" title="任务记录加载失败" :sub-title="assignmentsError"><template #extra><el-button @click="loadAssignments">重试</el-button></template></el-result>
        <div v-else class="data-table-wrap"><table class="data-table"><thead><tr><th>成员</th><th>任务</th><th>状态</th><th>领取时间</th><th>截止时间</th><th>提交次数</th><th>操作</th></tr></thead><tbody><tr v-for="item in assignments" :key="item.id"><td><strong>{{ item.memberName || `成员 #${item.memberId}` }}</strong><small>{{ item.memberEmail || '-' }}</small></td><td><strong>{{ item.taskTitle }}</strong><small>{{ item.taskKey }}</small></td><td><span class="status-chip" :data-status="item.status">{{ assignmentStatusText[item.status] || item.status }}</span></td><td>{{ formatDate(item.claimedAt) }}</td><td>{{ item.dueAt ? formatDate(item.dueAt) : '无固定截止' }}</td><td>{{ item.submissionCount }}</td><td><el-button v-if="item.status === 'OVERDUE'" link type="primary" @click="openRestart(item)">重启任务</el-button><span v-else>—</span></td></tr><tr v-if="!assignments.length"><td colspan="7" class="admin-empty">暂无任务记录</td></tr></tbody></table></div>
        <div v-if="assignmentTotal > assignmentFilters.size" class="admin-pagination"><span>共 {{ assignmentTotal }} 条 · 第 {{ assignmentFilters.page }} 页</span><el-button :disabled="assignmentFilters.page <= 1 || assignmentsLoading" @click="changeAssignmentPage(-1)">上一页</el-button><el-button :disabled="assignmentFilters.page * assignmentFilters.size >= assignmentTotal || assignmentsLoading" @click="changeAssignmentPage(1)">下一页</el-button></div>
      </section>

      <section v-else-if="section === '成员管理'" class="admin-section">
        <div class="admin-toolbar"><div><h2>成员与角色</h2><p>角色权限由 Quest 独立管理；关闭“排行榜显示”后，该成员将从所有成员的排行榜中移除。</p></div></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>成员</th><th>方向</th><th>等级 / 积分</th><th>角色</th><th>排行榜显示</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in members" :key="item.id"><td><strong>{{ item.nickname || '未填写昵称' }}</strong><small>{{ item.email || `成员 #${item.id}` }}</small></td><td>{{ item.directions || '-' }}</td><td>{{ item.currentLevel }} / {{ item.totalPoints }}</td><td><el-select :model-value="roleList(item.roles)" multiple collapse-tags aria-label="成员角色" @change="(value: string[]) => saveRoles(item, value)"><el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" /></el-select></td><td><el-switch :model-value="Boolean(item.leaderboardVisible)" :disabled="leaderboardUpdatingId === item.id" :aria-label="`${item.nickname || `成员 #${item.id}`}在排行榜中显示`" active-text="显示" inactive-text="隐藏" @change="(value: boolean) => saveLeaderboardVisibility(item, value)" /></td><td><el-switch :model-value="item.status === 'ACTIVE'" active-text="启用" inactive-text="禁用" @change="(value: boolean) => changeMemberStatus(item, value)" /></td><td><el-button link type="primary" @click="openMemberEditor(item)">编辑资料</el-button></td></tr></tbody></table></div>
      </section>

      <section v-else-if="section === '平台配置'" class="admin-section">
        <div class="admin-toolbar"><div><h2>系统公告</h2><p>发布后将同步生成站内通知，并展示在成员工作台。</p></div><el-button type="primary" @click="announcementDialog = true">新建公告</el-button></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>公告</th><th>状态</th><th>发布时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in announcements" :key="item.id"><td><strong>{{ item.title }}</strong><small>{{ item.content }}</small></td><td><span class="status-chip" :data-status="item.status">{{ statusText[item.status] || item.status }}</span></td><td>{{ item.publishedAt ? formatDate(item.publishedAt) : '-' }}</td><td><el-button v-if="item.status === 'DRAFT'" link type="primary" @click="publishNotice(item)">发布</el-button></td></tr></tbody></table></div>
        <div class="admin-toolbar config-heading"><div><h2>技术方向</h2><p>技术方向可停用但不会删除成员与路线历史。</p></div><el-button type="primary" @click="directionDialog = true">新建方向</el-button></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>标识</th><th>名称</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in directions" :key="item.id"><td><code>{{ item.directionKey }}</code></td><td><el-input v-model="item.name" aria-label="技术方向名称" /></td><td><el-input-number v-model="item.sortOrder" :min="0" aria-label="技术方向排序" /></td><td><el-switch v-model="item.active" active-text="启用" inactive-text="停用" /></td><td><el-button link type="primary" @click="saveDirection(item)">保存</el-button></td></tr></tbody></table></div>
        <div class="admin-toolbar config-heading"><div><h2>等级规则</h2><p>积分发放后按当前启用的最低积分门槛计算等级。</p></div></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>等级</th><th>名称</th><th>最低积分</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in levelRules" :key="item.levelKey"><td><strong>{{ item.levelKey }}</strong></td><td><el-input v-model="item.name" aria-label="等级名称" /></td><td><el-input-number v-model="item.minimumPoints" :min="0" aria-label="等级最低积分" /></td><td><el-switch v-model="item.active" active-text="启用" inactive-text="停用" /></td><td><el-button link type="primary" @click="saveLevelRule(item)">保存</el-button></td></tr></tbody></table></div>
      </section>

      <section v-else class="admin-section">
        <div class="admin-toolbar"><div><h2>操作日志</h2><p>保留 OAuth、权限、审核、积分和管理操作的可追溯记录。</p></div></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>操作者</th><th>操作</th><th>对象</th><th>详情</th></tr></thead><tbody><tr v-for="item in audits" :key="item.id"><td>{{ formatDate(item.createdAt) }}</td><td>{{ item.actorName || '系统 / 未登录用户' }}</td><td>{{ item.action }}</td><td>{{ item.targetType }} #{{ item.targetId || '-' }}</td><td><code>{{ item.detail }}</code></td></tr></tbody></table></div>
      </section>
    </template>

    <el-dialog v-model="routeDialog" title="新建成长路线" width="min(620px, calc(100vw - 32px))">
      <el-form label-position="top"><el-form-item label="路线名称" required><el-input v-model="routeForm.name" /></el-form-item><el-form-item label="路线标识" required><el-input v-model="routeForm.routeKey" placeholder="frontend-growth" /></el-form-item><el-form-item label="技术方向" required><el-select v-model="routeForm.directionId" style="width:100%"><el-option v-for="item in directions.filter(value => value.active)" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="路线说明"><el-input v-model="routeForm.description" type="textarea" :rows="3" /></el-form-item><el-form-item label="成长阶段"><el-checkbox-group v-model="routeForm.stageKeys"><el-checkbox v-for="item in stages" :key="item.stageKey" :label="item.stageKey">{{ item.stageKey }} {{ item.name }}</el-checkbox></el-checkbox-group></el-form-item></el-form>
      <template #footer><el-button @click="routeDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRoute">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="taskDialog" title="新建任务" width="min(760px, calc(100vw - 32px))">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="任务名称" required><el-input v-model="taskForm.title" /></el-form-item><el-form-item label="任务标识" required><el-input v-model="taskForm.taskKey" placeholder="git-first-issue" /></el-form-item>
          <el-form-item label="任务类型" required><el-select v-model="taskForm.taskType" style="width:100%"><el-option label="新人必做" value="ONBOARDING"/><el-option label="技术学习" value="LEARNING"/><el-option label="实践挑战" value="CHALLENGE"/><el-option label="真实项目" value="REAL_PROJECT"/></el-select></el-form-item><el-form-item label="难度" required><el-select v-model="taskForm.difficulty" style="width:100%"><el-option label="入门" value="ENTRY"/><el-option label="初级" value="BEGINNER"/><el-option label="中级" value="INTERMEDIATE"/><el-option label="高级" value="ADVANCED"/></el-select></el-form-item>
          <el-form-item label="成长路线"><el-select v-model="taskForm.routeId" clearable style="width:100%"><el-option v-for="item in routes" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="成长阶段"><el-select v-model="taskForm.stageId" clearable style="width:100%"><el-option v-for="item in stages" :key="item.id" :label="`${item.stageKey} ${item.name}`" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="预计用时（分钟）" required><el-input-number v-model="taskForm.estimatedMinutes" :min="1" style="width:100%" /></el-form-item><el-form-item label="通过积分" required><el-input-number v-model="taskForm.points" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="截止规则"><el-select v-model="taskForm.deadlineType" style="width:100%"><el-option label="无固定截止" value="NONE"/><el-option label="固定时间" value="FIXED"/><el-option label="领取后倒计时" value="AFTER_CLAIM"/></el-select></el-form-item>
          <el-form-item v-if="taskForm.deadlineType === 'FIXED'" label="固定截止时间" required><el-input v-model="taskForm.fixedDeadline" type="datetime-local" /></el-form-item>
          <el-form-item v-if="taskForm.deadlineType === 'AFTER_CLAIM'" label="领取后小时数" required><el-input-number v-model="taskForm.durationHours" :min="1" style="width:100%" /></el-form-item>
          <el-form-item label="提交次数上限"><el-input-number v-model="taskForm.submissionLimit" :min="1" style="width:100%" /></el-form-item><el-form-item label="领取人数上限"><el-input-number v-model="taskForm.capacity" :min="1" clearable style="width:100%" /></el-form-item>
          <el-form-item label="任务负责人"><el-select v-model="taskForm.ownerMemberId" clearable placeholder="默认当前管理员" style="width:100%"><el-option v-for="item in taskOwnerOptions" :key="item.id" :label="item.nickname || `成员 #${item.id}`" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="前置任务"><el-select v-model="taskForm.prerequisiteTaskIds" multiple clearable style="width:100%"><el-option v-for="item in tasks.filter(value => value.status === 'PUBLISHED')" :key="item.id" :label="item.title" :value="item.id" /></el-select></el-form-item>
        </div>
        <el-form-item label="任务简介" required><el-input v-model="taskForm.summary" type="textarea" :rows="2" /></el-form-item><el-form-item label="学习目标（每行一项）" required><el-input v-model="taskForm.learningObjectivesText" type="textarea" :rows="3" /></el-form-item><el-form-item label="操作步骤" required><el-input v-model="taskForm.instructions" type="textarea" :rows="4" /></el-form-item><el-form-item label="参考资料（每行一个链接）"><el-input v-model="taskForm.resourcesText" type="textarea" :rows="2" /></el-form-item><el-form-item label="提交要求" required><el-input v-model="taskForm.submissionRequirements" type="textarea" :rows="3" /></el-form-item><el-form-item label="验收标准" required><el-input v-model="taskForm.acceptanceCriteria" type="textarea" :rows="3" /></el-form-item><el-form-item label="常见问题（每行一项）"><el-input v-model="taskForm.faqText" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="taskDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveTask">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="assignmentDialog" title="直接分配任务" width="min(520px, calc(100vw - 32px))">
      <p v-if="assignmentTask" class="dialog-intro">将任务“{{ assignmentTask.title }}”直接分配给指定成员。</p>
      <el-form label-position="top">
        <el-form-item label="选择成员" required>
          <el-select v-model="assignmentMemberId" filterable placeholder="按昵称或成员编号选择" style="width:100%">
            <el-option v-for="item in activeMembers" :key="item.id" :label="`${item.nickname || '未填写昵称'} · #${item.id}`" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="assignmentDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAssignment">确认分配</el-button></template>
    </el-dialog>

    <el-dialog v-model="memberDialog" title="编辑成员资料" width="min(760px, calc(100vw - 32px))">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="昵称" required><el-input v-model="memberForm.nickname" maxlength="64" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="memberForm.email" /></el-form-item>
          <el-form-item label="学校"><el-input v-model="memberForm.school" /></el-form-item>
          <el-form-item label="学院"><el-input v-model="memberForm.college" /></el-form-item>
          <el-form-item label="专业"><el-input v-model="memberForm.major" /></el-form-item>
          <el-form-item label="年级"><el-input v-model="memberForm.grade" /></el-form-item>
          <el-form-item label="头像地址（成员下次登录时从 LMS 同步）"><el-input v-model="memberForm.avatarUrl" /></el-form-item>
          <el-form-item label="代码主页"><el-input v-model="memberForm.codeProfileUrl" /></el-form-item>
          <el-form-item label="每周投入时间"><el-input-number v-model="memberForm.weeklyHours" :min="0" :max="168" style="width:100%" /></el-form-item>
          <el-form-item label="总积分"><el-input-number v-model="memberForm.totalPoints" :min="0" style="width:100%" /><small class="field-help">成员等级将按积分门槛自动重算</small></el-form-item>
        </div>
        <el-form-item label="技术方向"><el-select v-model="memberForm.directionIds" multiple filterable style="width:100%"><el-option v-for="item in directions.filter(value => value.active)" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="技能"><el-select v-model="memberForm.skills" multiple allow-create filterable default-first-option style="width:100%"><el-option v-for="skill in skillOptions" :key="skill" :label="skill" :value="skill" /></el-select></el-form-item>
        <el-form-item label="个人简介"><el-input v-model="memberForm.bio" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="memberDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveMemberProfile">保存成员资料</el-button></template>
    </el-dialog>

    <el-dialog v-model="directionDialog" title="新建技术方向" width="min(520px, calc(100vw - 32px))">
      <el-form label-position="top"><el-form-item label="方向名称" required><el-input v-model="directionForm.name" /></el-form-item><el-form-item label="方向标识" required><el-input v-model="directionForm.directionKey" placeholder="cloud-native" /></el-form-item><el-form-item label="排序"><el-input-number v-model="directionForm.sortOrder" :min="0" style="width:100%" /></el-form-item></el-form>
      <template #footer><el-button @click="directionDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveNewDirection">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="announcementDialog" title="新建系统公告" width="min(620px, calc(100vw - 32px))">
      <el-form label-position="top"><el-form-item label="公告标题" required><el-input v-model="announcementForm.title" /></el-form-item><el-form-item label="公告内容" required><el-input v-model="announcementForm.content" type="textarea" :rows="6" /></el-form-item><el-form-item label="失效时间（可选）"><el-input v-model="announcementForm.expiresAt" type="datetime-local" /></el-form-item></el-form>
      <template #footer><el-button @click="announcementDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAnnouncement">保存草稿</el-button></template>
    </el-dialog>
    <el-dialog v-model="restartDialog" title="重启逾期任务" width="min(520px, calc(100vw - 32px))">
      <p class="admin-dialog-hint">为 {{ restartTarget?.memberName || '该成员' }} 的「{{ restartTarget?.taskTitle }}」设置新的截止时间。原有提交、审核和积分历史会保留。</p>
      <el-form label-position="top"><el-form-item label="新的截止时间" required><el-input v-model="restartForm.dueAt" type="datetime-local" /></el-form-item><el-form-item label="重启原因" required><el-input v-model.trim="restartForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明为什么允许重新开始" /></el-form-item></el-form>
      <template #footer><el-button @click="restartDialog = false">取消</el-button><el-button type="primary" :loading="restarting" @click="confirmRestart">确认重启</el-button></template>
    </el-dialog>
    </main>
  </div>
</template>

<script setup lang="ts">
import 'element-plus/es/components/dialog/style/css'
import { ElCheckboxGroup, ElDialog, ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getErrorMessage } from '@/api/http'
import { archiveRoute, assignTask, changeTaskStatus, createAnnouncement, createDirection, createRoute, createTask, getAdminAssignments, getAdminDirections, getAdminMemberProgress, getAdminMembers, getAdminRoutes, getAdminStats, getAdminTasks, getAnnouncements, getAuditLogs, getLevelRules, getStages, publishAnnouncement, publishRoute, restartAdminAssignment, updateDirection, updateLeaderboardVisibility, updateLevelRule, updateMemberProfile, updateMemberRoles, updateMemberStatus, type AdminAssignment, type MemberTaskProgress } from '@/api/quest'
const sections = ['数据概览', '任务管理', '成员进度', '成长路线', '成员管理', '平台配置', '操作日志']
const route = useRoute()
const router = useRouter()
const section = computed({
  get: () => sections.includes(String(route.query.section || '')) ? String(route.query.section) : '数据概览',
  set: (value: string) => { void router.replace({ name: 'admin', query: { section: value } }) },
})
const progressSearch = ref('')
const progress = ref<MemberTaskProgress[]>([])
const assignmentFilters = reactive({ memberId: undefined as number | undefined, taskId: undefined as number | undefined, status: 'ALL', page: 1, size: 20 })
const assignmentStatuses = [{ label: '全部状态', value: 'ALL' }, { label: '逾期', value: 'OVERDUE' }, { label: '进行中', value: 'IN_PROGRESS' }, { label: '待审核', value: 'PENDING_REVIEW' }, { label: '需修改', value: 'REVISION_REQUIRED' }, { label: '已通过', value: 'PASSED' }, { label: '已放弃', value: 'ABANDONED' }]
const assignmentStatusText: Record<string, string> = { IN_PROGRESS: '进行中', PENDING_REVIEW: '待审核', REVISION_REQUIRED: '需修改', PASSED: '已通过', OVERDUE: '已逾期', ABANDONED: '已放弃' }
const assignments = ref<AdminAssignment[]>([])
const assignmentTotal = ref(0)
const assignmentsLoading = ref(false)
const assignmentsError = ref('')
let assignmentRequestId = 0
const restartDialog = ref(false)
const restartTarget = ref<AdminAssignment | null>(null)
const restarting = ref(false)
const restartForm = reactive({ dueAt: '', reason: '' })
const overdueTotal = computed(() => progress.value.reduce((total, item) => total + Number(item.overdue || 0), 0))
const atRiskMembers = computed(() => progress.value.filter(item => Number(item.overdue) > 0).slice(0, 5))
const filteredProgress = computed(() => {
  const query = progressSearch.value.trim().toLowerCase()
  return query ? progress.value.filter(item => `${item.nickname || ''} ${item.email || ''} ${item.memberId}`.toLowerCase().includes(query)) : progress.value
})
const leaderboardUpdatingId = ref<number | null>(null)
const loading = ref(true), saving = ref(false), error = ref(''), routeDialog = ref(false), taskDialog = ref(false), directionDialog = ref(false), announcementDialog = ref(false), assignmentDialog = ref(false), memberDialog = ref(false)
const stats = ref<Record<string, any>>({}), routes = ref<Record<string, any>[]>([]), tasks = ref<Record<string, any>[]>([]), members = ref<Record<string, any>[]>([]), audits = ref<Record<string, any>[]>([]), directions = ref<Record<string, any>[]>([]), stages = ref<Record<string, any>[]>([]), levelRules = ref<Record<string, any>[]>([]), announcements = ref<Record<string, any>[]>([])
const roleOptions = [{label:'新成员',value:'MEMBER'},{label:'导师',value:'MENTOR'},{label:'项目负责人',value:'PROJECT_OWNER'},{label:'管理员',value:'ADMIN'}]
const statusText: Record<string,string> = { DRAFT:'草稿', PUBLISHED:'已发布', OFFLINE:'已下架', ARCHIVED:'已归档' }
const routeForm = reactive({ name:'', routeKey:'', description:'', directionId:undefined as number|undefined, stageKeys:['L0','L1','L2','L3','L4'] })
const taskForm = reactive({ title:'', taskKey:'', summary:'', taskType:'LEARNING', difficulty:'ENTRY', routeId:undefined as number|undefined, stageId:undefined as number|undefined, ownerMemberId:undefined as number|undefined, estimatedMinutes:60, points:50, learningObjectivesText:'', instructions:'', submissionRequirements:'', acceptanceCriteria:'', deadlineType:'NONE', fixedDeadline:'', durationHours:72, submissionLimit:3, capacity:undefined as number|undefined, prerequisiteTaskIds:[] as number[], resourcesText:'', faqText:'' })
const directionForm = reactive({ name:'', directionKey:'', sortOrder:90 })
const announcementForm = reactive({ title:'', content:'', expiresAt:'' })
const assignmentTask = ref<Record<string, any> | null>(null)
const assignmentMemberId = ref<number>()
const editingMemberId = ref<number>()
const skillOptions = ['HTML / CSS','JavaScript','TypeScript','Vue','React','Node.js','Java','Spring Boot','Python','FastAPI','MySQL','Redis','Git','Linux','Docker','GitHub Actions','REST API','OpenHarmony','ArkTS','人工智能','UI/UX','Figma','技术写作']
const memberForm = reactive({ nickname:'', avatarUrl:'', email:'', school:'', college:'', major:'', grade:'', skills:[] as string[], codeProfileUrl:'', weeklyHours:0, bio:'', directionIds:[] as number[], totalPoints:0 })
const activeMembers = computed(() => members.value.filter(item => item.status === 'ACTIVE'))
const taskOwnerOptions = computed(() => activeMembers.value.filter(item => roleList(item.roles).some(role => ['MENTOR','PROJECT_OWNER','ADMIN'].includes(role))))
function roleList(value?: string) { return value ? value.split(',') : [] }
function lines(value:string) { return value.split('\n').map(v=>v.trim()).filter(Boolean) }
function formatDate(value:string) { return new Date(value).toLocaleString('zh-CN',{hour12:false}) }
async function load() { loading.value=true; error.value=''; try { const [s,r,t,m,a,d,g,l,n,p]=await Promise.all([getAdminStats(),getAdminRoutes(),getAdminTasks(),getAdminMembers(),getAuditLogs(),getAdminDirections(),getStages(),getLevelRules(),getAnnouncements(),getAdminMemberProgress()]); stats.value=s; routes.value=r; tasks.value=t; members.value=m; audits.value=a; directions.value=d.map(item=>({...item,active:item.status==='ACTIVE'})); stages.value=g; levelRules.value=l.map(item=>({...item,active:item.status==='ACTIVE'}));announcements.value=n;progress.value=p } catch(reason){ error.value=getErrorMessage(reason) } finally{loading.value=false} }

async function loadAssignments() {
  const requestId = ++assignmentRequestId
  const filters = { ...assignmentFilters }
  assignmentsLoading.value = true
  assignmentsError.value = ''
  try {
    const result = await getAdminAssignments(filters)
    if (requestId !== assignmentRequestId) return
    assignments.value = result.items
    assignmentTotal.value = result.total
  } catch (reason) {
    if (requestId === assignmentRequestId) assignmentsError.value = getErrorMessage(reason, '无法加载任务记录')
  } finally {
    if (requestId === assignmentRequestId) assignmentsLoading.value = false
  }
}

function resetAssignmentPage() { assignmentFilters.page = 1; void loadAssignments() }
function clearAssignmentFilters() { assignmentFilters.memberId = undefined; assignmentFilters.taskId = undefined; assignmentFilters.status = 'ALL'; resetAssignmentPage() }
function refreshAll() { void Promise.all([load(), loadAssignments()]) }
function changeAssignmentPage(offset: number) { assignmentFilters.page += offset; void loadAssignments() }
function openMemberAssignments(item: MemberTaskProgress) { assignmentFilters.memberId = item.memberId; assignmentFilters.taskId = undefined; assignmentFilters.status = 'ALL'; section.value = '成员进度'; resetAssignmentPage() }
function showOverdue() { assignmentFilters.memberId = undefined; assignmentFilters.taskId = undefined; assignmentFilters.status = 'OVERDUE'; section.value = '成员进度'; resetAssignmentPage() }
function localDateTime(value: Date) { const pad = (n: number) => String(n).padStart(2, '0'); return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}` }
function openRestart(item: AdminAssignment) { restartTarget.value = item; restartForm.dueAt = localDateTime(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)); restartForm.reason = ''; restartDialog.value = true }
async function confirmRestart() {
  if (!restartTarget.value) return
  if (!restartForm.dueAt || new Date(restartForm.dueAt).getTime() <= Date.now()) return ElMessage.warning('请选择晚于当前时间的截止时间')
  if (!restartForm.reason.trim()) return ElMessage.warning('请填写重启原因')
  restarting.value = true
  try {
    await restartAdminAssignment(restartTarget.value.id, restartForm.dueAt, restartForm.reason.trim())
    restartDialog.value = false
    ElMessage.success('任务已重启，成员将收到通知')
    await Promise.all([load(), loadAssignments()])
  } catch (reason) {
    ElMessage.error(getErrorMessage(reason, '重启失败'))
  } finally {
    restarting.value = false
  }
}
async function saveRoute(){ if(!routeForm.name||!routeForm.routeKey||!routeForm.directionId) return ElMessage.warning('请填写路线必填项'); saving.value=true; try{await createRoute(routeForm); routeDialog.value=false; ElMessage.success('路线草稿已创建'); await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false} }
async function publish(item:Record<string,any>){await publishRoute(Number(item.id));ElMessage.success('路线已发布');await load()}
async function archiveRouteItem(item:Record<string,any>){await ElMessageBox.confirm(`归档后不可恢复，历史记录会保留。确认归档「${item.name}」？`,'归档路线',{type:'warning'});await archiveRoute(Number(item.id));ElMessage.success('路线已归档');await load()}
async function saveTask(){ if(!taskForm.title||!taskForm.taskKey||!taskForm.summary||!taskForm.learningObjectivesText||!taskForm.instructions||!taskForm.submissionRequirements||!taskForm.acceptanceCriteria)return ElMessage.warning('请填写任务必填项');if(taskForm.deadlineType==='FIXED'&&!taskForm.fixedDeadline)return ElMessage.warning('请填写固定截止时间'); saving.value=true; try{const selectedRoute=routes.value.find(r=>r.id===taskForm.routeId);await createTask({...taskForm,directionId:selectedRoute?.directionId||null,learningObjectives:lines(taskForm.learningObjectivesText),fixedDeadline:taskForm.deadlineType==='FIXED'?taskForm.fixedDeadline:null,durationHours:taskForm.deadlineType==='AFTER_CLAIM'?taskForm.durationHours:null,resources:lines(taskForm.resourcesText),ownerMemberId:taskForm.ownerMemberId||null,faq:lines(taskForm.faqText),capacity:taskForm.capacity||null,requiredInStage:true});taskDialog.value=false;ElMessage.success('任务草稿已创建');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false} }
function openAssignment(item:Record<string,any>){assignmentTask.value=item;assignmentMemberId.value=undefined;assignmentDialog.value=true}
async function saveAssignment(){if(!assignmentTask.value||!assignmentMemberId.value)return ElMessage.warning('请选择要分配的成员');saving.value=true;try{await assignTask(Number(assignmentTask.value.id),assignmentMemberId.value);assignmentDialog.value=false;ElMessage.success('任务已分配，成员将收到站内通知');await load()}catch(reason){ElMessage.error(getErrorMessage(reason,'任务分配失败'))}finally{saving.value=false}}
async function setTaskStatus(item:Record<string,any>,status:string){await changeTaskStatus(Number(item.id),status);ElMessage.success('任务状态已更新');await load()}
async function archiveTask(item:Record<string,any>){await ElMessageBox.confirm(`归档后不可恢复，确认归档「${item.title}」？`,'归档任务',{type:'warning'});await setTaskStatus(item,'ARCHIVED')}
async function saveRoles(item:Record<string,any>,roles:string[]){try{await updateMemberRoles(Number(item.id),roles);item.roles=roles.join(',');ElMessage.success('角色已更新')}catch(reason){ElMessage.error(getErrorMessage(reason));await load()}}
function openMemberEditor(item:Record<string,any>){editingMemberId.value=Number(item.id);Object.assign(memberForm,{nickname:item.nickname||'',avatarUrl:item.avatarUrl||'',email:item.email||'',school:item.school||'',college:item.college||'',major:item.major||'',grade:item.grade||'',skills:[...(item.skills||[])],codeProfileUrl:item.codeProfileUrl||'',weeklyHours:item.weeklyHours||0,bio:item.bio||'',directionIds:[...(item.directionIds||[])],totalPoints:item.totalPoints||0});memberDialog.value=true}
async function saveMemberProfile(){if(!editingMemberId.value||!memberForm.nickname.trim())return ElMessage.warning('请填写成员昵称');saving.value=true;try{await updateMemberProfile(editingMemberId.value,{...memberForm});memberDialog.value=false;ElMessage.success('成员资料与等级已更新');await load()}catch(reason){ElMessage.error(getErrorMessage(reason,'成员资料保存失败'))}finally{saving.value=false}}
async function changeMemberStatus(item:Record<string,any>,active:boolean){const next=active?'ACTIVE':'DISABLED';try{const {value}=await ElMessageBox.prompt(`请输入${active?'启用':'禁用'}原因`,'成员状态变更',{inputPattern:/\S+/,inputErrorMessage:'必须填写原因'});await updateMemberStatus(Number(item.id),next,value);item.status=next;ElMessage.success('成员状态已更新')}catch(reason){if(reason!=='cancel'&&reason!=='close')ElMessage.error(getErrorMessage(reason));await load()}}
async function saveLeaderboardVisibility(item:Record<string,any>, visible:boolean){leaderboardUpdatingId.value=Number(item.id);try{await updateLeaderboardVisibility(Number(item.id),visible);item.leaderboardVisible=visible;ElMessage.success(visible?'成员已显示在排行榜':'成员已从排行榜隐藏')}catch(reason){ElMessage.error(getErrorMessage(reason,'排行榜设置失败'))}finally{leaderboardUpdatingId.value=null}}
async function saveNewDirection(){if(!directionForm.name||!directionForm.directionKey)return ElMessage.warning('请填写方向名称和标识');saving.value=true;try{await createDirection(directionForm);directionDialog.value=false;Object.assign(directionForm,{name:'',directionKey:'',sortOrder:90});ElMessage.success('技术方向已创建');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false}}
async function saveDirection(item:Record<string,any>){try{await updateDirection(Number(item.id),{name:item.name,sortOrder:item.sortOrder,status:item.active?'ACTIVE':'ARCHIVED'});ElMessage.success('技术方向已更新');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}}
async function saveLevelRule(item:Record<string,any>){try{await updateLevelRule(String(item.levelKey),{name:item.name,minimumPoints:item.minimumPoints,status:item.active?'ACTIVE':'ARCHIVED'});ElMessage.success('等级规则已更新');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}}
async function saveAnnouncement(){if(!announcementForm.title||!announcementForm.content)return ElMessage.warning('请填写公告标题和内容');saving.value=true;try{await createAnnouncement({...announcementForm,expiresAt:announcementForm.expiresAt||null});announcementDialog.value=false;Object.assign(announcementForm,{title:'',content:'',expiresAt:''});ElMessage.success('公告草稿已创建');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false}}
async function publishNotice(item:Record<string,any>){await ElMessageBox.confirm(`发布后将通知所有启用成员，确认发布「${item.title}」？`,'发布公告',{type:'warning'});try{await publishAnnouncement(Number(item.id));ElMessage.success('公告已发布');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}}
watch(section,()=>{ if(section.value==='操作日志')void load() })
onMounted(() => {
  const storedTheme = localStorage.getItem('quest-theme')
  const dark = storedTheme ? storedTheme === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  void load()
  void loadAssignments()
})
</script>

<style scoped>
.admin-dashboard {
  --el-color-primary: var(--color-primary);
  --el-color-primary-dark-2: var(--color-primary-hover);
  --el-color-primary-light-3: color-mix(in srgb, var(--color-primary) 70%, var(--color-bg-container));
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  background: var(--color-bg-page);
}

.admin-dashboard :deep(.el-button--primary:not(.is-link)) {
  --el-button-text-color: var(--color-primary-foreground);
  --el-button-hover-text-color: var(--color-primary-foreground);
  --el-button-active-text-color: var(--color-primary-foreground);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  height: 100dvh;
  padding: 30px 16px 22px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-container);
}

.admin-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px;
}

.admin-sidebar__brand img {
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.admin-sidebar__brand span {
  display: grid;
  gap: 3px;
}

.admin-sidebar__brand strong {
  font-size: 11px;
  letter-spacing: .05em;
}

.admin-sidebar__brand small,
.admin-sidebar__label {
  color: var(--color-text-tertiary);
  font-size: 11px;
}

.admin-sidebar__label {
  margin: 48px 12px 14px;
  letter-spacing: .1em;
}

.admin-sidebar__nav {
  display: grid;
  gap: 4px;
}

.admin-sidebar__nav button {
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: transparent;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.admin-sidebar__nav button:hover,
.admin-sidebar__nav button.active {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.admin-sidebar__nav button.active {
  font-weight: 600;
}

.admin-sidebar__nav button:focus-visible,
.admin-risk-row:focus-visible,
.admin-card-title button:focus-visible,
.admin-metric--alert button:focus-visible {
  outline: 2px solid var(--color-text-primary);
  outline-offset: 2px;
}

.admin-sidebar__back {
  margin-top: auto;
  padding: 14px 12px 0;
  border-top: 1px solid var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 12px;
}

.admin-workspace {
  width: min(100%, 1500px);
  min-width: 0;
  margin: 0 auto;
  padding: 42px clamp(24px, 4vw, 64px) 80px;
}

.admin-section {
  min-width: 0;
  grid-template-columns: minmax(0, 1fr);
}

.admin-section > *,
.data-table-wrap {
  min-width: 0;
  max-width: 100%;
}

.admin-page-heading {
  margin-bottom: 32px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.admin-page-heading h1 {
  margin: 0;
  font-size: clamp(32px, 3vw, 44px);
  letter-spacing: -.04em;
}

.admin-page-heading p:last-child {
  margin: 10px 0 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.admin-dashboard .metric-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.admin-metric--alert {
  border-color: color-mix(in srgb, var(--color-warning) 24%, var(--color-border)) !important;
}

.admin-metric--alert strong,
.admin-metric--alert button {
  color: var(--color-warning);
}

.admin-metric--alert button,
.admin-card-title button {
  padding: 0;
  border: 0;
  background: none;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.admin-overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, .6fr);
  gap: 16px;
}

.admin-table-card h2 {
  margin: 0 0 18px;
  font-size: 18px;
}

.admin-card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.admin-card-title button {
  margin-bottom: 18px;
  color: var(--color-text-regular);
}

.admin-risk-row {
  width: 100%;
  padding: 13px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  border: 0;
  border-top: 1px solid var(--color-border-light);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.admin-risk-row > span:first-child {
  display: grid;
  gap: 4px;
}

.admin-risk-row strong {
  font-size: 14px;
}

.admin-risk-row small {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.admin-risk-row > span:last-child {
  color: var(--color-warning);
  font-size: 12px;
  white-space: nowrap;
}

.admin-facts > div {
  padding: 14px 0;
  display: flex;
  justify-content: space-between;
  border-top: 1px solid var(--color-border-light);
  font-size: 13px;
}

.admin-facts span {
  color: var(--color-text-secondary);
}

.admin-empty {
  padding: 28px;
  color: var(--color-text-tertiary);
  text-align: center;
}

.admin-search {
  width: min(300px, 100%);
}

.admin-progress-cell {
  min-width: 130px;
  display: grid;
  gap: 9px;
}

.admin-progress-cards {
  display: none;
}

.admin-text-warning {
  color: var(--color-warning);
}

.admin-progress-track {
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-bg-hover);
}

.admin-progress-track span {
  height: 100%;
  display: block;
  border-radius: inherit;
  background: var(--color-primary);
}

.admin-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.admin-filters :deep(.el-select) {
  width: min(240px, 100%);
}

.admin-filters__count {
  margin-left: auto;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.admin-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.admin-pagination span {
  margin-right: 8px;
}

.admin-pagination :deep(.el-button + .el-button) {
  margin-left: 0;
}

.admin-dialog-hint {
  margin: 0 0 20px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .admin-dashboard .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .admin-dashboard {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
    height: auto;
    padding: 16px;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .admin-sidebar__label {
    display: none;
  }

  .admin-sidebar__nav {
    margin-top: 16px;
    display: flex;
    overflow-x: auto;
  }

  .admin-sidebar__nav button {
    width: auto;
    flex: 0 0 auto;
    white-space: nowrap;
  }

  .admin-sidebar__back {
    margin-top: 12px;
    padding-top: 10px;
  }

  .admin-overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .admin-workspace {
    padding: 28px 16px 56px;
  }

  .admin-page-heading {
    align-items: flex-end;
  }

  .admin-dashboard .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-filters :deep(.el-select) {
    width: 100%;
  }

  .admin-progress-table {
    display: none;
  }

  .admin-progress-cards {
    display: grid;
    gap: 12px;
  }

  .admin-progress-cards .content-card {
    min-height: 0;
    padding: 18px;
  }

  .admin-progress-card__heading,
  .admin-progress-card__metrics {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .admin-progress-card__heading > span:first-child {
    min-width: 0;
    display: grid;
    gap: 4px;
  }

  .admin-progress-card__heading small {
    overflow: hidden;
    color: var(--color-text-tertiary);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .admin-progress-card__bar {
    margin-top: 20px;
    display: grid;
    gap: 8px;
    font-size: 13px;
  }

  .admin-progress-card__metrics {
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid var(--color-border-light);
    color: var(--color-text-secondary);
    font-size: 11px;
  }
}
</style>
