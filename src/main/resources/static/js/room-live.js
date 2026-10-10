document.addEventListener("DOMContentLoaded", async () => {
  if (!window.CPCall?.watchTopic) return;
  const id = new URLSearchParams(location.search).get("id"),
    room = !!document.getElementById("roomTitle"),
    home = !!document.getElementById("roomGrid");
  if (!room && !home) return;
  let stopped = false,
    timer,
    revision = -1,
    fetching = false,
    again = false;
  const emit = (name, detail) =>
    window.dispatchEvent(new CustomEvent(name, { detail }));
  const apply = (data) => {
    if (
      !data ||
      !Array.isArray(data.participants) ||
      Number(data.revision) < revision
    )
      return;
    revision = Number(data.revision);
    emit("cp-room-presence", data.participants);
  };
  async function snapshot() {
    if (!room || stopped) return;
    if (fetching) {
      again = true;
      return;
    }
    fetching = true;
    try {
      const response = await fetch(
        "/api/chats/" + encodeURIComponent(id) + "/call-presence",
        { credentials: "include", cache: "no-store" },
      );
      if (response.ok) apply(await response.json());
    } catch (_) {
    } finally {
      fetching = false;
      if (again) {
        again = false;
        snapshot();
      }
    }
  }
  function refresh() {
    clearTimeout(timer);
    timer = setTimeout(() => {
      if (stopped) return;
      emit(room ? "cp-room-refresh" : "cp-rooms-refresh");
      snapshot();
    }, 150);
  }
  const unsub = [];
  try {
    await CPCall.loadMe();
    if (stopped) return;
    if (room) {
      unsub.push(CPCall.watchTopic("/topic/rooms/" + id + "/members", refresh));
      unsub.push(
        CPCall.watchTopic("/topic/rooms/" + id + "/presence", (frame) => {
          try {
            apply(JSON.parse(frame.body));
          } catch (_) {}
        }),
      );
    } else unsub.push(CPCall.watchTopic("/topic/rooms/updates", refresh));
    CPCall.connectWS(refresh);
    snapshot();
  } catch (_) {
    refresh();
  }
  window.addEventListener("cp-ws-connected", refresh);
  window.addEventListener("focus", refresh);
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) refresh();
  });
  const fallback = setInterval(() => {
    if (!document.hidden) refresh();
  }, 30000);
  window.addEventListener(
    "pagehide",
    () => {
      stopped = true;
      clearTimeout(timer);
      clearInterval(fallback);
      unsub.forEach((fn) => fn());
    },
    { once: true },
  );
});
