// DOM simulations only: these are not authenticated Spotify or DRM tests.
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {JSDOM} = require('jsdom');
const file = path.join(__dirname, '../app/src/main/assets/protection.js');
const source = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
const options = {enabled:true, muteAds:true, hideBanners:true, hidePromos:true};
function page(html='', url='https://open.spotify.com/') {
  const dom = new JSDOM(html, {url, runScripts:'outside-only', pretendToBeVisual:true});
  dom.window.eval(source);
  return dom;
}
function api(dom) {
  assert.ok(dom.window.__supfyProtection, 'player protection must be installed');
  dom.window.__supfyProtection.configure(options);
  return dom.window.__supfyProtection;
}
function player(title='Song') {
  return `<div data-testid="now-playing-widget"><span data-testid="context-item-info-title">${title}</span></div><audio></audio>`;
}
const wait = () => new Promise(r=>setTimeout(r,220));

test('positive track link vetoes a stale advertiser marker',()=>{
 const d=page(player()); try {
  const w=d.window.document.querySelector('[data-testid="now-playing-widget"]');
  w.innerHTML='<i data-testid="track-info-advertiser"></i><a href="/track/0123456789ABCDEFGHIJKL">Music</a>';
  const a=api(d); assert.equal(a.snapshot().detected,0); assert.equal(d.window.document.querySelector('audio').muted,false);
 } finally { d.window.close(); }
});

test('an explicit ad badge without media or corroborating playback state cannot mute',()=>{
 const d=page('<div data-testid="now-playing-widget"><i data-testid="track-info-advertiser"></i></div>');
 try { assert.equal(api(d).snapshot().detected,0); } finally {d.window.close();}
});

test('audio component failure leaves visual protection running',()=>{
 const d=page(player()+'<div data-testid="ad-banner">Ad</div>'); try {
  const doc=d.window.document;
  doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  Object.defineProperty(doc.querySelector('audio'),'muted',{get:()=>false,set:()=>{throw new Error('unsupported mute');}});
  const a=api(d); assert.equal(a.snapshot().audioError,true);
  assert.notEqual(a.snapshot().phase,'error'); assert.equal(a.snapshot().banners,1);
 } finally {d.window.close();}
});

test('confirmed normal track playback is counted only after an ad ends',async()=>{
 const d=page(player()); try {
  const doc=d.window.document, w=doc.querySelector('[data-testid="now-playing-widget"]');
  w.innerHTML='<i data-testid="track-info-advertiser"></i>';
  const a=api(d); Object.defineProperty(doc.querySelector('audio'),'paused',{value:false,configurable:true});
  w.innerHTML='<a href="/track/0123456789ABCDEFGHIJKL">Music</a>'; await wait();
  assert.equal(a.snapshot().resumed,1); a.snapshot(); assert.equal(a.snapshot().resumed,1);
 } finally {d.window.close();}
});

test('document-start initialization waits safely for the root element',async()=>{
 const d=page(); try {
  const doc=d.window.document, root=doc.documentElement; root.remove();
  const a=api(d); assert.equal(a.snapshot().ready,false);
  doc.appendChild(root); doc.dispatchEvent(new d.window.Event('DOMContentLoaded')); await wait();
  assert.equal(a.snapshot().ready,true);
 } finally {d.window.close();}
});

test('CSS class changes end the ad and restore sound',async()=>{
 const d=page('<style>.off{display:none}</style>'+player()); try {
  const doc=d.window.document,w=doc.querySelector('[data-testid="now-playing-widget"]');
  w.innerHTML='<i data-testid="track-info-advertiser"></i>';const a=api(d);
  assert.equal(doc.querySelector('audio').muted,true);
  w.firstChild.className='off';await wait();
  assert.equal(a.snapshot().phase,'music');assert.equal(doc.querySelector('audio').muted,false);
 }finally{d.window.close();}
});

test('snapshot rescan observes computed visibility without DOM mutations',()=>{
 const d=page('<style>.hideAd{display:block}</style>'+player());try{
  const doc=d.window.document,w=doc.querySelector('[data-testid="now-playing-widget"]');
  w.innerHTML='<i class="hideAd" data-testid="track-info-advertiser"></i>';const a=api(d);
  assert.equal(doc.querySelector('audio').muted,true);
  // jsdom does not invalidate computed-style caches on CSSOM edits; model that browser boundary.
  const marker=w.firstChild, original=d.window.getComputedStyle.bind(d.window);
  d.window.getComputedStyle=(el)=>el===marker?{display:'none',visibility:'visible'}:original(el);
  assert.equal(a.snapshot().phase,'music');assert.equal(doc.querySelector('audio').muted,false);
 }finally{d.window.close();}
});

test('playback tracking follows pause and play events without controlling playback',async()=>{
 const d=page(player()); try {
  const a=api(d), media=d.window.document.querySelector('audio');
  assert.equal(a.snapshot().playing,false);
  Object.defineProperty(media,'paused',{value:false,configurable:true});
  media.dispatchEvent(new d.window.Event('play',{bubbles:true})); await wait();
  assert.equal(a.snapshot().playing,true);
  Object.defineProperty(media,'paused',{value:true,configurable:true});
  media.dispatchEvent(new d.window.Event('pause',{bubbles:true})); await wait();
  assert.equal(a.snapshot().playing,false);
 } finally {d.window.close();}
});

test('pagehide stops protection and restores audio',()=>{
 const d=page(player()); try {
  const doc=d.window.document; doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  api(d); assert.equal(doc.querySelector('audio').muted,true);
  d.window.dispatchEvent(new d.window.Event('pagehide'));
  assert.equal(doc.querySelector('audio').muted,false);
 } finally {d.window.close();}
});

test('detached banners are restored without retaining obsolete page nodes',async()=>{
 const d=page('<div data-testid="ad-banner">Ad</div>'); try {
  const a=api(d), banner=d.window.document.querySelector('[data-testid="ad-banner"]');
  assert.equal(banner.style.display,'none'); banner.remove(); await wait();
  assert.equal(banner.style.display,''); assert.equal(a.snapshot().banners,1);
 } finally {d.window.close();}
});

test('a user unmuting during an ad keeps that newer choice',async()=>{
 const d=page(player()); try {
  const doc=d.window.document; doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  const a=api(d), media=doc.querySelector('audio'); assert.equal(media.muted,true);
  media.muted=false; a.configure(options); a.stop(); assert.equal(media.muted,false);
 } finally {d.window.close();}
});

test('explicit stop and restart clears current failure but preserves cumulative error count',()=>{
 const d=page(player()); try {
  const a=api(d), doc=d.window.document, query=doc.querySelectorAll.bind(doc);
  doc.querySelectorAll=()=>{throw new Error('fixture failure');}; a.configure(options);
  assert.equal(a.snapshot().phase,'error');
  doc.querySelectorAll=query; a.stop(); a.configure(options);
  assert.equal(a.snapshot().phase,'music'); assert.equal(a.snapshot().errors,1); assert.equal(a.snapshot().enabled,true);
 } finally {d.window.close();}
});

test('explicit ad mutes once and restores original mute at ad end', async()=>{
 const d=page(player()); try {
  const a=api(d), media=d.window.document.querySelector('audio');
  media.volume=0.37; media.playbackRate=1.25; media.currentTime=12;
  const marker=d.window.document.createElement('span'); marker.dataset.testid='track-info-advertiser';
  d.window.document.querySelector('[data-testid="now-playing-widget"]').append(marker); await wait();
  assert.equal(media.muted,true); assert.equal(a.snapshot().detected,1); assert.equal(a.snapshot().muted,1);
  marker.remove(); await wait();
  assert.equal(media.muted,false); assert.equal(media.volume,0.37);
  assert.equal(media.playbackRate,1.25); assert.equal(media.currentTime,12);
  assert.equal(a.snapshot().restored,1);
 } finally {d.window.close();}
});

for (const title of ['Advertisement','Spotify','Reklam','advertisement: a real song']) {
 test(`track title alone is not an ad: ${title}`,()=>{
  const d=page(player(title)); try {const a=api(d); assert.equal(a.snapshot().detected,0); assert.equal(d.window.document.querySelector('audio').muted,false);} finally {d.window.close();}
 });
}

test('pre-muted media stays muted after disable',()=>{
 const d=page(player()); try {
  const media=d.window.document.querySelector('audio'); media.muted=true;
  d.window.document.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  const a=api(d); a.stop(); assert.equal(media.muted,true); assert.equal(a.snapshot().muted,0);
 } finally {d.window.close();}
});

test('disable restores detached media and banner original style',()=>{
 const d=page(player()+'<div data-testid="ad-banner" style="display:flex!important">Ad</div>'); try {
  const doc=d.window.document, media=doc.querySelector('audio');
  doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  const a=api(d), banner=doc.querySelector('[data-testid="ad-banner"]');
  assert.equal(media.muted,true); assert.equal(d.window.getComputedStyle(banner).display,'none');
  media.remove(); a.stop();
  assert.equal(media.muted,false); assert.equal(banner.style.cssText,'display: flex !important;');
 } finally {d.window.close();}
});

test('generic login and premium dialogs are preserved; explicit upsell is hidden once',()=>{
 const d=page('<div role="dialog">Login to enjoy ad-free music<button>Close</button></div><div data-testid="premium-upsell-banner">Premium</div>'); try {
  const a=api(d), doc=d.window.document;
  assert.notEqual(d.window.getComputedStyle(doc.querySelector('[role="dialog"]')).display,'none');
  assert.equal(d.window.getComputedStyle(doc.querySelector('[data-testid="premium-upsell-banner"]')).display,'none');
  assert.equal(a.snapshot().promos,1); a.snapshot(); assert.equal(a.snapshot().promos,1);
  a.configure({...options,hidePromos:false}); assert.notEqual(d.window.getComputedStyle(doc.querySelector('[data-testid="premium-upsell-banner"]')).display,'none');
 } finally {d.window.close();}
});

test('hidden ad marker is ignored',()=>{
 const d=page(player()+'<div hidden><i data-testid="video-player-ad-overlay"></i></div>'); try {
  assert.equal(api(d).snapshot().detected,0);
 } finally {d.window.close();}
});

for(const url of ['https://accounts.spotify.com/login','https://open.spotify.com.evil.example/','https://open.spotify.com:8443/','http://open.spotify.com/']) {
 test(`does not install on untrusted or authentication origin ${url}`,()=>{
  const d=page('<input type="password" value="do-not-read">',url); try {assert.equal(d.window.__supfyProtection,undefined);} finally {d.window.close();}
 });
}

test('script reinjection is idempotent',()=>{
 const d=page(player()+'<div data-testid="ad-banner">Ad</div>'); try {
  const a=api(d); d.window.eval(source); assert.equal(d.window.__supfyProtection,a); assert.equal(a.snapshot().banners,1);
 } finally {d.window.close();}
});

test('new media during ad is muted and restored when mute option switches off',async()=>{
 const d=page(player()); try {
  const doc=d.window.document; doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  const a=api(d); const second=doc.createElement('video'); doc.body.append(second); await wait();
  assert.equal(second.muted,true); assert.equal(a.snapshot().muted,2);
  a.configure({...options,muteAds:false}); assert.equal(second.muted,false); assert.equal(doc.querySelector('audio').muted,false);
 } finally {d.window.close();}
});

test('DOM exception fails open, restoring audio and styles',async()=>{
 const d=page(player()+'<div data-testid="ad-banner">Ad</div>'); try {
  const doc=d.window.document; doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  const a=api(d), media=doc.querySelector('audio'); assert.equal(media.muted,true);
  doc.querySelectorAll=()=>{throw new Error('fixture DOM failure');};
  a.configure(options);
  assert.equal(media.muted,false); assert.equal(a.snapshot().phase,'error'); assert.equal(a.snapshot().errors,1);
 } finally {d.window.close();}
});

test('missing host heartbeat restores audio after lease timeout',()=>{
 const d=page(player()); try {
  const doc=d.window.document; doc.querySelector('[data-testid="now-playing-widget"]').innerHTML='<i data-testid="track-info-advertiser"></i>';
  let clock=100; let watchdog; Object.defineProperty(d.window.performance,'now',{value:()=>clock,configurable:true});
  d.window.setInterval=(fn)=>{watchdog=fn;return 1;};
  const a=api(d); assert.equal(doc.querySelector('audio').muted,true);
  clock=31000; watchdog(); assert.equal(doc.querySelector('audio').muted,false); assert.equal(a.snapshot().phase,'error');
 } finally {d.window.close();}
});
