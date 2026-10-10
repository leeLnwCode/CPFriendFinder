document.addEventListener("DOMContentLoaded", async () => {
  const $ = (id) => document.getElementById(id),
    status = $("discoveryStatus"),
    add = $("addFriendButton"),
    next = $("nextPersonButton");
  let all = [],
    candidates = [],
    user = null,
    index = 0;
  const requested = new Set(),
    seen = new Set();
  const department = (value) =>
    String(value || "")
      .trim()
      .toLocaleLowerCase();
  const sameYear = (p) =>
    user?.year != null &&
    p.year != null &&
    String(user.year) === String(p.year);
  const sameDepartment = (p) =>
    !!department(user?.department) &&
    department(user.department) === department(p.department);
  async function request(url, options = {}) {
    let response;
    try {
      response = await fetch(url, {
        credentials: "include",
        signal: AbortSignal.timeout(30000),
        ...options,
      });
    } catch (error) {
      throw new Error(
        error.name === "TimeoutError"
          ? "ค้นหาใช้เวลานาน กรุณาลองอีกครั้ง"
          : "การเชื่อมต่อขัดข้อง กรุณาตรวจอินเทอร์เน็ตแล้วลองอีกครั้ง",
      );
    }
    if (response.status === 401 || response.status === 403) {
      location.href = "/login";
      throw new Error("กรุณาเข้าสู่ระบบอีกครั้ง");
    }
    if (!response.ok) {
      let data = {};
      try {
        data = await response.json();
      } catch (_) {}
      throw new Error(
        response.status >= 500
          ? "ระบบค้นหาไม่พร้อมชั่วคราว กรุณาลองอีกครั้ง"
          : data.message || "ไม่สามารถโหลดข้อมูลได้",
      );
    }
    try {
      return await response.json();
    } catch (_) {
      throw new Error("ข้อมูลตอบกลับไม่สมบูรณ์ กรุณาลองอีกครั้ง");
    }
  }
  let pictures = [],
    pictureIndex = 0;
  function photo() {
    const image = $("randomProfileImage");
    image.onerror = () => {
      image.onerror = null;
      image.src = "/images/avatar-placeholder.svg";
    };
    image.src = pictures[pictureIndex] || "/images/avatar-placeholder.svg";
    $("randomPhotoCounter").textContent =
      `${pictureIndex + 1}/${pictures.length || 1}`;
    $("randomPhotoPrev").disabled = pictureIndex === 0;
    $("randomPhotoNext").disabled = pictureIndex >= pictures.length - 1;
    for (const [i, button] of [...$("randomPhotoThumbs").children].entries())
      button.setAttribute("aria-current", String(i === pictureIndex));
  }
  function photos(person) {
    const gallery = Array.isArray(person?.galleryPhotos)
      ? person.galleryPhotos
          .filter((value) => typeof value === "string" && value)
          .slice(0, 5)
      : [];
    pictures = gallery.length
      ? gallery
      : [person?.imageUrl || "/images/avatar-placeholder.svg"];
    pictureIndex = 0;
    $("randomPhotoControls").hidden = gallery.length < 2;
    $("randomPhotoThumbs").hidden = gallery.length < 2;
    $("randomProfileImage")
      .closest(".random-profile-image-wrapper")
      .classList.toggle("has-gallery", gallery.length > 0);
    $("randomPhotoThumbs").replaceChildren();
    gallery.forEach((url, i) => {
      const button = document.createElement("button"),
        img = document.createElement("img");
      button.type = "button";
      button.setAttribute("aria-label", `ดูรูปแนะนำตัว ${i + 1}`);
      img.src = url;
      img.alt = "";
      button.append(img);
      button.onclick = () => {
        pictureIndex = i;
        photo();
      };
      $("randomPhotoThumbs").append(button);
    });
    photo();
  }
  $("randomPhotoPrev").onclick = () => {
    if (pictureIndex > 0) {
      pictureIndex--;
      photo();
    }
  };
  $("randomPhotoNext").onclick = () => {
    if (pictureIndex < pictures.length - 1) {
      pictureIndex++;
      photo();
    }
  };
  $("editDiscoverProfile").onclick = () =>
    window.dispatchEvent(new CustomEvent("cp-edit-own-profile"));
  window.addEventListener("cp-profile-updated", () => load());
  function render() {
    const p = candidates[index];
    photos(p);
    const tags = $("randomProfileInterests");
    tags.replaceChildren();
    $("randomMatchReasons").textContent = "";
    if (!p) {
      $("randomProfileName").textContent = "ยังไม่พบเพื่อนใหม่";
      $("randomProfileYear").textContent = "";
      $("randomProfileBio").textContent =
        "ลองปิดตัวกรอง หรือให้บัญชีอื่นสมัครและเลือกความสนใจ";
      $("randomProfileImage").src = "/images/avatar-placeholder.svg";
      add.disabled = next.disabled = true;
      status.textContent =
        "ไม่มีบัญชีที่ตรงกับตัวกรอง หรือบัญชีเหล่านั้นเป็นเพื่อน/มีคำขอค้างอยู่แล้ว";
      return;
    }
    seen.add(p.userId);
    $("randomProfileName").textContent =
      `${p.firstname || ""} ${p.lastname || ""}`.trim() || "ไม่ระบุชื่อ";
    $("randomProfileYear").textContent = [
      p.year ? `ปี ${p.year}` : "",
      p.department || "",
    ]
      .filter(Boolean)
      .join(" · ");
    $("randomProfileBio").textContent = p.bio || "ยังไม่มีข้อมูลเกี่ยวกับฉัน";
    for (const interest of p.sharedInterests || []) {
      const tag = document.createElement("span");
      tag.className = "interest-tag";
      tag.textContent = interest;
      tags.append(tag);
    }
    if (!tags.children.length) tags.textContent = "ยังไม่มีความสนใจร่วมกัน";
    const score = Number(p.matchScore);
    $("randomMatchReasons").textContent = [
      `ความสนใจตรงกัน ${Number.isFinite(score) ? score : 0}%`,
      sameYear(p) ? "ชั้นปีเดียวกัน" : "",
      sameDepartment(p) ? "สาขาเดียวกัน" : "",
    ]
      .filter(Boolean)
      .join(" · ");
    add.disabled = requested.has(p.userId);
    add.textContent = add.disabled ? "ส่งคำขอแล้ว" : "เพิ่มเพื่อน";
    next.disabled = candidates.length < 2;
    status.textContent = `พบบัญชีจริง ${candidates.length} คน · สุ่มคนถัดไปเพื่อดูโปรไฟล์อื่น`;
  }
  function filter() {
    candidates = all.filter(
      (p) =>
        (!$("sameYearOnly").checked || sameYear(p)) &&
        (!$("sameDepartmentOnly").checked || sameDepartment(p)),
    );
    index = 0;
    seen.clear();
    render();
  }
  $("sameYearOnly").addEventListener("change", filter);
  $("sameDepartmentOnly").addEventListener("change", filter);
  next.addEventListener("click", () => {
    let available = candidates
      .map((p, i) => i)
      .filter((i) => i !== index && !seen.has(candidates[i].userId));
    if (!available.length) {
      seen.clear();
      available = candidates.map((p, i) => i).filter((i) => i !== index);
    }
    if (!available.length) return;
    index = available[Math.floor(Math.random() * available.length)];
    render();
  });
  add.addEventListener("click", async () => {
    const p = candidates[index];
    if (!p || requested.has(p.userId)) return;
    add.disabled = next.disabled = true;
    add.textContent = "กำลังส่งคำขอ...";
    try {
      await request("/api/friend-requests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ receiverId: p.userId }),
      });
      requested.add(p.userId);
      render();
      status.textContent = "ส่งคำขอแล้ว รอเพื่อนตอบรับก่อนเริ่มคุย";
    } catch (error) {
      render();
      status.textContent = error.message;
    }
  });
  async function load() {
    add.disabled = next.disabled = true;
    status.textContent = "กำลังค้นหาเพื่อน...";
    try {
      user = await request("/api/users/me");
      all = await request(
        `/api/v1/matching/recommendations/${encodeURIComponent(user.id || user.userId)}?limit=50`,
      );
      if (!Array.isArray(all)) throw new Error("รูปแบบข้อมูลไม่ถูกต้อง");
      filter();
    } catch (error) {
      status.replaceChildren(document.createTextNode(error.message + " "));
      const retry = document.createElement("button");
      retry.textContent = "ลองอีกครั้ง";
      retry.addEventListener("click", load, { once: true });
      status.append(retry);
    }
  }
  await load();
});
