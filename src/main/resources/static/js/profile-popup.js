document.addEventListener("DOMContentLoaded", () => {
  const $ = (id) => document.getElementById(id),
    modal = $("profileModal");
  if (!modal) return;
  let photos = [],
    profilePhoto = null,
    busy = false,
    processing = false,
    generation = 0,
    returnFocus = null;
  const status = (message) => {
    $("profileEditStatus").textContent = message;
  };
  async function api(url, options = {}) {
    const r = await fetch(url, {
      credentials: "include",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      signal: AbortSignal.timeout(35000),
      ...options,
    });
    if (!r.ok) {
      let error = {};
      try {
        error = await r.json();
      } catch (_) {}
      throw Error(
        r.status === 401
          ? "กรุณาเข้าสู่ระบบใหม่"
          : r.status >= 500
            ? "ระบบไม่พร้อมชั่วคราว กรุณาลองอีกครั้ง"
            : error.message || "บันทึกข้อมูลไม่สำเร็จ",
      );
    }
    return r.json();
  }
  function enable() {
    const disabled = busy || processing;
    $("saveProfileEdit").disabled = disabled;
    $("galleryPhotoInput").disabled = disabled;
    $("addGalleryPhoto").disabled = disabled || photos.length >= 5;
    $("changeProfileImage").disabled = disabled;
    $("saveProfileEdit").textContent = busy
      ? "กำลังบันทึก…"
      : processing
        ? "กำลังเตรียมรูป…"
        : "บันทึกการเปลี่ยนแปลง";
  }
  function renderPhotos() {
    const grid = $("editGalleryPhotos");
    grid.replaceChildren();
    photos.forEach((photo, i) => {
      const card = document.createElement("article"),
        image = document.createElement("img"),
        bar = document.createElement("div");
      card.className = "profile-gallery-item";
      image.src = photo.imageBase64 || photo.url;
      image.alt = `รูปแนะนำตัว ${i + 1}`;
      image.onerror = () => {
        image.onerror = null;
        image.src = "/images/avatar-placeholder.svg";
      };
      for (const [label, text, action, disabled] of [
        [
          `เลื่อนรูป ${i + 1} ไปก่อนหน้า`,
          "←",
          () => {
            [photos[i - 1], photos[i]] = [photos[i], photos[i - 1]];
            renderPhotos();
          },
          i === 0,
        ],
        [
          `เลื่อนรูป ${i + 1} ไปถัดไป`,
          "→",
          () => {
            [photos[i + 1], photos[i]] = [photos[i], photos[i + 1]];
            renderPhotos();
          },
          i === photos.length - 1,
        ],
        [
          `ลบรูป ${i + 1}`,
          "ลบ",
          () => {
            photos.splice(i, 1);
            renderPhotos();
          },
          false,
        ],
      ]) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = text;
        button.setAttribute("aria-label", label);
        button.disabled = disabled || busy || processing;
        button.onclick = action;
        bar.append(button);
      }
      card.append(image, bar);
      grid.append(card);
    });
    $("addGalleryPhoto").disabled = busy || processing || photos.length >= 5;
    $("galleryPhotoCount").textContent = `${photos.length}/5 รูป`;
    if (!photos.length) {
      const empty = document.createElement("p");
      empty.className = "profile-help";
      empty.textContent =
        "ยังไม่มีรูปแนะนำตัว หน้าสุ่มคุยจะใช้รูปโปรไฟล์ของคุณ";
      grid.append(empty);
    }
  }
  async function prepare(file) {
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type))
      throw Error("เลือกรูป JPG, PNG หรือ WebP เท่านั้น");
    if (file.size > 8 * 1024 * 1024)
      throw Error("รูปต้นฉบับต้องมีขนาดไม่เกิน 8 MB");
    const image = await createImageBitmap(file);
    try {
      const ratio = Math.min(1, 1600 / Math.max(image.width, image.height)),
        canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(image.width * ratio));
      canvas.height = Math.max(1, Math.round(image.height * ratio));
      const context = canvas.getContext("2d");
      context.fillStyle = "#fff";
      context.fillRect(0, 0, canvas.width, canvas.height);
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      let data = canvas.toDataURL("image/jpeg", 0.82);
      if (data.length > 2800000) data = canvas.toDataURL("image/jpeg", 0.6);
      if (data.length > 2800000)
        throw Error("รูปนี้มีขนาดใหญ่เกินไป กรุณาเลือกรูปอื่น");
      return data;
    } finally {
      image.close();
    }
  }
  async function open() {
    if (busy) return;
    returnFocus = document.activeElement;
    const token = ++generation;
    modal.classList.add("show");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    status("กำลังโหลดโปรไฟล์…");
    busy = true;
    enable();
    try {
      const [user, interests, chosen] = await Promise.all([
        api("/api/users/me"),
        api("/api/interests"),
        api("/api/users/me/interests"),
      ]);
      if (token !== generation) return;
      for (const [field, key] of [
        ["editFirstname", "firstname"],
        ["editLastname", "lastname"],
        ["editBio", "bio"],
        ["editDateOfBirth", "dateOfBirth"],
        ["editYear", "year"],
        ["editDepartment", "department"],
      ])
        $(field).value = user[key] ?? "";
      profilePhoto = null;
      photos = (user.galleryPhotos || []).slice(0, 5).map((url) => ({ url }));
      $("editProfileImage").src =
        user.image_url || user.imageUrl || "/images/avatar-placeholder.svg";
      const selected = new Set(chosen.map((item) => String(item.id))),
        list = $("editProfileInterests");
      list.replaceChildren();
      for (const interest of interests.filter(
        (item) => item.isActive !== false,
      )) {
        const label = document.createElement("label"),
          input = document.createElement("input");
        input.type = "checkbox";
        input.value = interest.id;
        input.checked = selected.has(String(interest.id));
        label.append(input, document.createTextNode(interest.name));
        list.append(label);
      }
      if (!list.children.length) list.textContent = "ยังไม่มีความสนใจให้เลือก";
      $("bioCount").textContent = `${$("editBio").value.length}/500`;
      renderPhotos();
      status("");
      $("editFirstname").focus();
    } catch (error) {
      status(error.message);
      $("saveProfileEdit").dataset.loadFailed = "true";
    } finally {
      if (token === generation) {
        busy = false;
        enable();
        if ($("saveProfileEdit").dataset.loadFailed === "true")
          $("saveProfileEdit").disabled = true;
      }
    }
  }
  function close() {
    if (busy || processing) return;
    ++generation;
    modal.classList.remove("show");
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    returnFocus?.focus();
  }
  $("sidebarProfileButton")?.addEventListener("click", (event) => {
    event.preventDefault();
    delete $("saveProfileEdit").dataset.loadFailed;
    open();
  });
  window.addEventListener("cp-edit-own-profile", () => {
    delete $("saveProfileEdit").dataset.loadFailed;
    open();
  });
  for (const id of [
    "closeProfileModal",
    "cancelProfileEdit",
    "profileModalOverlay",
  ])
    $(id).addEventListener("click", close);
  $("editBio").addEventListener("input", () => {
    $("bioCount").textContent = `${$("editBio").value.length}/500`;
  });
  $("addGalleryPhoto").onclick = () => $("galleryPhotoInput").click();
  $("changeProfileImage").onclick = () => $("profileImageInput").click();
  for (const [id, gallery] of [
    ["galleryPhotoInput", true],
    ["profileImageInput", false],
  ])
    $(id).addEventListener("change", async (event) => {
      const files = [...event.target.files];
      event.target.value = "";
      if (!files.length || busy || processing) return;
      if (gallery && files.length + photos.length > 5) {
        status("เพิ่มรูปแนะนำตัวได้สูงสุด 5 รูป กรุณาลบบางรูปก่อน");
        return;
      }
      processing = true;
      enable();
      renderPhotos();
      status("กำลังเตรียมรูป…");
      try {
        const data = await Promise.all(files.map(prepare));
        if (gallery)
          photos.push(...data.map((imageBase64) => ({ imageBase64 })));
        else {
          profilePhoto = data[0];
          $("editProfileImage").src = profilePhoto;
        }
        status("รูปพร้อมแล้ว กดบันทึกเพื่อเผยแพร่");
      } catch (error) {
        status(error.message);
      } finally {
        processing = false;
        enable();
        renderPhotos();
      }
    });
  $("saveProfileEdit").onclick = async () => {
    if (busy || processing) return;
    const firstname = $("editFirstname").value.trim(),
      lastname = $("editLastname").value.trim();
    if (!firstname || !lastname) {
      status("กรุณากรอกชื่อและนามสกุล");
      return;
    }
    const bio = $("editBio").value.trim();
    if (bio.length > 500) {
      status("เกี่ยวกับฉันต้องไม่เกิน 500 ตัวอักษร");
      return;
    }
    const body = {
      firstname,
      lastname,
      bio,
      dateOfBirth: $("editDateOfBirth").value || null,
      year: $("editYear").value ? Number($("editYear").value) : null,
      department: $("editDepartment").value.trim(),
      interestIds: [
        ...$("editProfileInterests").querySelectorAll("input:checked"),
      ].map((input) => input.value),
      galleryPhotos: photos,
    };
    if (profilePhoto) body.imageBase64 = profilePhoto;
    busy = true;
    enable();
    renderPhotos();
    status("กำลังบันทึก…");
    try {
      const user = await api("/api/users/me", {
        method: "POST",
        body: JSON.stringify(body),
      });
      try {
        sessionStorage.setItem("cp-current-user", JSON.stringify(user));
      } catch (_) {}
      for (const [id, text] of [
        [
          "sidebarProfileName",
          `${user.firstname || ""} ${user.lastname || ""}`.trim(),
        ],
        ["sidebarProfileYear", user.year ? `ปี ${user.year}` : ""],
        ["sidebarProfileDepartment", user.department || ""],
      ])
        if ($(id)) $(id).textContent = text;
      if ($("sidebarProfileImage"))
        $("sidebarProfileImage").src =
          user.image_url || user.imageUrl || "/images/avatar-placeholder.svg";
      window.dispatchEvent(
        new CustomEvent("cp-profile-updated", { detail: user }),
      );
      window.dispatchEvent(new CustomEvent("cp-room-refresh"));
      busy = false;
      close();
    } catch (error) {
      status(error.message);
    } finally {
      busy = false;
      enable();
      renderPhotos();
    }
  };
  document.addEventListener("keydown", (event) => {
    if (!modal.classList.contains("show")) return;
    if (event.key === "Escape") close();
    if (event.key === "Tab") {
      const items = [
        ...modal.querySelectorAll(
          "button:not([disabled]),input:not([hidden]):not([disabled]),select,textarea",
        ),
      ].filter((item) => item.getClientRects().length);
      const first = items[0],
        last = items.at(-1);
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last?.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first?.focus();
      }
    }
  });
});
