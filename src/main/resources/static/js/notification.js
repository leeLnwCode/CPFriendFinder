document.addEventListener("DOMContentLoaded", () => {
  const notificationList = document.getElementById("friendRequestList");
  const messageList = document.querySelector(".message-list");

  const escapeHtml=value=>{const el=document.createElement("span");el.textContent=value??"";return el.innerHTML;};
  let notificationSocket = null, stopped = false, reconnectTimer;
  let stompSubscriptionId = null;

  /* =====================================================
     LOAD FRIEND REQUESTS
  ====================================================== */

  async function loadFriendRequests() {
    if (!notificationList) {
      return;
    }

    try {
      const response = await fetch("/api/friend-requests/incoming", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดคำขอเป็นเพื่อนไม่สำเร็จ (${response.status})`);
      }

      const requests = await response.json();

      notificationList.innerHTML = "";

      if (!requests || requests.length === 0) {
        notificationList.innerHTML = `
          <div class="friend-empty">
            ไม่มีคำขอเป็นเพื่อน
          </div>
        `;

        return;
      }

      requests.forEach((request) => {
        createFriendRequestCard(request);
      });
    } catch (error) {
      console.error("โหลดคำขอเป็นเพื่อนล้มเหลว:", error);

      notificationList.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถโหลดคำขอเป็นเพื่อนได้
        </div>
      `;
    }
  }

  /* =====================================================
     CREATE FRIEND REQUEST CARD
  ====================================================== */

  function createFriendRequestCard(request) {
    const card = document.createElement("div");

    card.className = "friend-request-card";

    card.dataset.requestId = request.id;

    const fullname =
      `${request.firstname || ""} ${request.lastname || ""}`.trim();

    const image = request.imageUrl || "/images/avatar-placeholder.svg";

    card.innerHTML = `
      <div
        class="friend-info friend-profile-trigger"
        data-profile
        data-id="${escapeHtml(request.userId)}"
        data-name="${escapeHtml(fullname || "ไม่ระบุชื่อ")}"
        data-image="${escapeHtml(image)}"
      >
        <div class="friend-avatar">
          <img
            src="${escapeHtml(image)}"
            alt="${escapeHtml(fullname || "เพื่อน")}"
          />
        </div>

        <div class="friend-detail">
          <div class="friend-name">
            ${escapeHtml(fullname || "ไม่ระบุชื่อ")}
          </div>

          <div class="friend-year"></div>
        </div>
      </div>

      <div class="request-actions">
        <button
          type="button"
          class="accept-button"
          onclick="acceptRequest(this)"
        >
          ยอมรับ
        </button>

        <button
          type="button"
          class="decline-button"
          onclick="declineRequest(this)"
        >
          ปฏิเสธ
        </button>
      </div>
    `;

    notificationList.appendChild(card);
  }

  /* =====================================================
     ACCEPT FRIEND REQUEST
  ====================================================== */

  window.acceptRequest = async function (button) {
    const card = button.closest(".friend-request-card");

    if (!card) {
      return;
    }

    const requestId = card.dataset.requestId;

    if (!requestId) {
      return;
    }

    try {
      button.disabled = true;

      const response = await fetch(
        `/api/friend-requests/${requestId}/accept`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        if([404,409].includes(response.status)){await loadFriendRequests();return;}
        throw new Error(`Accept ไม่สำเร็จ (${response.status})`);
      }

      removeRequestCard(card);
      window.dispatchEvent(new CustomEvent("cp-friends-updated"));
    } catch (error) {
      console.error("ยอมรับคำขอเป็นเพื่อนล้มเหลว:", error);

      button.disabled = false;

      alert("ไม่สามารถยอมรับคำขอเป็นเพื่อนได้");
    }
  };

  /* =====================================================
     DECLINE FRIEND REQUEST
  ====================================================== */

  window.declineRequest = async function (button) {
    const card = button.closest(".friend-request-card");

    if (!card) {
      return;
    }

    const requestId = card.dataset.requestId;

    if (!requestId) {
      return;
    }

    try {
      button.disabled = true;

      const response = await fetch(
        `/api/friend-requests/${requestId}/decline`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        if([404,409].includes(response.status)){await loadFriendRequests();return;}
        throw new Error(`Decline ไม่สำเร็จ (${response.status})`);
      }

      removeRequestCard(card);
      window.dispatchEvent(new CustomEvent("cp-friends-updated"));
    } catch (error) {
      console.error("ปฏิเสธคำขอเป็นเพื่อนล้มเหลว:", error);

      button.disabled = false;

      alert("ไม่สามารถปฏิเสธคำขอเป็นเพื่อนได้");
    }
  };

  /* =====================================================
     REMOVE FRIEND REQUEST CARD
  ====================================================== */

  function removeRequestCard(card) {
    card.style.transition = "0.25s";
    card.style.opacity = "0";

    setTimeout(() => {
      card.remove();

      const remainingCards = notificationList.querySelectorAll(
        ".friend-request-card",
      );

      if (remainingCards.length === 0) {
        notificationList.innerHTML = `
          <div class="friend-empty">
            ไม่มีคำขอเป็นเพื่อน
          </div>
        `;
      }
    }, 250);
  }

  /* =====================================================
     LOAD EXISTING MESSAGE NOTIFICATIONS
  ====================================================== */

  async function loadMessageNotifications() {
    if (!messageList) {
      return;
    }

    try {
      const response = await fetch(
        "/api/notifications?unread=false&page=0&size=20",
        {
          method: "GET",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(
          `โหลดการแจ้งเตือนไม่สำเร็จ (${response.status})`,
        );
      }

      const notifications = await response.json();

      messageList.innerHTML = "";

      const messageNotifications = notifications.filter(
        (notification) => notification.type === "NEW_MESSAGE",
      );

      if (messageNotifications.length === 0) {
        showNoMessageNotification();
        return;
      }

      messageNotifications.forEach((notification) => {
        createMessageNotification(notification);
      });
    } catch (error) {
      console.error("โหลดข้อความแจ้งเตือนล้มเหลว:", error);

      messageList.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถโหลดข้อความแจ้งเตือนได้
        </div>
      `;
    }
  }

  /* =====================================================
     CREATE MESSAGE NOTIFICATION
  ====================================================== */

  function createMessageNotification(notification, isNew = false) {
    if (!messageList) {
      return;
    }

    const actor = notification.actor;

    const fullname = actor
      ? `${actor.firstname || ""} ${actor.lastname || ""}`.trim()
      : "ไม่ระบุชื่อ";

    const image =
      actor && actor.imageUrl
        ? actor.imageUrl
        : "/images/avatar-placeholder.svg";

    const item = document.createElement("div");

    item.className = "message-item";

    if (!notification.isRead) {
      item.classList.add("unread");
    }

    item.dataset.notificationId = notification.id || "";
    item.dataset.roomId = notification.roomId || "";

    item.innerHTML = `
      <div class="message-avatar">
        <img
          src="${escapeHtml(image)}"
          alt="${escapeHtml(fullname)}"
        />
      </div>

      <div class="message-text">
        ${escapeHtml(fullname)} ส่งข้อความใหม่ถึงคุณ!
      </div>
    `;

    /*
     * ถ้าเป็นข้อความใหม่ ให้เอาไว้ด้านบนสุด
     */
    if (isNew) {
      messageList.prepend(item);
    } else {
      messageList.appendChild(item);
    }

    /*
     * กด notification
     */
    item.addEventListener("click", async () => {
      const notificationId = item.dataset.notificationId;
      const roomId = item.dataset.roomId;

      /*
       * mark notification เป็นอ่านแล้ว
       */
      if (notificationId) {
        try {
          const readResponse = await fetch(
            `/api/notifications/${notificationId}/read`,
            {
              method: "POST",
              headers: {
                Accept: "application/json",
              },
              credentials: "include",
            },
          );

          if (readResponse.ok) item.classList.remove("unread");
        } catch (error) {
          console.error(
            "mark notification เป็นอ่านแล้วไม่สำเร็จ:",
            error,
          );
        }
      }

      /*
       * ถ้ามี roomId ให้เปิดห้องนั้น
       */
      if (roomId) {
        try {
          const response = await fetch(`/api/chats/${encodeURIComponent(roomId)}`, {credentials: "include"});
          if (!response.ok) throw new Error("ไม่สามารถเปิดบทสนทนาได้");
          const room = await response.json();
          window.location.href = room.roomType === "DIRECT"
            ? `/friend?roomId=${encodeURIComponent(roomId)}` : `/room?id=${encodeURIComponent(roomId)}`;
        } catch (error) { alert(error.message); }
      }
    });
  }

  /* =====================================================
     EMPTY MESSAGE NOTIFICATION
  ====================================================== */

  function showNoMessageNotification() {
    if (!messageList) {
      return;
    }

    messageList.innerHTML = `
      <div class="friend-empty">
        ไม่มีข้อความใหม่
      </div>
    `;
  }

  /* =====================================================
     GET CURRENT USER
  ====================================================== */

  async function getCurrentUser() {
    const response = await fetch("/api/users/me", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(
        `โหลดข้อมูลผู้ใช้ไม่สำเร็จ (${response.status})`,
      );
    }

    return await response.json();
  }

  /* =====================================================
     LOAD UNREAD COUNT
  ====================================================== */

  async function loadUnreadCount() {
    try {
      const response = await fetch(
        "/api/notifications/unread-count",
        {
          method: "GET",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error(
          `โหลดจำนวนแจ้งเตือนไม่สำเร็จ (${response.status})`,
        );
      }

      const data = await response.json();

      console.log(
        "จำนวน Notification ที่ยังไม่อ่าน:",
        data.count,
      );

      /*
       * ตอนนี้ notification.html ยังไม่มี badge
       * ดังนั้นเก็บไว้ใน console ก่อน
       */
    } catch (error) {
      console.error(
        "โหลดจำนวนแจ้งเตือนไม่สำเร็จ:",
        error,
      );
    }
  }

  /* =====================================================
     STOMP FRAME HELPERS
  ====================================================== */

  function sendStompFrame(command, headers = {}, body = "") {
    if (!notificationSocket) {
      return;
    }

    let frame = `${command}\n`;

    Object.entries(headers).forEach(([key, value]) => {
      frame += `${key}:${value}\n`;
    });

    frame += `\n`;
    frame += body;
    frame += "\0";

    notificationSocket.send(frame);
  }

  /* =====================================================
     PARSE STOMP FRAME
  ====================================================== */

  function parseStompFrame(data) {
    const nullIndex = data.indexOf("\0");

    const frameText =
      nullIndex >= 0
        ? data.substring(0, nullIndex)
        : data;

    const separatorIndex = frameText.indexOf("\n\n");

    if (separatorIndex === -1) {
      return null;
    }

    const headerText = frameText.substring(
      0,
      separatorIndex,
    );

    const body = frameText.substring(
      separatorIndex + 2,
    );

    const lines = headerText.split("\n");

    const command = lines.shift();

    const headers = {};

    lines.forEach((line) => {
      const separator = line.indexOf(":");

      if (separator === -1) {
        return;
      }

      const key = line.substring(0, separator);
      const value = line.substring(separator + 1);

      headers[key] = value;
    });

    return {
      command,
      headers,
      body,
    };
  }

  /* =====================================================
     CONNECT NOTIFICATION WEBSOCKET
  ====================================================== */

  async function connectNotificationWebSocket() {
    try {
      const user = await getCurrentUser();

      if (!user || !user.id) {
        throw new Error("ไม่พบ user.id ของผู้ใช้ปัจจุบัน");
      }

      console.log(
        "Current notification user:",
        user.id,
      );

      const protocol =
        window.location.protocol === "https:"
          ? "wss:"
          : "ws:";

      const socketUrl =
        `${protocol}//${window.location.host}/ws`;

      notificationSocket =
        new WebSocket(socketUrl);

      notificationSocket.onopen = () => {
        console.log(
          "Notification WebSocket connected",
        );

        /*
         * STOMP CONNECT
         */
        sendStompFrame("CONNECT", {
          "accept-version": "1.2",
          host: window.location.host,
        });
      };

      notificationSocket.onmessage = (event) => {
        const frame = parseStompFrame(event.data);

        if (!frame) {
          return;
        }

        console.log(
          "Notification STOMP frame:",
          frame,
        );

        /*
         * Server ตอบ CONNECTED
         */
        if (frame.command === "CONNECTED") {
          console.log(
            "Notification STOMP connected",
          );

          stompSubscriptionId =
            `notification-${user.id}`;

          sendStompFrame("SUBSCRIBE", {
            id: stompSubscriptionId,
            destination:
              `/topic/notifications/${user.id}`,
            ack: "auto",
          });

          console.log(
            "Subscribed:",
            `/topic/notifications/${user.id}`,
          );

          return;
        }

        /*
         * มี notification ใหม่
         */
        if (frame.command === "MESSAGE") {
          try {
            const notification =
              JSON.parse(frame.body);
            if(notification.type === "FRIEND_REQUEST") loadFriendRequests();

            console.log(
              "New notification received:",
              notification,
            );

            /*
             * สนใจเฉพาะข้อความใหม่
             */
            if (
              notification.type === "NEW_MESSAGE"
            ) {
              createMessageNotification(
                notification,
                true,
              );

              /*
               * ถ้ามีข้อความใหม่เข้ามา
               * เอา "ไม่มีข้อความใหม่" ออก
               */
              const emptyMessage =
                messageList?.querySelector(
                  ".friend-empty",
                );

              if (emptyMessage) {
                emptyMessage.remove();
              }
            }
          } catch (error) {
            console.error(
              "อ่าน notification ไม่สำเร็จ:",
              error,
            );
          }
        }
      };

      notificationSocket.onerror = (error) => {
        console.error(
          "Notification WebSocket error:",
          error,
        );
      };

      notificationSocket.onclose = () => {
        console.log(
          "Notification WebSocket disconnected",
        );

        if (!stopped) { clearTimeout(reconnectTimer); reconnectTimer = setTimeout(connectNotificationWebSocket,5000); }
      };
    } catch (error) {
      console.error(
        "เชื่อมต่อ Notification WebSocket ไม่สำเร็จ:",
        error,
      );
    }
  }

  /* =====================================================
     INITIAL LOAD
  ====================================================== */

  window.addEventListener("focus", () => { loadFriendRequests(); loadMessageNotifications(); loadUnreadCount(); });
  window.addEventListener("cp-friends-updated",loadFriendRequests);
  const refreshTimer=setInterval(()=>{if(!document.hidden){loadFriendRequests();loadMessageNotifications();loadUnreadCount();}},15000);
  window.addEventListener('pagehide',()=>{stopped=true;clearInterval(refreshTimer);clearTimeout(reconnectTimer);notificationSocket?.close();},{once:true});
  loadFriendRequests();

  loadMessageNotifications();

  loadUnreadCount();

  connectNotificationWebSocket();
});