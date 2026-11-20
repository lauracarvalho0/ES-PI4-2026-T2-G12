// Camila Fernandes Costacurta
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

  // Verifica se o Caps Lock esta ativo no campo de senha do login.
  var campoSenhaLogin = document.querySelector('#form-login #password');
  var avisoCapsLock = document.getElementById('caps-lock-aviso');
  if (campoSenhaLogin && avisoCapsLock) {
    // Atualiza o aviso conforme o estado do Caps Lock.
    function atualizarAvisoCapsLock(evento) {
      // Confere se o navegador informa o estado da tecla.
      if (typeof evento.getModifierState === 'function') {
        // Mostra ou esconde o aviso.
        avisoCapsLock.hidden = !evento.getModifierState('CapsLock');
      }
    }

    // Verifica o Caps Lock quando uma tecla for pressionada.
    campoSenhaLogin.addEventListener('keydown', atualizarAvisoCapsLock);
    // Verifica o Caps Lock quando uma tecla for solta.
    campoSenhaLogin.addEventListener('keyup', atualizarAvisoCapsLock);
    // Verifica o Caps Lock ao clicar no campo.
    campoSenhaLogin.addEventListener('mousedown', atualizarAvisoCapsLock);
    // Esconde o aviso ao sair do campo.
    campoSenhaLogin.addEventListener('blur', function () {
      avisoCapsLock.hidden = true;
    });
  }

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
    // Guarda os campos de senha do cadastro.
    var senhaCadastro = document.getElementById('senha');
    var confirmacaoCadastro = document.getElementById('confirmar-senha');
    // Guarda o aviso exibido quando as senhas forem diferentes.
    var avisoSenhas = document.getElementById('senhas-aviso');

    // Mostra ou esconde o aviso de senhas diferentes.
    function mostrarAvisoSenhas(mostrar) {
      avisoSenhas.hidden = !mostrar;
      confirmacaoCadastro.setAttribute('aria-invalid', String(mostrar));
    }

    // Esconde o aviso ao editar a senha.
    senhaCadastro.addEventListener('input', function () { mostrarAvisoSenhas(false); });
    // Esconde o aviso ao editar a confirmacao da senha.
    confirmacaoCadastro.addEventListener('input', function () { mostrarAvisoSenhas(false); });

    formCadastro.addEventListener('submit', function (e) {
      e.preventDefault();
      var botao = formCadastro.querySelector('.btn-submit');
      var senha = senhaCadastro.value;
      var confirmar = confirmacaoCadastro.value;

      // Impede o cadastro quando as senhas forem diferentes.
      if (senha !== confirmar) {
        mostrarAvisoSenhas(true);
        confirmacaoCadastro.focus();
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
  // ---------- RECUPERAR SENHA (pede o link por e-mail) ----------
  var formRecuperar = document.getElementById('form-recuperar');
  if (formRecuperar) {
    var botaoEnviar = formRecuperar.querySelector('.btn-submit');
    var botaoReenviar = document.getElementById('btn-reenviar');
    var contagem = null;
    var enviando = false;

    // O servidor nao reenvia em menos de 60 s; o botao espera o mesmo tempo
    function iniciarContagem(segundos) {
      if (!botaoReenviar) return;
      clearInterval(contagem);
      var restante = segundos;
      botaoReenviar.disabled = true;
      botaoReenviar.textContent = 'Reenviar link (' + restante + 's)';
      contagem = setInterval(function () {
        restante--;
        if (restante <= 0) {
          clearInterval(contagem);
          botaoReenviar.disabled = false;
          botaoReenviar.textContent = 'Reenviar link';
        } else {
          botaoReenviar.textContent = 'Reenviar link (' + restante + 's)';
        }
      }, 1000);
    }

    function enviarLink() {
      if (enviando) return;
      var email = valor('email');
      if (!email) {
        mostrarMensagem(formRecuperar, 'Informe o seu e-mail.', false);
        return;
      }

      enviando = true;
      botaoEnviar.disabled = true;
      // urlBase: o link do e-mail volta para este mesmo site (o servidor so aceita enderecos permitidos)
      postar('/api/recuperar-senha', { email: email, urlBase: window.location.origin }).then(function (r) {
        enviando = false;
        botaoEnviar.disabled = false;
        mostrarMensagem(formRecuperar, r.mensagem, r.sucesso);
        if (r.sucesso) iniciarContagem(60);
      });
    }

    formRecuperar.addEventListener('submit', function (e) {
      e.preventDefault();
      enviarLink();
    });
    if (botaoReenviar) botaoReenviar.addEventListener('click', enviarLink);
  }

  // ---------- REDEFINIR SENHA (tela aberta pelo link do e-mail) ----------
  var formRedefinir = document.getElementById('form-redefinir');
  if (formRedefinir) {
    var tokenDoLink = new URLSearchParams(window.location.search).get('token') || '';
    var botaoRedefinir = formRedefinir.querySelector('.btn-submit');

    if (!tokenDoLink) {
      mostrarMensagem(formRedefinir, 'Link inválido. Peça um novo link de recuperação.', false);
      botaoRedefinir.disabled = true;
    }

    formRedefinir.addEventListener('submit', function (e) {
      e.preventDefault();
      if (!tokenDoLink) return;

      var senha = document.getElementById('senha').value;
      var confirmar = document.getElementById('confirmar-senha').value;
      if (senha !== confirmar) {
        mostrarMensagem(formRedefinir, 'As senhas não conferem.', false);
        return;
      }

      botaoRedefinir.disabled = true;
      postar('/api/redefinir-senha', { token: tokenDoLink, senha: senha }).then(function (r) {
        mostrarMensagem(formRedefinir, r.mensagem, r.sucesso);
        if (r.sucesso) {
          // senha trocada: o link nao serve mais; segue para o login
          setTimeout(function () { window.location.href = 'login.html'; }, 2000);
        } else {
          botaoRedefinir.disabled = false;
        }
      });
    });
  }
})();


})();

