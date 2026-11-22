// Laura Carvalho - 25014543

// só roda depois que o HTML inteiro carregou
document.addEventListener('DOMContentLoaded', function () {
  const totalSteps = 2;   // o formulário tem 2 etapas: preencher e revisar
  let currentStep = 1;    // etapa em que o usuário está agora

  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const formMessage = document.getElementById('formMessage');
  const plantioSelect = document.getElementById('plantio');
  const atividadeSelect = document.getElementById('atividade');
  const dataInput = document.getElementById('dataExecucao');

  // mostra mensagem de erro (vermelho) ou sucesso (verde); texto vazio esconde a mensagem
  function showMessage(text, ok) {
    formMessage.style.color = ok ? '#2e7d32' : '#c62828';
    formMessage.textContent = text;
    formMessage.style.display = text ? 'block' : 'none';
  }

  // O formulario mostra textos amigaveis; o servidor espera os codigos do dicionario de dados
  const tipoCodigos = {
    'Irrigação': 'IRRIGACAO',
    'Adubação de cobertura': 'ADUBACAO',
    'Aplicação de defensivo': 'TRATAMENTO',
    'Monitoramento': 'MONITORAMENTO',
    'Tratos culturais': 'TRATAMENTO'
  };
  const statusCodigos = {
    'Concluída': 'CONCLUIDA',
    'Pendente': 'PENDENTE',
    'Planejada': 'PENDENTE', // "Planejada" e "Pendente" são o mesmo status pro servidor
    'Atrasada': 'ATRASADA'
  };

  // usuário logado, salvo no sessionStorage pelo login (try/catch protege de JSON quebrado)
  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  // Carrega os plantios do usuario para o select
  function carregarPlantios() {
    // sem usuário logado: avisa e manda pro login depois de 1,5 s
    if (!usuario || !usuario.codigo) {
      plantioSelect.innerHTML = '<option value="">Sessão expirada</option>';
      showMessage('Sua sessão expirou. Entre novamente.', false);
      setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
      return;
    }

    fetch(API + '/api/plantios?usuarioCodigo=' + encodeURIComponent(usuario.codigo))
      .then(function (resp) {
        // se a resposta não for JSON, cria um erro no mesmo formato
        return resp.json().catch(function () {
          return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
        });
      })
      .catch(function () {
        // fetch falhou (servidor desligado, sem internet...)
        return { sucesso: false, mensagem: 'Não foi possível conectar ao servidor.' };
      })
      .then(function (r) {
        // aqui sempre chega { sucesso, ... }, deu certo ou não
        plantioSelect.innerHTML = '';
        if (!r.sucesso) {
          plantioSelect.innerHTML = '<option value="">Não foi possível carregar</option>';
          showMessage(r.mensagem, false);
          return;
        }
        if (r.plantios.length === 0) {
          plantioSelect.innerHTML = '<option value="">Nenhum plantio cadastrado</option>';
          showMessage('Cadastre um plantio antes de registrar atividades.', false);
          return;
        }
        plantioSelect.innerHTML = '<option value="">Selecione o plantio</option>';
        r.plantios.forEach(function (p) {
          const opt = document.createElement('option');
          opt.value = p.codigo;
          opt.textContent = p.nome + ' - ' + p.talhao; // textContent: nao interpreta HTML
          plantioSelect.appendChild(opt);
        });
      });
  }

  // texto da opção selecionada (não o value); usado na revisão
  function selectedText(select) {
    return select.selectedIndex >= 0 ? select.options[select.selectedIndex].textContent : '';
  }

  // Devolve a mensagem de erro da etapa 1, ou '' se estiver tudo certo
  function validateStep1() {
    if (!plantioSelect.value) return 'Selecione o plantio.';
    if (!atividadeSelect.value) return 'Selecione a atividade.';
    if (!statusSelect.value) return 'Selecione o status da atividade.';
    if (!dataInput.value) return 'Informe a data de execução.';
    return '';
  }

  // monta o JSON com os códigos do servidor e envia a atividade
  function salvarAtividade() {
    if (!usuario || !usuario.codigo) {
      showMessage('Sua sessão expirou. Entre novamente.', false);
      setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
      return;
    }

    const data = new FormData(form); // lê todos os campos do formulário pelo name
    const payload = {
      usuarioCodigo: usuario.codigo,
      plantioCodigo: parseInt(data.get('plantio'), 10),
      tipoAtividade: tipoCodigos[data.get('atividade')], // texto amigável -> código
      status: statusCodigos[data.get('status')],
      dataExecucao: data.get('dataExecucao'),
      quantidade: String(data.get('quantidade')).trim(),
      responsavel: String(data.get('responsavel')).trim(),
      observacoes: String(data.get('observacoes')).trim()
    };

    btnContinuar.disabled = true; // evita clique duplo enquanto envia
    fetch(API + '/api/atividades', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
      .then(function (resp) {
        return resp.json().catch(function () {
          return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
        });
      })
      .catch(function () {
        return { sucesso: false, mensagem: 'Não foi possível conectar ao servidor.' };
      })
      .then(function (r) {
        btnContinuar.disabled = false;
        if (r.sucesso) {
          showMessage(r.mensagem, true);
          // deixa o usuário ver a mensagem e depois volta pra lista
          setTimeout(function () { window.location.href = 'atividades.html'; }, 1200);
        } else {
          showMessage(r.mensagem, false);
        }
      });
  }

  // ---------- elementos do formulário em etapas ----------
  const stepItems = document.querySelectorAll('.step-item');       // bolinhas do topo
  const stepLines = document.querySelectorAll('.step-line');       // linhas entre as bolinhas
  const stepContents = document.querySelectorAll('.step-content'); // conteúdo de cada etapa
  const btnVoltar = document.getElementById('btnVoltar');
  const btnContinuar = document.getElementById('btnContinuar');

  const form = document.getElementById('registrarAtividadeForm');
  const observacoes = document.getElementById('observacoes');
  const charCount = document.getElementById('charCount');

  const statusSelect = document.getElementById('status');
  const statusDot = document.getElementById('statusDot');
  const reviewStatusDot = document.getElementById('reviewStatusDot');

  // Contador de caracteres das observações
  if (observacoes) {
    observacoes.addEventListener('input', function () {
      charCount.textContent = observacoes.value.length;
    });
  }

  // Bolinha colorida de acordo com o status selecionado
  const statusDotClasses = {
    'Concluída': '',          // verde é a cor padrão, sem classe extra
    'Pendente': 'dot-amber',
    'Atrasada': 'dot-red',
    'Planejada': 'dot-gray'
  };

  function applyStatusDotColor(dotEl, statusValue) {
    if (!dotEl) return;
    dotEl.className = dotEl.className.split(' ')[0]; // mantém só a classe base (status-dot ou review-status-dot)
    const extraClass = statusDotClasses[statusValue];
    if (extraClass) {
      dotEl.classList.add(extraClass);
    }
  }

  function updateStatusDot() {
    if (!statusSelect) return;
    applyStatusDotColor(statusDot, statusSelect.value);
  }

  if (statusSelect) {
    statusSelect.addEventListener('change', updateStatusDot);
    updateStatusDot(); // já pinta a bolinha com o valor inicial
  }

  // "2026-11-22" -> "22/11/2026"
  function formatDate(value) {
    if (!value) return '-';
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  // marca a etapa atual como "active" e as anteriores como "done"
  function renderStepper() {
    stepItems.forEach((item) => {
      const step = parseInt(item.dataset.step, 10);
      item.classList.remove('active', 'done');
      if (step === currentStep) {
        item.classList.add('active');
      } else if (step < currentStep) {
        item.classList.add('done');
      }
    });

    // a linha entre as etapas fica "done" quando a etapa anterior já foi concluída
    stepLines.forEach((line, index) => {
      const stepNumber = index + 1;
      line.classList.toggle('done', stepNumber < currentStep);
    });
  }

  // mostra só o conteúdo da etapa atual
  function renderStepContent() {
    stepContents.forEach((content) => {
      const step = parseInt(content.dataset.stepContent, 10);
      content.classList.toggle('active', step === currentStep);
    });
  }

  // esconde o "Voltar" na etapa 1 e troca o texto/ícone do botão na última etapa
  function renderFooter() {
    btnVoltar.style.visibility = currentStep === 1 ? 'hidden' : 'visible'; // hidden mantém o espaço no layout

    if (currentStep === totalSteps) {
      btnContinuar.innerHTML = `
        Confirmar Atividade
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>
      `;
    } else {
      btnContinuar.innerHTML = `
        Continuar
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"></path><path d="M12 5l7 7-7 7"></path></svg>
      `;
    }
  }

  // preenche a tela de revisão (etapa 2) com o que foi digitado na etapa 1
  function updateReview() {
    const data = new FormData(form);

    document.getElementById('reviewPlantio').textContent = selectedText(plantioSelect) || '-';
    document.getElementById('reviewAtividade').textContent = data.get('atividade') || '-';
    document.getElementById('reviewStatus').textContent = data.get('status') || '-';
    applyStatusDotColor(reviewStatusDot, data.get('status'));
    document.getElementById('reviewData').textContent = formatDate(data.get('dataExecucao'));

    const quantidade = String(data.get('quantidade')).trim();
    document.getElementById('reviewQuantidade').textContent = quantidade || '-';

    document.getElementById('reviewResponsavel').textContent = data.get('responsavel') || '-';

    // observação vazia mostra um texto padrão e ganha a classe "empty" (estilo apagado)
    const observacoesValue = data.get('observacoes');
    const reviewObservacoes = document.getElementById('reviewObservacoes');
    reviewObservacoes.textContent = observacoesValue ? observacoesValue : 'Nenhuma observação informada';
    reviewObservacoes.classList.toggle('empty', !observacoesValue);
  }

  // vai pra uma etapa e atualiza tudo na tela
  function goToStep(step) {
    showMessage('', false); // limpa mensagem de erro antiga
    currentStep = Math.min(Math.max(step, 1), totalSteps); // garante que fica entre 1 e totalSteps
    if (currentStep === totalSteps) {
      updateReview(); // só monta a revisão quando chega na última etapa
    }
    renderStepper();
    renderStepContent();
    renderFooter();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  btnVoltar.addEventListener('click', function () {
    goToStep(currentStep - 1);
  });

  // "Continuar" valida e avança; na última etapa vira "Confirmar" e salva
  btnContinuar.addEventListener('click', function () {
    const erro = validateStep1();
    if (erro) {
      showMessage(erro, false);
      return;
    }

    if (currentStep === totalSteps) {
      salvarAtividade();
      return;
    }
    goToStep(currentStep + 1);
  });

  // clicar nas bolinhas do topo só deixa voltar (não pula pra frente sem validar)
  stepItems.forEach((item) => {
    item.addEventListener('click', function () {
      const step = parseInt(item.dataset.step, 10);
      if (step < currentStep) {
        goToStep(step);
      }
    });
    item.style.cursor = 'pointer';
  });

  // qualquer botão com data-goto-step (ex.: "Editar" na revisão) leva pra etapa indicada
  document.querySelectorAll('[data-goto-step]').forEach((btn) => {
    btn.addEventListener('click', function () {
      goToStep(parseInt(btn.dataset.gotoStep, 10));
    });
  });

  // início da página: desenha a etapa 1 e carrega os plantios
  renderStepper();
  renderStepContent();
  renderFooter();
  carregarPlantios();
});