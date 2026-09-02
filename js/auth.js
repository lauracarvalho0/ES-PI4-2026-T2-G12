// Alterna a visibilidade dos campos de senha (icone de olho)
document.querySelectorAll('.toggle-password').forEach(function (btn) {
  btn.addEventListener('click', function () {
    var input = btn.previousElementSibling;
    var showing = input.type === 'text';

    input.type = showing ? 'password' : 'text';
    btn.setAttribute('aria-label', showing ? 'Mostrar senha' : 'Ocultar senha');
  });
});