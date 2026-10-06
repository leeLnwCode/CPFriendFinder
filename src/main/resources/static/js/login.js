document.addEventListener("DOMContentLoaded", function () {
  const loginForm = document.getElementById("loginForm");

  if (!loginForm) {
    return;
  }

  loginForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    if (email === "" || password === "") {
      alert("กรุณากรอก Email และ Password");
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
        body: JSON.stringify(requestData),
      });

      const data = await response.json();

      console.log("Login response:", data);

      if (!response.ok) {
        alert("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
        return;
      }

      // เก็บข้อมูลผู้ใช้ที่ Login
      sessionStorage.setItem("currentUser", JSON.stringify(data));

      alert("เข้าสู่ระบบสำเร็จ");

      window.location.href = "/home";
    } catch (error) {
      console.error("Login error:", error);

      alert("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้");
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
