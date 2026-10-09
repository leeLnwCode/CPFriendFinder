document.addEventListener("DOMContentLoaded", async () => {
  if (window.location.pathname === "/friend") return;
  if (typeof CPCall === "undefined") return;

  const overlay = document.getElementById("globalCallOverlay");
  const callerName = document.getElementById("globalCallName");
  const callerImage = document.getElementById("globalCallImage");
  const status = document.getElementById("globalCallStatus");
  const incomingActions = document.getElementById("globalIncomingActions");
  const acceptButton = document.getElementById("globalCallAccept");
  const declineButton = document.getElementById("globalCallDecline");
  const endButton = document.getElementById("globalCallEnd");
  const videoArea = document.getElementById("globalCallVideoArea");

  let incomingSignal = null;
  let currentMode = null;

  function closeCallUI() {
    overlay?.classList.remove("show");
    videoArea?.classList.remove("show");

    if (incomingActions) incomingActions.style.display = "flex";
    if (endButton) endButton.style.display = "none";

    incomingSignal = null;
    currentMode = null;
  }

  function showIncomingCall(signal) {
    incomingSignal = signal;
    currentMode = signal.mode === "VIDEO" ? "VIDEO" : "VOICE";

    if (callerName) callerName.textContent = signal.fromName || "เพื่อน";
    if (callerImage)
      callerImage.src = signal.fromImage || "/images/avatar-placeholder.svg";

    if (status) {
      status.textContent =
        currentMode === "VIDEO" ? "สายวิดีโอเข้า" : "สายเรียกเข้า";
    }

    videoArea?.classList.remove("show");
    overlay?.classList.add("show");
  }

  window.addEventListener("cp-call-signal", (event) => {
    const signal = event.detail;
    if (!signal) return;

    if (signal.type === "INVITE") {
      if (CPCall.isActive?.() || incomingSignal) return;
      showIncomingCall(signal);
      return;
    }

    if (signal.type === "CANCEL") {
      sessionStorage.removeItem("cp-pending-call");
      if (
        incomingSignal &&
        String(signal.fromUserId) === String(incomingSignal.fromUserId)
      ) {
        closeCallUI();
      }
    }
  });

  acceptButton?.addEventListener("click", () => {
    if (!incomingSignal) return;

    acceptButton.disabled = true;
    declineButton.disabled = true;
    sessionStorage.setItem("cp-pending-call", JSON.stringify(incomingSignal));

    window.location.href = "/friend";
  });

  declineButton?.addEventListener("click", async () => {
    if (!incomingSignal) return;

    const signal = incomingSignal;

    try {
      await CPCall.declineFriendCall(signal);
    } catch (error) {
      console.error("ปฏิเสธสายไม่สำเร็จ:", error);
    }

    closeCallUI();
  });

  endButton?.addEventListener("click", () => {
    CPCall.leaveCall();
    closeCallUI();
  });

  window.addEventListener("cp-call-connected", () => {
    if (status) status.textContent = "กำลังสนทนา";
  });

  window.addEventListener("cp-call-ended", () => {
    CPCall.leaveCall(false);
    closeCallUI();
  });

  try {
    await CPCall.loadMe();
    CPCall.connectWS();
  } catch (error) {
    console.error("เริ่มระบบรับสายไม่สำเร็จ:", error);
  }
});
