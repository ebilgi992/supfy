/* Supfy protection — original MIT implementation. No credentials, network or native bridge. */
(function () {
  'use strict';
  if (window.top !== window || location.origin !== 'https://open.spotify.com') return;
  if (window.__supfyProtection) return;
  let options = {enabled:false, muteAds:true, hideBanners:true, hidePromos:true, nativeAudio:false};
  let observer=null, pending=null, watchdog=null, heartbeat=0, adSince=0, awaitingRoot=false;
  let phase='idle', inAd=false, failed=false, playing=false, ready=false, awaitingMusic=false;
  let audioError=false, visualError=false, mediaAvailable=false, evidence=0;
  let leaseSuspended=false, suspendedAd=false, reason=0, mutedDomCount=0;
  const mediaSaved=new Map(), hidden=new Map();
  const totals={detected:0, muted:0, banners:0, promos:0, restored:0, errors:0, resumed:0};
  const widgets='[data-testid="now-playing-widget"], [data-testid="now-playing-bar"]';
  const advertiser='[data-testid="track-info-advertiser"], [data-testid="context-item-info-advertiser"]';
  const bannerSelector='[data-testid="ad-banner"], [data-testid="leaderboard-ad"], [data-testid="companion-ad"]';
  const promoSelector='[data-testid="premium-upsell-banner"], [data-testid="home-upsell-banner"]';
  const clock=()=>performance.now();
  function visible(node) {
    for(let el=node;el&&el.nodeType===1;el=el.parentElement) {
      if(el.hidden||el.getAttribute('aria-hidden')==='true') return false;
      const style=getComputedStyle(el);
      if(style.display==='none'||style.visibility==='hidden'||style.visibility==='collapse') return false;
    }
    return true;
  }
  function audioFault() {if(!audioError){audioError=true;totals.errors++;}}
  function visualFault() {if(!visualError){visualError=true;totals.errors++;}}
  function restoreAudio() {
    mediaSaved.forEach((muted,media)=>{
      try {if(media.muted===true){media.muted=muted;if(media.muted===muted)totals.restored++;}}
      catch(_){audioFault();}
    });
    mediaSaved.clear();
  }
  function restoreHidden(kind,detachedOnly=false) {
    hidden.forEach((record,el)=>{
      if((kind&&record.kind!==kind)||(detachedOnly&&el.isConnected))return;
      try {
        if(el.style.getPropertyValue('display')==='none'&&el.style.getPropertyPriority('display')==='important') {
          if(record.value)el.style.setProperty('display',record.value,record.priority);
          else el.style.removeProperty('display');
        }
      }catch(_){visualFault();}
      hidden.delete(el);
    });
  }
  function cleanup() {
    if(observer)observer.disconnect();observer=null;
    if(pending!==null)clearTimeout(pending);
    if(watchdog!==null)clearInterval(watchdog);
    pending=watchdog=null;
    document.removeEventListener('DOMContentLoaded',start);
    awaitingRoot=false;
    ['play','pause','ended','error'].forEach(name=>document.removeEventListener(name,schedule,true));
    restoreAudio();mutedDomCount=0;restoreHidden();inAd=false;adSince=0;ready=false;awaitingMusic=false;
  }
  function fail(code=3) {failed=true;options.enabled=false;phase='error';reason=code;totals.errors++;cleanup();}
  function suspendForLease() {
    if(leaseSuspended)return;
    suspendedAd=inAd;cleanup();leaseSuspended=true;phase='idle';reason=1;
  }
  function resumeLease() {
    if(!leaseSuspended)return;
    leaseSuspended=false;inAd=suspendedAd;suspendedAd=false;adSince=inAd?clock():0;reason=0;
  }
  function hide(selector,kind) {
    document.querySelectorAll(selector).forEach(el=>{
      if(hidden.has(el)||!visible(el))return;
      hidden.set(el,{kind,value:el.style.getPropertyValue('display'),priority:el.style.getPropertyPriority('display')});
      el.style.setProperty('display','none','important');
      if(el.style.getPropertyValue('display')==='none')totals[kind]++;
    });
  }
  function scan() {
    if(pending!==null)clearTimeout(pending);
    pending=null;
    if(!options.enabled||failed||!ready)return;
    try {
      const media=Array.from(document.querySelectorAll('audio,video'));
      mediaAvailable=media.length>0;
      const players=Array.from(document.querySelectorAll(widgets)).filter(visible);
      const marker=players.some(w=>Array.from(w.querySelectorAll(advertiser)).some(visible))||
        Array.from(document.querySelectorAll('[data-testid="video-player-ad-overlay"]')).some(visible);
      const normalTrack=players.some(w=>Array.from(w.querySelectorAll('a[href]')).some(a=>
        visible(a)&&/\/(?:track|episode)\/[A-Za-z0-9]{22}(?:[?#]|$)/.test(a.getAttribute('href')||'')));
      const pauseControl=Array.from(document.querySelectorAll('[data-testid="control-button-playpause"]')).some(b=>
        visible(b)&&/^(pause|duraklat)$/i.test((b.getAttribute('aria-label')||'').trim()));
      const playbackEvidence=mediaAvailable||(options.nativeAudio&&pauseControl);
      evidence=Number(marker)+Number(playbackEvidence)+Number(!normalTrack);
      const ad=marker&&playbackEvidence&&!normalTrack;
      let mediaPlaying=false;
      try {mediaPlaying=media.some(m=>!m.paused&&!m.ended);}catch(_){audioFault();restoreAudio();}
      playing=mediaPlaying||(options.nativeAudio&&pauseControl);
      if(ad&&!inAd){totals.detected++;adSince=clock();}
      if(inAd&&!ad)awaitingMusic=true;
      if(awaitingMusic&&!ad&&normalTrack&&playing){totals.resumed++;awaitingMusic=false;}
      if(!ad||!options.muteAds||audioError)restoreAudio();
      if(ad&&options.muteAds&&!audioError) {
        try {
          media.forEach(m=>{
            if(!mediaSaved.has(m)&&m.muted===false){
              mediaSaved.set(m,false);m.muted=true;
              if(m.muted!==true)throw new Error('Media mute did not apply');
              totals.muted++;
            }
          });
        }catch(_){audioFault();restoreAudio();}
      }
      mutedDomCount=0;
      try {mediaSaved.forEach((_,m)=>{if(m.isConnected&&m.muted===true)mutedDomCount=Math.min(10000,mutedDomCount+1);});}
      catch(_){audioFault();restoreAudio();mutedDomCount=0;}
      inAd=ad;phase=ad?'ad':(mediaAvailable||pauseControl?'music':'idle');
      if(!visualError) {
        try {
          restoreHidden(null,true);
          if(options.hideBanners)hide(bannerSelector,'banners');else restoreHidden('banners');
          if(options.hidePromos)hide(promoSelector,'promos');else restoreHidden('promos');
        }catch(_){visualFault();restoreHidden();}
      }
    }catch(_){fail();}
  }
  function schedule(){if(pending===null&&options.enabled&&ready)pending=setTimeout(scan,100);}
  function start() {
    awaitingRoot=false;
    if(!options.enabled||failed)return;
    if(!document.documentElement){
      awaitingRoot=true;document.addEventListener('DOMContentLoaded',start,{once:true});return;
    }
    if(!observer) {
      observer=new MutationObserver(schedule);
      observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true,
        attributeFilter:['data-testid','aria-hidden','hidden','style','class','data-state','src','href','aria-label']});
      ['play','pause','ended','error'].forEach(name=>document.addEventListener(name,schedule,true));
      watchdog=setInterval(()=>{
        if(clock()-heartbeat>20000)suspendForLease();
        else if(inAd&&clock()-adSince>120000)fail(2);
      },5000);
    }
    ready=true;scan();
  }
  const api={
    configure(next) {
      if(!next||!next.enabled){api.stop();return;}
      if(failed)return;
      options={enabled:true,muteAds:next.muteAds===true,hideBanners:next.hideBanners===true,
        hidePromos:next.hidePromos===true,nativeAudio:next.nativeAudio===true};
      heartbeat=clock();resumeLease();if(!awaitingRoot)start();
    },
    snapshot(){
      heartbeat=clock();
      if(options.enabled&&leaseSuspended&&!failed){resumeLease();start();}
      if(options.enabled&&ready&&!failed)scan();
      return {...totals,version:2,enabled:options.enabled,ready,phase,playing,evidence,
        reason,mutedDomCount,audioError,visualError,audioReady:ready&&!audioError&&(mediaAvailable||options.nativeAudio),
        eme:typeof navigator.requestMediaKeySystemAccess==='function'};
    },
    suspend(){if(options.enabled&&!failed)suspendForLease();},
    stop(){options.enabled=false;cleanup();phase='idle';failed=false;audioError=visualError=false;playing=false;leaseSuspended=suspendedAd=false;reason=0;}
  };
  Object.defineProperty(window,'__supfyProtection',{value:api,configurable:false});
  window.addEventListener('pagehide',()=>api.stop());
})();
