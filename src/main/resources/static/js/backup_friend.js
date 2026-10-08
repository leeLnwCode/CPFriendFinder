document.addEventListener("DOMContentLoaded", () => {
  /*
   * =========================================================
   * BASIC ELEMENTS
   * =========================================================
   */

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

  /*
   * Call
   */
  let currentCallMode = null;

  let incomingCallSignal = null;

  /*
   * =========================================================
   * CHAT WEBSOCKET STATE
   * =========================================================
   */

  let socket = null;

  let stompConnected = false;

  const renderedMessageIds = new Set();

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

    disconnectRoomWebSocket();

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
     * Friend profile popup
     */
    chatUserProfile.dataset.profile = "";

    chatUserProfile.dataset.id = currentFriendId;

    chatUserProfile.dataset.name = currentFriendName;

    chatUserProfile.dataset.year = friend.dataset.year || "";

    chatUserProfile.dataset.image = currentFriendImage;

    chatUserProfile.dataset.interests = friend.dataset.interests || "";

    chatUserProfile.dataset.bio = friend.dataset.bio || "";

    chatUserProfile.dataset.status = "friend";

    renderedMessageIds.clear();

    chatMessages.innerHTML = `
      <div class="friend-empty">
        กำลังเปิดห้องแชท...
      </div>
    `;

    chatMoreMenu.classList.remove("show");

    try {
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

      if (connectionToken !== roomConnectionToken) {
        return;
      }

      currentRoomId = room.id;

      if (!currentRoomId) {
        throw new Error("Backend ไม่ส่ง room id กลับมา");
      }

      console.log("เปิด Direct Room:", currentRoomId);

      await loadMessages();

      if (connectionToken !== roomConnectionToken) {
        return;
      }

      connectRoomWebSocket(connectionToken);
    } catch (error) {
      console.error("เปิดห้องแชทล้มเหลว:", error);

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
   * SEND MESSAGE
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
       * ถ้า WebSocket ไม่ connected
       * render response เอง
       */
      if (!stompConnected && sendingRoomId === currentRoomId) {
        appendMessage(message);
      }

      messageInput.value = "";

      messageInput.focus();
    } catch (error) {
      console.error("ส่งข้อความล้มเหลว:", error);

      alert("ไม่สามารถส่งข้อความได้");
    }
  });

  /*
   * =========================================================
   * ENTER SEND
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
   * APPEND MESSAGE
   * =========================================================
   */

  function appendMessage(message) {
    if (!message) {
      return;
    }

    if (message.messageType && message.messageType !== "TEXT") {
      return;
    }

    if (message.id && renderedMessageIds.has(String(message.id))) {
      return;
    }

    if (message.id) {
      renderedMessageIds.add(String(message.id));
    }

    removeEmptyMessage();

    const wrapper = document.createElement("div");

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
   * CHAT WEBSOCKET
   * =========================================================
   */

  function connectRoomWebSocket(connectionToken) {
    if (!currentRoomId) {
      return;
    }

    if (connectionToken !== roomConnectionToken) {
      return;
    }

    disconnectRoomWebSocket();

    stompConnected = false;

    const roomId = currentRoomId;

    const socketUrl = `${
      window.location.protocol === "https:" ? "wss" : "ws"
    }://${window.location.host}/ws`;

    const localSocket = new WebSocket(socketUrl);

    socket = localSocket;

    localSocket.onopen = () => {
      if (connectionToken !== roomConnectionToken) {
        localSocket.close();

        return;
      }

      console.log("Friend WebSocket connected");

      sendStompFrame(
        "CONNECT",
        {
          "accept-version": "1.2",

          "heart-beat": "0,0",
        },
        "",
        localSocket,
      );
    };

    localSocket.onmessage = (event) => {
      if (connectionToken !== roomConnectionToken) {
        return;
      }

      const frame = parseStompFrame(event.data);

      if (!frame) {
        return;
      }

      /*
       * CONNECTED
       */
      if (frame.command === "CONNECTED") {
        stompConnected = true;

        console.log("Friend STOMP connected");

        sendStompFrame(
          "SUBSCRIBE",
          {
            id: `friend-room-${roomId}`,

            destination: `/topic/rooms/${roomId}`,

            ack: "auto",
          },
          "",
          localSocket,
        );

        return;
      }

      /*
       * MESSAGE
       */
      if (frame.command === "MESSAGE") {
        try {
          const message = JSON.parse(frame.body);

          console.log("ได้รับข้อความใหม่:", message);

          if (
            message.roomId &&
            String(message.roomId) !== String(currentRoomId)
          ) {
            return;
          }

          appendMessage(message);
        } catch (error) {
          console.error("อ่าน WebSocket message ไม่ได้:", error);
        }
      }
    };

    localSocket.onerror = (error) => {
      console.error("Friend WebSocket error:", error);

      stompConnected = false;
    };

    localSocket.onclose = () => {
      console.log("Friend WebSocket disconnected");

      stompConnected = false;

      if (socket === localSocket) {
        socket = null;
      }
    };
  }

  /*
   * =========================================================
   * DISCONNECT CHAT WEBSOCKET
   * =========================================================
   */

  function disconnectRoomWebSocket() {
    const oldSocket = socket;

    const wasConnected = stompConnected;

    stompConnected = false;

    socket = null;

    if (!oldSocket) {
      return;
    }

    try {
      if (oldSocket.readyState === WebSocket.OPEN && wasConnected) {
        sendStompFrame("DISCONNECT", {}, "", oldSocket);
      }
    } catch (error) {
      console.warn("ส่ง STOMP DISCONNECT ไม่ได้:", error);
    }

    try {
      oldSocket.close();
    } catch (error) {
      console.warn("ปิด WebSocket เดิมไม่ได้:", error);
    }
  }

  /*
   * =========================================================
   * SEND STOMP FRAME
   * =========================================================
   */

  function sendStompFrame(
    command,
    headers = {},
    body = "",
    targetSocket = socket,
  ) {
    if (!targetSocket || targetSocket.readyState !== WebSocket.OPEN) {
      return;
    }

    let frame = command + "\n";

    Object.entries(headers).forEach(([key, value]) => {
      frame += `${key}:${value}\n`;
    });

    frame += "\n";

    frame += body;

    frame += "\0";

    targetSocket.send(frame);
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
   * CHAT MORE
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
   * CALL ELEMENTS
   * =========================================================
   */

  /*
   * VOICE
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

  const voiceCallAccept = document.getElementById("voiceCallAccept");

  const voiceCallDecline = document.getElementById("voiceCallDecline");

  const incomingVoiceActions = document.getElementById("incomingVoiceActions");

  /*
   * VIDEO
   */
  const videoCallButton = document.getElementById("videoCallButton");

  const videoCallOverlay = document.getElementById("videoCallOverlay");

  const videoCallClose = document.getElementById("videoCallClose");

  const videoCallEnd = document.getElementById("videoCallEnd");

  const videoCallMute = document.getElementById("videoCallMute");

  const videoCallCamera = document.getElementById("videoCallCamera");

  const videoCallUserName = document.getElementById("videoCallUserName");

  const videoCallUserImage = document.getElementById("videoCallUserImage");

  const videoCallRemoteImage = document.getElementById("videoCallRemoteImage");

  const videoCallDuration = document.getElementById("videoCallDuration");

  const videoCallAccept = document.getElementById("videoCallAccept");

  const videoCallDecline = document.getElementById("videoCallDecline");

  const incomingVideoActions = document.getElementById("incomingVideoActions");

  const videoCallRemoteVideo = document.getElementById("videoCallRemoteVideo");

  const videoCallLocalVideo = document.getElementById("videoCallLocalVideo");

  /*
   * =========================================================
   * CALL UI STATE
   * =========================================================
   */

  let microphoneMuted = false;

  let cameraOff = false;

  let speakerMuted = false;

  let callTimer = null;

  let callSeconds = 0;

  /*
   * =========================================================
   * OUTGOING VOICE
   * =========================================================
   */

  async function startVoiceCall() {
    if (!currentFriendId || !currentRoomId) {
      alert("กรุณาเลือกเพื่อนก่อนโทร");

      return;
    }

    if (
      typeof CPCall === "undefined" ||
      typeof CPCall.startFriendCall !== "function"
    ) {
      alert("ระบบโทรยังไม่พร้อม");

      return;
    }

    currentCallMode = "VOICE";

    try {
      await CPCall.startFriendCall(currentFriendId, currentRoomId, "VOICE");

      openVoiceCallUI(
        currentFriendName,
        currentFriendImage,
        "กำลังโทร...",
        false,
      );
    } catch (error) {
      console.error("เริ่มโทรเสียงไม่สำเร็จ:", error);

      alert("ไม่สามารถเริ่มโทรได้");
    }
  }

  /*
   * =========================================================
   * OUTGOING VIDEO
   * =========================================================
   */

  async function startVideoCall() {
    if (!currentFriendId || !currentRoomId) {
      alert("กรุณาเลือกเพื่อนก่อนโทร");

      return;
    }

    if (
      typeof CPCall === "undefined" ||
      typeof CPCall.startFriendCall !== "function"
    ) {
      alert("ระบบโทรยังไม่พร้อม");

      return;
    }

    currentCallMode = "VIDEO";

    try {
      await CPCall.startFriendCall(currentFriendId, currentRoomId, "VIDEO");

      openVideoCallUI(
        currentFriendName,
        currentFriendImage,
        "กำลังโทร...",
        false,
      );
    } catch (error) {
      console.error("เริ่มวิดีโอคอลไม่สำเร็จ:", error);

      alert("ไม่สามารถเริ่มโทรได้");
    }
  }

  /*
   * =========================================================
   * OPEN VOICE UI
   * =========================================================
   */

  function openVoiceCallUI(name, image, status, incoming = false) {
    closeVideoCall(false);

    if (voiceCallName) {
      voiceCallName.textContent = name || "เพื่อน";
    }

    if (voiceCallImage) {
      voiceCallImage.src = image || "/images/man.jpg";
    }

    if (voiceCallStatus) {
      voiceCallStatus.textContent = status || "";
    }

    resetVoiceCallUI();

    if (incomingVoiceActions) {
      incomingVoiceActions.style.display = incoming ? "flex" : "none";
    }

    if (voiceCallEnd) {
      voiceCallEnd.style.display = incoming ? "none" : "";
    }

    if (voiceCallMute) {
      voiceCallMute.style.display = incoming ? "none" : "";
    }

    if (voiceCallSpeaker) {
      voiceCallSpeaker.style.display = incoming ? "none" : "";
    }

    voiceCallOverlay?.classList.add("show");

    voiceCallOverlay?.classList.add("active");
  }

  /*
   * =========================================================
   * OPEN VIDEO UI
   * =========================================================
   */

  function openVideoCallUI(name, image, status, incoming = false) {
    closeVoiceCall(false);

    if (videoCallUserName) {
      videoCallUserName.textContent = name || "เพื่อน";
    }

    if (videoCallUserImage) {
      videoCallUserImage.src = image || "/images/man.jpg";
    }

    if (videoCallRemoteImage) {
      videoCallRemoteImage.src = image || "/images/man.jpg";
    }

    if (videoCallDuration) {
      videoCallDuration.textContent = "00:00";
    }

    resetVideoCallUI();

    if (incomingVideoActions) {
      incomingVideoActions.style.display = incoming ? "flex" : "none";
    }

    if (videoCallEnd) {
      videoCallEnd.style.display = incoming ? "none" : "";
    }

    if (videoCallMute) {
      videoCallMute.style.display = incoming ? "none" : "";
    }

    if (videoCallCamera) {
      videoCallCamera.style.display = incoming ? "none" : "";
    }

    videoCallOverlay?.classList.add("show");

    videoCallOverlay?.classList.add("active");

    stopCallTimer();

    if (videoCallRemoteVideo) {
      videoCallRemoteVideo.style.display = "none";
    }

    if (videoCallLocalVideo) {
      videoCallLocalVideo.style.display = "none";
    }
  }

  /*
   * =========================================================
   * RESET VOICE UI
   * =========================================================
   */

  function resetVoiceCallUI() {
    microphoneMuted = false;

    speakerMuted = false;

    if (voiceCallMute) {
      voiceCallMute.classList.remove("muted");

      const slash = voiceCallMute.querySelector(".voice-call-slash");

      if (slash) {
        slash.style.display = "none";
      }
    }

    if (voiceCallSpeaker) {
      voiceCallSpeaker.classList.remove("muted");

      const slash = voiceCallSpeaker.querySelector(".voice-call-slash");

      if (slash) {
        slash.style.display = "none";
      }
    }
  }

  /*
   * =========================================================
   * RESET VIDEO UI
   * =========================================================
   */

  function resetVideoCallUI() {
    microphoneMuted = false;

    cameraOff = false;

    if (videoCallMute) {
      videoCallMute.classList.remove("muted");

      const slash = videoCallMute.querySelector(".video-call-slash");

      const label = videoCallMute.querySelector("span:last-child");

      if (slash) {
        slash.style.display = "none";
      }

      if (label) {
        label.textContent = "ไมค์";
      }
    }

    if (videoCallCamera) {
      videoCallCamera.classList.remove("muted");

      const slash = videoCallCamera.querySelector(".video-call-slash");

      const label = videoCallCamera.querySelector("span:last-child");

      if (slash) {
        slash.style.display = "none";
      }

      if (label) {
        label.textContent = "กล้อง";
      }
    }
  }

  /*
   * =========================================================
   * INCOMING CALL LISTENER
   * =========================================================
   */

  async function setupIncomingCallListener() {
    if (
      typeof CPCall === "undefined" ||
      typeof CPCall.loadMe !== "function" ||
      typeof CPCall.connectWS !== "function"
    ) {
      console.error("CPCall ยังไม่พร้อมสำหรับ incoming call");

      return;
    }

    try {
      await CPCall.loadMe();

      CPCall.connectWS();
    } catch (error) {
      console.error("เชื่อม incoming call ไม่สำเร็จ:", error);
    }
  }

  /*
   * =========================================================
   * CALL SIGNAL
   * =========================================================
   */

  window.addEventListener("cp-call-signal", (event) => {
    const signal = event.detail;

    if (!signal) {
      return;
    }

    console.log("CALL SIGNAL:", signal);

    if (signal.type === "INVITE") {
      handleIncomingCall(signal);

      return;
    }

    if (signal.type === "ACCEPT") {
      handleCallAccepted(signal);

      return;
    }

    if (signal.type === "DECLINE") {
      handleCallDeclined(signal);

      return;
    }

    if (signal.type === "CANCEL") {
      handleCallCancelled(signal);

      return;
    }
  });

  /*
   * =========================================================
   * WEBRTC CONNECTED
   * =========================================================
   */

  window.addEventListener("cp-call-connected", (event) => {
    const mode = event.detail?.mode;

    if (mode === "VOICE") {
      if (voiceCallStatus) {
        voiceCallStatus.textContent = "กำลังสนทนา";
      }
    }

    if (mode === "VIDEO") {
      if (videoCallDuration) {
        videoCallDuration.textContent = "00:00";
      }

      startCallTimer();
    }
  });

  /*
   * =========================================================
   * INCOMING CALL
   * =========================================================
   */

  function handleIncomingCall(signal) {
    /*
     * ถ้ากำลังคุยสายอื่นอยู่
     */
    if (typeof CPCall !== "undefined" && CPCall.isActive?.()) {
      console.warn("กำลังมีสายอยู่ ไม่รับสายใหม่");

      return;
    }

    incomingCallSignal = signal;

    const mode = signal.mode === "VIDEO" ? "VIDEO" : "VOICE";

    currentCallMode = mode;

    if (mode === "VIDEO") {
      openVideoCallUI(
        signal.fromName || "เพื่อน",

        signal.fromImage || "/images/man.jpg",

        "สายวิดีโอเข้า",

        true,
      );
    } else {
      openVoiceCallUI(
        signal.fromName || "เพื่อน",

        signal.fromImage || "/images/man.jpg",

        "สายเรียกเข้า",

        true,
      );
    }
  }

  /*
   * =========================================================
   * INCOMING VOICE ACCEPT
   * =========================================================
   */

  if (voiceCallAccept) {
    voiceCallAccept.addEventListener("click", async () => {
      if (!incomingCallSignal) {
        return;
      }

      const signal = incomingCallSignal;

      try {
        await CPCall.acceptFriendCall(signal);

        incomingCallSignal = null;

        if (incomingVoiceActions) {
          incomingVoiceActions.style.display = "none";
        }

        if (voiceCallEnd) {
          voiceCallEnd.style.display = "";
        }

        if (voiceCallMute) {
          voiceCallMute.style.display = "";
        }

        if (voiceCallSpeaker) {
          voiceCallSpeaker.style.display = "";
        }

        if (voiceCallStatus) {
          voiceCallStatus.textContent = "กำลังเชื่อมต่อ...";
        }
      } catch (error) {
        console.error("รับสายเสียงไม่สำเร็จ:", error);

        if (voiceCallStatus) {
          voiceCallStatus.textContent = "เชื่อมต่อไม่สำเร็จ";
        }
      }
    });
  }

  /*
   * =========================================================
   * INCOMING VOICE DECLINE
   * =========================================================
   */

  if (voiceCallDecline) {
    voiceCallDecline.addEventListener("click", async () => {
      if (incomingCallSignal) {
        try {
          await CPCall.declineFriendCall(incomingCallSignal);
        } catch (error) {
          console.error("ปฏิเสธสายไม่สำเร็จ:", error);
        }
      }

      incomingCallSignal = null;

      closeVoiceCall();
    });
  }

  /*
   * =========================================================
   * INCOMING VIDEO ACCEPT
   * =========================================================
   */

  if (videoCallAccept) {
    videoCallAccept.addEventListener("click", async () => {
      if (!incomingCallSignal) {
        return;
      }

      const signal = incomingCallSignal;

      try {
        await CPCall.acceptFriendCall(signal);

        incomingCallSignal = null;

        if (incomingVideoActions) {
          incomingVideoActions.style.display = "none";
        }

        if (videoCallEnd) {
          videoCallEnd.style.display = "";
        }

        if (videoCallMute) {
          videoCallMute.style.display = "";
        }

        if (videoCallCamera) {
          videoCallCamera.style.display = "";
        }

        if (videoCallDuration) {
          videoCallDuration.textContent = "00:00";
        }

        if (videoCallStatus) {
          videoCallStatus.textContent = "กำลังเชื่อมต่อ...";
        }
      } catch (error) {
        console.error("รับสายวิดีโอไม่สำเร็จ:", error);
      }
    });
  }

  /*
   * =========================================================
   * INCOMING VIDEO DECLINE
   * =========================================================
   */

  if (videoCallDecline) {
    videoCallDecline.addEventListener("click", async () => {
      if (incomingCallSignal) {
        try {
          await CPCall.declineFriendCall(incomingCallSignal);
        } catch (error) {
          console.error("ปฏิเสธวิดีโอไม่สำเร็จ:", error);
        }
      }

      incomingCallSignal = null;

      closeVideoCall();

      stopCallTimer();
    });
  }

  /*
   * =========================================================
   * CALL ACCEPTED
   * =========================================================
   *
   * สำคัญ:
   * caller เป็นคน JOIN หลังจากได้รับ ACCEPT
   *
   * ไม่ต้อง startMedia ตอนกดโทร
   * เพราะจะขอ permission ตอนเพื่อนรับสายแล้ว
   * =========================================================
   */

  async function handleCallAccepted(signal) {
    console.log("เพื่อนรับสายแล้ว:", signal);

    const roomId = signal.roomId || currentRoomId;

    const mode = signal.mode === "VIDEO" ? "VIDEO" : currentCallMode || "VOICE";

    if (!roomId) {
      console.error("ไม่พบ roomId หลังรับสาย");

      return;
    }

    currentRoomId = roomId;

    currentCallMode = mode;

    try {
      await CPCall.joinRoomCall(roomId, mode);

      if (mode === "VOICE") {
        if (voiceCallStatus) {
          voiceCallStatus.textContent = "กำลังเชื่อมต่อ...";
        }
      }

      if (mode === "VIDEO") {
        if (videoCallDuration) {
          videoCallDuration.textContent = "00:00";
        }
      }
    } catch (error) {
      console.error("เริ่ม WebRTC ไม่สำเร็จ:", error);

      if (mode === "VOICE") {
        if (voiceCallStatus) {
          voiceCallStatus.textContent = "เชื่อมต่อไม่สำเร็จ";
        }
      }

      if (mode === "VIDEO") {
        stopCallTimer();
      }
    }
  }

  /*
   * =========================================================
   * DECLINED
   * =========================================================
   */

  function handleCallDeclined(signal) {
    console.log("เพื่อนปฏิเสธสาย:", signal);

    if (currentCallMode === "VOICE") {
      if (voiceCallStatus) {
        voiceCallStatus.textContent = "ไม่รับสาย";
      }

      setTimeout(() => {
        closeVoiceCall();

        if (typeof CPCall !== "undefined" && CPCall.isActive?.()) {
          CPCall.leaveCall();
        }
      }, 600);
    }

    if (currentCallMode === "VIDEO") {
      setTimeout(() => {
        closeVideoCall();

        if (typeof CPCall !== "undefined" && CPCall.isActive?.()) {
          CPCall.leaveCall();
        }
      }, 600);
    }

    currentCallMode = null;

    incomingCallSignal = null;

    stopCallTimer();
  }

  /*
   * =========================================================
   * CANCELLED
   * =========================================================
   */

  function handleCallCancelled(signal) {
    console.log("สายถูกยกเลิก:", signal);

    incomingCallSignal = null;

    /*
     * ถ้า active อยู่
     * ต้องหยุด WebRTC ด้วย
     */
    if (typeof CPCall !== "undefined" && CPCall.isActive?.()) {
      CPCall.leaveCall();
    }

    closeVoiceCall(false);

    closeVideoCall(false);

    currentCallMode = null;

    currentRoomId = null;

    stopCallTimer();
  }

  /*
   * =========================================================
   * VOICE BUTTON
   * =========================================================
   */

  if (voiceCallButton) {
    voiceCallButton.addEventListener("click", () => {
      startVoiceCall();
    });
  }

  /*
   * =========================================================
   * VIDEO BUTTON
   * =========================================================
   */

  if (videoCallButton) {
    videoCallButton.addEventListener("click", () => {
      startVideoCall();
    });
  }

  /*
   * =========================================================
   * CLOSE VOICE
   * =========================================================
   */

  function closeVoiceCall(shouldStopCall = true) {
    if (voiceCallOverlay) {
      voiceCallOverlay.classList.remove("show");

      voiceCallOverlay.classList.remove("active");
    }

    if (incomingVoiceActions) {
      incomingVoiceActions.style.display = "none";
    }

    if (voiceCallEnd) {
      voiceCallEnd.style.display = "";
    }

    if (voiceCallMute) {
      voiceCallMute.style.display = "";
    }

    if (voiceCallSpeaker) {
      voiceCallSpeaker.style.display = "";
    }

    if (shouldStopCall) {
      stopCallTimer();
    }
  }

  /*
   * =========================================================
   * CLOSE VOICE BUTTON
   * =========================================================
   */

  if (voiceCallClose) {
    voiceCallClose.addEventListener("click", async () => {
      /*
       * ถ้ายัง ringing
       * ส่ง CANCEL
       */
      if (currentCallMode === "VOICE") {
        try {
          await CPCall.cancelFriendCall();
        } catch (error) {
          console.warn("ส่ง CANCEL ไม่สำเร็จ:", error);
        }
      }

      /*
       * ถ้า active
       * leave
       */
      if (CPCall.isActive?.()) {
        CPCall.leaveCall();
      }

      closeVoiceCall();

      currentCallMode = null;

      incomingCallSignal = null;
    });
  }

  /*
   * =========================================================
   * END VOICE
   * =========================================================
   */

  if (voiceCallEnd) {
    voiceCallEnd.addEventListener("click", () => {
      if (voiceCallStatus) {
        voiceCallStatus.textContent = "สิ้นสุดการโทร";
      }

      CPCall.leaveCall();

      setTimeout(() => {
        closeVoiceCall();

        currentCallMode = null;
      }, 300);
    });
  }

  /*
   * =========================================================
   * VOICE MUTE
   * =========================================================
   */

  if (voiceCallMute) {
    voiceCallMute.addEventListener("click", () => {
      const enabled = CPCall.toggleMicrophone();

      microphoneMuted = !enabled;

      voiceCallMute.classList.toggle("muted", microphoneMuted);

      const slash = voiceCallMute.querySelector(".voice-call-slash");

      if (slash) {
        slash.style.display = microphoneMuted ? "block" : "none";
      }
    });
  }

  /*
   * =========================================================
   * VOICE SPEAKER
   * =========================================================
   *
   * ตอนนี้ไม่มี toggleSpeaker ใน call.js
   *
   * ดังนั้นปุ่มนี้ทำหน้าที่ UI เท่านั้น
   *
   * ไม่ได้ปิดเสียง remote จริง
   * =========================================================
   */

  if (voiceCallSpeaker) {
    voiceCallSpeaker.addEventListener("click", () => {
      speakerMuted = !speakerMuted;

      voiceCallSpeaker.classList.toggle("muted", speakerMuted);

      const slash = voiceCallSpeaker.querySelector(".voice-call-slash");

      if (slash) {
        slash.style.display = speakerMuted ? "block" : "none";
      }

      /*
       * remote audio
       */
      const remoteAudio = document.getElementById("remoteCallAudio");

      if (remoteAudio) {
        remoteAudio.muted = speakerMuted;
      }
    });
  }

  /*
   * =========================================================
   * CLOSE VIDEO
   * =========================================================
   */

  function closeVideoCall(shouldStopCall = true) {
    if (videoCallOverlay) {
      videoCallOverlay.classList.remove("show");

      videoCallOverlay.classList.remove("active");
    }

    if (incomingVideoActions) {
      incomingVideoActions.style.display = "none";
    }

    if (videoCallEnd) {
      videoCallEnd.style.display = "";
    }

    if (videoCallMute) {
      videoCallMute.style.display = "";
    }

    if (videoCallCamera) {
      videoCallCamera.style.display = "";
    }

    if (shouldStopCall) {
      stopCallTimer();
    }
  }

  /*
   * =========================================================
   * CLOSE VIDEO BUTTON
   * =========================================================
   */

  if (videoCallClose) {
    videoCallClose.addEventListener("click", async () => {
      if (currentCallMode === "VIDEO") {
        try {
          await CPCall.cancelFriendCall();
        } catch (error) {
          console.warn("ส่ง CANCEL ไม่สำเร็จ:", error);
        }
      }

      if (CPCall.isActive?.()) {
        CPCall.leaveCall();
      }

      closeVideoCall();

      currentCallMode = null;

      incomingCallSignal = null;
    });
  }

  /*
   * =========================================================
   * END VIDEO
   * =========================================================
   */

  if (videoCallEnd) {
    videoCallEnd.addEventListener("click", () => {
      CPCall.leaveCall();

      closeVideoCall();

      currentCallMode = null;

      incomingCallSignal = null;
    });
  }

  /*
   * =========================================================
   * VIDEO MUTE
   * =========================================================
   */

  if (videoCallMute) {
    videoCallMute.addEventListener("click", () => {
      const enabled = CPCall.toggleMicrophone();

      microphoneMuted = !enabled;

      videoCallMute.classList.toggle("muted", microphoneMuted);

      const slash = videoCallMute.querySelector(".video-call-slash");

      const label = videoCallMute.querySelector("span:last-child");

      if (slash) {
        slash.style.display = microphoneMuted ? "block" : "none";
      }

      if (label) {
        label.textContent = microphoneMuted ? "เปิดไมค์" : "ไมค์";
      }
    });
  }

  /*
   * =========================================================
   * VIDEO CAMERA
   * =========================================================
   */

  if (videoCallCamera) {
    videoCallCamera.addEventListener("click", () => {
      const enabled = CPCall.toggleCamera();

      cameraOff = !enabled;

      videoCallCamera.classList.toggle("muted", cameraOff);

      const slash = videoCallCamera.querySelector(".video-call-slash");

      const label = videoCallCamera.querySelector("span:last-child");

      if (slash) {
        slash.style.display = cameraOff ? "block" : "none";
      }

      if (label) {
        label.textContent = cameraOff ? "เปิดกล้อง" : "กล้อง";
      }

      if (videoCallLocalVideo) {
        videoCallLocalVideo.style.display = cameraOff ? "none" : "block";
      }
    });
  }

  /*
   * =========================================================
   * TIMER
   * =========================================================
   */

  function startCallTimer() {
    stopCallTimer();

    callSeconds = 0;

    if (videoCallDuration) {
      videoCallDuration.textContent = "00:00";
    }

    callTimer = setInterval(() => {
      callSeconds++;

      const minutes = String(Math.floor(callSeconds / 60)).padStart(2, "0");

      const seconds = String(callSeconds % 60).padStart(2, "0");

      if (videoCallDuration) {
        videoCallDuration.textContent = `${minutes}:${seconds}`;
      }
    }, 1000);
  }

  function stopCallTimer() {
    if (callTimer) {
      clearInterval(callTimer);

      callTimer = null;
    }

    callSeconds = 0;
  }

  /*
   * =========================================================
   * CLICK OUTSIDE VOICE
   * =========================================================
   */

  if (voiceCallOverlay) {
    voiceCallOverlay.addEventListener("click", (event) => {
      if (event.target !== voiceCallOverlay) {
        return;
      }

      /*
       * ถ้าคลิกพื้นหลัง
       * ไม่ควรตัดสายโดยไม่ตั้งใจ
       *
       * แค่ปิด UI ตอน ringing
       * แต่ยังให้ปุ่ม X เป็นตัวควบคุมจริง
       */
      if (!CPCall.isActive?.()) {
        closeVoiceCall();
      }
    });
  }

  /*
   * =========================================================
   * CLICK OUTSIDE VIDEO
   * =========================================================
   */

  if (videoCallOverlay) {
    videoCallOverlay.addEventListener("click", (event) => {
      if (event.target !== videoCallOverlay) {
        return;
      }

      if (!CPCall.isActive?.()) {
        closeVideoCall();
      }
    });
  }

  /*
   * =========================================================
   * ESC
   * =========================================================
   */

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") {
      return;
    }

    /*
     * ไม่ตัด active call
     * เพื่อไม่ให้กด ESC แล้วสายหลุด
     */
    if (videoCallOverlay && videoCallOverlay.classList.contains("show")) {
      if (!CPCall.isActive?.()) {
        closeVideoCall();
      }

      return;
    }

    if (voiceCallOverlay && voiceCallOverlay.classList.contains("show")) {
      if (!CPCall.isActive?.()) {
        closeVoiceCall();
      }
    }
  });

  /*
   * =========================================================
   * START PAGE
   * =========================================================
   */

  async function startFriendPage() {
    await loadCurrentUser();

    /*
     * incoming call
     */
    await setupIncomingCallListener();

    /*
     * friends
     */
    await loadFriends();
  }

  /*
   * =========================================================
   * PAGE HIDE
   * =========================================================
   */

  window.addEventListener("pagehide", () => {
    disconnectRoomWebSocket();

    if (
      typeof CPCall !== "undefined" &&
      typeof CPCall.leaveCall === "function"
    ) {
      CPCall.leaveCall();
    }
  });

  /*
   * =========================================================
   * RUN
   * =========================================================
   */

  startFriendPage();
});
