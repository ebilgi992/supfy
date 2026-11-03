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

  const downloadLinks = document.querySelectorAll("[data-download-link]");
  const downloadStatuses = document.querySelectorAll("[data-download-status]");
  const setDownloadStatus = (message) => downloadStatuses.forEach((node) => { node.textContent = message; });
  downloadLinks.forEach((downloadLink) => downloadLink.addEventListener("click", (event) => {
    if (downloadLink.getAttribute("aria-disabled") === "true") {
      event.preventDefault();
      setDownloadStatus("İndirme bağlantısı şu anda açılamıyor. Sayfayı yenileyip tekrar dene.");
    }
  }));

  const trustedDownload = (value) => {
    const url = new URL(value);
    const match = url.pathname.match(/^\/ebilgi992\/supfy\/apk-download-v5-(\d+)\/downloads\/Supfy(?:-V5)?-(\d+)\.apk$/);
    return url.protocol === "https:" && url.hostname === "raw.githubusercontent.com" &&
      !url.username && !url.password && !url.port && !url.search && !url.hash && match && match[1] === match[2];
  };
  const projectLink = (value) => {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname === "github.com" && !url.username &&
      !url.password && !url.port && !url.search && !url.hash && url.pathname.startsWith("/ebilgi992/supfy/");
  };

  fetch("release.json", { cache: "no-store", credentials: "omit" })
    .then((response) => { if (!response.ok) throw new Error("Download unavailable"); return response.json(); })
    .then((data) => {
      if (!data || typeof data.downloadUrl !== "string" || typeof data.sourceUrl !== "string" ||
        !trustedDownload(data.downloadUrl) || !projectLink(data.sourceUrl)) throw new Error("Invalid links");
      document.querySelectorAll("[data-source-link]").forEach((node) => { node.href = data.sourceUrl; });
      downloadLinks.forEach((downloadLink) => {
        downloadLink.href = data.downloadUrl;
        downloadLink.removeAttribute("aria-disabled");
        downloadLink.replaceChildren(document.createTextNode("Uygulamayı indir "));
        const arrow = document.createElement("span");
        arrow.setAttribute("aria-hidden", "true");
        arrow.textContent = "↓";
        downloadLink.append(arrow);
      });
      setDownloadStatus("APK dosyası GitHub üzerinden indirilir.");
    })
    .catch(() => {
      setDownloadStatus("İndirme bağlantısı yüklenemedi. Sayfayı yenileyip tekrar dene.");
      downloadLinks.forEach((downloadLink) => { downloadLink.textContent = "Şu anda indirilemiyor"; });
    });
})();
