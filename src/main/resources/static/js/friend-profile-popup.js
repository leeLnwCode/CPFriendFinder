document.addEventListener("DOMContentLoaded", () => {
  const overlay = document.getElementById("friendProfileOverlay");

  if (!overlay) {
    console.warn("ไม่พบ #friendProfileOverlay");
    return;
  }

  const closeButton = document.getElementById("friendProfileClose");
  const profileImage = document.getElementById("friendProfileImage");
  const profileName = document.getElementById("friendProfileName");
  const profileYear = document.getElementById("friendProfileYear");
  const profileInterests = document.getElementById("friendProfileInterests");
  const profileBio = document.getElementById("friendProfileBio");

  const actionButton = document.getElementById("friendProfileAction");
  const actionIcon = document.getElementById("friendProfileActionIcon");
  const actionText = document.getElementById("friendProfileActionText");

  if (
    !closeButton ||
    !profileImage ||
    !profileName ||
    !profileYear ||
    !profileInterests ||
    !profileBio ||
    !actionButton ||
    !actionIcon ||
    !actionText
  ) {
    console.error("Friend Profile Popup: element ไม่ครบ");
    return;
  }

  let currentProfileId = null;
  let currentStatus = "none";

  // --------------------------------------------------
  // Cache สำหรับข้อมูลความสัมพันธ์
  // --------------------------------------------------

  let friendsCache = null;
  let outgoingRequestsCache = null;

  let relationshipLoading = false;

  // ใช้ป้องกัน request เก่ากลับมาเปลี่ยนข้อมูลของ profile ใหม่
  let profileRequestToken = 0;

  // --------------------------------------------------
  // Utility
  // --------------------------------------------------

  function normalizeId(value) {
    if (value === null || value === undefined) {
      return "";
    }

    return String(value).trim().toLowerCase();
  }

  function getDatasetValue(element, key, fallback = "") {
    if (!element || !element.dataset) {
      return fallback;
    }

    const value = element.dataset[key];

    if (value === undefined || value === null) {
      return fallback;
    }

    return value;
  }

  // --------------------------------------------------
  // Interests
  // รองรับทั้ง:
  // "Game, Music"
  // ["Game", "Music"]
  // [{ name: "Game" }, { name: "Music" }]
  // --------------------------------------------------

  function parseInterests(value) {
    if (!value) {
      return [];
    }

    if (Array.isArray(value)) {
      return value
        .map((item) => {
          if (typeof item === "string") {
            return item.trim();
          }

          if (item && typeof item === "object") {
            return String(item.name || "").trim();
          }

          return "";
        })
        .filter(Boolean);
    }

    if (typeof value === "object") {
      if (Array.isArray(value.interests)) {
        return parseInterests(value.interests);
      }

      if (value.name) {
        return [String(value.name).trim()];
      }

      return [];
    }

    return String(value)
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
  }

  // --------------------------------------------------
  // Render interests
  // --------------------------------------------------

  function renderInterests(value) {
    profileInterests.innerHTML = "";

    const interestList = parseInterests(value);

    if (interestList.length === 0) {
      const empty = document.createElement("span");

      empty.className = "friend-profile-interest";
      empty.textContent = "ยังไม่มีข้อมูล";

      profileInterests.appendChild(empty);
      return;
    }

    interestList.forEach((interest) => {
      const tag = document.createElement("span");

      tag.className = "friend-profile-interest";
      tag.textContent = interest;

      profileInterests.appendChild(tag);
    });
  }

  // --------------------------------------------------
  // โหลดรายชื่อเพื่อน
  //
  // GET /api/friends
  //
  // Contract:
  // friendId = ID ของเพื่อน
  // --------------------------------------------------

  async function loadFriends() {
    if (friendsCache !== null) {
      return friendsCache;
    }

    const response = await fetch("/api/friends", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(`โหลดรายชื่อเพื่อนไม่สำเร็จ (${response.status})`);
    }

    const data = await response.json();

    friendsCache = Array.isArray(data) ? data : [];

    return friendsCache;
  }

  // --------------------------------------------------
  // โหลดคำขอที่เราส่งออกไป
  //
  // GET /api/friend-requests/outgoing
  //
  // Contract:
  // userId = ID ของ "คนที่เราเคยส่งคำขอไป"
  // status = PENDING
  // --------------------------------------------------

  async function loadOutgoingRequests() {
    if (outgoingRequestsCache !== null) {
      return outgoingRequestsCache;
    }

    const response = await fetch("/api/friend-requests/outgoing", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(
        `โหลดคำขอเป็นเพื่อนที่ส่งออกไปไม่สำเร็จ (${response.status})`,
      );
    }

    const data = await response.json();

    outgoingRequestsCache = Array.isArray(data) ? data : [];

    return outgoingRequestsCache;
  }

  // --------------------------------------------------
  // ตรวจสอบสถานะความสัมพันธ์
  //
  // 1. self
  // 2. friend
  // 3. pending
  // 4. none
  // --------------------------------------------------

  async function resolveRelationshipStatus(
    profileId,
    fallbackStatus = "none",
    isSelf = false,
  ) {
    const normalizedProfileId = normalizeId(profileId);

    if (!normalizedProfileId) {
      return "none";
    }

    // ------------------------------------------------
    // สำคัญ:
    // ถ้าเป็นตัวเราเอง ต้องตรวจตรงนี้ก่อน
    // ห้ามไปตรวจ friend / pending
    // ------------------------------------------------

    if (isSelf) {
      return "self";
    }

    try {
      const [friends, outgoingRequests] = await Promise.all([
        loadFriends(),
        loadOutgoingRequests(),
      ]);

      // ------------------------------
      // ตรวจว่าเป็นเพื่อนแล้วหรือยัง
      // ------------------------------

      const isFriend = friends.some((friend) => {
        return normalizeId(friend?.friendId) === normalizedProfileId;
      });

      if (isFriend) {
        return "friend";
      }

      // ------------------------------
      // ตรวจว่าเราส่งคำขอไปแล้วหรือยัง
      // ------------------------------

      const isPending = outgoingRequests.some((request) => {
        const sameUser = normalizeId(request?.userId) === normalizedProfileId;

        const status = String(request?.status || "").toUpperCase();

        return sameUser && status === "PENDING";
      });

      if (isPending) {
        return "pending";
      }

      // ------------------------------------------------
      // fallback:
      // รองรับกรณี Backend ในอนาคตส่ง friendStatus มาให้
      // ------------------------------------------------

      const normalizedFallback = String(fallbackStatus || "")
        .trim()
        .toLowerCase();

      if (normalizedFallback === "friend" || normalizedFallback === "pending") {
        return normalizedFallback;
      }

      return "none";
    } catch (error) {
      console.error("ตรวจสอบสถานะเพื่อนไม่สำเร็จ:", error);

      // ถ้า Backend อนาคตส่ง status มาแล้ว
      // ยังสามารถใช้ค่าดังกล่าวเป็น fallback ได้

      const normalizedFallback = String(fallbackStatus || "")
        .trim()
        .toLowerCase();

      if (normalizedFallback === "friend" || normalizedFallback === "pending") {
        return normalizedFallback;
      }

      return "error";
    }
  }

  // --------------------------------------------------
  // Update ปุ่ม Action
  // --------------------------------------------------

  function updateActionButton() {
    actionButton.classList.remove(
      "is-pending",
      "is-friend",
      "is-loading",
      "is-error",
      "is-self",
    );

    actionButton.disabled = false;

    // ------------------------------
    // ตัวเราเอง
    // ------------------------------

    if (currentStatus === "self") {
      actionButton.classList.add("is-self");
      return;
    }

    // ------------------------------
    // เป็นเพื่อนแล้ว
    // ------------------------------

    if (currentStatus === "friend") {
      actionIcon.textContent = "💬";
      actionText.textContent = "ส่งข้อความ";

      actionButton.classList.add("is-friend");

      return;
    }

    // ------------------------------
    // ส่งคำขอแล้ว
    // ------------------------------

    if (currentStatus === "pending") {
      actionIcon.textContent = "✓";
      actionText.textContent = "ส่งคำขอแล้ว";

      actionButton.classList.add("is-pending");
      actionButton.disabled = true;

      return;
    }

    // ------------------------------
    // ตรวจสอบสถานะไม่สำเร็จ
    // ------------------------------

    if (currentStatus === "error") {
      actionIcon.textContent = "!";
      actionText.textContent = "ตรวจสอบสถานะไม่ได้";

      actionButton.classList.add("is-error");
      actionButton.disabled = true;

      return;
    }

    // ------------------------------
    // กำลังโหลด
    // ------------------------------

    if (relationshipLoading) {
      actionIcon.textContent = "…";
      actionText.textContent = "กำลังตรวจสอบ...";

      actionButton.classList.add("is-loading");
      actionButton.disabled = true;

      return;
    }

    // ------------------------------
    // ยังไม่เป็นเพื่อน
    // ------------------------------

    actionIcon.textContent = "👤+";
    actionText.textContent = "เพิ่มเพื่อน";
  }

  // --------------------------------------------------
  // เปิด Profile Popup
  // --------------------------------------------------

  async function openFriendProfile(element) {
    if (!element) {
      return;
    }

    const requestToken = ++profileRequestToken;

    const name = getDatasetValue(element, "name", "").trim() || "ไม่ระบุชื่อ";

    const year = getDatasetValue(element, "year", "").trim();

    const image =
      getDatasetValue(element, "image", "").trim() || "/images/man2.jpg";

    const bio =
      getDatasetValue(element, "bio", "").trim() ||
      "ยังไม่มีข้อมูลเกี่ยวกับฉัน";

    const interests = getDatasetValue(element, "interests", "");

    const fallbackStatus =
      getDatasetValue(element, "status", "none").trim() || "none";

    const id = getDatasetValue(element, "id", "").trim();

    // ------------------------------------------------
    // ตรวจว่า Profile ที่กดคือ "ตัวเราเอง"
    //
    // room.js มีการใส่ class:
    // .local-member
    //
    // ให้กับ card ของผู้ใช้ปัจจุบันอยู่แล้ว
    // ------------------------------------------------

    const isSelf =
      element.classList.contains("local-member") ||
      getDatasetValue(element, "self", "false") === "true";

    currentProfileId = id;

    // ถ้าเป็นตัวเอง ให้ตั้งเป็น self ทันที
    // ไม่ต้องรอ API friend / request
    currentStatus = isSelf ? "self" : "none";

    // ------------------------------
    // แสดงข้อมูล Profile
    // ------------------------------

    profileName.textContent = name;

    profileYear.textContent = year;

    profileImage.src = image;
    profileImage.alt = name;

    profileBio.textContent = bio;

    renderInterests(interests);

    // ------------------------------
    // เปิด Popup ก่อน
    // ------------------------------

    overlay.classList.add("show");
    overlay.setAttribute("aria-hidden", "false");

    document.body.style.overflow = "hidden";

    // ------------------------------------------------
    // ถ้าเป็นตัวเอง
    // ไม่ต้องโหลด friend/request
    // ------------------------------------------------

    if (isSelf) {
      relationshipLoading = false;
      updateActionButton();
      return;
    }

    // ------------------------------
    // ตรวจสอบสถานะปุ่ม
    // ------------------------------

    relationshipLoading = true;
    updateActionButton();

    try {
      const status = await resolveRelationshipStatus(
        id,
        fallbackStatus,
        isSelf,
      );

      // ถ้าระหว่างรอ user กด profile คนอื่น
      // ห้ามเอาผล request เก่ามาใส่ profile ใหม่

      if (requestToken !== profileRequestToken || id !== currentProfileId) {
        return;
      }

      currentStatus = status;
    } catch (error) {
      console.error("โหลดสถานะ Profile ไม่สำเร็จ:", error);

      if (requestToken !== profileRequestToken || id !== currentProfileId) {
        return;
      }

      currentStatus = "error";
    } finally {
      if (requestToken === profileRequestToken && id === currentProfileId) {
        relationshipLoading = false;
        updateActionButton();
      }
    }
  }

  // --------------------------------------------------
  // ปิด Profile Popup
  // --------------------------------------------------

  function closeFriendProfile() {
    profileRequestToken++;

    currentProfileId = null;
    currentStatus = "none";
    relationshipLoading = false;

    overlay.classList.remove("show");
    overlay.setAttribute("aria-hidden", "true");

    document.body.style.overflow = "";

    actionButton.disabled = false;

    actionButton.classList.remove(
      "is-pending",
      "is-friend",
      "is-loading",
      "is-error",
      "is-self",
    );
  }

  // --------------------------------------------------
  // Click สมาชิกในห้อง
  // --------------------------------------------------

  document.addEventListener("click", (event) => {
    const profile = event.target.closest("[data-profile]");

    if (!profile) {
      return;
    }

    openFriendProfile(profile);
  });

  // --------------------------------------------------
  // ปุ่มปิด
  // --------------------------------------------------

  closeButton.addEventListener("click", () => {
    closeFriendProfile();
  });

  // --------------------------------------------------
  // คลิกพื้นหลังเพื่อปิด
  // --------------------------------------------------

  overlay.addEventListener("click", (event) => {
    if (event.target === overlay) {
      closeFriendProfile();
    }
  });

  // --------------------------------------------------
  // ESC เพื่อปิด
  // --------------------------------------------------

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && overlay.classList.contains("show")) {
      closeFriendProfile();
    }
  });

  // --------------------------------------------------
  // Action Button
  // --------------------------------------------------

  actionButton.addEventListener("click", async () => {
    if (!currentProfileId) {
      return;
    }

    // ----------------------------------------------
    // ตัวเราเอง
    // ไม่สามารถเพิ่มตัวเองเป็นเพื่อน
    // ----------------------------------------------

    if (currentStatus === "self") {
      return;
    }

    // ----------------------------------------------
    // เป็นเพื่อนแล้ว → ไปหน้า Friend / Chat
    // ----------------------------------------------

    if (currentStatus === "friend") {
      window.location.href = `/friend?id=${encodeURIComponent(currentProfileId)}`;

      return;
    }

    // ----------------------------------------------
    // ส่งคำขอไปแล้ว → กดซ้ำไม่ได้
    // ----------------------------------------------

    if (currentStatus === "pending") {
      return;
    }

    // ----------------------------------------------
    // กำลังตรวจสอบสถานะ
    // ----------------------------------------------

    if (relationshipLoading) {
      return;
    }

    // ----------------------------------------------
    // สถานะผิดพลาด
    // ----------------------------------------------

    if (currentStatus === "error") {
      return;
    }

    try {
      actionButton.disabled = true;

      actionIcon.textContent = "…";
      actionText.textContent = "กำลังส่ง...";
      actionButton.classList.add("is-loading");

      const response = await fetch("/api/friend-requests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          receiverId: currentProfileId,
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();

        throw new Error(`ส่งคำขอไม่สำเร็จ (${response.status}) ${errorText}`);
      }

      const result = await response.json();

      console.log("ส่งคำขอเป็นเพื่อนสำเร็จ:", result);

      // ----------------------------------------------
      // อัปเดต cache ทันที
      // ----------------------------------------------

      if (Array.isArray(outgoingRequestsCache)) {
        outgoingRequestsCache.push({
          id: result?.id || "",
          userId: result?.userId || currentProfileId,
          firstname: result?.firstname || "",
          lastname: result?.lastname || "",
          imageUrl: result?.imageUrl || "",
          year: result?.year || 0,
          department: result?.department || "",
          bio: result?.bio || "",
          interests: result?.interests || [],
          status: result?.status || "PENDING",
          createdAt: result?.createdAt || "",
        });
      }

      currentStatus = "pending";

      updateActionButton();

      // ใช้ SweetAlert ถ้ามี
      if (window.Swal) {
        await window.Swal.fire({
          icon: "success",
          title: "ส่งคำขอแล้ว",
          text: "ส่งคำขอเป็นเพื่อนเรียบร้อยแล้ว",
          confirmButtonText: "ตกลง",
        });
      } else {
        alert("ส่งคำขอเป็นเพื่อนแล้ว");
      }
    } catch (error) {
      console.error("ส่งคำขอเป็นเพื่อนล้มเหลว:", error);

      actionButton.disabled = false;
      actionButton.classList.remove("is-loading");

      // กลับไปสถานะเพิ่มเพื่อน
      currentStatus = "none";
      updateActionButton();

      if (window.Swal) {
        await window.Swal.fire({
          icon: "error",
          title: "ส่งคำขอไม่สำเร็จ",
          text: "ไม่สามารถส่งคำขอเป็นเพื่อนได้",
          confirmButtonText: "ตกลง",
        });
      } else {
        alert("ไม่สามารถส่งคำขอเป็นเพื่อนได้");
      }
    }
  });
});
