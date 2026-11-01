/* Explicitly requested account UI summary. No input, cookies, tokens, private state or API access. */
(function(){
 'use strict';var out={v:1,status:0,name:'',signals:0};
 if(location.origin!=='https://open.spotify.com')return JSON.stringify(out);
 function visible(el){if(!el)return false;var r=el.getBoundingClientRect();if(r.width<1||r.height<1)return false;for(var n=el,depth=0;n&&depth<64;n=n.parentElement,depth++){var s=getComputedStyle(n);if(n.hidden||n.getAttribute('aria-hidden')==='true'||s.display==='none'||s.visibility!=='visible'||s.opacity==='0')return false}return true}
 function first(selector){var all=document.querySelectorAll(selector);for(var i=0;i<Math.min(all.length,128);i++)if(visible(all[i]))return all[i];return null}
 function name(raw){var s=String(raw||'').replace(/[\u0000-\u001f\u007f\u202a-\u202e\u2066-\u2069]/g,'').trim().replace(/\s+/g,' ');if(!s||s.length>80||s.indexOf('@')>=0||/https?:|token|bearer/i.test(s))return '';if(/^(your profile|profile|profil|profilin|profiliniz|hesap|account|open user menu|kullanıcı menüsünü aç|user menu|hesabını aç|hesabı aç|hesabım|hesabınız)$/i.test(s))return '';return s}
 var guest=first('[data-testid="login-button"],a[href*="accounts.spotify.com/login"],a[href="/login"]');
 var widget=first('[data-testid="user-widget-link"],[data-testid="user-widget-dropdown-toggle"]');
 var avatar=widget?first('[data-testid="user-widget-link"] img,[data-testid="user-widget-dropdown-toggle"] img'):null;
 var profile=first('a[href^="/user/"],a[href^="https://open.spotify.com/user/"]');
 var logout=first('[data-testid="user-widget-dropdown-logout"],a[href*="accounts.spotify.com/logout"]');
 if(!logout){var buttons=document.querySelectorAll('button,[role="menuitem"]');for(var i=0;i<Math.min(buttons.length,128);i++)if(visible(buttons[i])&&/^(oturumu kapat|çıkış yap|log out|logout)$/i.test(buttons[i].textContent.trim())){logout=buttons[i];break}}
 var display=widget?name(widget.getAttribute('aria-label')):'';
 if(!display&&avatar)display=name(avatar.getAttribute('alt'));
 var positive=(widget?1:0)+(avatar?1:0)+(profile?1:0)+(logout?1:0)+(display?1:0);
 out.signals=Math.min(5,positive);
 if(guest){out.status=positive===0?1:0;return JSON.stringify(out)}
 if(positive>=2&&((widget&&(avatar||profile||logout))||(logout&&profile))){out.status=2;out.name=display}
 return JSON.stringify(out);
})();
