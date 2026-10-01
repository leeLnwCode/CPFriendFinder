document.addEventListener("DOMContentLoaded", function () {
  // =========================
  // Room Name
  // =========================

  const params = new URLSearchParams(window.location.search);

  const roomName = params.get("name");

  const members = params.get("members") || "0";

  const max = params.get("max") || "10";

  const roomTitle = document.getElementById("roomTitle");

  const ownerName = document.getElementById("ownerName");

  const ownerYearElement = document.getElementById("ownerYear");

  const ownerImageElement = document.getElementById("ownerImage");

  const roomMembers = document.getElementById("roomMembers");

  const chatMembers = document.getElementById("chatMembers");

  const owner = params.get("owner") || "เจ้าของห้อง";

  const ownerYear = params.get("ownerYear") || "-";

  const ownerImage = params.get("ownerImage") || "/images/man2.jpg";


console.log("OWNER =", owner);
console.log("OWNER YEAR =", ownerYear);
console.log("OWNER IMAGE =", ownerImage);

  if (roomName) {
    roomTitle.textContent = roomName;
  }

  ownerName.textContent = owner;
  ownerYearElement.textContent = ownerYear;
  ownerImageElement.src = ownerImage;

  roomMembers.textContent = members + "/" + max + " คนกำลังคุย";

  chatMembers.textContent = members + " คน";

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
});
