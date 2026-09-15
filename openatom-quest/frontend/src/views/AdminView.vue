<template>
  <div class="page-container admin-page">
    <header class="page-heading"><p class="eyebrow">PLATFORM OPERATIONS</p><h1>管理后台</h1><p>管理路线、任务、成员权限，并查看所有关键操作的审计记录。</p></header>
    <div class="filter-row"><el-segmented v-model="section" :options="sections" /></div>
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="error" icon="error" title="管理数据加载失败" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>

    <template v-else>
      <section v-if="section === '数据概览'" class="admin-section">
        <div class="metric-grid"><article><span>OAuth 登录成功率</span><strong>{{ stats.oauth?.successRate || 0 }}%</strong><small>近 30 天成功 {{ stats.oauth?.successes || 0 }} / 失败 {{ stats.oauth?.failures || 0 }}</small></article><article><span>成员总数</span><strong>{{ stats.members?.total || 0 }}</strong><small>近 30 天新增 {{ stats.members?.newIn30Days || 0 }}</small></article><article><span>任务领取</span><strong>{{ stats.tasks?.assignments || 0 }}</strong><small>完成率 {{ stats.tasks?.completionRate || 0 }}%</small></article><article><span>待审核</span><strong>{{ stats.reviews?.pending || 0 }}</strong><small>平均 {{ stats.reviews?.averageHours || 0 }} 小时</small></article><article><span>已完成</span><strong>{{ stats.tasks?.passed || 0 }}</strong><small>逾期 {{ stats.tasks?.overdue || 0 }}</small></article></div>
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

      <section v-else-if="section === '成员管理'" class="admin-section">
        <div class="admin-toolbar"><div><h2>成员与角色</h2><p>角色权限由 Quest 独立管理，不继承 OAuth 权限。</p></div></div>
        <div class="data-table-wrap"><table class="data-table"><thead><tr><th>成员</th><th>方向</th><th>等级 / 积分</th><th>角色</th><th>状态</th></tr></thead><tbody><tr v-for="item in members" :key="item.id"><td><strong>{{ item.nickname || '未填写昵称' }}</strong><small>{{ item.email || `成员 #${item.id}` }}</small></td><td>{{ item.directions || '-' }}</td><td>{{ item.currentLevel }} / {{ item.totalPoints }}</td><td><el-select :model-value="roleList(item.roles)" multiple collapse-tags aria-label="成员角色" @change="(value: string[]) => saveRoles(item, value)"><el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" /></el-select></td><td><el-switch :model-value="item.status === 'ACTIVE'" active-text="启用" inactive-text="禁用" @change="(value: boolean) => changeMemberStatus(item, value)" /></td></tr></tbody></table></div>
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

    <el-dialog v-model="directionDialog" title="新建技术方向" width="min(520px, calc(100vw - 32px))">
      <el-form label-position="top"><el-form-item label="方向名称" required><el-input v-model="directionForm.name" /></el-form-item><el-form-item label="方向标识" required><el-input v-model="directionForm.directionKey" placeholder="cloud-native" /></el-form-item><el-form-item label="排序"><el-input-number v-model="directionForm.sortOrder" :min="0" style="width:100%" /></el-form-item></el-form>
      <template #footer><el-button @click="directionDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveNewDirection">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="announcementDialog" title="新建系统公告" width="min(620px, calc(100vw - 32px))">
      <el-form label-position="top"><el-form-item label="公告标题" required><el-input v-model="announcementForm.title" /></el-form-item><el-form-item label="公告内容" required><el-input v-model="announcementForm.content" type="textarea" :rows="6" /></el-form-item><el-form-item label="失效时间（可选）"><el-input v-model="announcementForm.expiresAt" type="datetime-local" /></el-form-item></el-form>
      <template #footer><el-button @click="announcementDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAnnouncement">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import 'element-plus/es/components/dialog/style/css'
import { ElCheckboxGroup, ElDialog, ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { getErrorMessage } from '@/api/http'
import { archiveRoute, assignTask, changeTaskStatus, createAnnouncement, createDirection, createRoute, createTask, getAdminDirections, getAdminMembers, getAdminRoutes, getAdminStats, getAdminTasks, getAnnouncements, getAuditLogs, getLevelRules, getStages, publishAnnouncement, publishRoute, updateDirection, updateLevelRule, updateMemberRoles, updateMemberStatus } from '@/api/quest'
const sections = ['数据概览', '成长路线', '任务管理', '成员管理', '平台配置', '操作日志']
const section = ref('数据概览')
const loading = ref(true), saving = ref(false), error = ref(''), routeDialog = ref(false), taskDialog = ref(false), directionDialog = ref(false), announcementDialog = ref(false), assignmentDialog = ref(false)
const stats = ref<Record<string, any>>({}), routes = ref<Record<string, any>[]>([]), tasks = ref<Record<string, any>[]>([]), members = ref<Record<string, any>[]>([]), audits = ref<Record<string, any>[]>([]), directions = ref<Record<string, any>[]>([]), stages = ref<Record<string, any>[]>([]), levelRules = ref<Record<string, any>[]>([]), announcements = ref<Record<string, any>[]>([])
const roleOptions = [{label:'新成员',value:'MEMBER'},{label:'导师',value:'MENTOR'},{label:'项目负责人',value:'PROJECT_OWNER'},{label:'管理员',value:'ADMIN'}]
const statusText: Record<string,string> = { DRAFT:'草稿', PUBLISHED:'已发布', OFFLINE:'已下架', ARCHIVED:'已归档' }
const routeForm = reactive({ name:'', routeKey:'', description:'', directionId:undefined as number|undefined, stageKeys:['L0','L1','L2','L3','L4'] })
const taskForm = reactive({ title:'', taskKey:'', summary:'', taskType:'LEARNING', difficulty:'ENTRY', routeId:undefined as number|undefined, stageId:undefined as number|undefined, ownerMemberId:undefined as number|undefined, estimatedMinutes:60, points:50, learningObjectivesText:'', instructions:'', submissionRequirements:'', acceptanceCriteria:'', deadlineType:'NONE', fixedDeadline:'', durationHours:72, submissionLimit:3, capacity:undefined as number|undefined, prerequisiteTaskIds:[] as number[], resourcesText:'', faqText:'' })
const directionForm = reactive({ name:'', directionKey:'', sortOrder:90 })
const announcementForm = reactive({ title:'', content:'', expiresAt:'' })
const assignmentTask = ref<Record<string, any> | null>(null)
const assignmentMemberId = ref<number>()
const activeMembers = computed(() => members.value.filter(item => item.status === 'ACTIVE'))
const taskOwnerOptions = computed(() => activeMembers.value.filter(item => roleList(item.roles).some(role => ['MENTOR','PROJECT_OWNER','ADMIN'].includes(role))))
function roleList(value?: string) { return value ? value.split(',') : [] }
function lines(value:string) { return value.split('\n').map(v=>v.trim()).filter(Boolean) }
function formatDate(value:string) { return new Date(value).toLocaleString('zh-CN',{hour12:false}) }
async function load() { loading.value=true; error.value=''; try { const [s,r,t,m,a,d,g,l,n]=await Promise.all([getAdminStats(),getAdminRoutes(),getAdminTasks(),getAdminMembers(),getAuditLogs(),getAdminDirections(),getStages(),getLevelRules(),getAnnouncements()]); stats.value=s; routes.value=r; tasks.value=t; members.value=m; audits.value=a; directions.value=d.map(item=>({...item,active:item.status==='ACTIVE'})); stages.value=g; levelRules.value=l.map(item=>({...item,active:item.status==='ACTIVE'}));announcements.value=n } catch(reason){ error.value=getErrorMessage(reason) } finally{loading.value=false} }
async function saveRoute(){ if(!routeForm.name||!routeForm.routeKey||!routeForm.directionId) return ElMessage.warning('请填写路线必填项'); saving.value=true; try{await createRoute(routeForm); routeDialog.value=false; ElMessage.success('路线草稿已创建'); await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false} }
async function publish(item:Record<string,any>){await publishRoute(Number(item.id));ElMessage.success('路线已发布');await load()}
async function archiveRouteItem(item:Record<string,any>){await ElMessageBox.confirm(`归档后不可恢复，历史记录会保留。确认归档「${item.name}」？`,'归档路线',{type:'warning'});await archiveRoute(Number(item.id));ElMessage.success('路线已归档');await load()}
async function saveTask(){ if(!taskForm.title||!taskForm.taskKey||!taskForm.summary||!taskForm.learningObjectivesText||!taskForm.instructions||!taskForm.submissionRequirements||!taskForm.acceptanceCriteria)return ElMessage.warning('请填写任务必填项');if(taskForm.deadlineType==='FIXED'&&!taskForm.fixedDeadline)return ElMessage.warning('请填写固定截止时间'); saving.value=true; try{const selectedRoute=routes.value.find(r=>r.id===taskForm.routeId);await createTask({...taskForm,directionId:selectedRoute?.directionId||null,learningObjectives:lines(taskForm.learningObjectivesText),fixedDeadline:taskForm.deadlineType==='FIXED'?taskForm.fixedDeadline:null,durationHours:taskForm.deadlineType==='AFTER_CLAIM'?taskForm.durationHours:null,resources:lines(taskForm.resourcesText),ownerMemberId:taskForm.ownerMemberId||null,faq:lines(taskForm.faqText),capacity:taskForm.capacity||null,requiredInStage:true});taskDialog.value=false;ElMessage.success('任务草稿已创建');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false} }
function openAssignment(item:Record<string,any>){assignmentTask.value=item;assignmentMemberId.value=undefined;assignmentDialog.value=true}
async function saveAssignment(){if(!assignmentTask.value||!assignmentMemberId.value)return ElMessage.warning('请选择要分配的成员');saving.value=true;try{await assignTask(Number(assignmentTask.value.id),assignmentMemberId.value);assignmentDialog.value=false;ElMessage.success('任务已分配，成员将收到站内通知');await load()}catch(reason){ElMessage.error(getErrorMessage(reason,'任务分配失败'))}finally{saving.value=false}}
async function setTaskStatus(item:Record<string,any>,status:string){await changeTaskStatus(Number(item.id),status);ElMessage.success('任务状态已更新');await load()}
async function archiveTask(item:Record<string,any>){await ElMessageBox.confirm(`归档后不可恢复，确认归档「${item.title}」？`,'归档任务',{type:'warning'});await setTaskStatus(item,'ARCHIVED')}
async function saveRoles(item:Record<string,any>,roles:string[]){try{await updateMemberRoles(Number(item.id),roles);item.roles=roles.join(',');ElMessage.success('角色已更新')}catch(reason){ElMessage.error(getErrorMessage(reason));await load()}}
async function changeMemberStatus(item:Record<string,any>,active:boolean){const next=active?'ACTIVE':'DISABLED';try{const {value}=await ElMessageBox.prompt(`请输入${active?'启用':'禁用'}原因`,'成员状态变更',{inputPattern:/\S+/,inputErrorMessage:'必须填写原因'});await updateMemberStatus(Number(item.id),next,value);item.status=next;ElMessage.success('成员状态已更新')}catch(reason){if(reason!=='cancel'&&reason!=='close')ElMessage.error(getErrorMessage(reason));await load()}}
async function saveNewDirection(){if(!directionForm.name||!directionForm.directionKey)return ElMessage.warning('请填写方向名称和标识');saving.value=true;try{await createDirection(directionForm);directionDialog.value=false;Object.assign(directionForm,{name:'',directionKey:'',sortOrder:90});ElMessage.success('技术方向已创建');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false}}
async function saveDirection(item:Record<string,any>){try{await updateDirection(Number(item.id),{name:item.name,sortOrder:item.sortOrder,status:item.active?'ACTIVE':'ARCHIVED'});ElMessage.success('技术方向已更新');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}}
async function saveLevelRule(item:Record<string,any>){try{await updateLevelRule(String(item.levelKey),{name:item.name,minimumPoints:item.minimumPoints,status:item.active?'ACTIVE':'ARCHIVED'});ElMessage.success('等级规则已更新');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}}
async function saveAnnouncement(){if(!announcementForm.title||!announcementForm.content)return ElMessage.warning('请填写公告标题和内容');saving.value=true;try{await createAnnouncement({...announcementForm,expiresAt:announcementForm.expiresAt||null});announcementDialog.value=false;Object.assign(announcementForm,{title:'',content:'',expiresAt:''});ElMessage.success('公告草稿已创建');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}finally{saving.value=false}}
async function publishNotice(item:Record<string,any>){await ElMessageBox.confirm(`发布后将通知所有启用成员，确认发布「${item.title}」？`,'发布公告',{type:'warning'});try{await publishAnnouncement(Number(item.id));ElMessage.success('公告已发布');await load()}catch(reason){ElMessage.error(getErrorMessage(reason))}}
watch(section,()=>{ if(section.value==='操作日志')void load() })
onMounted(load)
</script>
