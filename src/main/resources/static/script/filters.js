(function () {
  const scopeInput = document.getElementById('scope');
  const scopeDropdown = document.getElementById('scope-dropdown');
  const scopeOptions = [...scopeDropdown.querySelectorAll('.scope-option')];
  const scopeClear = document.getElementById('scope-clear');

  function setScopeClearVisible() {
    scopeClear.classList.toggle('hidden', !scopeInput.value);
  }

  function filterAndShowScope() {
    const query = scopeInput.value.toLowerCase();
    const hasVisible = scopeOptions.some(function (opt) {
      const matches = opt.textContent.toLowerCase().includes(query);
      opt.classList.toggle('hidden', !matches);
      return matches;
    });
    scopeDropdown.classList.toggle('hidden', !hasVisible);
  }

  scopeInput.addEventListener('focus', filterAndShowScope);
  scopeDropdown.addEventListener('mousedown', function (e) { e.preventDefault(); });
  scopeInput.addEventListener('blur', function () {
    scopeDropdown.classList.add('hidden');
  });
  scopeInput.addEventListener('input', function () {
    filterAndShowScope();
    setScopeClearVisible();
  });
  scopeClear.addEventListener('click', function () {
    scopeInput.value = '';
    scopeInput.form.submit();
  });
  scopeOptions.forEach(function (opt) {
    opt.addEventListener('click', function () {
      scopeInput.value = opt.dataset.value;
      scopeDropdown.classList.add('hidden');
      setScopeClearVisible();
      scopeInput.form.submit();
    });
  });
  setScopeClearVisible();

  const appHidden = document.getElementById('applicationName');
  const appDisplay = document.getElementById('app-display');
  const appTrigger = document.getElementById('app-trigger');
  const appDropdown = document.getElementById('app-dropdown');
  const appOptions = [...appDropdown.querySelectorAll('.app-option')];

  appTrigger.addEventListener('click', function (e) {
    e.stopPropagation();
    appDropdown.classList.toggle('hidden');
  });
  appOptions.forEach(function (opt) {
    opt.addEventListener('click', function () {
      const val = opt.dataset.value;
      appHidden.value = val;
      appDisplay.textContent = val === 'all' ? 'Toutes les applications' : val;
      appDropdown.classList.add('hidden');
      appHidden.form.submit();
    });
  });
  document.addEventListener('click', function () {
    appDropdown.classList.add('hidden');
  });
})();
