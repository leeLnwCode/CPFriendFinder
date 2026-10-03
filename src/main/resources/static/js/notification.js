/* =========================
   ACCEPT FRIEND REQUEST
========================= */

function acceptRequest(button) {
  const card = button.closest(".friend-request-card");

  if (!card) {
    return;
  }

  card.style.transition = "0.25s";
  card.style.opacity = "0";

  setTimeout(function () {
    card.remove();
  }, 250);
}

/* =========================
   DECLINE FRIEND REQUEST
========================= */

function declineRequest(button) {
  const card = button.closest(".friend-request-card");

  if (!card) {
    return;
  }

  card.style.transition = "0.25s";
  card.style.opacity = "0";

  setTimeout(function () {
    card.remove();
  }, 250);
}
