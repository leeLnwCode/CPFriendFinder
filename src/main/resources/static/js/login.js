document.addEventListener("DOMContentLoaded", function () {
  const loginForm = document.getElementById("loginForm");

  if (!loginForm) {
    return;
  }

  loginForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    // =========================
    // Validate
    // =========================

    if (email === "" || password === "") {
      Swal.fire({
        icon: "warning",
        title: "กรุณากรอกข้อมูล",
        text: "กรุณากรอก Email และ Password",
        confirmButtonText: "ตกลง",
      });
      return;
    }

    const requestData = {
      email: email,
      password: password,
    };

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify(requestData),
      });

      const data = await response.json();

      console.log("Login response:", data);

      // =========================
      // Login Failed
      // =========================

      if (!response.ok) {
        Swal.fire({
          icon: "error",
          title: "เข้าสู่ระบบไม่สำเร็จ",
          text: "อีเมลหรือรหัสผ่านไม่ถูกต้อง",
          confirmButtonText: "ลองอีกครั้ง",
        });
        return;
      }

      // =========================
      // Login Success
      // =========================

      sessionStorage.setItem("currentUser", JSON.stringify(data));

      await Swal.fire({
        icon: "success",
        title: "เข้าสู่ระบบสำเร็จ",
        text: "กำลังเข้าสู่หน้าหลัก...",
        showConfirmButton: false,
        timer: 1500,
      });

      window.location.href = "/home";

    } catch (error) {
      console.error("Login error:", error);

      // =========================
      // Server Connection Error
      // =========================

      Swal.fire({
        icon: "error",
        title: "เกิดข้อผิดพลาด",
        text: "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้",
        confirmButtonText: "ตกลง",
      });
    }
  });

  // =========================
  // Show / Hide Password
  // =========================

  const showPasswordButton = document.getElementById("showPassword");
  const passwordInput = document.getElementById("password");

  if (showPasswordButton && passwordInput) {
    showPasswordButton.addEventListener("click", function () {
      if (passwordInput.type === "password") {
        passwordInput.type = "text";
        showPasswordButton.textContent = "Hide";
      } else {
        passwordInput.type = "password";
        showPasswordButton.textContent = "Show";
      }
    });
  }
});