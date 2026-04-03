/**
 * Client-side validation + toasts: username and password only.
 * Username: 2–10 characters, letters, numbers, underscore.
 * Password: 8–128 when required; profile change-password optional pair must match.
 */
(function (global) {
  'use strict';

  var USERNAME_PATTERN = /^[A-Za-z0-9_]{2,10}$/;
  var PW_MIN = 8;
  var PW_MAX = 128;

  function ensureToastHost() {
    var el = document.getElementById('formToastHost');
    if (el) return el;
    el = document.createElement('div');
    el.id = 'formToastHost';
    el.setAttribute('aria-live', 'polite');
    el.style.cssText = 'position:fixed;z-index:99999;left:50%;bottom:28px;transform:translateX(-50%);display:flex;flex-direction:column;align-items:center;gap:10px;pointer-events:none;max-width:min(420px,92vw);';
    document.body.appendChild(el);
    return el;
  }

  function showFormToast(message, kind) {
    kind = kind || 'error';
    var host = ensureToastHost();
    var toast = document.createElement('div');
    toast.role = 'status';
    var bg = kind === 'ok' ? 'rgba(22,101,52,.96)' : kind === 'warn' ? 'rgba(180,83,9,.96)' : 'rgba(185,28,28,.96)';
    var icon = kind === 'ok' ? '✓' : kind === 'warn' ? '⚠' : '✕';
    toast.style.cssText = 'pointer-events:auto;background:' + bg + ';color:#fff;padding:14px 20px;border-radius:12px;font-size:.88rem;font-weight:600;line-height:1.4;box-shadow:0 10px 40px rgba(0,0,0,.2);animation:formToastIn .22s ease;cursor:default;display:flex;align-items:flex-start;gap:10px;text-align:left;';
    toast.innerHTML = '<span style="flex-shrink:0;opacity:.95">' + icon + '</span><span style="flex:1">' + message + '</span>';
    if (!document.getElementById('formToastKeyframes')) {
      var s = document.createElement('style');
      s.id = 'formToastKeyframes';
      s.textContent = '@keyframes formToastIn{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}';
      document.head.appendChild(s);
    }
    host.appendChild(toast);
    var t = setTimeout(function () {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity .25s ease';
      setTimeout(function () {
        toast.remove();
      }, 260);
    }, kind === 'ok' ? 3200 : 4800);
    toast.addEventListener('click', function () {
      clearTimeout(t);
      toast.remove();
    });
  }

  function markInvalid(el, invalid) {
    if (!el) return;
    if (invalid) {
      el.classList.add('input-invalid');
      el.setAttribute('aria-invalid', 'true');
    } else {
      el.classList.remove('input-invalid');
      el.removeAttribute('aria-invalid');
    }
  }

  function validateUsername(input) {
    var v = (input.value || '').trim();
    if (!v) return 'Username is required.';
    if (v.length < 2 || v.length > 10) return 'Username must be 2–10 characters.';
    if (!USERNAME_PATTERN.test(v)) return 'Username may only use letters, numbers, and underscores.';
    return '';
  }

  function validatePasswordRequired(input) {
    var v = input.value || '';
    if (!v) return 'Password is required.';
    if (v.length < PW_MIN) return 'Password must be at least ' + PW_MIN + ' characters.';
    if (v.length > PW_MAX) return 'Password must be at most ' + PW_MAX + ' characters.';
    return '';
  }

  function validatePasswordOptional(npEl, cpEl) {
    var np = (npEl && npEl.value) || '';
    var cp = (cpEl && cpEl.value) || '';
    if (!np && !cp) return '';
    if (!np || !cp) return 'Enter both new password and confirmation, or leave both blank.';
    if (np.length < PW_MIN) return 'New password must be at least ' + PW_MIN + ' characters.';
    if (np.length > PW_MAX) return 'Password must be at most ' + PW_MAX + ' characters.';
    if (np !== cp) return 'New password and confirmation do not match.';
    return '';
  }

  function bindClearInvalid(input) {
    if (!input) return;
    input.addEventListener('input', function () {
      markInvalid(input, false);
    });
    input.addEventListener('change', function () {
      markInvalid(input, false);
    });
  }

  /**
   * @param {HTMLFormElement} form
   * @param {'register'|'adminCreate'|'profile'|'adminEdit'} mode
   */
  function attachUserFormValidation(form, mode) {
    if (!form) return;

    var un = form.querySelector('[name="username"], #un');
    var pw = form.querySelector('[name="password"]');
    var np = form.querySelector('[name="newPassword"], #np');
    var cp = form.querySelector('[name="confirmPassword"], #cp');

    [un, pw, np, cp].forEach(bindClearInvalid);

    form.addEventListener('submit', function (e) {
      var errors = [];
      var firstBad = null;

      function fail(msg, el) {
        errors.push(msg);
        if (el && !firstBad) firstBad = el;
        if (el) markInvalid(el, true);
      }

      if (un) {
        var ue = validateUsername(un);
        if (ue) fail(ue, un);
      }

      if (mode === 'register' || mode === 'adminCreate') {
        if (pw) {
          var pe = validatePasswordRequired(pw);
          if (pe) fail(pe, pw);
        }
      }

      if (mode === 'profile') {
        var poe = validatePasswordOptional(np, cp);
        if (poe) {
          fail(poe, np && !np.value ? np : cp && !cp.value ? cp : np);
        }
      }

      if (errors.length) {
        e.preventDefault();
        showFormToast(errors.join(' '), 'error');
        if (firstBad) {
          firstBad.focus();
          try {
            firstBad.scrollIntoView({ behavior: 'smooth', block: 'center' });
          } catch (ex) {}
        }
        return false;
      }

      if (mode === 'profile') {
        var npp = (np && np.value) || '';
        var cpp = (cp && cp.value) || '';
        if (npp && npp === cpp && npp.length >= PW_MIN) {
          showFormToast('Saving your profile…', 'ok');
        }
      }
    });

    if (un) {
      un.addEventListener('blur', function () {
        var v = (un.value || '').trim();
        if (v.length >= 9) showFormToast('Usernames are limited to 10 characters.', 'warn');
      });
    }
  }

  global.UserFormValidation = {
    showFormToast: showFormToast,
    attachUserFormValidation: attachUserFormValidation,
    validateUsername: validateUsername
  };
})(typeof window !== 'undefined' ? window : this);
