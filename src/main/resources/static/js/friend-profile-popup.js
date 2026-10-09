/* =========================================================
   FRIEND PROFILE POPUP
========================================================= */

document.addEventListener("DOMContentLoaded", () => {
  const overlay = document.getElementById("friendProfileOverlay");

  if (!overlay) {
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

  let currentProfileId = null;
  let currentStatus = "none";

  /* =====================================================
       OPEN POPUP
    ===================================================== */

  function openFriendProfile(element) {
    const name = element.dataset.name || "ไม่ระบุชื่อ";

    const year = element.dataset.year || "";

    const image = element.dataset.image || "/images/man2.jpg";

    const bio = element.dataset.bio || "ยังไม่มีข้อมูลเกี่ยวกับฉัน";

    const interests = element.dataset.interests || "";
    
    const status = element.dataset.status || "none";

    const id = element.dataset.id || "";

    currentProfileId = id;
    currentStatus = status;

    profileName.textContent = name;

    profileYear.textContent = year;

    profileImage.src = image;
    profileImage.alt = name;

    profileBio.textContent = bio;

    /* Interests */

    profileInterests.innerHTML = "";

    const interestList = interests
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item !== "");

    if (interestList.length === 0) {
      const empty = document.createElement("span");

      empty.textContent = "ยังไม่มีข้อมูล";

      empty.className = "friend-profile-interest";

      profileInterests.appendChild(empty);
    } else {
      interestList.forEach((interest) => {
        const tag = document.createElement("span");

        tag.className = "friend-profile-interest";

        tag.textContent = interest;

        profileInterests.appendChild(tag);
      });
    }

    updateActionButton();

    overlay.classList.add("show");

    overlay.setAttribute("aria-hidden", "false");

    document.body.style.overflow = "hidden";
  }

  /* =====================================================
       CLOSE POPUP
    ===================================================== */

  function closeFriendProfile() {
    overlay.classList.remove("show");

    overlay.setAttribute("aria-hidden", "true");

    document.body.style.overflow = "";
  }

  /* =====================================================
       ACTION BUTTON
    ===================================================== */

  function updateActionButton() {
    actionButton.classList.remove("is-pending", "is-friend");

    if (currentStatus === "friend") {
      actionIcon.textContent = "💬";

      actionText.textContent = "ส่งข้อความ";

      actionButton.classList.add("is-friend");

      return;
    }

    if (currentStatus === "pending") {
      actionIcon.textContent = "✓";

      actionText.textContent = "ส่งคำขอแล้ว";

      actionButton.classList.add("is-pending");

      return;
    }

    actionIcon.textContent = "👤+";

    actionText.textContent = "เพิ่มเพื่อน";
  }

  /* =====================================================
       CLICK PROFILE
    ===================================================== */

  document.addEventListener("click", (event) => {
    const profile = event.target.closest("[data-profile]");

    if (!profile) {
      return;
    }

    openFriendProfile(profile);
  });

  /* =====================================================
       CLOSE
    ===================================================== */

  closeButton.addEventListener("click", closeFriendProfile);

  overlay.addEventListener("click", (event) => {
    if (event.target === overlay) {
      closeFriendProfile();
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && overlay.classList.contains("show")) {
      closeFriendProfile();
    }
  });

  /* =====================================================
       SEND FRIEND REQUEST
    ===================================================== */

  actionButton.addEventListener("click", async () => {
    if (!currentProfileId) {
      return;
    }

    if (currentStatus === "friend") {
      window.location.href = `/friend?id=${currentProfileId}`;

      return;
    }

    if (currentStatus === "pending") {
      return;
    }

    try {
  actionButton.disabled = true;

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

    throw new Error(
      `ส่งคำขอไม่สำเร็จ (${response.status}) ${errorText}`
    );
  }

  const result = await response.json();

  console.log("ส่งคำขอเป็นเพื่อนสำเร็จ:", result);

  currentStatus = "pending";

  updateActionButton();

  alert("ส่งคำขอเป็นเพื่อนแล้ว");

} catch (error) {
  console.error("ส่งคำขอเป็นเพื่อนล้มเหลว:", error);

  alert("ไม่สามารถส่งคำขอเป็นเพื่อนได้");

} finally {
  actionButton.disabled = false;
}
  });
});
