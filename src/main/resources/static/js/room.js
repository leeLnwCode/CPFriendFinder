document.addEventListener("DOMContentLoaded", function () {
  // =========================
  // LOG COLLECTOR (debug)
  // สะสม log ทุก event ของ WebSocket/STOMP/WebRTC
  // =========================

  const _logs = [];
  const _origConsole = {
    log:   console.log.bind(console),
    info:  console.info.bind(console),
    warn:  console.warn.bind(console),
    error: console.error.bind(console),
    debug: console.debug.bind(console),
  };

  function _logLine(level, args) {
    const ts = new Date().toISOString();
    const msg = args.map(a => {
      try { return typeof a === "object" ? JSON.stringify(a) : String(a); }
      catch (_) { return String(a); }
    }).join(" ");
    _logs.push(`[${ts}] [${level.toUpperCase()}] ${msg}`);
    if (_logs.length > 5000) _logs.shift(); // ป้องกัน memory leak
  }

  ["log","info","warn","error","debug"].forEach(level => {
    console[level] = (...args) => {
      _origConsole[level](...args);
      _logLine(level, args);
    };
  });

  function downloadLogs() {
    const text = _logs.join("\n");
    const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement("a");
    a.href     = url;
    a.download = `room-debug-${new Date().toISOString().replace(/[:.]/g,"-")}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  // Ctrl+Shift+L = ดาวน์โหลด log
  document.addEventListener("keydown", (e) => {
    if (e.ctrlKey && e.shiftKey && e.key === "L") {
      e.preventDefault();
      downloadLogs();
    }
  });

  // เพิ่มปุ่ม download log ที่มุมขวาล่าง (ซ่อนๆ)
  const _dlBtn = document.createElement("button");
  _dlBtn.textContent = "⬇ Log";
  _dlBtn.title = "ดาวน์โหลด debug log (Ctrl+Shift+L)";
  Object.assign(_dlBtn.style, {
    position: "fixed", bottom: "90px", right: "16px", zIndex: "9999",
    padding: "5px 10px", fontSize: "11px", opacity: "0.6",
    background: "#333", color: "#fff", border: "none",
    borderRadius: "6px", cursor: "pointer",
  });
  _dlBtn.addEventListener("click", downloadLogs);
  document.body.appendChild(_dlBtn);

  console.info("[room.js] Log collector initialized. Press Ctrl+Shift+L to download.");

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
  const roomPage = document.querySelector(".room-page");
  const roomCameraButton = document.getElementById("roomCameraButton");
  const roomMicrophoneButton = document.getElementById("roomMicrophoneButton");
  const roomChatButton = document.getElementById("roomChatButton");
  const roomScreenShareButton = document.getElementById("roomScreenShareButton");
  const roomCallStatus = document.getElementById("roomCallStatus");

  // =========================
  // Room Data
  // =========================

  let room = null;
  let currentUserId = null;
  let cameraEnabled = false;
  let microphoneEnabled = false;
  let screenSharing = false;

  // =========================
  // WebSocket
  // =========================

  let webSocketConnected = false;

  // ใช้ STOMP client ของ call.js (window.CPCall) แทนการเปิด WebSocket ใหม่
  // เพื่อให้ทั้งแชทและ WebRTC signaling ใช้ connection เดียวกัน

  function connectWebSocket(userId) {
    console.info("[room.js] connectWebSocket() | userId=", userId,
      "| CPCall=", window.CPCall ? "ready" : "NOT FOUND ❌");

    if (!window.CPCall) {
      console.error("[room.js] window.CPCall ยังไม่พร้อม — call.js โหลดก่อน room.js หรือเปล่า?");
      return;
    }

    window.CPCall.connectWS(() => {
      console.info("[room.js] ✅ STOMP connected via CPCall — subscribing room chat | roomId=", roomId);
      webSocketConnected = true;
      subscribeRoom();
    });
  }

  function subscribeRoom() {
    console.info("[room.js] subscribeRoom() | roomId=", roomId,
      "| CPCall=", window.CPCall ? "ready" : "NOT FOUND ❌");

    if (!window.CPCall) return;

    window.CPCall.subscribeRoomChat(roomId, function (message) {
      console.log("[room.js] 📨 message received | senderId=",
        message?.senderId, "| type=", message?.messageType,
        "| content=", message?.content?.substring(0, 60));
      createMessageElement(message);
      scrollChatToBottom();
    });
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
      try {
        const currentUser = await window.CPCall?.loadMe();
        currentUserId = String(currentUser?.id || currentUser?.userId || "");
      } catch (error) {
        console.warn("โหลดผู้ใช้ปัจจุบันไม่สำเร็จ:", error);
      }

      renderRoom();
      renderMembers();
      await loadMessages();

      if (window.CPCall) {
        try {
          await window.CPCall.joinRoomCall(roomId, "VIDEO", {
            receiveOnly: true,
          });
          console.info("[WebRTC] joined room in receive-only mode", {
            roomId,
            userId: currentUserId,
          });
          setRoomCallStatus("เชื่อมต่อสัญญาณห้องแล้ว");
        } catch (error) {
          console.error("เข้าร่วมสัญญาณห้องไม่สำเร็จ:", error);
          setRoomCallStatus(error.message || "เชื่อมต่อสัญญาณห้องไม่สำเร็จ");
        }
      }
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
    card.dataset.userId = String(member.userId || "");

    const fullname = getFullName(member);
    const imageUrl = getImageUrl(member.imageUrl);

    const role = member.role || "";

    const department = member.department || "";
    const year = member.year || "";

    const isOwner = role === "OWNER" || role === "owner";
    const isCurrentUser =
      currentUserId && String(member.userId) === currentUserId;
    if (isCurrentUser) card.classList.add("local-member");

    /*
     * สมาชิกทุกคนกด Profile ได้
     */
    card.classList.add("friend-profile-trigger");

    card.setAttribute("data-profile", "");
    card.setAttribute("data-id", member.userId || "");
    card.setAttribute("data-name", fullname);
    card.setAttribute("data-image", imageUrl);
    card.setAttribute(
      "data-year",
      year ? `ปี ${year} ${department ? department : ""}`.trim() : "",
    );
    card.setAttribute("data-bio", member.bio || "");
    card.setAttribute("data-interests", member.interests || "");
    card.setAttribute("data-status", member.friendStatus || "none");

    card.innerHTML = `
      <div class="member-media">
        <img
          class="member-avatar"
          src="${escapeHtml(imageUrl)}"
          alt="${escapeHtml(fullname)}"
        />
        <video class="member-video" autoplay playsinline></video>
        <button
          type="button"
          class="member-expand-button"
          aria-label="ขยาย ${escapeHtml(fullname)} เต็มจอ"
          title="ขยายเต็มจอ"
        >
          <span aria-hidden="true">⛶</span>
        </button>
      </div>

      <h3>
        ${escapeHtml(fullname)}
      </h3>

      <p>
        ${isOwner ? "เจ้าของห้อง" : "สมาชิก"}
      </p>

      ${isOwner ? `<small>(เจ้าของห้อง)</small>` : `<small>(สมาชิก)</small>`}
    `;

    const expandButton = card.querySelector(".member-expand-button");
    const memberMedia = card.querySelector(".member-media");
    expandButton.addEventListener("click", async (event) => {
      event.preventDefault();
      event.stopPropagation();

      try {
        if (document.fullscreenElement === memberMedia) {
          await document.exitFullscreen();
        } else {
          if (document.fullscreenElement) {
            await document.exitFullscreen();
          }
          await memberMedia.requestFullscreen();
        }
      } catch (error) {
        console.error("ขยายวิดีโอสมาชิกไม่สำเร็จ:", error);
        setRoomCallStatus("ไม่สามารถขยายเต็มจอได้ใน browser นี้");
      }
    });

    document.addEventListener("fullscreenchange", () => {
      const isFullscreen = document.fullscreenElement === memberMedia;
      expandButton.setAttribute(
        "aria-label",
        isFullscreen
          ? `ย่อ ${fullname} กลับ`
          : `ขยาย ${fullname} เต็มจอ`,
      );
      expandButton.title = isFullscreen ? "ย่อกลับ" : "ขยายเต็มจอ";
    });

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
      window.CPCall?.leaveCall();
    } catch (error) {
      console.warn("ปิดการเชื่อมต่อคอลไม่สำเร็จ:", error);
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
  // ROOM MEDIA CONTROLS
  // =========================

  function setRoomCallStatus(message) {
    roomCallStatus.textContent = message || "";
  }

  function getMemberCard(userId) {
    return Array.from(membersGrid.querySelectorAll(".member-card")).find(
      (card) => card.dataset.userId === String(userId),
    );
  }

  function updateLocalVideo(stream = window.CPCall?.getLocalStream()) {
    if (!currentUserId) return;
    const card = getMemberCard(currentUserId);
    const video = card?.querySelector(".member-video");
    if (!card || !video) return;

    if (screenSharing && stream?.getVideoTracks().length) {
      video.srcObject = stream;
      video.muted = true;
      video.play().catch(() => {});
      card.classList.add("video-on", "screen-sharing");
      return;
    }

    const cameraTrack = stream?.getVideoTracks().find(
      (track) => track.readyState === "live" && track.enabled,
    );
    if (cameraTrack) {
      video.srcObject = stream;
      video.muted = true;
      video.play().catch(() => {});
      card.classList.add("video-on");
      card.classList.remove("screen-sharing");
    } else {
      video.srcObject = null;
      card.classList.remove("video-on", "screen-sharing");
    }
  }

  function setControlState(button, label, active, activeClass = "is-active") {
    button.classList.toggle(activeClass, active);
    button.setAttribute("aria-pressed", String(active));
    button.setAttribute("aria-label", label);
    button.title = label;
  }

  async function ensureRoomCall(mode, options = {}) {
    if (!window.CPCall) throw new Error("ระบบโทรผ่านห้องยังโหลดไม่สำเร็จ");
    if (!window.CPCall.isActive()) {
      await window.CPCall.joinRoomCall(roomId, mode, options);
    }
    return window.CPCall.getLocalStream();
  }

  async function toggleRoomCamera() {
    roomCameraButton.disabled = true;
    setRoomCallStatus("");
    try {
      if (!window.CPCall?.isActive()) {
        await ensureRoomCall("VIDEO", { audioEnabled: false });
        cameraEnabled = true;
      } else if (cameraEnabled) {
        await window.CPCall.setRoomCameraEnabled(false);
        cameraEnabled = false;
      } else {
        let stream = window.CPCall.getLocalStream();
        if (!stream?.getVideoTracks().length) {
          stream = await window.CPCall.startMedia("VIDEO", {
            audioEnabled: microphoneEnabled,
            videoEnabled: true,
          });
        }
        cameraEnabled = Boolean(
          await window.CPCall.setRoomCameraEnabled(true),
        );
      }

      setControlState(
        roomCameraButton,
        cameraEnabled ? "ปิดกล้อง" : "เปิดกล้อง",
        cameraEnabled,
      );
      updateLocalVideo();
      setRoomCallStatus(cameraEnabled ? "เปิดกล้องแล้ว" : "ปิดกล้องแล้ว");
      endCallWhenMediaIsOff();
    } catch (error) {
      console.error("เปิด/ปิดกล้องไม่สำเร็จ:", error);
      cameraEnabled = false;
      microphoneEnabled = false;
      setControlState(roomCameraButton, "เปิดกล้อง", false);
      setControlState(roomMicrophoneButton, "เปิดไมค์", false);
      updateLocalVideo();
      setRoomCallStatus(error.message || "ไม่สามารถเปิดกล้องได้");
    } finally {
      roomCameraButton.disabled = false;
    }
  }

  async function toggleRoomMicrophone() {
    roomMicrophoneButton.disabled = true;
    setRoomCallStatus("");
    try {
      if (!window.CPCall?.isActive()) {
        await ensureRoomCall("VOICE", { videoEnabled: false });
        microphoneEnabled = true;
      } else {
        let stream = window.CPCall.getLocalStream();
        if (!stream?.getAudioTracks().length) {
          stream = await window.CPCall.startMedia("VOICE", {
            audioEnabled: true,
            videoEnabled: cameraEnabled,
          });
          microphoneEnabled = true;
        } else {
          microphoneEnabled = Boolean(window.CPCall.toggleMicrophone());
        }
      }

      setControlState(
        roomMicrophoneButton,
        microphoneEnabled ? "ปิดไมค์" : "เปิดไมค์",
        microphoneEnabled,
      );
      setRoomCallStatus(microphoneEnabled ? "เปิดไมค์แล้ว" : "ปิดไมค์แล้ว");
      endCallWhenMediaIsOff();
    } catch (error) {
      console.error("เปิด/ปิดไมค์ไม่สำเร็จ:", error);
      microphoneEnabled = false;
      cameraEnabled = false;
      setControlState(roomMicrophoneButton, "เปิดไมค์", false);
      setControlState(roomCameraButton, "เปิดกล้อง", false);
      updateLocalVideo();
      setRoomCallStatus(error.message || "ไม่สามารถเปิดไมค์ได้");
    } finally {
      roomMicrophoneButton.disabled = false;
    }
  }

  function endCallWhenMediaIsOff() {
    if (!cameraEnabled && !microphoneEnabled && !screenSharing) {
      updateLocalVideo();
      setRoomCallStatus("ปิดกล้องและไมค์แล้ว แต่ยังรับสัญญาณจากห้องอยู่");
    }
  }

  function toggleRoomChat() {
    /*
     * เดสก์ท็อปเริ่มต้นแชทเปิดอยู่ (ไม่มี chat-hidden)
     * มือถือเริ่มต้นแชทปิดอยู่ (มี chat-hidden จากตอนโหลดหน้า)
     * ใช้สถานะ chat-hidden เป็นตัวตั้งต้นเสมอ เพื่อให้กดครั้งแรก
     * บนเดสก์ท็อปปิดแชทได้ทันที
     */
    const willShow = roomPage.classList.contains("chat-hidden");
    roomPage.classList.toggle("chat-hidden", !willShow);
    roomPage.classList.toggle("chat-visible", willShow);
    setControlState(
      roomChatButton,
      willShow ? "ปิดแชทด้านข้าง" : "เปิดแชทด้านข้าง",
      willShow,
      "is-chat-active",
    );
  }

  async function toggleRoomScreenShare() {
    roomScreenShareButton.disabled = true;
    setRoomCallStatus("");
    let displayStream = null;
    const callWasActive = Boolean(window.CPCall?.isActive());
    try {
      if (screenSharing) {
        await window.CPCall.stopScreenShare();
        return;
      }

      displayStream = await navigator.mediaDevices.getDisplayMedia({
        video: true,
        audio: true,
      });
      if (!callWasActive) {
        await ensureRoomCall("VIDEO", {
          screenStream: displayStream,
          audioEnabled: false,
          videoEnabled: false,
        });
      } else {
        await window.CPCall.startScreenShare(displayStream);
      }
    } catch (error) {
      displayStream?.getTracks().forEach((track) => track.stop());
      if (!callWasActive) window.CPCall?.leaveCall();
      console.error("แชร์หน้าจอไม่สำเร็จ:", error);
      setRoomCallStatus(error.message || "ไม่สามารถแชร์หน้าจอได้");
    } finally {
      roomScreenShareButton.disabled = false;
    }
  }

  function handleRemoteCallTrack(event) {
    const {
      userId,
      stream,
      connectionState,
      iceConnectionState,
      signalingState,
    } = event.detail || {};
    if (!userId || !stream) {
      console.warn("[WebRTC][Room] remote track missing identity or stream", {
        userId: userId || null,
        hasStream: Boolean(stream),
      });
      return;
    }
    const card = getMemberCard(userId);
    const video = card?.querySelector(".member-video");
    if (!card || !video) {
      console.warn("[WebRTC][Room] no member card for remote track", {
        remoteUserId: String(userId),
        knownMemberIds: Array.from(
          membersGrid.querySelectorAll(".member-card"),
          (memberCard) => memberCard.dataset.userId,
        ),
      });
      return;
    }

    console.info("[WebRTC][Room] attaching remote video element", {
      roomId,
      remoteUserId: String(userId),
      memberName: card.querySelector("h3")?.textContent || null,
      videoElement: video.tagName.toLowerCase(),
      videoElementId: video.id || null,
      connectionState,
      iceConnectionState,
      signalingState,
      trackKinds: stream.getTracks().map((track) => track.kind),
      videoTrackStates: stream.getVideoTracks().map((track) => ({
        readyState: track.readyState,
        muted: track.muted,
        enabled: track.enabled,
      })),
    });

    // ตั้ง srcObject เฉพาะเมื่อยังไม่ได้ set หรือ stream เปลี่ยน
    // ป้องกัน "play() interrupted by new load request" เมื่อ renegotiate
    if (video.srcObject !== stream) {
      video.srcObject = stream;
    }
    video.muted = true;
    video.autoplay = true;
    video.playsInline = true;

    // debounce play() เพื่อรอ ontrack ทุก track (audio+video) settle ก่อน
    // ป้องกัน "play() interrupted" จากการที่ ontrack fire 2 ครั้งต่อกัน
    if (video._playDebounceTimer) {
      clearTimeout(video._playDebounceTimer);
    }
    video._playDebounceTimer = setTimeout(() => {
      video._playDebounceTimer = null;
      if (video.paused) {
        video.play().catch((error) => {
          console.warn("[WebRTC][Room] remote video playback blocked", {
            remoteUserId: String(userId),
            error: error.message,
          });
        });
      }
    }, 80);
    video.addEventListener("loadedmetadata", () => {
      if (video.paused) {
        video.play().catch((error) => {
          console.warn("[WebRTC][Room] remote video playback blocked", {
            remoteUserId: String(userId),
            error: error.message,
          });
        });
      }
    }, { once: true });

    const updateVideo = (eventType = "state") => {
      const track = stream.getVideoTracks()[0];
      const isLive = Boolean(track && !track.muted && track.readyState === "live");
      card.classList.toggle("video-on", isLive);
      /*
       * ถ้า track วิดีโอหยุด/หาย (เช่น คนแชร์จอออกจากห้อง)
       * ต้องเคลียร์คลาส screen-sharing ด้วย ไม่งั้น CSS จะยังบังคับ
       * แสดงเฟรมสุดท้ายค้างทับรูปโปรไฟล์
       */
      if (!isLive) {
        card.classList.remove("screen-sharing");
        if (track?.readyState === "ended" || eventType === "ended") {
          video.srcObject = null;
        }
      }
      console.info("[WebRTC][Room] remote video state", {
        remoteUserId: String(userId),
        event: eventType,
        readyState: track?.readyState || "missing",
        muted: track?.muted ?? null,
        enabled: track?.enabled ?? null,
        videoWidth: video.videoWidth,
        videoHeight: video.videoHeight,
        visible: isLive,
      });
    };
    const videoTrack = stream.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.addEventListener("mute", () => updateVideo("mute"));
      videoTrack.addEventListener("unmute", () => updateVideo("unmute"));
      videoTrack.addEventListener("ended", () => updateVideo("ended"));
      video.addEventListener("loadedmetadata", () => {
        card.classList.toggle(
          "screen-sharing",
          video.videoWidth / Math.max(video.videoHeight, 1) > 1.7,
        );
        updateVideo("loadedmetadata");
      });
      video.addEventListener("playing", () => updateVideo("playing"));
    }
    updateVideo();
  }

  function resetRemoteCallTrack(event) {
    const card = getMemberCard(event.detail?.userId);
    if (!card) return;
    const video = card.querySelector(".member-video");
    if (video) video.srcObject = null;
    card.classList.remove("video-on", "screen-sharing");
  }

  roomCameraButton.addEventListener("click", toggleRoomCamera);
  roomMicrophoneButton.addEventListener("click", toggleRoomMicrophone);
  roomChatButton.addEventListener("click", toggleRoomChat);
  roomScreenShareButton.addEventListener("click", toggleRoomScreenShare);
  if (!window.matchMedia("(min-width: 801px)").matches) {
    roomPage.classList.add("chat-hidden");
    setControlState(roomChatButton, "เปิดแชทด้านข้าง", false, "is-chat-active");
  }
  window.addEventListener("cp-call-track", handleRemoteCallTrack);
  window.addEventListener("cp-call-peer-left", resetRemoteCallTrack);
  window.addEventListener("cp-call-media-state", (event) => {
    const { userId, video, screen } = event.detail || {};
    const card = getMemberCard(userId);
    if (!card) return;
    const videoElement = card.querySelector(".member-video");
    if (video === false && !screen) {
      /*
       * อีกฝั่งปิดกล้อง/หยุดแชร์จอ — เคลียร์วิดีโอทันที
       * แสดงรูปโปรไฟล์แทนเฟรมสุดท้ายที่ค้าง
       */
      if (videoElement) videoElement.srcObject = null;
      card.classList.remove("video-on", "screen-sharing");
    }
  });
  window.addEventListener("cp-call-screen-share", (event) => {
    screenSharing = Boolean(event.detail?.active);
    setControlState(
      roomScreenShareButton,
      screenSharing ? "หยุดแชร์หน้าจอ" : "แชร์หน้าจอ",
      screenSharing,
      "is-sharing",
    );
    updateLocalVideo(event.detail?.stream);
    setRoomCallStatus(screenSharing ? "กำลังแชร์หน้าจอ" : "หยุดแชร์หน้าจอแล้ว");
    endCallWhenMediaIsOff();
  });

  // =========================
  // START
  // =========================

  loadRoom().then(() => {
    // เชื่อม WebSocket หลังจากรู้ userId เพื่อส่ง login header ได้ถูกต้อง
    connectWebSocket(currentUserId);
  });
});
