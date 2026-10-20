import { articles, checklist, homeSections, links, illustrationStats } from './content.js';

const paths = {
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4 4"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"/>',
  moon: '<path d="M20.5 13A8.5 8.5 0 0 1 11 3.5 8.5 8.5 0 1 0 20.5 13Z"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  upRight: '<path d="M6 18 18 6M6 6h12v12"/>',
  home: '<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z"/>',
  key: '<circle cx="8" cy="8" r="5"/><path d="m11.5 11.5 9 9m-3-3 3-3m-6 0 3-3"/>',
  mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',
  compass: '<circle cx="12" cy="12" r="9"/><path d="m16 8-3 5-5 3 3-5Z"/>',
  flag: '<path d="M5 21V4m0 0c5-5 9 5 14 0v10c-5 5-9-5-14 0"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 11h18m-13 4h2m4 0h2"/>',
  upload: '<path d="M12 16V3m-5 5 5-5 5 5M4 15v5a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-5"/>',
  help: '<circle cx="12" cy="12" r="9"/><path d="M9.5 8.5a2.5 2.5 0 0 1 5 0c0 2-2.5 2-2.5 4M12 16v.5"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  book: '<path d="M12 5C8 2 4 3 3 4v16c3-2 6-1 9 1 3-2 6-3 9-1V4c-1-1-5-2-9 1Zm0 0v16"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
};
const icon = (name) => `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.book}</svg>`;
const escapeHtml = (text) => String(text).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const textOnly = (html) => new DOMParser().parseFromString(html, 'text/html').body.textContent.replace(/\s+/g, ' ').trim();
const read = (key, fallback) => { try { return JSON.parse(localStorage.getItem(key)) ?? fallback; } catch { return fallback; } };
const write = (key, value) => { try { localStorage.setItem(key, JSON.stringify(value)); return true; } catch { return false; } };
let completed = read('oa-guide-progress', []);
if (!Array.isArray(completed)) completed = [];
completed = completed.filter(id => checklist.some(item => item.id === id));
let currentId = '';
let observer;
let toastTimer;
let opener;
const $ = (selector) => document.querySelector(selector);
const main = $('#main-content');
const mobile = matchMedia('(max-width: 900px)');
if ('scrollRestoration' in history) history.scrollRestoration = 'manual';

function hydrateIcons(root = document) {
  root.querySelectorAll('[data-icon]').forEach(el => { el.innerHTML = icon(el.dataset.icon); });
}

function sidebar() {
  const groups = [...new Set(articles.map(a => a.group))];
  $('#sidebar').innerHTML = `<a href="#/" class="sidebar-home ${currentId === 'home' ? 'active' : ''}" ${currentId === 'home' ? 'aria-current="page"' : ''}>${icon('home')}<span>欢迎，新同学</span></a>${groups.map(group => `<div class="nav-group"><p>${group}</p>${articles.filter(a => a.group === group).map(a => `<a href="#/${a.id}" class="nav-link ${currentId === a.id ? 'active' : ''}" ${currentId === a.id ? 'aria-current="page"' : ''}>${icon(a.icon)}<span>${a.short}</span>${a.id === 'exploration' ? '<small>新成员必读</small>' : ''}</a>`).join('')}</div>`).join('')}<div class="sidebar-bottom"><span class="eyebrow">YOUR FIRST STEPS</span><div class="sidebar-progress-label"><strong>新成员上手进度</strong><span>${completed.length} / ${checklist.length}</span></div><div class="progress-track"><span style="width:${completed.length / checklist.length * 100}%"></span></div><p>在首页勾选已完成的步骤</p><a href="${links.main}" target="_blank" rel="noopener noreferrer">返回社团主站 ${icon('upRight')}</a></div>`;
  syncMobile();
}

function toc(sections) {
  return `<aside class="toc" aria-label="本页目录"><p>本页内容</p><nav>${sections.map((s, i) => `<a href="#/${currentId}/${s.id}" class="${i === 0 ? 'active' : ''}" data-toc="${s.id}">${s.title}</a>`).join('')}</nav><div class="toc-help">${icon('help')}<strong>遇到问题了？</strong><p>看看常见问题，或联系社团管理员。</p><a href="#/faq">获取帮助 <span aria-hidden="true">↗</span></a></div></aside>`;
}

function home() {
  const cards = articles.slice(0, 3);
  return `<div class="home-layout"><div class="home-content"><div class="breadcrumb">成员指南 <span>/</span> 欢迎加入</div><section class="welcome-hero"><p class="eyebrow"><span class="status-dot"></span> THE OPEN SOURCE JOURNEY</p><h1>你的开源之旅，<br>从这里开始<span class="title-period">。</span></h1><p class="hero-description">欢迎加入开放原子开源社团。<br>激活账号、连接伙伴，在真实的任务中开启成长。</p><div class="hero-actions"><a class="button button-primary" href="#/activation">开始激活账号 ${icon('arrow')}</a><a class="button button-text" href="#/quest">先了解 Quest ${icon('upRight')}</a></div><div class="hero-meta"><span>${icon('book')} ${illustrationStats.steps} 张逐步操作图</span><span>${icon('compass')} 新成员推荐阅读</span></div></section><section class="journey-section" id="journey"><div class="section-label"><span>01 / GET STARTED</span><span>从一个账号，连接整个社团</span></div><div class="journey-map" aria-label="新成员路线：激活账号，开通邮箱，进入 Quest"><a href="#/activation"><span class="journey-icon">${icon('key')}</span><small>STEP 01</small><strong>激活账号</strong><span>确认身份 · 连接社群</span></a><span class="journey-connector" aria-hidden="true">${icon('arrow')}</span><a href="#/mail"><span class="journey-icon">${icon('mail')}</span><small>STEP 02</small><strong>开通邮箱</strong><span>接收通知 · 开始沟通</span></a><span class="journey-connector" aria-hidden="true">${icon('arrow')}</span><a href="#/quest"><span class="journey-icon">${icon('compass')}</span><small>STEP 03</small><strong>进入 Quest</strong><span>探索方向 · 完成任务</span></a></div></section><section class="guides-section" id="guides"><div class="section-heading"><div><p class="eyebrow">THE ESSENTIALS</p><h2>每一步，都有指南。</h2></div><span>按顺序开始，也可以按需查阅</span></div><div class="guide-cards">${cards.map(a => `<a href="#/${a.id}" class="guide-card"><div class="guide-card-top">${icon(a.icon)}<span>${a.number}</span></div><h3>${a.short}</h3><p>${a.description}</p><span class="card-bottom">阅读指南 ${icon('arrow')}</span></a>`).join('')}</div><div class="secondary-guides">${articles.slice(4, 6).map(a => `<a href="#/${a.id}">${icon(a.icon)}<span>${a.short}</span>${icon('upRight')}</a>`).join('')}</div></section><section class="first-task" id="first-task"><div class="first-task-copy"><span class="eyebrow">YOUR FIRST QUEST <span class="task-chip">L0 新人报到</span></span><h2>先认识社团，<br>再找到你的方向。</h2><p>探索主站的三个页面，了解社团与协作规则，<br class="desktop-br">写下你的参与计划，完成第一项新人任务。</p><a href="#/exploration" class="button button-primary">认识社团任务指南 ${icon('arrow')}</a></div><div class="exploration-art" aria-label="探索关于我们、规章制度和社团活动三个页面"><div class="art-label">EXPLORE / DISCOVER / BELONG</div>${['关于我们', '规章制度', '社团活动'].map((t, i) => `<div class="exploration-art-row"><span>0${i + 1}</span><strong>${t}</strong>${icon('flag')}</div>`).join('')}<div class="art-result">三枚个人标记 <span>→</span> 你的第一步</div></div></section><section class="checklist-panel"><div><p class="eyebrow">A LITTLE PROGRESS, EVERY DAY</p><h2>准备好了，就打个勾。</h2><p>勾选你实际完成的步骤，记录只保存在当前浏览器。</p></div><div class="checklist-items">${checklist.map(item => `<div class="checklist-item"><label><input type="checkbox" data-check="${item.id}" ${completed.includes(item.id) ? 'checked' : ''}><span>${item.label}</span></label><a href="#/${item.article}" aria-label="阅读${item.label}指南">${icon('upRight')}</a></div>`).join('')}<span class="checklist-count" aria-live="polite">已完成 ${completed.length} / ${checklist.length} 步</span></div></section><div class="home-help"><span>${icon('help')} 没找到答案？</span><a href="#/faq">查看常见问题与求助方式 ${icon('arrow')}</a></div></div>${toc(homeSections)}</div>`;
}

function articlePage(article) {
  const index = articles.indexOf(article);
  const previous = articles[index - 1];
  const next = articles[index + 1];
  return `<div class="document-layout"><article class="document"><div class="breadcrumb"><a href="#/">成员指南</a><span>/</span>${article.group}<span>/</span>${article.short}</div><header class="document-header"><p class="eyebrow">GUIDE ${article.number} <span> / ${article.group}</span></p><h1>${article.title}</h1><p class="document-description">${article.description}</p><div class="document-meta"><span>${icon('clock')} ${article.time}</span><span class="document-operation-count">${illustrationStats.articles[article.id]} 步操作 · 每步配图</span><span>内容核对于 2026.10.20</span><button id="print-page">打印本页 ↗</button></div></header><div class="document-body">${article.sections.map(s => `<section id="${s.id}" class="doc-section"><h2>${s.title}<a class="heading-anchor" href="#/${article.id}/${s.id}" aria-label="链接到${s.title}">#</a></h2>${s.html}</section>`).join('')}</div><div class="document-end"><span>${icon('check')} 你已读到本篇末尾</span><a href="#/faq/ask-for-help">操作遇到问题？</a></div><nav class="article-pagination" aria-label="上一篇与下一篇">${previous ? `<a href="#/${previous.id}"><small>← 上一篇</small><strong>${previous.short}</strong></a>` : '<a href="#/"><small>← 返回</small><strong>指南首页</strong></a>'}${next ? `<a href="#/${next.id}"><small>下一篇 →</small><strong>${next.short}</strong></a>` : '<a href="#/"><small>接下来 →</small><strong>返回指南首页</strong></a>'}</nav></article>${toc(article.sections)}</div>`;
}

function missing() {
  return `<section class="not-found"><p class="eyebrow">PAGE NOT FOUND</p><h1>这一页还没有收录。</h1><p>可以从文档目录重新选择，或搜索你需要的内容。</p><a class="button button-primary" href="#/">返回指南首页 ${icon('arrow')}</a></section>`;
}

function route() {
  let segments;
  try { segments = decodeURIComponent(location.hash.slice(1)).split('/').filter(Boolean); } catch { segments = []; }
  const id = segments[0] || 'home';
  const sectionId = segments[1];
  const samePage = id === currentId;
  if (!samePage) {
    currentId = id;
    const article = articles.find(a => a.id === id);
    main.innerHTML = id === 'home' ? home() : article ? articlePage(article) : missing();
    document.title = `${article?.title || (id === 'home' ? '欢迎，新同学' : '页面未找到')} · 开放原子成员指南`;
    sidebar();
    hydrateIcons();
    observeSections();
    window.scrollTo({ top: 0, behavior: 'instant' });
    main.focus({ preventScroll: true });
  }
  closeMenu(false);
  if (sectionId) requestAnimationFrame(() => {
    const target = document.getElementById(sectionId);
    if (target?.closest('main')) {
      target.scrollIntoView({ behavior: samePage && !matchMedia('(prefers-reduced-motion: reduce)').matches ? 'smooth' : 'instant' });
      target.setAttribute('tabindex', '-1');
      target.focus({ preventScroll: true });
    }
  });
}

function observeSections() {
  observer?.disconnect();
  const targets = main.querySelectorAll('.doc-section, .journey-section, .guides-section, .first-task');
  observer = new IntersectionObserver(entries => {
    const visible = entries.filter(e => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
    if (visible) document.querySelectorAll('[data-toc]').forEach(a => {
      a.classList.toggle('active', a.dataset.toc === visible.target.id);
      if (a.dataset.toc === visible.target.id) a.setAttribute('aria-current', 'location'); else a.removeAttribute('aria-current');
    });
  }, { rootMargin: '-100px 0px -45% 0px', threshold: 0 });
  targets.forEach(el => observer.observe(el));
}

function syncMobile() {
  const isOpen = document.body.classList.contains('menu-open');
  $('#sidebar').inert = mobile.matches && !isOpen;
  $('#menu-toggle').setAttribute('aria-expanded', String(isOpen));
}
function closeMenu(restore = true) {
  const wasOpen = document.body.classList.contains('menu-open');
  document.body.classList.remove('menu-open');
  $('#sidebar-overlay').hidden = true;
  syncMobile();
  if (wasOpen && restore) $('#menu-toggle').focus();
}
$('#menu-toggle').addEventListener('click', () => {
  if (document.body.classList.contains('menu-open')) return closeMenu();
  document.body.classList.add('menu-open');
  $('#sidebar-overlay').hidden = false;
  syncMobile();
  $('#sidebar a').focus();
});
$('#sidebar-overlay').addEventListener('click', () => closeMenu());
mobile.addEventListener('change', () => closeMenu(false));

function syncTheme() {
  const dark = document.documentElement.dataset.theme === 'dark';
  $('#theme-toggle').innerHTML = icon(dark ? 'sun' : 'moon');
  $('#theme-toggle').setAttribute('aria-label', `切换${dark ? '浅色' : '深色'}模式`);
}
$('#theme-toggle').addEventListener('click', () => {
  const theme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = theme;
  try { localStorage.setItem('oa-guide-theme', theme); } catch {}
  syncTheme();
});

const searchIndex = articles.flatMap(a => a.sections.map(s => ({ article: a, section: s, text: textOnly(s.html) })));
function search(query = '') {
  const tokens = query.trim().toLocaleLowerCase().split(/\s+/).filter(Boolean);
  const results = tokens.length ? searchIndex.map(item => {
    const title = `${item.article.title} ${item.section.title} ${item.article.keywords}`.toLocaleLowerCase();
    const corpus = `${title} ${item.text.toLocaleLowerCase()}`;
    return { ...item, score: tokens.every(t => corpus.includes(t)) ? tokens.reduce((sum, t) => sum + (title.includes(t) ? 10 : 1), 0) : 0 };
  }).filter(item => item.score).sort((a, b) => b.score - a.score).slice(0, 12) : articles.slice(0, 6).map(a => ({ article: a, section: a.sections[0], text: a.description }));
  $('#search-results').innerHTML = `<p class="search-label">${tokens.length ? `找到 ${results.length} 条相关内容` : '推荐阅读'}</p>${results.length ? results.map(({ article: a, section: s, text }) => {
    const matchAt = Math.max(0, text.toLocaleLowerCase().indexOf(tokens[0] || '') - 20);
    const excerpt = text.slice(matchAt, matchAt + 88);
    return `<a class="search-result" href="#/${a.id}/${s.id}">${icon(a.icon)}<div><strong>${escapeHtml(tokens.length ? s.title : a.title)}</strong><small>${escapeHtml(a.short)} · ${escapeHtml(excerpt)}${text.length > matchAt + 88 ? '…' : ''}</small></div>${icon('arrow')}</a>`;
  }).join('') : '<div class="search-empty"><strong>还没有找到相关内容</strong><p>试试“入群”“邮箱地址”“个人标记”或“提交审核”。</p><a href="#/faq">查看常见问题 →</a></div>'}`;
}
function openSearch() {
  closeMenu(false);
  opener = document.activeElement;
  const dialog = $('#search-dialog');
  if (dialog.open) return;
  dialog.showModal();
  $('#search-input').value = '';
  search();
  $('#search-input').focus();
}
function closeSearch() { $('#search-dialog').close(); }
$('#search-trigger').addEventListener('click', openSearch);
$('#search-close').addEventListener('click', closeSearch);
$('#search-input').addEventListener('input', e => search(e.target.value));
$('#search-dialog').addEventListener('close', () => opener?.focus());
$('#search-results').addEventListener('click', e => { if (e.target.closest('a')) closeSearch(); });
$('#search-input').addEventListener('keydown', e => { if (e.key === 'Enter') $('#search-results a')?.click(); });
document.querySelectorAll('dialog').forEach(dialog => dialog.addEventListener('click', e => {
  const bounds = dialog.getBoundingClientRect();
  if (e.target === dialog && (e.clientX < bounds.left || e.clientX > bounds.right || e.clientY < bounds.top || e.clientY > bounds.bottom)) dialog.close();
}));

document.addEventListener('keydown', e => {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); openSearch(); }
  if (e.key === 'Escape') closeMenu();
  if (e.key === 'Tab' && document.body.classList.contains('menu-open') && mobile.matches) {
    const items = [$('#menu-toggle'), ...$('#sidebar').querySelectorAll('a')];
    const current = items.indexOf(document.activeElement);
    if (e.shiftKey && current === 0) { e.preventDefault(); items.at(-1).focus(); }
    if (!e.shiftKey && current === items.length - 1) { e.preventDefault(); items[0].focus(); }
  }
});
document.addEventListener('change', e => {
  if (!e.target.matches('[data-check]')) return;
  completed = completed.filter(id => id !== e.target.dataset.check);
  if (e.target.checked) completed.push(e.target.dataset.check);
  const saved = write('oa-guide-progress', completed);
  sidebar();
  $('.checklist-count').textContent = `已完成 ${completed.length} / ${checklist.length} 步`;
  if (!saved) toast('当前浏览器无法保存进度，本次勾选仍然有效。');
});
function toast(message) {
  clearTimeout(toastTimer);
  $('#toast').hidden = false;
  $('#toast').textContent = message;
  toastTimer = setTimeout(() => { $('#toast').hidden = true; }, 3500);
}
document.addEventListener('click', async e => {
  const routeLink = e.target.closest('a[href^="#/"]');
  if (routeLink && !e.metaKey && !e.ctrlKey && !e.shiftKey && !e.altKey) {
    e.preventDefault();
    const hash = routeLink.getAttribute('href');
    if (location.hash === hash) route();
    else location.hash = hash;
  }
  if (e.target.closest('a[href="#main-content"]')) {
    e.preventDefault();
    main.focus({ preventScroll: true });
    main.scrollIntoView({ behavior: 'instant' });
  }
  if (e.target.closest('#print-page')) window.print();
  const figureButton = e.target.closest('[data-figure]');
  if (figureButton) {
    const image = figureButton.querySelector('img');
    $('#figure-image').src = image.src;
    $('#figure-image').alt = image.alt;
    $('#figure-new-tab').href = image.src;
    $('#figure-dialog').classList.remove('original-size');
    $('#figure-size').setAttribute('aria-pressed', 'false');
    $('#figure-size').textContent = '原尺寸查看';
    $('#figure-caption').textContent = figureButton.closest('figure').querySelector('figcaption').textContent;
    $('#figure-dialog').showModal();
  }
  if (e.target.closest('[data-copy-help]')) {
    try {
      await navigator.clipboard.writeText($('#help-template').textContent);
      toast('求助模板已复制，填写后发送给相应负责人。');
    } catch {
      const selection = getSelection();
      const range = document.createRange();
      range.selectNodeContents($('#help-template'));
      selection.removeAllRanges(); selection.addRange(range);
      toast('已选中模板，请手动复制。');
    }
  }
});
$('#figure-close').addEventListener('click', () => $('#figure-dialog').close());
$('#figure-size').addEventListener('click', () => {
  const original = $('#figure-dialog').classList.toggle('original-size');
  $('#figure-size').setAttribute('aria-pressed', String(original));
  $('#figure-size').textContent = original ? '适应窗口' : '原尺寸查看';
});
window.addEventListener('hashchange', route);
hydrateIcons();
syncTheme();
route();
