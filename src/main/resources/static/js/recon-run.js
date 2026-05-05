(function () {
  const form = document.getElementById('recon-form');
  const overlay = document.getElementById('recon-progress-overlay');
  const submitBtn = document.getElementById('recon-submit');
  if (!form || !overlay) {
    return;
  }

  function showOverlay() {
    overlay.hidden = false;
    overlay.setAttribute('aria-hidden', 'false');
    if (submitBtn) {
      submitBtn.disabled = true;
    }
  }

  function hideOverlay() {
    overlay.hidden = true;
    overlay.setAttribute('aria-hidden', 'true');
    if (submitBtn) {
      submitBtn.disabled = false;
    }
  }

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    const dateInput = form.querySelector('input[name="date"]');
    const date = dateInput ? dateInput.value.trim() : '';
    if (!/^\d{8}$/.test(date)) {
      return;
    }

    showOverlay();

    try {
      const res = await fetch('/api/recon/run?date=' + encodeURIComponent(date), {
        method: 'POST',
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
      });

      let body = {};
      try {
        body = await res.json();
      } catch (_) {
        /* empty */
      }

      if (res.ok) {
        let href = '/ui/results?date=' + encodeURIComponent(date) + '&completed=1';
        if (body.reconciliationMillis != null) {
          href += '&reconMs=' + encodeURIComponent(String(body.reconciliationMillis));
        }
        if (body.durationMillis != null) {
          href += '&totalMs=' + encodeURIComponent(String(body.durationMillis));
        }
        window.location.href = href;
        return;
      }

      hideOverlay();

      const msg = typeof body.error === 'string' ? body.error : 'Request failed (' + res.status + ')';

      if (res.status === 409) {
        window.alert(msg);
        window.location.href = '/ui/results?date=' + encodeURIComponent(date);
        return;
      }

      window.alert(msg);
    } catch (err) {
      hideOverlay();
      window.alert(err && err.message ? err.message : 'Network error');
    }
  });
})();
