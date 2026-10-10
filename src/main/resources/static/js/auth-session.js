(() => {
  'use strict';
  if (window.CPAuthSession || !['/home','/room','/friend','/random','/notification','/setting'].includes(location.pathname)) return;
  const nativeFetch=window.fetch.bind(window);
  let redirecting=false, probing=null, ttl=null, deadline=0, timer=null, lastProbe=0;
  const channel=typeof BroadcastChannel==='function'?new BroadcastChannel('cp-auth-session'):null;
  function expire(broadcast=true){
    if(redirecting)return;redirecting=true;clearTimeout(timer);
    if(broadcast)channel?.postMessage({type:'expired'});
    try{window.CPCall?.leaveCall();}catch(_){}
    try{for(const key of Object.keys(sessionStorage))if(key.startsWith('cp-')||key==='currentUser'||key==='registrationLogin')sessionStorage.removeItem(key);}catch(_){}
    location.replace('/login');
  }
  function arm(expires,seconds,broadcast=true){
    if(redirecting||!Number.isFinite(seconds)||seconds<=0)return;
    ttl=seconds;deadline=expires;clearTimeout(timer);
    // Check only at the server's expected expiry, not with a keep-alive heartbeat.
    timer=setTimeout(()=>probe(true),Math.min(2147483647,Math.max(0,deadline-Date.now()+1000)));
    if(broadcast)channel?.postMessage({type:'activity',expires,seconds});
  }
  function inspect(response,url,started){
    if(response.status===401){expire();return;}
    if(response.redirected){try{if(new URL(response.url).pathname==='/login'){expire();return;}}catch(_){}}
    if(!response.ok)return;
    if(url.pathname==='/api/auth/logout'){expire();return;}
    const value=response.headers.get('X-Session-Timeout-Seconds');
    if(value!==null){const seconds=Number(value);if(Number.isFinite(seconds)){ttl=seconds;if(seconds<=0){deadline=0;clearTimeout(timer);return;}}}
    if(ttl>0)arm(started+ttl*1000,ttl);
  }
  window.fetch=async function(input,options){
    const started=Date.now(),response=await nativeFetch(input,options);
    try{
      const url=new URL(typeof input?.url==='string'?input.url:input,location.href);
      if(url.origin===location.origin&&url.pathname.startsWith('/api/')&&!['/api/auth/login','/api/auth/register'].includes(url.pathname))inspect(response,url,started);
    }catch(_){}
    return response;
  };
  async function probe(force=false){
    if(redirecting||probing||(!force&&Date.now()-lastProbe<5000))return;
    lastProbe=Date.now();const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),10000);
    probing=(async()=>{
      const started=Date.now();
      try{const response=await nativeFetch('/api/users/me',{credentials:'include',cache:'no-store',headers:{Accept:'application/json'},signal:controller.signal});inspect(response,new URL('/api/users/me',location.origin),started);}
      catch(_){/* A network failure is not proof of an expired session. */}
      finally{clearTimeout(timeout);probing=null;}
    })();
    return probing;
  }
  channel?.addEventListener('message',event=>{
    const data=event.data;
    if(data?.type==='expired')expire(false);
    if(data?.type==='activity'&&Number.isFinite(data.expires)&&data.expires>deadline&&Number.isFinite(data.seconds)&&data.seconds>0)arm(data.expires,data.seconds,false);
  });
  window.addEventListener('focus',()=>probe());
  window.addEventListener('pageshow',()=>probe(true));
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)probe(true);});
  window.addEventListener('cp-ws-disconnected',()=>probe());
  window.CPAuthSession={check:()=>probe(true),expire};
})();
