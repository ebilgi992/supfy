"use strict";

(() => {
  const header = document.querySelector(".site-header");
  const toggle = document.querySelector(".menu-toggle");
  const menu = document.querySelector("#ana-menu");
  const closeMenu = () => {
    header.classList.remove("menu-open");
    toggle.setAttribute("aria-expanded", "false");
  };
  toggle.addEventListener("click", () => {
    const open = toggle.getAttribute("aria-expanded") !== "true";
    header.classList.toggle("menu-open", open);
    toggle.setAttribute("aria-expanded", String(open));
  });
  menu.addEventListener("click", (event) => { if (event.target.closest("a")) closeMenu(); });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && toggle.getAttribute("aria-expanded") === "true") {
      closeMenu();
      toggle.focus();
    }
  });
  document.addEventListener("click", (event) => { if (!header.contains(event.target)) closeMenu(); });
  window.matchMedia("(min-width: 851px)").addEventListener("change", (event) => { if (event.matches) closeMenu(); });

  let release = null;
  const downloadLink = document.querySelector("[data-download-link]");
  const downloadStatus = document.querySelector("#download-status");
  const copyStatus = document.querySelector("#copy-status");
  downloadLink.addEventListener("click", (event) => {
    if (downloadLink.getAttribute("aria-disabled") === "true") {
      event.preventDefault();
      downloadStatus.textContent = "İndirme henüz doğrulanmadı. Kaynak kodunu ve test raporunu inceleyebilirsin.";
    }
  });

  // A release file can only activate this project's single, explicitly published APK path.
  const trustedDownload = (value) => {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname === "raw.githubusercontent.com" && !url.username && !url.password && !url.port && !url.search && !url.hash &&
      url.pathname === "/ebilgi992/supfy/apk-download-v5-1029/downloads/Supfy-V5-1029.apk";
  };
  const projectLink = (value) => {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname === "github.com" && !url.username && !url.password && !url.port && url.pathname.startsWith("/ebilgi992/supfy/");
  };
  const validFacts = (data) => {
    const facts = data.verification;
    return data.version === "1.0.0-v5" && data.versionCode === 1029 && typeof data.channel === "string" &&
      typeof data.minimumAndroid === "string" && typeof data.sizeLabel === "string" && typeof data.signing === "string" &&
      /^[a-f0-9]{64}$/i.test(data.sha256) && Array.isArray(data.architectures) && data.architectures.every((item) => typeof item === "string") &&
      data.download && typeof data.download.verified === "boolean" && trustedDownload(data.download.url) &&
      projectLink(data.sourceUrl) && projectLink(data.reportUrl) && facts &&
      ["jvmPassed", "auditFindings", "negativeFixtures", "deviceTotal", "devicePassed", "deviceFailed", "deviceInterrupted", "deviceNotRun", "lintFindings", "lintTranslationGaps"].every((key) => Number.isInteger(facts[key]) && facts[key] >= 0) &&
      facts.devicePassed + facts.deviceFailed + facts.deviceInterrupted + facts.deviceNotRun === facts.deviceTotal && typeof facts.deviceNote === "string" && typeof facts.latestDeviceRun === "string";
  };

  fetch("release.json", { cache: "no-store", credentials: "omit" })
    .then((response) => { if (!response.ok) throw new Error("Release unavailable"); return response.json(); })
    .then((data) => {
      if (!validFacts(data)) throw new Error("Release facts invalid");
      release = data;
      document.querySelectorAll("[data-release]").forEach((node) => { node.textContent = String(data[node.dataset.release]); });
      document.querySelectorAll("[data-fact]").forEach((node) => { node.textContent = String(data.verification[node.dataset.fact]); });
      document.querySelectorAll("[data-source-link]").forEach((node) => { node.href = data.sourceUrl; });
      document.querySelectorAll("[data-report-link]").forEach((node) => { node.href = data.reportUrl; });
      document.querySelector("[data-architectures]").textContent = data.architectures.join(" / ");
      document.querySelector("[data-sha256]").textContent = data.sha256;
      if (data.download.verified) {
        downloadLink.href = data.download.url;
        downloadLink.removeAttribute("aria-disabled");
        downloadLink.replaceChildren(document.createTextNode("Test APK’sını indir "));
        const arrow = document.createElement("span");
        arrow.setAttribute("aria-hidden", "true");
        arrow.textContent = "↓";
        downloadLink.append(arrow);
        downloadStatus.textContent = "Doğrulanmış dosya bağlantısı · GitHub üzerinden indirilir.";
      } else {
        downloadLink.textContent = "APK henüz yayıma açılmadı";
        downloadStatus.textContent = "İndirme bağlantısı doğrulanmayı bekliyor. Kaynak kodu ve test raporu erişilebilir.";
      }
    })
    .catch(() => {
      downloadStatus.textContent = "Sürüm bilgisi yüklenemedi; indirme güvenli biçimde kapalı tutuldu. Sayfayı yeniden açmayı dene.";
      downloadLink.textContent = "Sürüm bilgisi yüklenemedi";
    });

  document.querySelectorAll("[data-copy]").forEach((button) => {
    button.addEventListener("click", async () => {
      copyStatus.classList.remove("error");
      if (!release) {
        copyStatus.classList.add("error");
        copyStatus.textContent = "Sürüm bilgisi doğrulanamadığı için kopyalanamadı. Sayfayı yeniden açmayı dene.";
        return;
      }
      const hash = button.dataset.copy === "sha256";
      const value = hash ? release.sha256 : `Supfy ${release.version} / ${release.versionCode} · ${release.channel}`;
      try {
        if (!navigator.clipboard?.writeText) throw new Error("Clipboard unavailable");
        await navigator.clipboard.writeText(value);
        copyStatus.textContent = hash ? "SHA-256 panoya kopyalandı." : "Sürüm bilgisi panoya kopyalandı.";
      } catch {
        copyStatus.classList.add("error");
        copyStatus.textContent = "Panoya erişilemedi. Görünen değeri seçip elle kopyalayabilirsin.";
      }
    });
  });
})();
