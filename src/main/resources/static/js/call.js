(() => {
  "use strict";

  let stompClient = null;
  const wsWaiters = [];
  let me = null;
  let localStream = null;
  let currentRoomId = null;
  let currentMode = null;
  let activeCall = false;
  let currentCallFriendId = null;
  let roomCallSubscription = null;
  let incomingCallSubscription = null;
  let remoteAudio = null;

  const peers = new Map();

  const ICE_CONFIG = {
    iceServers: [
      { urls: "stun:stun.relay.metered.ca:80" },
      {
        urls: "turn:asia-east.relay.metered.ca:80",
        username: "9163b771c40b903fe9b0a80d",
        credential: "Avx5pFF0JyQ3Fm1i",
      },
      {
        urls: "turn:asia-east.relay.metered.ca:80?transport=tcp",
        username: "9163b771c40b903fe9b0a80d",
        credential: "Avx5pFF0JyQ3Fm1i",
      },
      {
        urls: "turn:asia-east.relay.metered.ca:443",
        username: "9163b771c40b903fe9b0a80d",
        credential: "Avx5pFF0JyQ3Fm1i",
      },
      {
        urls: "turns:asia-east.relay.metered.ca:443?transport=tcp",
        username: "9163b771c40b903fe9b0a80d",
        credential: "Avx5pFF0JyQ3Fm1i",
      },
    ],
  };

  async function loadMe() {
    if (me?.id) return me;
    const response = await fetch("/api/users/me", {
      method: "GET",
      headers: { Accept: "application/json" },
      credentials: "include",
    });
    if (!response.ok) throw new Error("ไม่สามารถโหลดข้อมูลผู้ใช้ปัจจุบันได้");
    me = await response.json();
    if (!me.id && me.userId) me.id = me.userId;
    if (!me?.id) throw new Error("ไม่พบ user id");
    return me;
  }

  function connectWS(onConnected) {
    if (stompClient?.connected) {
      onConnected?.();
      return;
    }

    if (onConnected) wsWaiters.push(onConnected);
    if (stompClient) return;

    const scheme = location.protocol === "https:" ? "wss" : "ws";
    stompClient = new StompJs.Client({
      brokerURL: `${scheme}://${location.host}/ws`,
      reconnectDelay: 5000,
      debug: () => {},
    });

    stompClient.onStompError = (frame) => {
      console.error(
        "Call STOMP error:",
        frame.headers?.message || frame.body || frame,
      );
    };

    stompClient.onWebSocketError = (error) => {
      console.error("Call WebSocket error:", error);
    };

    stompClient.onConnect = () => {
      if (me?.id) subscribeIncomingCalls();
      const callbacks = wsWaiters.splice(0);
      callbacks.forEach((callback) => {
        try {
          callback();
        } catch (error) {
          console.error("Call WS callback error:", error);
        }
      });
    };

    stompClient.activate();
  }

  function wsPublish(destination, body) {
    if (!stompClient?.connected) return false;
    stompClient.publish({ destination, body: JSON.stringify(body) });
    return true;
  }

  function createPeer(userId) {
    if (!localStream) throw new Error("ยังไม่มี local media stream");

    const normalizedUserId = String(userId);
    const existing = peers.get(normalizedUserId);
    if (existing) return existing;

    const pc = new RTCPeerConnection(ICE_CONFIG);
    const entry = {
      pc,
      polite: String(me.id) < normalizedUserId,
      makingOffer: false,
      ignoreOffer: false,
      pendingCandidates: [],
    };
    peers.set(normalizedUserId, entry);

    localStream.getTracks().forEach((track) => pc.addTrack(track, localStream));

    pc.onicecandidate = (event) => {
      if (!event.candidate || !currentRoomId) return;
      wsPublish(`/app/rooms/${currentRoomId}/call`, {
        type: "ICE",
        targetUserId: normalizedUserId,
        payload: JSON.stringify(event.candidate),
      });
    };

    pc.ontrack = (event) => {
      const stream = event.streams?.[0];
      if (!stream) return;
      showRemoteStream(stream);
      if (currentMode === "VIDEO") showRemoteVideo(stream);
    };

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === "connected") {
        window.dispatchEvent(
          new CustomEvent("cp-call-connected", {
            detail: { userId: normalizedUserId, mode: currentMode },
          }),
        );
      }
      if (["failed", "closed"].includes(pc.connectionState))
        removePeer(normalizedUserId);
    };

    pc.oniceconnectionstatechange = () => {
      if (
        pc.iceConnectionState === "failed" &&
        typeof pc.restartIce === "function"
      ) {
        try {
          pc.restartIce();
        } catch (error) {
          console.warn("restart ICE ไม่สำเร็จ:", error);
        }
      }
    };

    pc.onnegotiationneeded = async () => {
      if (!currentRoomId || !peers.has(normalizedUserId)) return;
      try {
        entry.makingOffer = true;
        await pc.setLocalDescription();
        if (!pc.localDescription) return;
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

  function removePeer(userId) {
    const key = String(userId);
    const entry = peers.get(key);
    if (!entry) return;
    try {
      entry.pc.close();
    } catch (_) {}
    peers.delete(key);
  }

  async function flushPendingCandidates(entry) {
    const candidates = entry?.pendingCandidates || [];
    entry.pendingCandidates = [];
    for (const candidate of candidates) {
      try {
        await entry.pc.addIceCandidate(candidate);
      } catch (error) {
        console.warn("เพิ่ม buffered ICE ไม่สำเร็จ:", error);
      }
    }
  }

  async function handleWebRTCSignal(signal) {
    if (!signal?.fromUserId || !signal?.type) return;
    const fromUserId = String(signal.fromUserId);
    if (String(me.id) === fromUserId) return;

    const entry = peers.get(fromUserId) || createPeer(fromUserId);
    const pc = entry.pc;

    try {
      if (signal.type === "OFFER") {
        if (!signal.payload) return;
        const offer = JSON.parse(signal.payload);
        const offerCollision =
          entry.makingOffer || pc.signalingState !== "stable";
        entry.ignoreOffer = offerCollision && !entry.polite;
        if (entry.ignoreOffer) return;

        await pc.setRemoteDescription(offer);
        await pc.setLocalDescription();
        wsPublish(`/app/rooms/${currentRoomId}/call`, {
          type: "ANSWER",
          targetUserId: fromUserId,
          payload: JSON.stringify(pc.localDescription),
        });
        await flushPendingCandidates(entry);
        return;
      }

      if (signal.type === "ANSWER") {
        if (!signal.payload) return;
        await pc.setRemoteDescription(JSON.parse(signal.payload));
        await flushPendingCandidates(entry);
        return;
      }

      if (signal.type === "ICE") {
        if (!signal.payload) return;
        const candidate = JSON.parse(signal.payload);
        if (pc.remoteDescription) {
          try {
            await pc.addIceCandidate(candidate);
          } catch (error) {
            if (!entry.ignoreOffer) console.warn("เพิ่ม ICE ไม่สำเร็จ:", error);
          }
        } else {
          entry.pendingCandidates.push(candidate);
        }
      }
    } catch (error) {
      if (!entry.ignoreOffer)
        console.error("WebRTC signal error:", error, signal);
    }
  }

  function onCallSignal(signal) {
    if (!signal || !me?.id) return;
    const myId = String(me.id);
    const fromUserId =
      signal.fromUserId == null ? null : String(signal.fromUserId);
    const targetUserId =
      signal.targetUserId == null ? null : String(signal.targetUserId);

    if (fromUserId === myId) return;
    if (targetUserId && targetUserId !== myId) return;

    if (signal.type === "JOIN") {
      if (fromUserId) createPeer(fromUserId);
      return;
    }

    if (signal.type === "LEAVE") {
      if (fromUserId) removePeer(fromUserId);
      return;
    }
    if (signal.type === "MEDIA") {
      if (!fromUserId || !signal.payload) return;

      try {
        const mediaState = JSON.parse(signal.payload);

        window.dispatchEvent(
          new CustomEvent("cp-call-media-state", {
            detail: {
              userId: fromUserId,
              videoEnabled: mediaState.videoEnabled !== false,
            },
          }),
        );
      } catch (error) {
        console.error("อ่าน MEDIA signal ไม่สำเร็จ:", error);
      }

      return;
    }

    if (["OFFER", "ANSWER", "ICE"].includes(signal.type)) {
      handleWebRTCSignal(signal);
    }
  }

  function subscribeRoomCall(roomId) {
    if (!stompClient?.connected || !roomId) return;
    unsubscribeRoomCall();
    roomCallSubscription = stompClient.subscribe(
      `/topic/rooms/${roomId}/call`,
      (frame) => {
        try {
          onCallSignal(JSON.parse(frame.body));
        } catch (error) {
          console.error("อ่าน call signal ไม่ได้:", error);
        }
      },
    );
  }

  function unsubscribeRoomCall() {
    if (!roomCallSubscription) return;
    try {
      roomCallSubscription.unsubscribe();
    } catch (_) {}
    roomCallSubscription = null;
  }

  async function startMedia(mode) {
    const normalizedMode = mode === "VIDEO" ? "VIDEO" : "VOICE";

    if (localStream) {
      if (
        normalizedMode === "VIDEO" &&
        localStream.getVideoTracks().length === 0
      ) {
        const videoStream = await navigator.mediaDevices.getUserMedia({
          video: true,
        });
        videoStream.getVideoTracks().forEach((track) => {
          localStream.addTrack(track);
          peers.forEach(({ pc }) => pc.addTrack(track, localStream));
        });
      }
      currentMode = normalizedMode;
      showLocalStream(localStream, normalizedMode);
      return localStream;
    }

    currentMode = normalizedMode;
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: normalizedMode === "VIDEO",
    });
    showLocalStream(localStream, normalizedMode);
    return localStream;
  }

  async function joinRoomCall(roomId, mode) {
    if (!roomId) throw new Error("ไม่พบ roomId");
    await loadMe();
    currentRoomId = roomId;
    currentMode = mode === "VIDEO" ? "VIDEO" : "VOICE";
    activeCall = true;
    await startMedia(currentMode);

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          subscribeRoomCall(roomId);
          if (!wsPublish(`/app/rooms/${roomId}/call`, { type: "JOIN" })) {
            throw new Error("ส่ง JOIN ไม่สำเร็จ");
          }
          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  function leaveCall() {
    const roomId = currentRoomId;
    if (roomId && stompClient?.connected) {
      wsPublish(`/app/rooms/${roomId}/call`, { type: "LEAVE" });
    }

    unsubscribeRoomCall();
    peers.forEach(({ pc }) => {
      try {
        pc.close();
      } catch (_) {}
    });
    peers.clear();

    if (localStream) {
      localStream.getTracks().forEach((track) => {
        try {
          track.stop();
        } catch (_) {}
      });
    }
    localStream = null;

    const localVideo = document.getElementById("videoCallLocalVideo");
    const remoteVideo = document.getElementById("videoCallRemoteVideo");
    if (localVideo) localVideo.srcObject = null;
    if (remoteVideo) remoteVideo.srcObject = null;

    if (remoteAudio) {
      remoteAudio.srcObject = null;
      remoteAudio.remove();
      remoteAudio = null;
    }

    currentRoomId = null;
    currentMode = null;
    activeCall = false;
    currentCallFriendId = null;
  }

  function toggleMicrophone() {
    const track = localStream?.getAudioTracks?.()[0];
    if (!track) return false;
    track.enabled = !track.enabled;
    return track.enabled;
  }

  function toggleCamera() {
    const track = localStream?.getVideoTracks?.()[0];

    if (!track) return false;

    track.enabled = !track.enabled;

    if (currentRoomId && stompClient?.connected) {
      wsPublish(`/app/rooms/${currentRoomId}/call`, {
        type: "MEDIA",
        payload: JSON.stringify({
          videoEnabled: track.enabled,
        }),
      });
    }

    return track.enabled;
  }

  function showRemoteStream(stream) {
    if (!remoteAudio) {
      remoteAudio = document.createElement("audio");
      remoteAudio.id = "remoteCallAudio";
      remoteAudio.autoplay = true;
      remoteAudio.playsInline = true;
      remoteAudio.setAttribute("aria-hidden", "true");
      remoteAudio.style.display = "none";
      document.body.appendChild(remoteAudio);
    }
    remoteAudio.srcObject = stream;
    remoteAudio.play().catch(() => {});
  }

  function showLocalStream(stream, mode) {
    const video = document.getElementById("videoCallLocalVideo");
    if (!video) return;
    video.srcObject = stream;
    video.muted = true;
    video.autoplay = true;
    video.playsInline = true;
    video.style.display = mode === "VIDEO" ? "block" : "none";
    video.play().catch(() => {});
  }

  function showRemoteVideo(stream) {
    const video = document.getElementById("videoCallRemoteVideo");
    if (!video) return;
    video.srcObject = stream;
    video.autoplay = true;
    video.playsInline = true;
    video.style.display = "block";
    video.play().catch(() => {});
    const placeholder =
      video.parentElement?.querySelector(".video-placeholder");
    if (placeholder) placeholder.style.display = "none";
  }

  async function startFriendCall(friendId, roomId, mode) {
    if (!friendId) throw new Error("ไม่พบ friendId");
    if (!roomId) throw new Error("ไม่พบ roomId");
    await loadMe();

    currentCallFriendId = friendId;
    currentRoomId = roomId;
    currentMode = mode === "VIDEO" ? "VIDEO" : "VOICE";
    activeCall = false;

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          if (
            !wsPublish("/app/call", {
              type: "INVITE",
              toUserId: friendId,
              roomId,
              mode: currentMode,
            })
          )
            throw new Error("ส่งสายเรียกเข้าไม่สำเร็จ");
          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  async function acceptFriendCall(signal) {
    if (!signal?.fromUserId || !signal?.roomId)
      throw new Error("ข้อมูลสายเรียกเข้าไม่ครบ");
    await loadMe();

    currentRoomId = signal.roomId;
    currentCallFriendId = signal.fromUserId;
    currentMode = signal.mode === "VIDEO" ? "VIDEO" : "VOICE";
    activeCall = true;

    await startMedia(currentMode);

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          if (
            !wsPublish("/app/call", {
              type: "ACCEPT",
              toUserId: signal.fromUserId,
              roomId: signal.roomId,
              mode: currentMode,
            })
          )
            throw new Error("ส่ง ACCEPT ไม่สำเร็จ");

          subscribeRoomCall(signal.roomId);
          if (
            !wsPublish(`/app/rooms/${signal.roomId}/call`, { type: "JOIN" })
          ) {
            throw new Error("ส่ง JOIN ไม่สำเร็จ");
          }
          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  async function declineFriendCall(signal) {
    if (!signal?.fromUserId) return;
    await loadMe();
    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          if (
            !wsPublish("/app/call", {
              type: "DECLINE",
              toUserId: signal.fromUserId,
              roomId: signal.roomId || null,
            })
          )
            throw new Error("ส่ง DECLINE ไม่สำเร็จ");
          resolve();
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  async function cancelFriendCall(signal = null) {
    const targetUserId = signal?.fromUserId || currentCallFriendId;
    const roomId = signal?.roomId || currentRoomId;
    if (!targetUserId) return false;
    await loadMe();
    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          if (
            !wsPublish("/app/call", {
              type: "CANCEL",
              toUserId: targetUserId,
              roomId: roomId || null,
            })
          )
            throw new Error("ส่ง CANCEL ไม่สำเร็จ");
          resolve(true);
        } catch (error) {
          reject(error);
        }
      });
    });
  }

  function subscribeIncomingCalls() {
    if (!stompClient?.connected || !me?.id) return;
    if (incomingCallSubscription) {
      try {
        incomingCallSubscription.unsubscribe();
      } catch (_) {}
    }
    incomingCallSubscription = stompClient.subscribe(
      `/topic/call/${me.id}`,
      (frame) => {
        try {
          window.dispatchEvent(
            new CustomEvent("cp-call-signal", {
              detail: JSON.parse(frame.body),
            }),
          );
        } catch (error) {
          console.error("อ่าน incoming call ไม่ได้:", error);
        }
      },
    );
  }

  window.addEventListener("pagehide", () => {
    leaveCall();
    if (stompClient) {
      try {
        stompClient.deactivate();
      } catch (_) {}
    }
  });

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
