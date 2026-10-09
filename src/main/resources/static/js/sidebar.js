document.addEventListener("DOMContentLoaded", async () => {
  const profileImage = document.getElementById("sidebarProfileImage");
  const profileName = document.getElementById("sidebarProfileName");
  const profileYear = document.getElementById("sidebarProfileYear");
  const sidebarProfile = document.querySelector(".sidebar-profile");
  const profileDepartment = document.getElementById("sidebarProfileDepartment");

  if (!profileName) return;

  function renderUser(user) {
    sidebarProfile?.classList.add("profile-loaded");
    if (!user) return;

    const firstname = user.firstname || "";
    const lastname = user.lastname || "";
    const fullname = `${firstname} ${lastname}`.trim();

    profileName.textContent = fullname || "ไม่ระบุชื่อ";

    profileYear.textContent =
      user.year !== null && user.year !== undefined ? `ปี ${user.year}` : "";

    profileDepartment.textContent = user.department || "";

    profileImage.onerror = () => {
      profileImage.onerror = null;
      profileImage.src = "/images/avatar-placeholder.svg";
    };
    profileImage.src = user.image_url || "/images/avatar-placeholder.svg";
  }

  const cached = sessionStorage.getItem("cp-current-user");

  if (cached) {
    try {
      renderUser(JSON.parse(cached));
    } catch (_) {
      sessionStorage.removeItem("cp-current-user");
    }
  }
  if (!cached) {
    profileName.textContent = "กำลังโหลด...";
    profileYear.textContent = "";
    profileDepartment.textContent = "";

    sidebarProfile?.classList.add("profile-loaded");
  }

  try {
    const response = await fetch("/api/users/me", {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(`โหลดข้อมูลผู้ใช้ไม่สำเร็จ (${response.status})`);
    }

    const user = await response.json();

    sessionStorage.setItem("cp-current-user", JSON.stringify(user));

    renderUser(user);
  } catch (error) {
    console.error("โหลดข้อมูล Sidebar ไม่สำเร็จ:", error);

    if (!cached) {
      profileName.textContent = "ไม่สามารถโหลดข้อมูล";
      profileYear.textContent = "";
      profileDepartment.textContent = "";
    }
  }
});
