document.addEventListener("DOMContentLoaded", async () => {
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

  let currentFriendId = null;
  let currentRoomId = null;
  let currentFriendName = "";
  let currentFriendImage = "/images/man.jpg";
  let currentUserId = null;

  let socket = null;
  let stompConnected = false;

  /*
   * =========================================================
   * CURRENT USER
   * =========================================================
   */

  async function loadCurrentUser() {
    try {
      const response = await fetch("/api/users/me", {
        method: "GET",
        headers: { Accept: "application/json" },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(
          `โหลดข้อมูลผู้ใช้ปัจจุบันไม่สำเร็จ (${response.status})`,
        );
      }

      const user = await response.json();
      currentUserId = user.id || user.userId || user.user?.id || null;
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
          <div class="friend-empty">ยังไม่มีเพื่อน</div>
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

    const image = friend.imageUrl || "/images/man.jpg";

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

    document.querySelectorAll(".friend-list-item").forEach((item) => {
      item.classList.remove("active");
    });

    friend.classList.add("active");

    currentFriendId = friendId;
    currentFriendName = friend.dataset.name || "เพื่อน";
    currentFriendImage = friend.dataset.image || "/images/man.jpg";

    chatUserName.textContent = currentFriendName;
    chatUserYear.textContent = friend.dataset.year || "";
    chatUserImage.src = currentFriendImage;

    /*
     * ข้อมูลสำหรับ Friend Profile Popup
     */
    chatUserProfile.dataset.profile = "";
    chatUserProfile.dataset.id = currentFriendId;
    chatUserProfile.dataset.name = currentFriendName;
    chatUserProfile.dataset.year = friend.dataset.year || "";
    chatUserProfile.dataset.image = currentFriendImage;
    chatUserProfile.dataset.interests = friend.dataset.interests || "";
    chatUserProfile.dataset.bio = friend.dataset.bio || "";
    chatUserProfile.dataset.status = "friend";

    chatMessages.innerHTML = `
      <div class="friend-empty">
        กำลังเปิดห้องแชท...
      </div>
    `;

    chatMoreMenu.classList.remove("show");

    try {
      /*
       * ถ้ามีห้องอยู่แล้ว Backend จะคืนห้องเดิม
       * ถ้ายังไม่มี Backend จะสร้างห้องใหม่
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

      await loadMessages();

      connectRoomWebSocket();
    } catch (error) {
      console.error("เปิดห้องแชทล้มเหลว:", error);

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

    try {
      const response = await fetch(`/api/chats/${currentRoomId}/messages`, {
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
       * ถ้า WebSocket ทำงานปกติ
       * ข้อความจะถูกส่งกลับมาทาง WebSocket
       *
       * ถ้า WebSocket ยังไม่ทัน connect
       * ให้แสดง response ที่ได้จาก POST เอง
       */
      if (!stompConnected) {
        removeEmptyMessage();
        appendMessage(message);
        scrollToBottom();
      }

      messageInput.value = "";
      messageInput.focus();
    } catch (error) {
      console.error("ส่งข้อความล้มเหลว:", error);
      alert("ไม่สามารถส่งข้อความได้");
    }
  });

  /*
   * Enter = ส่ง
   * Shift + Enter = ขึ้นบรรทัดใหม่
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
     * ถ้าไม่ใช่ TEXT ตอนนี้ยังไม่ render
     */
    if (message.messageType && message.messageType !== "TEXT") {
      return;
    }

    removeEmptyMessage();

    const wrapper = document.createElement("div");

    /*
     * ใช้ senderId เพื่อแยกข้อความของเรา / ของเพื่อน
     *
     * ถ้าไม่มี current user id
     * ใช้ class received เป็นค่าเริ่มต้น
     */
    const isMine =
      currentUserId != null &&
      message.senderId != null &&
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
   */

  function connectRoomWebSocket() {
    if (!currentRoomId) {
      return;
    }

    /*
     * ปิด connection เก่าก่อน
     */
    if (socket) {
      try {
        socket.close();
      } catch (error) {
        console.warn("ปิด WebSocket เดิมไม่ได้:", error);
      }

      socket = null;
    }

    stompConnected = false;

    socket = new WebSocket(
      `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`,
    );

    socket.onopen = () => {
      console.log("Friend WebSocket connected");

      sendStompFrame("CONNECT", {
        "accept-version": "1.2",
        "heart-beat": "0,0",
      });
    };

    socket.onmessage = (event) => {
      const frame = parseStompFrame(event.data);

      if (!frame) {
        return;
      }

      /*
       * STOMP CONNECTED
       */
      if (frame.command === "CONNECTED") {
        stompConnected = true;

        console.log("Friend STOMP connected");

        sendStompFrame("SUBSCRIBE", {
          id: `friend-room-${currentRoomId}`,
          destination: `/topic/rooms/${currentRoomId}`,
          ack: "auto",
        });

        return;
      }

      /*
       * STOMP MESSAGE
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

      if (socket) {
        try {
          socket.close();
        } catch (error) {
          console.warn(error);
        }
      }

      currentFriendId = null;
      currentRoomId = null;

      await loadFriends();

      chatMoreMenu.classList.remove("show");
    } catch (error) {
      console.error("เลิกเป็นเพื่อนล้มเหลว:", error);
      alert("ไม่สามารถเลิกเป็นเพื่อนได้");
    }
  });

  /*
   * =========================================================
   * IMAGE
   * =========================================================
   *
   * ตอนนี้ยังเป็น preview prototype
   * เพราะ backend ยังไม่มี endpoint upload image โดยตรง
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
    currentFriendId = null;
    currentRoomId = null;

    chatUserName.textContent = "เลือกเพื่อน";
    chatUserYear.textContent = "";
    chatUserImage.src = "/images/man.jpg";

    chatUserProfile.removeAttribute("data-profile");
    chatUserProfile.removeAttribute("data-id");

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
   * CALL UI ELEMENTS
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
  const voiceCallAccept = document.getElementById("voiceCallAccept");
  const voiceCallDecline = document.getElementById("voiceCallDecline");
  const incomingVoiceActions = document.getElementById("incomingVoiceActions");

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

  let currentCallMode = null;
  let incomingCallSignal = null;
  let microphoneMuted = false;
  let cameraOff = false;
  let speakerMuted = false;
  let callTimer = null;
  let callSeconds = 0;

  function resetCallControls() {
    microphoneMuted = false;
    cameraOff = false;
    speakerMuted = false;

    [voiceCallMute, voiceCallSpeaker, videoCallMute, videoCallCamera].forEach(
      (button) => {
        button?.classList.remove("muted");
      },
    );

    voiceCallMute
      ?.querySelector(".voice-call-slash")
      ?.style.setProperty("display", "none");
    voiceCallSpeaker
      ?.querySelector(".voice-call-slash")
      ?.style.setProperty("display", "none");
    videoCallMute
      ?.querySelector(".video-call-slash")
      ?.style.setProperty("display", "none");
    videoCallCamera
      ?.querySelector(".video-call-slash")
      ?.style.setProperty("display", "none");

    if (videoCallMute)
      videoCallMute
        .querySelector("span:last-child")
        ?.replaceChildren(document.createTextNode("ไมค์"));
    if (videoCallCamera)
      videoCallCamera
        .querySelector("span:last-child")
        ?.replaceChildren(document.createTextNode("กล้อง"));
  }

  function startCallTimer() {
    stopCallTimer();
    callSeconds = 0;
    if (videoCallDuration) videoCallDuration.textContent = "00:00";
    callTimer = setInterval(() => {
      callSeconds += 1;
      const minutes = String(Math.floor(callSeconds / 60)).padStart(2, "0");
      const seconds = String(callSeconds % 60).padStart(2, "0");
      if (videoCallDuration)
        videoCallDuration.textContent = `${minutes}:${seconds}`;
    }, 1000);
  }

  function stopCallTimer() {
    if (callTimer) clearInterval(callTimer);
    callTimer = null;
    callSeconds = 0;
  }

  function openVoiceCallUI(name, image, status, incoming = false) {
    closeVideoCall(false);
    resetCallControls();
    if (voiceCallName) voiceCallName.textContent = name || "เพื่อน";
    if (voiceCallImage) voiceCallImage.src = image || "/images/man.jpg";
    if (voiceCallStatus) voiceCallStatus.textContent = status || "";
    if (incomingVoiceActions)
      incomingVoiceActions.style.display = incoming ? "flex" : "none";
    if (voiceCallEnd) voiceCallEnd.style.display = incoming ? "none" : "";
    if (voiceCallMute) voiceCallMute.style.display = incoming ? "none" : "";
    if (voiceCallSpeaker)
      voiceCallSpeaker.style.display = incoming ? "none" : "";
    voiceCallOverlay?.classList.add("show", "active");
  }

  function openVideoCallUI(name, image, status, incoming = false) {
    closeVoiceCall(false);
    resetCallControls();
    if (videoCallUserName) videoCallUserName.textContent = name || "เพื่อน";
    if (videoCallUserImage) videoCallUserImage.src = image || "/images/man.jpg";
    if (videoCallRemoteImage)
      videoCallRemoteImage.src = image || "/images/man.jpg";
    if (videoCallDuration) videoCallDuration.textContent = "00:00";
    if (incomingVideoActions)
      incomingVideoActions.style.display = incoming ? "flex" : "none";
    if (videoCallEnd) videoCallEnd.style.display = incoming ? "none" : "";
    if (videoCallMute) videoCallMute.style.display = incoming ? "none" : "";
    if (videoCallCamera) videoCallCamera.style.display = incoming ? "none" : "";
    if (videoCallRemoteVideo) videoCallRemoteVideo.style.display = "none";
    if (videoCallLocalVideo) videoCallLocalVideo.style.display = "none";
    stopCallTimer();
    videoCallOverlay?.classList.add("show", "active");
  }

  function closeVoiceCall(stopTimer = true) {
    voiceCallOverlay?.classList.remove("show", "active");
    if (incomingVoiceActions) incomingVoiceActions.style.display = "none";
    if (voiceCallEnd) voiceCallEnd.style.display = "";
    if (voiceCallMute) voiceCallMute.style.display = "";
    if (voiceCallSpeaker) voiceCallSpeaker.style.display = "";
    if (stopTimer) stopCallTimer();
  }

  function closeVideoCall(stopTimer = true) {
    videoCallOverlay?.classList.remove("show", "active");
    if (incomingVideoActions) incomingVideoActions.style.display = "none";
    if (videoCallEnd) videoCallEnd.style.display = "";
    if (videoCallMute) videoCallMute.style.display = "";
    if (videoCallCamera) videoCallCamera.style.display = "";
    if (stopTimer) stopCallTimer();
  }

  async function startVoiceCall() {
    if (!currentFriendId || !currentRoomId)
      return alert("กรุณาเลือกเพื่อนก่อนโทร");
    try {
      currentCallMode = "VOICE";
      await CPCall.startFriendCall(currentFriendId, currentRoomId, "VOICE");
      openVoiceCallUI(
        currentFriendName,
        currentFriendImage,
        "กำลังโทร...",
        false,
      );
    } catch (error) {
      console.error("เริ่มโทรเสียงไม่สำเร็จ:", error);
      currentCallMode = null;
      alert("ไม่สามารถเริ่มโทรได้");
    }
  }

  async function startVideoCall() {
    if (!currentFriendId || !currentRoomId)
      return alert("กรุณาเลือกเพื่อนก่อนโทร");
    try {
      currentCallMode = "VIDEO";
      await CPCall.startFriendCall(currentFriendId, currentRoomId, "VIDEO");
      openVideoCallUI(
        currentFriendName,
        currentFriendImage,
        "กำลังโทร...",
        false,
      );
    } catch (error) {
      console.error("เริ่มวิดีโอคอลไม่สำเร็จ:", error);
      currentCallMode = null;
      alert("ไม่สามารถเริ่มโทรได้");
    }
  }

  async function cancelOutgoingCall() {
    try {
      await CPCall.cancelFriendCall();
    } catch (error) {
      console.warn("ส่ง CANCEL ไม่สำเร็จ:", error);
    }
    currentCallMode = null;
    incomingCallSignal = null;
    closeVoiceCall();
    closeVideoCall();
  }

  window.addEventListener("cp-call-connected", (event) => {
    const mode = event.detail?.mode;
    if (mode === "VOICE" && voiceCallStatus)
      voiceCallStatus.textContent = "กำลังสนทนา";
    if (mode === "VIDEO") startCallTimer();
  });

  window.addEventListener("cp-call-signal", async (event) => {
    const signal = event.detail;
    if (!signal) return;

    if (signal.type === "INVITE") {
      if (CPCall.isActive?.() || incomingCallSignal) return;
      incomingCallSignal = signal;
      const mode = signal.mode === "VIDEO" ? "VIDEO" : "VOICE";
      currentCallMode = mode;
      if (mode === "VIDEO") {
        openVideoCallUI(
          signal.fromName,
          signal.fromImage,
          "สายวิดีโอเข้า",
          true,
        );
      } else {
        openVoiceCallUI(
          signal.fromName,
          signal.fromImage,
          "สายเรียกเข้า",
          true,
        );
      }
      return;
    }

    if (signal.type === "ACCEPT") {
      const roomId = signal.roomId || currentRoomId;
      const mode =
        signal.mode === "VIDEO" ? "VIDEO" : currentCallMode || "VOICE";
      if (!roomId) return;
      currentRoomId = roomId;
      currentCallMode = mode;
      try {
        await CPCall.joinRoomCall(roomId, mode);
        if (mode === "VOICE" && voiceCallStatus)
          voiceCallStatus.textContent = "กำลังเชื่อมต่อ...";
        if (mode === "VIDEO" && videoCallDuration)
          videoCallDuration.textContent = "00:00";
      } catch (error) {
        console.error("เริ่ม WebRTC ไม่สำเร็จ:", error);
        closeVoiceCall();
        closeVideoCall();
        currentCallMode = null;
      }
      return;
    }

    if (signal.type === "DECLINE") {
      if (currentCallMode === "VOICE") {
        if (voiceCallStatus) voiceCallStatus.textContent = "ไม่รับสาย";
        setTimeout(() => closeVoiceCall(), 500);
      } else if (currentCallMode === "VIDEO") {
        setTimeout(() => closeVideoCall(), 500);
      }
      if (CPCall.isActive?.()) CPCall.leaveCall();
      currentCallMode = null;
      incomingCallSignal = null;
      stopCallTimer();
      return;
    }

    if (signal.type === "CANCEL") {
      incomingCallSignal = null;
      if (CPCall.isActive?.()) CPCall.leaveCall();
      closeVoiceCall(false);
      closeVideoCall(false);
      currentCallMode = null;
      currentRoomId = null;
      stopCallTimer();
    }
  });

  if (voiceCallButton)
    voiceCallButton.addEventListener("click", startVoiceCall);
  if (videoCallButton)
    videoCallButton.addEventListener("click", startVideoCall);

  if (voiceCallAccept)
    voiceCallAccept.addEventListener("click", async () => {
      if (!incomingCallSignal) return;
      const signal = incomingCallSignal;
      try {
        await CPCall.acceptFriendCall(signal);
        incomingCallSignal = null;
        if (incomingVoiceActions) incomingVoiceActions.style.display = "none";
        if (voiceCallEnd) voiceCallEnd.style.display = "";
        if (voiceCallMute) voiceCallMute.style.display = "";
        if (voiceCallSpeaker) voiceCallSpeaker.style.display = "";
        if (voiceCallStatus) voiceCallStatus.textContent = "กำลังเชื่อมต่อ...";
      } catch (error) {
        console.error("รับสายเสียงไม่สำเร็จ:", error);
        if (voiceCallStatus) voiceCallStatus.textContent = "เชื่อมต่อไม่สำเร็จ";
      }
    });

  if (voiceCallDecline)
    voiceCallDecline.addEventListener("click", async () => {
      const signal = incomingCallSignal;
      if (signal) {
        try {
          await CPCall.declineFriendCall(signal);
        } catch (error) {
          console.error("ปฏิเสธสายไม่สำเร็จ:", error);
        }
      }
      incomingCallSignal = null;
      currentCallMode = null;
      closeVoiceCall();
    });

  if (videoCallAccept)
    videoCallAccept.addEventListener("click", async () => {
      if (!incomingCallSignal) return;
      const signal = incomingCallSignal;
      try {
        await CPCall.acceptFriendCall(signal);
        incomingCallSignal = null;
        if (incomingVideoActions) incomingVideoActions.style.display = "none";
        if (videoCallEnd) videoCallEnd.style.display = "";
        if (videoCallMute) videoCallMute.style.display = "";
        if (videoCallCamera) videoCallCamera.style.display = "";
        if (videoCallDuration) videoCallDuration.textContent = "00:00";
      } catch (error) {
        console.error("รับสายวิดีโอไม่สำเร็จ:", error);
        closeVideoCall();
      }
    });

  if (videoCallDecline)
    videoCallDecline.addEventListener("click", async () => {
      const signal = incomingCallSignal;
      if (signal) {
        try {
          await CPCall.declineFriendCall(signal);
        } catch (error) {
          console.error("ปฏิเสธวิดีโอไม่สำเร็จ:", error);
        }
      }
      incomingCallSignal = null;
      currentCallMode = null;
      closeVideoCall();
    });

  if (voiceCallClose)
    voiceCallClose.addEventListener("click", async () => {
      if (CPCall.isActive?.()) CPCall.leaveCall();
      else if (currentCallMode === "VOICE") await cancelOutgoingCall();
      closeVoiceCall();
      currentCallMode = null;
      incomingCallSignal = null;
    });

  if (voiceCallEnd)
    voiceCallEnd.addEventListener("click", () => {
      CPCall.leaveCall();
      closeVoiceCall();
      currentCallMode = null;
      incomingCallSignal = null;
    });

  if (videoCallClose)
    videoCallClose.addEventListener("click", async () => {
      if (CPCall.isActive?.()) CPCall.leaveCall();
      else if (currentCallMode === "VIDEO") await cancelOutgoingCall();
      closeVideoCall();
      currentCallMode = null;
      incomingCallSignal = null;
    });

  if (videoCallEnd)
    videoCallEnd.addEventListener("click", () => {
      CPCall.leaveCall();
      closeVideoCall();
      currentCallMode = null;
      incomingCallSignal = null;
    });

  if (voiceCallMute)
    voiceCallMute.addEventListener("click", () => {
      const enabled = CPCall.toggleMicrophone();
      microphoneMuted = !enabled;
      voiceCallMute.classList.toggle("muted", microphoneMuted);
      voiceCallMute
        .querySelector(".voice-call-slash")
        ?.style.setProperty("display", microphoneMuted ? "block" : "none");
    });

  if (voiceCallSpeaker)
    voiceCallSpeaker.addEventListener("click", () => {
      speakerMuted = !speakerMuted;
      voiceCallSpeaker.classList.toggle("muted", speakerMuted);
      voiceCallSpeaker
        .querySelector(".voice-call-slash")
        ?.style.setProperty("display", speakerMuted ? "block" : "none");
      const audio = document.getElementById("remoteCallAudio");
      if (audio) audio.muted = speakerMuted;
    });

  if (videoCallMute)
    videoCallMute.addEventListener("click", () => {
      const enabled = CPCall.toggleMicrophone();
      microphoneMuted = !enabled;
      videoCallMute.classList.toggle("muted", microphoneMuted);
      videoCallMute
        .querySelector(".video-call-slash")
        ?.style.setProperty("display", microphoneMuted ? "block" : "none");
      const label = videoCallMute.querySelector("span:last-child");
      if (label) label.textContent = microphoneMuted ? "เปิดไมค์" : "ไมค์";
    });

  if (videoCallCamera)
    videoCallCamera.addEventListener("click", () => {
      const enabled = CPCall.toggleCamera();
      cameraOff = !enabled;
      videoCallCamera.classList.toggle("muted", cameraOff);
      videoCallCamera
        .querySelector(".video-call-slash")
        ?.style.setProperty("display", cameraOff ? "block" : "none");
      const label = videoCallCamera.querySelector("span:last-child");
      if (label) label.textContent = cameraOff ? "เปิดกล้อง" : "กล้อง";
      if (videoCallLocalVideo)
        videoCallLocalVideo.style.display = cameraOff ? "none" : "block";
    });

  if (voiceCallOverlay)
    voiceCallOverlay.addEventListener("click", (event) => {
      if (event.target === voiceCallOverlay && !CPCall.isActive?.())
        closeVoiceCall();
    });

  if (videoCallOverlay)
    videoCallOverlay.addEventListener("click", (event) => {
      if (event.target === videoCallOverlay && !CPCall.isActive?.())
        closeVideoCall();
    });

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    if (CPCall.isActive?.()) return;
    if (videoCallOverlay?.classList.contains("show")) closeVideoCall();
    if (voiceCallOverlay?.classList.contains("show")) closeVoiceCall();
  });

  window.addEventListener("pagehide", () => {
    disconnectRoomWebSocket();
    if (typeof CPCall !== "undefined" && CPCall.leaveCall) CPCall.leaveCall();
  });

  /*
   * =========================================================
   * START
   * =========================================================
   */

  await loadCurrentUser();
  loadFriends();
});
