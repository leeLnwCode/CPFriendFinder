document.addEventListener("DOMContentLoaded", function () {
  const $ = (id) => document.getElementById(id);

  const roomGrid = $("roomGrid");

  if (!roomGrid) {
    return;
  }

  // =====================================================
  // HELPERS
  // =====================================================

  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
  }

  function yearLabel(value) {
    if (value === "all") {
      return "ทุกชั้นปี";
    }

    if (value === "5") {
      return "ปีลึก";
    }

    return "ปี " + value;
  }

  function checkedValues(checkboxes) {
    return Array.from(checkboxes)
      .filter((item) => item.checked)
      .map((item) => item.value);
  }

  function splitData(value) {
    return (value || "")
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item !== "");
  }

  // =====================================================
  // GO TO ROOM
  // =====================================================

  function goToRoom(roomId) {
    if (!roomId) {
      alert("ไม่พบรหัสห้อง");
      return;
    }

    window.location.href = "/room?id=" + encodeURIComponent(roomId);
  }

  // =====================================================
  // INTERESTS
  // =====================================================

  let interestList = [];

  async function loadInterests() {
    try {
      const response = await fetch("/api/interests", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดความสนใจไม่สำเร็จ (${response.status})`);
      }

      interestList = await response.json();

      const options = roomInterest.querySelector(".multi-select-options");
      options.replaceChildren();
      interestFilter.replaceChildren(new Option("ทุกความสนใจ", ""));
      interestList
        .filter((interest) => interest.isActive !== false)
        .forEach((interest) => {
          const label = document.createElement("label");
          const checkbox = document.createElement("input");
          checkbox.type = "checkbox";
          checkbox.value = interest.id;
          label.append(checkbox, document.createTextNode(" " + interest.name));
          options.append(label);
          interestFilter.add(new Option(interest.name, interest.name));
        });
      if (!options.children.length)
        options.textContent = "ยังไม่มีความสนใจให้เลือก";
      confirmCreateRoom.disabled = !options.querySelector("input");
    } catch (error) {
      console.error("โหลดความสนใจล้มเหลว:", error);
      roomInterest.querySelector(".multi-select-options").textContent =
        "โหลดความสนใจไม่สำเร็จ กรุณาโหลดหน้านี้ใหม่";
      confirmCreateRoom.disabled = true;
    }
  }

  // =====================================================
  // LOAD ROOMS
  // =====================================================

  async function loadRooms(quiet = false) {
    try {
      const response = await fetch("/api/chats/discover?page=0&size=20", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`โหลดห้องไม่สำเร็จ (${response.status})`);
      }

      const rooms = await response.json();

      roomGrid.innerHTML = "";

      if (!rooms || rooms.length === 0) {
        roomGrid.innerHTML = `
          <div class="friend-empty">
            ยังไม่มีห้องพูดคุย
          </div>
        `;

        return;
      }

      rooms.forEach(function (room) {
        createRoomCard(room);
      });

      filterRooms();
    } catch (error) {
      console.error("โหลดห้องล้มเหลว:", error);

      if (quiet) return;
      roomGrid.innerHTML = `
        <div class="friend-empty">
          ไม่สามารถโหลดห้องพูดคุยได้
        </div>
      `;
    }
  }

  // =====================================================
  // CREATE ROOM CARD
  // =====================================================

  function createRoomCard(room) {
    const card = document.createElement("article");

    card.className = "room-card";

    // UUID ของห้องจาก Backend
    card.dataset.roomId = room.id || "";

    card.dataset.members = room.memberCount || 0;

    card.dataset.max = room.maxMembers || 10;

    card.dataset.private = room.isPrivate ? "true" : "false";

    const interests = (room.interests || []).map(function (interest) {
      return interest.name;
    });

    card.dataset.interest = interests.join(",");

    card.innerHTML = `
      <h3>
        ${escapeHtml(room.roomName || "ห้องไม่มีชื่อ")}
      </h3>

      <p class="room-description">
        ${escapeHtml(String(room.memberCount || 0))}
        /
        ${escapeHtml(String(room.maxMembers || 10))}
        สมาชิกในห้อง
      </p>

      <div class="room-tags">
        ${interests
          .map(function (interest) {
            return `
              <span>
                ${escapeHtml(interest)}
              </span>
            `;
          })
          .join("")}
      </div>

      <div class="room-owner">
        <div>
          <strong>
            ห้องพูดคุย
          </strong>

          <small>
            ${room.isPrivate ? "Private" : "Public"}
          </small>
        </div>
      </div>

      <div class="room-bottom">
        <span class="room-type">
          ${room.isPrivate ? "Private" : "Public"}
        </span>

        <button
          type="button"
          class="join-button"
        >
          เข้าห้อง
        </button>
      </div>
    `;

    roomGrid.appendChild(card);
  }

  // =====================================================
  // FILTER
  // =====================================================

  const searchInput = $("roomSearch");
  const yearFilter = $("yearFilter");
  const interestFilter = $("interestFilter");

  function filterRooms() {
    const search = searchInput ? searchInput.value.toLowerCase().trim() : "";

    const year = yearFilter ? yearFilter.value : "";

    const interest = interestFilter ? interestFilter.value : "";

    document.querySelectorAll(".room-card").forEach(function (room) {
      const roomYears = splitData(room.dataset.year);

      const roomInterests = splitData(room.dataset.interest);

      const matchSearch = room.innerText.toLowerCase().includes(search);

      const matchYear =
        year === "" || roomYears.includes(year) || roomYears.includes("all");

      const matchInterest =
        interest === "" ||
        roomInterests.some(function (roomInterest) {
          return (
            roomInterest.trim().toUpperCase() === interest.trim().toUpperCase()
          );
        });

      room.style.display =
        matchSearch && matchYear && matchInterest ? "flex" : "none";
    });
  }

  if (searchInput) {
    searchInput.addEventListener("input", filterRooms);
  }

  if (yearFilter) {
    yearFilter.addEventListener("change", filterRooms);
  }

  if (interestFilter) {
    interestFilter.addEventListener("change", filterRooms);
  }

  // =====================================================
  // CREATE ROOM MODAL ELEMENTS
  // =====================================================

  const createRoomButton = $("createRoomButton");

  const createRoomModal = $("createRoomModal");

  const cancelCreateRoom = $("cancelCreateRoom");

  const confirmCreateRoom = $("confirmCreateRoom");

  const roomName = $("roomName");

  const roomDescription = $("roomDescription");

  const roomMax = $("roomMax");

  const roomType = $("roomType");

  const roomInterest = $("roomInterest");

  const selectedInterestText = $("selectedInterestText");

  const roomYearSelect = $("roomYearSelect");

  const selectedYearText = $("selectedYearText");

  if (
    !createRoomButton ||
    !createRoomModal ||
    !cancelCreateRoom ||
    !confirmCreateRoom
  ) {
    return;
  }

  const interestCheckboxes = () =>
    roomInterest.querySelectorAll('input[type="checkbox"]');
  confirmCreateRoom.disabled = true;

  const yearCheckboxes = roomYearSelect.querySelectorAll(
    'input[name="roomYear"]',
  );

  // =====================================================
  // DROPDOWN
  // =====================================================

  document.addEventListener("click", function (event) {
    if (roomInterest && !roomInterest.contains(event.target)) {
      roomInterest.removeAttribute("open");
    }

    if (roomYearSelect && !roomYearSelect.contains(event.target)) {
      roomYearSelect.removeAttribute("open");
    }
  });

  // =====================================================
  // INTEREST DROPDOWN
  // =====================================================

  const interestSummary = roomInterest.querySelector("summary");

  if (interestSummary) {
    interestSummary.addEventListener("click", function (event) {
      event.preventDefault();

      roomInterest.open = !roomInterest.open;
    });
  }

  // =====================================================
  // YEAR DROPDOWN
  // =====================================================

  const yearSummary = roomYearSelect.querySelector("summary");

  if (yearSummary) {
    yearSummary.addEventListener("click", function (event) {
      event.preventDefault();

      roomYearSelect.open = !roomYearSelect.open;
    });
  }

  // =====================================================
  // OPEN CREATE ROOM
  // =====================================================

  createRoomButton.addEventListener("click", function () {
    createRoomModal.style.display = "flex";
  });

  // =====================================================
  // CANCEL CREATE ROOM
  // =====================================================

  cancelCreateRoom.addEventListener("click", function () {
    createRoomModal.style.display = "none";

    roomInterest.removeAttribute("open");

    roomYearSelect.removeAttribute("open");
  });

  // =====================================================
  // INTEREST CHECKBOX
  // =====================================================

  roomInterest.addEventListener("change", () => {
    const selected = Array.from(interestCheckboxes())
      .filter((item) => item.checked)
      .map((item) => item.parentElement.textContent.trim());
    selectedInterestText.textContent = selected.length
      ? selected.join(", ")
      : "เลือกความสนใจ";
  });

  // =====================================================
  // YEAR CHECKBOX - เลือกได้แค่ 1 ชั้นปี
  // =====================================================

  function updateSelectedYearText() {
    const selected = Array.from(yearCheckboxes).find(function (item) {
      return item.checked;
    });

    if (!selected) {
      selectedYearText.textContent = "เลือกชั้นปี";
      return;
    }

    if (selected.value === "all") {
      selectedYearText.textContent = "ทุกชั้นปี";
    } else if (selected.value === "5") {
      selectedYearText.textContent = "ปีลึก";
    } else {
      selectedYearText.textContent = "ปี " + selected.value;
    }
  }

  yearCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener("change", function () {
      // ถ้าเลือกอันใหม่
      if (this.checked) {
        yearCheckboxes.forEach(function (otherCheckbox) {
          if (otherCheckbox !== checkbox) {
            otherCheckbox.checked = false;
          }
        });
      }

      // ห้ามไม่มีอันไหนถูกเลือก
      else {
        this.checked = true;
      }

      updateSelectedYearText();
    });
  });

  updateSelectedYearText();

  // =====================================================
  // CREATE ROOM - BACKEND
  // =====================================================

  confirmCreateRoom.addEventListener("click", async function () {
    const name = roomName.value.trim();

    const description = roomDescription.value.trim();

    const maxPeople = Number(roomMax.value || 10);

    const type = roomType.value || "Public";

    const selectedInterests = checkedValues(interestCheckboxes());

    const selectedYears = Array.from(yearCheckboxes)
      .filter(function (checkbox) {
        return checkbox.checked;
      })
      .map(function (checkbox) {
        return checkbox.value;
      });

    // -----------------------------------------------
    // VALIDATION
    // -----------------------------------------------

    if (name === "") {
      alert("กรุณาใส่ชื่อห้อง");
      roomName.focus();
      return;
    }

    if (selectedInterests.length === 0) {
      alert("กรุณาเลือกความสนใจอย่างน้อย 1 อย่าง");
      return;
    }

    if (selectedYears.length === 0) {
      alert("กรุณาเลือกชั้นปีอย่างน้อย 1 อย่าง");
      return;
    }

    if (maxPeople < 2 || maxPeople > 10) {
      alert("จำนวนสมาชิกต้องอยู่ระหว่าง 2-10 คน");
      return;
    }

    // -----------------------------------------------
    // GET INTEREST UUIDs
    // -----------------------------------------------

    const interestIds = Array.from(interestCheckboxes())
      .filter(function (checkbox) {
        return checkbox.checked;
      })
      .map(function (checkbox) {
        return checkbox.value;
      });

    if (interestIds.length !== selectedInterests.length) {
      alert("ไม่สามารถจับคู่ความสนใจบางรายการได้");
      return;
    }

    // -----------------------------------------------
    // PRIVATE PASSWORD
    // -----------------------------------------------

    let password = null;

    if (type === "Private") {
      password = prompt("กรุณาตั้งรหัสเข้าห้อง");

      if (password === null) {
        return;
      }

      password = password.trim();

      if (password.length < 4) {
        alert("รหัสเข้าห้องต้องมีอย่างน้อย 4 ตัวอักษร");
        return;
      }
    }

    // -----------------------------------------------
    // BACKEND REQUEST
    // -----------------------------------------------

    const requestBody = {
      roomName: name,
      description: description || null,
      targetYear: selectedYears[0] === "all" ? null : Number(selectedYears[0]),
      interestIds: interestIds,
      maxMembers: maxPeople,
      password: type === "Private" ? password : null,
      private: type === "Private",
    };

    try {
      confirmCreateRoom.disabled = true;

      const response = await fetch("/api/chats", {
        method: "POST",

        headers: {
          "Content-Type": "application/json",

          Accept: "application/json",
        },

        credentials: "include",

        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        const errorText = await response.text();

        console.error("CREATE ROOM STATUS:", response.status);
        console.error("CREATE ROOM RESPONSE:", errorText);

        alert(
          "สร้างห้องไม่สำเร็จ\nStatus: " + response.status + "\n" + errorText,
        );

        return;
      }

      const createdRoom = await response.json();

      // ---------------------------------------------
      // CLOSE MODAL
      // ---------------------------------------------

      createRoomModal.style.display = "none";

      roomInterest.removeAttribute("open");

      roomYearSelect.removeAttribute("open");

      // ---------------------------------------------
      // RESET FORM
      // ---------------------------------------------

      roomName.value = "";

      roomDescription.value = "";

      roomMax.value = "10";

      roomType.value = "Public";

      interestCheckboxes().forEach(function (checkbox) {
        checkbox.checked = false;
      });

      selectedInterestText.textContent = "เลือกความสนใจ";

      yearCheckboxes.forEach(function (checkbox) {
        checkbox.checked = checkbox.value === "1";
      });

      selectedYearText.textContent = "ปี 1";

      // ---------------------------------------------
      // เข้า Room ที่เพิ่งสร้าง
      // ---------------------------------------------

      if (createdRoom.id) {
        goToRoom(createdRoom.id);
      } else {
        alert("สร้างห้องสำเร็จ แต่ไม่พบรหัสห้อง");

        await loadRooms();
      }
    } catch (error) {
      console.error("สร้างห้องล้มเหลว:", error);

      alert(error.message || "ไม่สามารถสร้างห้องได้");
    } finally {
      confirmCreateRoom.disabled = false;
    }
  });

  // =====================================================
  // PRIVATE ROOM MODAL
  // =====================================================

  const privateRoomModal = $("privateRoomModal");

  const privateRoomName = $("privateRoomName");

  const privateRoomCode = $("privateRoomCode");

  const privateRoomError = $("privateRoomError");

  const cancelPrivateRoom = $("cancelPrivateRoom");

  const confirmPrivateRoom = $("confirmPrivateRoom");

  let pendingRoomId = null;

  // =====================================================
  // OPEN PRIVATE ROOM
  // =====================================================

  function openPrivateRoom(roomId) {
    pendingRoomId = roomId;

    privateRoomName.textContent = "กรุณากรอกรหัสเพื่อเข้าห้อง";

    privateRoomCode.value = "";

    privateRoomError.textContent = "";

    privateRoomModal.style.display = "flex";

    privateRoomCode.focus();
  }

  // =====================================================
  // CLOSE PRIVATE ROOM
  // =====================================================

  function closePrivateRoom() {
    pendingRoomId = null;

    privateRoomModal.style.display = "none";

    privateRoomCode.value = "";

    privateRoomError.textContent = "";
  }

  // =====================================================
  // PRIVATE ERROR
  // =====================================================

  function showPrivateError(message) {
    privateRoomError.textContent = message;

    privateRoomError.style.color = "#d93025";
  }

  // =====================================================
  // JOIN ROOM BACKEND
  // =====================================================

  async function joinRoom(roomId, password = null) {
    const body = password
      ? {
          password: password,
        }
      : {};

    const response = await fetch(`/api/chats/${roomId}/join`, {
      method: "POST",

      headers: {
        "Content-Type": "application/json",

        Accept: "application/json",
      },

      credentials: "include",

      body: JSON.stringify(body),
    });

    if (!response.ok) {
      let message = "ไม่สามารถเข้าห้องได้";

      try {
        const errorData = await response.json();

        if (errorData.message) {
          message = errorData.message;
        }
      } catch (error) {
        // ใช้ข้อความเดิม
      }

      const error = new Error(message);
      error.status = response.status;
      throw error;
    }

    goToRoom(roomId);
  }

  // =====================================================
  // SUBMIT PRIVATE ROOM
  // =====================================================

  async function submitPrivateRoom() {
    if (!pendingRoomId) {
      return;
    }

    const code = privateRoomCode.value.trim();

    if (code === "") {
      showPrivateError("กรุณากรอกรหัสเข้าห้อง");

      return;
    }

    try {
      confirmPrivateRoom.disabled = true;

      await joinRoom(pendingRoomId, code);
    } catch (error) {
      console.error("เข้าห้อง Private ล้มเหลว:", error);

      showPrivateError(error.message || "รหัสไม่ถูกต้อง");
    } finally {
      confirmPrivateRoom.disabled = false;
    }
  }

  // =====================================================
  // PRIVATE BUTTONS
  // =====================================================

  if (cancelPrivateRoom) {
    cancelPrivateRoom.addEventListener("click", closePrivateRoom);
  }

  if (confirmPrivateRoom) {
    confirmPrivateRoom.addEventListener("click", submitPrivateRoom);
  }

  if (privateRoomCode) {
    privateRoomCode.addEventListener("keydown", function (event) {
      if (event.key === "Enter") {
        submitPrivateRoom();
      }
    });
  }

  // =====================================================
  // CLICK OUTSIDE PRIVATE MODAL
  // =====================================================

  if (privateRoomModal) {
    privateRoomModal.addEventListener("click", function (event) {
      if (event.target === privateRoomModal) {
        closePrivateRoom();
      }
    });
  }

  // =====================================================
  // CLICK OUTSIDE CREATE MODAL
  // =====================================================

  createRoomModal.addEventListener("click", function (event) {
    if (event.target === createRoomModal) {
      createRoomModal.style.display = "none";
    }
  });

  // =====================================================
  // ESC
  // =====================================================

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      closePrivateRoom();

      createRoomModal.style.display = "none";
    }
  });

  // =====================================================
  // JOIN ROOM
  // =====================================================

  roomGrid.addEventListener("click", async function (event) {
    const button = event.target.closest(".join-button");

    if (!button) {
      return;
    }

    const roomCard = button.closest(".room-card");

    if (!roomCard) {
      return;
    }

    const roomId = roomCard.dataset.roomId;

    if (!roomId) {
      alert("ไม่พบรหัสห้อง");

      return;
    }

    const isPrivate = roomCard.dataset.private === "true";

    // ---------------------------------------------
    // PRIVATE
    // ---------------------------------------------

    if (isPrivate) {
      // Existing members can resume without entering the room password again.
      button.disabled = true;
      try {
        await joinRoom(roomId);
      } catch (error) {
        if (error.status === 403) openPrivateRoom(roomId);
        else alert(error.message || "ไม่สามารถเข้าห้องได้");
      } finally {
        button.disabled = false;
      }
      return;
    }

    // ---------------------------------------------
    // PUBLIC
    // ---------------------------------------------

    try {
      await joinRoom(roomId);
    } catch (error) {
      console.error("เข้าห้องล้มเหลว:", error);

      alert(error.message || "ไม่สามารถเข้าห้องได้");
    }
  });

  // =====================================================
  // INITIAL LOAD
  // =====================================================

  document.querySelector(".random-button")?.addEventListener("click", () => {
    window.location.href = "/random";
  });
  loadInterests();
  window.addEventListener("cp-rooms-refresh", () => loadRooms(true));
  loadRooms();
});
