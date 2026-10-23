document.addEventListener('DOMContentLoaded', function () {

  // Começa vazio: só aparece o que vier de Clima.carregar()
  const alerts = [];

  const PAGE_SIZE = 5;
  let currentPage = 1;

  const listView = document.getElementById('listView');
  const detailView = document.getElementById('detailView');
  const tableBody = document.getElementById('alertsTableBody');
  const pagination = document.getElementById('alertsPagination');

  const filtroTipo = document.getElementById('filtroTipo');
  const filtroPrioridade = document.getElementById('filtroPrioridade');
  const filtroCultura = document.getElementById('filtroCultura');
  const filtroTalhao = document.getElementById('filtroTalhao');
  const ordenarPor = document.getElementById('ordenarPor');

  function normalize(text) {
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }

  const priorityOrder = { 'Crítico': 0, 'Alta': 1, 'Média': 2 };

  function getFilteredSortedAlerts() {
    let result = alerts.filter((alert) => {
      if (filtroTipo.value && alert.type !== filtroTipo.value) return false;
      if (filtroPrioridade.value && alert.priority !== filtroPrioridade.value) return false;
      if (filtroCultura.value && alert.culture !== filtroCultura.value) return false;
      if (filtroTalhao.value && alert.talhao !== filtroTalhao.value) return false;
      return true;
    });

    if (ordenarPor.value === 'antigos') {
      result = result.slice().sort((a, b) => a.dateISO.localeCompare(b.dateISO));
    } else if (ordenarPor.value === 'prioridade') {
      result = result.slice().sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority]);
    } else {
      result = result.slice().sort((a, b) => b.dateISO.localeCompare(a.dateISO));
    }

    return result;
  }

  function priorityClass(priority) {
    return normalize(priority);
  }

  // ===== ESTADO DE CARREGAMENTO =====
  function mostrarCarregando() {
    tableBody.innerHTML = `<tr><td colspan="7"><div class="alerts-empty-state">Carregando alertas...</div></td></tr>`;
    pagination.innerHTML = '';
  }

  // ===== FILTROS DINÂMICOS (cultura e talhão dos plantios cadastrados) =====
  function preencherSelect(select, valores) {
    const atual = select.value;
    const primeira = select.options[0];
    select.innerHTML = '';
    select.appendChild(primeira);
    Array.from(new Set(valores.filter(Boolean)))
      .sort((a, b) => a.localeCompare(b, 'pt-BR', { numeric: true }))
      .forEach((v) => {
        const op = document.createElement('option');
        op.value = v;
        op.textContent = v;
        select.appendChild(op);
      });
    select.value = atual;
  }

  function preencherFiltros(plantios) {
    preencherSelect(filtroCultura, plantios.map((p) => p.cultura));
    preencherSelect(filtroTalhao, plantios.map((p) => p.talhao));
  }

  // ===== CARDS DE RESUMO =====
  function dataLocalISO(d) {
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${m}-${dia}`;
  }

  function atualizarResumo() {
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    const hojeISO = dataLocalISO(hoje);
    const em7 = new Date(hoje);
    em7.setDate(em7.getDate() + 7);
    const em7ISO = dataLocalISO(em7);

    const abertos = alerts.filter((a) => a.situacao !== 'Lido');

    document.getElementById('resumoCriticos').textContent =
      abertos.filter((a) => a.priority === 'Crítico').length;
    document.getElementById('resumoHoje').textContent =
      abertos.filter((a) => a.dateISO === hojeISO).length;
    document.getElementById('resumoSemana').textContent =
      abertos.filter((a) => a.dateISO > hojeISO && a.dateISO <= em7ISO).length;
    document.getElementById('resumoResolvidos').textContent =
      alerts.filter((a) => a.situacao === 'Lido').length;
  }

  function renderTable() {
    atualizarResumo();
    const filtered = getFilteredSortedAlerts();
    const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    currentPage = Math.min(currentPage, totalPages);
    const start = (currentPage - 1) * PAGE_SIZE;
    const pageItems = filtered.slice(start, start + PAGE_SIZE);

    if (pageItems.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="7"><div class="alerts-empty-state">Nenhum alerta encontrado com os filtros selecionados.</div></td></tr>`;
    } else {
      tableBody.innerHTML = pageItems.map((alert) => {
        const pClass = priorityClass(alert.priority);
        const situacaoClass = alert.situacao === 'Lido' ? 'situacao-lido' : 'situacao-nao-lido';
        return `
          <tr class="${alert.situacao === 'Não Lido' ? 'is-unread' : ''}">
            <td class="alert-title-cell">
              <p class="alert-name">${alert.title}</p>
              <p class="alert-desc">${alert.desc}</p>
            </td>
            <td><span class="alert-type-badge">${alert.type}</span></td>
            <td>
              <p class="alert-reference">${escaparHtml(alert.reference)}</p>
              <p class="alert-reference-sub">${escaparHtml(alert.talhao)}</p>
            </td>
            <td>
              <p class="alert-date">${alert.dateDisplay}</p>
              <p class="alert-date-relative">${alert.dateRelative}</p>
            </td>
            <td><span class="priority-text priority-${pClass}">${alert.priority}</span></td>
            <td><span class="situacao-text ${situacaoClass}">${alert.situacao}</span></td>
            <td><button type="button" class="btn-ver-detalhes" data-alert-id="${alert.id}">Ver detalhes</button></td>
          </tr>
        `;
      }).join('');
    }

    renderPagination(totalPages);
  }

  function renderPagination(totalPages) {
    let html = `<button type="button" id="pagPrev" ${currentPage === 1 ? 'disabled' : ''}>
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"></path></svg>
      </button>`;

    for (let i = 1; i <= totalPages; i++) {
      html += `<button type="button" class="${i === currentPage ? 'active' : ''}" data-page="${i}">${i}</button>`;
    }

    html += `<button type="button" id="pagNext" ${currentPage === totalPages ? 'disabled' : ''}>
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"></path></svg>
      </button>`;

    pagination.innerHTML = html;

    document.getElementById('pagPrev').addEventListener('click', function () {
      currentPage -= 1;
      renderTable();
    });
    document.getElementById('pagNext').addEventListener('click', function () {
      currentPage += 1;
      renderTable();
    });
    pagination.querySelectorAll('button[data-page]').forEach((btn) => {
      btn.addEventListener('click', function () {
        currentPage = parseInt(btn.dataset.page, 10);
        renderTable();
      });
    });
  }

  [filtroTipo, filtroPrioridade, filtroCultura, filtroTalhao, ordenarPor].forEach((el) => {
    el.addEventListener('change', function () {
      currentPage = 1;
      renderTable();
    });
  });

  tableBody.addEventListener('click', function (e) {
    const btn = e.target.closest('button[data-alert-id]');
    if (!btn) return;
    openDetail(btn.dataset.alertId);
  });

  // ===== DETAIL VIEW =====
  function weatherIcon(rainPct) {
    if (rainPct >= 60) {
      return `<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 16.5a4 4 0 0 1 .5-8 5.5 5.5 0 0 1 10.6 1.6A3.6 3.6 0 0 1 17.5 17H7Z"></path><path d="M9 19l-1 2M13 19l-1 2M17 19l-1 2"></path></svg>`;
    }
    if (rainPct >= 25) {
      return `<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 16.5a4 4 0 0 1 .5-8 5.5 5.5 0 0 1 10.6 1.6A3.6 3.6 0 0 1 17.5 17H7Z"></path></svg>`;
    }
    return `<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4.5"></circle><path d="M12 3v2M12 19v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M3 12h2M19 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4"></path></svg>`;
  }

  function openDetail(alertId) {
    const alert = alerts.find((a) => String(a.id) === String(alertId));
    if (!alert) return;

    const pClass = priorityClass(alert.priority);

    document.getElementById('detailTitle').textContent = alert.title;
    document.getElementById('detailDesc').innerHTML = alert.desc;

    const badge = document.getElementById('detailPriorityBadge');
    badge.textContent = alert.priority;
    badge.className = `detail-priority-badge priority-${pClass}`;

    document.getElementById('detailTipo').textContent = alert.type;
    document.getElementById('detailDataAlerta').textContent = alert.detail.dataAlertaFull;

    const situacaoEl = document.getElementById('detailSituacao');
    situacaoEl.textContent = alert.situacao;
    situacaoEl.className = `detail-meta-value ${alert.situacao === 'Lido' ? 'tone-green' : 'tone-red'}`;

    const prioridadeEl = document.getElementById('detailPrioridade');
    prioridadeEl.textContent = alert.priority;
    const prioridadeTone = alert.priority === 'Crítico' ? 'tone-red' : alert.priority === 'Alta' ? 'tone-amber' : 'tone-yellow';
    prioridadeEl.className = `detail-meta-value ${prioridadeTone}`;

    document.getElementById('detailImpactActivity').textContent = alert.detail.impact.activity;
    document.getElementById('detailImpactReference').textContent = alert.detail.impact.reference;
    const impactDateEl = document.getElementById('detailImpactDate');
    impactDateEl.textContent = alert.detail.impact.dateLabel;
    impactDateEl.className = `impact-date tone-${alert.detail.impact.tone}`;

    document.getElementById('detailRecommendations').innerHTML = alert.detail.recommendations.map((rec) => `
      <li>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>
        <span>${rec}</span>
      </li>
    `).join('');

    document.getElementById('detailForecastGrid').innerHTML = alert.detail.forecast.map((day) => `
      <div class="forecast-day">
        <p class="forecast-day-label">${day.label}</p>
        <p class="forecast-day-date">${day.date}</p>
        <div class="forecast-day-icon">${weatherIcon(day.rain)}</div>
        <p class="forecast-day-temp">${day.tempMax}°C / ${day.tempMin}°C</p>
        <p class="forecast-day-rain">${day.rain}% de chuva</p>
      </div>
    `).join('');

    document.getElementById('detailTips').innerHTML = alert.detail.tips;

    const btnMarcarLido = document.getElementById('btnMarcarLido');
    updateMarcarLidoButton(alert, btnMarcarLido);

    btnMarcarLido.onclick = function () {
      alert.situacao = 'Lido';
      Clima.marcarLido(alert.id);
      situacaoEl.textContent = 'Lido';
      situacaoEl.className = 'detail-meta-value tone-green';
      updateMarcarLidoButton(alert, btnMarcarLido);
      renderTable();
    };

    listView.classList.add('hidden');
    detailView.classList.add('active');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function updateMarcarLidoButton(alert, btn) {
    if (alert.situacao === 'Lido') {
      btn.innerHTML = `Marcado como lido
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>`;
      btn.disabled = true;
    } else {
      btn.innerHTML = `Marcar como lido
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"></path></svg>`;
      btn.disabled = false;
    }
  }

  document.getElementById('btnVoltarDetalhe').addEventListener('click', function () {
    detailView.classList.remove('active');
    listView.classList.remove('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // ===== CARREGAMENTO INICIAL =====
  mostrarCarregando();
  carregarAlertasClimaticos();

  async function carregarAlertasClimaticos() {
    const avisos = [];
    try {
      const r = await Clima.carregar();
      alerts.push(...r.alertas);
      preencherFiltros(r.plantios || []);
      if (r.semLocalizacao) avisos.push('Cadastre a localização do plantio para receber alertas climáticos.');
      if (r.naoEncontrados.length) avisos.push('Não encontramos a localização: ' + r.naoEncontrados.join('; ') + '. Use o formato "Cidade, UF".');
      if (r.falhas.length) avisos.push('Não foi possível consultar a previsão do tempo de: ' + r.falhas.join(', ') + '. Tente novamente mais tarde.');
    } catch (e) {
      console.error('Falha ao carregar alertas climáticos:', e);
      avisos.push('Não foi possível carregar os alertas climáticos agora.');
    } finally {
      renderTable();
      mostrarAviso(avisos);
    }
  }

  function mostrarAviso(avisos) {
    const antigo = document.getElementById('weatherNote');
    if (antigo) antigo.remove();
    if (!avisos.length) return;
    const aviso = document.createElement('p');
    aviso.id = 'weatherNote';
    aviso.className = 'weather-source-note';
    aviso.textContent = avisos.join(' ');
    tableBody.closest('table').insertAdjacentElement('afterend', aviso);
  }

  function escaparHtml(texto) {
    return String(texto == null ? '' : texto).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
});