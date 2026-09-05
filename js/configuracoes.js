document.addEventListener('DOMContentLoaded', function () {

  // ===== ESTADO DO PERFIL (viria da API) =====
  const profile = {
    nome: 'João da Silva',
    email: 'João.silva@gmail.com',
    telefone: '(19) 998765647'
  };

  const displayNome = document.getElementById('displayNome');
  const displayEmail = document.getElementById('displayEmail');
  const displayTelefone = document.getElementById('displayTelefone');
  const sidebarUserName = document.getElementById('sidebarUserName');

  function refreshProfileDisplay() {
    displayNome.textContent = profile.nome;
    displayEmail.textContent = profile.email;
    displayTelefone.textContent = profile.telefone;
    sidebarUserName.textContent = profile.nome;
  }

  // ===== HELPERS DE MODAL =====
  function openModal(modalId) {
    document.getElementById(modalId).classList.add('is-open');
  }

  function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('is-open');
  }

  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', function () {
      closeModal(btn.dataset.closeModal);
    });
  });

  document.querySelectorAll('.modal-overlay').forEach((overlay) => {
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) {
        overlay.classList.remove('is-open');
      }
    });
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.is-open').forEach((overlay) => {
        overlay.classList.remove('is-open');
      });
    }
  });

  // ===== TOAST =====
  let toastTimeout;
  function showToast(message) {
    const toast = document.getElementById('toast');
    document.getElementById('toastText').textContent = message;
    toast.classList.add('is-visible');
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
      toast.classList.remove('is-visible');
    }, 2600);
  }

  // ===== MODAL: ALTERAR INFORMAÇÕES =====
  const inputNome = document.getElementById('inputNome');
  const inputEmail = document.getElementById('inputEmail');
  const inputTelefone = document.getElementById('inputTelefone');

  document.getElementById('btnAbrirEditarInfo').addEventListener('click', function () {
    inputNome.value = profile.nome;
    inputEmail.value = profile.email;
    inputTelefone.value = profile.telefone;
    openModal('modalEditarInfo');
  });

  document.getElementById('formEditarInfo').addEventListener('submit', function (e) {
    e.preventDefault();
    profile.nome = inputNome.value.trim() || profile.nome;
    profile.email = inputEmail.value.trim() || profile.email;
    profile.telefone = inputTelefone.value.trim() || profile.telefone;
    refreshProfileDisplay();
    closeModal('modalEditarInfo');
    showToast('Informações atualizadas com sucesso.');
  });

  // ===== MODAL: ALTERAR SENHA =====
  const inputSenhaAtual = document.getElementById('inputSenhaAtual');
  const inputNovaSenha = document.getElementById('inputNovaSenha');
  const inputConfirmaSenha = document.getElementById('inputConfirmaSenha');
  const erroSenha = document.getElementById('erroSenha');

  document.getElementById('btnAbrirAlterarSenha').addEventListener('click', function () {
    document.getElementById('formAlterarSenha').reset();
    erroSenha.classList.remove('is-visible');
    openModal('modalAlterarSenha');
  });

  document.getElementById('formAlterarSenha').addEventListener('submit', function (e) {
    e.preventDefault();

    if (inputNovaSenha.value !== inputConfirmaSenha.value) {
      erroSenha.textContent = 'As senhas não coincidem.';
      erroSenha.classList.add('is-visible');
      return;
    }

    if (inputNovaSenha.value.length < 6) {
      erroSenha.textContent = 'A nova senha deve ter pelo menos 6 caracteres.';
      erroSenha.classList.add('is-visible');
      return;
    }

    erroSenha.classList.remove('is-visible');
    closeModal('modalAlterarSenha');
    showToast('Senha alterada com sucesso.');
  });

  // Alternar visibilidade dos campos de senha
  document.querySelectorAll('.modal-toggle-password').forEach((btn) => {
    btn.addEventListener('click', function () {
      const target = document.getElementById(btn.dataset.target);
      const isPassword = target.type === 'password';
      target.type = isPassword ? 'text' : 'password';
    });
  });

  // ===== MODAL: EXCLUIR CONTA =====
  document.getElementById('btnAbrirExcluirConta').addEventListener('click', function () {
    openModal('modalExcluirConta');
  });

  document.getElementById('btnConfirmarExclusao').addEventListener('click', function () {
    // Aqui entraria a chamada para excluir a conta na API
    closeModal('modalExcluirConta');
    window.location.href = '/pages/auth/login.html';
  });

  // ===== TOGGLE DE NOTIFICAÇÕES =====
  document.getElementById('toggleAlertasEmail').addEventListener('change', function (e) {
    showToast(e.target.checked ? 'Alertas por e-mail ativados.' : 'Alertas por e-mail desativados.');
  });

  refreshProfileDisplay();
});