'use strict';
const fs=require('node:fs'),path=require('node:path');
function getTurnConfig(env=process.env,projectRoot=__dirname){
 const fallback=[{urls:'stun:stun.relay.metered.ca:80'}];
 const filename=path.resolve(projectRoot,env.TURN_CONFIG_FILE||'.turn-servers.json');
 let configured=[];
 if(fs.existsSync(filename)){
  try{configured=JSON.parse(fs.readFileSync(filename,'utf8').replace(/^\uFEFF/,''));}catch(_){throw new Error('TURN configuration file is not valid JSON');}
  if(!Array.isArray(configured))throw new Error('TURN configuration must be an ICE servers array');
 }
 const urls=(env.TURN_URLS||'').split(',').map(s=>s.trim()).filter(Boolean);
 if(urls.length||env.TURN_USERNAME||env.TURN_CREDENTIAL){
  if(!urls.length||!env.TURN_USERNAME||!env.TURN_CREDENTIAL)throw new Error('TURN_URLS, TURN_USERNAME and TURN_CREDENTIAL must all be configured');
  configured.push({urls,username:env.TURN_USERNAME,credential:env.TURN_CREDENTIAL});
 }
 for(const server of configured){
  if(!server||typeof server!=='object')throw new Error('Invalid TURN server entry');
  const addresses=Array.isArray(server.urls)?server.urls:[server.urls];
  if(!addresses.length||addresses.some(url=>typeof url!=='string'||! /^(stun|stuns|turn|turns):[^\s]+$/.test(url)))throw new Error('Invalid STUN/TURN address');
  if(addresses.some(url=>/^turns?:/.test(url))&&(typeof server.username!=='string'||!server.username.trim()||typeof server.credential!=='string'||!server.credential.trim()))throw new Error('TURN server requires username and credential');
 }
 const iceServers=configured.length?configured:fallback;
 return {iceServers,relayConfigured:iceServers.some(server=>(Array.isArray(server.urls)?server.urls:[server.urls]).some(url=>/^turns?:/.test(url)))};
}
module.exports={getTurnConfig};
