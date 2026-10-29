/* Supfy protection, original MIT implementation. No credentials, network or native bridge. */
(function () {
  'use strict';
  if (window.top !== window || location.origin !== 'https://open.spotify.com') return;
  if (window.__supfyProtection) return;

  let options = {enabled:false, muteAds:true, hideBanners:true, hidePromos:true};
  let observer = null, pending = null, watchdog = null, heartbeat = 0, adSince = 0;
  let phase = 'idle', inAd = false, failed = false, playing = false;
  const mediaSaved = new Map(), hidden = new Map();
  const totals = {detected:0, muted:0, banners:0, promos:0, restored:0, errors:0};
  const adSelector = '[data-testid="now-playing-widget"] [data-testid="track-info-advertiser"], ' +
    '[data-testid="now-playing-widget"] [data-testid="context-item-info-advertiser"], ' +
    '[data-testid="video-player-ad-overlay"]';
  const bannerSelector = '[data-testid="ad-banner"], [data-testid="leaderboard-ad"], [data-testid="companion-ad"]';
  const promoSelector = '[data-testid="premium-upsell-banner"], [data-testid="home-upsell-banner"]';

  function visible(node) {
    for (let el = node; el && el.nodeType === 1; el = el.parentElement) {
      if (el.hidden || el.getAttribute('aria-hidden') === 'true') return false;
      const style = getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden' || style.visibility === 'collapse') return false;
    }
    return true;
  }
  function restoreAudio() {
    mediaSaved.forEach((muted, media) => {
      // A user who already unmuted this element keeps that newer choice.
      if (media.muted === true) {media.muted = muted; totals.restored++;}
    });
    mediaSaved.clear();
  }
  function restoreHidden(kind, detachedOnly = false) {
    hidden.forEach((record, el) => {
      if (kind && record.kind !== kind) return;
      if (detachedOnly && el.isConnected) return;
      if (el.style.getPropertyValue('display') === 'none' && el.style.getPropertyPriority('display') === 'important') {
        if (record.value) el.style.setProperty('display', record.value, record.priority);
        else el.style.removeProperty('display');
      }
      hidden.delete(el);
    });
  }
  function cleanup() {
    if (observer) observer.disconnect();
    observer = null;
    if (pending !== null) clearTimeout(pending);
    if (watchdog !== null) clearInterval(watchdog);
    pending = watchdog = null;
    ['play','pause','ended','error'].forEach(name => document.removeEventListener(name,schedule,true));
    restoreAudio(); restoreHidden(); inAd = false; adSince = 0;
  }
  function fail() {
    failed = true; options.enabled = false; phase = 'error'; totals.errors++;
    cleanup();
  }
  function hide(selector, kind) {
    document.querySelectorAll(selector).forEach(el => {
      if (hidden.has(el) || !visible(el)) return;
      hidden.set(el, {kind, value:el.style.getPropertyValue('display'), priority:el.style.getPropertyPriority('display')});
      el.style.setProperty('display','none','important');
      totals[kind]++;
    });
  }
  function scan() {
    pending = null;
    if (!options.enabled || failed) return;
    try {
      restoreHidden(null,true);
      const ad = Array.from(document.querySelectorAll(adSelector)).some(visible);
      if (ad && !inAd) {totals.detected++; adSince = Date.now();}
      if (!ad || !options.muteAds) restoreAudio();
      const mediaElements = Array.from(document.querySelectorAll('audio,video'));
      playing = mediaElements.some(media => !media.paused && !media.ended);
      if (ad && options.muteAds) {
        mediaElements.forEach(media => {
          if (!mediaSaved.has(media) && media.muted === false) {
            mediaSaved.set(media, false); media.muted = true; totals.muted++;
          }
        });
      }
      inAd = ad;
      phase = ad ? 'ad' : (mediaElements.length ? 'music' : 'idle');
      if (options.hideBanners) hide(bannerSelector,'banners'); else restoreHidden('banners');
      if (options.hidePromos) hide(promoSelector,'promos'); else restoreHidden('promos');
    } catch (_) {fail();}
  }
  function schedule() {
    if (pending === null && options.enabled) pending = setTimeout(scan,100);
  }
  const api = {
    configure(next) {
      if (!next || !next.enabled) {api.stop(); return;}
      if (failed) return;
      options = {enabled:true, muteAds:next.muteAds === true, hideBanners:next.hideBanners === true, hidePromos:next.hidePromos === true};
      heartbeat = Date.now();
      if (!observer) {
        ['play','pause','ended','error'].forEach(name => document.addEventListener(name,schedule,true));
        observer = new MutationObserver(schedule);
        observer.observe(document.documentElement, {subtree:true, childList:true, attributes:true,
          attributeFilter:['data-testid','aria-hidden','hidden','style','src']});
        watchdog = setInterval(() => {
          if (Date.now() - heartbeat > 20000 || (inAd && Date.now() - adSince > 120000)) fail();
        },5000);
      }
      scan();
    },
    snapshot() {
      heartbeat = Date.now();
      return {...totals, version:1, enabled:options.enabled, phase, playing,
        eme:typeof navigator.requestMediaKeySystemAccess === 'function'};
    },
    stop() {options.enabled = false; cleanup(); phase = 'idle'; failed = false;}
  };
  Object.defineProperty(window,'__supfyProtection',{value:api, configurable:false});
  window.addEventListener('pagehide', () => api.stop());
})();
