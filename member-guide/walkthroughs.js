// Each entry is one visible user action and owns one independently rendered image.
// Interface illustrations use demo data. No accounts or business APIs are accessed.
const step = (id, title, body, result, screen, focus, state = {}) => ({ id, title, body, result, screen, focus, state });

export const walkthroughs = {
  activation: {
    welcome: [
      step('account-open', '打开主站登录页', '访问主站 /login，找到右侧的“账号登录”表单。准备社团通知或管理员提供的账号与当前密码。', '页面显示账号、密码输入框和“登录”按钮。', 'site-login', 'form'),
      step('account-username', '填写用户名或学号', '在“账号”输入框填写自己的用户名或学号。图中 demo.member 仅为演示账号。', '账号框中显示你填写的账号，暂时还未登录。', 'site-login', 'username', { user: true }),
      step('account-password', '填写当前密码', '在“密码”框填写当前密码。如果不知道密码，先联系管理员确认；不要猜测初始密码。', '密码框以圆点显示已输入内容。', 'site-login', 'password', { user: true, password: true }),
      step('account-login', '点击“登录”', '核对账号后点击“登录”。未激活成员在启用激活引导时会进入激活页；没有自动进入时，可手动打开 /activation。', '登录成功并进入激活引导；失败时按错误提示处理。', 'site-login', 'submit', { user: true, password: true }),
      step('activation-welcome', '欢迎页：点击“Fork 未来”', '确认页面上的成员称呼与所属部门，阅读欢迎信息，然后点击“Fork 未来”。', '进入社团使命介绍页。', 'activation-intro', 'next', { stage: 'welcome' }),
      step('activation-mission', '使命页：点击“Git Init 使命”', '阅读“让开源在校园里真正发生”与开放协作、工程实践、持续成长的介绍，再继续。', '进入“我们做什么”页面。', 'activation-intro', 'next', { stage: 'mission' }),
      step('activation-directions', '方向页：点击“Explore 方向”', '了解技术分享、项目实践、竞赛训练和开源协作四个方向，点击底部继续按钮。', '进入你所属部门的介绍页面。', 'activation-intro', 'next', { stage: 'directions' }),
      step('activation-department', '部门页：点击“Merge 入列”', '阅读页面展示的部门职责与负责人信息。图中的部门仅为示例，实际以你的成员信息为准。', '进入社团核心团队介绍页。', 'activation-intro', 'next', { stage: 'department' }),
      step('activation-team', '团队页：点击“Connect 团队”', '认识社团负责人及后续协作时的联系对象，然后点击“Connect 团队”。', '进入“加入社团群聊”。', 'activation-intro', 'next', { stage: 'team' }),
    ],
    'join-group': [
      step('group-number', '查看并复制社团 QQ 群号', '在“QQ群身份验证”区域确认群号，点击“复制群号”。如果还需要加入微信群，按页面的微信群二维码操作。', '获得本次应申请加入的社团 QQ 群号。', 'activation-group', 'groupNumber'),
      step('group-generate', '点击“生成入群验证码”', '点击生成按钮。页面会显示一枚一次性入群验证码，有效期以生成后的提示为准。', '页面出现你的入群验证码和“复制验证码”按钮。', 'activation-group', 'generate'),
      step('group-copy', '点击“复制验证码”', '复制页面中为你生成的验证码。不要使用本指南图片里的演示文字，也不要把验证码分享给他人。', '验证码已复制，可以前往 QQ 填写入群申请。', 'activation-group', 'copy', { token: true }),
      step('group-qq-apply', '在 QQ 入群申请中填写验证码', '打开 QQ，搜索刚才的群号并申请加入，将验证码填入申请验证信息。QQ 客户端版本的界面可能不同，图中只演示填写关系。', '入群申请已提交，等待审核与系统识别。', 'qq-application', 'verification'),
      step('group-continue', '回到网页，确认“已加入QQ群”后继续', '回到激活页等待自动检测。出现“已加入QQ群”后，点击“Link 社群”。若一直未识别，请联系管理员核对账号与 QQ 的绑定。', '进入基础资料填写页面。', 'activation-group', 'next', { token: true, joined: true }),
    ],
    'profile-password': [
      step('profile-name', '填写姓名和学号', '在“完善你的基础信息”中填写或核对真实姓名与学号。演示图中的姓名和学号不能代替你的资料。', '姓名与学号已填写，继续核对学籍信息。', 'activation-profile', 'identity'),
      step('profile-school', '填写学院、专业、年级和班级', '按实际学籍填写学院、专业、年级和班级。手机号可留空；填写时应为有效的 11 位号码。', '必填基础资料完整，可以保存。', 'activation-profile', 'education', { complete: true }),
      step('profile-save', '点击“保存并继续”', '核对资料后提交。若提示必填项缺失或手机号格式错误，先修正对应字段再保存。', '基础资料保存成功，进入密码设置页。', 'activation-profile', 'submit', { complete: true }),
      step('password-current', '填写当前密码', '在“设置你的密码”页面第一项输入当前登录密码。此处要求当前密码，不是 QQ 密码或邮箱密码。', '当前密码已填写，接下来设置新密码。', 'activation-password', 'current'),
      step('password-new', '填写新密码', '在新密码框填写准备使用的密码。页面当前至少要求 4 位，建议使用更长且独立的密码，并自行妥善保存。', '新密码满足页面要求，可以提交激活。', 'activation-password', 'new', { filled: true }),
      step('password-activate', '点击“Deploy 激活”', '确认当前密码和新密码无误后，点击激活按钮，等待页面处理。出现错误时按提示核对，不要连续重复点击。', '激活完成后显示成功提示与成员凭证。', 'activation-password', 'submit', { filled: true }),
      step('activation-success', '确认激活成功', '查看“成员身份与数字通行证已完成激活”提示。按系统提示重新登录；成员姓名、部门与凭证编号以你实际页面为准。', '成员账号已经激活。', 'activation-success', 'success'),
      step('account-relogin', '使用新密码重新登录', '回到主站登录页，填写相同账号和刚设置的新密码，点击“登录”。后续邮箱和 Quest 都使用这个社团账号授权。', '可以正常进入主站，账号激活流程完成。', 'site-login', 'submit', { user: true, password: true, relogin: true }),
    ],
  },
  mail: {
    login: [
      step('mail-open', '打开社团邮箱，点击安全登录', '访问 mail.jmi-openatom.cn，点击“使用 OAuth 安全登录”。也可以从主站顶部的邮箱入口进入。', '跳转到社团统一登录或授权页面。', 'mail-login', 'submit'),
      step('mail-main-login', '如有提示，先登录社团账号', '如果主站尚未登录，在统一登录页填写账号与当前密码后点击“登录”。已经登录时可能直接进入授权页。', '进入本次邮箱应用的授权确认流程。', 'site-login', 'submit', { user: true, password: true, destination: '邮箱' }),
      step('mail-consent', '核对应用信息，确认本次授权', '如果出现“确认授权登录”，核对应用名称、当前账号和申请范围。确认是自己发起的邮箱登录后，点击“同意并继续”。', '返回邮箱；首次使用可能出现地址激活页。', 'oauth-consent', 'submit', { app: 'OpenAtom Mail', domain: 'mail.jmi-openatom.cn' }),
    ],
    address: [
      step('mail-address-mode', '选择邮箱地址生成方式', '首次出现“激活你的邮箱地址”时，选择“延续拼音”或“自定义主机名”。已经有可用地址的成员可跳过本节。', '延续拼音按姓名生成；选自定义时会显示前缀输入框。', 'mail-address', 'mode'),
      step('mail-address-custom', '如果自定义，填写邮箱前缀', '选择“自定义主机名”后填写前缀。仅允许小写字母、数字和点号，2–48 位。图中 demo.member 仅作演示。', '前缀与 @mailer.jmi-openatom.cn 组成你的拟用地址。', 'mail-address', 'prefix', { custom: true }),
      step('mail-address-confirm', '核对后点击“确定并进入邮箱”', '地址确定后一般不可自行修改。仔细核对所选方式与前缀，然后点击确认；地址不可用时按提示调整。', '地址激活成功，显示完整邮箱地址。', 'mail-address', 'submit', { custom: true }),
      step('mail-address-success', '记录完整地址，点击“进入邮箱”', '记录成功页显示的完整邮箱地址，再点击“进入邮箱”。请以自己的实际地址为准，不要照抄图片中的示例。', '进入邮箱收件界面。', 'mail-address-success', 'submit'),
    ],
    read: [
      step('mail-inbox-folder', '选择“收件箱”', '在电脑端左侧文件夹中点击“收件箱”。手机端的布局可能有所不同，按当前界面进入邮件列表。', '中间区域展示收件箱中的邮件。', 'mail-inbox', 'inbox'),
      step('mail-open-message', '点击要阅读的邮件', '在邮件列表中点击主题。先核对发件人与主题，再阅读完整正文，留意通知的时间与具体要求。', '电脑端右侧显示正文；手机端进入邮件阅读页。', 'mail-inbox', 'message'),
      step('mail-search', '输入关键词搜索邮件', '在顶部搜索框输入主题、发件人或正文关键词。找不到时核对当前文件夹，并检查垃圾邮件、归档或废纸篓。', '列表按搜索条件显示相关邮件。', 'mail-inbox', 'search', { searching: true }),
      step('mail-reply', '需要回复时，点击正文工具栏的回复', '打开目标邮件后，点击正文顶部的回复按钮。图中高亮的是回复入口；正式发送前仍需核对收件人与内容。', '打开带有回复上下文的写信窗口。', 'mail-reader', 'reply'),
      step('mail-download', '点击正文下方的附件下载', '邮件有附件时，在正文下方点击附件。当前只支持下载，不提供在线预览；附件处理失败时按页面提示操作。', '浏览器开始下载所选附件，或显示具体错误提示。', 'mail-reader', 'attachment'),
    ],
    send: [
      step('mail-compose-open', '点击“写邮件”', '在左侧点击“写邮件”；手机端可使用底部“写信”入口。', '打开“新邮件”窗口。', 'mail-inbox', 'compose'),
      step('mail-compose-to', '填写收件人', '在“收件人”填写完整邮箱地址，多个地址用逗号分隔。请替换为真正要联系的收件人。', '收件人栏已填写，继续填写主题。', 'mail-compose', 'to'),
      step('mail-compose-subject', '填写明确的邮件主题', '在“主题”中概括邮件目的，例如“【任务咨询】姓名 · 任务名称”。', '主题已填写，对方能够快速了解邮件目的。', 'mail-compose', 'subject', { subject: true }),
      step('mail-compose-body', '填写邮件正文', '说明来意、背景、已尝试的方法和具体问题。需要引用成果时附上可访问的链接。', '正文已整理，可以检查或添加附件。', 'mail-compose', 'body', { subject: true, body: true }),
      step('mail-compose-attachment', '按需点击“添加附件”', '选择电脑上的附件并等待上传完成。每封邮件最多 10 个附件，单个和总大小都不能超过 20 MiB；不需要附件可跳过。', '上传成功的文件显示在待发送附件区域。', 'mail-compose', 'attachment', { subject: true, body: true, attachment: true }),
      step('mail-compose-send', '核对后点击“发送邮件”', '检查收件人、主题、正文和附件，确认无误再发送。等待“邮件已提交发送”提示，不要重复点击。', '邮件已提交发送；这不代表收件人已经收到或阅读。', 'mail-compose', 'submit', { subject: true, body: true, attachment: true }),
      step('mail-sent', '到“已发送”查看记录', '在文件夹中点击“已发送”确认记录。如果对方未收到，核对地址、查看退信，并请对方检查垃圾邮件。', '可以查到刚才提交发送的邮件记录。', 'mail-inbox', 'sent', { sent: true }),
    ],
  },
  quest: {
    'first-login': [
      step('quest-login', '打开 Quest，使用社团账号登录', '访问 quest.jmi-openatom.cn，点击“使用 JMI-OPENATOM 账号登录”。Quest 不提供独立注册或密码登录。', '进入主站统一登录或授权页面。', 'quest-login', 'submit'),
      step('quest-main-login', '如有提示，登录主站账号', '主站尚未登录时，填写自己的社团账号与当前密码并点击“登录”。已经登录时可以跳过此页。', '进入 Quest 的授权或返回流程。', 'site-login', 'submit', { user: true, password: true, destination: 'Quest' }),
      step('quest-consent', '核对 Quest 授权后继续', '若出现授权确认页，检查应用、账号与申请范围。确认是自己发起的登录，再点击“同意并继续”。', '返回 Quest，首次登录进入七步新人引导。', 'oauth-consent', 'submit', { app: 'OpenAtom Quest', domain: 'quest.jmi-openatom.cn' }),
      step('quest-onboarding-name', '第 1 步：核对名片与昵称', '填写姓名或社团昵称，核对同步的学校、学院、专业和年级，再点击“保存并继续”。学籍有误时先在主站核对或请管理员协助。', '保存名片，进入成员行为准则页面。', 'quest-onboarding', 'answer', { stage: 'identity' }),
      step('quest-onboarding-conduct', '第 2 步：阅读并同意行为准则', '阅读友善协作、真实提交、保护隐私和对成果负责的说明；同意时勾选复选框并点击“保存并继续”。', '进入技术方向选择。', 'quest-onboarding', 'answer', { stage: 'conduct' }),
      step('quest-onboarding-direction', '第 3 步：选择技术方向', '至少选择一个感兴趣的方向。第一个选择的方向作为主方向，可以取消后重选，再点击“保存并继续”。', '已选择的方向用于后续成长路线推荐。', 'quest-onboarding', 'answer', { stage: 'direction' }),
      step('quest-onboarding-skills', '第 4 步：填写基础与可投入时间', '如实填写已掌握的技能和每周可投入时间，可选填代码主页、学习经历与个人简介，保存后继续。', '进入基础测评。', 'quest-onboarding', 'answer', { stage: 'skills' }),
      step('quest-onboarding-assessment', '第 5 步：选择当前基础', '从“刚刚开始”“有一些基础”“做过完整项目”中选择最符合自己的状态，再保存继续。零基础也可以开始。', '系统展示推荐的成长路线。', 'quest-onboarding', 'answer', { stage: 'assessment' }),
      step('quest-onboarding-route', '第 6 步：选择成长路线', '根据刚才的方向选择推荐路线，再保存继续。没有可用路线时按页面提示先完成新人任务，之后再加入路线。', '进入最后一步的名片与首个任务确认。', 'quest-onboarding', 'answer', { stage: 'route' }),
      step('quest-onboarding-finish', '第 7 步：确认名片并完成引导', '核对信息和新人任务。页面提供“领取任务，完成引导”时点击领取；若已领取或已完成，按钮会显示相应的完成引导文案。', '完成新人引导，并进入任务或工作台继续。', 'quest-onboarding', 'next', { stage: 'finish' }),
    ],
    route: [
      step('quest-route-open', '从导航打开“成长路线”', '点击 Quest 导航中的“成长路线”，找到想继续了解的方向。', '页面显示与你所选技术方向相关的成长路线。', 'quest-catalog', 'routes', { view: 'routes' }),
      step('quest-route-detail', '点击“查看路线”或“继续路线”', '打开一条路线，按 L0 到 L4 查看阶段与任务。不要把路线名称或任务数量当作固定不变的内容。', '进入该路线详情，可以了解阶段目标与任务。', 'quest-catalog', 'card', { view: 'routes' }),
    ],
    claim: [
      step('quest-task-center', '打开“任务中心”并选择阶段', '点击“任务中心”，按当前基础选择“全部”或 L0–L4。也可从成长路线进入具体任务。', '列表展示当前筛选条件下的可见任务。', 'quest-catalog', 'filters'),
      step('quest-task-open', '点击任务卡片进入详情', '选择适合自己的任务，点击标题或卡片，不要只看积分就直接开始。', '显示任务目标、步骤、提交要求与验收标准。', 'quest-catalog', 'card'),
      step('quest-task-requirements', '阅读目标、步骤和验收要求', '逐项阅读四个内容区，并检查负责人、截止时间、提交限制与前置任务。存在未完成的前置任务时先完成它。', '清楚这项任务要做什么、提交什么以及怎样通过。', 'quest-task', 'requirements'),
      step('quest-task-claim', '点击“领取任务”', '确认前置条件满足后点击领取。负责人直接分配的任务可以到“我的任务”查看，不需要重复领取。', '创建你的任务记录，进入个人任务页继续。', 'quest-task', 'claim'),
      step('quest-my-tasks', '在“我的任务”中继续', '从导航打开“我的任务”，点击对应记录。这里也展示负责人分配给你的任务。', '进入个人任务详情，查看状态并准备提交。', 'quest-assignments', 'row'),
    ],
  },
  exploration: {
    prepare: [
      step('exploration-find', '找到“主站探索：认识开放原子开源社团”', '在任务中心 L0 或新人引导中寻找这个完整任务名。它使用个人标记自动核验。', '找到正确的新人任务，而不是普通实践任务。', 'quest-catalog', 'card'),
      step('exploration-claim', '领取并进入个人任务记录', '阅读要求后点击“领取任务”；已领取时点击“查看我的任务”，或从“我的任务”进入。', '出现“提交个人探索标记”的专用表单。', 'quest-task', 'claim', { exploration: true }),
    ],
    'three-pages': [
      step('exploration-about-open', '点击“关于我们”的“打开页面”', '保持 Quest 登录，使用同一个浏览器，从表单右侧链接打开主站“关于我们”。', '新标签页打开社团介绍页面。', 'exploration-form', 'aboutLink'),
      step('exploration-about-copy', '阅读“关于我们”，复制第 1 枚标记', '阅读社团介绍和新人成长方式，找到探索标记区域，点击“复制我的标记”。使用自己账号中的完整标记，不要用图片里的演示文字。', '剪贴板中保存“关于我们”页面的个人标记。', 'site-flag', 'copy', { page: 'about' }),
      step('exploration-about-paste', '回到 Quest，粘贴到“关于我们”', '切回任务记录，将刚复制的完整 OA{…} 标记粘贴到“关于我们”输入框。', '第一个输入框已填写，对应关系正确。', 'exploration-form', 'about', { count: 1 }),
      step('exploration-regulations-open', '打开“规章制度”页面', '点击“规章制度”旁边的“打开页面”，继续使用同一个 Quest 账号。', '主站打开公开制度与协作规范页面。', 'exploration-form', 'regulationsLink', { count: 1 }),
      step('exploration-regulations-copy', '阅读规章制度，复制第 2 枚标记', '浏览公开制度与协作规范，找到第 2 枚个人标记，点击“复制我的标记”。列表暂时为空也可以在页面中寻找标记。', '获得“规章制度”页面的个人标记。', 'site-flag', 'copy', { page: 'regulations' }),
      step('exploration-regulations-paste', '粘贴到 Quest 的“规章制度”框', '回到任务表单，将第 2 枚标记填入“规章制度”，不要覆盖第 1 枚标记。', '前两个输入框已按页面分别填写。', 'exploration-form', 'regulations', { count: 2 }),
      step('exploration-activities-open', '打开“社团活动”页面', '点击“社团活动”旁边的“打开页面”，看看社团有哪些参与机会。', '主站打开社团活动页。', 'exploration-form', 'activitiesLink', { count: 2 }),
      step('exploration-activities-copy', '浏览活动，复制第 3 枚标记', '了解活动信息和报名方式，在探索标记区域点击“复制我的标记”，复制第 3 枚个人标记。活动列表为空时也能寻找标记。', '获得“社团活动”页面的个人标记。', 'site-flag', 'copy', { page: 'activities' }),
      step('exploration-activities-paste', '粘贴到 Quest 的“社团活动”框', '回到任务表单，将最后一枚标记填入“社团活动”。核对三个页面与三个输入框一一对应。', '三个个人标记都已填写完整。', 'exploration-form', 'activities', { count: 3 }),
    ],
    'submit-flags': [
      step('exploration-plan', '填写“我想怎样参与社团”', '用自己的话写 1–2 句话，说明想参与什么、准备怎样开始。至少 8 个字，建议写具体方向和第一步行动。', '参与计划已填写，三个标记也均已就绪。', 'exploration-form', 'reflection', { count: 3, reflection: true }),
      step('exploration-submit', '点击“提交并自动核验”', '再次核对标记与页面的对应关系后提交。这个任务不需要仓库、截图或视频。', '系统立即核验三枚个人标记和参与计划。', 'exploration-form', 'submit', { count: 3, reflection: true }),
      step('exploration-passed', '确认任务已经通过', '看到校验通过提示后检查任务状态与积分。失败时核对账号、页面对应关系及完整字符后再提交。', '状态为“已通过”，可以继续成长路线。', 'quest-status', 'passed', { exploration: true }),
    ],
  },
  activities: {
    discover: [
      step('activity-list', '打开主站“活动”', '从社团主站导航点击“活动”，或访问 /activities，浏览重点活动和活动列表。', '可以看到已发布的活动与参与入口。', 'activity-list', 'nav'),
      step('activity-open', '点击活动卡片查看详情', '选择感兴趣的活动，点击标题、卡片或“查看详情”，阅读完整安排。', '进入该活动的详情页面。', 'activity-list', 'card'),
    ],
    register: [
      step('activity-facts', '核对时间、地点和报名方式', '阅读活动介绍，确认时间地点、需要准备的物品以及是否需要官网报名。地点待定时关注后续通知。', '已了解参加这次活动需要做哪些准备。', 'activity-detail', 'facts'),
      step('activity-fill', '需要报名时，登录并填写报名字段', '如果要求官网报名，先登录，再按实际活动配置填写必填字段。图片中的字段只是演示；无需报名的活动按说明参与，可跳过本步和提交步骤。', '报名字段已填写，可以核对并提交。', 'activity-detail', 'fields', { form: true }),
      step('activity-submit', '点击“提交报名”', '确认填写正确后提交。按钮不可用时先阅读页面的登录、时间范围或其他限制提示。', '出现“报名已提交”提示，继续关注页面状态与组织者通知。', 'activity-detail', 'submit', { form: true }),
      step('activity-confirm', '确认提交结果，关注后续通知', '查看提交提示，核对是否还需要进一步确认。报名活动不会自动完成 Quest 任务；有对应任务时仍需按要求提交。', '已知道本次报名的处理结果和后续安排。', 'activity-detail', 'feedback', { form: true, submitted: true }),
    ],
  },
  submission: {
    'prepare-result': [
      step('submission-open', '从“我的任务”打开任务记录', '找到准备提交的普通实践任务并进入个人记录。若正在做“主站探索”，请使用上一篇的专用表单。', '进入任务详情，可查看状态、要求与提交区域。', 'quest-assignments', 'row', { normal: true }),
      step('submission-requirements', '提交前重新对照验收标准', '核对任务详情中的“提交要求”和“验收标准”，整理完成情况与审核人可以访问的成果链接。', '明确哪些内容必须提交，避免只有链接而没有说明。', 'quest-task', 'requirements', { normal: true }),
    ],
    'fill-submit': [
      step('submission-note', '填写“完成情况说明”', '说明做了什么、覆盖哪些要求，以及审核人如何验证结果。这一项是必填。', '审核人能了解你的工作范围和验证方式。', 'submission-form', 'note'),
      step('submission-links', '添加可核验的成果链接', '按任务要求填写仓库、Pull Request、在线演示或视频地址；未使用的项目可以留空。当前没有直接上传成果附件的入口。', '需要的成果链接已填写，审核人可以访问。', 'submission-form', 'links', { links: true }),
      step('submission-learning', '填写遇到的问题与学习总结', '记录遇到的问题、尝试的方法、解决过程与新的收获，便于导师理解你的实践过程。', '提交材料中包含学习过程和总结。', 'submission-form', 'learning', { links: true, learning: true }),
      step('submission-ai-check', '如果使用 AI，勾选对应选项', '本次成果使用了 AI 时，勾选“本次成果使用了 AI 工具”。未使用时保持未勾选，并跳过下一步。', '勾选后显示“AI 参与的具体内容”输入框。', 'submission-form', 'aiCheck', { links: true, learning: true, ai: true }),
      step('submission-ai-detail', '说明 AI 参与的具体内容', '填写用了什么工具、用于哪些环节，以及你如何理解、检查和验证结果。勾选使用 AI 后，此项为必填。', 'AI 使用情况已如实说明，可以提交。', 'submission-form', 'aiDetail', { links: true, learning: true, ai: true, aiDetail: true }),
      step('submission-submit', '点击“提交审核”', '核对成果链接与说明后提交。浏览器自动保存草稿不等于提交，必须点击提交按钮。', '创建一个提交版本，任务进入“待审核”。', 'submission-form', 'submit', { links: true, learning: true, ai: true, aiDetail: true }),
    ],
    status: [
      step('submission-pending', '检查版本与“待审核”状态', '提交后检查当前版本和状态，确认不是只保存了草稿。待审核时按页面提示等待。', '确认正式提交成功，进入导师审核流程。', 'quest-status', 'pending'),
      step('submission-notifications', '打开“站内通知”查看审核消息', '留意 Quest 的站内通知。收到审核或修改消息后，点击对应通知或回到任务记录。', '能够查看审核结论与后续处理入口。', 'quest-notifications', 'notification'),
      step('submission-passed', '通过后查看积分与下一步', '任务状态为“已通过”时，查看任务积分与后续成长路线。积分数值以当前页面记录为准。', '本次任务完成，可以继续后续实践。', 'quest-status', 'passed', { normal: true }),
    ],
    revision: [
      step('submission-feedback', '需要修改时，阅读“导师最新反馈”', '打开标为“需修改”的任务，逐项阅读反馈，先修正作品和成果说明。', '明确需要修改的内容，再准备新版本。', 'quest-status', 'feedback', { revision: true }),
      step('submission-resubmit', '在原任务记录提交新版本', '完成修改后，通过同一任务的“重新提交新版本”表单更新说明与链接，再点击“提交审核”。', '产生新版本，提交历史保留之前的审核结论。', 'submission-form', 'submit', { links: true, learning: true, revision: true }),
      step('submission-history', '在“提交历史”核对各版本', '查看版本、提交时间与审核结论，确认最新提交已进入审核；旧版本仍可用于回顾。', '能够追踪修改过程与每次审核反馈。', 'quest-status', 'history', { revision: true }),
      step('submission-appeal', '有异议时，从提交历史发起申诉', '对“需修改”或“未通过”的某次审核有异议时，点击“对本次审核发起申诉”。填写具体异议与可核验证据后提交，等待复核。', '申诉进入复核流程；不是再次提交成果。', 'quest-status', 'appeal', { revision: true }),
    ],
  },
  faq: {
    'ask-for-help': [
      step('help-copy', '复制求助模板，补充问题信息', '点击本页的“复制模板”，填写系统、账号或学号、发生时间、任务名称、操作与页面提示。截图先遮挡密码、验证码和他人的个人信息，再自行发给相应负责人。', '准备好一份便于管理员或任务负责人核查的问题说明。', 'help-template', 'copy'),
    ],
  },
};

export const allSteps = Object.entries(walkthroughs).flatMap(([article, sections]) => {
  let number = 0;
  return Object.entries(sections).flatMap(([section, steps]) => steps.map(s => ({ ...s, article, section, number: ++number })));
});

export const stepGroups = Object.fromEntries(Object.entries(walkthroughs).map(([article, sections]) => [article,
  Object.fromEntries(Object.keys(sections).map(section => [section, allSteps.filter(s => s.article === article && s.section === section)])),
]));
