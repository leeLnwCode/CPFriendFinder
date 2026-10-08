document.addEventListener("DOMContentLoaded", async () => {
  const profileImage = document.getElementById("sidebarProfileImage");

  const profileName = document.getElementById("sidebarProfileName");

  const profileYear = document.getElementById("sidebarProfileYear");

  const profileDepartment = document.getElementById("sidebarProfileDepartment");

  const notificationBadge = document.getElementById("notificationBadge");

  /* =====================================================
     LOAD CURRENT USER
  ====================================================== */

  async function loadCurrentUser() {
    try {
      const response = await fetch("/api/users/me", {
        method: "GET",

        headers: {
          Accept: "application/json",
        },

        credentials: "include",
      });

      if (response.status === 401 || response.status === 403) {
        sessionStorage.removeItem("currentUser");

        window.location.href = "/login";

        return null;
      }

      if (!response.ok) {
        throw new Error(`โหลดข้อมูลผู้ใช้ไม่สำเร็จ (${response.status})`);
      }

      const user = await response.json();

      sessionStorage.setItem("currentUser", JSON.stringify(user));

      return user;
    } catch (error) {
      console.error("โหลดข้อมูลผู้ใช้ไม่สำเร็จ:", error);

      return null;
    }
  }

  /* =====================================================
     RENDER SIDEBAR PROFILE
  ====================================================== */

  function renderProfile(user) {
    if (!user) {
      return;
    }

    if (profileName) {
      const firstname = user.firstname || "";

      const lastname = user.lastname || "";

      const fullname = `${firstname} ${lastname}`.trim();

      profileName.textContent = fullname || "ไม่ระบุชื่อ";
    }

    if (profileYear) {
      if (user.year !== null && user.year !== undefined && user.year !== "") {
        profileYear.textContent = `ปี ${user.year}`;
      } else {
        profileYear.textContent = "";
      }
    }

    if (profileDepartment) {
      profileDepartment.textContent = user.department || "";
    }

    if (profileImage && user.image_url) {
      profileImage.src = user.image_url;
    }
  }

  /* =====================================================
     LOAD NOTIFICATION BADGE
  ====================================================== */

  async function loadNotificationBadge() {
    if (!notificationBadge) {
      return;
    }

    try {
      const response = await fetch("/api/notifications/unread-count", {
        method: "GET",

        headers: {
          Accept: "application/json",
        },

        credentials: "include",
      });

      if (response.status === 401 || response.status === 403) {
        return;
      }

      if (!response.ok) {
        throw new Error(`โหลดจำนวนแจ้งเตือนไม่สำเร็จ (${response.status})`);
      }

      const data = await response.json();

      const count = Number(data?.count || 0);

      if (!Number.isFinite(count) || count <= 0) {
        notificationBadge.textContent = "";

        notificationBadge.classList.remove("show");

        return;
      }

      notificationBadge.textContent = count > 99 ? "99+" : String(count);

      notificationBadge.classList.add("show");
    } catch (error) {
      console.error("โหลด Notification Badge ไม่สำเร็จ:", error);
    }
  }

  /* =====================================================
     START
  ====================================================== */

  const user = await loadCurrentUser();

  if (user) {
    renderProfile(user);
  }

  await loadNotificationBadge();
});
