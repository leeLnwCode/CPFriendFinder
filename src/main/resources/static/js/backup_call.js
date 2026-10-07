(() => {
  "use strict";

  /*
   * =========================================================
   * CP FRIEND FINDER - WEBRTC CALL
   * =========================================================
   *
   * หน้าที่ของไฟล์นี้:
   * - STOMP signaling สำหรับ call
   * - WebRTC peer connection
   * - microphone / camera
   * - local / remote stream
   * - incoming call signal
   *
   * UI ของ Voice / Video Call อยู่ใน friend.js
   * =========================================================
   */

  let stompClient = null;

  const wsWaiters = [];

  let me = null;

  /*
   * userId -> {
   *   pc,
   *   polite,
   *   makingOffer,
   *   ignoreOffer,
   *   pendingCandidates
   * }
   */
  const peers = new Map();

  let localStream = null;

  let currentRoomId = null;

  let currentMode = null;

  let activeCall = false;

  let currentCallFriendId = null;

  /*
   * subscription ห้อง call ปัจจุบัน
   */
  let roomCallSubscription = null;

  /*
   * subscription incoming call
   */
  let incomingCallSubscription = null;

  /*
   * =========================================================
   * ICE CONFIG
   * =========================================================
   *
   * STUN ใช้ได้สำหรับทดสอบ
   *
   * Production / คนละ network / NAT บางประเภท
   * ควรใส่ TURN server เพิ่ม
   * =========================================================
   */

  const ICE_CONFIG = {
    iceServers: [
      {
        urls: "stun:stun.relay.metered.ca:80",
      },

      /*
       * ตัวอย่าง TURN
       *
       * อย่า commit credential จริงลง Git
       *
       * {
       *   urls: "turn:xxxxx.relay.metered.ca:80",
       *   username: "...",
       *   credential: "..."
       * }
       */
    ],
  };

  /*
   * =========================================================
   * LOAD CURRENT USER
   * =========================================================
   */

  async function loadMe() {
    if (me) {
      return me;
    }

    const response = await fetch("/api/users/me", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error("ไม่สามารถโหลดข้อมูลผู้ใช้ปัจจุบันได้");
    }

    me = await response.json();

    /*
     * รองรับกรณี backend ส่ง userId
     */
    if (!me.id && me.userId) {
      me.id = me.userId;
    }

    return me;
  }

  /*
   * =========================================================
   * STOMP CONNECTION
   * =========================================================
   */

  function connectWS(onConnected) {
    /*
     * ต่ออยู่แล้ว
     */
    if (stompClient?.connected) {
      if (onConnected) {
        onConnected();
      }

      return;
    }

    /*
     * ถ้ามี callback ให้รอจน connected
     */
    if (onConnected) {
      wsWaiters.push(onConnected);
    }

    /*
     * client ถูกสร้างแล้ว
     * รอ reconnect
     */
    if (stompClient) {
      return;
    }

    const scheme = location.protocol === "https:" ? "wss" : "ws";

    stompClient = new StompJs.Client({
      brokerURL: `${scheme}://${location.host}/ws`,

      reconnectDelay: 5000,

      debug: () => {
        /*
         * ถ้าอยาก debug STOMP เต็ม ๆ
         * สามารถเปิด console.log ได้ตรงนี้
         */
      },
    });

    stompClient.onStompError = (frame) => {
      console.error(
        "STOMP error:",
        frame.headers?.["message"] || frame.body || frame,
      );
    };

    stompClient.onWebSocketError = (error) => {
      console.error("WebSocket connection failed:", error);
    };

    stompClient.onDisconnect = () => {
      console.log("STOMP disconnected");
    };

    /*
     * =====================================================
     * CONNECTED
     * =====================================================
     */

    stompClient.onConnect = () => {
      console.log("Call STOMP connected");

      /*
       * Incoming call ต้อง subscribe ใหม่ทุกครั้ง
       * ที่ reconnect
       */
      if (me?.id) {
        subscribeIncomingCalls();
      }

      /*
       * callback ที่รออยู่
       */
      const callbacks = wsWaiters.splice(0);

      callbacks.forEach((callback) => {
        try {
          callback();
        } catch (error) {
          console.error("WebSocket callback error:", error);
        }
      });
    };

    stompClient.activate();
  }

  /*
   * =========================================================
   * PUBLISH
   * =========================================================
   */

  function wsPublish(destination, body) {
    if (!stompClient?.connected) {
      console.warn(
        "ไม่สามารถส่ง WebSocket เพราะ STOMP ยังไม่ได้ connect:",
        destination,
      );

      return false;
    }

    stompClient.publish({
      destination,
      body: JSON.stringify(body),
    });

    return true;
  }

  /*
   * =========================================================
   * CREATE PEER
   * =========================================================
   */

  function createPeer(userId) {
    if (!localStream) {
      throw new Error("ยังไม่มี local media stream");
    }

    const normalizedUserId = String(userId);

    /*
     * มี peer อยู่แล้ว
     */
    if (peers.has(normalizedUserId)) {
      return peers.get(normalizedUserId);
    }

    const pc = new RTCPeerConnection(ICE_CONFIG);

    /*
     * Perfect Negotiation
     *
     * คนที่ ID ต่ำกว่า = polite
     */
    const entry = {
      pc,

      polite: String(me.id) < normalizedUserId,

      makingOffer: false,

      ignoreOffer: false,

      pendingCandidates: [],
    };

    peers.set(normalizedUserId, entry);

    /*
     * =====================================================
     * LOCAL TRACKS
     * =====================================================
     */

    localStream.getTracks().forEach((track) => {
      pc.addTrack(track, localStream);
    });

    /*
     * =====================================================
     * ICE CANDIDATE
     * =====================================================
     */

    pc.onicecandidate = (event) => {
      if (!event.candidate) {
        return;
      }

      if (!currentRoomId) {
        return;
      }

      wsPublish(`/app/rooms/${currentRoomId}/call`, {
        type: "ICE",

        targetUserId: normalizedUserId,

        payload: JSON.stringify(event.candidate),
      });
    };

    /*
     * =====================================================
     * REMOTE TRACK
     * =====================================================
     */

    pc.ontrack = (event) => {
      const stream = event.streams?.[0];

      if (!stream) {
        return;
      }

      showRemoteStream(stream);

      if (currentMode === "VIDEO") {
        showRemoteVideo(stream);
      }
    };

    /*
     * =====================================================
     * CONNECTION STATE
     * =====================================================
     */

    pc.onconnectionstatechange = () => {
      console.log("Peer state:", normalizedUserId, pc.connectionState);

      /*
       * แจ้ง UI
       */
      if (pc.connectionState === "connected") {
        window.dispatchEvent(
          new CustomEvent("cp-call-connected", {
            detail: {
              userId: normalizedUserId,
              mode: currentMode,
            },
          }),
        );
      }

      if (pc.connectionState === "failed" || pc.connectionState === "closed") {
        removePeer(normalizedUserId);
      }
    };

    /*
     * =====================================================
     * ICE CONNECTION STATE
     * =====================================================
     */

    pc.oniceconnectionstatechange = () => {
      console.log("ICE state:", normalizedUserId, pc.iceConnectionState);

      if (pc.iceConnectionState === "failed") {
        /*
         * ลอง restart ICE
         */
        if (pc.restartIce) {
          try {
            pc.restartIce();
          } catch (error) {
            console.warn("restart ICE ไม่สำเร็จ:", error);
          }
        }
      }
    };

    /*
     * =====================================================
     * NEGOTIATION
     * =====================================================
     */

    pc.onnegotiationneeded = async () => {
      try {
        entry.makingOffer = true;

        /*
         * Browser จะสร้าง offer ให้เอง
         */
        await pc.setLocalDescription();

        if (!pc.localDescription) {
          return;
        }

        wsPublish(`/app/rooms/${currentRoomId}/call`, {
          type: "OFFER",

          targetUserId: normalizedUserId,

          payload: JSON.stringify(pc.localDescription),
        });
      } catch (error) {
        console.error("สร้าง OFFER ไม่สำเร็จ:", error);
      } finally {
        entry.makingOffer = false;
      }
    };

    return entry;
  }

  /*
   * =========================================================
   * REMOVE PEER
   * =========================================================
   */

  function removePeer(userId) {
    const normalizedUserId = String(userId);

    const entry = peers.get(normalizedUserId);

    if (!entry) {
      return;
    }

    try {
      entry.pc.ontrack = null;
      entry.pc.onicecandidate = null;
      entry.pc.onnegotiationneeded = null;
      entry.pc.close();
    } catch (error) {
      console.warn("ปิด peer ไม่สำเร็จ:", error);
    }

    peers.delete(normalizedUserId);

    console.log("ลบ peer:", normalizedUserId);
  }

  /*
   * =========================================================
   * WEBRTC SIGNAL
   * =========================================================
   */

  async function handleWebRTCSignal(signal) {
    if (!signal?.fromUserId) {
      return;
    }

    const fromUserId = String(signal.fromUserId);

    let entry = peers.get(fromUserId);

    /*
     * OFFER / ANSWER / ICE
     * อาจมาก่อน JOIN
     *
     * ดังนั้นสร้าง peer ได้ตรงนี้
     */
    if (!entry) {
      entry = createPeer(fromUserId);
    }

    const pc = entry.pc;

    try {
      /*
       * =====================================================
       * OFFER
       * =====================================================
       */

      if (signal.type === "OFFER") {
        if (!signal.payload) {
          return;
        }

        const offer = JSON.parse(signal.payload);

        /*
         * Perfect Negotiation
         */
        const offerCollision =
          entry.makingOffer || pc.signalingState !== "stable";

        entry.ignoreOffer = offerCollision && !entry.polite;

        if (entry.ignoreOffer) {
          console.log("ignore offer จาก:", fromUserId);

          return;
        }

        await pc.setRemoteDescription(offer);

        /*
         * หลังรับ OFFER
         * สร้าง ANSWER
         */
        await pc.setLocalDescription();

        if (!pc.localDescription) {
          return;
        }

        wsPublish(`/app/rooms/${currentRoomId}/call`, {
          type: "ANSWER",

          targetUserId: fromUserId,

          payload: JSON.stringify(pc.localDescription),
        });

        await flushPendingCandidates(entry);

        return;
      }

      /*
       * =====================================================
       * ANSWER
       * =====================================================
       */

      if (signal.type === "ANSWER") {
        if (!signal.payload) {
          return;
        }

        const answer = JSON.parse(signal.payload);

        await pc.setRemoteDescription(answer);

        await flushPendingCandidates(entry);

        return;
      }

      /*
       * =====================================================
       * ICE
       * =====================================================
       */

      if (signal.type === "ICE") {
        if (!signal.payload) {
          return;
        }

        const candidate = JSON.parse(signal.payload);

        /*
         * remote description พร้อมแล้ว
         */
        if (pc.remoteDescription) {
          try {
            await pc.addIceCandidate(candidate);
          } catch (error) {
            if (!entry.ignoreOffer) {
              console.warn("เพิ่ม ICE ไม่สำเร็จ:", error);
            }
          }
        } else {
          /*
           * ICE มาก่อน OFFER
           */
          entry.pendingCandidates.push(candidate);
        }
      }
    } catch (error) {
      if (!entry.ignoreOffer) {
        console.error("WebRTC signal error:", error, signal);
      }
    }
  }

  /*
   * =========================================================
   * FLUSH ICE
   * =========================================================
   */

  async function flushPendingCandidates(entry) {
    if (!entry?.pendingCandidates?.length) {
      return;
    }

    const candidates = [...entry.pendingCandidates];

    entry.pendingCandidates = [];

    for (const candidate of candidates) {
      try {
        await entry.pc.addIceCandidate(candidate);
      } catch (error) {
        console.warn("เพิ่ม buffered ICE ไม่สำเร็จ:", error);
      }
    }
  }

  /*
   * =========================================================
   * ROOM CALL SIGNAL
   * =========================================================
   */

  function onCallSignal(signal) {
    if (!signal || !me?.id) {
      return;
    }

    const fromUserId =
      signal.fromUserId != null ? String(signal.fromUserId) : null;

    const myId = String(me.id);

    /*
     * =====================================================
     * IGNORE SELF
     * =====================================================
     */

    if (fromUserId === myId && signal.type !== "ICE") {
      return;
    }

    /*
     * =====================================================
     * JOIN
     * =====================================================
     */

    if (signal.type === "JOIN") {
      if (!fromUserId || fromUserId === myId) {
        return;
      }

      try {
        createPeer(fromUserId);
      } catch (error) {
        console.error("สร้าง peer ตอน JOIN ไม่สำเร็จ:", error);
      }

      return;
    }

    /*
     * =====================================================
     * LEAVE
     * =====================================================
     */

    if (signal.type === "LEAVE") {
      if (fromUserId) {
        removePeer(fromUserId);
      }

      return;
    }

    /*
     * =====================================================
     * OFFER / ANSWER / ICE
     * =====================================================
     */

    if (signal.toUserId != null && String(signal.toUserId) !== myId) {
      return;
    }

    handleWebRTCSignal(signal);
  }

  /*
   * =========================================================
   * SUBSCRIBE ROOM CALL
   * =========================================================
   */

  function subscribeRoomCall(roomId) {
    if (!stompClient?.connected || !roomId) {
      return;
    }

    /*
     * ป้องกัน subscribe ซ้ำ
     */
    if (roomCallSubscription) {
      try {
        roomCallSubscription.unsubscribe();
      } catch (error) {
        console.warn("unsubscribe ห้อง call เดิมไม่สำเร็จ:", error);
      }

      roomCallSubscription = null;
    }

    roomCallSubscription = stompClient.subscribe(
      `/topic/rooms/${roomId}/call`,
      (frame) => {
        try {
          const signal = JSON.parse(frame.body);

          onCallSignal(signal);
        } catch (error) {
          console.error("อ่าน call signal ไม่ได้:", error);
        }
      },
    );

    console.log("Subscribed call room:", roomId);
  }

  /*
   * =========================================================
   * UNSUBSCRIBE ROOM CALL
   * =========================================================
   */

  function unsubscribeRoomCall() {
    if (!roomCallSubscription) {
      return;
    }

    try {
      roomCallSubscription.unsubscribe();
    } catch (error) {
      console.warn("unsubscribe call room ไม่สำเร็จ:", error);
    }

    roomCallSubscription = null;
  }

  /*
   * =========================================================
   * START MEDIA
   * =========================================================
   */

  async function startMedia(mode) {
    /*
     * ถ้ามี stream อยู่แล้ว
     */
    if (localStream) {
      /*
       * ถ้าจาก VOICE -> VIDEO
       * ต้องเปิดกล้องเพิ่ม
       */
      if (mode === "VIDEO" && localStream.getVideoTracks().length === 0) {
        const videoStream = await navigator.mediaDevices.getUserMedia({
          video: true,
        });

        videoStream.getVideoTracks().forEach((track) => {
          localStream.addTrack(track);

          peers.forEach((entry) => {
            entry.pc.addTrack(track, localStream);
          });
        });
      }

      currentMode = mode;

      showLocalStream(localStream, mode);

      return localStream;
    }

    currentMode = mode;

    localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: mode === "VIDEO",
    });

    showLocalStream(localStream, mode);

    return localStream;
  }

  /*
   * =========================================================
   * JOIN ROOM CALL
   * =========================================================
   */

  async function joinRoomCall(roomId, mode) {
    if (!roomId) {
      throw new Error("ไม่พบ roomId");
    }

    await loadMe();

    currentRoomId = roomId;

    currentMode = mode === "VIDEO" ? "VIDEO" : "VOICE";

    activeCall = true;

    await startMedia(currentMode);

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          subscribeRoomCall(roomId);

          const published = wsPublish(`/app/rooms/${roomId}/call`, {
            type: "JOIN",
          });

          if (!published) {
            throw new Error("ส่ง JOIN ไม่สำเร็จ");
          }

          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  /*
   * =========================================================
   * LEAVE CALL
   * =========================================================
   */

  function leaveCall() {
    const roomId = currentRoomId;

    /*
     * ส่ง LEAVE ก่อน reset state
     */
    if (roomId && stompClient?.connected) {
      wsPublish(`/app/rooms/${roomId}/call`, {
        type: "LEAVE",
      });
    }

    /*
     * unsubscribe
     */
    unsubscribeRoomCall();

    /*
     * ปิด peers
     */
    peers.forEach((entry) => {
      try {
        entry.pc.ontrack = null;
        entry.pc.onicecandidate = null;
        entry.pc.onnegotiationneeded = null;
        entry.pc.close();
      } catch (error) {
        console.warn("ปิด peer ไม่สำเร็จ:", error);
      }
    });

    peers.clear();

    /*
     * ปิด local media
     */
    if (localStream) {
      localStream.getTracks().forEach((track) => {
        try {
          track.stop();
        } catch (error) {
          console.warn("หยุด media track ไม่สำเร็จ:", error);
        }
      });
    }

    localStream = null;

    /*
     * clear video elements
     */
    const localVideo = document.getElementById("videoCallLocalVideo");

    if (localVideo) {
      localVideo.srcObject = null;
    }

    const remoteVideo = document.getElementById("videoCallRemoteVideo");

    if (remoteVideo) {
      remoteVideo.srcObject = null;
    }

    /*
     * remote audio
     */
    if (remoteAudio) {
      remoteAudio.srcObject = null;
    }

    /*
     * reset state
     */
    currentRoomId = null;

    currentMode = null;

    activeCall = false;

    currentCallFriendId = null;
  }

  /*
   * =========================================================
   * MICROPHONE
   * =========================================================
   */

  function toggleMicrophone() {
    if (!localStream) {
      return false;
    }

    const track = localStream.getAudioTracks()[0];

    if (!track) {
      return false;
    }

    track.enabled = !track.enabled;

    return track.enabled;
  }

  /*
   * =========================================================
   * CAMERA
   * =========================================================
   */

  function toggleCamera() {
    if (!localStream) {
      return false;
    }

    const track = localStream.getVideoTracks()[0];

    if (!track) {
      return false;
    }

    track.enabled = !track.enabled;

    return track.enabled;
  }

  /*
   * =========================================================
   * REMOTE AUDIO
   * =========================================================
   */

  let remoteAudio = null;

  function showRemoteStream(stream) {
    if (!remoteAudio) {
      remoteAudio = document.createElement("audio");

      remoteAudio.id = "remoteCallAudio";

      remoteAudio.autoplay = true;

      remoteAudio.playsInline = true;

      remoteAudio.setAttribute("aria-hidden", "true");

      /*
       * ไม่ต้องให้ user เห็น audio element
       */
      remoteAudio.style.display = "none";

      document.body.appendChild(remoteAudio);
    }

    remoteAudio.srcObject = stream;

    /*
     * browser บางตัวต้อง play()
     */
    remoteAudio.play().catch(() => {});
  }

  /*
   * =========================================================
   * LOCAL VIDEO
   * =========================================================
   */

  function showLocalStream(stream, mode) {
    const video = document.getElementById("videoCallLocalVideo");

    if (!video) {
      return;
    }

    video.srcObject = stream;

    video.muted = true;

    video.autoplay = true;

    video.playsInline = true;

    video.style.display = mode === "VIDEO" ? "block" : "none";

    video.play().catch(() => {});
  }

  /*
   * =========================================================
   * REMOTE VIDEO
   * =========================================================
   */

  function showRemoteVideo(stream) {
    const video = document.getElementById("videoCallRemoteVideo");

    if (!video) {
      return;
    }

    video.srcObject = stream;

    video.autoplay = true;

    video.playsInline = true;

    video.style.display = "block";

    video.play().catch(() => {});

    /*
     * ซ่อน fallback image
     */
    const placeholder =
      video.parentElement?.querySelector(".video-placeholder");

    if (placeholder) {
      placeholder.style.display = "none";
    }
  }

  /*
   * =========================================================
   * FRIEND INVITE
   * =========================================================
   */

  async function startFriendCall(friendId, roomId, mode) {
    if (!friendId) {
      throw new Error("ไม่พบ friendId");
    }

    if (!roomId) {
      throw new Error("ไม่พบ roomId");
    }

    await loadMe();

    currentCallFriendId = friendId;

    currentRoomId = roomId;

    currentMode = mode === "VIDEO" ? "VIDEO" : "VOICE";

    activeCall = false;

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          const published = wsPublish("/app/call", {
            type: "INVITE",

            toUserId: friendId,

            roomId,

            mode: currentMode,
          });

          if (!published) {
            throw new Error("ส่งสายเรียกเข้าไม่สำเร็จ");
          }

          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  /*
   * =========================================================
   * ACCEPT FRIEND CALL
   * =========================================================
   */

  async function acceptFriendCall(signal) {
    if (!signal?.fromUserId) {
      throw new Error("ข้อมูลผู้โทรไม่ครบ");
    }

    if (!signal.roomId) {
      throw new Error("ไม่พบ roomId");
    }

    await loadMe();

    currentRoomId = signal.roomId;

    currentCallFriendId = signal.fromUserId;

    currentMode = signal.mode === "VIDEO" ? "VIDEO" : "VOICE";

    activeCall = true;

    /*
     * เปิด microphone / camera
     */
    await startMedia(currentMode);

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          /*
           * แจ้ง caller ว่ารับสาย
           */
          const accepted = wsPublish("/app/call", {
            type: "ACCEPT",

            toUserId: signal.fromUserId,

            roomId: signal.roomId,

            mode: currentMode,
          });

          if (!accepted) {
            throw new Error("ส่ง ACCEPT ไม่สำเร็จ");
          }

          /*
           * subscribe room call
           */
          subscribeRoomCall(signal.roomId);

          /*
           * เข้าห้อง WebRTC
           */
          const joined = wsPublish(`/app/rooms/${signal.roomId}/call`, {
            type: "JOIN",
          });

          if (!joined) {
            throw new Error("ส่ง JOIN ไม่สำเร็จ");
          }

          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  /*
   * =========================================================
   * DECLINE
   * =========================================================
   */

  async function declineFriendCall(signal) {
    if (!signal?.fromUserId) {
      return;
    }

    await loadMe();

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          wsPublish("/app/call", {
            type: "DECLINE",

            toUserId: signal.fromUserId,

            roomId: signal.roomId,
          });

          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  /*
   * =========================================================
   * CANCEL
   * =========================================================
   */

  async function cancelFriendCall(signal = null) {
    const targetUserId = signal?.fromUserId || currentCallFriendId;

    const roomId = signal?.roomId || currentRoomId;

    if (!targetUserId) {
      return false;
    }

    await loadMe();

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          wsPublish("/app/call", {
            type: "CANCEL",

            toUserId: targetUserId,

            roomId: roomId || null,
          });

          resolve(true);
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  /*
   * =========================================================
   * INCOMING CALL SUBSCRIPTION
   * =========================================================
   */

  function subscribeIncomingCalls() {
    if (!stompClient?.connected || !me?.id) {
      return;
    }

    /*
     * ป้องกัน duplicate subscription
     */
    if (incomingCallSubscription) {
      try {
        incomingCallSubscription.unsubscribe();
      } catch (error) {
        console.warn("unsubscribe incoming call ไม่สำเร็จ:", error);
      }

      incomingCallSubscription = null;
    }

    incomingCallSubscription = stompClient.subscribe(
      `/topic/call/${me.id}`,
      (frame) => {
        try {
          const signal = JSON.parse(frame.body);

          console.log("Incoming call signal:", signal);

          window.dispatchEvent(
            new CustomEvent("cp-call-signal", {
              detail: signal,
            }),
          );
        } catch (error) {
          console.error("อ่าน incoming call ไม่ได้:", error);
        }
      },
    );
  }

  /*
   * =========================================================
   * PAGE HIDE
   * =========================================================
   */

  window.addEventListener("pagehide", () => {
    leaveCall();

    if (stompClient) {
      try {
        stompClient.deactivate();
      } catch (error) {
        console.warn("deactivate STOMP ไม่สำเร็จ:", error);
      }
    }
  });

  /*
   * =========================================================
   * PUBLIC API
   * =========================================================
   */

  window.CPCall = {
    connectWS,

    wsPublish,

    loadMe,

    subscribeIncomingCalls,

    startMedia,

    joinRoomCall,

    leaveCall,

    toggleMicrophone,

    toggleCamera,

    startFriendCall,

    acceptFriendCall,

    declineFriendCall,

    cancelFriendCall,

    showRemoteVideo,

    getLocalStream: () => localStream,

    getCurrentRoomId: () => currentRoomId,

    getCurrentMode: () => currentMode,

    getCurrentFriendId: () => currentCallFriendId,

    isActive: () => activeCall,
  };
})();
