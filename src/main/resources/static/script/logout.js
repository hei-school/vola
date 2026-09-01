(function () {
  var logoutBtn = document.getElementById('logout-btn');
  var logoutDialog = document.getElementById('logout-dialog');
  var cancelBtn = document.getElementById('logout-cancel-btn');

  function openDialog() {
    if (logoutDialog) logoutDialog.showModal();
  }

  function closeDialog() {
    if (logoutDialog) logoutDialog.close();
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', openDialog);
  }

  if (cancelBtn) {
    cancelBtn.addEventListener('click', closeDialog);
  }

  if (logoutDialog) {
    logoutDialog.addEventListener('click', function (e) {
      if (e.target === logoutDialog) closeDialog();
    });
  }
})();
