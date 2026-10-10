document.addEventListener("DOMContentLoaded", function () {
  const loginForm = document.getElementById("loginForm");

  if (!loginForm) {
    return;
  }

  const params = new URLSearchParams(location.search);
  if (params.get("registered") === "1") {
    let saved = null;
    try {
      saved = JSON.parse(sessionStorage.getItem("registrationLogin") || "null");
      sessionStorage.removeItem("registrationLogin");
    } catch (_) {}
    const emailField = document.getElementById("email");
    emailField.value = params.get("email") || "";
    if (
      saved &&
      saved.expiresAt > Date.now() &&
      saved.email === emailField.value
    )
      document.getElementById("password").value = saved.password || "";
    const notice = document.createElement("p");
    notice.className = "registration-success";
    notice.setAttribute("role", "status");
    notice.textContent = "สมัครสมาชิกสำเร็จ พร้อมเข้าสู่ระบบ";
    notice.style.cssText =
      "color:#166534;background:#dcfce7;padding:12px;border-radius:12px";
    loginForm.prepend(notice);
    history.replaceState(null, "", location.pathname);
  }

  const status = document.createElement("p");
  status.id = "loginStatus";
  status.setAttribute("role", "status");
  status.setAttribute("aria-live", "polite");
  status.hidden = true;
  loginForm.append(status);
  let submitting = false;
  function message(text) {
    status.textContent = text;
    status.hidden = false;
    status.style.cssText =
      "color:#b91c1c;background:#fef2f2;padding:12px;border-radius:12px;line-height:1.6";
  }
  function finish(user) {
    try {
      sessionStorage.setItem("currentUser", JSON.stringify(user));
      sessionStorage.removeItem("cp-current-user");
    } catch (_) {}
    window.location.assign("/home");
  }
  async function verifySession(email) {
    try {
      const r = await fetch("/api/users/me", {
        credentials: "include",
        headers: { Accept: "application/json" },
        signal: AbortSignal.timeout(10000),
      });
      if (!r.ok) return null;
      const user = await r.json();
      return String(user.email || "").toLowerCase() === email.toLowerCase()
        ? user
        : null;
    } catch (_) {
      return null;
    }
  }
  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (submitting) return;
    const email = document.getElementById("email").value.trim(),
      password = document.getElementById("password").value;
    if (!email || !password) {
      message("กรุณากรอกอีเมลและรหัสผ่าน");
      return;
    }
    const submit = loginForm.querySelector('[type="submit"]');
    submitting = true;
    status.hidden = true;
    if (submit) {
      submit.disabled = true;
      submit.setAttribute("aria-busy", "true");
    }
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({ email, password }),
        signal: AbortSignal.timeout(35000),
      });
      if (!response.ok) {
        let data = {};
        try {
          data = await response.json();
        } catch (_) {}
        if (response.status === 400 || response.status === 401) {
          message(
            /not active/i.test(data.message || "")
              ? "บัญชีนี้ยังไม่พร้อมใช้งาน"
              : "อีเมลหรือรหัสผ่านไม่ถูกต้อง",
          );
        } else if (response.status === 403)
          message("ระบบปฏิเสธคำขอเข้าสู่ระบบ (HTTP 403) กรุณาลองอีกครั้ง");
        else
          message(
            "เซิร์ฟเวอร์เข้าสู่ระบบไม่พร้อมชั่วคราว (HTTP " +
              response.status +
              ") กรุณาลองอีกครั้ง",
          );
        return;
      }
      let data = null;
      try {
        data = await response.json();
      } catch (_) {}
      if (!data?.id && !data?.userId) data = await verifySession(email);
      if (data?.id || data?.userId) {
        finish(data);
        return;
      }
      message(
        "เซิร์ฟเวอร์รับคำขอแล้ว แต่ยังยืนยันบัญชีไม่ได้ กรุณาลองเข้าสู่ระบบอีกครั้ง",
      );
    } catch (error) {
      message(
        error.name === "TimeoutError"
          ? "เซิร์ฟเวอร์ตอบช้า กรุณาลองอีกครั้ง"
          : "การเชื่อมต่อขัดข้องระหว่างเข้าสู่ระบบ กรุณาลองอีกครั้ง",
      );
    } finally {
      submitting = false;
      if (submit) {
        submit.disabled = false;
        submit.removeAttribute("aria-busy");
      }
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
        showPasswordButton.textContent = "ซ่อน";
      } else {
        passwordInput.type = "password";
        showPasswordButton.textContent = "แสดง";
      }
    });
  }
});
