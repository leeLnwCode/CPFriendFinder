document.addEventListener("DOMContentLoaded", function () {
  // =========================
  // Room Data
  // =========================

  const params = new URLSearchParams(window.location.search);

  const roomName = params.get("name") || "ห้องพูดคุย";
  const members = params.get("members") || "1";
  const max = params.get("max") || "10";

  const roomCode = params.get("code") || "";
  const isOwner = params.get("isOwner") === "true";

  // =========================
  // Owner Data
  // =========================

  const owner = params.get("owner") || "เจ้าของห้อง";

  const ownerYear = params.get("ownerYear") || "-";

  const ownerImage = params.get("ownerImage") || "/images/man2.jpg";

  // =========================
  // Current User Data
  // =========================

  const currentUser = params.get("currentUser") || "";

  const currentUserYear = params.get("currentUserYear") || "";

  const currentUserImage = params.get("currentUserImage") || "/images/man.jpg";

  // =========================
  // Elements
  // =========================

  const roomTitle = document.getElementById("roomTitle");

  const roomMembers = document.getElementById("roomMembers");

  const chatMembers = document.getElementById("chatMembers");

  const membersGrid = document.getElementById("membersGrid");

  const roomCodeElement = document.getElementById("roomCode");

  const ownerRoomCode = document.getElementById("ownerRoomCode");

  // =========================
  // Room Header
  // =========================

  roomTitle.textContent = roomName;

  roomMembers.textContent = members + "/" + max + " คนกำลังคุย";

  chatMembers.textContent = members + " คน";

  // =========================
  // Room Code
  // เจ้าของเท่านั้นที่เห็น
  // =========================

  if (isOwner && roomCode) {
    roomCodeElement.textContent = roomCode;

    ownerRoomCode.style.display = "block";
  } else {
    ownerRoomCode.style.display = "none";
  }

  // =========================
  // Members
  // =========================

  membersGrid.innerHTML = "";

  // =========================
  // Owner Card
  // =========================

  const ownerCard = document.createElement("article");

  ownerCard.className = "member-card";

  ownerCard.innerHTML = `
    <img
      src="${ownerImage}"
      alt="${owner}"
    />

    <h3>${owner}</h3>

    <p>${ownerYear}</p>

    <small>(เจ้าของห้อง)</small>
  `;

  membersGrid.appendChild(ownerCard);

  // =========================
  // Current User Card
  // =========================

  if (!isOwner && currentUser) {
    const currentUserCard = document.createElement("article");

    currentUserCard.className = "member-card";

    currentUserCard.innerHTML = `
      <img
        src="${currentUserImage}"
        alt="${currentUser}"
      />

      <h3>${currentUser}</h3>

      <p>${currentUserYear}</p>

      <small>(คุณ)</small>
    `;

    membersGrid.appendChild(currentUserCard);
  }

  // =========================
  // Chat
  // =========================

  const chatInput = document.getElementById("chatInput");

  const sendMessageButton = document.getElementById("sendMessageButton");

  const chatMessages = document.getElementById("chatMessages");

  function sendMessage() {
    const message = chatInput.value.trim();

    if (message === "") {
      return;
    }

    const messageElement = document.createElement("div");

    messageElement.className = "message";

    messageElement.innerHTML = `
      <strong>Singha</strong>
      <p>${message}</p>
    `;

    chatMessages.appendChild(messageElement);

    chatInput.value = "";

    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  sendMessageButton.addEventListener("click", sendMessage);

  chatInput.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
      sendMessage();
    }
  });

  // =========================
  // Leave Room
  // =========================

  const leaveRoomButton = document.getElementById("leaveRoomButton");

  leaveRoomButton.addEventListener("click", function () {
    window.location.href = "/home";
  });

  // =========================
  // Image
  // =========================

  const imageButton = document.getElementById("imageButton");
  const imageInput = document.getElementById("imageInput");

  imageButton.addEventListener("click", function () {
    imageInput.click();
  });

  imageInput.addEventListener("change", function () {
    const file = this.files[0];

    if (!file) {
      return;
    }

    const imageUrl = URL.createObjectURL(file);

    const messageElement = document.createElement("div");

    messageElement.className = "message";

    messageElement.innerHTML = `
    <strong>Singha</strong>

    <img
      src="${imageUrl}"
      alt="รูปภาพ"
      style="
        max-width: 200px;
        border-radius: 10px;
        margin-top: 5px;
      "
    >
  `;

    chatMessages.appendChild(messageElement);

    chatMessages.scrollTop = chatMessages.scrollHeight;

    imageInput.value = "";
  });
});
