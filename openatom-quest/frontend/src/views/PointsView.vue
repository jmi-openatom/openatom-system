<template>
  <div class="page-container growth-profile-page">
    <header class="page-heading"><p class="eyebrow">GROWTH RECORD</p><h1>积分与等级</h1><p>每一笔积分都对应真实学习成果或贡献，可追溯且不会重复发放。</p></header>
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="error" icon="error" title="成长记录加载失败" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <template v-else-if="data">
      <section class="growth-summary"><article class="welcome-card"><div><p class="eyebrow eyebrow--inverse">CURRENT LEVEL</p><h2>{{ data.level }}</h2><p>当前累计 {{ data.points }} 积分</p></div></article><article class="content-card level-track"><div v-for="rule in data.rules" :key="rule.levelKey" :class="{ reached: data.points >= rule.minimumPoints }"><strong>{{ rule.levelKey }} · {{ rule.name }}</strong><span>{{ rule.minimumPoints }} 分</span></div></article></section>
      <section class="content-card admin-table-card"><div class="section-heading"><div><p class="eyebrow">POINT LEDGER</p><h2>积分明细</h2></div></div><div v-if="data.ledger.length" class="data-table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>来源</th><th>变动</th><th>余额</th></tr></thead><tbody><tr v-for="item in data.ledger" :key="item.id"><td>{{ formatDate(item.createdAt) }}</td><td>{{ item.reason }}</td><td><strong>+{{ item.amount }}</strong></td><td>{{ item.balanceAfter }}</td></tr></tbody></table></div><div v-else class="empty-copy">完成首个任务后，积分记录会显示在这里。</div></section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getErrorMessage } from '@/api/http'
import { getMemberGrowth } from '@/api/quest'
const data = ref<Record<string, any> | null>(null), loading = ref(true), error = ref('')
function formatDate(value:string){return new Date(value).toLocaleString('zh-CN',{hour12:false})}
async function load(){loading.value=true;error.value='';try{data.value=await getMemberGrowth()}catch(reason){error.value=getErrorMessage(reason)}finally{loading.value=false}}
onMounted(load)
</script>
