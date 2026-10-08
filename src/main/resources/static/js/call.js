(() => {
  "use strict";

  // ─────────────────────────────────────────────
  // STATE
  // ─────────────────────────────────────────────
  let stompClient   = null;
  const wsWaiters   = [];
  let me            = null;
  let localStream   = null;
  let screenStream  = null;
  let currentRoomId = null;
  let currentMode   = null;
  let activeCall    = false;
  let currentCallFriendId = null;

  // peerId → { pc, polite, makingOffer, ignoreOffer, pendingCandidates, remoteStream }
  const peers = new Map();

  // remoteAudio elements keyed by userId
  const remoteAudios = new Map();

  let roomCallSubscription = null;
  let roomChatSubscription = null;
  let incomingCallSubscription = null;

  // ─────────────────────────────────────────────
  // ICE / TURN CONFIG
  // ─────────────────────────────────────────────
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

  // ─────────────────────────────────────────────
  // USER
  // ─────────────────────────────────────────────
  async function loadMe() {
    if (me?.id) return me;
    const res = await fetch("/api/users/me", { credentials: "include" });
    if (!res.ok) throw new Error("โหลด user ไม่ได้");
    me = await res.json();
    if (!me.id && me.userId) me.id = me.userId;
    return me;
  }

  // ─────────────────────────────────────────────
  // STOMP / WS
  // ─────────────────────────────────────────────
  function connectWS(onConnected) {
    if (stompClient?.connected) { onConnected?.(); return; }
    if (onConnected) wsWaiters.push(onConnected);
    if (stompClient) return; // already connecting

    const scheme = location.protocol === "https:" ? "wss" : "ws";
    console.info("[WS] connecting | userId=", me?.id);

    stompClient = new StompJs.Client({
      brokerURL: `${scheme}://${location.host}/ws`,
      reconnectDelay: 5000,
      debug: (s) => console.debug("[STOMP]", s),
      connectHeaders: { login: me?.id || "" },
    });

    stompClient.onStompError = (f) =>
      console.error("[WS] STOMP error:", f.headers?.message || f.body);

    stompClient.onWebSocketError = (e) =>
      console.error("[WS] WebSocket error:", e);

    stompClient.onConnect = async () => {
      console.info("[WS] connected | me=", me?.id);

      // re-subscribe incoming calls
      if (me?.id) subscribeIncomingCalls();

      // reconnect scenario — restore room subscriptions
      if (currentRoomId && activeCall) {
        subscribeRoomCall(currentRoomId);

        // รอ rollback ทุก peer ที่ค้างใน have-local-offer ให้เสร็จก่อน
        // ถ้าไม่รอ browser จะ fire onnegotiationneeded ใหม่ไม่ได้เพราะยัง have-local-offer อยู่
        const rollbacks = [];
        peers.forEach((entry, peerId) => {
          if (entry.pc.signalingState === "have-local-offer") {
            console.info("[WS] reconnect: rolling back stuck peer", peerId);
            rollbacks.push(
              entry.pc.setLocalDescription({ type: "rollback" })
                .then(() => { entry.makingOffer = false; })
                .catch((e) => console.warn("[WS] rollback failed:", e.message))
            );
          }
        });

        if (rollbacks.length > 0) {
          await Promise.allSettled(rollbacks);
          console.info("[WS] rollback(s) done, sending JOIN");
        }

        wsPublish(`/app/rooms/${currentRoomId}/call`, { type: "JOIN" });
        broadcastMediaState();
      }

      wsWaiters.splice(0).forEach((cb) => { try { cb(); } catch (e) { console.error(e); } });
    };

    stompClient.activate();
  }

  function wsPublish(destination, body) {
    if (!stompClient?.connected) {
      console.warn("[WS] not connected, drop:", destination, body?.type);
      return false;
    }
    stompClient.publish({ destination, body: JSON.stringify(body) });
    if (body?.type !== "ICE" && body?.type !== "MEDIA") {
      console.info("[WS] sent", body?.type, "→", destination);
    }
    return true;
  }

  function subscribeIncomingCalls() {
    if (!me?.id || !stompClient?.connected) return;
    if (incomingCallSubscription) {
      try { incomingCallSubscription.unsubscribe(); } catch (_) {}
    }
    incomingCallSubscription = stompClient.subscribe(
      `/topic/call/${me.id}`,
      (frame) => {
        try { onCallInvite(JSON.parse(frame.body)); } catch (e) { console.error(e); }
      }
    );
  }

  function subscribeRoomCall(roomId) {
    if (!stompClient?.connected || !roomId) return;
    if (roomCallSubscription) {
      try { roomCallSubscription.unsubscribe(); } catch (_) {}
    }
    roomCallSubscription = stompClient.subscribe(
      `/topic/rooms/${roomId}/call`,
      (frame) => {
        try { onRoomSignal(JSON.parse(frame.body), roomId); } catch (e) { console.error(e); }
      }
    );
  }

  function subscribeRoomChat(roomId, onMessage) {
    if (!stompClient?.connected || !roomId) {
      console.warn("[WS] subscribeRoomChat: not ready");
      return;
    }
    if (roomChatSubscription) {
      try { roomChatSubscription.unsubscribe(); } catch (_) {}
    }
    roomChatSubscription = stompClient.subscribe(
      `/topic/rooms/${roomId}`,
      (frame) => {
        try { onMessage(JSON.parse(frame.body)); } catch (e) { console.error(e); }
      }
    );
    console.info("[WS] subscribed room chat:", roomId);
  }

  // ─────────────────────────────────────────────
  // MEDIA
  // ─────────────────────────────────────────────
  async function acquireMedia(video, audio) {
    return navigator.mediaDevices.getUserMedia({ video, audio });
  }

  async function startMedia(mode, opts = {}) {
    const wantVideo = (opts.videoEnabled ?? mode === "VIDEO");
    const wantAudio = (opts.audioEnabled ?? true);

    if (localStream) {
      const needVideo = wantVideo && !getCameraTrack();
      const needAudio = wantAudio && localStream.getAudioTracks().length === 0;
      if (needVideo || needAudio) {
        const extra = await acquireMedia(needVideo, needAudio);
        for (const t of extra.getTracks()) {
          localStream.addTrack(t);
          if (t.kind === "video") {
            for (const { pc } of peers.values()) replaceTrackOnPeer(pc, "video", t, localStream);
          }
        }
      }
      currentMode = mode;
      showLocalStream(localStream, mode);
      return localStream;
    }

    currentMode = mode;
    localStream = await acquireMedia(wantVideo, wantAudio);
    showLocalStream(localStream, mode);
    return localStream;
  }

  function getCameraTrack() {
    return localStream?.getVideoTracks()
      .find((t) => !screenStream?.getTracks().includes(t)) || null;
  }

  // ─────────────────────────────────────────────
  // PEER CONNECTION
  // ─────────────────────────────────────────────

  /**
   * polite = UUID string comparison.
   * peer ที่มี UUID น้อยกว่าเป็น impolite (initiator)
   * peer ที่มี UUID มากกว่าเป็น polite (yields on collision)
   */
  function createPeer(remoteId) {
    const id = String(remoteId);
    if (peers.has(id)) return peers.get(id);

    const pc = new RTCPeerConnection(ICE_CONFIG);
    const entry = {
      pc,
      polite: String(me.id) > id,   // polite = UUID ใหญ่กว่า → yields
      makingOffer: false,
      ignoreOffer: false,
      pendingCandidates: [],
      remoteStream: new MediaStream(),
    };
    peers.set(id, entry);
    console.info("[Peer] created", { local: me.id, remote: id, polite: entry.polite });

    // ── add local tracks ──
    // ใช้ sendrecv เสมอ ตาม Perfect Negotiation standard
    // ถ้าไม่มี track ให้ใส่ null ผ่าน addTransceiver direction=sendrecv
    // เพื่อให้ negotiate ได้สองทางตั้งแต่แรก
    const audioTrack = localStream?.getAudioTracks()[0];
    if (audioTrack) {
      pc.addTrack(audioTrack, localStream);
    } else {
      // ไม่มี track แต่ยัง sendrecv เพื่อรับ remote audio
      pc.addTransceiver("audio", { direction: "sendrecv" });
    }

    const videoTrack = screenStream?.getVideoTracks()[0] || getCameraTrack();
    if (videoTrack?.readyState === "live") {
      pc.addTrack(videoTrack, screenStream || localStream);
    } else {
      // ไม่มี track แต่ยัง sendrecv เพื่อรับ remote video
      pc.addTransceiver("video", { direction: "sendrecv" });
    }

    // ── callbacks ──
    pc.onicecandidate = ({ candidate }) => {
      if (!candidate || !currentRoomId) return;
      wsPublish(`/app/rooms/${currentRoomId}/call`, {
        type: "ICE",
        targetUserId: id,
        payload: JSON.stringify(candidate),
      });
    };

    pc.ontrack = ({ track, streams }) => {
      const stream = streams[0] || entry.remoteStream;
      if (!entry.remoteStream.getTracks().includes(track)) {
        entry.remoteStream.addTrack(track);
      }
      console.info("[Peer] remote track", { remote: id, kind: track.kind });
      window.dispatchEvent(new CustomEvent("cp-call-track", {
        detail: { userId: id, stream, connectionState: pc.connectionState, iceConnectionState: pc.iceConnectionState, signalingState: pc.signalingState },
      }));
      showRemoteStream(stream, id);
      if (currentMode === "VIDEO") showRemoteVideo(stream);
    };

    pc.onconnectionstatechange = () => {
      console.info("[Peer] state", { remote: id, state: pc.connectionState });
      if (pc.connectionState === "connected") {
        window.dispatchEvent(new CustomEvent("cp-call-connected", { detail: { userId: id, mode: currentMode } }));
      }
      if (["failed", "closed"].includes(pc.connectionState)) removePeer(id);
    };

    pc.oniceconnectionstatechange = () => {
      console.info("[Peer] ICE", { remote: id, state: pc.iceConnectionState });
      if (pc.iceConnectionState === "failed") {
        try { pc.restartIce(); } catch (_) {}
      }
    };

    // ── Perfect Negotiation: onnegotiationneeded ──
    pc.onnegotiationneeded = async () => {
      // ต้อง stable เท่านั้น — ถ้าไม่ stable browser จะ fire ซ้ำเองหลัง stable
      if (pc.signalingState !== "stable" || entry.makingOffer) {
        console.info("[Peer] negotiation skip (not stable)", { remote: id, state: pc.signalingState });
        return;
      }
      if (!currentRoomId || !peers.has(id)) return;
      try {
        entry.makingOffer = true;
        await pc.setLocalDescription();            // implicit offer
        wsPublish(`/app/rooms/${currentRoomId}/call`, {
          type: "OFFER",
          targetUserId: id,
          payload: JSON.stringify(pc.localDescription),
        });
        console.info("[Peer] OFFER sent", { local: me.id, remote: id, polite: entry.polite });
      } catch (err) {
        console.error("[Peer] offer error:", err);
      } finally {
        entry.makingOffer = false;
      }
    };

    return entry;
  }

  function removePeer(id) {
    const entry = peers.get(String(id));
    if (!entry) return;
    try { entry.pc.close(); } catch (_) {}
    peers.delete(String(id));
    const audio = remoteAudios.get(String(id));
    if (audio) { audio.srcObject = null; audio.remove(); remoteAudios.delete(String(id)); }
    window.dispatchEvent(new CustomEvent("cp-call-peer-left", { detail: { userId: String(id) } }));
  }

  async function flushCandidates(entry) {
    const list = entry.pendingCandidates.splice(0);
    for (const c of list) {
      try { await entry.pc.addIceCandidate(c); } catch (_) {}
    }
  }

  // ─────────────────────────────────────────────
  // SIGNAL HANDLER (Perfect Negotiation)
  // ─────────────────────────────────────────────
  async function handleSignal(signal) {
    const fromId = String(signal.fromUserId);
    if (fromId === String(me.id)) return;

    const entry = peers.get(fromId) || createPeer(fromId);
    const { pc } = entry;

    try {
      // ── OFFER ──
      if (signal.type === "OFFER") {
        const offer = JSON.parse(signal.payload);
        const collision = entry.makingOffer || pc.signalingState !== "stable";
        entry.ignoreOffer = collision && !entry.polite;

        if (entry.ignoreOffer) {
          console.info("[Peer] OFFER ignored (impolite collision)", { remote: fromId });
          return;
        }

        if (collision) {
          // polite peer: rollback ตัวเอง รับ offer อีกฝั่ง
          console.info("[Peer] polite rollback", { remote: fromId });
          await pc.setLocalDescription({ type: "rollback" });
          entry.makingOffer = false;
        }

        await pc.setRemoteDescription(offer);
        entry.ignoreOffer = false;
        await flushCandidates(entry);

        await pc.setLocalDescription();            // implicit answer
        wsPublish(`/app/rooms/${currentRoomId}/call`, {
          type: "ANSWER",
          targetUserId: fromId,
          payload: JSON.stringify(pc.localDescription),
        });
        console.info("[Peer] ANSWER sent", { local: me.id, remote: fromId });
        return;
      }

      // ── ANSWER ──
      if (signal.type === "ANSWER") {
        if (pc.signalingState !== "have-local-offer") {
          console.info("[Peer] ANSWER ignored (state=" + pc.signalingState + ")", { remote: fromId });
          return;
        }
        await pc.setRemoteDescription(JSON.parse(signal.payload));
        console.info("[Peer] ANSWER applied", { remote: fromId });
        await flushCandidates(entry);
        return;
      }

      // ── ICE ──
      if (signal.type === "ICE") {
        const candidate = JSON.parse(signal.payload);
        if (pc.remoteDescription?.type) {
          try { await pc.addIceCandidate(candidate); }
          catch (e) { if (!entry.ignoreOffer) console.warn("[Peer] ICE add failed:", e.message); }
        } else {
          entry.pendingCandidates.push(candidate);
        }
      }
    } catch (err) {
      if (!entry.ignoreOffer) console.error("[Peer] signal error:", err, signal.type);
    }
  }

  // ─────────────────────────────────────────────
  // ROOM SIGNAL ROUTER
  // ─────────────────────────────────────────────
  function onRoomSignal(signal, roomId) {
    if (!signal || !me?.id) return;
    const myId   = String(me.id);
    const fromId = signal.fromUserId != null ? String(signal.fromUserId) : null;
    const toId   = signal.toUserId   != null ? String(signal.toUserId)   : null;

    if (fromId === myId) return;   // echo จากตัวเอง

    if (signal.type === "JOIN") {
      if (!fromId) return;
      console.info("[Room] JOIN from", fromId);
      createPeer(fromId);
      broadcastMediaState();
      return;
    }

    if (signal.type === "LEAVE") {
      if (fromId) removePeer(fromId);
      return;
    }

    if (signal.type === "MEDIA") {
      let media = null;
      try { media = signal.payload ? JSON.parse(signal.payload) : null; } catch (_) {}
      if (media && fromId) {
        window.dispatchEvent(new CustomEvent("cp-call-media-state", {
          detail: { userId: fromId, ...media },
        }));
      }
      return;
    }

    // directed signals: OFFER / ANSWER / ICE
    if (["OFFER", "ANSWER", "ICE"].includes(signal.type)) {
      if (!toId || toId !== myId) return;  // ไม่ใช่ของเรา
      handleSignal(signal);
    }
  }

  // ─────────────────────────────────────────────
  // TRACK HELPERS
  // ─────────────────────────────────────────────
  function replaceTrackOnPeer(pc, kind, track, stream) {
    // หา transceiver ด้วย sender.track หรือ receiver.track
    const transceiver = pc.getTransceivers().find((t) => {
      if (t.stopped) return false;
      // ตรวจทั้ง sender และ receiver kind
      return (t.sender.track?.kind === kind) ||
             (t.receiver.track?.kind === kind);
    });
    if (!transceiver) {
      // ไม่มี transceiver เลย -- addTrack ใหม่
      if (track && stream) pc.addTrack(track, stream);
      return;
    }
    transceiver.sender.replaceTrack(track || null).catch(() => {});
    if (track) {
      // เปลี่ยน direction ให้ส่งได้
      if (transceiver.direction === "recvonly") {
        transceiver.direction = "sendrecv";
      } else if (transceiver.direction === "inactive") {
        transceiver.direction = "sendonly";
      }
    } else {
      // ไม่มี track -- เปลี่ยนกลับเป็น recvonly
      if (transceiver.direction === "sendrecv") {
        transceiver.direction = "recvonly";
      } else if (transceiver.direction === "sendonly") {
        transceiver.direction = "inactive";
      }
    }
  }

  async function replaceVideoAllPeers(track, stream) {
    for (const { pc } of peers.values()) {
      replaceTrackOnPeer(pc, "video", track, stream);
    }
  }

  function broadcastMediaState() {
    if (!currentRoomId) return;
    wsPublish(`/app/rooms/${currentRoomId}/call`, {
      type: "MEDIA",
      payload: JSON.stringify({
        video: Boolean(getCameraTrack()?.enabled),
        screen: Boolean(screenStream),
      }),
    });
  }

  // ─────────────────────────────────────────────
  // PUBLIC API
  // ─────────────────────────────────────────────

  /** เข้าห้อง */
  async function joinRoomCall(roomId, mode, opts = {}) {
    if (!roomId) throw new Error("ไม่พบ roomId");
    await loadMe();
    currentRoomId = roomId;
    currentMode   = mode === "VIDEO" ? "VIDEO" : "VOICE";
    activeCall    = true;

    if (opts.receiveOnly) {
      localStream = localStream || new MediaStream();
    } else {
      await startMedia(currentMode, opts);
    }

    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          subscribeRoomCall(roomId);
          if (!wsPublish(`/app/rooms/${roomId}/call`, { type: "JOIN" }))
            throw new Error("ส่ง JOIN ไม่สำเร็จ");
          broadcastMediaState();
          resolve();
        } catch (e) { reject(e); }
      });
    });
  }

  /** ออกจากห้อง */
  function leaveCall() {
    const roomId = currentRoomId;
    if (roomId && stompClient?.connected) {
      wsPublish(`/app/rooms/${roomId}/call`, { type: "LEAVE" });
    }
    if (roomCallSubscription) { try { roomCallSubscription.unsubscribe(); } catch (_) {} roomCallSubscription = null; }

    peers.forEach(({ pc }) => { try { pc.close(); } catch (_) {} });
    peers.clear();

    if (screenStream) {
      screenStream.getTracks().forEach((t) => { t.onended = null; t.stop(); });
      screenStream = null;
    }
    if (localStream) {
      localStream.getTracks().forEach((t) => { try { t.stop(); } catch (_) {} });
      localStream = null;
    }

    remoteAudios.forEach((el) => { el.srcObject = null; el.remove(); });
    remoteAudios.clear();

    const lv = document.getElementById("videoCallLocalVideo");
    const rv = document.getElementById("videoCallRemoteVideo");
    if (lv) lv.srcObject = null;
    if (rv) rv.srcObject = null;

    currentRoomId = null;
    currentMode   = null;
    activeCall    = false;
    currentCallFriendId = null;
  }

  /** toggle mic */
  function toggleMicrophone() {
    const t = localStream?.getAudioTracks()[0];
    if (!t) return false;
    t.enabled = !t.enabled;
    return t.enabled;
  }

  /** toggle camera */
  function toggleCamera() {
    const t = getCameraTrack();
    if (!t) return false;
    t.enabled = !t.enabled;
    if (!screenStream) replaceVideoAllPeers(t.enabled ? t : null, localStream).catch(() => {});
    broadcastMediaState();
    return t.enabled;
  }

  async function setRoomCameraEnabled(enabled) {
    const t = getCameraTrack();
    if (!t) return null;
    t.enabled = enabled;
    if (!screenStream) await replaceVideoAllPeers(enabled ? t : null, localStream);
    broadcastMediaState();
    return t.enabled;
  }

  /** screen share */
  async function startScreenShare(stream = null) {
    if (screenStream) return screenStream;
    const capture = stream || await navigator.mediaDevices.getDisplayMedia({ video: true, audio: true });
    const vt = capture.getVideoTracks()[0];
    if (!vt) throw new Error("ไม่พบ video track");
    screenStream = capture;
    await replaceVideoAllPeers(vt, capture);
    vt.onended = () => stopScreenShare();
    broadcastMediaState();
    window.dispatchEvent(new CustomEvent("cp-call-screen-share", { detail: { active: true, stream: capture } }));
    return capture;
  }

  async function stopScreenShare() {
    if (!screenStream) return false;
    const prev = screenStream;
    screenStream = null;
    const cam = getCameraTrack();
    await replaceVideoAllPeers(cam?.enabled ? cam : null, localStream);
    broadcastMediaState();
    prev.getTracks().forEach((t) => { t.onended = null; t.stop(); });
    window.dispatchEvent(new CustomEvent("cp-call-screen-share", { detail: { active: false, stream: localStream } }));
    return true;
  }

  // ─────────────────────────────────────────────
  // DISPLAY HELPERS
  // ─────────────────────────────────────────────
  function showRemoteStream(stream, userId) {
    const key = String(userId);
    let el = remoteAudios.get(key);
    if (!el) {
      el = document.createElement("audio");
      el.autoplay = el.playsInline = true;
      el.setAttribute("aria-hidden", "true");
      el.style.display = "none";
      document.body.appendChild(el);
      remoteAudios.set(key, el);
    }
    el.srcObject = stream;
    el.play().catch(() => {});
  }

  function showLocalStream(stream, mode) {
    const v = document.getElementById("videoCallLocalVideo");
    if (!v) return;
    v.srcObject = stream; v.muted = true; v.autoplay = v.playsInline = true;
    v.style.display = mode === "VIDEO" ? "block" : "none";
    v.play().catch(() => {});
  }

  function showRemoteVideo(stream) {
    const v = document.getElementById("videoCallRemoteVideo");
    if (!v) return;
    v.srcObject = stream; v.autoplay = v.playsInline = true; v.style.display = "block";
    v.play().catch(() => {});
    const ph = v.parentElement?.querySelector(".video-placeholder");
    if (ph) ph.style.display = "none";
  }

  // ─────────────────────────────────────────────
  // FRIEND CALL (INVITE flow)
  // ─────────────────────────────────────────────
  function onCallInvite(signal) {
    window.dispatchEvent(new CustomEvent("cp-incoming-call", { detail: signal }));
  }

  async function startFriendCall(friendId, roomId, mode) {
    await loadMe();
    currentCallFriendId = friendId;
    currentRoomId = roomId;
    currentMode   = mode === "VIDEO" ? "VIDEO" : "VOICE";
    activeCall    = false;
    return new Promise((resolve, reject) => {
      connectWS(() => {
        if (!wsPublish("/app/call", { type: "INVITE", toUserId: friendId, roomId, mode: currentMode }))
          return reject(new Error("ส่งสายเรียกเข้าไม่สำเร็จ"));
        resolve();
      });
    });
  }

  async function acceptFriendCall(signal) {
    await loadMe();
    currentRoomId       = signal.roomId;
    currentCallFriendId = signal.fromUserId;
    currentMode         = signal.mode === "VIDEO" ? "VIDEO" : "VOICE";
    activeCall          = true;
    await startMedia(currentMode);
    return new Promise((resolve, reject) => {
      connectWS(() => {
        try {
          wsPublish("/app/call", { type: "ACCEPT", toUserId: signal.fromUserId, roomId: signal.roomId, mode: currentMode });
          subscribeRoomCall(signal.roomId);
          wsPublish(`/app/rooms/${signal.roomId}/call`, { type: "JOIN" });
          resolve();
        } catch (e) { reject(e); }
      });
    });
  }

  async function declineFriendCall(signal) {
    await loadMe();
    return new Promise((resolve, reject) => {
      connectWS(() => {
        if (!wsPublish("/app/call", { type: "DECLINE", toUserId: signal.fromUserId, roomId: signal.roomId }))
          return reject(new Error("ส่ง decline ไม่สำเร็จ"));
        resolve();
      });
    });
  }

  async function cancelFriendCall(toUserId, roomId) {
    await loadMe();
    return new Promise((resolve, reject) => {
      connectWS(() => {
        if (!wsPublish("/app/call", { type: "CANCEL", toUserId, roomId }))
          return reject(new Error("ส่ง cancel ไม่สำเร็จ"));
        currentCallFriendId = null;
        activeCall = false;
        resolve();
      });
    });
  }

  // ─────────────────────────────────────────────
  // EXPORTS
  // ─────────────────────────────────────────────
  window.CPCall = {
    connectWS,
    wsPublish,
    loadMe,
    subscribeIncomingCalls,
    subscribeRoomChat,
    startMedia,
    joinRoomCall,
    leaveCall,
    toggleMicrophone,
    toggleCamera,
    setRoomCameraEnabled,
    startScreenShare,
    stopScreenShare,
    startFriendCall,
    acceptFriendCall,
    declineFriendCall,
    cancelFriendCall,
    showRemoteVideo,
    getLocalStream:      () => localStream,
    isScreenSharing:     () => Boolean(screenStream),
    getCurrentRoomId:    () => currentRoomId,
    getCurrentMode:      () => currentMode,
    getCurrentFriendId:  () => currentCallFriendId,
    isActive:            () => activeCall,
  };
})();
