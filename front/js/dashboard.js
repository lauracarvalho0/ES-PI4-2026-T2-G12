// Mostra o nome do usuario logado (guardado na sessao pelo login) no menu lateral
// de todas as telas e na saudacao "Ola, ..." quando a tela tiver #saudacaoTitulo.
document.addEventListener('DOMContentLoaded', function () {
  var usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  var nome = usuario && usuario.nome ? String(usuario.nome).trim() : '';
  if (!nome) return;

  document.querySelectorAll('.sidebar-user-name').forEach(function (el) {
    el.textContent = nome;
  });

  var saudacao = document.getElementById('saudacaoTitulo');
  if (saudacao) saudacao.textContent = 'Olá, ' + nome.split(/\s+/)[0] + '!';
});

// Abre/fecha o menu lateral no mobile (botao hamburguer + overlay)
document.addEventListener('DOMContentLoaded', function () {
  var sidebar = document.querySelector('.sidebar');
  var overlay = document.querySelector('.sidebar-overlay');
  var toggleBtn = document.querySelector('.menu-toggle');

  if (!sidebar || !overlay || !toggleBtn) return;

  function openMenu() {
    sidebar.classList.add('is-open');
    overlay.classList.add('is-open');
    toggleBtn.setAttribute('aria-expanded', 'true');
  }

  function closeMenu() {
    sidebar.classList.remove('is-open');
    overlay.classList.remove('is-open');
    toggleBtn.setAttribute('aria-expanded', 'false');
  }

  toggleBtn.addEventListener('click', function () {
    if (sidebar.classList.contains('is-open')) {
      closeMenu();
    } else {
      openMenu();
    }
  });

  overlay.addEventListener('click', closeMenu);

  // Fecha o menu ao clicar em um link (evita ficar aberto ao navegar)
  sidebar.querySelectorAll('a').forEach(function (link) {
    link.addEventListener('click', closeMenu);
  });

  // Fecha o menu automaticamente se a tela for redimensionada para desktop
  window.addEventListener('resize', function () {
    if (window.innerWidth > 800) {
      closeMenu();
    }
  });
});

// Confirmacao ao clicar em "Sair": evita que o usuario saia sem querer.
// So limpa a sessao e vai para o login se ele confirmar.
document.addEventListener('DOMContentLoaded', function () {
  var botaoSair = document.querySelector('.sidebar-logout');
  if (!botaoSair) return;

  var destino = botaoSair.getAttribute('href');

  var overlay = document.createElement('div');
  overlay.className = 'logout-overlay';
  overlay.innerHTML =
    '<div class="logout-box" role="dialog" aria-modal="true" aria-labelledby="logoutTitulo">' +
      '<h2 class="logout-title" id="logoutTitulo">Sair da conta?</h2>' +
      '<p class="logout-text">Deseja mesmo sair do AgroMonitor?</p>' +
      '<div class="logout-actions">' +
        '<button type="button" class="logout-btn-cancel">Cancelar</button>' +
        '<button type="button" class="logout-btn-confirm">Sair</button>' +
      '</div>' +
    '</div>';
  document.body.appendChild(overlay);

  var btnCancelar = overlay.querySelector('.logout-btn-cancel');
  var btnConfirmar = overlay.querySelector('.logout-btn-confirm');

  function abrir() {
    overlay.classList.add('is-open');
    btnCancelar.focus();
  }

  function fechar() {
    overlay.classList.remove('is-open');
    botaoSair.focus();
  }

  botaoSair.addEventListener('click', function (e) {
    e.preventDefault();
    abrir();
  });

  btnCancelar.addEventListener('click', fechar);

  // Clicar fora da caixa tambem cancela
  overlay.addEventListener('click', function (e) {
    if (e.target === overlay) fechar();
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && overlay.classList.contains('is-open')) fechar();
  });

  btnConfirmar.addEventListener('click', function () {
    try { sessionStorage.removeItem('usuario'); } catch (err) {}
    window.location.href = destino;
  });
});
