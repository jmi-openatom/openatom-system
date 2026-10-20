const ink='#1d1d1f', muted='#6e6e73', edge='#dedee3';
export const escapeXml=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'})[c]);
const rect=(x,y,w,h,fill='#fff',r=8)=>`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${edge}"/>`;
const text=(x,y,t,size=16,color=ink,weight=400)=>`<text x="${x}" y="${y}" font-size="${size}" fill="${color}" font-weight="${weight}">${escapeXml(t)}</text>`;
const lines=(x,y,list,size=16,gap=34)=>list.map((t,i)=>text(x,y+i*gap,t,size,muted)).join('');
const articleNames={activation:'社团账号激活',mail:'社团邮箱',quest:'Quest 入门',exploration:'认识社团任务',activities:'社团活动',submission:'成果提交与反馈',faq:'常见问题'};

export function renderStep(step,logo){
  let body='';const targets={},s=step.state;
  const add=(key,svg,bounds)=>{body+=svg;if(key)targets[key]=bounds;};
  const txt=(x,y,t,size=16,color=ink,weight=400)=>{body+=text(x,y,t,size,color,weight);};
  const area=(key,x,y,w,h,fill='#fafafa')=>add(key,rect(x,y,w,h,fill),[x-5,y-5,w+10,h+10]);
  const field=(key,x,y,w,label,value,h=42)=>add(key,text(x,y,label,14,muted)+rect(x,y+12,w,h,'#fafafa',6)+text(x+14,y+40,value,16,value?ink:muted),[x-5,y+7,w+10,h+10]);
  const btn=(key,x,y,w,label,primary=true)=>add(key,rect(x,y,w,42,primary?ink:'#fff',6)+text(x+16,y+28,label,16,primary?'#fff':ink,600),[x-5,y-5,w+10,52]);
  const check=(x,y,t,checked=false)=>rect(x,y,18,18,checked?ink:'#fff',4)+(checked?text(x+3,y+15,'✓',15,'#fff'):'')+text(x+30,y+15,t,15);
  const brand=(label='开放原子开源社团')=>{body+=`<image x="60" y="124" width="34" height="34" href="data:image/svg+xml;base64,${logo}"/>`+text(106,141,label,16,ink,600)+text(106,160,'JMI-OPENATOM',10,muted,600);};
  const title=(label,sub='',x=320,y=237)=>{txt(x,y,label,29,ink,600);if(sub)txt(x,y+38,sub,15,muted);};
  const quest=(active='任务中心')=>{
    body+=rect(31,111,203,557,'#fafafa',0);brand('OPENATOM QUEST');
    ['工作台','成长路线','任务中心','我的任务','积分等级','站内通知'].forEach((label,i)=>{let y=207+i*47;add({'成长路线':'routes','任务中心':'tasks','我的任务':'assignments','站内通知':'notifications'}[label],(label===active?rect(45,y-27,175,40,'#e8e8ed',6):'')+text(64,y,label,16,label===active?ink:muted),[40,y-32,185,50]);});
    txt(54,635,'演示成员 · L0',12,muted);
  };
  const site=()=>{brand();['首页','活动','博客','成员','更多'].forEach((t,i)=>add(t==='活动'?'nav':null,text(570+i*84,147,t,16,muted),[554+i*84,120,74,43]));body+=`<path d="M31 181h1138" stroke="${edge}"/>`;};
  const mail=()=>{
    brand('JMI-OPENATOM 邮箱系统');txt(107,179,'demo.member@mailer.jmi-openatom.cn',11,muted);
    add('search',rect(469,128,403,39,'#fafafa',6)+text(485,154,s.searching?'技术分享':'搜索主题、发件人或正文',14,muted),[464,123,413,49]);
    body+=rect(31,195,202,473,'#fafafa',0);btn('compose',48,215,168,'＋ 写邮件');
    ['收件箱','已发送','草稿','归档','垃圾邮件','废纸篓'].forEach((t,i)=>{const y=302+i*45;const active=t===(s.sent?'已发送':'收件箱');add(i===0?'inbox':i===1?'sent':null,(active?rect(44,y-26,178,40,'#e8e8ed',6):'')+text(65,y,t,15,active?ink:muted),[39,y-31,188,50]);});
    txt(49,639,'演示数据 · 非真实邮件',12,muted);
  };
  const exploration='主站探索：认识开放原子开源社团',normal='完成一次入门实践（示例任务）';
  let url='www.jmi-openatom.cn';
  if(step.screen.startsWith('mail'))url='mail.jmi-openatom.cn';
  if(step.screen.startsWith('quest')||['exploration-form','submission-form'].includes(step.screen))url='quest.jmi-openatom.cn';
  if(step.screen.startsWith('activation'))url+='/activation';

  switch(step.screen){
    case 'site-login':{
      url='www.jmi-openatom.cn/login';body+=rect(31,111,470,557,'#f5f5f7',0);brand();title('开放原子开源社团','一个账号，连接整个社团。',65,315);body+=lines(65,458,['社团官网与活动信息','成员系统与开放应用','社团邮箱与 Quest 成长实践'],17,43);title('欢迎回来',s.destination?`登录后继续：${s.destination}`:'使用社团账号登录',620,231);txt(620,313,'账号登录',15,ink,600);
      targets.form=[608,198,454,417];field('username',620,350,423,'账号',s.user?'demo.member':'请输入用户名或学号');field('password',620,445,423,'密码',s.password?'••••••••••':'请输入密码');txt(930,531,'忘记密码？',12,muted);btn('submit',620,558,423,'登录 →');break;
    }
    case 'activation-intro':{
      brand();const variants={welcome:['欢迎加入','演示成员 · 示例部门','从这里连接伙伴，开启开源之旅。','Fork 未来'],mission:['让开源在校园里真正发生','开放协作 · 工程实践 · 持续成长','让每个人都能参与，用真实项目训练能力。','Git Init 使命'],directions:['我们做什么','技术分享 · 项目实践','竞赛训练 · 开源协作','Explore 方向'],department:['你的部门（演示）','了解部门职责与协作方向','部门和负责人信息以你的成员资料为准。','Merge 入列'],team:['与你同行的人','认识社团的核心管理团队','后续遇到问题时，可以联系相应负责人。','Connect 团队']};const[t,a,b,label]=variants[s.stage];txt(320,232,'GET CONNECTED / 成员激活引导',12,muted,600);title(t,'',320,299);txt(320,368,a,22,ink,600);txt(320,417,b,17,muted);btn('next',320,544,574,`${label} →`);break;
    }
    case 'activation-group':{
      brand();title('加入社团群聊','QQ 入群验证是账号激活的前置条件。',301,222);area(null,301,285,600,283);txt(324,321,'社团QQ群号',14,muted);txt(324,356,'以你的页面显示为准',22,ink,600);btn('groupNumber',718,319,156,'复制群号',false);
      if(s.token){txt(324,398,'入群验证码',14,muted);body+=rect(324,417,331,53);txt(338,450,'你的验证码（演示位置）',21,ink,600);btn('copy',679,422,195,'复制验证码',false);}else btn('generate',324,417,550,'生成入群验证码 →',false);
      txt(324,520,s.joined?'✓ 已加入QQ群':'生成验证码 → 申请入群 → 等待系统检测',15,s.joined?'#248a3d':muted);btn('next',301,598,600,s.joined?'Link 社群 →':'请先加入QQ群');break;
    }
    case 'qq-application':{
      url='QQ 客户端 · 入群申请关系示意（版本不同可能有差异）';title('QQ 入群申请','客户端版本不同，请以实际申请界面为准。',300,237);area(null,300,306,600,293);txt(326,350,'申请加入社团 QQ 群',23,ink,600);field('verification',326,408,547,'申请验证信息','粘贴激活页生成的个人验证码',66);btn('submit',326,533,547,'提交入群申请（位置示意）');break;
    }
    case 'activation-profile':{
      brand();title('完善你的基础信息','带 * 的项目为必填。',305,215);field(null,305,287,281,'姓名 *','演示成员');field(null,607,287,281,'学号 *','DEMO-2026');targets.identity=[296,271,600,83];field(null,305,373,583,'学院 *',s.complete?'你的实际学院':'请选择学院');field(null,305,459,281,'专业 *',s.complete?'你的实际专业':'填写专业');field(null,607,459,281,'年级 *',s.complete?'2026':'例如：2026');field(null,305,544,583,'班级 *',s.complete?'你的实际班级':'填写班级');targets.education=[296,355,600,251];txt(305,637,'手机号：选填，有效的 11 位号码',12,muted);btn('submit',682,615,206,'保存并继续 →');break;
    }
    case 'activation-password':brand();title('设置你的密码','设置新密码后，你的账号将正式激活。');field('current',320,343,567,'当前密码','••••••••••');field('new',320,442,567,'新密码（至少 4 位）',s.filled?'••••••••••••':'请输入准备使用的新密码');btn('submit',320,556,567,'Deploy 激活 →');break;
    case 'activation-success':brand();title('欢迎加入，演示成员','你的成员身份与数字通行证已完成激活。');area('success',320,327,567,118);txt(343,362,'OPENATOM · FOUNDING THE FUTURE',12,muted,600);txt(343,400,'演示成员的成员凭证',23,ink,600);txt(343,427,'示例部门 · 编号以实际页面为准',13,muted);body+=rect(320,471,567,60,ink);txt(344,508,'✓ Account activated successfully',19,'#fff');btn('next',320,567,567,'Launch →');break;
    case 'mail-login':brand();txt(123,272,'OPENATOM MAIL',14,muted,600);title('欢迎使用社团邮箱','使用开放原子账号登录，开始收发社团邮件。',123,338);txt(123,421,'首次使用时，按页面提示确认自己的邮箱地址。',18,muted);btn('submit',123,508,518,'使用 OAuth 安全登录 →');txt(123,606,'社团账号统一授权 · 不需要另行注册网页登录密码',14,muted);break;
    case 'oauth-consent':{
      url='www.jmi-openatom.cn · 统一身份认证';body+=rect(31,111,450,557,'#f5f5f7',0);brand();title('统一身份认证平台','核对应用与账号，再决定是否授权。',65,330);title('确认授权登录','请确认以下应用和资料，再决定是否继续。',550,207);area(null,550,274,545,79);txt(572,307,s.app,21,ink,600);txt(572,335,s.domain,14,muted);txt(550,393,'将以“演示成员”的 OpenAtom 账号登录',17);txt(550,437,'应用将可获取',17,ink,600);body+=lines(550,476,['✓ 账号标识、姓名与头像','✓ 邮箱、手机号与学籍资料','✓ 社团成员身份、角色与权限'],15,30);btn('submit',550,589,545,'同意并继续');break;
    }
    case 'mail-address':{
      brand();title('激活你的邮箱地址','地址确定后一般不可自行修改，请仔细核对。');area(null,320,302,567,68);body+=check(340,321,'延续拼音',!s.custom);txt(370,355,'根据姓名自动生成',13,muted);area(null,320,384,567,68);body+=check(340,402,'自定义主机名',!!s.custom);txt(370,438,'手动填写想使用的邮箱前缀',13,muted);targets.mode=[312,294,583,166];if(s.custom){field('prefix',320,489,567,'邮箱前缀','demo.member   @mailer.jmi-openatom.cn');txt(320,565,'仅允许小写字母、数字和点号，2–48 位。',13,muted);}btn('submit',320,602,567,'确定并进入邮箱 →');break;
    }
    case 'mail-address-success':brand();title('✓ 激活成功！','你的邮箱地址是',320,282);area(null,320,368,567,73);txt(340,413,'demo.member@mailer.jmi-openatom.cn',24,ink,600);txt(320,488,'图中为演示地址；请记录自己的完整地址。',16,muted);btn('submit',320,552,567,'进入邮箱 →');break;
    case 'mail-inbox':case 'mail-reader':{
      mail();title(s.sent?'已发送':'收件箱','',253,237);area('message',250,264,314,110,'#f5f5f7');txt(268,299,'技术分享活动提醒（演示）',16,ink,600);txt(268,333,'活动通知与准备事项…',14,muted);txt(268,361,'演示记录 · 非真实通知',11,muted);txt(268,425,'任务咨询（演示）',17);txt(268,459,'你提交的邮件记录…',14,muted);btn('reply',616,211,110,'↶ 回复',false);txt(752,239,'归档',15,muted);txt(850,239,'移到废纸篓',15,muted);title('技术分享活动提醒','演示发件人 → 你的社团邮箱',616,307);body+=lines(616,418,['请查看活动详情，提前准备所需资料。','如有问题，请与活动组织者联系。','本图使用演示邮件，无真实通知或联系人。'],16,36);area('attachment',616,553,510,65);txt(632,582,'活动资料.pdf（示例附件）',16);txt(632,605,'点击下载 · 仅下载，不在线预览',12,muted);break;
    }
    case 'mail-compose':{
      mail();area(null,265,213,878,433,'#fff');txt(287,249,'新邮件',23,ink,600);field('to',287,292,833,'收件人','recipient@example.com（请替换）');field('subject',287,374,833,'主题',s.subject?'【任务咨询】演示成员 · 任务名称':'邮件主题');area('body',287,449,833,91);txt(302,481,s.body?'你好，我正在完成任务，已尝试以下方法……':'填写邮件正文，说明背景与具体问题',16);txt(302,518,s.body?'相关链接、问题和希望获得的帮助……':'',14,muted);if(s.attachment)txt(287,571,'⌕ 活动资料.pdf · 示例附件已上传',14,muted);btn('attachment',287,594,171,'添加附件',false);txt(476,620,'最多 10 个，总计 20 MiB',13,muted);btn('submit',924,594,196,'发送邮件 →');break;
    }
    case 'quest-login':{
      brand('JMI-OPENATOM-QUEST');title('把每一步学习，','',82,299);title('变成真实贡献。','',82,350);txt(82,412,'沿着成长路线完成任务，获得反馈，参与真实项目。',17,muted);btn('submit',82,506,561,'使用 JMI-OPENATOM 账号登录 →');area(null,750,175,358,448);txt(773,222,'从新人报到，到项目贡献者。',20,ink,600);body+=lines(782,289,['L0  新人报到','L1  开源入门','L2  技术学习','L3  项目实战','L4  项目贡献'],20,64);break;
    }
    case 'quest-onboarding':{
      brand('JMI-OPENATOM-QUEST');const stages={identity:[1,'欢迎加入开放原子开源社团'],conduct:[2,'一起建立友善的协作环境'],direction:[3,'选择你想探索的技术方向'],skills:[4,'让我们了解你的起点'],assessment:[5,'你现在处于哪一个阶段？'],route:[6,'选择一条成长路线'],finish:[7,'用第一项任务开启 Quest']};const[n,t]=stages[s.stage];txt(974,145,`0${n} / 07`,16,muted);txt(320,219,`STEP 0${n} · 新人引导`,12,muted,600);title(t,'',320,266);
      if(s.stage==='identity'){field(null,320,322,624,'姓名或社团昵称','演示成员');txt(320,428,'学校 / 学院 / 专业 / 年级',17,ink,600);txt(320,468,'由 LMS 自动同步，请核对自己的学籍信息。',16,muted);}
      if(s.stage==='conduct'){body+=lines(320,322,['友善协作：尊重不同经验与观点','真实提交：如实记录工作与 AI 辅助内容','保护隐私：不公开资料与访问凭证','对成果负责：按要求完成并验证'],17,39);body+=check(320,498,'我已阅读并同意社团成员行为准则',true);}
      if(s.stage==='direction'){['前端方向（示例）   ✓ 主方向','后端方向（示例）','按你的页面选择其他方向'].forEach((t,i)=>{area(null,320,305+i*70,624,56,i===0?'#e8e8ed':'#fff');txt(340,341+i*70,t,18);});}
      if(s.stage==='skills'){field(null,320,320,624,'已接触或掌握的技能（可选）','Git / HTML / CSS（演示）');field(null,320,409,624,'GitHub 或 Gitee 主页（可选）','https://github.com/你的账号');txt(320,525,'每周可投入时间：按实际填写，例如 4 小时',17,muted);}
      if(s.stage==='assessment'){['✓ 刚刚开始','○ 有一些基础','○ 做过完整项目'].forEach((t,i)=>{area(null,320,305+i*73,624,56,i===0?'#e8e8ed':'#fff');txt(340,341+i*73,t,18);});}
      if(s.stage==='route'){area(null,320,313,624,168);txt(340,355,'适合所选方向的成长路线（示例）',22,ink,600);txt(340,401,'L0 新人报到 → L4 项目贡献',18,muted);txt(340,449,'选择一条路线，或按页面提示稍后加入。',16,muted);}
      if(s.stage==='finish'){area(null,320,308,624,203);body+=lines(340,348,['社团昵称：演示成员','方向 / 每周投入 / 路线：核对自己的选择','新人任务：主站探索，认识开放原子开源社团'],16,43);txt(340,486,'首次领取时，完成引导将一起领取新人任务。',14,muted);}
      targets.answer=[312,296,640,250];btn('next',320,585,624,s.stage==='finish'?'领取任务，完成引导 →':'保存并继续 →');txt(320,650,'已完成的步骤自动保存，刷新后可继续。',12,muted);break;
    }
    case 'quest-catalog':{
      const route=s.view==='routes';quest(route?'成长路线':'任务中心');title(route?'成长路线':'任务中心','具体路线、任务与数量以当前系统为准。',270,180);add('filters',['全部','L0','L1','L2','L3','L4'].map((t,i)=>rect(270+i*115,254,100,38,i===1?ink:'#fafafa',6)+text(293+i*115,280,t,16,i===1?'#fff':muted)).join(''),[262,246,693,54]);area('card',270,331,846,203,'#fff');txt(295,374,route?'你的方向 · 成长路线（示例）':exploration,24,ink,600);txt(295,427,route?'选择适合自己的路线，从新人报到逐步实践。':'寻找三个页面的个人标记，认识社团、规则与活动。',16,muted);if(route)btn(null,295,471,204,'查看路线 →',false);else{txt(295,494,'L0 · 入门',16,muted);txt(963,495,'可领取 →',16);}break;
    }
    case 'quest-task':{
      quest();title(s.normal?normal:exploration,'',268,189);['学习目标','操作步骤','提交要求','验收标准'].forEach((t,i)=>{area(null,268,242+i*91,597,75);txt(286,271+i*91,t,18,ink,600);txt(286,301+i*91,`阅读本任务的${t}，以当前任务为准。`,14,muted);});targets.requirements=[260,234,613,364];area(null,903,242,213,348);txt(921,282,'任务信息',20,ink,600);body+=lines(921,328,['负责人：以页面为准','截止时间：以页面为准','前置任务：先检查完成','提交限制：查看要求'],13,42);btn('claim',921,521,177,'领取任务 →');break;
    }
    case 'quest-assignments':quest('我的任务');title('我的任务','继续进行中的任务，或根据导师反馈提交新版本。',268,182);area('row',268,288,850,133,'#fff');txt(289,326,'进行中',15,muted);txt(403,336,s.normal?normal:exploration,21,ink,600);txt(403,378,'点击记录，查看要求并继续提交。',15,muted);txt(1068,357,'→',25);break;
    case 'exploration-form':{
      quest('我的任务');title('提交个人探索标记','保持 Quest 登录，分别打开页面并填写自己的标记。',269,166);['about','regulations','activities'].forEach((key,i)=>{let y=247+i*87;field(key,269,y,633,['关于我们','规章制度','社团活动'][i],(s.count||0)>i?'OA{你的个人标记 · 演示位置}':'填写对应页面的 OA{…} 标记');btn(`${key}Link`,931,y+12,186,'打开页面 ↗',false);});field('reflection',269,520,848,'我想怎样参与社团（至少 8 个字）',s.reflection?'我想参与前端项目，准备先学习 Git 并完成页面练习。':'说明你感兴趣的方向或活动，以及准备做的第一步');btn('submit',749,609,368,'提交并自动核验 →');break;
    }
    case 'site-flag':{
      const pages={about:['关于我们','了解社团与新人成长路线',1],regulations:['规章制度','浏览公开的协作规范',2],activities:['社团活动','看看有哪些参与机会',3]};const[t,sub,n]=pages[s.page];url+=`/${s.page==='about'?'about':s.page==='regulations'?'regulations':'activities'}`;site();title(t,sub,107,249);area(null,107,328,984,278);txt(136,368,`✦ OPENATOM QUEST · ${n} / 03`,15,muted,600);txt(136,421,`读到这里，你找到了第 ${n} 枚探索标记。`,24,ink,600);txt(136,474,'OA{你的个人标记 · 演示位置}',28,ink,600);btn('copy',796,511,265,'复制我的标记',false);txt(136,579,'这枚标记属于你的 Quest 账号，完成探索后回到 Quest 提交。',15,muted);break;
    }
    case 'activity-list':site();title('活动日程','讲座、工作坊、项目冲刺和竞赛复盘都会在这里发布。',99,244);txt(99,351,'重点活动',23,ink,600);area('card',99,381,1010,206);body+=rect(118,399,218,169,'#e8e8ed');txt(153,477,'活动封面',27,muted,600);txt(153,516,'仅作演示',16,muted);txt(367,434,'一次技术分享活动（演示）',28,ink,600);txt(367,480,'活动安排、参与方式与资料说明',17,muted);txt(367,544,'官网报名',15,muted);txt(969,546,'查看详情 →',16);break;
    case 'activity-detail':{
      site();title('一次技术分享活动（演示）','名称、时间地点与报名字段仅用于讲解操作。',85,242);area('facts',85,306,1028,81);['活动时间','活动地点','报名方式'].forEach((t,i)=>{txt(107+i*327,338,t,14,muted);txt(107+i*327,369,['以活动详情为准','以实际通知为准','官网报名 / 无需报名'][i],17,ink,600);});txt(85,443,'活动介绍',22,ink,600);body+=lines(85,485,['仔细阅读活动安排与参与要求。','提前准备所需电脑、软件或资料。','无需报名的活动，按说明参与。'],17,38);area(null,685,412,428,233,'#fff');txt(707,448,'活动报名',21,ink,600);field('fields',707,481,382,'活动配置的必填字段（演示）',s.form?'按你的实际情况填写':'填写页面要求的信息');btn('submit',707,571,382,'提交报名 →');if(s.submitted){area('feedback',85,605,535,44,'#f0f7f1');txt(104,634,'✓ 报名已提交 · 继续关注后续通知',16,'#248a3d');}break;
    }
    case 'submission-form':{
      quest('我的任务');title(s.revision?'重新提交新版本':'提交成果','',269,162);txt(827,161,'自动保存草稿 ≠ 正式提交',12,muted);field('note',269,204,844,'完成情况说明 *','说明完成了什么，以及审核人如何验证结果',57);field(null,269,307,411,'仓库地址 / Pull Request 地址',s.links?'https://github.com/你的账号/项目':'按任务要求填写');field(null,704,307,409,'在线演示 / 视频地址',s.links?'提供可访问的成果地址':'未使用的项目可以留空');targets.links=[261,290,860,83];field('learning',269,397,844,'遇到的问题与学习总结',s.learning?'记录问题、解决过程和新的收获。':'填写你的实践过程与总结',50);add('aiCheck',check(269,488,'本次成果使用了 AI 工具',!!s.ai),[261,480,412,35]);if(s.ai)field('aiDetail',269,535,844,'AI 参与的具体内容 *',s.aiDetail?'说明工具、参与环节与自己的验证过程。':'如实说明使用情况');btn('submit',870,613,243,'提交审核 →');break;
    }
    case 'quest-status':{
      quest('我的任务');title(s.exploration?exploration:normal,'',269,176);
      if(step.focus==='passed'){area('passed',269,218,848,89,'#f1f7f2');txt(293,255,'✓ 已通过',24,'#248a3d',600);txt(293,286,'任务已完成，积分以当前系统记录为准。',16,muted);title('任务已经通过','继续前往成长路线查看下一步。',269,387);}
      else if(step.focus==='pending'){area('pending',269,218,848,89);txt(293,255,'待审核 · 当前版本 v1（演示）',24,ink,600);txt(293,286,'成果正在等待导师审核，请留意站内通知。',16,muted);title('提交成功，等待导师审核','草稿自动保存与正式提交是不同状态。',269,387);}
      else{area('feedback',269,216,848,109);txt(291,248,'需修改 · 导师最新反馈',20,ink,600);txt(291,282,'示例：请补充验证步骤，并检查成果链接是否可访问。',16,muted);txt(291,310,'反馈以当前导师给出的内容为准。',12,muted);txt(269,373,'提交历史',23,ink,600);area('history',269,399,848,178,'#fff');txt(290,439,'v2 · 等待审核（演示）',19,ink,600);txt(290,473,'最新版本已提交，之前的记录继续保留。',15,muted);txt(290,521,'v1 · 需修改（演示）',19);txt(290,555,'查看之前的审核结论与提交时间',14,muted);add('appeal',text(802,552,'对本次审核发起申诉',15,ink,600),[790,525,308,43]);}break;
    }
    case 'quest-notifications':quest('站内通知');title('站内通知','关注审核结果与任务相关消息。',269,182);area('notification',269,283,848,132);txt(292,324,'任务审核结果（演示）',22,ink,600);txt(292,366,'进入对应任务查看结论、反馈与后续处理方式。',17,muted);txt(1063,366,'→',25);break;
    case 'help-template':brand();title('怎样描述问题，更容易得到帮助','',118,247);area(null,118,292,964,332);body+=lines(145,341,['遇到问题的系统：主站 / 邮箱 / Quest','账号或学号：','发生时间：','页面地址或任务名称：','我进行的操作：','页面提示或实际结果：','已经尝试的方法：','截图：遮挡密码、验证码和其他人的个人信息'],16,34);btn('copy',877,309,177,'复制模板',false);break;
    default:throw new Error(`Unknown screen: ${step.screen}`);
  }
  const focus=targets[step.focus];if(!focus)throw new Error(`Missing focus: ${step.screen}/${step.focus} (${step.id})`);
  const[x,y,w,h]=focus;
  const highlight=`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="none" stroke="#0066cc" stroke-width="3"/><circle cx="${x+8}" cy="${y+8}" r="15" fill="#0066cc" stroke="#fff" stroke-width="2"/>${text(x+3,y+13,'→',15,'#fff',600)}<path d="m${x+w-23} ${y+h-12} 0 27 7-7 6 12 6-3-6-12 10-1Z" fill="#0066cc" stroke="#fff" stroke-width="2"/>`;
  return `<?xml version="1.0" encoding="UTF-8"?><svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800" role="img" aria-labelledby="title desc"><title id="title">步骤 ${step.number}：${escapeXml(step.title)}</title><desc id="desc">${escapeXml(step.body)} 完成后：${escapeXml(step.result)}。蓝框标出本步操作区域。本图为使用演示数据渲染的界面演示，非线上截图。</desc><rect width="1200" height="800" fill="#f5f5f7"/><g font-family="-apple-system,BlinkMacSystemFont,'PingFang SC','Microsoft YaHei',sans-serif">${text(31,30,`${articleNames[step.article]} / STEP ${String(step.number).padStart(2,'0')}`,12,muted,600)}${text(31,58,step.title,22,ink,600)}${text(961,41,'界面演示 · 非线上截图',12,muted)}${rect(30,77,1140,594)}${rect(31,78,1138,33,'#fafafa')}${text(51,100,url,12,muted)}${text(1021,100,'演示数据',11,muted)}<g clip-path="url(#screen)">${body}${highlight}</g>${text(33,711,'蓝色标注 = 本步操作位置',13,'#0066cc',600)}${text(33,747,'完成后',12,muted,600)}${text(111,747,step.result,16)}${text(33,783,'按自己的实际账号与页面操作；图中的示例账号、地址与标记不能用于真实提交。',12,muted)}</g><defs><clipPath id="screen"><rect x="31" y="111" width="1138" height="558"/></clipPath></defs></svg>`;
}
