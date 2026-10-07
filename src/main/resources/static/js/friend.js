document.addEventListener("DOMContentLoaded", () => {
  const friendSearch = document.getElementById("friendSearch");
  const friendList = document.getElementById("friendList");

  const chatUserImage = document.getElementById("chatUserImage");
  const chatUserName = document.getElementById("chatUserName");
  const chatUserYear = document.getElementById("chatUserYear");
  const chatUserProfile = document.getElementById("chatUserProfile");

  const chatMessages = document.getElementById("chatMessages");
  const messageForm = document.getElementById("messageForm");
  const messageInput = document.getElementById("messageInput");

  const chatMoreButton = document.getElementById("chatMoreButton");
  const chatMoreMenu = document.getElementById("chatMoreMenu");
  const unfriendButton = document.getElementById("unfriendButton");

  const imageButton = document.getElementById("imageButton");
  const imageInput = document.getElementById("imageInput");

  /*
   * =========================================================
   * STATE
   * =========================================================
   */

  let currentUserId = null;

  let currentFriendId = null;
  let currentRoomId = null;

  let currentFriendName = "";
  let currentFriendImage = "/images/man.jpg";

  let socket = null;
  let stompConnected = false;

  /*
   * ใช้สำหรับป้องกันข้อความซ้ำ
   * จาก REST response + WebSocket
   */
  const renderedMessageIds = new Set();

  /*
   * ใช้ระบุ connection ปัจจุบัน
   * ป้องกัน WebSocket ห้องเก่ามารบกวนห้องใหม่
   */
  let roomConnectionToken = 0;

  /*
   * =========================================================
   * CURRENT USER
   * =========================================================
   */

  async function loadCurrentUser() {
    try {
      const response = await fetch("/api/users/me", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(
          `โหลดข้อมูลผู้ใช้ปัจจุบันไม่สำเร็จ (${response.status})`,
        );
      }

      const user = await response.json();

      /*
       * รองรับทั้ง id และ userId
       */
      currentUserId = user.id || user.userId || user.user?.id || null;

      if (!currentUserId) {
        console.warn("ไม่พบ user ID จาก /api/users/me", user);
      } else {
        console.log("Current User ID:", currentUserId);
      }
    } catch (error) {
      console.error("โหลด current user ล้มเหลว:", error);

      currentUserId = null;
    }
  }

  /*
   * =========================================================
   * FRIENDS
   * =========================================================
   */

  async function loadFriends() {
    try {
      const response = await fetch("/api/friends", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดเพื่อนไม่สำเร็จ (${response.status})`);
      }

      const friends = await response.json();

      friendList.innerHTML = "";

      if (!friends || friends.length === 0) {
        friendList.innerHTML = `
          <div class="friend-empty">
            ยังไม่มีเพื่อน
          </div>
        `;

        clearChat();
        return;
      }

      friends.forEach((friend, index) => {
        const friendItem = createFriendItem(friend);

        friendList.appendChild(friendItem);

        if (index === 0) {
          friendItem.classList.add("active");
        }
      });

      const firstFriend = friendList.querySelector(".friend-list-item");

      if (firstFriend) {
        await loadConversation(firstFriend);
      }
    } catch (error) {
      console.error("โหลดรายชื่อเพื่อนล้มเหลว:", error);

      friendList.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถโหลดรายชื่อเพื่อนได้
        </div>
      `;

      clearChat();
    }
  }

  function createFriendItem(friend) {
    const friendItem = document.createElement("div");

    friendItem.className = "friend-list-item";

    /*
     * FriendResponse อาจใช้ friendId หรือ id
     */
    const friendId = friend.friendId || friend.id;

    const fullname =
      `${friend.firstname || ""} ${friend.lastname || ""}`.trim() ||
      "ไม่ระบุชื่อ";

    const image = friend.imageUrl || friend.image_url || "/images/man.jpg";

    friendItem.dataset.id = friendId || "";

    friendItem.dataset.name = fullname;

    friendItem.dataset.year =
      friend.year !== null && friend.year !== undefined
        ? `ปี ${friend.year}`
        : "";

    friendItem.dataset.image = image;

    friendItem.dataset.interests = formatInterests(friend.interests);

    friendItem.dataset.bio = friend.bio || "";

    friendItem.innerHTML = `
      <img
        class="friend-avatar"
        src="${escapeHtml(image)}"
        alt="${escapeHtml(fullname)}"
      >

      <div class="friend-info">
        <div class="friend-name">
          ${escapeHtml(fullname)}
        </div>

        <div class="friend-year">
          ${escapeHtml(friendItem.dataset.year)}
        </div>
      </div>
    `;

    friendItem.addEventListener("click", () => {
      loadConversation(friendItem);
    });

    return friendItem;
  }

  /*
   * =========================================================
   * OPEN DIRECT ROOM
   * =========================================================
   */

  async function loadConversation(friend) {
    const friendId = friend.dataset.id;

    if (!friendId) {
      console.error("ไม่พบ friendId ของเพื่อน");
      return;
    }

    /*
     * ปิด WebSocket ห้องเดิมก่อน
     */
    disconnectRoomWebSocket();

    /*
     * สร้าง token ใหม่สำหรับการเปิดห้อง
     */
    const connectionToken = ++roomConnectionToken;

    document.querySelectorAll(".friend-list-item").forEach((item) => {
      item.classList.remove("active");
    });

    friend.classList.add("active");

    currentFriendId = friendId;

    currentFriendName = friend.dataset.name || "เพื่อน";

    currentFriendImage = friend.dataset.image || "/images/man.jpg";

    currentRoomId = null;

    /*
     * Header
     */
    chatUserName.textContent = currentFriendName;

    chatUserYear.textContent = friend.dataset.year || "";

    chatUserImage.src = currentFriendImage;

    /*
     * =====================================================
     * FRIEND PROFILE POPUP
     * =====================================================
     */

    chatUserProfile.dataset.profile = "";

    chatUserProfile.dataset.id = currentFriendId;

    chatUserProfile.dataset.name = currentFriendName;

    chatUserProfile.dataset.year = friend.dataset.year || "";

    chatUserProfile.dataset.image = currentFriendImage;

    chatUserProfile.dataset.interests = friend.dataset.interests || "";

    chatUserProfile.dataset.bio = friend.dataset.bio || "";

    chatUserProfile.dataset.status = "friend";

    /*
     * Clear message ID cache
     */
    renderedMessageIds.clear();

    chatMessages.innerHTML = `
      <div class="friend-empty">
        กำลังเปิดห้องแชท...
      </div>
    `;

    chatMoreMenu.classList.remove("show");

    try {
      /*
       * =====================================================
       * GET /api/chats/direct/{friendId}
       *
       * Backend จะ:
       * - คืนห้องเดิม ถ้ามี
       * - สร้างห้องใหม่ ถ้ายังไม่มี
       * =====================================================
       */

      const response = await fetch(`/api/chats/direct/${friendId}`, {
        method: "POST",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
          `เปิดห้องแชทไม่สำเร็จ (${response.status}) ${errorText}`,
        );
      }

      const room = await response.json();

      /*
       * ถ้าผู้ใช้กดเปลี่ยนเพื่อน
       * ระหว่างที่ request เดิมยังไม่เสร็จ
       * ให้หยุด request เดิม
       */
      if (connectionToken !== roomConnectionToken) {
        return;
      }

      /*
       * ChatRoomSummaryResponse:
       *
       * {
       *   id,
       *   roomName,
       *   roomType,
       *   isPrivate,
       *   maxMembers,
       *   memberCount,
       *   unreadCount,
       *   interests,
       *   createdAt
       * }
       */

      currentRoomId = room.id;

      if (!currentRoomId) {
        throw new Error("Backend ไม่ส่ง room id กลับมา");
      }

      console.log("เปิด Direct Room:", currentRoomId);

      /*
       * โหลดประวัติข้อความก่อน
       */
      await loadMessages();

      /*
       * ตรวจอีกครั้งว่าผู้ใช้ยังอยู่ห้องเดิม
       */
      if (connectionToken !== roomConnectionToken) {
        return;
      }

      /*
       * เปิด WebSocket
       */
      connectRoomWebSocket(connectionToken);
    } catch (error) {
      console.error("เปิดห้องแชทล้มเหลว:", error);

      /*
       * ถ้าเป็น request ห้องเก่า
       * ไม่ต้องเขียนทับห้องใหม่
       */
      if (connectionToken !== roomConnectionToken) {
        return;
      }

      chatMessages.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถเปิดห้องแชทได้
        </div>
      `;
    }
  }

  /*
   * =========================================================
   * LOAD MESSAGE HISTORY
   * =========================================================
   */

  async function loadMessages() {
    if (!currentRoomId) {
      return;
    }

    try {
      const response = await fetch(
        `/api/chats/${currentRoomId}/messages?limit=50`,
        {
          method: "GET",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(`โหลดข้อความไม่สำเร็จ (${response.status})`);
      }

      const messages = await response.json();

      chatMessages.innerHTML = "";

      /*
       * reset กันข้อความซ้ำ
       */
      renderedMessageIds.clear();

      if (!messages || messages.length === 0) {
        chatMessages.innerHTML = `
          <div class="friend-empty">
            ยังไม่มีข้อความ
          </div>
        `;

        return;
      }

      messages.forEach((message) => {
        appendMessage(message);
      });

      scrollToBottom();
    } catch (error) {
      console.error("โหลดข้อความล้มเหลว:", error);

      chatMessages.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถโหลดข้อความได้
        </div>
      `;
    }
  }

  /*
   * =========================================================
   * SEND TEXT MESSAGE
   * =========================================================
   */

  messageForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const text = messageInput.value.trim();

    if (!text) {
      return;
    }

    if (!currentRoomId) {
      alert("ยังไม่ได้เปิดห้องแชท");
      return;
    }

    /*
     * เก็บ room ที่กำลังส่ง
     * ป้องกันผู้ใช้เปลี่ยนห้องระหว่าง request
     */
    const sendingRoomId = currentRoomId;

    try {
      const response = await fetch(`/api/chats/${sendingRoomId}/messages`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        credentials: "include",

        body: JSON.stringify({
          content: text,
          messageType: "TEXT",
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
          `ส่งข้อความไม่สำเร็จ (${response.status}) ${errorText}`,
        );
      }

      const message = await response.json();

      /*
       * ถ้า WebSocket ยังไม่ connected
       * จะไม่มี broadcast ให้เรา
       *
       * ดังนั้นต้องแสดง response เอง
       */
      if (!stompConnected && sendingRoomId === currentRoomId) {
        appendMessage(message);
      }

      /*
       * ถ้า WebSocket connected:
       *
       * Backend จะ broadcast message
       * ผ่าน /topic/rooms/{roomId}
       *
       * แล้ว appendMessage()
       * จะถูกเรียกจาก WebSocket
       */

      messageInput.value = "";
      messageInput.focus();
    } catch (error) {
      console.error("ส่งข้อความล้มเหลว:", error);

      alert("ไม่สามารถส่งข้อความได้");
    }
  });

  /*
   * =========================================================
   * ENTER = SEND
   * SHIFT + ENTER = NEW LINE
   * =========================================================
   */

  messageInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();

      messageForm.requestSubmit();
    }
  });

  /*
   * =========================================================
   * RENDER MESSAGE
   * =========================================================
   */

  function appendMessage(message) {
    if (!message) {
      return;
    }

    /*
     * ตอนนี้หน้า Friend รองรับ TEXT
     */
    if (message.messageType && message.messageType !== "TEXT") {
      return;
    }

    /*
     * ถ้ามี ID และเคย render แล้ว
     * ไม่ต้อง render ซ้ำ
     */
    if (message.id && renderedMessageIds.has(String(message.id))) {
      return;
    }

    if (message.id) {
      renderedMessageIds.add(String(message.id));
    }

    removeEmptyMessage();

    const wrapper = document.createElement("div");

    /*
     * =====================================================
     * แยกข้อความของเรา / ของเพื่อน
     *
     * ChatMessageResponse:
     *
     * senderId
     * senderFirstname
     * senderLastname
     * senderImageUrl
     * content
     * messageType
     * createdAt
     * =====================================================
     */

    const isMine =
      currentUserId &&
      message.senderId &&
      String(message.senderId) === String(currentUserId);

    wrapper.className = isMine
      ? "friend-chat-message sent"
      : "friend-chat-message received";

    const content = document.createElement("div");

    content.className = "friend-chat-message-content";

    content.textContent = message.content || "";

    wrapper.appendChild(content);

    chatMessages.appendChild(wrapper);

    scrollToBottom();
  }

  function removeEmptyMessage() {
    const empty = chatMessages.querySelector(".friend-empty");

    if (empty) {
      empty.remove();
    }
  }

  /*
   * =========================================================
   * WEBSOCKET / STOMP
   * =========================================================
   *
   * Backend จริง:
   *
   * REST POST
   *   /api/chats/{roomId}/messages
   *
   * Backend broadcast:
   *   /topic/rooms/{roomId}
   *
   * ดังนั้นตรงนี้ใช้ WebSocket
   * สำหรับ CONNECT + SUBSCRIBE + RECEIVE
   *
   * ไม่ส่งข้อความผ่าน /app
   * =========================================================
   */

  function connectRoomWebSocket(connectionToken) {
    if (!currentRoomId) {
      return;
    }

    /*
     * ถ้าเป็น connection เก่า
     * ไม่ต้องเปิด
     */
    if (connectionToken !== roomConnectionToken) {
      return;
    }

    disconnectRoomWebSocket();

    stompConnected = false;

    const roomId = currentRoomId;

    const socketUrl = `${
      window.location.protocol === "https:" ? "wss" : "ws"
    }://${window.location.host}/ws`;

    socket = new WebSocket(socketUrl);

    socket.onopen = () => {
      /*
       * ตรวจว่า connection นี้
       * ยังเป็นห้องปัจจุบันหรือไม่
       */
      if (connectionToken !== roomConnectionToken) {
        socket.close();
        return;
      }

      console.log("Friend WebSocket connected");

      sendStompFrame("CONNECT", {
        "accept-version": "1.2",
        "heart-beat": "0,0",
      });
    };

    socket.onmessage = (event) => {
      /*
       * ถ้าเป็น connection เก่า
       * ไม่รับ message
       */
      if (connectionToken !== roomConnectionToken) {
        return;
      }

      const frame = parseStompFrame(event.data);

      if (!frame) {
        return;
      }

      /*
       * =====================================================
       * STOMP CONNECTED
       * =====================================================
       */

      if (frame.command === "CONNECTED") {
        stompConnected = true;

        console.log("Friend STOMP connected");

        /*
         * Subscribe ห้องปัจจุบัน
         */
        sendStompFrame("SUBSCRIBE", {
          id: `friend-room-${roomId}`,
          destination: `/topic/rooms/${roomId}`,
          ack: "auto",
        });

        return;
      }

      /*
       * =====================================================
       * STOMP MESSAGE
       * =====================================================
       */

      if (frame.command === "MESSAGE") {
        try {
          const message = JSON.parse(frame.body);

          console.log("ได้รับข้อความใหม่:", message);

          /*
           * กันข้อความจากห้องอื่น
           */
          if (
            message.roomId &&
            String(message.roomId) !== String(currentRoomId)
          ) {
            return;
          }

          /*
           * เพิ่มข้อความ
           *
           * appendMessage()
           * มี renderedMessageIds
           * ป้องกัน duplicate
           */
          appendMessage(message);
        } catch (error) {
          console.error("อ่าน WebSocket message ไม่ได้:", error);
        }
      }
    };

    socket.onerror = (error) => {
      console.error("Friend WebSocket error:", error);

      stompConnected = false;
    };

    socket.onclose = () => {
      console.log("Friend WebSocket disconnected");

      stompConnected = false;
    };
  }

  /*
   * =========================================================
   * DISCONNECT WEBSOCKET
   * =========================================================
   */

  function disconnectRoomWebSocket() {
    stompConnected = false;

    if (!socket) {
      return;
    }

    try {
      /*
       * ถ้า STOMP connected
       * ส่ง DISCONNECT ก่อนปิด
       */
      if (socket.readyState === WebSocket.OPEN && stompConnected) {
        sendStompFrame("DISCONNECT");
      }
    } catch (error) {
      console.warn("ส่ง STOMP DISCONNECT ไม่ได้:", error);
    }

    try {
      socket.close();
    } catch (error) {
      console.warn("ปิด WebSocket เดิมไม่ได้:", error);
    }

    socket = null;
  }

  /*
   * =========================================================
   * SEND STOMP FRAME
   * =========================================================
   */

  function sendStompFrame(command, headers = {}, body = "") {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return;
    }

    let frame = command + "\n";

    Object.entries(headers).forEach(([key, value]) => {
      frame += `${key}:${value}\n`;
    });

    frame += "\n";
    frame += body;
    frame += "\0";

    socket.send(frame);
  }

  /*
   * =========================================================
   * PARSE STOMP FRAME
   * =========================================================
   */

  function parseStompFrame(data) {
    if (!data) {
      return null;
    }

    const cleanData = data.replace(/^\x00+/, "");

    const separatorIndex = cleanData.indexOf("\n\n");

    if (separatorIndex === -1) {
      return null;
    }

    const headerPart = cleanData.substring(0, separatorIndex);

    let body = cleanData.substring(separatorIndex + 2);

    body = body.replace(/\0+$/, "");

    const lines = headerPart.split("\n");

    const command = lines.shift();

    const headers = {};

    lines.forEach((line) => {
      const index = line.indexOf(":");

      if (index === -1) {
        return;
      }

      const key = line.substring(0, index);

      const value = line.substring(index + 1);

      headers[key] = value;
    });

    return {
      command,
      headers,
      body,
    };
  }

  /*
   * =========================================================
   * SEARCH FRIEND
   * =========================================================
   */

  friendSearch.addEventListener("input", () => {
    const keyword = friendSearch.value.trim().toLowerCase();

    const friends = document.querySelectorAll(".friend-list-item");

    let found = 0;

    friends.forEach((friend) => {
      const name = (friend.dataset.name || "").toLowerCase();

      const year = (friend.dataset.year || "").toLowerCase();

      const matched = name.includes(keyword) || year.includes(keyword);

      friend.style.display = matched ? "flex" : "none";

      if (matched) {
        found++;
      }
    });

    let emptyMessage = document.getElementById("friendEmpty");

    if (found === 0 && friends.length > 0) {
      if (!emptyMessage) {
        emptyMessage = document.createElement("div");

        emptyMessage.id = "friendEmpty";

        emptyMessage.className = "friend-empty";

        emptyMessage.textContent = "ไม่พบเพื่อนที่ค้นหา";

        friendList.appendChild(emptyMessage);
      }
    } else {
      if (emptyMessage) {
        emptyMessage.remove();
      }
    }
  });

  /*
   * =========================================================
   * CHAT MORE MENU
   * =========================================================
   */

  chatMoreButton.addEventListener("click", (event) => {
    event.stopPropagation();

    chatMoreMenu.classList.toggle("show");
  });

  document.addEventListener("click", (event) => {
    if (!event.target.closest(".chat-more")) {
      chatMoreMenu.classList.remove("show");
    }
  });

  /*
   * =========================================================
   * UNFRIEND
   * =========================================================
   */

  unfriendButton.addEventListener("click", async () => {
    if (!currentFriendId) {
      return;
    }

    const confirmUnfriend = confirm(
      `ต้องการเลิกเป็นเพื่อนกับ ${currentFriendName} หรือไม่?`,
    );

    if (!confirmUnfriend) {
      return;
    }

    try {
      const response = await fetch(`/api/friends/${currentFriendId}`, {
        method: "DELETE",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
          `เลิกเป็นเพื่อนไม่สำเร็จ (${response.status}) ${errorText}`,
        );
      }

      /*
       * ปิด WebSocket
       */
      roomConnectionToken++;

      disconnectRoomWebSocket();

      currentFriendId = null;

      currentRoomId = null;

      currentFriendName = "";

      currentFriendImage = "/images/man.jpg";

      renderedMessageIds.clear();

      await loadFriends();

      chatMoreMenu.classList.remove("show");
    } catch (error) {
      console.error("เลิกเป็นเพื่อนล้มเหลว:", error);

      alert("ไม่สามารถเลิกเป็นเพื่อนได้");
    }
  });

  /*
   * =========================================================
   * IMAGE PREVIEW
   * =========================================================
   *
   * ตอนนี้เป็น preview prototype
   *
   * ยังไม่ได้ POST รูปเข้า backend
   * เพราะ endpoint upload รูปโดยตรงยังไม่ได้กำหนด
   * =========================================================
   */

  imageButton.addEventListener("click", () => {
    imageInput.click();
  });

  imageInput.addEventListener("change", (event) => {
    const file = event.target.files[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      alert("กรุณาเลือกไฟล์รูปภาพ");

      imageInput.value = "";

      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      removeEmptyMessage();

      const wrapper = document.createElement("div");

      /*
       * รูปนี้เป็นของเรา
       */
      wrapper.className = "friend-chat-message sent";

      const image = document.createElement("img");

      image.src = reader.result;

      image.alt = "รูปภาพที่ส่ง";

      image.style.maxWidth = "240px";

      image.style.borderRadius = "12px";

      wrapper.appendChild(image);

      chatMessages.appendChild(wrapper);

      scrollToBottom();

      imageInput.value = "";
    };

    reader.readAsDataURL(file);
  });

  /*
   * =========================================================
   * CLEAR CHAT
   * =========================================================
   */

  function clearChat() {
    roomConnectionToken++;

    disconnectRoomWebSocket();

    currentFriendId = null;

    currentRoomId = null;

    currentFriendName = "";

    currentFriendImage = "/images/man.jpg";

    renderedMessageIds.clear();

    chatUserName.textContent = "เลือกเพื่อน";

    chatUserYear.textContent = "";

    chatUserImage.src = "/images/man.jpg";

    chatUserProfile.removeAttribute("data-profile");

    chatUserProfile.removeAttribute("data-id");

    chatUserProfile.removeAttribute("data-name");

    chatUserProfile.removeAttribute("data-year");

    chatUserProfile.removeAttribute("data-image");

    chatUserProfile.removeAttribute("data-interests");

    chatUserProfile.removeAttribute("data-bio");

    chatUserProfile.removeAttribute("data-status");

    chatMessages.innerHTML = `
      <div class="friend-empty">
        เลือกเพื่อนเพื่อเริ่มสนทนา
      </div>
    `;
  }

  /*
   * =========================================================
   * HELPERS
   * =========================================================
   */

  function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  function formatInterests(interests) {
    if (!interests) {
      return "";
    }

    if (Array.isArray(interests)) {
      return interests
        .map((interest) => {
          if (typeof interest === "string") {
            return interest;
          }

          return interest.name || "";
        })
        .filter(Boolean)
        .join(",");
    }

    return String(interests);
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  /*
   * =========================================================
   * START
   * =========================================================
   */

  async function startFriendPage() {
    /*
     * ต้องรู้ ID ของ user ก่อน
     * เพื่อแยก sent / received
     */
    await loadCurrentUser();

    /*
     * จากนั้นโหลดเพื่อน
     */
    await loadFriends();
  }

  startFriendPage();
});

/*
 * =========================================================
 * VOICE CALL
 * =========================================================
 */

const voiceCallButton = document.getElementById("voiceCallButton");

const voiceCallOverlay = document.getElementById("voiceCallOverlay");

const voiceCallClose = document.getElementById("voiceCallClose");

const voiceCallEnd = document.getElementById("voiceCallEnd");

const voiceCallImage = document.getElementById("voiceCallImage");

const voiceCallName = document.getElementById("voiceCallName");

const voiceCallStatus = document.getElementById("voiceCallStatus");

const voiceCallMute = document.getElementById("voiceCallMute");

const voiceCallSpeaker = document.getElementById("voiceCallSpeaker");

/*
 * =========================================================
 * OPEN VOICE CALL
 * =========================================================
 */

if (voiceCallButton) {
  voiceCallButton.addEventListener("click", () => {
    const image = document.getElementById("chatUserImage");

    const name = document.getElementById("chatUserName");

    if (image && voiceCallImage) {
      voiceCallImage.src = image.src;
    }

    if (name && voiceCallName) {
      voiceCallName.textContent = name.textContent.trim();
    }

    if (voiceCallStatus) {
      voiceCallStatus.textContent = "กำลังโทร...";
    }

    if (voiceCallMute) {
      voiceCallMute.classList.remove("muted");
    }

    if (voiceCallSpeaker) {
      voiceCallSpeaker.classList.remove("muted");
    }

    if (voiceCallOverlay) {
      voiceCallOverlay.classList.add("show");
    }
  });
}

/*
 * =========================================================
 * CLOSE VOICE CALL
 * =========================================================
 */

function closeVoiceCall() {
  if (voiceCallOverlay) {
    voiceCallOverlay.classList.remove("show");
  }
}

/*
 * =========================================================
 * CLOSE BUTTON
 * =========================================================
 */

if (voiceCallClose) {
  voiceCallClose.addEventListener("click", closeVoiceCall);
}

/*
 * =========================================================
 * END CALL
 * =========================================================
 */

if (voiceCallEnd) {
  voiceCallEnd.addEventListener("click", () => {
    if (voiceCallStatus) {
      voiceCallStatus.textContent = "สิ้นสุดการโทร";
    }

    setTimeout(closeVoiceCall, 500);
  });
}

/*
 * =========================================================
 * CLICK OUTSIDE
 * =========================================================
 */

if (voiceCallOverlay) {
  voiceCallOverlay.addEventListener("click", (event) => {
    if (event.target === voiceCallOverlay) {
      closeVoiceCall();
    }
  });
}

/*
 * =========================================================
 * ESC
 * =========================================================
 */

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeVoiceCall();
  }
});

/*
 * =========================================================
 * MUTE
 * =========================================================
 */

if (voiceCallMute) {
  voiceCallMute.addEventListener("click", () => {
    voiceCallMute.classList.toggle("muted");
  });
}

/*
 * =========================================================
 * SPEAKER
 * =========================================================
 */

if (voiceCallSpeaker) {
  voiceCallSpeaker.addEventListener("click", () => {
    voiceCallSpeaker.classList.toggle("muted");
  });
}
