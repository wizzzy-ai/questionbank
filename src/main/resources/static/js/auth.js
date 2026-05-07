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
  const submitButton = document.querySelector("[data-register-form] .primary-button");
  const termsCheckbox = document.querySelector("[data-register-form] input[type='checkbox']");

  // Form validation state
  let validationErrors = {};
  let isSubmitting = false;

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
    const requirements = {
      length: value.length >= 8,
      uppercase: /[A-Z]/.test(value),
      number: /[0-9]/.test(value),
      special: /[^A-Za-z0-9]/.test(value)
    };

    Object.values(requirements).forEach(met => {
      if (met) score += 1;
    });

    const tone = score <= 1 ? "is-weak" : score <= 2 ? "is-medium" : "is-strong";

    strengthBars.forEach((bar, index) => {
      bar.className = "";
      if (index < score) {
        bar.classList.add("is-active", tone);
      }
    });

    // Update password requirements tooltip if exists
    updatePasswordRequirements(requirements);
  };

  const updatePasswordRequirements = (requirements) => {
    let tooltip = document.getElementById('password-requirements');
    if (!tooltip && passwordInput && document.activeElement === passwordInput) {
      tooltip = createPasswordRequirementsTooltip();
    }
    if (tooltip) {
      updateRequirementCheckboxes(tooltip, requirements);
    }
  };

  const createPasswordRequirementsTooltip = () => {
    const tooltip = document.createElement('div');
    tooltip.id = 'password-requirements';
    tooltip.className = 'password-requirements-tooltip';
    tooltip.innerHTML = `
      <div class="requirement" data-requirement="length">
        <span class="checkmark">✓</span>
        <span>At least 8 characters</span>
      </div>
      <div class="requirement" data-requirement="uppercase">
        <span class="checkmark">✓</span>
        <span>At least 1 uppercase letter</span>
      </div>
      <div class="requirement" data-requirement="number">
        <span class="checkmark">✓</span>
        <span>At least 1 number</span>
      </div>
      <div class="requirement" data-requirement="special">
        <span class="checkmark">✓</span>
        <span>At least 1 special character</span>
      </div>
    `;
    
    const passwordField = passwordInput.closest('.field-group');
    passwordField.appendChild(tooltip);
    return tooltip;
  };

  const updateRequirementCheckboxes = (tooltip, requirements) => {
    Object.entries(requirements).forEach(([key, met]) => {
      const requirement = tooltip.querySelector(`[data-requirement="${key}"]`);
      if (requirement) {
        requirement.classList.toggle('met', met);
      }
    });
  };

  const validateForm = () => {
    const errors = {};
    
    // Validate first name
    const firstName = document.getElementById('register-firstName').value.trim();
    if (!firstName) {
      errors.firstName = 'This field is required';
    }
    
    // Validate last name
    const lastName = document.getElementById('register-lastName').value.trim();
    if (!lastName) {
      errors.lastName = 'This field is required';
    }
    
    // Validate email
    const email = document.getElementById('register-email').value.trim();
    if (!email) {
      errors.email = 'This field is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Please enter a valid email address';
    }
    
    // Validate password
    const password = passwordInput ? passwordInput.value : '';
    if (!password) {
      errors.password = 'This field is required';
    } else {
      const requirements = {
        length: password.length >= 8,
        uppercase: /[A-Z]/.test(password),
        number: /[0-9]/.test(password),
        special: /[^A-Za-z0-9]/.test(password)
      };
      const metRequirements = Object.values(requirements).filter(Boolean).length;
      if (metRequirements < 4) {
        errors.password = 'Password does not meet all requirements';
      }
    }
    
    // Validate confirm password
    const confirmPassword = document.getElementById('register-confirmPassword').value;
    if (!confirmPassword) {
      errors.confirmPassword = 'This field is required';
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }
    
    // Validate terms checkbox
    if (!termsCheckbox || !termsCheckbox.checked) {
      errors.terms = 'You must agree to the Terms and Privacy Policy to continue';
    }
    
    return errors;
  };

  const showFieldErrors = (errors) => {
    // Clear all previous errors
    document.querySelectorAll('.field-error.client-error').forEach(el => el.remove());
    document.querySelectorAll('.field-shell.has-error').forEach(el => el.classList.remove('has-error'));
    document.querySelectorAll('[aria-invalid]').forEach(el => el.removeAttribute('aria-invalid'));
    
    // Show new errors
    Object.entries(errors).forEach(([field, message]) => {
      let fieldElement;
      let errorContainer;
      
      switch(field) {
        case 'firstName':
          fieldElement = document.getElementById('register-firstName');
          break;
        case 'lastName':
          fieldElement = document.getElementById('register-lastName');
          break;
        case 'email':
          fieldElement = document.getElementById('register-email');
          break;
        case 'password':
          fieldElement = document.getElementById('register-password');
          break;
        case 'confirmPassword':
          fieldElement = document.getElementById('register-confirmPassword');
          break;
        case 'terms':
          fieldElement = termsCheckbox;
          break;
      }
      
      if (fieldElement) {
        const fieldShell = fieldElement.closest('.field-group') || fieldElement.closest('.check-row');
        fieldShell.classList.add('has-error');
        fieldElement.setAttribute('aria-invalid', 'true');
        fieldElement.setAttribute('aria-describedby', `${field}-error`);
        
        // Create error message
        const errorDiv = document.createElement('p');
        errorDiv.className = 'field-error client-error';
        errorDiv.id = `${field}-error`;
        errorDiv.textContent = message;
        
        if (field === 'terms') {
          fieldShell.appendChild(errorDiv);
        } else {
          fieldShell.appendChild(errorDiv);
        }
      }
    });
  };

  const updateSubmitButton = () => {
    if (!submitButton) return;
    
    const errors = validateForm();
    const hasErrors = Object.keys(errors).length > 0;
    
    submitButton.disabled = hasErrors || isSubmitting;
    submitButton.style.opacity = hasErrors ? '0.6' : '1';
    submitButton.style.cursor = hasErrors ? 'not-allowed' : 'pointer';
  };

  const setLoadingState = (loading) => {
    isSubmitting = loading;
    if (!submitButton) return;
    
    if (loading) {
      submitButton.disabled = true;
      submitButton.innerHTML = `
        <span class="spinner" style="display:inline-block;width:16px;height:16px;margin-right:8px;border:2px solid transparent;border-top:2px solid currentColor;border-radius:50%;animation:spin 1s linear infinite;"></span>
        Creating account...
      `;
    } else {
      updateSubmitButton();
      submitButton.textContent = 'Create My Account';
    }
  };

  const triggerShakeAnimation = (fieldId) => {
    const fieldElement = document.getElementById(fieldId) || 
                        document.querySelector(`[data-field="${fieldId}"]`) ||
                        termsCheckbox;
    
    if (fieldElement) {
      fieldElement.classList.add('shake');
      setTimeout(() => fieldElement.classList.remove('shake'), 300);
    }
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    
    if (isSubmitting) return;
    
    const errors = validateForm();
    
    if (Object.keys(errors).length > 0) {
      validationErrors = errors;
      showFieldErrors(errors);
      
      // Scroll to first error
      const firstErrorField = document.querySelector('[aria-invalid="true"]');
      firstErrorField?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      
      // Special handling for Terms checkbox
      if (errors.terms) {
        triggerShakeAnimation('terms');
      }
      return;
    }
    
    setLoadingState(true);
    
    // Save form draft to sessionStorage
    saveFormDraft();
    
    // Submit the form
    e.target.submit();
  };

  const saveFormDraft = () => {
    const draft = {
      firstName: document.getElementById('register-firstName').value,
      lastName: document.getElementById('register-lastName').value,
      email: document.getElementById('register-email').value,
      timestamp: Date.now()
    };
    sessionStorage.setItem('registrationDraft', JSON.stringify(draft));
  };

  const loadFormDraft = () => {
    const draft = sessionStorage.getItem('registrationDraft');
    if (draft) {
      try {
        const data = JSON.parse(draft);
        const hourAgo = Date.now() - (60 * 60 * 1000);
        
        // Only restore if less than an hour old
        if (data.timestamp > hourAgo) {
          document.getElementById('register-firstName').value = data.firstName || '';
          document.getElementById('register-lastName').value = data.lastName || '';
          document.getElementById('register-email').value = data.email || '';
          syncFullName();
        }
      } catch (e) {
        // Invalid draft, remove it
        sessionStorage.removeItem('registrationDraft');
      }
    }
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
    registerForm.addEventListener("submit", handleFormSubmit);
  }

  // Add input event listeners for real-time validation
  const formInputs = ['register-firstName', 'register-lastName', 'register-email', 'register-password', 'register-confirmPassword'];
  formInputs.forEach(id => {
    const input = document.getElementById(id);
    if (input) {
      input.addEventListener('input', updateSubmitButton);
      input.addEventListener('blur', updateSubmitButton);
    }
  });

  if (termsCheckbox) {
    termsCheckbox.addEventListener('change', updateSubmitButton);
  }

  // Handle remember me checkbox visual feedback
  const rememberMeCheckbox = document.querySelector("form[action*='/login'] input[type='checkbox']");
  if (rememberMeCheckbox) {
    rememberMeCheckbox.addEventListener('change', function() {
      // Force visual update by triggering reflow and CSS update
      const checkRow = this.closest('.check-row');
      const checkBox = checkRow?.querySelector('.check-row__box');
      
      if (checkRow && checkBox) {
        // Force reflow to ensure CSS updates
        checkRow.style.transform = 'scale(0.98)';
        checkBox.style.transform = 'scale(0.98)';
        
        setTimeout(() => {
          checkRow.style.transform = 'scale(1)';
          checkBox.style.transform = 'scale(1)';
        }, 100);
        
        // Add/remove checked class for additional styling
        if (this.checked) {
          checkRow.classList.add('is-checked');
        } else {
          checkRow.classList.remove('is-checked');
        }
      }
    });
  }

  // Also handle click on the check-row label for better UX
  document.querySelectorAll('.check-row').forEach(checkRow => {
    checkRow.addEventListener('click', function(e) {
      const checkbox = this.querySelector('input[type="checkbox"]');
      if (checkbox && e.target !== checkbox) {
        checkbox.checked = !checkbox.checked;
        // Trigger change event for any listeners
        checkbox.dispatchEvent(new Event('change'));
        e.preventDefault(); // Prevent double-clicking
      }
    });
    
    // Initialize checked state styling
    const checkbox = checkRow.querySelector('input[type="checkbox"]');
    if (checkbox && checkbox.checked) {
      checkRow.classList.add('is-checked');
    }
  });

  // Password field focus/blur events for requirements tooltip
  if (passwordInput) {
    passwordInput.addEventListener('focus', () => {
      const requirements = {
        length: passwordInput.value.length >= 8,
        uppercase: /[A-Z]/.test(passwordInput.value),
        number: /[0-9]/.test(passwordInput.value),
        special: /[^A-Za-z0-9]/.test(passwordInput.value)
      };
      createPasswordRequirementsTooltip();
      updatePasswordRequirements(requirements);
    });

    passwordInput.addEventListener('blur', () => {
      // Hide tooltip after a short delay to allow click events
      setTimeout(() => {
        const tooltip = document.getElementById('password-requirements');
        if (tooltip && document.activeElement !== passwordInput) {
          tooltip.remove();
        }
      }, 200);
    });
  }

  // Load form draft on page load
  if (getCurrentMode() === 'register') {
    loadFormDraft();
  }

  // Initialize submit button state
  updateSubmitButton();

  if (passwordInput) {
    passwordInput.addEventListener("input", updateStrengthMeter);
    updateStrengthMeter();
  }

  setMode(getCurrentMode());
  updateContainerHeight();
})();
