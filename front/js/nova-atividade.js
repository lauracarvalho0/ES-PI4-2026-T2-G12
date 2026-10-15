document.addEventListener('DOMContentLoaded', function () {
  const totalSteps = 2;
  let currentStep = 1;

  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const formMessage = document.getElementById('formMessage');
  const plantioSelect = document.getElementById('plantio');
  const atividadeSelect = document.getElementById('atividade');
  const dataInput = document.getElementById('dataExecucao');

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
    'Planejada': 'PENDENTE',
    'Atrasada': 'ATRASADA'
  };

  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  // Carrega os plantios do usuario para o select
  function carregarPlantios() {
    if (!usuario || !usuario.codigo) {
      plantioSelect.innerHTML = '<option value="">Sessão expirada</option>';
      showMessage('Sua sessão expirou. Entre novamente.', false);
      setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
      return;
    }

    fetch(API + '/api/plantios?usuarioCodigo=' + encodeURIComponent(usuario.codigo))
      .then(function (resp) {
        return resp.json().catch(function () {
          return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
        });
      })
      .catch(function () {
        return { sucesso: false, mensagem: 'Não foi possível conectar ao servidor.' };
      })
      .then(function (r) {
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

  function salvarAtividade() {
    if (!usuario || !usuario.codigo) {
      showMessage('Sua sessão expirou. Entre novamente.', false);
      setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
      return;
    }

    const data = new FormData(form);
    const payload = {
      usuarioCodigo: usuario.codigo,
      plantioCodigo: parseInt(data.get('plantio'), 10),
      tipoAtividade: tipoCodigos[data.get('atividade')],
      status: statusCodigos[data.get('status')],
      dataExecucao: data.get('dataExecucao'),
      quantidade: String(data.get('quantidade')).trim(),
      responsavel: String(data.get('responsavel')).trim(),
      observacoes: String(data.get('observacoes')).trim()
    };

    btnContinuar.disabled = true;
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
          setTimeout(function () { window.location.href = 'atividades.html'; }, 1200);
        } else {
          showMessage(r.mensagem, false);
        }
      });
  }

  const stepItems = document.querySelectorAll('.step-item');
  const stepLines = document.querySelectorAll('.step-line');
  const stepContents = document.querySelectorAll('.step-content');
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
    'Concluída': '',
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
    updateStatusDot();
  }

  function formatDate(value) {
    if (!value) return '-';
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

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

    stepLines.forEach((line, index) => {
      const stepNumber = index + 1;
      line.classList.toggle('done', stepNumber < currentStep);
    });
  }

  function renderStepContent() {
    stepContents.forEach((content) => {
      const step = parseInt(content.dataset.stepContent, 10);
      content.classList.toggle('active', step === currentStep);
    });
  }

  function renderFooter() {
    btnVoltar.style.visibility = currentStep === 1 ? 'hidden' : 'visible';

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

    const observacoesValue = data.get('observacoes');
    const reviewObservacoes = document.getElementById('reviewObservacoes');
    reviewObservacoes.textContent = observacoesValue ? observacoesValue : 'Nenhuma observação informada';
    reviewObservacoes.classList.toggle('empty', !observacoesValue);
  }

  function goToStep(step) {
    showMessage('', false);
    currentStep = Math.min(Math.max(step, 1), totalSteps);
    if (currentStep === totalSteps) {
      updateReview();
    }
    renderStepper();
    renderStepContent();
    renderFooter();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  btnVoltar.addEventListener('click', function () {
    goToStep(currentStep - 1);
  });

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

  stepItems.forEach((item) => {
    item.addEventListener('click', function () {
      const step = parseInt(item.dataset.step, 10);
      if (step < currentStep) {
        goToStep(step);
      }
    });
    item.style.cursor = 'pointer';
  });

  document.querySelectorAll('[data-goto-step]').forEach((btn) => {
    btn.addEventListener('click', function () {
      goToStep(parseInt(btn.dataset.gotoStep, 10));
    });
  });

  renderStepper();
  renderStepContent();
  renderFooter();
  carregarPlantios();
});