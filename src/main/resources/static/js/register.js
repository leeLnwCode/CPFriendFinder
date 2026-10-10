let currentStep = 1;

/* =========================
   CHANGE STEP
========================= */

function nextStep(step) {
  /* =========================
     VALIDATE BEFORE NEXT STEP
  ========================= */

  if (step === 2) {
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const confirmPasswordInput = document.getElementById("confirmPassword");

    const email = emailInput ? emailInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value : "";
    const confirmPassword = confirmPasswordInput
      ? confirmPasswordInput.value
      : "";

    if (email === "") {
      showRegisterError("กรุณากรอก Email");
      return;
    }

    if (password === "") {
      showRegisterError("กรุณากรอก Password");
      return;
    }

    if (password.length < 8) {
      showRegisterError("รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร");
      return;
    }

    if (confirmPassword === "") {
      showRegisterError("กรุณายืนยันรหัสผ่าน");
      return;
    }

    if (password !== confirmPassword) {
      showRegisterError("รหัสผ่านไม่ตรงกัน");
      return;
    }
  }

  /* =========================
     CHANGE STEP
  ========================= */

  document.querySelectorAll(".register-step").forEach(function (section) {
    section.classList.remove("active-step");
  });

  const targetStep = document.getElementById("step" + step);

  if (targetStep) {
    targetStep.classList.add("active-step");
  }

  document.querySelectorAll(".step").forEach(function (indicator, index) {
    indicator.classList.remove("active");
    indicator.classList.toggle("done", index + 1 < step);
  });

  for (let i = 1; i <= step; i++) {
    const indicator = document.getElementById("stepIndicator" + i);

    if (indicator) {
      indicator.classList.add("active");
    }
  }

  currentStep = step;

  window.scrollTo({
    top: 0,
    behavior: "smooth",
  });
}

/* =========================
   SHOW / HIDE PASSWORD
========================= */

document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll(".password-toggle").forEach(function (button) {
    button.addEventListener("click", function () {
      const targetId = button.getAttribute("data-target");

      const input = document.getElementById(targetId);

      if (!input) {
        return;
      }

      if (input.type === "password") {
        input.type = "text";
        button.textContent = "Hide";
      } else {
        input.type = "password";
        button.textContent = "Show";
      }
    });
  });
});

/* =========================
   PROFILE IMAGE PREVIEW
========================= */

function previewProfile(event) {
  const file = event.target.files[0];

  if (!file) {
    return;
  }

  const preview = document.getElementById("profilePreview");

  const placeholder = document.getElementById("profilePlaceholder");

  if (!preview || !placeholder) {
    return;
  }

  preview.src = URL.createObjectURL(file);

  preview.style.display = "block";

  placeholder.style.display = "none";
}

/* =========================
   INTEREST
========================= */

function toggleInterest(button) {
  button.classList.toggle("selected");
}

/* =========================
   FILE → BASE64
========================= */

function fileToBase64(file) {
  return new Promise(function (resolve, reject) {
    const reader = new FileReader();

    reader.onload = function () {
      resolve(reader.result);
    };

    reader.onerror = function (error) {
      reject(error);
    };

    reader.readAsDataURL(file);
  });
}

/* =========================
   CREATE ACCOUNT
========================= */

async function createAccount() {
  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;
  const confirmPassword = document.getElementById("confirmPassword").value;
  const firstname = document.getElementById("firstname").value.trim();
  const lastname = document.getElementById("lastname").value.trim();
  const dateOfBirth = document.getElementById("dateOfBirth").value;
  const year = Number(document.getElementById("year").value);
  const department = document.getElementById("department").value;
  const profileInput = document.getElementById("profileImage");
  const selectedInterests = Array.from(
    document.querySelectorAll("#step3 .interest.selected"),
  )
    .map(function (button) {
      const parts = button.textContent.trim().split(/\s+/);

      return parts.length > 1 ? parts.slice(1).join(" ") : parts[0];
    })
    .filter(function (name) {
      return name !== "";
    });

  // ตรวจสอบรหัสผ่านอย่างน้อย 8 ตัว
  if (password.length < 8) {
    showRegisterError("รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร");
    return;
  }

  /* =========================
     Validation
  ========================= */

  if (email === "") {
    showRegisterError("กรุณากรอก Email");
    return;
  }

  if (password === "") {
    showRegisterError("กรุณากรอก Password");
    return;
  }

  if (password !== confirmPassword) {
    showRegisterError("รหัสผ่านไม่ตรงกัน");
    return;
  }

  if (firstname === "") {
    showRegisterError("กรุณากรอกชื่อ");
    return;
  }

  if (lastname === "") {
    showRegisterError("กรุณากรอกนามสกุล");
    return;
  }

  if (dateOfBirth === "") {
    showRegisterError("กรุณาเลือกวันเกิด");
    return;
  }

  if (!year) {
    showRegisterError("กรุณาเลือกชั้นปี");
    return;
  }

  if (department === "") {
    showRegisterError("กรุณาเลือกสาขา");
    return;
  }

  /* =========================
     Profile Image
  ========================= */

  let imageBase64 = "";

  if (profileInput && profileInput.files.length > 0) {
    try {
      imageBase64 = await fileToBase64(profileInput.files[0]);
    } catch (error) {
      console.error("Image error:", error);

      showRegisterError("ไม่สามารถอ่านรูปโปรไฟล์ได้");

      return;
    }
  }

  /* =========================
     API Request
  ========================= */

  const requestData = {
    email: email,

    password: password,

    firstname: firstname,

    lastname: lastname,

    imageBase64: imageBase64,

    dateOfBirth: dateOfBirth,

    year: year,

    department: department,

    interests: selectedInterests,
  };

  if (window.registrationSubmitting) return;
  window.registrationSubmitting = true;
  const submit = document.querySelector("#step3 .next-button");
  if (submit) {
    submit.disabled = true;
    submit.textContent = "กำลังสร้างบัญชี...";
  }
  let response;
  try {
    response = await fetch("/api/auth/register", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestData),
    });
  } catch (_) {
    showRegisterError(
      "การเชื่อมต่อขัดข้อง หากสร้างบัญชีแล้ว ให้ลองเข้าสู่ระบบก่อนสมัครซ้ำ",
    );
  }
  if (response) {
    if (response.ok) {
      // Successful creation does not depend on parsing an optional body or browser storage.
      try {
        sessionStorage.setItem(
          "registrationLogin",
          JSON.stringify({
            email,
            password,
            expiresAt: Date.now() + 10 * 60 * 1000,
          }),
        );
      } catch (_) {}
      window.location.assign(
        "/login?registered=1&email=" + encodeURIComponent(email),
      );
      return;
    }
    let data = {};
    try {
      data = await response.json();
    } catch (_) {}
    const message = String(data.message || "");
    if (/email.*exists/i.test(message))
      showRegisterError("อีเมลนี้มีบัญชีแล้ว กรุณาเข้าสู่ระบบ");
    else if (response.status === 400)
      showRegisterError(
        "สมัครไม่สำเร็จ: " +
          (data.errors?.map((e) => e.message).join(" · ") ||
            message ||
            "ตรวจสอบข้อมูลที่กรอก"),
      );
    else showRegisterError("ระบบสมัครสมาชิกขัดข้อง กรุณาลองใหม่ภายหลัง");
  }
  window.registrationSubmitting = false;
  if (submit) {
    submit.disabled = false;
    submit.textContent = "สร้างบัญชี";
  }
}

/* =========================
   CREATE OTHER INTEREST
========================= */

function addCustomInterest() {
  const input = document.getElementById("customInterest");

  const interestName = input.value.trim();

  if (interestName === "") {
    return;
  }

  const interestList = document.getElementById("customList");

  const button = document.createElement("button");

  button.type = "button";

  button.className = "interest";

  button.textContent = "✨ " + interestName;

  button.onclick = function () {
    toggleInterest(this);
  };

  interestList.appendChild(button);

  input.value = "";
}

/* =========================
   INLINE ERROR (แทน alert)
========================= */

function showRegisterError(text) {
  let box = document.getElementById("registerError");

  if (!box) {
    box = document.createElement("div");
    box.id = "registerError";
    box.className = "register-error";
    box.setAttribute("role", "alert");
    const card = document.querySelector(".register-card");
    (card || document.body).appendChild(box);
  }

  box.textContent = text;
  box.hidden = false;

  clearTimeout(showRegisterError._timer);
  showRegisterError._timer = setTimeout(function () {
    box.hidden = true;
  }, 4000);
}
