/** M3 像素诊断：用 readPixels 量化三个 MPR canvas 的灰度分布与空间结构 */
import puppeteer from 'puppeteer'

const BASE = 'http://localhost:5173'
const sleep = ms => new Promise(r => setTimeout(r, ms))

async function main() {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox',
           '--use-gl=angle', '--use-angle=swiftshader',
           '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
  })
  const page = await browser.newPage()
  await page.setViewport({ width: 1440, height: 900 })

  await page.goto(BASE + '/#/data-ingestion', { waitUntil: 'networkidle0' })
  await page.evaluate(() => {
    localStorage.setItem('mdt_token', 'demo')
    localStorage.setItem('mdt_user', 'doctor')
    localStorage.setItem('mdt_tenant', 'T001')
    localStorage.setItem('mdt_role', 'DOCTOR')
  })
  // hash 路由下需整页 reload 才能让 auth 状态从 localStorage 重新初始化
  await page.reload({ waitUntil: 'networkidle0' })
  await page.goto(BASE + '/#/imaging?studyUid=PV-T001-P1001-AccCT001' +
    '&patientId=P1001&accessionNumber=AccCT001' +
    '&patientVisitUid=PV-T001-P1001-AccCT001&modality=CT', { waitUntil: 'networkidle0' })
  await sleep(5000)

  const meta = await page.evaluate(() => ({
    url: location.href,
    canvasCount: document.querySelectorAll('canvas').length,
    hasLoading: !!document.querySelector('.status'),
    statusText: document.querySelector('.status')?.textContent || null,
  }))
  console.log('META:', JSON.stringify(meta))

  const stats = await page.evaluate(() => {
    const canvases = Array.from(document.querySelectorAll('canvas'))
    return canvases.map((c, i) => {
      const gl = c.getContext('webgl2')
      if (!gl) return { i, err: 'no-gl' }
      const w = c.width, h = c.height
      const px = new Uint8Array(w * h * 4)
      gl.readPixels(0, 0, w, h, gl.RGBA, gl.UNSIGNED_BYTE, px)
      let min = 255, max = 0, sum = 0, n = w * h
      for (let p = 0; p < n; p++) { const v = px[p * 4]; if (v < min) min = v; if (v > max) max = v; sum += v }
      const mean = sum / n
      let vs = 0
      for (let p = 0; p < n; p++) { const d = px[p * 4] - mean; vs += d * d }
      const std = Math.sqrt(vs / n)
      // 中心 vs 边缘（判断有无空间结构）
      const cx = (w / 2) | 0, cy = (h / 2) | 0, R = (w / 6) | 0
      let cMin = 255, cMax = 0, cSum = 0, cn = 0, eMin = 255, eMax = 0, eSum = 0, en = 0
      for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
        const v = px[(y * w + x) * 4]
        const d = Math.hypot(x - cx, y - cy)
        if (d < R) { if (v < cMin) cMin = v; if (v > cMax) cMax = v; cSum += v; cn++ }
        else if (d > R * 2) { if (v < eMin) eMin = v; if (v > eMax) eMax = v; eSum += v; en++ }
      }
      return {
        i, w, h,
        min, max, mean: +mean.toFixed(1), std: +std.toFixed(1),
        center: { min: cMin, max: cMax, mean: +(cSum / cn).toFixed(1) },
        edge:   { min: eMin, max: eMax, mean: +(eSum / en).toFixed(1) },
      }
    })
  })
  console.log(JSON.stringify(stats, null, 2))
  await browser.close()
}
main().catch(e => { console.error(e); process.exit(1) })
