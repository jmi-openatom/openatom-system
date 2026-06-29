import { CodeTabs } from "/Users/ariven/1-jmiopenatom/openatom-system/docs-site/node_modules/.pnpm/@vuepress+plugin-markdown-tab@2.0.0-rc.130_@vuepress+bundler-vite@2.0.0-rc.30_@types+no_ee1bfbc01c30087bfe891983770df462/node_modules/@vuepress/plugin-markdown-tab/dist/client/components/CodeTabs.js";
import { Tabs } from "/Users/ariven/1-jmiopenatom/openatom-system/docs-site/node_modules/.pnpm/@vuepress+plugin-markdown-tab@2.0.0-rc.130_@vuepress+bundler-vite@2.0.0-rc.30_@types+no_ee1bfbc01c30087bfe891983770df462/node_modules/@vuepress/plugin-markdown-tab/dist/client/components/Tabs.js";

export default {
  enhance: ({ app }) => {
    app.component("CodeTabs", CodeTabs);
    app.component("Tabs", Tabs);
  },
};
