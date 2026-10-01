const THEMES = ["light", "dark", "blue"];

function getSavedTheme() {
  const theme = localStorage.getItem("theme");
  return THEMES.includes(theme) ? theme : "light";
}

function applyTheme(theme) {
  document.body.classList.remove(...THEMES.map((t) => `theme-${t}`));

  if (theme !== "light") {
    document.body.classList.add(`theme-${theme}`);
  }
}

function setTheme(theme) {
  applyTheme(theme);
  localStorage.setItem("theme", theme);
}

document.addEventListener("DOMContentLoaded", () => {
  applyTheme(getSavedTheme());
});