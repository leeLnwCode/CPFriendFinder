document.addEventListener("DOMContentLoaded", () => {
  // =========================================================
  // ELEMENTS
  // =========================================================

  const roomGrid = document.getElementById("roomGrid");
  const roomSearch = document.getElementById("roomSearch");
  const yearFilter = document.getElementById("yearFilter");
  const interestFilter = document.getElementById("interestFilter");
  const createRoomModal = document.getElementById("createRoomModal");
  const createRoomForm = document.getElementById("createRoomForm");
  const roomInterestOptions = document.getElementById("roomInterestOptions");
  const roomType = document.getElementById("roomType");
  const roomPasswordField = document.getElementById("roomPasswordField");
  const roomPassword = document.getElementById("roomPassword");
  const createRoomError = document.getElementById("createRoomError");
  const confirmCreateRoom = document.getElementById("confirmCreateRoom");

  const createRoomButton =
    document.getElementById("createRoomButton");
  const closeCreateRoomButton =
    document.getElementById("closeCreateRoom");
  const cancelCreateRoomButton =
    document.getElementById("cancelCreateRoom");

  const roomNavigationButtons =
    document.querySelectorAll(".room-navigation button");

  // =========================================================
  // STATE
  // =========================================================

  let currentPage = 0;
  const pageSize = 20;

  let searchTimeout = null;
  let activeInterests = [];

  // =========================================================
  // API BASE
  // =========================================================

  const API_BASE = "";

  // =========================================================
  // LOAD INTERESTS
  // =========================================================

  async function loadInterests() {
    try {
      const response = await fetch(
        `${API_BASE}/api/interests`,
        {
          method: "GET",
          headers: {
            Accept: "*/*",
          },
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const interests = await response.json();
      activeInterests = interests.filter((interest) => interest.isActive);

      if (interestFilter) {
        interestFilter.replaceChildren(
          new Option("ทุกความสนใจ", "")
        );

        activeInterests.forEach((interest) => {
          const option = document.createElement("option");

          option.value = interest.id;
          option.textContent = interest.name;

          interestFilter.appendChild(option);
        });
      }

      renderCreateRoomInterests();
    } catch (error) {
      console.error(
        "โหลดความสนใจไม่สำเร็จ:",
        error
      );

      if (interestFilter) {
        interestFilter.replaceChildren(
          new Option("โหลดความสนใจไม่สำเร็จ", "")
        );
      }

      if (roomInterestOptions) {
        roomInterestOptions.innerHTML =
          '<p class="create-room-interest-empty">โหลดความสนใจไม่สำเร็จ</p>';
      }

      showMessage(
        "ไม่สามารถโหลดรายการความสนใจได้",
        "error"
      );
    }
  }
  // =========================================================
  // LOAD ROOMS
  // =========================================================

  async function loadRooms() {
    try {
      showLoading();

      const params = new URLSearchParams();

      // -------------------------
      // Search
      // -------------------------

      const search =
        roomSearch?.value.trim() || "";

      if (search) {
        params.append("search", search);
      }

      // -------------------------
      // Interest
      // -------------------------

      const interestId =
        interestFilter?.value || "";

      if (interestId) {
        params.append(
          "interestId",
          interestId
        );
      }

      // -------------------------
      // Pagination
      // -------------------------

      params.append(
        "page",
        currentPage
      );

      params.append(
        "size",
        pageSize
      );

      const url =
        `${API_BASE}/api/chats/discover?${params.toString()}`;

      console.log("GET:", url);

      const response = await fetch(url, {
        method: "GET",
        headers: {
          Accept: "*/*",
        },
      });

      if (!response.ok) {
        throw new Error(
          `HTTP ${response.status}`
        );
      }

      const rooms = await response.json();

      console.log("Rooms:", rooms);

      renderRooms(rooms);

      updateNavigation(rooms);

    } catch (error) {
      console.error(
        "โหลดห้องไม่สำเร็จ:",
        error
      );

      roomGrid.innerHTML = `
        <div class="room-empty">
          <div class="room-empty-icon">
            ⚠
          </div>

          <h3>
            โหลดห้องไม่สำเร็จ
          </h3>

          <p>
            กรุณาลองใหม่อีกครั้ง
          </p>
        </div>
      `;

      showMessage(
        "ไม่สามารถโหลดรายการห้องได้",
        "error"
      );
    }
  }

  // =========================================================
  // RENDER ROOMS
  // =========================================================

  function renderRooms(rooms) {
    roomGrid.innerHTML = "";

    if (
      !Array.isArray(rooms) ||
      rooms.length === 0
    ) {
      roomGrid.innerHTML = `
        <div class="room-empty">

          <div class="room-empty-icon">
            ⌕
          </div>

          <h3>
            ไม่พบห้อง
          </h3>

          <p>
            ลองเปลี่ยนคำค้นหาหรือความสนใจดู
          </p>

        </div>
      `;

      return;
    }

    rooms.forEach((room) => {
      const card =
        createRoomCard(room);

      roomGrid.appendChild(card);
    });
  }


  // =========================================================
  // CREATE ROOM CARD
  // =========================================================

  function createRoomCard(room) {
    const card = document.createElement("div");
    card.className = "room-card";

    if (room.isPrivate) {
      card.classList.add("private");
    }

    const memberCount = Number(room.memberCount || 0);
    const maxMembers = Number(room.maxMembers || 0);
    const isFull = maxMembers > 0 && memberCount >= maxMembers;

    const interests = Array.isArray(room.interests)
      ? room.interests.filter((interest) => interest?.name)
      : [];

    const maxVisibleInterests = 3;
    const visibleInterests = interests.slice(0, maxVisibleInterests);
    const remainingInterestCount = Math.max(
      interests.length - visibleInterests.length,
      0
    );

    const interestHTML = visibleInterests.length > 0
      ? visibleInterests
          .map(
            (interest) => `
              <span class="room-interest">
                ${escapeHTML(interest.name)}
              </span>
            `
          )
          .join("")
      : `
          <span class="room-interest room-interest-empty">
            ทั่วไป
          </span>
        `;

    const remainingInterestHTML = remainingInterestCount > 0
      ? `
          <span class="room-interest room-interest-more">
            +${remainingInterestCount}
          </span>
        `
      : "";

    const description = String(room.description || "").trim();
    const descriptionHTML = description
      ? `
          <p class="room-description">
            ${escapeHTML(description)}
          </p>
        `
      : "";

    const targetYear = Number(room.targetYear);
    const targetYearText =
      Number.isInteger(targetYear) && targetYear > 0
        ? `ปี ${targetYear}`
        : "ทุกชั้นปี";

    const roomTypeText = room.isPrivate ? "Private" : "Public";

    card.innerHTML = `
      <div class="room-card-header">
        <div class="room-card-icon">
          ${room.isPrivate ? "🔒" : "💬"}
        </div>

        <div class="room-card-type ${room.isPrivate ? "private" : "public"}">
          ${roomTypeText}
        </div>
      </div>

      <div class="room-card-body">
        <h3 class="room-name">
          ${escapeHTML(room.roomName || "ไม่มีชื่อห้อง")}
        </h3>

        ${descriptionHTML}

        <div class="room-interests" aria-label="ความสนใจ">
          ${interestHTML}
          ${remainingInterestHTML}
        </div>
      </div>

      <div class="room-card-footer">
        <div class="room-members">
          <span>👥</span>
          <span>${memberCount}/${maxMembers}</span>
        </div>

        <div class="room-target-year">
          🎓 ${escapeHTML(targetYearText)}
        </div>

        ${
          Number(room.unreadCount || 0) > 0
            ? `
                <div class="room-unread">
                  ${room.unreadCount}
                </div>
              `
            : ""
        }

        <div class="room-card-status">
          ${
            isFull
              ? `<span class="room-full">เต็มแล้ว</span>`
              : `<span class="room-available">ว่าง</span>`
          }
        </div>
      </div>
    `;

    card.addEventListener("click", () => {
      if (isFull) {
        showMessage("ห้องนี้เต็มแล้ว", "warning");
        return;
      }

      if (room.isPrivate) {
        openPrivateRoomSweetAlert(room);
        return;
      }

      joinRoom(room);
    });

    return card;
  }

  // =========================================================
  // JOIN ROOM
  // =========================================================

  async function joinRoom(room, password = null) {
    try {
      const response = await fetch(
        `${API_BASE}/api/chats/${encodeURIComponent(room.id)}/join`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "*/*",
          },
          body: JSON.stringify(
            password === null ? {} : { password }
          ),
        }
      );

      if (!response.ok) {
        let message = `HTTP ${response.status}`;

        try {
          const errorData = await response.json();
          message = errorData.message || errorData.detail || message;
        } catch (error) {
          // ไม่มี JSON response
        }

        if (
          response.status === 409 &&
          message.toLowerCase().includes("already a member")
        ) {
          window.location.href = `/room?id=${encodeURIComponent(room.id)}`;
          return null;
        }

        const joinError = new Error(message);
        joinError.status = response.status;
        throw joinError;
      }

      window.location.href = `/room?id=${encodeURIComponent(room.id)}`;
    } catch (error) {
      console.error("เข้าห้องไม่สำเร็จ:", error);

      if (room.isPrivate) {
        return error;
      }

      await Swal.fire({
        icon: "error",
        title: "เข้าห้องไม่สำเร็จ",
        text: error.message || "กรุณาลองใหม่อีกครั้ง",
        confirmButtonText: "ตกลง",
      });

      return error;
    }

    return null;
  }

  // =========================================================
  // PRIVATE ROOM - SWEETALERT
  // =========================================================

  async function openPrivateRoomSweetAlert(room) {
    while (true) {
      const { isConfirmed, value: password } =
        await Swal.fire({
          icon: "info",

          title: "เข้าห้อง Private",

          text:
            `ห้อง "${room.roomName}" ` +
            `กรุณากรอกรหัสเพื่อเข้าห้อง`,

          input: "password",

          inputPlaceholder:
            "กรอกรหัสเข้าห้อง",

          inputAttributes: {
            autocapitalize: "off",
            autocorrect: "off",
          },

          showCancelButton: true,

          confirmButtonText:
            "เข้าห้อง",

          cancelButtonText:
            "ยกเลิก",

          inputValidator: (value) => {
            if (!value || !value.trim()) {
              return "กรุณากรอกรหัสเข้าห้อง";
            }

            return undefined;
          },
        });

      if (!isConfirmed) {
        return;
      }

      const error = await joinRoom(room, password.trim());

      if (!error) {
        return;
      }

      if (error.status !== 403) {
        await Swal.fire({
          icon: "error",
          title: "เข้าห้องไม่สำเร็จ",
          text: error.message || "กรุณาลองใหม่อีกครั้ง",
          confirmButtonText: "ตกลง",
        });
        return;
      }

      await Swal.fire({
        icon: "error",
        title: "รหัสผ่านไม่ถูกต้อง",
        text: "กรุณาตรวจสอบรหัสผ่านแล้วลองอีกครั้ง",
        confirmButtonText: "ลองอีกครั้ง",
      });
    }
  }

  // =========================================================
  // SEARCH
  // =========================================================

  if (roomSearch) {
    roomSearch.addEventListener(
      "input",
      () => {

        clearTimeout(
          searchTimeout
        );

        searchTimeout =
          setTimeout(() => {

            currentPage = 0;

            loadRooms();

          }, 400);
      }
    );
  }

  // =========================================================
  // INTEREST FILTER
  // =========================================================

  if (interestFilter) {
    interestFilter.addEventListener(
      "change",
      () => {

        currentPage = 0;

        loadRooms();
      }
    );
  }

  // =========================================================
  // YEAR FILTER
  // =========================================================

  if (yearFilter) {
    yearFilter.addEventListener(
      "change",
      () => {

        /*
         * /api/chats/discover
         * ตอนนี้ยังไม่มี parameter year
         *
         * ดังนั้นยังไม่ส่ง year ไป backend
         */

        currentPage = 0;

        loadRooms();
      }
    );
  }

  // =========================================================
  // PAGINATION
  // =========================================================

  function updateNavigation(rooms) {
    const prevButton =
      roomNavigationButtons[0];

    const nextButton =
      roomNavigationButtons[1];

    if (
      !prevButton ||
      !nextButton
    ) {
      return;
    }

    prevButton.disabled =
      currentPage <= 0;

    nextButton.disabled =
      !Array.isArray(rooms) ||
      rooms.length < pageSize;
  }

  if (
    roomNavigationButtons.length >= 2
  ) {
    const prevButton =
      roomNavigationButtons[0];

    const nextButton =
      roomNavigationButtons[1];

    // Previous
    prevButton.addEventListener(
      "click",
      () => {

        if (currentPage <= 0) {
          return;
        }

        currentPage--;

        loadRooms();
      }
    );

    // Next
    nextButton.addEventListener(
      "click",
      () => {

        currentPage++;

        loadRooms();
      }
    );
  }


  // =========================================================
  // CREATE ROOM BUTTON
  // =========================================================

  if (createRoomButton) {
    createRoomButton.addEventListener(
      "click",
      () => {
        openCreateRoomModal();
      }
    );
  }

  closeCreateRoomButton?.addEventListener(
    "click",
    closeCreateRoomModal
  );

  cancelCreateRoomButton?.addEventListener(
    "click",
    closeCreateRoomModal
  );

  createRoomModal?.addEventListener("click", (event) => {
    if (event.target === createRoomModal) {
      closeCreateRoomModal();
    }
  });

  document.addEventListener("keydown", (event) => {
    if (
      event.key === "Escape" &&
      createRoomModal?.classList.contains("is-open")
    ) {
      closeCreateRoomModal();
    }
  });

  roomType?.addEventListener("change", () => {
    const isPrivate = roomType.value === "Private";
    roomPasswordField.hidden = !isPrivate;
    roomPassword.required = isPrivate;

    if (!isPrivate) {
      roomPassword.value = "";
    }
  });

  createRoomForm?.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (!createRoomForm.reportValidity()) {
      return;
    }

    const isPrivate = roomType.value === "Private";
    const roomData = {
      roomName: document.getElementById("roomName").value.trim(),
      description: document.getElementById("roomDescription").value.trim(),
      targetYear: document.getElementById("roomYear").value
        ? Number(document.getElementById("roomYear").value)
        : null,
      interestIds: Array.from(
        roomInterestOptions.querySelectorAll("input:checked")
      ).map((checkbox) => checkbox.value),
      maxMembers: Number(document.getElementById("roomMax").value),
      password: isPrivate ? roomPassword.value.trim() : null,
      isPrivate: isPrivate,
    };

    await createRoom(roomData);
  });

  function renderCreateRoomInterests() {
    if (!roomInterestOptions) {
      return;
    }

    roomInterestOptions.replaceChildren();

    if (activeInterests.length === 0) {
      const emptyMessage = document.createElement("p");
      emptyMessage.className = "create-room-interest-empty";
      emptyMessage.textContent = "ไม่มีความสนใจให้เลือก";
      roomInterestOptions.appendChild(emptyMessage);
      return;
    }

    activeInterests.forEach((interest) => {
      const label = document.createElement("label");
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.value = interest.id;

      label.append(checkbox, document.createTextNode(interest.name));
      roomInterestOptions.appendChild(label);
    });
  }

  function openCreateRoomModal() {
    createRoomForm.reset();
    document.getElementById("roomMax").value = "10";
    roomType.dispatchEvent(new Event("change"));
    createRoomError.hidden = true;
    createRoomError.textContent = "";
    createRoomModal.classList.add("is-open");
    createRoomModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    document.getElementById("roomName").focus();
  }

  function closeCreateRoomModal() {
    if (confirmCreateRoom.disabled) {
      return;
    }

    createRoomModal.classList.remove("is-open");
    createRoomModal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    createRoomButton?.focus();
  }

  // =========================================================
  // POST CREATE ROOM
  // =========================================================

  async function createRoom(roomData) {
    try {
      confirmCreateRoom.disabled = true;
      confirmCreateRoom.textContent = "กำลังสร้าง...";
      createRoomError.hidden = true;
      createRoomError.textContent = "";

      console.log(
        "POST /api/chats"
      );

      console.log(
        "Request body:",
        roomData
      );

      // ---------------------------------------------
      // POST
      // ---------------------------------------------

      const response =
        await fetch(
          `${API_BASE}/api/chats`,
          {
            method: "POST",

            headers: {
              "Content-Type":
                "application/json",

              Accept: "*/*",
            },

            body:
              JSON.stringify(
                roomData
              ),
          }
        );

      // ---------------------------------------------
      // Error
      // ---------------------------------------------

      if (!response.ok) {

        let errorMessage =
          `HTTP ${response.status}`;

        try {

          const errorData =
            await response.json();

          if (
            errorData.message
          ) {
            errorMessage =
              errorData.message;
          }

        } catch (error) {
          // ไม่มี JSON response
        }

        throw new Error(
          errorMessage
        );
      }

      // ---------------------------------------------
      // Response
      // ---------------------------------------------

      const createdRoom =
        await response.json();

      console.log(
        "สร้างห้องสำเร็จ:",
        createdRoom
      );

      confirmCreateRoom.disabled = false;
      closeCreateRoomModal();

      window.location.href =
        `/room?id=${encodeURIComponent(
          createdRoom.id
        )}`;

    } catch (error) {

      console.error(
        "สร้างห้องไม่สำเร็จ:",
        error
      );

      createRoomError.textContent =
        error.message || "สร้างห้องไม่สำเร็จ กรุณาลองใหม่อีกครั้ง";
      createRoomError.hidden = false;
    } finally {
      confirmCreateRoom.disabled = false;
      confirmCreateRoom.textContent = "สร้างห้อง";
    }
  }
  // =========================================================
  // LOADING
  // =========================================================

  function showLoading() {
    roomGrid.innerHTML = `
      <div class="room-loading">

        <div
          class="room-loading-spinner"
        ></div>

        <p>
          กำลังโหลดห้อง...
        </p>

      </div>
    `;
  }

  // =========================================================
  // SWEETALERT MESSAGE
  // =========================================================

  function showMessage(
    message,
    icon = "info"
  ) {
    if (
      typeof Swal !== "undefined"
    ) {

      Swal.fire({
        text: message,
        icon: icon,

        confirmButtonText:
          "ตกลง",
      });

    } else {

      alert(message);

    }
  }

  // =========================================================
  // ESCAPE HTML
  // =========================================================

  function escapeHTML(value) {
    return String(value ?? "")
      .replace(
        /&/g,
        "&amp;"
      )
      .replace(
        /</g,
        "&lt;"
      )
      .replace(
        />/g,
        "&gt;"
      )
      .replace(
        /"/g,
        "&quot;"
      )
      .replace(
        /'/g,
        "&#039;"
      );
  }

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  async function init() {
    await loadInterests();

    await loadRooms();
  }

  init();
});