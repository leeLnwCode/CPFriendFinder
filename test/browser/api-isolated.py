import urllib.request,urllib.error,http.cookiejar,json,uuid,datetime,sys
from pathlib import Path
base=sys.argv[1] if len(sys.argv)>1 else 'http://127.0.0.1:18080'
if not base.startswith(('http://127.0.0.1:','http://localhost:')):raise SystemExit('Use only an isolated localhost test database; this script creates test accounts.')
checks=[]
def client():return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def api(c,method,path,data=None,expected=200):
 req=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),method=method,headers={'Content-Type':'application/json'})
 try:
  with c.open(req,timeout=20) as r:status=r.status;raw=r.read()
 except urllib.error.HTTPError as e:status=e.code;raw=e.read()
 assert status==expected,(method,path,status,expected,raw[:200].decode(errors='replace'))
 return json.loads(raw) if raw else None
suffix=uuid.uuid4().hex[:8];a,b,guest=client(),client(),client();ids=[]
for c,who in [(a,'A'),(b,'B')]:
 email=f'final.{who}.{suffix}@example.invalid';password='IsolatedTest123!'
 api(c,'POST','/api/auth/register',{'email':email,'password':password,'firstname':'Tester'+who,'lastname':'Isolated','year':1,'department':'CS','dateOfBirth':'2004-01-01','interests':['Coding','Music']},201)
 u=api(c,'POST','/api/auth/login',{'email':email,'password':password});ids.append(u.get('id') or u['userId'])
checks.append('Two isolated accounts register and authenticate against real Spring/H2')
api(a,'POST','/api/users/me',{'bio':'Final biography'})
assert api(a,'GET','/api/users/me')['bio']=='Final biography'
summary=api(b,'GET',f'/api/users/{ids[0]}/profile');assert summary['bio']=='Final biography' and 'Coding' in summary['interests'];assert 'passwordHash' not in summary and 'email' not in summary
checks.append('Biography persists through shared-key profile; public profile exposes current interests without sensitive fields')
api(a,'POST','/api/users/me',{'bio':'x'*501},400);api(a,'POST','/api/users/me',{'galleryPhotos':[{'url':'/someone-elses-photo.png'}]},400)
checks.append('Profile rejects oversized biography and foreign retained gallery URL')
f=api(a,'POST','/api/friend-requests',{'receiverId':ids[1]},201)
assert api(a,'GET',f'/api/users/{ids[1]}/profile')['friendStatus']=='pending'
assert api(b,'GET',f'/api/users/{ids[0]}/profile')['friendStatus']=='incoming'
api(a,'POST',f'/api/friend-requests/{f["id"]}/accept',{},403)
api(b,'POST',f'/api/friend-requests/{f["id"]}/accept',{})
assert api(a,'GET',f'/api/users/{ids[1]}/profile')['friendStatus']=='friend'
checks.append('Pending/incoming request IDs agree; only receiver accepts; friendship then visible')
r=api(a,'POST','/api/chats/direct/'+ids[1],{});rid=r['id'];msg=api(a,'POST',f'/api/chats/{rid}/messages',{'content':'Original','messageType':'TEXT'},201)
api(b,'PUT',f'/api/chats/{rid}/messages/{msg["id"]}',{'content':'Stolen'},403)
updated=api(a,'PUT',f'/api/chats/{rid}/messages/{msg["id"]}',{'content':'Edited'});assert updated['content']=='Edited'
assert any(m['content']=='Edited' for m in api(b,'GET',f'/api/chats/{rid}/messages'))
api(a,'PUT',f'/api/chats/{rid}/messages/{msg["id"]}',{'content':' '},400)
api(a,'DELETE',f'/api/chats/{rid}/messages/{msg["id"]}',expected=204)
assert not any(m['id']==msg['id'] for m in api(b,'GET',f'/api/chats/{rid}/messages'))
checks.append('Direct text CRUD persists edits, prevents another sender editing, validates blank content and hides soft-deleted history')
rooms=[]
for name,year in [('Zebra',1),('Alpha',2),('Beta',1)]:rooms.append(api(a,'POST','/api/chats',{'roomName':name+suffix,'targetYear':year,'maxMembers':10},201)['id'])
selected=api(a,'GET',f'/api/chats/discover?search={suffix}&year=1&sort=name&size=1&page=0');nextpage=api(a,'GET',f'/api/chats/discover?search={suffix}&year=1&sort=name&size=1&page=1')
assert [selected[0]['id'],nextpage[0]['id']]==[rooms[2],rooms[0]]
api(a,'GET','/api/chats/discover?sort=passwordHash',expected=400)
checks.append('Real SQL-backed discovery filters year, sorts and paginates without duplicate skip; rejects invalid sort')
gid=rooms[0];api(b,'POST',f'/api/chats/{gid}/join',{});again=api(b,'POST',f'/api/chats/{gid}/join',{});assert again['memberCount']==2
api(b,'GET',f'/api/chats/{gid}/call-presence')
api(guest,'GET',f'/api/chats/{gid}/call-presence',expected=401)
checks.append('Join is idempotent; call-presence requires authenticated member')
api(a,'PUT',f'/api/chats/{gid}',{'roomName':'Renamed'+suffix});api(b,'DELETE',f'/api/chats/{gid}',expected=403)
api(a,'POST',f'/api/chats/{gid}/messages',{'content':'Group live text','messageType':'TEXT'},201)
assert api(a,'GET',f'/api/chats/{gid}/messages')==[]
api(a,'DELETE',f'/api/chats/{gid}',expected=204);api(a,'GET',f'/api/chats/{gid}',expected=404);api(b,'POST',f'/api/chats/{gid}/join',{},404);api(b,'GET',f'/api/chats/{gid}/call-presence',expected=403)
assert not any(r['id']==gid for r in api(a,'GET','/api/chats'))
checks.append('Room CRUD enforces owner-only deletion, closes memberships, hides deleted rooms and prevents rejoin; group history remains live-only')
with guest.open(base+'/swagger-ui/index.html') as r:assert r.status==200
api(guest,'GET','/v3/api-docs')
checks.append('Swagger UI and OpenAPI load from running backend')
result={'executedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'Actual HTTP APIs and real Spring/JPA with isolated disposable H2 database. No live production data, real S3 uploads or physical media-network tests.','passed':len(checks),'checks':checks}
out=Path(__file__).parent/'results';out.mkdir(exist_ok=True);(out/'api-verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result))
