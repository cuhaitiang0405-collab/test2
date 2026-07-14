/** UI 菜单分级 — 完整验证（含时序修复） */
import puppeteer from 'puppeteer'

const BASE = 'http://localhost:5173'

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

async function check(role) {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox','--disable-setuid-sandbox'],
  })
  const page = await browser.newPage()
  await page.setViewport({ width: 1440, height: 900 })

  await page.evaluateOnNewDocument((r) => {
    localStorage.setItem('mdt_token', 'demo-token')
    localStorage.setItem('mdt_user', 'doctor')
    localStorage.setItem('mdt_tenant', 'T001')
    localStorage.setItem('mdt_role', r)
    localStorage.setItem('mdt_trace', 'ui-verify')
  }, role)

  await page.goto(BASE + '/#/workbench', { waitUntil: 'networkidle0', timeout: 15000 })
  await sleep(2500)

  const nav = await page.evaluate(() => ({
    url: location.href,
    groups: Array.from(document.querySelectorAll('.nav-sep')).map(s => s.textContent?.trim()),
    items: Array.from(document.querySelectorAll('.navitem')).map(el => ({
      label: el.querySelector('b')?.textContent?.trim() || '',
      desc: el.querySelector('small')?.textContent?.trim() || '',
      href: el.getAttribute('href') || '',
      badge: el.querySelector('.chip')?.textContent?.trim() || '',
      active: el.classList.contains('active'),
      upcoming: el.classList.contains('upcoming'),
    })),
    workbenchTitle: document.querySelector('.top h2')?.textContent?.trim(),
    statCards: Array.from(document.querySelectorAll('.stat-num')).map(s => s.textContent?.trim()),
  }))

  console.log(`\n=== ${role} 角色 ===`)
  console.log('URL:', nav.url)
  console.log('页面标题:', nav.workbenchTitle)
  console.log('统计卡片:', nav.statCards)
  console.log('菜单分组:', nav.groups)
  console.log('菜单项:')
  for (const i of nav.items) {
    const flags = []
    if (i.active) flags.push('当前')
    if (i.upcoming) flags.push('未开放')
    if (i.badge) flags.push(i.badge)
    console.log(`  ${i.label} → ${i.desc}  ${flags.length ? '[' + flags.join(', ') + ']' : ''}`)
  }

  return { page, browser, nav }
}

async function main() {
  // — 1. 医生视角 —
  const doc = await check('DOCTOR')
  await doc.page.screenshot({ path: '/workspace/docs/ui-menu-workbench.png' })
  console.log('  → 截图: ui-menu-workbench.png')

  // 导航到患者管理
  await doc.page.evaluate(() => {
    const links = Array.from(document.querySelectorAll('a.navitem'))
    const l = links.find(a => a.textContent?.includes('患者管理'))
    if (l) l.click()
  })
  await sleep(1500)
  const patientTitle = await doc.page.evaluate(() => document.querySelector('.top h2')?.textContent?.trim())
  console.log('  患者管理页标题:', patientTitle)
  await doc.page.screenshot({ path: '/workspace/docs/ui-menu-patients.png' })
  console.log('  → 截图: ui-menu-patients.png')

  await doc.browser.close()

  // — 2. 管理员视角 —
  const adm = await check('ADMIN')
  await adm.page.screenshot({ path: '/workspace/docs/ui-menu-admin.png' })
  console.log('  → 截图: ui-menu-admin.png')

  // 导航到系统管理
  await adm.page.evaluate(() => {
    const links = Array.from(document.querySelectorAll('a.navitem'))
    const l = links.find(a => a.textContent?.includes('数据与权限'))
    if (l) l.click()
  })
  await sleep(1500)
  const adminTitle = await adm.page.evaluate(() => document.querySelector('.top h2')?.textContent?.trim())
  console.log('  系统管理页标题:', adminTitle)
  await adm.page.screenshot({ path: '/workspace/docs/ui-menu-admin-detail.png' })
  console.log('  → 截图: ui-menu-admin-detail.png')

  await adm.browser.close()

  console.log('\n✅ UI 菜单分级验证完成')
}

main().catch(e => { console.error('❌', e); process.exit(1) })
