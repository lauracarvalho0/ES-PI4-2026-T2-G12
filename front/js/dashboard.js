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