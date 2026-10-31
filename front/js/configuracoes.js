document.addEventListener('DOMContentLoaded', function () {

  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  // ===== SESSÃO =====
  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  if (!usuario || !usuario.codigo) {
    window.location.href = '../auth/login.html';
    return;
  }

  // Chama o backend. Sempre devolve { sucesso, mensagem, ... }.
  function chamar(metodo, caminho, corpo) {
    const opcoes = { method: metodo, headers: { 'Content-Type': 'application/json' } };
    if (corpo) opcoes.body = JSON.stringify(Object.assign({ usuarioCodigo: usuario.codigo }, corpo));
    const url = API + caminho + (metodo === 'GET' ? '?usuarioCodigo=' + encodeURIComponent(usuario.codigo) : '');
    return fetch(url, opcoes)
      .then(function (resp) {
        return resp.json().catch(function () {
          return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
        });
      })
      .catch(function () {
        return { sucesso: false, mensagem: 'Não foi possível conectar ao servidor.' };
      });
  }

  // ===== ESTADO DA CONTA (vem da API) =====
  const profile = { nome: '', email: '', telefone: '' };
  const prefs = { receberAlertasPorEmail: true };

  const displayNome = document.getElementById('displayNome');
  const displayEmail = document.getElementById('displayEmail');
  const displayTelefone = document.getElementById('displayTelefone');
  const sidebarUserName = document.getElementById('sidebarUserName');
  const toggleEmail = document.getElementById('toggleAlertasEmail');

  function refreshProfileDisplay() {
    displayNome.textContent = profile.nome;
    displayEmail.textContent = profile.email;
    displayTelefone.textContent = profile.telefone || 'Não informado';
    sidebarUserName.textContent = profile.nome;
  }

  function refreshPrefs() {
    toggleEmail.checked = prefs.receberAlertasPorEmail;
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

  function mostrarErro(el, texto) {
    el.textContent = texto;
    el.classList.add('is-visible');
  }

  function limparErro(el) {
    el.classList.remove('is-visible');
  }

  // Desabilita o botão de envio enquanto a chamada está em andamento
  function comEspera(botao, promessa) {
    botao.disabled = true;
    return promessa.finally(function () { botao.disabled = false; });
  }

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

  // ===== CARREGAR DADOS =====
  chamar('GET', '/api/conta').then(function (r) {
    if (!r.sucesso) {
      showToast(r.mensagem || 'Não foi possível carregar seus dados.');
      return;
    }
    profile.nome = r.conta.nome || '';
    profile.email = r.conta.email || '';
    profile.telefone = r.conta.telefone || '';
    prefs.receberAlertasPorEmail = r.conta.receberAlertasPorEmail !== false;
    refreshProfileDisplay();
    refreshPrefs();
  });

  // ===== MODAL: ALTERAR INFORMAÇÕES =====
  const inputNome = document.getElementById('inputNome');
  const inputEmail = document.getElementById('inputEmail');
  const inputTelefone = document.getElementById('inputTelefone');
  const erroInfo = document.getElementById('erroInfo');

  document.getElementById('btnAbrirEditarInfo').addEventListener('click', function () {
    inputNome.value = profile.nome;
    inputEmail.value = profile.email;
    inputTelefone.value = profile.telefone;
    limparErro(erroInfo);
    openModal('modalEditarInfo');
  });

  document.getElementById('formEditarInfo').addEventListener('submit', function (e) {
    e.preventDefault();
    const dados = {
      nome: inputNome.value.trim(),
      email: inputEmail.value.trim(),
      telefone: inputTelefone.value.trim()
    };
    const botao = e.target.querySelector('.modal-btn-save');

    comEspera(botao, chamar('PUT', '/api/conta', dados)).then(function (r) {
      if (!r.sucesso) {
        mostrarErro(erroInfo, r.mensagem);
        return;
      }
      profile.nome = dados.nome;
      profile.email = dados.email.toLowerCase();
      profile.telefone = dados.telefone;
      // mantém o nome/e-mail da sessão em sincronia com o que foi salvo
      usuario.nome = profile.nome;
      usuario.email = profile.email;
      try { sessionStorage.setItem('usuario', JSON.stringify(usuario)); } catch (err) {}
      refreshProfileDisplay();
      closeModal('modalEditarInfo');
      showToast('Informações atualizadas com sucesso.');
    });
  });

  // ===== MODAL: ALTERAR SENHA =====
  const inputSenhaAtual = document.getElementById('inputSenhaAtual');
  const inputNovaSenha = document.getElementById('inputNovaSenha');
  const inputConfirmaSenha = document.getElementById('inputConfirmaSenha');
  const erroSenha = document.getElementById('erroSenha');

  document.getElementById('btnAbrirAlterarSenha').addEventListener('click', function () {
    document.getElementById('formAlterarSenha').reset();
    limparErro(erroSenha);
    openModal('modalAlterarSenha');
  });

  document.getElementById('formAlterarSenha').addEventListener('submit', function (e) {
    e.preventDefault();

    if (inputNovaSenha.value !== inputConfirmaSenha.value) {
      mostrarErro(erroSenha, 'As senhas não coincidem.');
      return;
    }

    if (inputNovaSenha.value.length < 6) {
      mostrarErro(erroSenha, 'A nova senha deve ter pelo menos 6 caracteres.');
      return;
    }

    limparErro(erroSenha);
    const botao = e.target.querySelector('.modal-btn-save');
    const pedido = chamar('PUT', '/api/conta/senha', {
      senhaAtual: inputSenhaAtual.value,
      novaSenha: inputNovaSenha.value
    });

    comEspera(botao, pedido).then(function (r) {
      if (!r.sucesso) {
        mostrarErro(erroSenha, r.mensagem);
        return;
      }
      document.getElementById('formAlterarSenha').reset();
      closeModal('modalAlterarSenha');
      showToast('Senha alterada com sucesso.');
    });
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
  const inputSenhaExclusao = document.getElementById('inputSenhaExclusao');
  const erroExclusao = document.getElementById('erroExclusao');

  document.getElementById('btnAbrirExcluirConta').addEventListener('click', function () {
    inputSenhaExclusao.value = '';
    inputSenhaExclusao.type = 'password';
    limparErro(erroExclusao);
    openModal('modalExcluirConta');
  });

  document.getElementById('btnConfirmarExclusao').addEventListener('click', function () {
    if (!inputSenhaExclusao.value) {
      mostrarErro(erroExclusao, 'Digite sua senha para confirmar.');
      return;
    }
    limparErro(erroExclusao);

    comEspera(this, chamar('POST', '/api/conta/excluir', { senha: inputSenhaExclusao.value })).then(function (r) {
      if (!r.sucesso) {
        mostrarErro(erroExclusao, r.mensagem);
        return;
      }
      try { sessionStorage.removeItem('usuario'); } catch (err) {}
      window.location.href = '../auth/login.html';
    });
  });

  // ===== PREFERÊNCIAS E NOTIFICAÇÕES =====
  // Salva a preferência de alertas por e-mail; se falhar, volta ao valor anterior.
  function salvarPreferencias(mensagemOk) {
    const novas = {
      receberAlertasPorEmail: toggleEmail.checked
    };
    chamar('PUT', '/api/conta/preferencias', novas).then(function (r) {
      if (!r.sucesso) {
        refreshPrefs();
        showToast(r.mensagem || 'Não foi possível salvar.');
        return;
      }
      Object.assign(prefs, novas);
      showToast(mensagemOk);
    });
  }

  toggleEmail.addEventListener('change', function () {
    salvarPreferencias(toggleEmail.checked ? 'Alertas por e-mail ativados.' : 'Alertas por e-mail desativados.');
  });

  // ===== PLANO (sincronizado com a tela de Planos) =====
  const planNames = {
    gratuito: 'Gratuito',
    essencial: 'Essencial',
    avancado: 'Avançado'
  };

  function refreshPlanoAtualLabel() {
    const planoId = localStorage.getItem('agromonitor_plano') || 'gratuito';
    document.getElementById('planoAtualLabel').textContent = planNames[planoId] || 'Gratuito';
  }

  refreshPlanoAtualLabel();
});
