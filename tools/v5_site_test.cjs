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
for (const filename of ["index.html", "styles.css", "app.js", "release.json", "assets/supfy-icon.png", "assets/afterglow.svg", ".nojekyll"]) {
  const file = path.join(site, filename);
  if (fs.existsSync(file)) report.files[filename] = crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

const types = { ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".json": "application/json; charset=utf-8", ".svg": "image/svg+xml", ".png": "image/png" };
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
  await check("Website uses the exact app logo bytes", async () => {
    assert.equal(fs.readFileSync(path.join(site, "assets/supfy-icon.png")).compare(fs.readFileSync(path.join(root, "branding/supfy-icon-512.png"))), 0);
    assert.equal(await page.locator(".brand img").first().getAttribute("src"), "assets/supfy-icon.png");
  });
  await check("Marketing page has no QA, checksum, signing or version disclosures", async () => {
    const text = await page.locator("body").innerText();
    assert.doesNotMatch(text, /SHA[-– ]?256|checksum|test build|debug|\bANR\b|\blint\b|1\.0\.0-v5|\b1029\b|test raporu/i);
    assert.match(text, /Spotify AB ile bağlantısı/);
    assert.match(text, /Myspoti/);
    assert.match(text, /GPL-3\.0/);
  });
  await check("Published configuration activates the configured APK link", async () => {
    assert.deepEqual(await page.locator("[data-download-link]").evaluateAll((links) => links.map((link) => ({ href: link.href, disabled: link.getAttribute("aria-disabled") }))), Array.from({ length: 3 }, () => ({ href: release.downloadUrl, disabled: null })));
  });
  await check("No Instagram identity, profile photo or poster links on the site", async () => {
    assert.doesNotMatch(await page.content(), /instagram|smt\.ozdemr|poster|whatsapp/i);
    assert.deepEqual([...new Set(await page.locator("img").evaluateAll((images) => images.map((image) => image.getAttribute("src"))))].sort(), ["assets/afterglow.svg", "assets/supfy-icon.png"]);
  });
  await check("Direct APK download is visible without scrolling on desktop and mobile", async () => {
    for (const [width, height] of [[1440, 900], [390, 844], [320, 640]]) {
      await page.setViewportSize({ width, height });
      await navigate();
      const link = page.locator(".hero [data-download-link]");
      const box = await link.boundingBox();
      assert.ok(box && box.y >= 0 && box.y + box.height <= height, `Download button must fit initial ${width}x${height} viewport`);
      assert.equal(await link.getAttribute("href"), release.downloadUrl);
      assert.match(await link.innerText(), /Uygulamayı indir/);
    }
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
    await page.locator('#ana-menu a[href="#tercihler"]').click();
    assert.equal(await page.locator(".menu-toggle").getAttribute("aria-expanded"), "false");
    assert.equal(new URL(page.url()).hash, "#tercihler");
  });
  await check("Native FAQ opens and closes using Enter", async () => {
    await page.locator("summary").first().focus();
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("details").first().evaluate((el) => el.open), true);
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("details").first().evaluate((el) => el.open), false);
  });
  const route = "**/supfy/release.json";
  for (const [name, data] of [
    ["foreign download host", { ...release, downloadUrl: "https://example.org/fake.apk" }],
    ["mismatched artifact version", { ...release, downloadUrl: "https://raw.githubusercontent.com/ebilgi992/supfy/apk-download-v5-1030/downloads/Supfy-1029.apk" }],
    ["foreign source URL", { ...release, sourceUrl: "https://example.org/source" }],
  ]) {
    await check(`Rejects ${name} and disables download`, async () => {
      await page.route(route, (request) => request.fulfill({ json: data }));
      await navigate();
      assert.deepEqual(await page.locator("[data-download-link]").evaluateAll((links) => links.map((link) => link.getAttribute("aria-disabled"))), ["true", "true", "true"]);
      assert.match(await page.locator("#download-status").innerText(), /yüklenemedi/);
      await page.locator(".hero [data-download-link]").click({ force: true });
      assert.equal(page.url(), base);
      await page.unroute(route);
    });
  }
  await check("Malformed release JSON disables download", async () => {
    await page.route(route, (request) => request.fulfill({ contentType: "application/json", body: "{" }));
    await navigate();
    assert.deepEqual(await page.locator("[data-download-link]").evaluateAll((links) => links.map((link) => link.getAttribute("aria-disabled"))), ["true", "true", "true"]);
    await page.unroute(route);
  });
  await check("Release fetch failure disables download", async () => {
    await page.route(route, (request) => request.abort());
    await navigate();
    assert.deepEqual(await page.locator("[data-download-link]").evaluateAll((links) => links.map((link) => link.getAttribute("aria-disabled"))), ["true", "true", "true"]);
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
