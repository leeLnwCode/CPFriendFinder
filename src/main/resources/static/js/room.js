document.addEventListener("DOMContentLoaded", function () {
  // =========================
  // Room ID
  // =========================

  const params = new URLSearchParams(window.location.search);
  const roomId = params.get("id");

  if (!roomId) {
    alert("ไม่พบรหัสห้อง");
    window.location.href = "/home";
    return;
  }

  // =========================
  // Elements
  // =========================

  const roomTitle = document.getElementById("roomTitle");
  const roomMembers = document.getElementById("roomMembers");
  const chatMembers = document.getElementById("chatMembers");
  const membersGrid = document.getElementById("membersGrid");

  const roomCodeElement = document.getElementById("roomCode");
  const ownerRoomCode = document.getElementById("ownerRoomCode");

  const leaveRoomButton = document.getElementById("leaveRoomButton");

  const chatInput = document.getElementById("chatInput");
  const sendMessageButton = document.getElementById("sendMessageButton");
  const chatMessages = document.getElementById("chatMessages");

  const imageButton = document.getElementById("imageButton");
  const imageInput = document.getElementById("imageInput");

  // =========================
  // Room Data
  // =========================

  let room = null;

  // =========================
  // WebSocket
  // =========================

  let socket = null;
  let webSocketConnected = false;

  function connectWebSocket() {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    const socketUrl = `${protocol}//${window.location.host}/ws`;

    console.log("กำลังเชื่อม WebSocket:", socketUrl);

    socket = new WebSocket(socketUrl);

    socket.onopen = function () {
      console.log("WebSocket connected");

      /*
       * Spring STOMP CONNECT frame
       */
      socket.send(
        "CONNECT\n" +
          "accept-version:1.2\n" +
          "heart-beat:10000,10000\n" +
          "\n" +
          "\0",
      );
    };

    socket.onmessage = function (event) {
      const frame = event.data;

      console.log("WebSocket frame:", frame);

      /*
       * Spring ตอบ CONNECTED
       */
      if (frame.startsWith("CONNECTED")) {
        webSocketConnected = true;

        console.log("STOMP connected");

        subscribeRoom();
        return;
      }

      /*
       * MESSAGE จาก /topic/rooms/{roomId}
       */
      if (frame.startsWith("MESSAGE")) {
        handleWebSocketMessage(frame);
      }
    };

    socket.onerror = function (error) {
      console.error("WebSocket error:", error);

      webSocketConnected = false;
    };

    socket.onclose = function () {
      console.log("WebSocket disconnected");

      webSocketConnected = false;
    };
  }

  function subscribeRoom() {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return;
    }

    const frame =
      "SUBSCRIBE\n" +
      `id:room-${roomId}\n` +
      `destination:/topic/rooms/${roomId}\n` +
      "\n" +
      "\0";

    socket.send(frame);

    console.log("Subscribed:", `/topic/rooms/${roomId}`);
  }

  function handleWebSocketMessage(frame) {
    try {
      const separatorIndex = frame.indexOf("\n\n");

      if (separatorIndex === -1) {
        return;
      }

      const body = frame.substring(separatorIndex + 2).replace(/\0$/, "");

      if (!body) {
        return;
      }

      const message = JSON.parse(body);

      console.log("ข้อความจาก Backend:", message);

      createMessageElement(message);
      scrollChatToBottom();
    } catch (error) {
      console.error("อ่าน WebSocket message ไม่สำเร็จ:", error);
    }
  }

  // =========================
  // Helpers
  // =========================

  function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value ?? "";
    return div.innerHTML;
  }

  function getFullName(member) {
    const firstname = member.firstname || "";
    const lastname = member.lastname || "";

    const fullname = `${firstname} ${lastname}`.trim();

    return fullname || "ไม่ระบุชื่อ";
  }

  function getImageUrl(imageUrl) {
    return imageUrl || "/images/man.jpg";
  }

  // =========================
  // LOAD ROOM
  // =========================

  async function loadRoom() {
    try {
      const response = await fetch(`/api/chats/${roomId}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดข้อมูลห้องไม่สำเร็จ (${response.status})`);
      }

      room = await response.json();

      renderRoom();
      renderMembers();
      await loadMessages();
    } catch (error) {
      console.error("โหลดข้อมูลห้องล้มเหลว:", error);

      alert("ไม่สามารถโหลดข้อมูลห้องได้");
      window.location.href = "/home";
    }
  }

  // =========================
  // RENDER ROOM
  // =========================

  function renderRoom() {
    if (!room) return;

    roomTitle.textContent = room.roomName || "ห้องพูดคุย";

    roomMembers.textContent = `${room.memberCount || 0}/${room.maxMembers || 10} คนกำลังคุย`;

    chatMembers.textContent = `${room.memberCount || 0} คน`;

    ownerRoomCode.style.display = "none";
    roomCodeElement.textContent = "-";
  }

  // =========================
  // RENDER MEMBERS
  // =========================

  function renderMembers() {
    if (!membersGrid) return;

    membersGrid.innerHTML = "";

    const members = room?.members || [];

    if (members.length === 0) {
      membersGrid.innerHTML = `
        <div>
          ยังไม่มีสมาชิกในห้อง
        </div>
      `;
      return;
    }

    members.forEach(function (member) {
      createMemberCard(member);
    });
  }

  // =========================
  // MEMBER CARD
  // =========================

  function createMemberCard(member) {
    const card = document.createElement("article");

    card.className = "member-card";

    const fullname = getFullName(member);
    const imageUrl = getImageUrl(member.imageUrl);

    const role = member.role || "";

    const department = member.department || "";
    const year = member.year || "";

    const isOwner = role === "OWNER" || role === "owner";

    /*
     * เจ้าของห้องเปิด Profile Popup ได้
     */
    if (isOwner) {
      card.classList.add("friend-profile-trigger");

      card.setAttribute("data-profile", "");
      card.setAttribute("data-name", fullname);
      card.setAttribute("data-image", imageUrl);
    }

    card.innerHTML = `
      <img
        src="${escapeHtml(imageUrl)}"
        alt="${escapeHtml(fullname)}"
      />

      <h3>
        ${escapeHtml(fullname)}
      </h3>

      <p>
        ${isOwner ? "เจ้าของห้อง" : "สมาชิก"}
      </p>

      ${isOwner ? `<small>(เจ้าของห้อง)</small>` : `<small>(สมาชิก)</small>`}
    `;

    membersGrid.appendChild(card);
  }

  // =========================
  // LOAD MESSAGES
  // =========================

  async function loadMessages() {
    if (!chatMessages) return;

    try {
      const response = await fetch(`/api/chats/${roomId}/messages`, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดข้อความไม่สำเร็จ (${response.status})`);
      }

      const messages = await response.json();

      chatMessages.innerHTML = "";

      if (!messages || messages.length === 0) {
        chatMessages.innerHTML = `
          <div class="chat-empty">
            ยังไม่มีข้อความ
          </div>
        `;
        return;
      }

      messages.forEach(function (message) {
        createMessageElement(message);
      });

      scrollChatToBottom();
    } catch (error) {
      console.error("โหลดข้อความล้มเหลว:", error);

      chatMessages.innerHTML = `
        <div class="chat-empty">
          ยังไม่มีข้อความ
        </div>
      `;
    }
  }

  // =========================
  // MESSAGE
  // =========================

  function createMessageElement(message) {
    if (!chatMessages) {
      return;
    }

    /*
     * ถ้าเป็น empty state
     * ให้เอา "ยังไม่มีข้อความ" ออกก่อน
     */
    const emptyMessage = chatMessages.querySelector(".chat-empty");

    if (emptyMessage) {
      emptyMessage.remove();
    }

    const messageElement = document.createElement("div");

    messageElement.className = "message";

    const firstname = message.senderFirstname || "";

    const lastname = message.senderLastname || "";

    const senderName = `${firstname} ${lastname}`.trim() || "สมาชิก";

    const content = message.content || "";

    messageElement.innerHTML = `
      <strong>
        ${escapeHtml(senderName)}
      </strong>

      ${content ? `<p>${escapeHtml(content)}</p>` : ""}
    `;

    chatMessages.appendChild(messageElement);
  }

  // =========================
  // SEND MESSAGE
  // =========================

  async function sendMessage() {
    if (!chatInput) {
      return;
    }

    const message = chatInput.value.trim();

    if (message === "") {
      return;
    }

    try {
      sendMessageButton.disabled = true;

      const response = await fetch(`/api/chats/${roomId}/messages`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          content: message,
          messageType: "TEXT",
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
          `ส่งข้อความไม่สำเร็จ (${response.status}) ${errorText}`,
        );
      }

      const sentMessage = await response.json();

      console.log("ส่งข้อความสำเร็จ:", sentMessage);

      /*
       * ถ้า WebSocket ยังเชื่อมไม่ได้
       * ให้แสดงข้อความจาก REST response เอง
       *
       * ถ้า WebSocket เชื่อมได้แล้ว
       * ไม่ต้องแสดงตรงนี้ เพราะ Backend
       * จะ broadcast กลับมาให้ผ่าน WebSocket
       */
      if (!webSocketConnected && sentMessage) {
        createMessageElement(sentMessage);
        scrollChatToBottom();
      }

      chatInput.value = "";
    } catch (error) {
      console.error("ส่งข้อความล้มเหลว:", error);

      alert("ไม่สามารถส่งข้อความได้");
    } finally {
      sendMessageButton.disabled = false;

      chatInput.focus();
    }
  }

  if (sendMessageButton) {
    sendMessageButton.addEventListener("click", sendMessage);
  }

  if (chatInput) {
    chatInput.addEventListener("keydown", function (event) {
      if (event.key === "Enter") {
        event.preventDefault();
        sendMessage();
      }
    });
  }

  // =========================
  // LEAVE ROOM
  // =========================

  async function leaveRoom() {
    if (!roomId) {
      window.location.href = "/home";
      return;
    }

    const confirmed = confirm("คุณต้องการออกจากห้องนี้ใช่หรือไม่?");

    if (!confirmed) {
      return;
    }

    try {
      leaveRoomButton.disabled = true;

      const response = await fetch(`/api/chats/${roomId}/leave`, {
        method: "POST",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`ออกจากห้องไม่สำเร็จ (${response.status})`);
      }

      window.location.href = "/home";
    } catch (error) {
      console.error("ออกจากห้องล้มเหลว:", error);

      leaveRoomButton.disabled = false;

      alert("ไม่สามารถออกจากห้องได้");
    }
  }

  if (leaveRoomButton) {
    leaveRoomButton.addEventListener("click", leaveRoom);
  }

  // =========================
  // IMAGE
  // =========================

  if (imageButton && imageInput) {
    imageButton.addEventListener("click", function () {
      imageInput.click();
    });

    imageInput.addEventListener("change", function () {
      const file = this.files[0];

      if (!file) {
        return;
      }

      const imageUrl = URL.createObjectURL(file);

      const messageElement = document.createElement("div");

      messageElement.className = "message";

      messageElement.innerHTML = `
          <strong>คุณ</strong>

          <img
            src="${escapeHtml(imageUrl)}"
            alt="รูปภาพ"
            style="
              max-width: 200px;
              border-radius: 10px;
              margin-top: 5px;
            "
          />
        `;

      chatMessages.appendChild(messageElement);

      scrollChatToBottom();

      imageInput.value = "";
    });
  }

  // =========================
  // SCROLL CHAT
  // =========================

  function scrollChatToBottom() {
    if (!chatMessages) return;

    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  // =========================
  // START
  // =========================

  connectWebSocket();
  loadRoom();
});
