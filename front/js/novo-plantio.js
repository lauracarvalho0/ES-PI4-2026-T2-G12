document.addEventListener('DOMContentLoaded', function () {
  const totalSteps = 4;
  let currentStep = 1;

  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const formMessage = document.getElementById('formMessage');

  function showMessage(text, ok) {
    formMessage.style.color = ok ? '#2e7d32' : '#c62828';
    formMessage.textContent = text;
    formMessage.style.display = text ? 'block' : 'none';
  }

  // O formulario mostra textos amigaveis; o servidor espera os codigos do dicionario de dados
  const sistemaCodigos = { 'Sequeiro': 'SEQUEIRO', 'Irrigado': 'IRRIGADO' };
  const metodoCodigos = {
    'Plantio direto': 'PLANTIO_DIRETO',
    'Plantio convencional': 'CONVENCIONAL',
    'Plantio mínimo': 'MINIMO'
  };

  const stepItems = document.querySelectorAll('.step-item');
  const stepLines = document.querySelectorAll('.step-line');
  const stepContents = document.querySelectorAll('.step-content');
  const btnVoltar = document.getElementById('btnVoltar');
  const btnContinuar = document.getElementById('btnContinuar');

  const form = document.getElementById('novoPlantioForm');
  const descricao = document.getElementById('descricao');
  const charCount = document.getElementById('charCount');

  // Contador de caracteres da descrição
  if (descricao) {
    descricao.addEventListener('input', function () {
      charCount.textContent = descricao.value.length;
    });
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
        Confirmar Plantio
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>
      `;
    } else {
      btnContinuar.innerHTML = `
        Continuar
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"></path><path d="M12 5l7 7-7 7"></path></svg>
      `;
    }
  }

  // Deslocamento em dias de cada atividade em relação à data de plantio,
  // usado como previsão automática quando o usuário não faz um ajuste manual.
  const activityOffsets = {
    plantio: 0,
    irrigacao: 7,
    adubacao: 20,
    tratos: 35
  };

  const defaultForecasts = {
    plantio: '15/05/2026',
    irrigacao: '22/05/2026',
    adubacao: '04/06/2026',
    tratos: '19/06/2026'
  };

  function addDays(dateValue, days) {
    // dateValue no formato yyyy-mm-dd (input type="date")
    const [year, month, day] = dateValue.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    date.setDate(date.getDate() + days);
    const dd = String(date.getDate()).padStart(2, '0');
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const yyyy = date.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  }

  function resolveActivityDate(key, adjustFieldName, offset) {
    const data = new FormData(form);
    const adjustValue = data.get(adjustFieldName);
    if (adjustValue) {
      return formatDate(adjustValue);
    }
    const dataPlantio = data.get('dataPlantio');
    if (dataPlantio) {
      return addDays(dataPlantio, offset);
    }
    return defaultForecasts[key];
  }

  function updateReview() {
    const data = new FormData(form);

    document.getElementById('reviewDataAtividadePlantio').textContent = resolveActivityDate('plantio', 'ajustePlantio', activityOffsets.plantio);
    document.getElementById('reviewDataIrrigacao').textContent = resolveActivityDate('irrigacao', 'ajusteIrrigacao', activityOffsets.irrigacao);
    document.getElementById('reviewDataAdubacao').textContent = resolveActivityDate('adubacao', 'ajusteAdubacao', activityOffsets.adubacao);
    document.getElementById('reviewDataTratos').textContent = resolveActivityDate('tratos', 'ajusteTratos', activityOffsets.tratos);

    const cultura = data.get('cultura') || '-';
    document.getElementById('reviewCultura').textContent = cultura;
    document.getElementById('culturaLabelActivities').textContent = cultura !== '-' ? cultura : 'Soja';
    document.getElementById('culturaLabelReview').textContent = cultura !== '-' ? cultura : 'Soja';

    document.getElementById('reviewNome').textContent = data.get('nomePlantio') || '-';
    document.getElementById('reviewSafra').textContent = data.get('safra') || '-';

    const dataPlantioValue = formatDate(data.get('dataPlantio'));
    document.getElementById('reviewDataPlantio').querySelector('span').textContent = dataPlantioValue;

    document.getElementById('reviewMetodo').textContent = data.get('metodoPlantio') || '-';

    const descricaoValue = data.get('descricao');
    const reviewDescricao = document.getElementById('reviewDescricao');
    reviewDescricao.textContent = descricaoValue ? descricaoValue : 'Nenhuma descrição informada';
    reviewDescricao.classList.toggle('empty', !descricaoValue);

    document.getElementById('reviewTalhao').textContent = data.get('talhao') || '-';

    const area = data.get('areaPlantada');
    document.getElementById('reviewArea').textContent = area ? `${area} ha` : '-';

    document.getElementById('reviewSistema').textContent = data.get('sistemaCultivo') || '-';
    document.getElementById('reviewLocalizacao').querySelector('span').textContent = data.get('localizacao') || '-';

    const altitude = data.get('altitude');
    document.getElementById('reviewAltitude').textContent = altitude ? `${altitude} m` : '-';
  }

  // Aceita "12,5" ou "12.5"; devolve NaN se nao for numero
  function parseNumber(text) {
    return Number(String(text).trim().replace(',', '.'));
  }

  // Devolve a mensagem de erro da etapa, ou '' se estiver tudo certo
  function validateStep(step) {
    const data = new FormData(form);

    if (step === 1) {
      if (!data.get('cultura')) return 'Selecione a cultura.';
      if (!String(data.get('nomePlantio')).trim()) return 'Informe o nome do plantio.';
      if (!data.get('safra')) return 'Selecione a safra.';
    }

    if (step === 2) {
      if (!data.get('talhao')) return 'Selecione o talhão.';
      const area = parseNumber(data.get('areaPlantada'));
      if (!String(data.get('areaPlantada')).trim() || isNaN(area) || area <= 0) {
        return 'Informe uma área plantada válida (maior que zero).';
      }
      const altitude = String(data.get('altitude')).trim();
      if (altitude !== '' && (!/^\d+$/.test(altitude))) {
        return 'A altitude deve ser um número inteiro, sem negativos.';
      }
    }

    if (step === 3) {
      if (!data.get('dataPlantio')) return 'Informe a data de plantio.';
    }

    return '';
  }

  // Monta o JSON que o backend espera
  function buildPayload(usuario) {
    const data = new FormData(form);
    const altitude = String(data.get('altitude')).trim();

    return {
      usuarioCodigo: usuario.codigo,
      cultura: data.get('cultura'),
      nome: String(data.get('nomePlantio')).trim(),
      safra: data.get('safra'),
      descricao: String(data.get('descricao')).trim(),
      sistemaCultivo: sistemaCodigos[data.get('sistemaCultivo')],
      talhao: data.get('talhao'),
      areaPlantada: parseNumber(data.get('areaPlantada')),
      localizacao: String(data.get('localizacao')).trim(),
      altitude: altitude === '' ? null : parseInt(altitude, 10),
      dataPlantio: data.get('dataPlantio'),
      metodoPlantio: metodoCodigos[data.get('metodoPlantio')],
      ajustePlantio: data.get('ajustePlantio'),
      ajusteIrrigacao: data.get('ajusteIrrigacao'),
      ajusteAdubacao: data.get('ajusteAdubacao'),
      ajusteTratos: data.get('ajusteTratos')
    };
  }

  function salvarPlantio() {
    let usuario = null;
    try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

    if (!usuario || !usuario.codigo) {
      showMessage('Sua sessão expirou. Entre novamente.', false);
      setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
      return;
    }

    btnContinuar.disabled = true;
    fetch(API + '/api/plantios', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(buildPayload(usuario))
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
          setTimeout(function () { window.location.href = 'plantios.html'; }, 1200);
        } else {
          showMessage(r.mensagem, false);
        }
      });
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
    const erro = validateStep(currentStep);
    if (erro) {
      showMessage(erro, false);
      return;
    }

    if (currentStep === totalSteps) {
      salvarPlantio();
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
});