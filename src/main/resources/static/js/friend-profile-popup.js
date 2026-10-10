document.addEventListener("DOMContentLoaded", () => {
  const overlay = document.getElementById("friendProfileOverlay");
  if (!overlay) return;
  const $ = (id) => document.getElementById(id),
    button = $("friendProfileAction");
  let sequence = 0,
    currentId = null,
    status = "loading",
    requestId = null,
    trigger = null;
  function close() {
    sequence++;
    overlay.classList.remove("show");
    overlay.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
  }
  function action() {
    button.classList.toggle("is-pending", status === "pending");
    button.classList.toggle("is-friend", status === "friend");
    button.disabled = ["loading", "error", "pending"].includes(status);
    const labels = {
      self: ["✎", "แก้ไขข้อมูลของฉัน"],
      friend: ["💬", "ส่งข้อความ"],
      pending: ["✓", "ส่งคำขอแล้ว"],
      incoming: ["✓", "ยอมรับคำขอเป็นเพื่อน"],
      loading: ["", "กำลังโหลด…"],
      error: ["", "โหลดไม่สำเร็จ กรุณาเปิดโปรไฟล์อีกครั้ง"],
      none: ["👤+", "เพิ่มเพื่อน"],
    };
    const label = labels[status] || labels.none;
    $("friendProfileActionIcon").textContent = label[0];
    $("friendProfileActionText").textContent = label[1];
  }
  function interests(values) {
    const node = $("friendProfileInterests");
    node.replaceChildren();
    for (const value of values.length ? values : ["ยังไม่มีข้อมูล"]) {
      const tag = document.createElement("span");
      tag.className = "friend-profile-interest";
      tag.textContent = typeof value === "string" ? value : value.name;
      node.append(tag);
    }
  }
  async function open(element) {
    const seq = ++sequence;
    trigger = element;
    currentId = element.dataset.id || "";
    requestId = null;
    status = "loading";
    action();
    $("friendProfileName").textContent = element.dataset.name || "โปรไฟล์";
    $("friendProfileYear").textContent = "";
    $("friendProfileImage").src =
      element.dataset.image || "/images/avatar-placeholder.svg";
    $("friendProfileBio").textContent = "กำลังโหลดข้อมูล…";
    interests([]);
    overlay.classList.add("show");
    overlay.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    try {
      if (!currentId) throw new Error("Missing profile ID");
      const response = await fetch(
        `/api/users/${encodeURIComponent(currentId)}/profile`,
        { credentials: "include", cache: "no-store" },
      );
      if (!response.ok) throw new Error("Profile unavailable");
      const p = await response.json();
      if (seq !== sequence) return;
      $("friendProfileName").textContent =
        [p.firstname, p.lastname].filter(Boolean).join(" ") || "ไม่ระบุชื่อ";
      $("friendProfileImage").src =
        p.imageUrl || "/images/avatar-placeholder.svg";
      $("friendProfileImage").alt = $("friendProfileName").textContent;
      $("friendProfileYear").textContent = [
        p.year ? "ปี " + p.year : "",
        p.department || "",
      ]
        .filter(Boolean)
        .join(" ");
      $("friendProfileBio").textContent = p.bio || "ยังไม่มีข้อมูลเกี่ยวกับฉัน";
      interests(p.interests || []);
      status = p.friendStatus || "none";
      requestId = p.requestId;
      action();
    } catch (error) {
      if (seq === sequence) {
        status = "error";
        $("friendProfileBio").textContent = "ไม่สามารถโหลดข้อมูลโปรไฟล์ได้";
        action();
      }
    }
  }
  document.addEventListener("click", (e) => {
    const p = e.target.closest("[data-profile]");
    if (p) open(p);
  });
  $("friendProfileClose").addEventListener("click", close);
  overlay.addEventListener("click", (e) => {
    if (e.target === overlay) close();
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") close();
  });
  button.addEventListener("click", async () => {
    if (status === "self") {
      close();
      window.dispatchEvent(new CustomEvent("cp-edit-own-profile"));
      return;
    }
    if (status === "friend") {
      location.href = `/friend?friendId=${encodeURIComponent(currentId)}`;
      return;
    }
    if (!["none", "incoming"].includes(status)) return;
    const seq = sequence,
      id = currentId,
      incoming = status === "incoming";
    button.disabled = true;
    try {
      const response = await fetch(
        incoming
          ? `/api/friend-requests/${encodeURIComponent(requestId)}/accept`
          : "/api/friend-requests",
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          ...(incoming ? {} : { body: JSON.stringify({ receiverId: id }) }),
        },
      );
      if (!response.ok)
        throw new Error(
          "ไม่สามารถดำเนินการได้ กรุณาเปิดโปรไฟล์เพื่ออัปเดตสถานะอีกครั้ง",
        );
      if (seq !== sequence) return;
      status = incoming ? "friend" : "pending";
      action();
      window.dispatchEvent(new CustomEvent("cp-friends-updated"));
    } catch (error) {
      if (seq === sequence) {
        alert(error.message);
        if (trigger) await open(trigger);
      }
    }
  });
});
