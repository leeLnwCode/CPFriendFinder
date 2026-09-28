document.addEventListener("DOMContentLoaded", function () {
  // =========================
  // Filter
  // =========================

  const searchInput = document.getElementById("roomSearch");
  const yearFilter = document.getElementById("yearFilter");
  const interestFilter = document.getElementById("interestFilter");

  function filterRooms() {
    const search = searchInput.value.toLowerCase().trim();
    const year = yearFilter.value;
    const interest = interestFilter.value;

    const rooms = document.querySelectorAll(".room-card");

    rooms.forEach(function (room) {
      const text = room.innerText.toLowerCase();

      const roomYears = (room.dataset.year || "")
        .split(",")
        .map(function (item) {
          return item.trim();
        });

      const roomInterests = (room.dataset.interest || "")
        .split(",")
        .map(function (item) {
          return item.trim();
        });

      const matchSearch = text.includes(search);

      const matchYear =
        year === "" ||
        roomYears.includes(year) ||
        roomYears.includes("all");

      const matchInterest =
        interest === "" || roomInterests.includes(interest);

      if (matchSearch && matchYear && matchInterest) {
        room.style.display = "flex";
      } else {
        room.style.display = "none";
      }
    });
  }

  searchInput.addEventListener("input", filterRooms);

  yearFilter.addEventListener("change", filterRooms);

  interestFilter.addEventListener("change", filterRooms);


  // =========================
  // Create Room Popup
  // =========================

  const createRoomButton =
    document.getElementById("createRoomButton");

  const createRoomModal =
    document.getElementById("createRoomModal");

  const cancelCreateRoom =
    document.getElementById("cancelCreateRoom");

  const confirmCreateRoom =
    document.getElementById("confirmCreateRoom");

  const roomGrid =
    document.getElementById("roomGrid");


  // =========================
  // Form
  // =========================

  const roomName =
    document.getElementById("roomName");

  const roomDescription =
    document.getElementById("roomDescription");


  // =========================
  // Interest Dropdown
  // =========================

  const roomInterest =
    document.getElementById("roomInterest");

  const selectedInterestText =
    document.getElementById("selectedInterestText");

  const interestCheckboxes =
    roomInterest.querySelectorAll(
      'input[type="checkbox"]'
    );


  // =========================
  // Year Dropdown
  // =========================

  const roomYearSelect =
    document.getElementById("roomYearSelect");

  const selectedYearText =
    document.getElementById("selectedYearText");

  const yearCheckboxes =
    roomYearSelect.querySelectorAll(
      'input[name="roomYear"]'
    );


  // =========================
  // Other Form
  // =========================

  const roomMax =
    document.getElementById("roomMax");

  const roomType =
    document.getElementById("roomType");


  // =========================
  // Close Dropdown
  // =========================

  document.addEventListener("click", function (event) {

    // ปิดความสนใจ
    if (!roomInterest.contains(event.target)) {
      roomInterest.removeAttribute("open");
    }

    // ปิดชั้นปี
    if (!roomYearSelect.contains(event.target)) {
      roomYearSelect.removeAttribute("open");
    }

  });


  // =========================
  // Open Popup
  // =========================

  createRoomButton.addEventListener("click", function () {

    createRoomModal.style.display = "flex";

  });


  // =========================
  // Close Popup
  // =========================

  cancelCreateRoom.addEventListener("click", function () {

    createRoomModal.style.display = "none";

  });


  // =========================
  // Interest Checkbox
  // =========================

  interestCheckboxes.forEach(function (checkbox) {

    checkbox.addEventListener("change", function () {

      const selected =
        Array.from(interestCheckboxes)
          .filter(function (item) {
            return item.checked;
          })
          .map(function (item) {
            return item.value;
          });


      if (selected.length === 0) {

        selectedInterestText.textContent =
          "เลือกความสนใจ";

      } else {

        selectedInterestText.textContent =
          selected.join(", ");

      }

    });

  });


  // =========================
  // Year Checkbox (Multi-Select)
  // =========================

  function updateSelectedYearText() {

    const selected =
      Array.from(yearCheckboxes)
        .filter(function (item) {
          return item.checked;
        })
        .map(function (item) {
          return item.parentElement.textContent.trim();
        });

    if (selected.length === 0) {

      selectedYearText.textContent =
        "เลือกชั้นปี";

    } else {

      selectedYearText.textContent =
        selected.join(", ");

    }

  }

  yearCheckboxes.forEach(function (checkbox) {

    checkbox.addEventListener("change", function () {

      // ถ้าเลือก "ทุกชั้นปี" ให้ยกเลิกตัวเลือกอื่นทั้งหมด
      if (this.value === "all" && this.checked) {

        yearCheckboxes.forEach(function (item) {

          if (item.value !== "all") {
            item.checked = false;
          }

        });

      }

      // ถ้าเลือกปีใดปีหนึ่ง ให้ยกเลิก "ทุกชั้นปี"
      if (this.value !== "all" && this.checked) {

        yearCheckboxes.forEach(function (item) {

          if (item.value === "all") {
            item.checked = false;
          }

        });

      }

      updateSelectedYearText();

    });

  });


  // =========================
  // Create Room
  // =========================

  confirmCreateRoom.addEventListener(
    "click",
    function () {

      // -------------------------
      // Get Form Data
      // -------------------------

      const name =
        roomName.value.trim();

      const description =
        roomDescription.value.trim();


      // ชั้นปีที่เลือก
      const selectedYears =
        Array.from(yearCheckboxes)
          .filter(function (checkbox) {
            return checkbox.checked;
          })
          .map(function (checkbox) {
            return checkbox.value;
          });


      const maxPeople =
        roomMax.value;

      const type =
        roomType.value;


      // -------------------------
      // Selected Interests
      // -------------------------

      const selectedInterests =
        Array.from(interestCheckboxes)
          .filter(function (checkbox) {
            return checkbox.checked;
          })
          .map(function (checkbox) {
            return checkbox.value;
          });


      // -------------------------
      // Validation
      // -------------------------

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


      // =========================
      // Create Room Card
      // =========================

      const newRoom =
        document.createElement("article");

      newRoom.className =
        "room-card";


      // -------------------------
      // Data สำหรับ Filter
      // -------------------------

      newRoom.dataset.year =
        selectedYears.join(",");

      newRoom.dataset.interest =
        selectedInterests.join(",");


      // -------------------------
      // Interest Tags
      // -------------------------

      const interestTags =
        selectedInterests
          .map(function (interest) {

            return `
              <span>${interest}</span>
            `;

          })
          .join("");


      // -------------------------
      // Year Display Text
      // -------------------------

      function yearLabel(value) {
        if (value === "all") return "ทุกชั้นปี";
        if (value === "other") return "ปีลึก";
        return "ปี " + value;
      }

      const yearTags =
        selectedYears
          .map(function (value) {

            return `
              <span>${yearLabel(value)}</span>
            `;

          })
          .join("");

      const yearText =
        selectedYears
          .map(yearLabel)
          .join(", ");

      // =========================
      // Room HTML
      // =========================

      newRoom.innerHTML = `

        <h3>
          ${name}
        </h3>

        <p class="room-description">

          1/${maxPeople} คนกำลังคุย

          &nbsp;&nbsp;

          ${description || "ยังไม่มีคำอธิบาย"}

        </p>


        <div class="room-tags">

          ${interestTags}

          ${yearTags}

        </div>


        <div class="room-owner">

          <img
            src="/images/man.jpg"
            alt="Owner"
          >

          <div>

            <strong>
              Singha Waraha
            </strong>

            <small>
              ${yearText}
            </small>

          </div>

        </div>


        <div class="room-bottom">

          <span class="room-type">
            ${type}
          </span>

          <button
            type="button"
            class="join-button"
          >
            เข้าห้อง
          </button>

        </div>

      `;


      // =========================
      // Add Room
      // =========================

      roomGrid.prepend(newRoom);


      // =========================
      // Close Popup
      // =========================

      createRoomModal.style.display =
        "none";


      // =========================
      // Reset Form
      // =========================

      roomName.value = "";

      roomDescription.value = "";


      // Reset Year
      yearCheckboxes.forEach(function (checkbox) {

        checkbox.checked =
          checkbox.value === "1";

      });

      selectedYearText.textContent =
        "ปี 1";

      roomYearSelect.removeAttribute("open");


      // Reset Max People
      roomMax.value =
        "10";


      // Reset Room Type
      roomType.value =
        "Public";


      // Reset Interest
      interestCheckboxes.forEach(
        function (checkbox) {

          checkbox.checked =
            false;

        }
      );


      selectedInterestText.textContent =
        "เลือกความสนใจ";


      roomInterest.removeAttribute(
        "open"
      );


      // =========================
      // Update Filter
      // =========================

      filterRooms();

    }
  );

});