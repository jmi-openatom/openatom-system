import { stepGroups, allSteps } from './walkthroughs.js';

export const links = {
  main: 'https://www.jmi-openatom.cn',
  activation: 'https://www.jmi-openatom.cn/activation',
  login: 'https://www.jmi-openatom.cn/login',
  mail: 'https://mail.jmi-openatom.cn',
  quest: 'https://quest.jmi-openatom.cn',
  activities: 'https://www.jmi-openatom.cn/activities',
};

const external = (url, text) => `<a class="inline-link" href="${url}" target="_blank" rel="noopener noreferrer">${text} <span aria-hidden="true">↗</span><span class="sr-only">（在新标签页打开）</span></a>`;
const note = (title, body) => `<aside class="callout"><span class="callout-mark" aria-hidden="true">i</span><div><strong>${title}</strong><p>${body}</p></div></aside>`;
const tutorial = (article, section) => {
  const items = stepGroups[article]?.[section];
  if (!items?.length) throw new Error(`Missing walkthrough: ${article}/${section}`);
  const total = allSteps.filter(s => s.article === article).length;
  return `<ol class="illustrated-steps" aria-label="逐步图文操作">${items.map(s => `<li id="${s.id}" class="illustrated-step" data-step-id="${s.id}"><header class="step-heading"><span class="step-number">STEP ${String(s.number).padStart(2, '0')} <small>/ ${String(total).padStart(2, '0')}</small></span><h3>${s.title}<a class="heading-anchor" href="#/${article}/${s.id}" aria-label="链接到步骤 ${s.number}：${s.title}">#</a></h3></header><p class="step-instruction">${s.body}</p><figure class="guide-figure step-figure"><button class="figure-open" type="button" data-figure="${s.id}" aria-label="放大步骤 ${s.number} 图片：${s.title}"><img src="./assets/steps/${s.id}.svg" alt="步骤 ${s.number}：${s.title}。蓝框标出本步操作位置。完成后：${s.result}" width="1200" height="800" loading="lazy" /><span class="figure-zoom" aria-hidden="true">↗ 放大 / 原尺寸查看</span></button><figcaption><span>本步界面演示</span> 使用演示数据渲染，蓝框标出本步操作位置；实际界面以系统为准。</figcaption></figure><p class="step-result"><span aria-hidden="true">✓</span><strong>完成后</strong>${s.result}</p></li>`).join('')}</ol>`;
};

const table = (head, rows) => `<div class="table-scroll" role="region" aria-label="${head.join('与')}对照表" tabindex="0"><table><thead><tr>${head.map(h => `<th scope="col">${h}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map((cell, i) => i === 0 ? `<th scope="row">${cell}</th>` : `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
const section = (id, title, html) => ({ id, title, html });
const faq = (question, answer) => `<details class="faq-item"><summary>${question}<span aria-hidden="true">+</span></summary><div>${answer}</div></details>`;

export const articles = [
  {
    id: 'activation', group: '从这里开始', number: '01', icon: 'key', title: '激活社团账号', short: '账号激活', time: '逐步跟随操作',
    description: '确认你的成员身份，连接社群，开启在社团的第一步。',
    keywords: '初始密码 登录 学号 QQ 群 验证码 个人资料 安全 姓名 激活失败',
    sections: [
      section('before', '开始前，准备好这些', `<p>准备你的社团账号或学号、当前密码，以及可以使用的 QQ。账号信息以社团通知或管理员提供的内容为准；已经完成激活的成员可以直接阅读<a href="#/mail">邮箱指南</a>。</p>${note('还没有账号或不知道密码？', '先联系社团管理员确认账号信息。主站仅在开放注册时显示“新账号注册”，注册入口是否出现以页面为准。不要反复猜测初始密码。')}<div class="entry-link">${external(links.login, '打开主站登录页')}<span>使用用户名或学号登录</span></div>`),
      section('welcome', '登录并认识社团', `${tutorial('activation', 'welcome')}`),
      section('join-group', '完成 QQ 入群验证', `${tutorial('activation', 'join-group')}${note('已经在群里，仍不能继续？', '稍等片刻等待状态刷新。若持续未同步，请管理员核对社团账号与 QQ 的绑定。不要发送密码或完整验证码。')}`),
      section('profile-password', '补全资料，设置新密码', `${tutorial('activation', 'profile-password')}${note('密码更新失败怎么办？', '确认当前密码正确，再按提示处理。若账号状态与密码不一致，请管理员核对，避免重复提交。')}`),
      section('done', '怎样确认已经完成', `<ul class="check-list"><li>QQ 入群状态已确认，基础资料已保存。</li><li>页面出现激活成功提示。</li><li>可以使用新密码登录主站。</li></ul><p>接下来，开通社团邮箱，建立接收通知和对外沟通的渠道。</p>`),
    ],
  },
  {
    id: 'mail', group: '常用系统', number: '02', icon: 'mail', title: '使用社团邮箱', short: '社团邮箱', time: '逐步跟随操作',
    description: '从首次开通，到收发第一封邮件，把社团沟通安排妥当。',
    keywords: 'OAuth 授权 邮箱激活 自定义前缀 拼音 收件箱 发件 附件 20 MiB 10 垃圾邮件 通知',
    sections: [
      section('login', '用社团账号登录', `<p>邮箱通过社团账号授权登录；如果某个登录或授权页面没有出现，直接继续后面的操作。</p>${tutorial('mail', 'login')}`),
      section('address', '首次使用：确定邮箱地址', `<p>以下步骤只针对首次出现“激活你的邮箱地址”的情况；已有可用地址时直接阅读下一节。</p>${tutorial('mail', 'address')}`),
      section('read', '收信、查找与回复', `<p>从文件夹进入邮件列表，再打开正文。以下分别展示查看、搜索、回复与下载附件的入口。</p>${tutorial('mail', 'read')}`),
      section('send', '发送第一封邮件', `${tutorial('mail', 'send')}<div class="example-block"><span class="eyebrow">邮件示例 · 请按实际情况修改</span><p><strong>主题：</strong>【任务咨询】姓名 · 任务名称</p><p>你好，我正在完成「任务名称」。目前已完成……，在……环节遇到问题，已尝试……。相关链接：……。想请教……，谢谢！</p></div>`),
      section('mail-help', '常见问题与使用习惯', `${faq('授权后一直没有进入邮箱怎么办？', '<p>重新从邮箱首页发起登录。若提示账号停用或邮箱尚未就绪，记录错误提示和发生时间并联系管理员。不要另行创建账号来绕过问题。</p>')}${faq('为什么对方没有收到我发出的邮件？', '<p>核对完整收件地址，在“已发送”确认记录，查看是否收到退信，并请对方检查垃圾邮件。持续异常时，将主题、发送时间和退信提示提供给管理员。</p>')}${faq('如何确认自己的邮箱地址？', '<p>以邮箱激活成功页、邮箱顶部账号信息或主站个人资料中显示的完整地址为准。不要自行拼接姓名与域名。</p>')}<p>建议定期查看邮箱，同时关注主站与 Quest 的站内通知。不同系统的通知渠道以实际页面为准，邮箱不一定同步所有任务反馈。</p>`),
    ],
  },
  {
    id: 'quest', group: '常用系统', number: '03', icon: 'compass', title: '认识 Quest 成长系统', short: 'Quest 入门', time: '逐步跟随操作',
    description: '选择适合自己的方向，把每一步学习变成看得见的实践。',
    keywords: 'OAuth 首次登录 新人引导 七步 个人资料 学籍 LMS 成长路线 L0 L1 L2 L3 L4 积分 导师',
    sections: [
      section('what', 'Quest 是用来做什么的', `<p>Quest 是社团的成长路线与任务实践平台。你可以在这里选择学习方向、领取任务、提交成果、获得导师反馈，并查看积分与成长记录。</p>${table(['你想做的事', '去哪里找'], [['确定接下来学什么', '成长路线'], ['寻找可以参与的实践', '任务中心'], ['继续已领取或被分配的任务', '我的任务'], ['查看审核结果与修改建议', '任务详情、站内通知'], ['查看获得的积分与等级', '积分等级']])}`),
      section('first-login', '首次登录与七步新人引导', `<p>首次进入 Quest 时，按七步引导确认资料、选择方向并开启新人任务。已经完成引导的成员可直接继续任务。</p>${tutorial('quest', 'first-login')}`),
      section('route', '读懂一条成长路线', `${tutorial('quest', 'route')}${table(['阶段', '主要内容'], [['L0 · 新人报到', '认识社团、协作规则与参与方式'], ['L1 · 开源入门', '学习 Git、Issue 与 Pull Request'], ['L2 · 技术学习', '完成所选方向的基础任务'], ['L3 · 项目实战', '解决接近真实项目的问题'], ['L4 · 项目贡献', '进入真实开源项目，积累协作经验']])}<p>路线中的任务可能有前置要求。先完成前置任务，再领取后续任务；具体阶段任务、积分和要求以当前任务详情为准。</p>`),
      section('claim', '找到并领取你的任务', `${tutorial('quest', 'claim')}${note('建议先完成主站探索', '“主站探索：认识开放原子开源社团”是已有路线共用的新人任务。它使用个人标记自动核验，详见下一篇。')}`),
    ],
  },
  {
    id: 'exploration', group: '参与与实践', number: '04', icon: 'flag', title: '完成「认识社团」任务', short: '认识社团任务', time: '逐步跟随操作',
    description: '带着三条线索探索主站，完成你的第一项新人实践。',
    keywords: '主站探索 认识开放原子开源社团 flag OA 标记 关于我们 规章制度 社团活动 自动核验 至少8个字 20积分',
    sections: [
      section('prepare', '领取「主站探索」', `${note('请先保持 Quest 登录', '在同一个浏览器中打开 Quest 与三个主站页面。个人标记属于当前 Quest 账号，不要切换到其他账号或无痕窗口。')}${tutorial('exploration', 'prepare')}`),
      section('three-pages', '浏览三个页面，找到个人标记', `<p>三个页面需要分别“打开 → 阅读 → 复制 → 回到 Quest 填写”，每一枚标记都对应一个输入框。</p>${tutorial('exploration', 'three-pages')}`),
      section('submit-flags', '填写标记与参与计划', `${tutorial('exploration', 'submit-flags')}${note('本任务不需要上传附件', '只需三个个人标记与至少 8 个字的参与计划，不需要仓库、截图或视频。普通任务使用另一种成果审核流程。')}`),
      section('exploration-help', '标记找不到或校验不通过', `${faq('页面里看不到标记怎么办？', '<p>确认 Quest 仍处于登录状态，从任务中的链接重新打开对应主站页面并浏览页面内容。尽量使用同一个浏览器；若仍没有标记，记录页面地址和提示，联系管理员。</p>')}${faq('活动或制度列表为空，还能完成吗？', '<p>可以。这个任务的探索标记设置在对应页面中，列表暂时为空也可以寻找标记。</p>')}${faq('提示标记错误怎么办？', '<p>核对三个页面与三个输入框是否对应，检查是否漏掉 OA、花括号或字符，并确认复制的是当前 Quest 账号的标记。不要填写本指南的格式示例。</p>')}${faq('提交后一直显示待审核，是同一个任务吗？', '<p>主站探索使用专门的“提交并自动核验”表单。若你看到的是仓库地址、成果说明和“提交审核”，请先核对任务名称；普通实践任务使用另一种审核流程。</p>')}`),
    ],
  },
  {
    id: 'activities', group: '参与与实践', number: '05', icon: 'calendar', title: '发现并参与社团活动', short: '参与社团活动', time: '逐步跟随操作',
    description: '从一次技术分享或工作坊开始，找到与你一起实践的人。',
    keywords: '活动日程 讲座 工作坊 报名 现场 时间 地点 活动积分 Quest区别',
    sections: [
      section('discover', '在哪里查看活动', `${tutorial('activities', 'discover')}`),
      section('register', '看清要求，再决定参与', `${tutorial('activities', 'register')}${note('按钮不能点击时', '先阅读页面提示，可能需要登录，或当前不在允许报名的时间范围。以实际活动页面为准。')}`),
      section('activity-vs-task', '活动与 Quest 任务有什么关系', `${table(['', '社团活动', 'Quest 任务'], [['主要作用', '参与讲座、工作坊、项目冲刺等集体活动', '完成学习目标与可核验的实践成果'], ['主要入口', '主站 → 活动', 'Quest → 成长路线 / 任务中心'], ['参与方式', '按活动说明报名或现场参与', '领取或接受分配，按要求提交'], ['完成依据', '活动组织者公布的参与要求', '任务验收标准、自动核验或导师审核']])}<p>活动和任务可以相互关联，但<strong>报名活动不会自动代替 Quest 任务提交</strong>。活动参与积分与 Quest 任务积分也应分别以各自系统显示的规则和记录为准。</p>`),
      section('after', '参与之后，继续积累', `<p>记录活动中的收获和问题。如果活动对应某项 Quest 任务，回到 Quest 查看明确的成果要求，再提交自己的学习总结或作品链接。遇到安排变动，及时与活动组织者沟通。</p>`),
    ],
  },
  {
    id: 'submission', group: '参与与实践', number: '06', icon: 'upload', title: '提交成果与查看反馈', short: '成果提交与反馈', time: '逐步跟随操作',
    description: '清楚说明你做了什么，让每一次实践都有可核验的成果。',
    keywords: '草稿 仓库 GitHub Gitee PR Pull Request 演示 视频 AI 导师 审核 修改 版本 积分 申诉 超期 放弃',
    sections: [
      section('prepare-result', '提交前，先对照验收标准', `<p>本篇介绍<strong>普通实践任务</strong>。正在做主站探索时，请使用<a href="#/exploration">个人标记自动核验流程</a>。</p>${tutorial('submission', 'prepare-result')}`),
      section('fill-submit', '填写表单，提交审核', `${tutorial('submission', 'fill-submit')}`),
      section('status', '读懂任务状态', `${tutorial('submission', 'status')}${table(['状态', '含义与下一步'], [['进行中', '继续实践并整理成果，可填写并提交表单'], ['待审核', '已提交，等待导师审核，留意站内通知'], ['需修改', '阅读“导师最新反馈”，修正后提交新版本'], ['已通过', '任务已完成，查看积分与后续成长路线'], ['已逾期 / 已结束', '当前记录不可继续提交，联系负责人确认安排']])}<p>提交后到 Quest 的“站内通知”和任务记录查看结果。普通任务审核通过后获得对应积分；积分多少和任务状态以页面为准。</p>`),
      section('revision', '修改、版本与申诉', `${tutorial('submission', 'revision')}${note('谨慎使用“放弃任务”', '放弃后本次任务结束，历史提交仍保留。遇到困难时，建议先联系任务负责人确认可行安排。')}`),
    ],
  },
  {
    id: 'faq', group: '帮助', number: '07', icon: 'help', title: '常见问题与求助', short: '常见问题', time: '按需查阅',
    description: '卡在哪一步，就从这里找到下一步。',
    keywords: '忘记密码 登录失败 找不到账号 邮箱 错误 任务 管理员 联系 问题反馈',
    sections: [
      section('account-help', '账号与登录', `${faq('账号、学号、邮箱地址是一回事吗？', '<p>主站登录页支持用户名或学号；邮箱地址是收发邮件使用的地址。邮箱和 Quest 通过主站账号授权登录，不要把它们当作三套需要分别注册的账号。</p>')}${faq('忘记当前密码或无法登录怎么办？', '<p>联系社团管理员核对账号并处理密码问题。提供账号或学号、错误提示与发生时间即可；不要发送当前密码、新密码或授权凭证。</p>')}${faq('为什么登录后又进入激活页？', '<p>可能尚未完成账号激活，或账号状态需要核对。先按“激活社团账号”完成引导；若此前已经成功激活且能用新密码登录，请将现象告知管理员。</p>')}${faq('授权页取消了，怎么重新进入？', '<p>返回邮箱或 Quest 的首页，再次点击社团账号登录按钮，重新完成登录和授权。回调失效时也应重新发起登录。</p>')}`),
      section('mail-help', '邮箱使用', `${faq('账号激活成功，为什么邮箱还要激活？', '<p>社团账号激活用于确认成员身份与账号安全；邮箱地址激活用于选择收发信的完整地址。出现“激活你的邮箱地址”时，按“社团邮箱”指南完成即可。</p>')}${faq('邮箱地址可以随便改吗？', '<p>地址确定后一般不可自行修改。首次确认前仔细核对；需要更正时联系管理员，以实际可用的处理方式为准。</p>')}${faq('为什么附件无法上传或下载？', '<p>检查网络、附件数量与大小：最多 10 个，总大小不超过 20 MiB。附件处理服务异常也可能阻止操作，持续失败时保留错误提示并联系管理员。</p>')}`),
      section('quest-help', 'Quest 与活动任务', `${faq('没有技术基础，可以使用 Quest 吗？', '<p>可以。新人引导中如实选择当前基础，从 L0 和适合自己的方向开始，不需要虚构技能或项目经历。</p>')}${faq('任务无法领取，按钮提示先完成前置任务？', '<p>打开任务详情的“前置任务”，先完成所列任务并确认状态已通过，再返回领取。认识社团的主站探索任务可能是方向任务的前置条件。</p>')}${faq('提交了草稿，为什么还没有审核？', '<p>自动保存只保存在当前浏览器。必须点击“提交审核”，确认出现提交版本和待审核状态。主站探索则需要点击“提交并自动核验”。</p>')}${faq('报名了活动，Quest 里怎么还没完成？', '<p>报名活动和完成任务是两个流程。请核对是否存在对应任务，并在 Quest 按明确的提交要求提交成果。</p>')}`),
      section('ask-for-help', '怎样描述问题，更容易得到帮助', `${tutorial('faq', 'ask-for-help')}<p>在社团群聊中联系相应系统管理员、活动组织者或任务负责人。可以复制下面的模板，补充信息后发送。</p><div class="copy-block"><button class="copy-button" data-copy-help type="button">复制模板</button><pre id="help-template">遇到问题的系统：主站 / 邮箱 / Quest
账号或学号：
发生时间：
页面地址或任务名称：
我进行的操作：
页面提示或实际结果：
已经尝试的方法：
截图：请遮挡密码、验证码和其他人的个人信息</pre></div><p>截图请尽量包含页面提示和发生问题的位置。遇到任务要求或审核问题，优先联系任务负责人，并附任务名称与提交版本。</p>`),
    ],
  },
];

export const checklist = [
  { id: 'account', label: '激活社团账号', article: 'activation' },
  { id: 'mail', label: '开通并登录邮箱', article: 'mail' },
  { id: 'quest', label: '完成 Quest 新人引导', article: 'quest' },
  { id: 'exploration', label: '完成认识社团任务', article: 'exploration' },
];

export const homeSections = [
  { id: 'journey', title: '推荐上手路线' },
  { id: 'guides', title: '按需要查阅' },
  { id: 'first-task', title: '你的第一项任务' },
];

export const illustrationStats = { steps: allSteps.length, articles: Object.fromEntries(articles.map(a => [a.id, allSteps.filter(s => s.article === a.id).length])) };
