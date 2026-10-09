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

  const accountFields = {
    email: { id: "accountEmail", message: "กรอกอีเมลใหม่" },
    username: { id: "accountUsername", message: "กรอกชื่อผู้ใช้งานใหม่" },
    password: { id: "accountPassword", message: "กรอกรหัสผ่านใหม่" },
  };

  document.querySelectorAll(".edit-button").forEach((button) => {
    button.addEventListener("click", () => {
      const type = button.dataset.edit;
      const field = accountFields[type];
      const element = field && document.getElementById(field.id);

      if (!element) {
        return;
      }

      const newValue = prompt(field.message)?.trim();

      if (!newValue) {
        return;
      }

      element.textContent = type === "password" ? "• • • • • • • •" : newValue;
    });
  });

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
