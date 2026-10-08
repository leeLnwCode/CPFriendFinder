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
      alert("กรุณากรอก Email");
      return;
    }

    if (password === "") {
      alert("กรุณากรอก Password");
      return;
    }

    if (password.length < 8) {
      alert("รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร");
      return;
    }

    if (confirmPassword === "") {
      alert("กรุณายืนยันรหัสผ่าน");
      return;
    }

    if (password !== confirmPassword) {
      alert("รหัสผ่านไม่ตรงกัน");
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

  document.querySelectorAll(".step").forEach(function (indicator) {
    indicator.classList.remove("active");
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

  // ตรวจสอบรหัสผ่านอย่างน้อย 8 ตัว
  if (password.length < 8) {
    alert("รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร");
    return;
  }

  /* =========================
     Validation
  ========================= */

  if (email === "") {
    alert("กรุณากรอก Email");
    return;
  }

  if (password === "") {
    alert("กรุณากรอก Password");
    return;
  }

  if (password !== confirmPassword) {
    alert("รหัสผ่านไม่ตรงกัน");
    return;
  }

  if (firstname === "") {
    alert("กรุณากรอกชื่อ");
    return;
  }

  if (lastname === "") {
    alert("กรุณากรอกนามสกุล");
    return;
  }

  if (dateOfBirth === "") {
    alert("กรุณาเลือกวันเกิด");
    return;
  }

  if (!year) {
    alert("กรุณาเลือกชั้นปี");
    return;
  }

  if (department === "") {
    alert("กรุณาเลือกสาขา");
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

      alert("ไม่สามารถอ่านรูปโปรไฟล์ได้");

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
  };

  console.log("Register request:", requestData);
  console.log(requestData);

  /* =========================
     Call Backend
  ========================= */

  try {
    const response = await fetch("/api/auth/register", {
      method: "POST",

      headers: {
        "Content-Type": "application/json",
      },

      body: JSON.stringify(requestData),
    });

    const data = await response.json();

    console.log("Register response:", data);

    /* =========================
       Error
    ========================= */

    if (!response.ok) {
      console.error("Register failed:", data);

      alert("สมัครสมาชิกไม่สำเร็จ");

      return;
    }

    /* =========================
       Success
    ========================= */

    console.log("Register success:", data);

    alert("สมัครสมาชิกสำเร็จ");

    window.location.href = "/login";
  } catch (error) {
    console.error("Register connection error:", error);

    alert("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้");
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
