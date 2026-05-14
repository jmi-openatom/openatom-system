<template>
  <router-view v-slot="{ Component, route }">
    <transition mode="out-in" name="oa-page">
      <component :is="Component" :key="route.fullPath" />
    </transition>
  </router-view>
</template>
