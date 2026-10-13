(function () {
  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  var API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  // Alterna a visibilidade dos campos de senha (icone de olho)
  document.querySelectorAll('.toggle-password').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = btn.previousElementSibling;
      var showing = input.type === 'text';

      input.type = showing ? 'password' : 'text';
      btn.setAttribute('aria-label', showing ? 'Mostrar senha' : 'Ocultar senha');
    });
  });

  // Mostra mensagem de erro/sucesso acima do botao de enviar
  function mostrarMensagem(form, texto, ok) {
    var el = form.querySelector('.form-message');
    if (!el) {
      el = document.createElement('p');
      el.className = 'form-message';
      el.setAttribute('role', 'alert');
      el.style.cssText = 'margin:0 0 12px;font-size:14px;';
      form.querySelector('.btn-submit').before(el);
    }
    el.style.color = ok ? '#2e7d32' : '#c62828';
    el.textContent = texto;
  }

  // POST JSON para o backend. Sempre devolve um objeto {sucesso, mensagem, ...}
  function postar(caminho, dados) {
    return fetch(API + caminho, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dados)
    })
      .then(function (resp) {
        return resp.json().catch(function () {
          return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
        });
      })
      .catch(function () {
        return { sucesso: false, mensagem: 'Nao foi possivel conectar ao servidor.' };
      });
  }

  function valor(id) {
    var el = document.getElementById(id);
    return el ? el.value.trim() : '';
  }

  // ---------- LOGIN ----------
  var formLogin = document.getElementById('form-login');
  if (formLogin) {
    formLogin.addEventListener('submit', function (e) {
      e.preventDefault();
      var botao = formLogin.querySelector('.btn-submit');
      var email = valor('email');
      var senha = document.getElementById('password').value;

      if (!email || !senha) {
        mostrarMensagem(formLogin, 'Informe e-mail e senha.', false);
        return;
      }

      botao.disabled = true;
      postar('/api/login', { email: email, senha: senha }).then(function (r) {
        botao.disabled = false;
        if (r.sucesso) {
          try { sessionStorage.setItem('usuario', JSON.stringify(r.usuario)); } catch (err) {}
          window.location.href = '../dashboard/inicio.html';
        } else {
          mostrarMensagem(formLogin, r.mensagem, false);
        }
      });
    });
  }

  // ---------- CADASTRO ----------
  var formCadastro = document.getElementById('form-cadastro');
  if (formCadastro) {
    formCadastro.addEventListener('submit', function (e) {
      e.preventDefault();
      var botao = formCadastro.querySelector('.btn-submit');
      var senha = document.getElementById('senha').value;
      var confirmar = document.getElementById('confirmar-senha').value;

      if (senha !== confirmar) {
        mostrarMensagem(formCadastro, 'As senhas nao conferem.', false);
        return;
      }

      botao.disabled = true;
      postar('/api/cadastro', {
        nome: valor('nome'),
        email: valor('email'),
        cpf: valor('cpf'),
        telefone: valor('telefone'),
        senha: senha
      }).then(function (r) {
        botao.disabled = false;
        mostrarMensagem(formCadastro, r.mensagem, r.sucesso);
        if (r.sucesso) {
          setTimeout(function () { window.location.href = 'login.html'; }, 1500);
        }
      });
    });
  }
})();
