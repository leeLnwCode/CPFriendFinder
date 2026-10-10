(() => {
  "use strict";
  const icon = (paths) => `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">${paths}</svg>`;
  const editIcon = icon('<path d="m15 5 4 4M4 20l4-1L20 7a2.8 2.8 0 0 0-4-4L4 15z"/>');
  const deleteIcon = icon('<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>');

  function apply(container, event) {
    const id = event.messageId || event.message?.id;
    if (!id) return;
    const node = [...container.querySelectorAll('[data-message-id]')].find(n => n.dataset.messageId === String(id));
    if (!node) return;
    const content = node.querySelector('.friend-chat-message-content');
    if (event.type === 'DELETE') {
      node.dataset.deleted = 'true';
      content.textContent = 'ข้อความนี้ถูกลบแล้ว';
      node.querySelector('.message-actions')?.remove();
    } else if (event.type === 'EDIT' && node.dataset.deleted !== 'true') {
      content.textContent = event.message.content;
    }
  }

  function attach(node, message, {userId, roomId, container}) {
    if (!userId || !message.senderId || !message.id || message.deleted || String(message.senderId) !== String(userId)) return;
    const targetRoom = message.roomId || roomId;
    const actions = document.createElement('div');
    actions.className = 'message-actions';
    function action(kind, label, graphic) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = `message-${kind}-button`;
      button.setAttribute('aria-label', label);
      button.title = label;
      button.innerHTML = graphic;
      button.addEventListener('click', () => open(kind));
      actions.append(button);
    }
    if (message.messageType === 'TEXT') action('edit', 'แก้ไขข้อความของฉัน', editIcon);
    action('delete', 'ลบข้อความของฉัน', deleteIcon);
    node.append(actions);

    function open(kind) {
      if (node.dataset.deleted === 'true') return;
      const dialog = document.createElement('dialog');
      dialog.className = 'message-action-dialog';
      const form = document.createElement('form');
      const heading = document.createElement('h2');
      heading.id = 'message-action-title';
      heading.textContent = kind === 'edit' ? 'แก้ไขข้อความ' : 'ลบข้อความนี้?';
      dialog.setAttribute('aria-labelledby', heading.id);
      const input = document.createElement('textarea');
      input.setAttribute('aria-label', 'ข้อความที่แก้ไข');
      input.maxLength = 5000;
      input.rows = 5;
      input.value = node.querySelector('.friend-chat-message-content').textContent;
      const description = document.createElement('p');
      description.textContent = 'ข้อความจะถูกลบออกจากบทสนทนาของทั้งสองฝ่าย';
      const status = document.createElement('p');
      status.setAttribute('role', 'status');
      const footer = document.createElement('div');
      footer.className = 'message-dialog-buttons';
      const cancel = document.createElement('button');
      cancel.type = 'button'; cancel.textContent = 'ยกเลิก';
      const save = document.createElement('button');
      save.type = 'submit'; save.textContent = kind === 'edit' ? 'บันทึก' : 'ลบข้อความ';
      save.className = kind === 'delete' ? 'danger' : 'primary';
      let busy = false;
      cancel.onclick = () => dialog.close();
      dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
      dialog.addEventListener('close', () => dialog.remove(), {once:true});
      footer.append(cancel, save);
      form.append(heading, kind === 'edit' ? input : description, status, footer);
      form.onsubmit = async event => {
        event.preventDefault();
        if (busy) return;
        const content = input.value.trim();
        if (node.dataset.deleted === 'true') { dialog.close(); return; }
        if (kind === 'edit' && !content) { status.textContent = 'กรุณากรอกข้อความ'; input.focus(); return; }
        busy = true; save.disabled = cancel.disabled = input.disabled = true;
        status.textContent = kind === 'delete' ? 'กำลังลบ…' : 'กำลังบันทึก…';
        try {
          const response = await fetch(`/api/chats/${encodeURIComponent(targetRoom)}/messages/${encodeURIComponent(message.id)}`, {
            method: kind === 'edit' ? 'PUT' : 'DELETE', credentials: 'include',
            headers: {'Content-Type':'application/json', Accept:'application/json'},
            ...(kind === 'edit' ? {body:JSON.stringify({content})} : {})
          });
          if (!response.ok) {
            if (kind === 'delete' && response.status === 404) {
              apply(container, {type:'DELETE', messageId:message.id}); dialog.close(); return;
            }
            throw Error(response.status === 403 ? 'คุณไม่มีสิทธิ์แก้ไขหรือลบข้อความนี้' : response.status === 404 ? 'ไม่พบข้อความ อาจถูกลบไปแล้ว' : 'ทำรายการไม่สำเร็จ กรุณาลองอีกครั้ง');
          }
          apply(container, kind === 'delete' ? {type:'DELETE', messageId:message.id} : {type:'EDIT', message:await response.json()});
          dialog.close();
        } catch (error) { status.textContent = error.message || 'การเชื่อมต่อขัดข้อง กรุณาลองอีกครั้ง'; }
        finally { busy = false; save.disabled = cancel.disabled = input.disabled = false; }
      };
      dialog.append(form); document.body.append(dialog); dialog.showModal();
      (kind === 'edit' ? input : cancel).focus();
    }
  }
  window.CPMessageActions = {attach, apply};
})();
