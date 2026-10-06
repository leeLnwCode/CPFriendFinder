document.addEventListener("DOMContentLoaded", () => {
  const notificationList = document.getElementById("friendRequestList");

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

    const image = request.imageUrl || "/images/man.jpg";

    card.innerHTML = `
    <div
      class="friend-info friend-profile-trigger"
      data-profile
      data-name="${fullname || "ไม่ระบุชื่อ"}"
      data-image="${image}"
    >

      <div class="friend-avatar">
        <img
          src="${image}"
          alt="${fullname || "เพื่อน"}"
        />
      </div>

      <div class="friend-detail">

        <div class="friend-name">
          ${fullname || "ไม่ระบุชื่อ"}
        </div>

        <div class="friend-year">
          <!-- Backend ตอนนี้ยังไม่มีข้อมูลปี -->
        </div>

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
      /*
       * ป้องกันกดซ้ำ
       */
      button.disabled = true;

      const response = await fetch(`/api/friend-requests/${requestId}/accept`, {
        method: "POST",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Accept ไม่สำเร็จ (${response.status})`);
      }


      removeRequestCard(card);
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
      /*
       * ป้องกันกดซ้ำ
       */
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
        throw new Error(`Decline ไม่สำเร็จ (${response.status})`);
      }

      removeRequestCard(card);
    } catch (error) {
      console.error("ปฏิเสธคำขอเป็นเพื่อนล้มเหลว:", error);

      button.disabled = false;

      alert("ไม่สามารถปฏิเสธคำขอเป็นเพื่อนได้");
    }
  };

  /* =====================================================
     REMOVE CARD
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
     INITIAL LOAD
  ====================================================== */

  loadFriendRequests();
});
