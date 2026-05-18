<template>
  <div class="site-info-strip">
    <article v-for="item in items" :key="item.label">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </article>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string
    value: string | number
  }>
}>()
</script>

<style scoped>
.site-info-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  border-top: 1px solid var(--oa-border);
}

.site-info-strip article {
  display: grid;
  gap: 10px;
  min-height: 112px;
  align-content: center;
  padding: 20px 24px 0;
  border-left: 1px solid var(--oa-border);
}

.site-info-strip article:first-child {
  border-left: 0;
}

.site-info-strip span {
  color: var(--oa-muted);
  font-size: 14px;
}

.site-info-strip strong {
  color: var(--oa-text);
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: clamp(18px, 2vw, 24px);
  font-weight: 500;
  line-height: 1.45;
}

@media (max-width: 720px) {
  .site-info-strip {
    grid-template-columns: 1fr;
    border-top: 0;
  }

  .site-info-strip article {
    min-height: 0;
    padding: 18px 0;
    border-top: 1px solid var(--oa-border);
    border-left: 0;
  }
}
</style>
