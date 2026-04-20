const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const requests = [];
  page.on('request', req => {
    if (req.url().includes('/api/review-data/records')) {
      requests.push({ method: req.method(), url: req.url() });
    }
  });
  page.on('console', msg => console.log('[browser-console]', msg.type(), msg.text()));
  await page.goto('http://localhost:18181/review-data/home', { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: '²éÑ¯' }).click();
  await page.waitForTimeout(1500);
  console.log(JSON.stringify({ requests }, null, 2));
  await browser.close();
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
