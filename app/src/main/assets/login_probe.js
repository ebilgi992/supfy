/* Supfy local diagnostic: bounded structural counts only. No text, values, cookies or URLs. */
(function () {
  'use strict';
  var out = {v:1, ready:false, contentPresent:false, code:0, metrics:{}};
  var m = out.metrics;
  function count(n) { return Math.max(0, Math.min(32768, Math.round(Number(n) || 0))); }
  try {
    m.state = document.readyState === 'complete' ? 3 : document.readyState === 'interactive' ? 2 : 1;
    m.origin = location.origin === 'https://accounts.spotify.com' ? 1 : location.origin === 'https://open.spotify.com' ? 2 : location.origin === 'null' ? 0 : 3;
    m.route = /(?:^|\/)login(?:\/|$)/.test(location.pathname) ? 1 : /(?:^|\/)authorize(?:\/|$)/.test(location.pathname) ? 2 : /(?:^|\/)challenge(?:\/|$)/.test(location.pathname) ? 3 : 0;
    m.body = document.body ? 1 : 0;
    m.bodyChildren = document.body ? count(document.body.childElementCount) : 0;
    m.rootChildren = document.documentElement ? count(document.documentElement.childElementCount) : 0;
    m.width = count(window.innerWidth); m.height = count(window.innerHeight);
    m.visualWidth = window.visualViewport ? count(window.visualViewport.width) : 0;
    m.visualHeight = window.visualViewport ? count(window.visualViewport.height) : 0;
    m.dpr1000=count(window.devicePixelRatio*1000);
    m.visualScale1000=window.visualViewport?count(window.visualViewport.scale*1000):0;
    m.visualOffsetX=window.visualViewport?count(window.visualViewport.offsetLeft):0;
    m.visualOffsetY=window.visualViewport?count(window.visualViewport.offsetTop):0;
    m.scrollX = count(window.scrollX); m.scrollY = count(window.scrollY);
    m.inputs = count(document.querySelectorAll('input:not([type="hidden"])').length);
    m.iframes = count(document.querySelectorAll('iframe').length);
    m.scripts = count(document.querySelectorAll('script').length);
    m.styles = count(document.querySelectorAll('style,link[rel="stylesheet"]').length);
    m.headings = count(document.querySelectorAll('h1,h2,p,[role="alert"]').length);
    m.nextData = document.getElementById('__NEXT_DATA__') ? 1 : 0;
    var next = document.getElementById('__next');
    m.nextChildren = next ? count(next.childElementCount) : 0;
    m.documentVisible=document.visibilityState==='visible'?1:0;
    m.documentFocused=document.hasFocus()?1:0;
    function bounds(prefix,el) {
      var r=el?el.getBoundingClientRect():null;
      m[prefix+'Width']=r?count(r.width):0;m[prefix+'Height']=r?count(r.height):0;
    }
    bounds('html',document.documentElement);bounds('body',document.body);bounds('main',document.querySelector('main'));
    var input=document.querySelector('input:not([type="hidden"])');bounds('field',input);
    var field=input?input.getBoundingClientRect():null;
    m.fieldLeft=field?count(field.left):0;m.fieldTop=field?count(field.top):0;
    m.rootPseudo=0;
    for(var root of [document.documentElement,document.body])if(root)for(var pseudo of ['::before','::after']) {
      var ps=getComputedStyle(root,pseudo);
      if(ps.content!=='none'&&ps.content!=='normal'&&ps.display!=='none'&&ps.visibility==='visible'&&ps.opacity!=='0')m.rootPseudo++;
    }
    m.modal=0;
    try{m.modal=count(document.querySelectorAll(':modal').length)}catch(_){}
    m.animations=0;try{if(typeof document.getAnimations==='function')m.animations=count(document.getAnimations().length)}catch(_){}
    m.visibilityApi=input&&typeof input.checkVisibility==='function'?1:0;
    var early = window.__supfyLoginProbe;
    m.early = early && early.v === 1 ? 1 : 0;
    for (var key of ['maxInputs','removedInputs','errors','rejections','csp']) {
      m[key] = early && typeof early[key] === 'number' ? count(early[key]) : 0;
    }
    var nodes = document.querySelectorAll('input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]),textarea,button,[role="button"],[role="alert"]');
    m.candidates = count(nodes.length);
    for (var key of ['enabled','size','viewport','style','hit','disabled','readonly','hidden','ariaHidden','displayNone','visibilityHidden','opacityZero','contentHidden','hitNull','hitOther','centerOther','hitHtml','hitBody','hitAncestor','hitIframe','hitDialog','hitUnrelated','pointerNone','inert','clipped','transforms','backfaceHidden','filters','perspectives','overflowClips','contentAuto','checkInvisible']) m[key] = 0;
    for (var i=0; i<Math.min(nodes.length,512); i++) {
      var el=nodes[i],enabled=!el.disabled&&!el.readOnly;
      if (el.disabled) m.disabled++;
      if (el.readOnly) m.readonly++;
      if (enabled) m.enabled++;
      var r=el.getBoundingClientRect(),sized=r.width>=12&&r.height>=12;
      if (sized) m.size++;
      var inView=sized&&r.right>0&&r.bottom>0&&r.left<window.innerWidth&&r.top<window.innerHeight;
      if (inView) m.viewport++;
      var visible=true,pointerNone=false,inert=false,clipped=false;
      for (var n=el; n&&n.nodeType===1; n=n.parentElement) {
        var s=getComputedStyle(n);
        if(s.pointerEvents==='none')pointerNone=true;
        if(s.transform&&s.transform!=='none')m.transforms++;
        if(s.backfaceVisibility==='hidden')m.backfaceHidden++;
        if(s.filter&&s.filter!=='none')m.filters++;
        if(s.perspective&&s.perspective!=='none')m.perspectives++;
        if(s.contentVisibility==='auto')m.contentAuto++;
        if(n!==el) {
          var nr=n.getBoundingClientRect(),ex=r.left+r.width/2,ey=r.top+r.height/2;
          if((/^(hidden|clip|auto|scroll)$/.test(s.overflowX+'')&&(ex<nr.left||ex>=nr.right))||
             (/^(hidden|clip|auto|scroll)$/.test(s.overflowY+'')&&(ey<nr.top||ey>=nr.bottom)))m.overflowClips++;
        }
        if(n.inert)inert=true;
        if((s.clipPath&&s.clipPath!=='none')||(s.maskImage&&s.maskImage!=='none'))clipped=true;
        if (n.hidden) {m.hidden++;visible=false;break;}
        if (n.getAttribute('aria-hidden')==='true') {m.ariaHidden++;visible=false;break;}
        if (s.display==='none') {m.displayNone++;visible=false;break;}
        if (s.visibility!=='visible') {m.visibilityHidden++;visible=false;break;}
        if (s.opacity==='0') {m.opacityZero++;visible=false;break;}
        if (s.contentVisibility==='hidden') {m.contentHidden++;visible=false;break;}
      }
      if(pointerNone)m.pointerNone++;
      if(inert)m.inert++;
      if(clipped)m.clipped++;
      if (visible) m.style++;
      try{if(typeof el.checkVisibility==='function'&&!el.checkVisibility({checkOpacity:true,checkVisibilityCSS:true,contentVisibilityAuto:true}))m.checkInvisible++}catch(_){}
      if (!inView||!visible)continue;
      if(enabled)out.contentPresent=true;
      var left=Math.max(0,r.left),right=Math.min(window.innerWidth,r.right);
      var upper=Math.max(0,r.top),lower=Math.min(window.innerHeight,r.bottom);
      var cx=(left+right)/2,cy=(upper+lower)/2;
      var points=[[cx,cy],[left+(right-left)*0.25,cy],[left+(right-left)*0.75,cy],
                  [cx,upper+(lower-upper)*0.25],[cx,upper+(lower-upper)*0.75]];
      function matches(hit) {
        if(!hit)return false;
        if(hit===el||el.contains(hit))return true;
        for(var label=hit,depth=0;label&&depth<8;label=label.parentElement,depth++) {
          if(label.tagName==='LABEL'&&label.control===el)return true;
        }
        return false;
      }
      var center=document.elementFromPoint(cx,cy);
      if(center&&!matches(center)) {
        m.centerOther++;
        if(center===document.documentElement)m.hitHtml++;
        else if(center===document.body)m.hitBody++;
        else if(center.tagName==='IFRAME')m.hitIframe++;
        else if(center.tagName==='DIALOG'||center.getAttribute('role')==='dialog')m.hitDialog++;
        else if(center.contains(el))m.hitAncestor++;
        else m.hitUnrelated++;
      }
      var matched=false;
      for(var point of points)if(matches(document.elementFromPoint(point[0],point[1]))) {matched=true;break;}
      if(matched) {m.hit++;if(enabled)out.ready=true;}
      else if(!center)m.hitNull++;
      else m.hitOther++;
    }
  } catch (_) {out.ready=false;out.contentPresent=false;out.code=1;}
  return JSON.stringify(out);
})();
