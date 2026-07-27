(function () {
  var scopeInput = document.getElementById('scope');
  var scopeDropdown = document.getElementById('scope-dropdown');
  var scopeOptions = [...scopeDropdown.querySelectorAll('.scope-option')];
  var scopeClear = document.getElementById('scope-clear');

  function setScopeClearVisible() {
    scopeClear.classList.toggle('hidden', !scopeInput.value);
  }

  function filterAndShowScope() {
    var query = scopeInput.value.toLowerCase();
    var hasVisible = scopeOptions.some(function (opt) {
      var matches = opt.textContent.toLowerCase().includes(query);
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

  var appHidden = document.getElementById('applicationName');
  var appDisplay = document.getElementById('app-display');
  var appTrigger = document.getElementById('app-trigger');
  var appDropdown = document.getElementById('app-dropdown');
  var appOptions = [...appDropdown.querySelectorAll('.app-option')];

  appTrigger.addEventListener('click', function (e) {
    e.stopPropagation();
    appDropdown.classList.toggle('hidden');
  });
  appOptions.forEach(function (opt) {
    opt.addEventListener('click', function () {
      var val = opt.dataset.value;
      appHidden.value = val === 'all' ? '' : val;
      appDisplay.textContent = val === 'all' ? 'Toutes les applications' : val;
      appDropdown.classList.add('hidden');
      appHidden.form.submit();
    });
  });
  document.addEventListener('click', function () {
    appDropdown.classList.add('hidden');
  });
})();
