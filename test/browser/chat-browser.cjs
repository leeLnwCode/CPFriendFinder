const fs=require('fs'),path=require('path'),assert=require('assert/strict');fs.mkdirSync(path.join(__dirname,'results'),{recursive:true});
const {chromium}=require('playwright');
const root=fs.existsSync(path.resolve(__dirname,'../../code/src/main/resources'))?path.resolve(__dirname,'../../code/src/main/resources'):path.resolve(__dirname,'../../src/main/resources'),checks=[],errors=[];
let browser;
const pixel=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=','base64');
function html(kind){let s=fs.readFileSync(root+'/templates/'+kind+'.html','utf8').replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi,'');s=s.replace(/<div th:replace="~\{fragments\/friend-profile-popup[^\"]*\}"><\/div>/g,fs.readFileSync(root+'/templates/fragments/friend-profile-popup.html','utf8'));if(!s.includes('id="friendProfileOverlay"'))s=s.replace('</body>',fs.readFileSync(root+'/templates/fragments/friend-profile-popup.html','utf8')+'</body>');return s.replace('</body>',`<script src="/js/chat-images.js"></script><script src="/js/friend-profile-popup.js"></script><script src="/js/${kind}.js"></script></body>`);}
async function run(kind){
 const context=await browser.newContext(),page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.type()==='prompt'?d.accept('Edited from browser'):d.dismiss());
 await page.addInitScript(()=>{window.CPCall=new Proxy({watchTopic:(t,cb)=>window.__receive=cb,connectWS:cb=>cb?.(),loadMe:async()=>({id:'me'})},{get:(o,k)=>o[k]||(()=>false)});window.WebSocket=class{static OPEN=1;constructor(){this.readyState=1;queueMicrotask(()=>this.onopen?.());}send(){}close(){}};});
 const state={hold:null,release:null,failure:false,posts:[],history:[],status:'pending'};
 await page.route('**/*',async route=>{const req=route.request(),p=new URL(req.url()).pathname;let data;
 if(p==='/'+kind)return route.fulfill({contentType:'text/html',body:html(kind)});
 if(/^\/(js|css|images)\//.test(p)){const f=root+'/static'+p;return fs.existsSync(f)?route.fulfill({path:f}):route.fulfill({status:404,body:''});}
 if(p==='/photo.png')return route.fulfill({contentType:'image/png',body:pixel});
 if(p==='/api/users/me')data={id:'me'};
 else if(p==='/api/friends')data=[{friendId:'other',firstname:'Other',lastname:'Person'}];
 else if(p==='/api/chats/direct/other')data={id:'room1'};
 else if(p==='/api/users/other/profile')data={userId:'other',firstname:'Other',bio:'Current biography',interests:['Coding','Music'],friendStatus:state.status,requestId:'request1'};
 else if(p==='/api/users/me/profile')data={userId:'me',firstname:'Me',interests:['Coding'],friendStatus:'self'};
 else if(p==='/api/chats/room1')data={id:'room1',roomType:kind==='room'?'GROUP':'DIRECT',roomName:'Chat',members:[{userId:'other',firstname:'Other'},{userId:'me',firstname:'Me'}],memberCount:2,maxMembers:10};
 else if(p.startsWith('/api/chats/room1/messages/')&&req.method()==='PUT'){const existing=state.history.find(m=>m.id===p.split('/').at(-1));assert.ok(existing);existing.content=req.postDataJSON().content;data=existing;}
 else if(p==='/api/chats/room1/messages'){
 if(req.method()==='GET')data=state.history;
 else{const payload=req.postDataJSON();state.posts.push(payload);if(state.hold)await state.hold;if(state.failure)return route.fulfill({status:500,body:'Unavailable'});data={id:'msg-'+state.posts.length,roomId:'room1',senderId:'me',senderFirstname:'Me',content:payload.messageType==='IMAGE'?'/photo.png':payload.content,messageType:payload.messageType,createdAt:new Date().toISOString()};if(kind==='friend')state.history.push(data);else await page.evaluate(m=>window.__receive?.({body:JSON.stringify(m)}),data);}}
 else if(p==='/api/friend-requests/request1/accept'){state.status='friend';data={id:'request1'};}
 else data=[];
 return route.fulfill({contentType:'application/json',body:JSON.stringify(data)});
 });
 await page.goto('http://chat.test/'+kind+(kind==='room'?'?id=room1':''));
 const input=page.locator(kind==='room'?'#chatInput':'#messageInput');
 if(kind==='friend')await page.waitForFunction(()=>document.getElementById('chatUserName').textContent==='Other Person'&&!document.querySelector('#chatMessages .friend-empty')?.textContent.includes('กำลัง'));
 else await page.locator('[data-id="other"]').waitFor();
 const send=async()=>kind==='room'?page.click('#sendMessageButton'):input.press('Enter');
 state.hold=new Promise(r=>state.release=r);await input.fill('first draft');await send();await page.waitForFunction(()=>document.querySelector('#chatInput,#messageInput').value==='');await input.fill('next draft');await input.press('Enter');assert.equal(state.posts.length,1);state.release();state.hold=null;await page.waitForFunction(()=>document.getElementById('chatMessages').textContent.includes('first draft'));assert.equal(await input.inputValue(),'next draft');checks.push(kind+': immediate clear, duplicate prevention, retain new draft');assert.equal(await page.locator('#chatMessages').evaluate(el=>el.textContent.split('first draft').length-1),1);checks.push(kind+': no REST/WS duplicate');
 state.failure=true;await input.fill('failed draft');await send();await page.waitForFunction(()=>document.querySelector('#chatInput,#messageInput').value==='failed draft');state.failure=false;checks.push(kind+': restore failed draft');
 await page.setInputFiles('#imageInput',{name:'photo.png',mimeType:'image/png',buffer:pixel});await page.waitForFunction(()=>[...document.querySelectorAll('#chatMessages img')].some(i=>i.getAttribute('src')==='/photo.png'));assert.equal(state.posts.at(-1).messageType,'IMAGE');assert.match(state.posts.at(-1).content,/^data:image\/jpeg;base64,/);checks.push(kind+': real browser image processing and API send');assert.equal(await page.locator('#imageButton img').getAttribute('src'),'/images/image.png');checks.push(kind+': same image icon');
 if(kind==='friend'){await page.locator('.message-edit-button').first().click();await page.waitForFunction(()=>document.getElementById('chatMessages').textContent.includes('Edited from browser'));assert.equal(await page.locator('[data-message-id="msg-1"]').count(),1);checks.push('friend: own text edit replaces existing node');await page.reload();await page.waitForFunction(()=>[...document.querySelectorAll('#chatMessages img')].some(i=>i.getAttribute('src')==='/photo.png'));checks.push('friend: persisted history fixture reload');assert.match(await page.locator('#chatMessages').textContent(),/Edited from browser/);checks.push('friend: edited history fixture survives reload');}
 const trigger=kind==='room'?'[data-id="other"]':'#chatUserProfile';await page.click(trigger);await page.waitForFunction(()=>document.getElementById('friendProfileActionText').textContent==='ส่งคำขอแล้ว');assert.match(await page.locator('#friendProfileInterests').textContent(),/Coding/);checks.push(kind+': live interests and outgoing state');await page.click('#friendProfileClose');state.status='incoming';await page.click(trigger);await page.waitForFunction(()=>document.getElementById('friendProfileActionText').textContent==='ยอมรับคำขอเป็นเพื่อน');await page.click('#friendProfileAction');await page.waitForFunction(()=>document.getElementById('friendProfileActionText').textContent==='ส่งข้อความ');checks.push(kind+': latest incoming request accepted');
 if(kind==='room'){await page.click('#friendProfileClose');await page.click('[data-id="me"]');await page.waitForFunction(()=>document.getElementById('friendProfileActionText').textContent==='แก้ไขข้อมูลของฉัน');checks.push('room: self profile edit action');}
 await context.close();
}
(async()=>{try{browser=await chromium.launch({...(process.env.BROWSER_CHANNEL==='chromium'?{}:{channel:process.env.BROWSER_CHANNEL||'msedge'}),headless:true});await run('room');await run('friend');assert.deepEqual(errors,[]);checks.push('no browser script errors');fs.writeFileSync(__dirname+'/results/chat-verification.json',JSON.stringify({executedAt:new Date().toISOString(),scope:'Native Edge; isolated API/history/storage/signaling fixtures, not real user login or physical cross-network trial',passed:checks.length,checks,errors},null,2));console.log(JSON.stringify({passed:checks.length,checks}));}finally{await browser?.close();}})().catch(e=>{console.error(e);process.exitCode=1;});

