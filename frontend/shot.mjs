import puppeteer from 'puppeteer-core'

const exe = '/usr/bin/chromium'
const browser = await puppeteer.launch({
  executablePath: exe, headless: 'new',
  args: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
})
const page = await browser.newPage()
await page.setViewport({ width: 1366, height: 820 })
page.on('console', m => { if (m.type() === 'error') console.log('PAGE-ERR:', m.text()) })

// 1) 登录页
await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
await new Promise(r => setTimeout(r, 1000))
await page.screenshot({ path: '/tmp/login.png' })

// 2) 自动登录 -> 工作台（等待工作台专属侧栏出现）
await page.evaluate(() => {
  const u = document.querySelector('input[autocomplete="username"]')
  const p = document.querySelector('input[type="password"]')
  if (u && p) { u.value = 'doctor'; p.value = 'doctor123';
    u.dispatchEvent(new Event('input', { bubbles: true }))
    p.dispatchEvent(new Event('input', { bubbles: true })) }
})
await page.click('button[type="submit"]')
try {
  await page.waitForSelector('.side', { timeout: 10000 })
  await new Promise(r => setTimeout(r, 800))
  await page.screenshot({ path: '/tmp/workbench.png' })
  console.log('workbench OK')
} catch (e) {
  console.log('workbench wait failed:', e.message, '| url=', page.url())
}

// 3) 运行字段更新自测
try {
  await page.evaluate(() => {
    const b = [...document.querySelectorAll('button')].find(x => x.textContent.includes('运行字段更新自测'))
    if (b) b.click()
  })
  await new Promise(r => setTimeout(r, 1500))
  await page.screenshot({ path: '/tmp/selftest.png' })
  console.log('selftest shot OK')
} catch (e) { console.log('selftest shot skipped:', e.message) }

await browser.close()
console.log('screenshots done')
