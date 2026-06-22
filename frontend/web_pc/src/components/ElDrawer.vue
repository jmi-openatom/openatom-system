<template>
  <ElementDrawer
    v-bind="$attrs"
    append-to-body
    class="oa-global-drawer"
    modal-class="oa-global-overlay"
  >
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps || {}" />
    </template>
  </ElementDrawer>
</template>

<script setup lang="ts">
import "element-plus/es/components/drawer/style/css";
import { ElDrawer as ElementDrawer } from "element-plus/es/components/drawer/index";

defineOptions({
  name: "ElDrawer",
  inheritAttrs: false,
});
</script>
