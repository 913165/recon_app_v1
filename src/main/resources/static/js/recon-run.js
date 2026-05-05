(function () {
  const form = document.getElementById('recon-form');
  const overlay = document.getElementById('recon-progress-overlay');
  const submitBtn = document.getElementById('recon-submit');
  const elapsedLabel = document.getElementById('recon-progress-seconds');
  let elapsedSeconds = 0;
  let elapsedTimerId = null;
  if (!form || !overlay) {
    return;
  }

  function setElapsedLabel() {
    if (elapsedLabel) {
      elapsedLabel.textContent = 'Elapsed: ' + elapsedSeconds + 's';
    }
  }

  function stopElapsedTimer() {
    if (elapsedTimerId != null) {
      window.clearInterval(elapsedTimerId);
      elapsedTimerId = null;
    }
  }

  function startElapsedTimer() {
    stopElapsedTimer();
    elapsedSeconds = 0;
    setElapsedLabel();
    elapsedTimerId = window.setInterval(function () {
      elapsedSeconds += 1;
      setElapsedLabel();
    }, 1000);
  }

  function showOverlay() {
    overlay.hidden = false;
    overlay.setAttribute('aria-hidden', 'false');
    startElapsedTimer();
    if (submitBtn) {
      submitBtn.disabled = true;
    }
  }

  function hideOverlay() {
    stopElapsedTimer();
    overlay.hidden = true;
    overlay.setAttribute('aria-hidden', 'true');
    if (submitBtn) {
      submitBtn.disabled = false;
    }
  }

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    const dateInput = form.querySelector('input[name="date"]');
    const dbInput = form.querySelector('select[name="db"]');
    const date = dateInput ? dateInput.value.trim() : '';
    const db = dbInput ? dbInput.value.trim().toUpperCase() : 'POSTGRES';
    if (!/^\d{8}$/.test(date)) {
      return;
    }

    showOverlay();

    try {
      const res = await fetch('/api/recon/run?date=' + encodeURIComponent(date) + '&db=' + encodeURIComponent(db), {
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
        let href = '/ui/results?date=' + encodeURIComponent(date) + '&completed=1&db=' + encodeURIComponent(db);
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
        window.location.href = '/ui/results?date=' + encodeURIComponent(date) + '&db=' + encodeURIComponent(db);
        return;
      }

      window.alert(msg);
    } catch (err) {
      hideOverlay();
      window.alert(err && err.message ? err.message : 'Network error');
    }
  });
})();
