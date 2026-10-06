document.addEventListener("DOMContentLoaded", () => {
  const friendSearch = document.getElementById("friendSearch");

  const friendList = document.getElementById("friendList");

  const chatUserImage = document.getElementById("chatUserImage");

  const chatUserName = document.getElementById("chatUserName");

  const chatUserYear = document.getElementById("chatUserYear");

  const chatUserProfile = document.getElementById("chatUserProfile");

  const chatMessages = document.getElementById("chatMessages");

  const messageForm = document.getElementById("messageForm");

  const messageInput = document.getElementById("messageInput");

  const voiceCallButton = document.getElementById("voiceCallButton");

  const videoCallButton = document.getElementById("videoCallButton");

  const chatMoreButton = document.getElementById("chatMoreButton");

  const chatMoreMenu = document.getElementById("chatMoreMenu");

  const unfriendButton = document.getElementById("unfriendButton");

  const imageButton = document.getElementById("imageButton");

  const imageInput = document.getElementById("imageInput");

  /* =====================================================
         CHAT DATA
      ====================================================== */

  const conversations = {
    1: [
      {
        type: "received",
        text: "ใช่น้องที่เล่น ROV ตานั้นมั้ยครับ",
        image: "/images/man2.jpg",
      },
      {
        type: "sent",
        text: "ใช่ครับ",
      },
    ],

    2: [
      {
        type: "received",
        text: "สวัสดีครับ เห็นว่าเล่นเกมเหมือนกัน",
        image: "/images/Worawut.jpg",
      },
      {
        type: "sent",
        text: "ใช่ครับ เล่นหลายเกมเลยครับ",
      },
    ],

    3: [
      {
        type: "received",
        text: "สวัสดีครับ",
        image: "/images/Napha.jpg",
      },
      {
        type: "sent",
        text: "สวัสดีครับ",
      },
    ],
  };

  /* =====================================================
         LOAD CHAT
      ====================================================== */

  function loadConversation(friend) {
    const id = friend.dataset.id;

    const name = friend.dataset.name;

    const year = friend.dataset.year;

    const image = friend.dataset.image;

    /* Active friend */

    document.querySelectorAll(".friend-list-item").forEach((item) => {
      item.classList.remove("active");
    });

    friend.classList.add("active");

    /* Header */

    chatUserName.textContent = name;

    chatUserYear.textContent = year;

    chatUserImage.src = image;

    /* Profile Popup Data */

    chatUserProfile.dataset.name = name;

    chatUserProfile.dataset.year = year;

    chatUserProfile.dataset.image = image;

    chatUserProfile.dataset.id = id;

    chatUserProfile.dataset.interests = friend.dataset.interests || "";

    chatUserProfile.dataset.bio = friend.dataset.bio || "";

    chatUserProfile.dataset.status = "friend";

    /* Messages */

    renderMessages(id, name, image);

    /* Close more menu */

    chatMoreMenu.classList.remove("show");
  }

  /* =====================================================
         RENDER MESSAGES
      ====================================================== */

  function renderMessages(friendId, friendName, friendImage) {
    chatMessages.innerHTML = "";

    const messages = conversations[friendId] || [];

    messages.forEach((message) => {
      const row = document.createElement("div");

      row.className = `message-row ${message.type}`;

      if (message.type === "received") {
        const avatar = document.createElement("img");

        avatar.src = message.image || friendImage;

        avatar.alt = friendName;

        avatar.className = "message-avatar";

        row.appendChild(avatar);
      }

      const bubble = document.createElement("div");

      bubble.className = "message-bubble";

      if (message.imageUrl) {
        const image = document.createElement("img");

        image.src = message.imageUrl;

        image.alt = "รูปภาพ";

        image.className = "sent-chat-image";

        bubble.appendChild(image);
      } else {
        bubble.textContent = message.text;
      }

      row.appendChild(bubble);

      chatMessages.appendChild(row);
    });

    scrollToBottom();
  }

  /* =====================================================
         FRIEND CLICK
      ====================================================== */

  document.querySelectorAll(".friend-list-item").forEach((friend) => {
    friend.addEventListener("click", () => {
      loadConversation(friend);
    });
  });

  /* =====================================================
         SEARCH
      ====================================================== */

  friendSearch.addEventListener("input", () => {
    const keyword = friendSearch.value.trim().toLowerCase();

    const friends = document.querySelectorAll(".friend-list-item");

    let found = 0;

    friends.forEach((friend) => {
      const name = friend.dataset.name.toLowerCase();

      const year = friend.dataset.year.toLowerCase();

      const matched = name.includes(keyword) || year.includes(keyword);

      friend.style.display = matched ? "flex" : "none";

      if (matched) {
        found++;
      }
    });

    let emptyMessage = document.getElementById("friendEmpty");

    if (found === 0) {
      if (!emptyMessage) {
        emptyMessage = document.createElement("div");

        emptyMessage.id = "friendEmpty";

        emptyMessage.className = "friend-empty";

        emptyMessage.textContent = "ไม่พบเพื่อนที่ค้นหา";

        friendList.appendChild(emptyMessage);
      }
    } else {
      if (emptyMessage) {
        emptyMessage.remove();
      }
    }
  });

  /* =====================================================
         SEND TEXT
      ====================================================== */

  messageForm.addEventListener("submit", (event) => {
    event.preventDefault();

    const text = messageInput.value.trim();

    if (text === "") {
      return;
    }

    const activeFriend = document.querySelector(".friend-list-item.active");

    if (!activeFriend) {
      return;
    }

    const friendId = activeFriend.dataset.id;

    /* Save message */

    if (!conversations[friendId]) {
      conversations[friendId] = [];
    }

    conversations[friendId].push({
      type: "sent",
      text: text,
    });

    /* Re-render */

    renderMessages(
      friendId,
      activeFriend.dataset.name,
      activeFriend.dataset.image,
    );

    messageInput.value = "";

    messageInput.focus();
  });

  /* =====================================================
         ENTER TO SEND
      ====================================================== */

  messageInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();

      messageForm.requestSubmit();
    }
  });

  /* =====================================================
         VOICE CALL
      ====================================================== */

  voiceCallButton.addEventListener("click", () => {
    const name = chatUserName.textContent;

    alert(`กำลังโทรเสียงหา ${name}`);
  });

  /* =====================================================
         VIDEO CALL
      ====================================================== */

  videoCallButton.addEventListener("click", () => {
    const name = chatUserName.textContent;

    alert(`กำลังวิดีโอคอลกับ ${name}`);
  });

  /* =====================================================
         MORE MENU
      ====================================================== */

  chatMoreButton.addEventListener("click", (event) => {
    event.stopPropagation();

    chatMoreMenu.classList.toggle("show");
  });

  /* ปิดเมนูเมื่อคลิกข้างนอก */

  document.addEventListener("click", (event) => {
    if (!event.target.closest(".chat-more")) {
      chatMoreMenu.classList.remove("show");
    }
  });

  /* =====================================================
         UNFRIEND
      ====================================================== */

  unfriendButton.addEventListener("click", () => {
    const activeFriend = document.querySelector(".friend-list-item.active");

    if (!activeFriend) {
      return;
    }

    const name = activeFriend.dataset.name;

    const confirmUnfriend = confirm(
      `ต้องการเลิกเป็นเพื่อนกับ ${name} หรือไม่?`,
    );

    if (!confirmUnfriend) {
      return;
    }

    activeFriend.remove();

    chatUserName.textContent = "เลือกเพื่อน";

    chatUserYear.textContent = "";

    chatUserImage.src = "/images/man.jpg";

    chatUserProfile.removeAttribute("data-profile");

    chatMessages.innerHTML = `
                <div class="friend-empty">
                    คุณเลิกเป็นเพื่อนกับ ${name} แล้ว
                </div>
            `;

    chatMoreMenu.classList.remove("show");
  });

  /* =====================================================
         IMAGE BUTTON
      ====================================================== */

  imageButton.addEventListener("click", () => {
    imageInput.click();
  });

  /* =====================================================
         IMAGE SELECT
      ====================================================== */

  imageInput.addEventListener("change", (event) => {
    const file = event.target.files[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith("image/")) {
      alert("กรุณาเลือกไฟล์รูปภาพ");

      imageInput.value = "";

      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      const activeFriend = document.querySelector(".friend-list-item.active");

      if (!activeFriend) {
        return;
      }

      const friendId = activeFriend.dataset.id;

      if (!conversations[friendId]) {
        conversations[friendId] = [];
      }

      conversations[friendId].push({
        type: "sent",
        imageUrl: reader.result,
      });

      renderMessages(
        friendId,
        activeFriend.dataset.name,
        activeFriend.dataset.image,
      );

      imageInput.value = "";
    };

    reader.readAsDataURL(file);
  });

  /* =====================================================
         SCROLL
      ====================================================== */

  function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  /* =====================================================
         INITIAL CHAT
      ====================================================== */

  const firstFriend = document.querySelector(".friend-list-item.active");

  if (firstFriend) {
    loadConversation(firstFriend);
  }
});
