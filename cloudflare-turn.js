'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {getTurnConfig}=require('./turn-config');
function readCloudflare(env=process.env,root=__dirname){
 const filename=path.resolve(root,env.CLOUDFLARE_TURN_CONFIG_FILE||'.cloudflare-turn.json');
 if(!fs.existsSync(filename)&&!env.CLOUDFLARE_TURN_KEY_ID&&!env.CLOUDFLARE_TURN_API_TOKEN)return null;
 let file={};
 if(fs.existsSync(filename)){try{file=JSON.parse(fs.readFileSync(filename,'utf8').replace(/^\uFEFF/,''));}catch(_){throw new Error('Cloudflare TURN file must be valid JSON');}}
 const keyId=env.CLOUDFLARE_TURN_KEY_ID||file.keyId;
 const apiToken=env.CLOUDFLARE_TURN_API_TOKEN||file.apiToken;
 const ttl=Number(env.CLOUDFLARE_TURN_TTL||file.ttl||86400);
 if(typeof keyId!=='string'||!keyId.trim()||typeof apiToken!=='string'||!apiToken.trim())throw new Error('Fill keyId and apiToken in .cloudflare-turn.json');
 if(!/^[a-zA-Z0-9_-]+$/.test(keyId)||!Number.isInteger(ttl)||ttl<600||ttl>86400)throw new Error('Invalid Cloudflare TURN key ID or ttl');
 return {keyId,apiToken,ttl};
}
function getIceStatus(env=process.env,root=__dirname){
 try{const cloudflare=readCloudflare(env,root);return cloudflare?{relayConfigured:true,provider:'cloudflare'}:{relayConfigured:getTurnConfig(env,root).relayConfigured,provider:'static'};}
 catch(_){return {relayConfigured:false,provider:'cloudflare',configurationError:true};}
}
function createIceResolver({fetchImpl=globalThis.fetch,now=Date.now}={}){
 const cache=new Map();
 return async function resolveIceConfig({env=process.env,root=__dirname,identity='local'}={}){
  const settings=readCloudflare(env,root);
  if(!settings)return getTurnConfig(env,root);
  const key=crypto.createHash('sha256').update(JSON.stringify([settings,identity])).digest('hex');
  const time=now();
  for(const [id,item]of cache)if(item.expires<=time)cache.delete(id);
  const previous=cache.get(key);if(previous)return previous.promise;
  const promise=(async()=>{
   let response;
   try{response=await fetchImpl(`https://rtc.live.cloudflare.com/v1/turn/keys/${encodeURIComponent(settings.keyId)}/credentials/generate-ice-servers`,{method:'POST',headers:{Authorization:`Bearer ${settings.apiToken}`,'Content-Type':'application/json'},body:JSON.stringify({ttl:settings.ttl}),signal:AbortSignal.timeout(10000),redirect:'error'});}
   catch(_){throw new Error('Cloudflare TURN request failed or timed out');}
   if(!response.ok)throw new Error(`Cloudflare TURN rejected credential generation (HTTP ${response.status})`);
   let body;try{body=await response.json();}catch(_){throw new Error('Cloudflare TURN returned invalid JSON');}
   if(!Array.isArray(body.iceServers)||!body.iceServers.length)throw new Error('Cloudflare TURN returned no ICE servers');
   const iceServers=body.iceServers.map(server=>{
    const urls=Array.isArray(server.urls)?server.urls:[server.urls];
    if(urls.some(url=>typeof url!=='string'||! /^(stun|stuns|turn|turns):[^\s]+$/.test(url)))throw new Error('Cloudflare TURN returned invalid addresses');
    if(urls.some(url=>/^turns?:/.test(url))&&(typeof server.username!=='string'||!server.username||typeof server.credential!=='string'||!server.credential))throw new Error('Cloudflare TURN returned incomplete credentials');
    return {urls:server.urls,...(server.username?{username:server.username,credential:server.credential}:{})};
   });
   if(!iceServers.some(server=>(Array.isArray(server.urls)?server.urls:[server.urls]).some(url=>/^turns?:/.test(url))))throw new Error('Cloudflare TURN returned no relay servers');
   return {iceServers,relayConfigured:true,provider:'cloudflare',expiresAt:new Date(now()+settings.ttl*1000).toISOString()};
  })();
  if(cache.size>=1000)cache.delete(cache.keys().next().value);
  const entry={expires:time+Math.min(300,settings.ttl-60)*1000,promise};cache.set(key,entry);
  try{return await promise;}catch(error){if(cache.get(key)===entry)cache.delete(key);throw error;}
 };
}
module.exports={readCloudflare,getIceStatus,createIceResolver};
