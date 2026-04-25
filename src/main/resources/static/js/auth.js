(() => {
  const body = document.body;
  const toggleButtons = Array.from(document.querySelectorAll("[data-mode-trigger]"));
  const formPanels = Array.from(document.querySelectorAll("[data-mode-panel]"));
  const registerForm = document.querySelector("[data-register-form]");
  const fullNameInput = document.getElementById("register-fullName");
  const firstNameInput = document.getElementById("register-firstName");
  const lastNameInput = document.getElementById("register-lastName");
  const passwordInput = document.querySelector("[data-password-strength]");
  const strengthBars = Array.from(document.querySelectorAll(".strength-meter span"));
  const modeContainer = document.querySelector("[data-mode-container]");

  const getCurrentMode = () => body.dataset.activeMode === "register" ? "register" : "login";

  const setMode = (mode, options = {}) => {
    body.dataset.activeMode = mode;

    toggleButtons.forEach((button) => {
      const active = button.dataset.modeTrigger === mode;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-selected", String(active));
    });

    formPanels.forEach((panel) => {
      const active = panel.dataset.modePanel === mode;
      panel.classList.toggle("is-active", active);
      panel.setAttribute("aria-hidden", String(!active));
    });

    updateContainerHeight();

    if (options.updateHistory) {
      const nextPath = mode === "register" ? "/register" : "/login";
      window.history.pushState({ mode }, "", nextPath);
    }
  };

  const updateContainerHeight = () => {
    if (!modeContainer) {
      return;
    }

    const activePanel = formPanels.find((panel) => panel.classList.contains("is-active"));
    if (!activePanel) {
      modeContainer.style.height = "";
      return;
    }

    modeContainer.style.height = `${activePanel.scrollHeight}px`;
  };

  const syncFullName = () => {
    if (!fullNameInput || !firstNameInput || !lastNameInput) {
      return;
    }

    fullNameInput.value = [firstNameInput.value.trim(), lastNameInput.value.trim()]
      .filter(Boolean)
      .join(" ");
  };

  const splitFullName = () => {
    if (!fullNameInput || !firstNameInput || !lastNameInput || !fullNameInput.value.trim()) {
      return;
    }

    const parts = fullNameInput.value.trim().split(/\s+/);
    firstNameInput.value = parts.shift() || "";
    lastNameInput.value = parts.join(" ");
  };

  const updateStrengthMeter = () => {
    if (!passwordInput || strengthBars.length === 0) {
      return;
    }

    const value = passwordInput.value;
    let score = 0;

    if (value.length >= 8) score += 1;
    if (/[A-Z]/.test(value)) score += 1;
    if (/[0-9]/.test(value)) score += 1;
    if (/[^A-Za-z0-9]/.test(value)) score += 1;

    const tone = score <= 1 ? "is-weak" : score <= 2 ? "is-medium" : "is-strong";

    strengthBars.forEach((bar, index) => {
      bar.className = "";
      if (index < score) {
        bar.classList.add("is-active", tone);
      }
    });
  };

  toggleButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const targetMode = button.dataset.modeTrigger;
      if (targetMode && targetMode !== getCurrentMode()) {
        setMode(targetMode, { updateHistory: true });
      }
    });
  });

  window.addEventListener("popstate", () => {
    const mode = window.location.pathname.includes("/register") ? "register" : "login";
    setMode(mode);
  });

  window.addEventListener("resize", updateContainerHeight);

  document.querySelectorAll("[data-password-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
      const input = document.getElementById(button.dataset.passwordToggle);
      if (!input) {
        return;
      }

      const showing = input.type === "text";
      input.type = showing ? "password" : "text";
      button.innerHTML = showing
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M17.94 17.94A10.94 10.94 0 0112 20C5 20 1 12 1 12a21.8 21.8 0 015.08-5.91"/><path d="M9.9 4.24A10.8 10.8 0 0112 4c7 0 11 8 11 8a22.1 22.1 0 01-2.17 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';
    });
  });

  if (firstNameInput && lastNameInput) {
    splitFullName();
    firstNameInput.addEventListener("input", syncFullName);
    lastNameInput.addEventListener("input", syncFullName);
  }

  if (registerForm) {
    registerForm.addEventListener("submit", syncFullName);
  }

  if (passwordInput) {
    passwordInput.addEventListener("input", updateStrengthMeter);
    updateStrengthMeter();
  }

  setMode(getCurrentMode());
  updateContainerHeight();
})();
