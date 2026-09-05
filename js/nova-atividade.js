document.addEventListener('DOMContentLoaded', function () {
  const totalSteps = 2;
  let currentStep = 1;

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

    document.getElementById('reviewPlantio').textContent = data.get('plantio') || '-';
    document.getElementById('reviewAtividade').textContent = data.get('atividade') || '-';
    document.getElementById('reviewStatus').textContent = data.get('status') || '-';
    applyStatusDotColor(reviewStatusDot, data.get('status'));
    document.getElementById('reviewData').textContent = formatDate(data.get('dataExecucao'));

    const quantidade = data.get('quantidade');
    document.getElementById('reviewQuantidade').textContent = quantidade ? `${quantidade} mm` : '-';

    document.getElementById('reviewResponsavel').textContent = data.get('responsavel') || '-';

    const observacoesValue = data.get('observacoes');
    const reviewObservacoes = document.getElementById('reviewObservacoes');
    reviewObservacoes.textContent = observacoesValue ? observacoesValue : 'Nenhuma observação informada';
    reviewObservacoes.classList.toggle('empty', !observacoesValue);
  }

  function goToStep(step) {
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
    if (currentStep === totalSteps) {
      // Aqui entraria a chamada para salvar a atividade (API / backend)
      window.location.href = 'atividades.html';
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