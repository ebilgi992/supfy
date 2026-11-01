/* Public Web Platform/player UI only. No private API, account, cookie or token reads. */
(function(){
'use strict';
if(location.origin!=='https://open.spotify.com'||window.top!==window||/(?:^|\/)(?:login|signup|logout)(?:\/|$)/.test(location.pathname))return;
if(window.__supfyPlayback&&window.__supfyPlayback.v===1)return;
function usable(node,probe){
 if(node.disabled||node.getAttribute('aria-disabled')==='true'){if(probe)probe.flags|=64;return false;}
 for(var n=node;n&&n.nodeType===1;n=n.parentElement){var style=getComputedStyle(n);if(n.hidden||n.inert||n.getAttribute('aria-hidden')==='true'||style.display==='none'||style.visibility==='hidden'||style.visibility==='collapse'){if(probe)probe.flags|=128;return false;}}
 return true;
}
function control(name,probe){
 var nodes=document.querySelectorAll('[data-testid="control-button-'+name+'"]');
 if(probe&&nodes.length)probe.flags|=1;
 for(var i=0;i<Math.min(nodes.length,16);i++)if(usable(nodes[i],probe)){if(probe)probe.flags|=2;return nodes[i];}
 // Responsive pages can retain a hidden/disabled desktop player beside mobile controls.
 // Only a usable exact control may suppress the scoped public-label fallback.
 var regions=document.querySelectorAll('[data-testid="now-playing-view"],[data-testid="fullscreen-player"],[data-testid="mobile-player"],[data-testid="player-controls"],[data-testid="now-playing-bar"]');
 var labels=name==='playpause'?/^(duraklat|pause|çal|oynat|play|resume)$/:name==='skip-forward'?/^(sonraki|sonraki şarkı|next|next track)$/:/^(önceki|önceki şarkı|previous|previous track)$/;
 for(var r=0;r<Math.min(regions.length,16);r++){
  nodes=regions[r].querySelectorAll('button[aria-label]');
  for(var j=0;j<Math.min(nodes.length,64);j++){
   if(!labels.test((nodes[j].getAttribute('aria-label')||'').trim().toLocaleLowerCase('tr')))continue;
   if(probe)probe.flags|=4;
   if(usable(nodes[j],probe)){if(probe)probe.flags|=8;return nodes[j];}
  }
 }
 return null;
}
function text(selector,max){var n=document.querySelector(selector);return n?(n.textContent||'').replace(/\s+/g,' ').trim().slice(0,max):'';}
function milliseconds(n){return Number.isFinite(n)?Math.max(0,Math.min(86400000,Math.round(n*1000))):0;}
function clock(selector){var value=text(selector,16);if(!/^\d{1,2}:\d{2}(?::\d{2})?$/.test(value))return 0;var p=value.split(':').map(Number),n=0;for(var i=0;i<p.length;i++)n=n*60+p[i];return milliseconds(n);}
function playButtonState(button){if(!button)return null;var label=(button.getAttribute('aria-label')||'').toLocaleLowerCase('tr');if(/duraklat|pause/.test(label))return true;if(/^(çal|oynat|play|resume)$/.test(label))return false;return null;}
function snapshot(){
 var probe={flags:0},button=control('playpause',probe),buttonState=playButtonState(button),session=navigator.mediaSession,metadata=session&&session.metadata;
 var media=Array.prototype.slice.call(document.querySelectorAll('audio,video'),0,16),active=null;
 for(var i=0;i<media.length;i++)if(!media[i].paused&&!media[i].ended&&(media[i].readyState>=2||media[i].currentTime>0)){active=media[i];break;}
 var first=active||media[0],playing=buttonState!==null?buttonState:!!active;
 // Standard metadata alone never proves playback; use the actual control or active media element.
 var title=metadata&&typeof metadata.title==='string'?metadata.title.slice(0,200):text('[data-testid="now-playing-widget"] [data-testid="context-item-info-title"], [data-testid="context-item-info-title"]',200);
 var artist=metadata&&typeof metadata.artist==='string'?metadata.artist.slice(0,120):text('[data-testid="now-playing-widget"] [data-testid="context-item-info-artist"], [data-testid="context-item-info-artist"]',120);
 var standardSession=!!(metadata&&typeof metadata.title==='string'&&metadata.title.trim()&&session&&(session.playbackState==='playing'||session.playbackState==='paused'));
 if(active)probe.flags|=16;if(standardSession)probe.flags|=32;
 if(buttonState===null&&!active&&standardSession)playing=session.playbackState==='playing';
 var canPlayPause=!!(button&&!button.disabled)||!!first;
 var available=canPlayPause||standardSession;
 var duration=first?milliseconds(first.duration):0,position=first?milliseconds(first.currentTime):0;
 if(!duration)duration=clock('[data-testid="playback-duration"]');if(!position)position=clock('[data-testid="playback-position"]');if(duration)position=Math.min(position,duration);
 return {v:1,available:available,canPlayPause:canPlayPause,playing:available&&playing,title:title,artist:artist,durationMs:duration,positionMs:position,next:!!control('skip-forward'),previous:!!control('skip-back'),probeFlags:probe.flags,evidence:buttonState!==null?1:active?2:standardSession?3:0};
}
function command(name){
 if(location.origin!=='https://open.spotify.com'||/(?:^|\/)(?:login|signup|logout)(?:\/|$)/.test(location.pathname))return false;
 var target=null,s=snapshot();
 if(name==='play'||name==='pause'||name==='stop'){
  if(!s.available)return false;
  if((name==='play'&&s.playing)||(name!=='play'&&!s.playing))return true;
  target=control('playpause');
 }else if(name==='next')target=control('skip-forward');else if(name==='previous')target=control('skip-back');else return false;
 if(!target&&(name==='play'||name==='pause'||name==='stop')){
  var media=document.querySelector('audio,video');if(!media)return false;
  try {if(name==='play'){var result=media.play();if(result&&result.catch)result.catch(function(){});}else media.pause();return true;}catch(_){return false;}
 }
 if(!target||target.disabled)return false;target.click();return true;
}
Object.defineProperty(window,'__supfyPlayback',{value:{v:1,snapshot:snapshot,command:command},configurable:true});
})();
