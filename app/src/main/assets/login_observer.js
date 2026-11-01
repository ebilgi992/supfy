/* Early, temporary counters. Never reads text, values, identities, cookies or tokens. */
(function () {
  'use strict';
  if (window.top!==window || location.origin!=='https://accounts.spotify.com' || window.__supfyLoginProbe) return;
  var s={v:1,maxInputs:0,removedInputs:0,errors:0,rejections:0,csp:0};
  Object.defineProperty(window,'__supfyLoginProbe',{value:s,configurable:true});
  var last=0,pending=false,stopped=false,timer=null;
  function bump(key) {s[key]=Math.min(32768,s[key]+1);}
  function sample() {
    pending=false;if(stopped)return;
    try {
      var n=document.querySelectorAll('input:not([type="hidden"])').length;
      s.maxInputs=Math.min(32768,Math.max(s.maxInputs,n));
      if(last>0 && n===0)bump('removedInputs');
      last=n;
    } catch (_) {bump('errors');}
  }
  function hasInput(node) {
    return node.nodeType===1 && (node.matches('input:not([type="hidden"])') || !!node.querySelector('input:not([type="hidden"])'));
  }
  var observer=new MutationObserver(function(records){
    if(stopped)return;
    // Remember parser-added fields even if hydration removes them before the delayed sample.
    try {
      for(var i=0;i<Math.min(records.length,128);i++) {
        var r=records[i];
        for(var j=0;j<Math.min(r.addedNodes.length,64);j++)if(hasInput(r.addedNodes[j]))s.maxInputs=Math.max(s.maxInputs,1);
        for(var j=0;j<Math.min(r.removedNodes.length,64);j++)if(hasInput(r.removedNodes[j]))bump('removedInputs');
      }
    }catch(_){bump('errors');}
    if(!pending){pending=true;timer=setTimeout(sample,100);}
  });
  observer.observe(document,{childList:true,subtree:true});
  addEventListener('error',function(){bump('errors');},true);
  addEventListener('unhandledrejection',function(){bump('rejections');});
  addEventListener('securitypolicyviolation',function(){bump('csp');});
  function stop(){stopped=true;observer.disconnect();if(timer!==null)clearTimeout(timer);}
  var expiry=setTimeout(stop,15000);
  addEventListener('pagehide',function(){stop();clearTimeout(expiry);},{once:true});
  sample();
})();
