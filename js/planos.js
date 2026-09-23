document.addEventListener('DOMContentLoaded', function () {

  const planNames = {
    gratuito: 'Gratuito',
    essencial: 'Essencial',
    avancado: 'Avançado'
  };

  const plansGrid = document.getElementById('plansGrid');
  const bannerPlanName = document.getElementById('bannerPlanName');
  const modal = document.getElementById('modalAssinarPlano');
  const modalPlanoNome = document.getElementById('modalPlanoNome');
  const modalPlanoPreco = document.getElementById('modalPlanoPreco');
  const btnConfirmarAssinatura = document.getElementById('btnConfirmarAssinatura');

  let planoSelecionadoId = null;
  let currentPlanId = localStorage.getItem('agromonitor_plano') || 'gratuito';

  function applyCurrentPlan() {
    document.querySelectorAll('.plan-card').forEach((card) => {
      const isCurrent = card.dataset.planId === currentPlanId;
      card.classList.toggle('is-current', isCurrent);

      const badge = card.querySelector('[data-current-badge]');
      badge.style.display = isCurrent ? 'inline-block' : 'none';

      const btn = card.querySelector('[data-plan-btn]');
      if (isCurrent) {
        btn.textContent = 'Seu plano atual';
        btn.className = 'plan-card-btn is-active-plan';
        btn.disabled = true;
      } else {
        btn.textContent = 'Assinar esse plano';
        btn.className = 'plan-card-btn is-subscribe';
        btn.disabled = false;
      }
    });

    bannerPlanName.textContent = planNames[currentPlanId] || 'Gratuito';
    localStorage.setItem('agromonitor_plano', currentPlanId);
  }

  plansGrid.addEventListener('click', function (e) {
    const btn = e.target.closest('[data-plan-btn]');
    if (!btn || btn.disabled) return;

    const card = btn.closest('.plan-card');
    planoSelecionadoId = card.dataset.planId;
    modalPlanoNome.textContent = card.dataset.planName;
    modalPlanoPreco.textContent = card.dataset.planPrice;
    modal.classList.add('is-open');
  });

  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', function () {
      document.getElementById(btn.dataset.closeModal).classList.remove('is-open');
    });
  });

  document.querySelectorAll('.modal-overlay').forEach((overlay) => {
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) overlay.classList.remove('is-open');
    });
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') modal.classList.remove('is-open');
  });

  btnConfirmarAssinatura.addEventListener('click', function () {
    if (!planoSelecionadoId) return;
    currentPlanId = planoSelecionadoId;
    applyCurrentPlan();
    modal.classList.remove('is-open');
    showToast(`Você agora está no plano ${planNames[currentPlanId]}.`);
  });

  let toastTimeout;
  function showToast(message) {
    const toast = document.getElementById('toast');
    document.getElementById('toastText').textContent = message;
    toast.classList.add('is-visible');
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => toast.classList.remove('is-visible'), 2600);
  }

  applyCurrentPlan();
});