document.addEventListener("DOMContentLoaded", () => {
  // =========================
  // SETTINGS MODALS
  // =========================

  const modals = document.querySelectorAll(".modal");

  const closeAllModals = () =>
    modals.forEach((modal) => modal.classList.remove("show"));

  // เปิด Modal
  document.querySelectorAll(".settings-card").forEach((card) => {
    card.addEventListener("click", () => {
      document.getElementById(card.dataset.modal)?.classList.add("show");
    });
  });

  // ปิดด้วยปุ่ม X
  document.querySelectorAll(".modal-close").forEach((button) => {
    button.addEventListener("click", () => {
      button.closest(".modal")?.classList.remove("show");
    });
  });

  // ปิดเมื่อคลิกพื้นที่ด้านนอก
  modals.forEach((modal) => {
    modal.addEventListener("click", (event) => {
      if (event.target === modal) {
        modal.classList.remove("show");
      }
    });
  });

  // ปิดด้วย ESC
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeAllModals();
    }
  });

  // =========================
  // ACCOUNT EDIT
  // =========================

  async function loadAccount() {
    try {
      const response = await fetch("/api/users/me", {
        credentials: "include",
        headers: { Accept: "application/json" },
      });
      if (!response.ok) throw new Error("โหลดบัญชีไม่สำเร็จ");
      const user = await response.json();
      document.getElementById("accountEmail").textContent =
        user.email || "ไม่ระบุ";
      document.getElementById("accountUsername").textContent =
        `${user.firstname || ""} ${user.lastname || ""}`.trim() ||
        "ไม่ระบุชื่อ";
    } catch (error) {
      document.getElementById("accountEmail").textContent = error.message;
      document.getElementById("accountUsername").textContent = "—";
    }
  }
  loadAccount();

  // =========================
  // THEME (ใช้ applyTheme / setTheme จาก theme.js)
  // =========================

  const themeOptions = document.querySelectorAll(".theme-option");

  function markSelected(theme) {
    themeOptions.forEach((option) => {
      option.classList.toggle("selected", option.dataset.theme === theme);
    });
  }

  themeOptions.forEach((option) => {
    option.addEventListener("click", () => {
      const theme = option.dataset.theme;
      setTheme(theme);
      markSelected(theme);
    });
  });

  markSelected(getSavedTheme());
});
