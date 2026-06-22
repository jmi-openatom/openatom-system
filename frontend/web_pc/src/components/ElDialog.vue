<template>
  <ElementDialog
    v-bind="$attrs"
    append-to-body
    class="oa-global-dialog"
    modal-class="oa-global-overlay"
  >
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps || {}" />
    </template>
  </ElementDialog>
</template>

<script setup lang="ts">
import "element-plus/es/components/dialog/style/css";
import { ElDialog as ElementDialog } from "element-plus/es/components/dialog/index";

defineOptions({
  name: "ElDialog",
  inheritAttrs: false,
});
</script>
