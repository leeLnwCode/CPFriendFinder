document.addEventListener("DOMContentLoaded", function () {

  // =========================================================
  // Random People
  // =========================================================

  const people = [
    {
      name: "Prashant Kumar Singh",
      year: "ปี 3 CS-VIP",
      image: "/images/man2.jpg",
      interests: ["Gaming", "ROV", "Programming"],
      bio: "ชอบเล่นเกมโดยเฉพาะ ROV ชวนเล่นได้ทุกๆคน อยากหาเพื่อนคุยและทำโปรเจคด้วยกันครับ",
      messages: [
        "ไหนลองมาคุยกันเรื่อง ROV ด้วยกันมั้ยครับ",
        "ว่างช่วงเย็นวันนี้มั้ยครับ",
        "ชื่อ อนันต์โอ้ะครับ PKS_inwza"
      ]
    },

    {
      name: "Napha",
      year: "ปี 3 CS-VIP",
      image: "/images/Napha.jpg",
      interests: ["Music", "Gaming", "Movies"],
      bio: "ชอบฟังเพลง เล่นเกม และหาเพื่อนใหม่ๆ มาคุยกันได้นะ",
      messages: [
        "สวัสดีครับ",
        "ชอบเล่นเกมอะไรบ้างครับ",
        "ช่วงนี้กำลังหาเพื่อนเล่นเกมอยู่เลย"
      ]
    },

    {
      name: "Worawut",
      year: "ปี 1 CY-VIP",
      image: "/images/Worawut.jpg",
      interests: ["Programming", "ROV", "Project"],
      bio: "สนใจด้าน Programming และชอบทำ Project อยากหาเพื่อนช่วยกันทำงานครับ",
      messages: [
        "สวัสดีครับ ยินดีที่ได้รู้จัก",
        "เรียนอยู่ปี 1 เหมือนกันไหมครับ",
        "กำลังหาเพื่อนทำ Project อยู่ครับ"
      ]
    }
  ];


  // =========================================================
  // Elements
  // =========================================================

  const profileImage =
    document.getElementById("randomProfileImage");

  const profileName =
    document.getElementById("randomProfileName");

  const profileYear =
    document.getElementById("randomProfileYear");

  const profileInterests =
    document.getElementById("randomProfileInterests");

  const profileBio =
    document.getElementById("randomProfileBio");

  const chatProfileImage =
    document.getElementById("chatProfileImage");

  const chatProfileName =
    document.getElementById("chatProfileName");

  const chatProfileYear =
    document.getElementById("chatProfileYear");

  const chatMessages =
    document.getElementById("randomChatMessages");

  const nextPersonButton =
    document.getElementById("nextPersonButton");

  const addFriendButton =
    document.getElementById("addFriendButton");

  const chatInput =
    document.getElementById("randomChatInput");

  const sendButton =
    document.getElementById("randomSendButton");

  const imageButton =
    document.getElementById("randomImageButton");

  const imageInput =
    document.getElementById("randomImageInput");


  // =========================================================
  // Current Person
  // =========================================================

  let currentIndex = 0;

  let currentPerson = people[currentIndex];


  // =========================================================
  // Render Profile
  // =========================================================

  function renderPerson(person) {

    profileImage.src = person.image;
    profileImage.alt = person.name;

    profileName.textContent = person.name;

    profileYear.textContent = person.year;


    // Interests

    profileInterests.innerHTML = "";

    person.interests.forEach(function (interest) {

      const tag = document.createElement("span");

      tag.className = "interest-tag";

      tag.textContent = interest;

      profileInterests.appendChild(tag);

    });


    // Bio

    profileBio.textContent = person.bio;


    // Chat Header

    chatProfileImage.src = person.image;
    chatProfileImage.alt = person.name;

    chatProfileName.textContent = person.name;

    chatProfileYear.textContent = person.year;


    // Chat

    renderMessages(person.messages);


    // Reset friend button

    addFriendButton.innerHTML = `
      <span>👤+</span>
      <span>เพิ่มเพื่อน</span>
    `;

    addFriendButton.disabled = false;

  }


  // =========================================================
  // Render Messages
  // =========================================================

  function renderMessages(messages) {

    chatMessages.innerHTML = "";

    messages.forEach(function (message) {

      const messageElement =
        document.createElement("div");

      messageElement.className =
        "chat-message received";

      messageElement.innerHTML = `
        <img
          src="${currentPerson.image}"
          alt="${currentPerson.name}"
        />

        <div class="message-bubble">
          ${message}
        </div>
      `;

      chatMessages.appendChild(messageElement);

    });

    chatMessages.scrollTop =
      chatMessages.scrollHeight;

  }


  // =========================================================
  // Next Person
  // =========================================================

  nextPersonButton.addEventListener(
    "click",
    function () {

      currentIndex++;

      if (currentIndex >= people.length) {
        currentIndex = 0;
      }

      currentPerson = people[currentIndex];

      renderPerson(currentPerson);

    }
  );


  // =========================================================
  // Add Friend
  // =========================================================

  addFriendButton.addEventListener(
    "click",
    function () {

      addFriendButton.innerHTML = `
        <span>✓</span>
        <span>ส่งคำขอแล้ว</span>
      `;

      addFriendButton.disabled = true;

    }
  );


  // =========================================================
  // Send Message
  // =========================================================

  function sendMessage() {

    const message =
      chatInput.value.trim();

    if (message === "") {
      return;
    }


    const messageElement =
      document.createElement("div");

    messageElement.className =
      "chat-message sent";

    messageElement.innerHTML = `
      <div class="message-bubble">
        ${message}
      </div>
    `;

    chatMessages.appendChild(messageElement);

    chatInput.value = "";

    chatMessages.scrollTop =
      chatMessages.scrollHeight;

  }


  sendButton.addEventListener(
    "click",
    sendMessage
  );


  chatInput.addEventListener(
    "keydown",
    function (event) {

      if (event.key === "Enter") {

        event.preventDefault();

        sendMessage();

      }

    }
  );


  // =========================================================
  // Send Image
  // =========================================================

  imageButton.addEventListener(
    "click",
    function () {

      imageInput.click();

    }
  );


  imageInput.addEventListener(
    "change",
    function () {

      const file = this.files[0];

      if (!file) {
        return;
      }


      const imageUrl =
        URL.createObjectURL(file);


      const messageElement =
        document.createElement("div");

      messageElement.className =
        "chat-message sent";

      messageElement.innerHTML = `
        <img
          src="${imageUrl}"
          alt="รูปภาพ"
          style="
            width: 180px;
            height: auto;
            border-radius: 12px;
          "
        />
      `;

      chatMessages.appendChild(messageElement);

      chatMessages.scrollTop =
        chatMessages.scrollHeight;

      imageInput.value = "";

    }
  );


  // =========================================================
  // Initial Render
  // =========================================================

  renderPerson(currentPerson);

});