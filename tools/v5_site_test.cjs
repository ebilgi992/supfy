#!/usr/bin/env node
"use strict";

// No server or test framework dependency. Supply an existing playwright-core installation.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const http = require("node:http");
const crypto = require("node:crypto");

const root = path.resolve(__dirname, "..");
const site = path.join(root, "site");
const output = path.resolve(process.env.SUPFY_SITE_ARTIFACT_DIR || "/tmp/supfy-site-checks");
const modules = [process.env.SUPFY_PLAYWRIGHT_MODULE, "playwright-core", "/workspace/quantum-research/runner/node_modules/playwright-core"].filter(Boolean);
let playwright;
let modulePath;
for (const candidate of modules) {
  try { playwright = require(candidate); modulePath = require.resolve(candidate); break; } catch {}
}
if (!playwright) throw new Error("Set SUPFY_PLAYWRIGHT_MODULE to an existing playwright-core installation.");
const browserPath = [process.env.SUPFY_BROWSER_PATH, "/usr/bin/chromium", "/workspace/login-root-cause/chrome124/chrome-linux64/chrome"].find((p) => p && fs.existsSync(p));
if (!browserPath) throw new Error("Set SUPFY_BROWSER_PATH to an existing Chromium executable.");
const release = JSON.parse(fs.readFileSync(path.join(site, "release.json"), "utf8"));
const report = { started: new Date().toISOString(), browserPath, playwrightModule: modulePath, checks: [], files: {} };
for (const filename of ["index.html", "styles.css", "app.js", "release.json", "assets/supfy-mark.svg", "assets/afterglow.svg", ".nojekyll"]) {
  const file = path.join(site, filename);
  if (fs.existsSync(file)) report.files[filename] = crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

const types = { ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".json": "application/json; charset=utf-8", ".svg": "image/svg+xml" };
const server = http.createServer((request, response) => {
  try {
    const pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
    if (!pathname.startsWith("/supfy/")) { response.writeHead(404).end(); return; }
    const file = path.resolve(site, pathname.slice("/supfy/".length) || "index.html");
    if (!file.startsWith(site + path.sep) || !fs.statSync(file).isFile()) { response.writeHead(404).end(); return; }
    response.writeHead(200, { "Content-Type": types[path.extname(file)] || "application/octet-stream" });
    fs.createReadStream(file).pipe(response);
  } catch { response.writeHead(404).end(); }
});

let browser;
async function check(name, action) {
  await action();
  report.checks.push({ name, result: "PASS" });
  console.log(`PASS ${name}`);
}

(async () => {
  fs.mkdirSync(output, { recursive: true });
  await new Promise((resolve, reject) => { server.once("error", reject); server.listen(0, "127.0.0.1", resolve); });
  const origin = `http://127.0.0.1:${server.address().port}`;
  const base = origin + "/supfy/";
  browser = await playwright.chromium.launch({ executablePath: browserPath, headless: true, args: ["--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"] });
  report.browserVersion = browser.version();
  const context = await browser.newContext({ permissions: ["clipboard-read", "clipboard-write"] });
  const page = await context.newPage();
  const errors = [];
  const external = [];
  const badResponses = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => { if (new URL(request.url()).origin !== origin) external.push(request.url()); });
  page.on("response", (response) => { if (response.status() >= 400) badResponses.push(`${response.status()} ${response.url()}`); });
  const navigate = () => page.goto(base, { waitUntil: "networkidle" });
  for (const width of [1440, 1024, 768, 390, 320]) {
    await check(`Responsive ${width}px: no horizontal overflow`, async () => {
      await page.setViewportSize({ width, height: 900 });
      await navigate();
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), width);
    });
  }
  const route = "**/supfy/release.json";
  await check("Unverified configuration keeps download disabled with terminal copy", async () => {
    const unverified = structuredClone(release);
    unverified.download.verified = false;
    await page.route(route, (request) => request.fulfill({ json: unverified }));
    await navigate();
    assert.equal(await page.locator("[data-download-link]").getAttribute("aria-disabled"), "true");
    assert.match(await page.locator("[data-download-link]").innerText(), /henüz yayıma açılmadı/);
    // aria-disabled blocks Playwright actionability; a physical click must still be harmless.
    await page.locator("[data-download-link]").click({ force: true });
    assert.match(await page.locator("#download-status").innerText(), /henüz doğrulanmadı/);
    assert.equal(page.url(), base);
    await page.unroute(route);
    await navigate();
  });
  await check("Mobile menu opens; Escape closes it and restores toggle focus", async () => {
    await page.locator(".menu-toggle").click();
    assert.equal(await page.locator(".menu-toggle").getAttribute("aria-expanded"), "true");
    await page.keyboard.press("Escape");
    assert.equal(await page.locator(".menu-toggle").getAttribute("aria-expanded"), "false");
    assert.equal(await page.locator(".menu-toggle").evaluate((el) => el === document.activeElement), true);
  });
  await check("Mobile anchor navigation closes menu", async () => {
    await page.locator(".menu-toggle").click();
    await page.locator('#ana-menu a[href="#guvenlik"]').click();
    assert.equal(await page.locator(".menu-toggle").getAttribute("aria-expanded"), "false");
    assert.equal(new URL(page.url()).hash, "#guvenlik");
  });
  await check("Native FAQ opens and closes using Enter", async () => {
    await page.locator("summary").first().focus();
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("details").first().evaluate((el) => el.open), true);
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("details").first().evaluate((el) => el.open), false);
  });
  await check("SHA-256 copy roundtrip uses the actual browser clipboard", async () => {
    await page.locator('[data-copy="sha256"]').click();
    await page.waitForFunction(() => document.querySelector("#copy-status").textContent.includes("panoya kopyalandı"));
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), release.sha256);
  });
  await check("Version copy roundtrip uses the actual browser clipboard", async () => {
    await page.locator('[data-copy="version"]').click();
    await page.waitForFunction(() => document.querySelector("#copy-status").textContent.startsWith("Sürüm bilgisi panoya"));
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), `Supfy ${release.version} / ${release.versionCode} · ${release.channel}`);
  });
  await check("Clipboard refusal shows manual-copy feedback", async () => {
    await page.evaluate(() => Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText: async () => { throw new Error("denied"); } } }));
    await page.locator('[data-copy="sha256"]').click();
    await page.waitForFunction(() => document.querySelector("#copy-status").classList.contains("error"));
    assert.match(await page.locator("#copy-status").innerText(), /elle kopyalayabilirsin/);
  });
  await check("Verified configuration activates only the configured APK URL (no APK request)", async () => {
    const verified = structuredClone(release);
    verified.download.verified = true;
    await page.route(route, (request) => request.fulfill({ json: verified }));
    await navigate();
    assert.equal(await page.locator("[data-download-link]").getAttribute("href"), release.download.url);
    assert.equal(await page.locator("[data-download-link]").getAttribute("aria-disabled"), null);
    await page.unroute(route);
  });
  await check("Untrusted configured APK URL remains disabled", async () => {
    const untrusted = structuredClone(release);
    untrusted.download.verified = true;
    untrusted.download.url = "https://example.org/fake.apk";
    await page.route(route, (request) => request.fulfill({ json: untrusted }));
    await navigate();
    assert.equal(await page.locator("[data-download-link]").getAttribute("aria-disabled"), "true");
    assert.match(await page.locator("#download-status").innerText(), /yüklenemedi/);
    await page.unroute(route);
  });
  await check("Malformed release JSON fails closed", async () => {
    await page.route(route, (request) => request.fulfill({ contentType: "application/json", body: "{" }));
    await navigate();
    assert.equal(await page.locator("[data-download-link]").getAttribute("aria-disabled"), "true");
    assert.match(await page.locator("#download-status").innerText(), /yüklenemedi/);
    await page.unroute(route);
  });
  await check("Release fetch failure fails closed with visible feedback", async () => {
    await page.route(route, (request) => request.abort());
    await navigate();
    assert.equal(await page.locator("[data-download-link]").getAttribute("aria-disabled"), "true");
    assert.match(await page.locator("#download-status").innerText(), /yüklenemedi/);
    await page.unroute(route);
  });
  await navigate();
  await check("Internal anchors resolve and page IDs are unique", async () => {
    assert.deepEqual(await page.evaluate(() => {
      const ids = [...document.querySelectorAll("[id]")].map((el) => el.id);
      return {
        duplicateIds: ids.filter((id, index) => ids.indexOf(id) !== index),
        missingTargets: [...document.querySelectorAll('a[href^="#"]')].map((el) => el.getAttribute("href").slice(1)).filter((id) => !document.getElementById(id)),
      };
    }), { duplicateIds: [], missingTargets: [] });
  });
  await check("Reduced-motion preference disables smooth scrolling", async () => {
    await page.emulateMedia({ reducedMotion: "reduce" });
    assert.equal(await page.evaluate(() => getComputedStyle(document.documentElement).scrollBehavior), "auto");
    await page.emulateMedia({ reducedMotion: "no-preference" });
  });
  for (const [name, width] of [["desktop", 1440], ["mobile", 390]]) {
    await page.setViewportSize({ width, height: 1000 });
    await navigate();
    await page.screenshot({ path: path.join(output, `${name}.png`), fullPage: true });
  }
  await check("No page exceptions, external requests or HTTP failures", async () => {
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    assert.deepEqual(badResponses, []);
  });
  report.result = "PASS";
  console.log(`PASS ${report.checks.length} checks; artifacts: ${output}`);
})().catch((error) => {
  report.result = "FAIL";
  report.error = error.stack || String(error);
  console.error(report.error);
  process.exitCode = 1;
}).finally(async () => {
  if (browser) await browser.close();
  if (server.listening) await new Promise((resolve) => server.close(resolve));
  report.finished = new Date().toISOString();
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, "report.json"), JSON.stringify(report, null, 2) + "\n");
});
