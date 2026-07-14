import puppeteer from 'puppeteer';

const BASE = 'http://localhost:5173';
const results = [];

function log(r) { console.log(r); results.push(r); }
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function main() {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox','--disable-setuid-sandbox',
      '--enable-webgl','--use-gl=angle','--use-angle=swiftshader',
      '--enable-unsafe-swiftshader','--ignore-gpu-blocklist','--disable-gpu-sandbox'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  await page.evaluateOnNewDocument(() => {
    localStorage.setItem('mdt_token', 'demo-token');
    localStorage.setItem('mdt_user', 'doctor');
    localStorage.setItem('mdt_tenant', 'T001');
    localStorage.setItem('mdt_role', 'DOCTOR');
    localStorage.setItem('mdt_trace', 'm2m3-check');
  });

  // ═══ 1. 路由 ═══
  log('\n=== 1. 路由检查 ===');
  await page.goto(BASE, { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(2500);
  log(page.url().includes('/#/workbench') ? '✅ / 默认跳转到工作台' : '❌ 未跳到工作台: ' + page.url());

  await page.goto(BASE + '/#/data-ingestion', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(2000);
  log(page.url().includes('/#/patients') ? '✅ /data-ingestion → /patients 重定向正确' : '❌ 旧路由未重定向: ' + page.url());

  // ═══ 2. 侧边栏高亮 ═══
  log('\n=== 2. 侧边栏导航高亮 ===');

  async function checkActive(expectedLabel) {
    return page.evaluate((exp) => {
      const items = document.querySelectorAll('a.navitem');
      const active = [];
      items.forEach(el => {
        if (el.classList.contains('active')) {
          const b = el.querySelector('b');
          if (b) active.push(b.textContent.trim());
        }
      });
      return { ok: active.length === 1 && active[0] === exp, active };
    }, expectedLabel);
  }

  await page.goto(BASE + '/#/workbench', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(2500);
  var r = await checkActive('工作台');
  log(r.ok ? '✅ /workbench → "工作台"高亮' : '❌ 高亮不正确: ' + JSON.stringify(r.active));

  await page.goto(BASE + '/#/patients', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(2500);
  r = await checkActive('患者管理');
  log(r.ok ? '✅ /patients → "患者管理"高亮' : '❌ 高亮不正确: ' + JSON.stringify(r.active));

  await page.goto(BASE + '/#/imaging?studyUid=PV-T001-P1001-AccCT001&patientId=P1001&accessionNumber=AccCT001&patientVisitUid=PV-T001-P1001-AccCT001&modality=CT',
    { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(4000);
  r = await checkActive('影像中心');
  log(r.ok ? '✅ /imaging → "影像中心"高亮' : '❌ 高亮不正确: ' + JSON.stringify(r.active));

  // ═══ 3. 页面标题 ═══
  log('\n=== 3. 页面标题与面包屑 ===');

  async function checkPage(title, crumb) {
    return page.evaluate(([t, c]) => {
      const h2 = (document.querySelector('.top h2') || {}).textContent || '';
      const p = (document.querySelector('.crumb') || {}).textContent || '';
      return { title: h2.trim(), crumb: p.trim(), tOk: h2.trim() === t, cOk: p.trim() === c };
    }, [title, crumb]);
  }

  await page.goto(BASE + '/#/workbench', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(2000);
  var pt = await checkPage('工作台', '今日概览 · 待办事项 · 快捷入口');
  log(pt.tOk ? '✅ 工作台标题: "' + pt.title + '"' : '❌ 标题不符: "' + pt.title + '"');

  await page.goto(BASE + '/#/patients', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(2500);
  pt = await checkPage('患者管理', '患者检索 · 检查记录 · 临床数据');
  log(pt.tOk ? '✅ 患者管理标题: "' + pt.title + '"' : '❌ 标题不符: "' + pt.title + '"');

  await page.goto(BASE + '/#/imaging?studyUid=PV-T001-P1001-AccCT001&patientId=P1001&accessionNumber=AccCT001&patientVisitUid=PV-T001-P1001-AccCT001&modality=CT',
    { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(4000);
  pt = await checkPage('影像中心 · MPR 阅片', 'PV-T001-P1001-AccCT001 | P1001 / AccCT001');
  log(pt.tOk ? '✅ 影像标题: "' + pt.title + '"' : '❌ 标题不符: "' + pt.title + '"');
  log(pt.cOk ? '✅ 面包屑正确' : '❌ 面包屑不符: "' + pt.crumb + '"');

  // ═══ 4. M2 患者管理功能 ═══
  log('\n=== 4. M2 患者管理功能 ===');
  await page.goto(BASE + '/#/patients', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(3500);

  var m2 = await page.evaluate(() => {
    const rows = document.querySelectorAll('tbody tr');
    const btns = document.querySelectorAll('.card .btn');
    return {
      rowCount: rows.length,
      btnTexts: Array.from(btns).map(b => b.textContent.trim()),
      opsTitle: (document.querySelector('.card h3') || {}).textContent || '',
    };
  });
  log(m2.rowCount > 0 ? '✅ 患者列表 ' + m2.rowCount + ' 条' : '⚠️ 列表为空');
  log(m2.opsTitle.includes('模拟接入') ? '✅ SCU/SCP 模拟按钮存在' : '⚠️ 模拟接入区未找到');

  var links = await page.evaluate(() => {
    const a = document.querySelector('tbody a[href*="imaging"]');
    return a ? { text: a.textContent.trim(), href: a.getAttribute('href') } : null;
  });
  log(links && links.text === '阅片' ? '✅ 阅片链接文本正确' : '❌ 阅片链接异常');
  log(links && links.href.includes('imaging?') ? '✅ 阅片链接指向 /imaging?studyUid=...' : '❌ 路径: ' + (links ? links.href : 'null'));

  // ═══ 5. M3 影像中心 ═══
  log('\n=== 5. M3 影像中心功能 ===');
  await page.goto(BASE + '/#/imaging?studyUid=PV-T001-P1001-AccCT001&patientId=P1001&accessionNumber=AccCT001&patientVisitUid=PV-T001-P1001-AccCT001&modality=CT',
    { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(5000);

  var m3 = await page.evaluate(() => {
    const cs = document.querySelectorAll('canvas.mpr-canvas');
    const hasLoading = !!document.querySelector('.status');
    const wlBtns = Array.from(document.querySelectorAll('button')).filter(b =>
      ['腹部','肺部','骨窗','脑部'].includes(b.textContent.trim()));
    const sliders = document.querySelectorAll('input[type="range"]');
    return {
      canvasCount: cs.length,
      sizes: Array.from(cs).map(c => c.width + 'x' + c.height),
      hasLoading, wlCount: wlBtns.length, sliderCount: sliders.length,
    };
  });
  log(m3.canvasCount === 3 ? '✅ 3 MPR canvas (' + m3.sizes.join(', ') + ')' : '❌ canvas 数: ' + m3.canvasCount);
  log(!m3.hasLoading ? '✅ 体数据已加载' : '⚠️ 仍在加载');
  log(m3.wlCount >= 4 ? '✅ WL 预设 ' + m3.wlCount + ' 个' : '⚠️ WL: ' + m3.wlCount);
  log(m3.sliderCount >= 3 ? '✅ 切片滑块 ' + m3.sliderCount + ' 个' : '⚠️ 滑块: ' + m3.sliderCount);

  // 像素诊断
  var px = await page.evaluate(() => {
    const results = [];
    document.querySelectorAll('canvas.mpr-canvas').forEach((c, i) => {
      const gl = c.getContext('webgl2');
      if (!gl) { results.push({ i, err: 'no webgl2' }); return; }
      const buf = new Uint8Array(c.width * c.height * 4);
      gl.readPixels(0, 0, c.width, c.height, gl.RGBA, gl.UNSIGNED_BYTE, buf);
      const rVals = [];
      for (let j = 0; j < buf.length; j += 4) rVals.push(buf[j]);
      let min = 255, max = 0, sum = 0;
      rVals.forEach(v => { if (v < min) min = v; if (v > max) max = v; sum += v; });
      const mean = sum / rVals.length;
      const variance = rVals.reduce((s, v) => s + (v - mean) ** 2, 0) / rVals.length;
      results.push({ i, min, max, mean: +mean.toFixed(1), std: +Math.sqrt(variance).toFixed(1) });
    });
    return results;
  });
  px.forEach(p => {
    if (p.err) log('❌ canvas#' + p.i + ': ' + p.err);
    else if (p.std > 30) log('✅ canvas#' + p.i + ': std=' + p.std + ' mean=' + p.mean + ' — 真实结构');
    else if (p.std > 5) log('⚠️ canvas#' + p.i + ': std=' + p.std + ' — 较弱');
    else log('❌ canvas#' + p.i + ': std=' + p.std + ' — 疑似灰图回归!');
  });

  // ═══ 6. 跨页跳转 ═══
  log('\n=== 6. M2→M3 跨页跳转 ===');
  await page.goto(BASE + '/#/patients', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(3000);
  await page.evaluate(() => {
    const a = document.querySelector('tbody a[href*="imaging"]');
    if (a) a.click();
  });
  await sleep(3500);
  const jumped = page.url().includes('/imaging');
  log(jumped ? '✅ 点击"阅片"→成功跳转影像中心' : '❌ 跳转失败: ' + page.url());
  if (jumped) {
    const count = await page.evaluate(() => document.querySelectorAll('canvas.mpr-canvas').length);
    log(count === 3 ? '✅ 跳转后 MPR canvas 已渲染' : '⚠️ canvas 数: ' + count);
  }

  // ═══ 7. 无参数保护 ═══
  log('\n=== 7. 无参数 /imaging 保护 ===');
  await page.goto(BASE + '/#/imaging', { waitUntil: 'networkidle0', timeout: 15000 });
  await sleep(3000);
  log(page.url().includes('/#/patients') ? '✅ 无 studyUid → 自动跳回患者管理' : '❌ 未跳回: ' + page.url());

  // ═══ 汇总 ═══
  let pass = 0, warn = 0, fail = 0;
  results.forEach(l => { if (l.startsWith('✅')) pass++; else if (l.startsWith('⚠️')) warn++; else if (l.startsWith('❌')) fail++; });
  log('\n═══════════════════════════════');
  log('自检完成: ✅' + pass + ' 通过  ⚠️' + warn + ' 提示  ❌' + fail + ' 失败  (共' + results.length + '项)');
  log('═══════════════════════════════');

  await page.screenshot({ path: '/workspace/docs/m2m3-selfcheck.png' });
  await browser.close();
  return { pass, warn, fail };
}

main().then(r => { if (r.fail > 0) process.exit(1); }).catch(e => { console.error('异常:', e); process.exit(1); });
