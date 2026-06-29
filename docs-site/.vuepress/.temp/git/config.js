import { GitContributors } from "/Users/ariven/1-jmiopenatom/openatom-system/docs-site/node_modules/.pnpm/@vuepress+plugin-git@2.0.0-rc.130_@vuepress+bundler-vite@2.0.0-rc.30_@types+node@26.0.1_cb7c06aab6d54d18080b07e797393ac3/node_modules/@vuepress/plugin-git/dist/client/components/GitContributors.js";
import { GitChangelog } from "/Users/ariven/1-jmiopenatom/openatom-system/docs-site/node_modules/.pnpm/@vuepress+plugin-git@2.0.0-rc.130_@vuepress+bundler-vite@2.0.0-rc.30_@types+node@26.0.1_cb7c06aab6d54d18080b07e797393ac3/node_modules/@vuepress/plugin-git/dist/client/components/GitChangelog.js";

export default {
  enhance: ({ app }) => {
    app.component("GitContributors", GitContributors);
    app.component("GitChangelog", GitChangelog);
  },
};
