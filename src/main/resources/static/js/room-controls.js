document.addEventListener('DOMContentLoaded', () => {
  let snapshot = null, me = null;
  const leave = document.getElementById('leaveRoomButton');
  if (!leave) return;
  const edit = document.createElement('button');
  edit.id = 'editRoomButton'; edit.type = 'button'; edit.className = 'room-edit-button';
  edit.textContent = 'แก้ไขห้อง'; edit.hidden = true; leave.before(edit);
  let sequence = 0;
  window.addEventListener('cp-room-loaded', async event => {
    snapshot = event.detail;
    const token = ++sequence;
    try { me = await window.CPCall?.loadMe?.(); } catch (_) { me = null; }
    if (token !== sequence) return;
    const role = snapshot.members?.find(m => String(m.userId) === String(me?.id || me?.userId))?.role;
    edit.hidden = !['OWNER','MODERATOR'].includes(role);
  });
  edit.onclick = async () => {
    if (!snapshot || edit.hidden || document.querySelector('.room-editor-dialog')) return;
    const room = snapshot, dialog = document.createElement('dialog');
    dialog.className = 'room-editor-dialog'; dialog.setAttribute('aria-labelledby','room-editor-title');
    dialog.innerHTML = `<form><h2 id="room-editor-title">แก้ไขห้องพูดคุย</h2>
      <label for="manageRoomName">ชื่อห้อง</label><input id="manageRoomName" name="roomName" required maxlength="100">
      <label for="manageRoomDescription">คำอธิบาย</label><textarea id="manageRoomDescription" name="description" maxlength="500" rows="3"></textarea>
      <label for="manageRoomMax">จำนวนสมาชิกสูงสุด</label><input id="manageRoomMax" name="maxMembers" type="number" min="2" max="100" required>
      <label for="manageRoomType">ประเภทห้อง</label><select id="manageRoomType" name="isPrivate"><option value="false">สาธารณะ</option><option value="true">ส่วนตัว</option></select>
      <label for="manageRoomPassword">รหัสห้องใหม่ (เว้นว่างเพื่อใช้รหัสเดิม)</label><input id="manageRoomPassword" name="password" type="password" minlength="4" maxlength="100" autocomplete="new-password">
      <fieldset><legend>ความสนใจของห้อง</legend><div class="room-editor-interests">กำลังโหลด…</div></fieldset>
      <p role="status"></p><div class="message-dialog-buttons"><button type="button" data-cancel>ยกเลิก</button><button type="submit" class="primary">บันทึก</button></div></form>`;
    const form = dialog.querySelector('form'), field = name => form.elements.namedItem(name), status = dialog.querySelector('[role="status"]');
    field('roomName').value = room.roomName || ''; field('description').value = room.description || '';
    field('maxMembers').value = room.maxMembers || 10; field('isPrivate').value = String(!!room.isPrivate);
    const save = dialog.querySelector('[type="submit"]'), cancel = dialog.querySelector('[data-cancel]');
    let busy = false, loaded = false;
    save.disabled = true;
    cancel.onclick = () => dialog.close();
    dialog.oncancel = event => { if (busy) event.preventDefault(); };
    dialog.addEventListener('close', () => { dialog.remove(); edit.focus(); }, {once:true});
    document.body.append(dialog); dialog.showModal(); field('roomName').focus();
    try {
      const response = await fetch('/api/interests',{credentials:'include'});
      if (!response.ok) throw Error('โหลดความสนใจไม่สำเร็จ กรุณาปิดแล้วลองใหม่');
      const interests = await response.json(), selected = new Set((room.interests || []).map(i => String(i.id)));
      const list = dialog.querySelector('.room-editor-interests'); list.replaceChildren();
      for (const interest of interests.filter(i => i.isActive !== false || selected.has(String(i.id)))) {
        const label = document.createElement('label'), input = document.createElement('input');
        input.type = 'checkbox'; input.name = 'interestIds'; input.value = interest.id; input.checked = selected.has(String(interest.id));
        label.append(input,document.createTextNode(interest.name)); list.append(label);
      }
      loaded = true; save.disabled = false;
    } catch (error) { status.textContent = error.message; }
    form.onsubmit = async event => {
      event.preventDefault(); if (busy || !loaded) return;
      const name = field('roomName').value.trim(), isPrivate = field('isPrivate').value === 'true', password = field('password').value;
      if (!name) { status.textContent = 'กรุณากรอกชื่อห้อง'; return; }
      if (isPrivate && !room.isPrivate && password.length < 4) { status.textContent = 'ห้องส่วนตัวใหม่ต้องมีรหัสอย่างน้อย 4 ตัวอักษร'; return; }
      const maxMembers = Number(field('maxMembers').value);
      if (!Number.isInteger(maxMembers) || maxMembers < Math.max(2,room.memberCount || 0) || maxMembers > 100) { status.textContent = 'จำนวนสูงสุดต้องไม่น้อยกว่าสมาชิกปัจจุบัน และอยู่ระหว่าง 2–100 คน'; return; }
      const body = {roomName:name,description:field('description').value.trim(),maxMembers,isPrivate,
        interestIds:[...form.querySelectorAll('[name="interestIds"]:checked')].map(i => i.value)};
      if (isPrivate && password) body.password = password;
      busy = true; save.disabled = cancel.disabled = true; status.textContent = 'กำลังบันทึก…';
      try {
        const response = await fetch(`/api/chats/${encodeURIComponent(room.id)}`,{method:'PUT',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
        if (!response.ok) throw Error(response.status === 403 ? 'คุณไม่มีสิทธิ์แก้ไขห้องนี้แล้ว' : 'บันทึกห้องไม่สำเร็จ กรุณาลองอีกครั้ง');
        dialog.close(); window.dispatchEvent(new CustomEvent('cp-room-refresh'));
      } catch (error) { status.textContent = error.message; }
      finally { busy = false; save.disabled = cancel.disabled = false; }
    };
  };
});
