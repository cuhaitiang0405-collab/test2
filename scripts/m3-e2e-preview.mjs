/** M3 端到端预览脚本 — 使用 page.evaluate 交互 */
import puppeteer from 'puppeteer'

const BASE = 'http://localhost:5173'

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

async function main() {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      // 启用 WebGL2 软件渲染（headless Chrome 需显式开启）
      '--enable-webgl',
      '--use-gl=angle',
      '--use-angle=swiftshader',
      '--enable-unsafe-swiftshader',
      '--enable-features=Vulkan,UseSkiaRenderer',
      '--ignore-gpu-blocklist',
      '--disable-gpu-sandbox',
    ],
  })
  const page = await browser.newPage()
  await page.setViewport({ width: 1440, height: 900 })

  // 收集浏览器控制台消息
  const browserLogs = []
  page.on('console', msg => browserLogs.push(`[${msg.type()}] ${msg.text()}`))
  page.on('pageerror', err => browserLogs.push(`[pageerror] ${err.message}`))

  // ── 1. 登录 ──
  console.log('1. 登录...')
  await page.goto(BASE + '/#/login', { waitUntil: 'networkidle0' })
  await sleep(1000)

  // 用 DOM API 直接填入并提交
  const loggedIn = await page.evaluate(async () => {
    const usernameInput = document.querySelector('input[placeholder="doctor"]')
    const passwordInput = document.querySelector('input[placeholder*="••••"]')
    if (!usernameInput || !passwordInput) return false

    // 设置值并触发 input 事件（Vue v-model 需要）
    const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype, 'value').set
    nativeInputValueSetter.call(usernameInput, 'doctor')
    usernameInput.dispatchEvent(new Event('input', { bubbles: true }))
    nativeInputValueSetter.call(passwordInput, 'doctor123')
    passwordInput.dispatchEvent(new Event('input', { bubbles: true }))

    // 提交表单
    const form = usernameInput.closest('form')
    if (form) {
      form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }))
      return true
    }
    return false
  })
  console.log('   登录提交:', loggedIn ? '已触发' : '失败')
  await sleep(3000)

  const url1 = page.url()
  console.log('   当前 URL:', url1)

  // if still on login, init localStorage + reload
  if (url1.includes('/login')) {
    console.log('   登录未跳转，用 localStorage 方式...')
    await page.evaluate(() => {
      localStorage.setItem('mdt_token', 'demo-token')
      localStorage.setItem('mdt_user', 'doctor')
      localStorage.setItem('mdt_tenant', 'T001')
      localStorage.setItem('mdt_role', 'DOCTOR')
      localStorage.setItem('mdt_trace', 'trace-demo')
    })
    // 重新加载以使 Vue 从 localStorage 初始化 auth state
    await page.goto(BASE + '/#/data-ingestion', { waitUntil: 'networkidle0' })
    await sleep(1000)
    console.log('   当前 URL:', page.url())
  }

  // ── 2. 模拟 SCU 拉取 ──
  console.log('2. 模拟 SCU 拉取...')
  await page.evaluate(async () => {
    const btns = Array.from(document.querySelectorAll('button'))
    const scu = btns.find(b => b.textContent?.includes('SCU'))
    if (scu) scu.click()
  })
  await sleep(3000)

  // ── 3. 点击 "阅片" 进入影像引擎 ──
  console.log('3. 进入阅片...')
  let navTo = ''
  await page.evaluate(() => {
    const links = Array.from(document.querySelectorAll('a'))
    const readLink = links.find(a => a.textContent?.includes('阅片'))
    if (readLink) readLink.click()
  })
  await sleep(1000)

  const url2 = page.url()
  console.log('   导航后 URL:', url2)
  if (!url2.includes('/imaging')) {
    console.log('   手动导航到/imaging...')
    navTo = '/#/imaging?studyUid=PV-T001-P1001-AccCT001' +
      '&patientId=P1001&accessionNumber=AccCT001' +
      '&patientVisitUid=PV-T001-P1001-AccCT001&modality=CT'
    await page.goto(BASE + navTo, { waitUntil: 'networkidle0' })
  }

  // 等待体积数据加载与 WebGL 初始化
  console.log('   等待体数据加载与 WebGL 渲染...')
  await sleep(5000)

  // ── 5. 调试：检查 WebGL 状态与 canvas 像素 ──
  console.log('5. 调试 WebGL 状态...')
  const debug = await page.evaluate(() => {
    const canvases = Array.from(document.querySelectorAll('canvas'))
    return canvases.map(c => ({
      className: c.className,
      clientWidth: c.clientWidth,
      clientHeight: c.clientHeight,
      bufferWidth: c.width,
      bufferHeight: c.height,
      hasContext: !!(c.getContext('webgl2') || c.getContext('webgl'))
    }))
  })
  console.log('   Canvas 状态:', JSON.stringify(debug, null, 2))

  // 拖动 axial 切片到 30%（应该穿过软组织中心）+ 应用肺部预设
  console.log('6. 调整切片 + 应用肺部预设以提高对比度...')
  await page.evaluate(() => {
    const sliders = document.querySelectorAll('.view .slider')
    if (sliders[0]) {
      sliders[0].value = '0.3'
      sliders[0].dispatchEvent(new Event('input', { bubbles: true }))
    }
    const lungBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent === '肺部')
    if (lungBtn) lungBtn.click()
  })
  await sleep(500)

  // ── 4. 截图 MPR 视图 ──
  console.log('7. 截取 MPR 视图...')
  await page.screenshot({ path: '/workspace/docs/m3-mpr-view.png', fullPage: false })
  console.log('   MPR 截图已保存到 /workspace/docs/m3-mpr-view.png')

  // ── 5. 检测 Canvas 元素 ──
  const canvasCount = await page.evaluate(() => document.querySelectorAll('canvas').length)
  console.log(`   检测到 ${canvasCount} 个 Canvas 元素`)

  // ── 6. 检查 Sidebar 导航标记 ──
  const sidebarActive = await page.evaluate(() => {
    const items = document.querySelectorAll('.navitem.on')
    return Array.from(items).map(i => i.textContent?.trim())
  })
  console.log('   侧边栏当前项:', sidebarActive)

  await browser.close()

  console.log('\n=== 浏览器控制台日志 ===')
  for (const l of browserLogs) console.log(l)

  console.log('\n✅ M3 端到端预览完成')
}

main().catch(e => { console.error('❌', e); process.exit(1) })
