(() => {
    const scopeInput    = document.getElementById('scope');
    const scopeDropdown = document.getElementById('scope-dropdown');
    const scopeOptions  = [...scopeDropdown.querySelectorAll('.scope-option')];
    const scopeClear    = document.getElementById('scope-clear');

    const setScopeClearVisible = () =>
        scopeClear.classList.toggle('hidden', !scopeInput.value);

    const filterAndShowScope = () => {
        const query = scopeInput.value.toLowerCase();
        const hasVisible = scopeOptions.some(opt => {
            const matches = opt.textContent.toLowerCase().includes(query);
            opt.classList.toggle('hidden', !matches);
            return matches;
        });
        scopeDropdown.classList.toggle('hidden', !hasVisible);
    };

    scopeInput.addEventListener('focus', filterAndShowScope);

    scopeInput.addEventListener('blur', () =>
        setTimeout(() => scopeDropdown.classList.add('hidden'), 200)
    );

    scopeInput.addEventListener('input', () => {
        filterAndShowScope();
        setScopeClearVisible();
    });

    scopeClear.addEventListener('click', () => {
        scopeInput.value = '';
        scopeInput.form.submit();
    });

    scopeOptions.forEach(opt =>
        opt.addEventListener('click', () => {
            scopeInput.value = opt.dataset.value;
            scopeDropdown.classList.add('hidden');
            setScopeClearVisible();
            scopeInput.form.submit();
        })
    );

    setScopeClearVisible();

    const appHidden   = document.getElementById('applicationName');
    const appDisplay  = document.getElementById('app-display');
    const appTrigger  = document.getElementById('app-trigger');
    const appDropdown = document.getElementById('app-dropdown');
    const appOptions  = [...appDropdown.querySelectorAll('.app-option')];

    appTrigger.addEventListener('click', e => {
        e.stopPropagation();
        appDropdown.classList.toggle('hidden');
    });

    appOptions.forEach(opt =>
        opt.addEventListener('click', () => {
            const val = opt.dataset.value;
            appHidden.value    = val;
            appDisplay.textContent = val === 'all' ? 'Toutes les applications' : val;
            appDropdown.classList.add('hidden');
            appHidden.form.submit();
        })
    );

    document.addEventListener('click', () => appDropdown.classList.add('hidden'));
})();