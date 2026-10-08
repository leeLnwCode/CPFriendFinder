document.addEventListener("DOMContentLoaded", () => {
  const notificationList = document.getElementById("friendRequestList");

  const messageList = document.querySelector(".message-list");

  const notificationBadge = document.getElementById("notificationBadge");

  let currentUser = null;

  let stompClient = null;

  let notificationSubscription = null;

  let isLoadingFriendRequests = false;

  let isLoadingMessages = false;

  /* =====================================================
     UTILITY
  ====================================================== */

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function getFullName(user) {
    const firstname = user?.firstname || "";
    const lastname = user?.lastname || "";

    return `${firstname} ${lastname}`.trim() || "ไม่ระบุชื่อ";
  }

  function normalizeInterests(interests) {
    if (!interests) {
      return [];
    }

    if (Array.isArray(interests)) {
      return interests
        .map((interest) => {
          if (typeof interest === "string") {
            return interest.trim();
          }

          if (interest && typeof interest === "object") {
            return String(interest.name || "").trim();
          }

          return "";
        })
        .filter(Boolean);
    }

    return [];
  }

  /* =====================================================
     CURRENT USER
  ====================================================== */

  async function getCurrentUser() {
    const response = await fetch("/api/users/me", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      credentials: "include",
    });

    if (response.status === 401 || response.status === 403) {
      window.location.href = "/login";
      throw new Error("ยังไม่ได้เข้าสู่ระบบ");
    }

    if (!response.ok) {
      throw new Error(`โหลดข้อมูลผู้ใช้ไม่สำเร็จ (${response.status})`);
    }

    return await response.json();
  }

  /* =====================================================
     LOAD FRIEND REQUESTS
  ====================================================== */

  async function loadFriendRequests() {
    if (!notificationList || isLoadingFriendRequests) {
      return;
    }

    isLoadingFriendRequests = true;

    try {
      const response = await fetch("/api/friend-requests/incoming", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        return;
      }

      if (!response.ok) {
        throw new Error(`โหลดคำขอเป็นเพื่อนไม่สำเร็จ (${response.status})`);
      }

      const requests = await response.json();

      notificationList.innerHTML = "";

      if (!Array.isArray(requests) || requests.length === 0) {
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
    } finally {
      isLoadingFriendRequests = false;
    }
  }

  /* =====================================================
     CREATE FRIEND REQUEST CARD
  ====================================================== */

  function createFriendRequestCard(request) {
    if (!notificationList || !request) {
      return;
    }

    const card = document.createElement("div");

    card.className = "friend-request-card";

    card.dataset.requestId = request.id || "";

    const userId = request.userId || "";

    const fullname = getFullName(request);

    const image = request.imageUrl || "/images/man.jpg";

    const year =
      request.year !== null && request.year !== undefined
        ? `ปี ${request.year}`
        : "";

    const bio = request.bio || "";

    const interests = normalizeInterests(request.interests);

    const interestsJson = JSON.stringify(interests);

    /*
     * สำคัญ:
     *
     * data-id = userId
     * ไม่ใช่ request.id
     *
     * เพราะ Friend Profile Popup ต้องใช้
     * ID ของ "ผู้ส่งคำขอ"
     */
    card.innerHTML = `
      <div
        class="friend-info friend-profile-trigger"
        data-profile
        data-id="${escapeHtml(userId)}"
        data-name="${escapeHtml(fullname)}"
        data-image="${escapeHtml(image)}"
        data-year="${escapeHtml(request.year ?? "")}"
        data-bio="${escapeHtml(bio)}"
        data-interests="${escapeHtml(interests.join(", "))}"
        data-status="none"
      >

        <div class="friend-avatar">
          <img
            src="${escapeHtml(image)}"
            alt="${escapeHtml(fullname)}"
            onerror="this.src='/images/man.jpg'"
          />
        </div>

        <div class="friend-detail">

          <div class="friend-name">
            ${escapeHtml(fullname)}
          </div>

          <div class="friend-year">
            ${escapeHtml(year)}
          </div>

        </div>

      </div>


      <div class="request-actions">

        <button
          type="button"
          class="accept-button"
          data-action="accept"
          data-request-id="${escapeHtml(request.id || "")}"
        >
          ยอมรับ
        </button>

        <button
          type="button"
          class="decline-button"
          data-action="decline"
          data-request-id="${escapeHtml(request.id || "")}"
        >
          ปฏิเสธ
        </button>

      </div>
    `;

    /*
     * เก็บ interests ไว้แบบ JSON ด้วย
     * เผื่อ popup ต้องการข้อมูล object ในอนาคต
     */
    card.dataset.interests = interestsJson;

    notificationList.appendChild(card);
  }

  /* =====================================================
     ACCEPT REQUEST
  ====================================================== */

  async function acceptRequest(button) {
    if (!button) {
      return;
    }

    const requestId = button.dataset.requestId;

    if (!requestId) {
      console.error("ไม่พบ friend request ID");
      return;
    }

    const card = button.closest(".friend-request-card");

    const buttons = card?.querySelectorAll("button");

    if (buttons) {
      buttons.forEach((item) => {
        item.disabled = true;
      });
    }

    try {
      const response = await fetch(
        `/api/friend-requests/${encodeURIComponent(requestId)}/accept`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
          `ยอมรับคำขอไม่สำเร็จ (${response.status}) ${errorText}`,
        );
      }

      await response.json();

      removeRequestCard(card);

      await updateNotificationBadge();

      if (window.Swal) {
        await window.Swal.fire({
          icon: "success",
          title: "เพิ่มเพื่อนสำเร็จ",
          text: "คุณเป็นเพื่อนกันแล้ว",
          confirmButtonText: "ตกลง",
        });
      }
    } catch (error) {
      console.error("ยอมรับคำขอไม่สำเร็จ:", error);

      if (buttons) {
        buttons.forEach((item) => {
          item.disabled = false;
        });
      }

      if (window.Swal) {
        await window.Swal.fire({
          icon: "error",
          title: "เกิดข้อผิดพลาด",
          text: "ไม่สามารถยอมรับคำขอเป็นเพื่อนได้",
          confirmButtonText: "ตกลง",
        });
      }
    }
  }

  /* =====================================================
     DECLINE REQUEST
  ====================================================== */

  async function declineRequest(button) {
    if (!button) {
      return;
    }

    const requestId = button.dataset.requestId;

    if (!requestId) {
      console.error("ไม่พบ friend request ID");
      return;
    }

    const card = button.closest(".friend-request-card");

    const buttons = card?.querySelectorAll("button");

    if (buttons) {
      buttons.forEach((item) => {
        item.disabled = true;
      });
    }

    try {
      const response = await fetch(
        `/api/friend-requests/${encodeURIComponent(requestId)}/decline`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
          },
          credentials: "include",
        },
      );

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
          `ปฏิเสธคำขอไม่สำเร็จ (${response.status}) ${errorText}`,
        );
      }

      await response.json();

      removeRequestCard(card);

      await updateNotificationBadge();

      if (window.Swal) {
        await window.Swal.fire({
          icon: "success",
          title: "ปฏิเสธคำขอแล้ว",
          confirmButtonText: "ตกลง",
        });
      }
    } catch (error) {
      console.error("ปฏิเสธคำขอไม่สำเร็จ:", error);

      if (buttons) {
        buttons.forEach((item) => {
          item.disabled = false;
        });
      }

      if (window.Swal) {
        await window.Swal.fire({
          icon: "error",
          title: "เกิดข้อผิดพลาด",
          text: "ไม่สามารถปฏิเสธคำขอเป็นเพื่อนได้",
          confirmButtonText: "ตกลง",
        });
      }
    }
  }

  /* =====================================================
     REMOVE REQUEST CARD
  ====================================================== */

  function removeRequestCard(card) {
    if (!card) {
      return;
    }

    card.style.opacity = "0";
    card.style.transform = "scale(0.98)";

    setTimeout(() => {
      card.remove();

      if (
        notificationList &&
        !notificationList.querySelector(".friend-request-card")
      ) {
        notificationList.innerHTML = `
          <div class="friend-empty">
            ไม่มีคำขอเป็นเพื่อน
          </div>
        `;
      }
    }, 180);
  }

  /* =====================================================
     EVENT DELEGATION
  ====================================================== */

  if (notificationList) {
    notificationList.addEventListener("click", (event) => {
      const button = event.target.closest("button[data-action]");

      if (!button) {
        return;
      }

      const action = button.dataset.action;

      if (action === "accept") {
        acceptRequest(button);
        return;
      }

      if (action === "decline") {
        declineRequest(button);
      }
    });
  }

  /* =====================================================
     LOAD MESSAGE NOTIFICATIONS
  ====================================================== */

  async function loadMessageNotifications() {
    if (!messageList || isLoadingMessages) {
      return;
    }

    isLoadingMessages = true;

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

      if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        return;
      }

      if (!response.ok) {
        throw new Error(`โหลดข้อความแจ้งเตือนไม่สำเร็จ (${response.status})`);
      }

      const notifications = await response.json();

      messageList.innerHTML = "";

      const messages = Array.isArray(notifications)
        ? notifications.filter(
            (notification) => notification?.type === "NEW_MESSAGE",
          )
        : [];

      if (messages.length === 0) {
        messageList.innerHTML = `
          <div class="friend-empty">
            ไม่มีข้อความ
          </div>
        `;

        return;
      }

      messages.forEach((notification) => {
        createMessageNotification(notification);
      });
    } catch (error) {
      console.error("โหลดข้อความแจ้งเตือนล้มเหลว:", error);

      messageList.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถโหลดข้อความได้
        </div>
      `;
    } finally {
      isLoadingMessages = false;
    }
  }

  /* =====================================================
     CREATE MESSAGE NOTIFICATION
  ====================================================== */

  function createMessageNotification(notification) {
    const item = document.createElement("div");

    item.className = "message-notification-item";

    if (!notification.isRead) {
      item.classList.add("unread");
    }

    item.dataset.notificationId = notification.id || "";

    item.dataset.roomId = notification.roomId || "";

    const actor = notification.actor;

    const image = actor?.imageUrl || "/images/man.jpg";

    const actorName = getFullName(actor);

    item.innerHTML = `
      <div class="message-notification-avatar">

        <img
          src="${escapeHtml(image)}"
          alt="${escapeHtml(actorName)}"
          onerror="this.src='/images/man.jpg'"
        />

      </div>


      <div class="message-notification-content">

        <div class="message-notification-title">
          ${escapeHtml(notification.title || "ข้อความใหม่")}
        </div>

        <div class="message-notification-text">
          ${escapeHtml(notification.message || "มีข้อความใหม่")}
        </div>

      </div>
    `;

    item.addEventListener("click", async () => {
      await openMessageNotification(notification);
    });

    messageList.appendChild(item);
  }

  /* =====================================================
     OPEN MESSAGE NOTIFICATION
  ====================================================== */

  async function openMessageNotification(notification) {
    if (!notification) {
      return;
    }

    if (notification.id) {
      try {
        await fetch(
          `/api/notifications/${encodeURIComponent(notification.id)}/read`,
          {
            method: "POST",
            headers: {
              Accept: "application/json",
            },
            credentials: "include",
          },
        );
      } catch (error) {
        console.error("Mark notification read failed:", error);
      }
    }

    await updateNotificationBadge();

    if (notification.roomId) {
      window.location.href = `/room?id=${encodeURIComponent(
        notification.roomId,
      )}`;
    }
  }

  /* =====================================================
     NOTIFICATION BADGE
  ====================================================== */

  async function updateNotificationBadge() {
    if (!notificationBadge) {
      return;
    }

    try {
      const response = await fetch("/api/notifications/unread-count", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดจำนวนแจ้งเตือนไม่สำเร็จ (${response.status})`);
      }

      const data = await response.json();

      const count = Number(data?.count || 0);

      if (!Number.isFinite(count) || count <= 0) {
        notificationBadge.textContent = "";
        notificationBadge.classList.remove("show");
        return;
      }

      notificationBadge.textContent = count > 99 ? "99+" : String(count);

      notificationBadge.classList.add("show");
    } catch (error) {
      console.error("โหลด Badge ไม่สำเร็จ:", error);
    }
  }

  /* =====================================================
     STOMP CONNECTION
  ====================================================== */

  function connectNotificationWebSocket() {
    if (!currentUser?.id) {
      return;
    }

    if (typeof StompJs === "undefined" || !StompJs.Client) {
      console.error("ไม่พบ StompJs library");
      return;
    }

    if (stompClient && stompClient.connected) {
      return;
    }

    if (stompClient) {
      return;
    }

    const scheme = window.location.protocol === "https:" ? "wss" : "ws";

    stompClient = new StompJs.Client({
      brokerURL: `${scheme}://${window.location.host}/ws`,

      reconnectDelay: 5000,

      debug: () => {},
    });

    stompClient.onStompError = (frame) => {
      console.error("STOMP error:", frame.headers?.message);
    };

    stompClient.onWebSocketError = (error) => {
      console.error("WebSocket error:", error);
    };

    stompClient.onConnect = () => {
      console.log("Notification WebSocket connected");

      /*
       * ป้องกัน subscribe ซ้ำ
       */
      if (notificationSubscription) {
        try {
          notificationSubscription.unsubscribe();
        } catch (error) {
          console.warn("ยกเลิก subscription เดิมไม่สำเร็จ:", error);
        }

        notificationSubscription = null;
      }

      notificationSubscription = stompClient.subscribe(
        `/topic/notifications/${currentUser.id}`,
        async (frame) => {
          try {
            const notification = JSON.parse(frame.body);

            if (!notification || !notification.type) {
              return;
            }

            /*
             * FRIEND_REQUEST
             *
             * คนอื่นส่งคำขอเป็นเพื่อนมา
             */
            if (notification.type === "FRIEND_REQUEST") {
              await loadFriendRequests();

              await updateNotificationBadge();

              return;
            }

            /*
             * NEW_MESSAGE
             *
             * มีข้อความใหม่
             */
            if (notification.type === "NEW_MESSAGE") {
              await loadMessageNotifications();

              await updateNotificationBadge();

              return;
            }

            /*
             * Notification type อื่น
             */
            await updateNotificationBadge();
          } catch (error) {
            console.error("อ่าน Notification WebSocket ไม่สำเร็จ:", error);
          }
        },
      );

      /*
       * sync badge ทันทีหลัง connect
       */
      updateNotificationBadge();
    };

    stompClient.activate();
  }

  /* =====================================================
     STOP WEBSOCKET
  ====================================================== */

  function disconnectNotificationWebSocket() {
    if (!stompClient) {
      return;
    }

    try {
      if (notificationSubscription) {
        notificationSubscription.unsubscribe();
      }
    } catch (error) {
      console.warn("ยกเลิก Notification subscription ไม่สำเร็จ:", error);
    }

    notificationSubscription = null;

    stompClient.deactivate();

    stompClient = null;
  }

  /* =====================================================
     PAGEHIDE
  ====================================================== */

  window.addEventListener("pagehide", () => {
    disconnectNotificationWebSocket();
  });

  /* =====================================================
     START
  ====================================================== */

  async function init() {
    try {
      currentUser = await getCurrentUser();

      /*
       * โหลดข้อมูลพร้อมกัน
       */
      await Promise.all([
        loadFriendRequests(),
        loadMessageNotifications(),
        updateNotificationBadge(),
      ]);

      /*
       * WebSocket ต้องหลังจากรู้ user.id
       */
      connectNotificationWebSocket();
    } catch (error) {
      console.error("Notification page initialization failed:", error);
    }
  }

  init();
});
