document.addEventListener("DOMContentLoaded", function () {
  const $ = (id) => document.getElementById(id);

  const roomGrid = $("roomGrid");

  if (!roomGrid) {
    return;
  }

  // =========================
  // Helpers
  // =========================

  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  function yearLabel(value) {
    if (value === "all") return "ทุกชั้นปี";
    if (value === "other") return "ปีลึก";
    return "ปี " + value;
  }

  function checkedValues(checkboxes) {
    return Array.from(checkboxes)
      .filter((item) => item.checked)
      .map((item) => item.value);
  }

  // ไปหน้าห้อง พร้อมส่งข้อมูลห้องผ่าน query string
  function goToRoom(room) {
    const query = [
      ["name", room.name],
      ["members", room.members],
      ["max", room.max],
      ["owner", room.owner],
      ["ownerYear", room.ownerYear],
      ["ownerImage", room.ownerImage],
    ]
      .map(([key, value]) => key + "=" + encodeURIComponent(value))
      .join("&");

    window.location.href = "/room?" + query;
  }

  // Filter

  const searchInput = $("roomSearch");
  const yearFilter = $("yearFilter");
  const interestFilter = $("interestFilter");

  function splitData(value) {
    return (value || "").split(",").map((item) => item.trim());
  }

  function filterRooms() {
    const search = searchInput.value.toLowerCase().trim();
    const year = yearFilter.value;
    const interest = interestFilter.value;

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

  searchInput.addEventListener("input", filterRooms);
  yearFilter.addEventListener("change", filterRooms);
  interestFilter.addEventListener("change", filterRooms);

  // Create Room Popup

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
  const interestCheckboxes = roomInterest.querySelectorAll(
    'input[type="checkbox"]',
  );

  const roomYearSelect = $("roomYearSelect");
  const selectedYearText = $("selectedYearText");
  const yearCheckboxes = roomYearSelect.querySelectorAll(
    'input[name="roomYear"]',
  );

  // ปิด dropdown เมื่อคลิกข้างนอก
  document.addEventListener("click", function (event) {
    if (!roomInterest.contains(event.target)) {
      roomInterest.removeAttribute("open");
    }

    if (!roomYearSelect.contains(event.target)) {
      roomYearSelect.removeAttribute("open");
    }
  });

  createRoomButton.addEventListener("click", function () {
    createRoomModal.style.display = "flex";
  });

  cancelCreateRoom.addEventListener("click", function () {
    createRoomModal.style.display = "none";
  });

  //  dropdown ความสนใจ
  interestCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener("change", function () {
      const selected = checkedValues(interestCheckboxes);

      selectedInterestText.textContent =
        selected.length === 0 ? "เลือกความสนใจ" : selected.join(", ");
    });
  });

  // dropdown ชั้นปี
  function updateSelectedYearText() {
    const selected = Array.from(yearCheckboxes)
      .filter((item) => item.checked)
      .map((item) => item.parentElement.textContent.trim());

    selectedYearText.textContent =
      selected.length === 0 ? "เลือกชั้นปี" : selected.join(", ");
  }

  yearCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener("change", function () {
      // เลือก "ทุกชั้นปี" → ยกเลิกตัวเลือกอื่น
      // เลือกปีใดปีหนึ่ง → ยกเลิก "ทุกชั้นปี"
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

  // Create Room

  confirmCreateRoom.addEventListener("click", function () {
    const name = roomName.value.trim();
    const description = roomDescription.value.trim();
    const maxPeople = roomMax.value;
    const type = roomType.value;

    const selectedYears = checkedValues(yearCheckboxes);
    const selectedInterests = checkedValues(interestCheckboxes);

    // Validation
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

    // ข้อความ/แท็กที่ใช้แสดงผล (ต้องคำนวณก่อนใช้ใน dataset)
    const yearText = selectedYears.map(yearLabel).join(", ");

    const interestTags = selectedInterests
      .map((interest) => `<span>${escapeHtml(interest)}</span>`)
      .join("");

    const yearTags = selectedYears
      .map((value) => `<span>${yearLabel(value)}</span>`)
      .join("");

    // สร้างการ์ดห้อง
    const newRoom = document.createElement("article");

    newRoom.className = "room-card";

    // data สำหรับ Filter / เข้าห้อง
    newRoom.dataset.year = selectedYears.join(",");
    newRoom.dataset.interest = selectedInterests.join(",");
    newRoom.dataset.members = "1";
    newRoom.dataset.max = maxPeople;
    newRoom.dataset.owner = "Singha Waraha";
    newRoom.dataset.ownerYear = yearText;
    newRoom.dataset.ownerImage = "/images/man.jpg";
    newRoom.dataset.type = type;

    newRoom.innerHTML = `
      <h3>${escapeHtml(name)}</h3>

      <p class="room-description">
        1/${escapeHtml(maxPeople)} คนกำลังคุย
        &nbsp;&nbsp;
        ${escapeHtml(description) || "ยังไม่มีคำอธิบาย"}
      </p>

      <div class="room-tags">
        ${interestTags}
        ${yearTags}
      </div>

      <div class="room-owner">
        <img src="/images/man.jpg" alt="Owner" />

        <div>
          <strong>Singha Waraha</strong>
          <small>${yearText}</small>
        </div>
      </div>

      <div class="room-bottom">
        <span class="room-type">${escapeHtml(type)}</span>

        <button type="button" class="join-button">เข้าห้อง</button>
      </div>
    `;

    roomGrid.prepend(newRoom);

    // ปิด popup
    createRoomModal.style.display = "none";

    // Reset Form
    roomName.value = "";
    roomDescription.value = "";

    yearCheckboxes.forEach(function (checkbox) {
      checkbox.checked = checkbox.value === "1";
    });
    selectedYearText.textContent = "ปี 1";
    roomYearSelect.removeAttribute("open");

    roomMax.value = "10";
    roomType.value = "Public";

    interestCheckboxes.forEach(function (checkbox) {
      checkbox.checked = false;
    });
    selectedInterestText.textContent = "เลือกความสนใจ";
    roomInterest.removeAttribute("open");

    filterRooms();
  });

  // =========================
  // Private Room Popup
  // =========================

  const privateRoomModal = $("privateRoomModal");
  const privateRoomName = $("privateRoomName");
  const privateRoomCode = $("privateRoomCode");
  const privateRoomError = $("privateRoomError");
  const cancelPrivateRoom = $("cancelPrivateRoom");
  const confirmPrivateRoom = $("confirmPrivateRoom");

  console.log("Private Room:", privateRoomModal);
  console.log("Cancel:", cancelPrivateRoom);
  console.log("Confirm:", confirmPrivateRoom);

  // ห้อง Private ที่กำลังจะเข้า
  let pendingRoom = null;

  function openPrivateRoom(room) {
    pendingRoom = room;

    privateRoomName.textContent = "กรุณากรอกรหัสเพื่อเข้าห้อง " + room.name;
    privateRoomCode.value = "";
    privateRoomError.textContent = "";

    privateRoomModal.style.display = "flex";
    privateRoomCode.focus();
  }

  function closePrivateRoom() {
    pendingRoom = null;

    privateRoomModal.style.display = "none";
    privateRoomCode.value = "";
    privateRoomError.textContent = "";
  }

  function showPrivateError(message) {
    privateRoomError.textContent = message;
    privateRoomError.style.color = "#d93025";
  }

  function submitPrivateRoom() {
    if (!pendingRoom) {
      return;
    }

    const code = privateRoomCode.value.trim();

    if (code.length !== 6) {
      showPrivateError("กรุณากรอกรหัส 6 หลัก");
      return;
    }

    // ถ้าการ์ดห้องมี data-code ให้เทียบรหัสตรงนี้
    // (ระบบจริงควรตรวจรหัสที่ฝั่ง server)
    if (pendingRoom.code && code !== pendingRoom.code) {
      showPrivateError("รหัสไม่ถูกต้อง");
      return;
    }

    goToRoom(pendingRoom);
  }

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

  // คลิกพื้นที่มืดด้านนอกเพื่อปิด
  privateRoomModal.addEventListener("click", function (event) {
    if (event.target === privateRoomModal) {
      closePrivateRoom();
    }
  });

  createRoomModal.addEventListener("click", function (event) {
    if (event.target === createRoomModal) {
      createRoomModal.style.display = "none";
    }
  });

  // ESC ปิดทั้งสอง popup
  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      closePrivateRoom();
      createRoomModal.style.display = "none";
    }
  });

  // =========================
  // Join Room
  // =========================

  roomGrid.addEventListener("click", function (event) {
    const button = event.target.closest(".join-button");

    if (!button) return;

    const roomCard = button.closest(".room-card");

    if (!roomCard) return;

    const titleElement = roomCard.querySelector("h3");

    if (!titleElement) return;

    const room = {
      name: titleElement.textContent.trim(),
      members: roomCard.dataset.members || "0",
      max: roomCard.dataset.max || "10",
      owner: roomCard.dataset.owner || "",
      ownerYear: roomCard.dataset.ownerYear || "",
      ownerImage: roomCard.dataset.ownerImage || "",
      code: roomCard.dataset.code || "",
    };

    const type = roomCard.querySelector(".room-type").textContent.trim();

    if (type === "Private") {
      openPrivateRoom(room);
      return;
    }

    goToRoom(room);
  });
});
