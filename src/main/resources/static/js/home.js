document.addEventListener("DOMContentLoaded", function () {
  const $ = (id) => document.getElementById(id);

  const roomGrid = $("roomGrid");

  if (!roomGrid) {
    return;
  }

  // =====================================================
  // CURRENT USER
  // =====================================================

  const currentUser = {
    name: "Singha Waraha",
    year: "ปี 1 CS",
    image: "/images/man.jpg",
  };

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

    if (value === "other") {
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

  function goToRoom(room, options = {}) {
    const isOwner = options.isOwner !== undefined ? options.isOwner : false;

    const user = options.currentUser || currentUser;

    const members = Number(room.members || 0);

    const query = [
      ["name", room.name || ""],
      ["members", members],
      ["max", room.max || "10"],

      ["owner", room.owner || ""],
      ["ownerYear", room.ownerYear || ""],
      ["ownerImage", room.ownerImage || ""],

      ["code", room.code || ""],

      ["currentUser", user.name || ""],
      ["currentUserYear", user.year || ""],
      ["currentUserImage", user.image || ""],

      ["isOwner", isOwner],
    ]
      .map(([key, value]) => {
        return key + "=" + encodeURIComponent(value);
      })
      .join("&");

    window.location.href = "/room?" + query;
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

      const matchInterest = interest === "" || roomInterests.includes(interest);

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
  // CREATE ROOM MODAL
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

  const interestCheckboxes = roomInterest.querySelectorAll(
    'input[type="checkbox"]',
  );

  const yearCheckboxes = roomYearSelect.querySelectorAll(
    'input[name="roomYear"]',
  );

  // =====================================================
  // CLOSE DROPDOWN WHEN CLICK OUTSIDE
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

    if (roomInterest) {
      roomInterest.removeAttribute("open");
    }

    if (roomYearSelect) {
      roomYearSelect.removeAttribute("open");
    }
  });

  // =====================================================
  // INTEREST CHECKBOX
  // =====================================================

  interestCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener("change", function () {
      const selected = checkedValues(interestCheckboxes);

      selectedInterestText.textContent =
        selected.length === 0 ? "เลือกความสนใจ" : selected.join(", ");
    });
  });

  // =====================================================
  // YEAR CHECKBOX
  // =====================================================

  function updateSelectedYearText() {
    const selected = Array.from(yearCheckboxes)
      .filter((item) => item.checked)
      .map(function (item) {
        return item.parentElement.textContent.trim();
      });

    selectedYearText.textContent =
      selected.length === 0 ? "เลือกชั้นปี" : selected.join(", ");
  }

  yearCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener("change", function () {
      if (this.checked) {
        yearCheckboxes.forEach(function (item) {
          const isAll = item.value === "all";

          const thisIsAll = checkbox.value === "all";

          if (isAll !== thisIsAll) {
            item.checked = false;
          }
        });
      }

      updateSelectedYearText();
    });
  });

  // =====================================================
  // CREATE ROOM
  // =====================================================

  confirmCreateRoom.addEventListener("click", function () {
    const name = roomName.value.trim();

    const description = roomDescription.value.trim();

    const maxPeople = roomMax.value || "10";

    const type = roomType.value || "Public";

    const selectedYears = checkedValues(yearCheckboxes);

    const selectedInterests = checkedValues(interestCheckboxes);

    // ---------------------------------------------
    // VALIDATION
    // ---------------------------------------------

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

    // ---------------------------------------------
    // ROOM CODE
    // ---------------------------------------------

    const roomCode =
      type === "Private"
        ? Math.floor(100000 + Math.random() * 900000).toString()
        : "";

    // ---------------------------------------------
    // DISPLAY TEXT
    // ---------------------------------------------

    const yearText = selectedYears.map(yearLabel).join(", ");

    const interestTags = selectedInterests
      .map(function (interest) {
        return `
              <span>
                ${escapeHtml(interest)}
              </span>
            `;
      })
      .join("");

    const yearTags = selectedYears
      .map(function (value) {
        return `
              <span>
                ${escapeHtml(yearLabel(value))}
              </span>
            `;
      })
      .join("");

    // ---------------------------------------------
    // CREATE ROOM CARD
    // ---------------------------------------------

    const newRoom = document.createElement("article");

    newRoom.className = "room-card";

    // ---------------------------------------------
    // DATA
    // ---------------------------------------------

    newRoom.dataset.year = selectedYears.join(",");

    newRoom.dataset.interest = selectedInterests.join(",");

    // สำคัญ:
    // ตอนสร้างห้อง เจ้าของห้อง = 1 คน
    newRoom.dataset.members = "1";

    newRoom.dataset.max = maxPeople;

    newRoom.dataset.owner = currentUser.name;

    newRoom.dataset.ownerYear = currentUser.year;

    newRoom.dataset.ownerImage = currentUser.image;

    newRoom.dataset.type = type;

    newRoom.dataset.code = roomCode;

    // ---------------------------------------------
    // CARD HTML
    // ---------------------------------------------

    newRoom.innerHTML = `
        <h3>
          ${escapeHtml(name)}
        </h3>

        <p class="room-description">
          1/${escapeHtml(maxPeople)}
          คนกำลังคุย
          &nbsp;&nbsp;
          ${escapeHtml(description) || "ยังไม่มีคำอธิบาย"}
        </p>

        <div class="room-tags">
          ${interestTags}
          ${yearTags}
        </div>

        <div class="room-owner">
          <img
            src="${currentUser.image}"
            alt="${escapeHtml(currentUser.name)}"
          />

          <div>
            <strong>
              ${escapeHtml(currentUser.name)}
            </strong>

            <small>
              ${escapeHtml(yearText)}
            </small>
          </div>
        </div>

        <div class="room-bottom">
          <span class="room-type">
            ${escapeHtml(type)}
          </span>

          <button
            type="button"
            class="join-button"
          >
            เข้าห้อง
          </button>
        </div>
      `;

    roomGrid.prepend(newRoom);

    // ---------------------------------------------
    // RESET MODAL
    // ---------------------------------------------

    roomName.value = "";

    roomDescription.value = "";

    roomMax.value = "10";

    roomType.value = "Public";

    yearCheckboxes.forEach(function (checkbox) {
      checkbox.checked = checkbox.value === "1";
    });

    selectedYearText.textContent = "ปี 1";

    roomYearSelect.removeAttribute("open");

    interestCheckboxes.forEach(function (checkbox) {
      checkbox.checked = false;
    });

    selectedInterestText.textContent = "เลือกความสนใจ";

    roomInterest.removeAttribute("open");

    createRoomModal.style.display = "none";

    // ---------------------------------------------
    // เข้า ROOM ทันที
    // ---------------------------------------------
    //
    // เพราะคนสร้างห้องคือเจ้าของ
    // ดังนั้น:
    // members = 1
    // isOwner = true
    //
    // ---------------------------------------------

    const createdRoom = {
      name: name,
      members: "1",
      max: maxPeople,

      owner: currentUser.name,
      ownerYear: currentUser.year,
      ownerImage: currentUser.image,

      code: roomCode,
    };

    goToRoom(createdRoom, {
      isOwner: true,
      currentUser: currentUser,
    });
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

  let pendingRoom = null;

  // =====================================================
  // OPEN PRIVATE ROOM
  // =====================================================

  function openPrivateRoom(room) {
    pendingRoom = room;

    privateRoomName.textContent = "กรุณากรอกรหัสเพื่อเข้าห้อง " + room.name;

    privateRoomCode.value = "";

    privateRoomError.textContent = "";

    privateRoomModal.style.display = "flex";

    privateRoomCode.focus();
  }

  // =====================================================
  // CLOSE PRIVATE ROOM
  // =====================================================

  function closePrivateRoom() {
    pendingRoom = null;

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
  // SUBMIT PRIVATE ROOM
  // =====================================================

  function submitPrivateRoom() {
    if (!pendingRoom) {
      return;
    }

    const code = privateRoomCode.value.trim();

    if (code.length !== 6) {
      showPrivateError("กรุณากรอกรหัส 6 หลัก");
      return;
    }

    if (pendingRoom.code && code !== pendingRoom.code) {
      showPrivateError("รหัสไม่ถูกต้อง");
      return;
    }

    // ---------------------------------------------
    // คนที่เข้าห้องทีหลัง
    // เพิ่มจำนวนอีก 1
    // ---------------------------------------------

    const joinedRoom = {
      name: pendingRoom.name,

      members: Number(pendingRoom.members || 0) + 1,

      max: pendingRoom.max || "10",

      owner: pendingRoom.owner || "",

      ownerYear: pendingRoom.ownerYear || "",

      ownerImage: pendingRoom.ownerImage || "",

      code: pendingRoom.code || "",
    };

    closePrivateRoom();

    goToRoom(joinedRoom, {
      isOwner: false,
      currentUser: currentUser,
    });
  }

  // =====================================================
  // PRIVATE BUTTONS
  // =====================================================

  cancelPrivateRoom.onclick = function () {
    closePrivateRoom();
  };

  confirmPrivateRoom.onclick = function () {
    submitPrivateRoom();
  };

  privateRoomCode.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
      submitPrivateRoom();
    }
  });

  // =====================================================
  // CLICK OUTSIDE PRIVATE MODAL
  // =====================================================

  privateRoomModal.addEventListener("click", function (event) {
    if (event.target === privateRoomModal) {
      closePrivateRoom();
    }
  });

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

  roomGrid.addEventListener("click", function (event) {
    const button = event.target.closest(".join-button");

    if (!button) {
      return;
    }

    const roomCard = button.closest(".room-card");

    if (!roomCard) {
      return;
    }

    const titleElement = roomCard.querySelector("h3");

    if (!titleElement) {
      return;
    }

    // ---------------------------------------------
    // ข้อมูลห้องจากการ์ด
    // ---------------------------------------------

    const room = {
      name: titleElement.textContent.trim(),

      members: roomCard.dataset.members || "0",

      max: roomCard.dataset.max || "10",

      owner: roomCard.dataset.owner || "",

      ownerYear: roomCard.dataset.ownerYear || "",

      ownerImage: roomCard.dataset.ownerImage || "",

      code: roomCard.dataset.code || "",
    };

    const typeElement = roomCard.querySelector(".room-type");

    const type = typeElement ? typeElement.textContent.trim() : "Public";

    // ---------------------------------------------
    // PRIVATE
    // ---------------------------------------------

    if (type === "Private") {
      openPrivateRoom(room);
      return;
    }

    // ---------------------------------------------
    // PUBLIC
    // ---------------------------------------------

    const joinedRoom = {
      ...room,

      members: Number(room.members || 0) + 1,
    };

    goToRoom(joinedRoom, {
      isOwner: false,
      currentUser: currentUser,
    });
  });
});
