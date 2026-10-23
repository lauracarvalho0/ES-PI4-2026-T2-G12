document.addEventListener('DOMContentLoaded', function () {

  // ===== DADOS DE EXEMPLO (viriam da API) =====
  const alerts = [
    {
      id: 1,
      title: 'Chuva intensa prevista',
      desc: 'Risco para a aplicação de defensivos.<br>Previsão para as próximas 24h.',
      type: 'Climático',
      culture: 'Milho',
      reference: 'Milho verão 25/26',
      talhao: 'Talhão 01',
      dateISO: '2025-06-01',
      dateDisplay: '01/06/2025',
      dateRelative: 'Hoje',
      priority: 'Crítico',
      situacao: 'Não Lido',
      detail: {
        dataAlertaFull: '01/05/2025 8:30',
        origem: 'Previsão do tempo',
        impact: { activity: 'Aplicação de defensivos', reference: 'Milho Verão 2025/26 - Talhão 01', dateLabel: 'Hoje 01/06', tone: 'red' },
        recommendations: [
          'Evite aplicar defensivos nas próximas 24 horas devido ao risco de chuva.',
          'Aguarde melhora nas condições climáticas para garantir eficácia do produto.',
          'Acompanhe a previsão e reagende a atividade, se necessário.'
        ],
        forecast: [
          { label: 'Hoje', date: '01/05', tempMax: 28, tempMin: 20, rain: 90 },
          { label: 'Amanhã', date: '02/05', tempMax: 26, tempMin: 19, rain: 80 },
          { label: 'Terça', date: '03/05', tempMax: 27, tempMin: 19, rain: 30 },
          { label: 'Quarta', date: '04/05', tempMax: 28, tempMin: 20, rain: 10 }
        ],
        tips: 'Reagendar para o dia <span class="highlight">03/05</span> pode ser uma alternativa. A previsão indica redução de chuva.'
      }
    },
    {
      id: 2,
      title: 'Atividade atrasada',
      desc: 'Adubação de cobertura está atrasada.<br>Deveria ter sido realizada em 29/05.',
      type: 'Atividade',
      culture: 'Soja',
      reference: 'Soja verão 25/26',
      talhao: 'Talhão 02',
      dateISO: '2025-05-31',
      dateDisplay: '31/05/2025',
      dateRelative: '2 dias de atraso',
      priority: 'Alta',
      situacao: 'Não Lido',
      detail: {
        dataAlertaFull: '31/05/2025 9:00',
        origem: 'Sistema de monitoramento',
        impact: { activity: 'Adubação de cobertura', reference: 'Soja Verão 2025/26 - Talhão 02', dateLabel: 'Atrasada desde 29/05', tone: 'red' },
        recommendations: [
          'Realize a adubação de cobertura o quanto antes para não comprometer o desenvolvimento da cultura.',
          'Verifique a disponibilidade de insumos e equipe para a atividade.',
          'Registre a atividade assim que for concluída para atualizar a linha do tempo.'
        ],
        forecast: [
          { label: 'Hoje', date: '31/05', tempMax: 27, tempMin: 18, rain: 20 },
          { label: 'Amanhã', date: '01/06', tempMax: 28, tempMin: 19, rain: 10 },
          { label: 'Terça', date: '02/06', tempMax: 29, tempMin: 19, rain: 5 },
          { label: 'Quarta', date: '03/06', tempMax: 27, tempMin: 18, rain: 15 }
        ],
        tips: 'Aproveite a janela de tempo seco nos próximos dias para concluir a adubação sem atrasos adicionais.'
      }
    },
    {
      id: 3,
      title: 'Risco de geadas',
      desc: 'Temperaturas baixas podem afetar<br>o desenvolvimento da cultura.',
      type: 'Climático',
      culture: 'Feijão',
      reference: 'Feijão inverno 25/26',
      talhao: 'Talhão 03',
      dateISO: '2025-06-03',
      dateDisplay: '03/06/2025',
      dateRelative: 'Amanhã',
      priority: 'Alta',
      situacao: 'Não Lido',
      detail: {
        dataAlertaFull: '02/06/2025 7:15',
        origem: 'Previsão do tempo',
        impact: { activity: 'Proteção contra geadas', reference: 'Feijão Inverno 2025/26 - Talhão 03', dateLabel: 'Amanhã 03/06', tone: 'amber' },
        recommendations: [
          'Considere irrigação por aspersão durante a madrugada para proteger as plantas.',
          'Evite podas ou aplicações que exponham ainda mais a cultura ao frio.',
          'Monitore a temperatura mínima prevista para as próximas noites.'
        ],
        forecast: [
          { label: 'Hoje', date: '02/06', tempMax: 18, tempMin: 4, rain: 5 },
          { label: 'Amanhã', date: '03/06', tempMax: 16, tempMin: 2, rain: 5 },
          { label: 'Terça', date: '04/06', tempMax: 17, tempMin: 3, rain: 10 },
          { label: 'Quarta', date: '05/06', tempMax: 19, tempMin: 6, rain: 5 }
        ],
        tips: 'As temperaturas devem se estabilizar a partir de <span class="highlight">05/06</span>. Priorize a proteção das áreas mais sensíveis até lá.'
      }
    },
    {
      id: 4,
      title: 'Ventos fortes previstos',
      desc: 'Risco para a aplicação de defensivos.<br>Previsão para as próximas 24h.',
      type: 'Climático',
      culture: 'Algodão',
      reference: 'Algodão 25/26',
      talhao: 'Talhão 04',
      dateISO: '2025-06-05',
      dateDisplay: '05/06/2025',
      dateRelative: 'Em 2 dias',
      priority: 'Média',
      situacao: 'Não Lido',
      detail: {
        dataAlertaFull: '03/06/2025 14:20',
        origem: 'Previsão do tempo',
        impact: { activity: 'Aplicação de defensivos', reference: 'Algodão 2025/26 - Talhão 04', dateLabel: 'Em 2 dias - 05/06', tone: 'yellow' },
        recommendations: [
          'Evite pulverizações em dias de vento acima de 15 km/h para reduzir a deriva do produto.',
          'Priorize aplicações no início da manhã ou fim da tarde, quando o vento tende a ser mais fraco.',
          'Reagende a atividade caso a previsão de vento se confirme.'
        ],
        forecast: [
          { label: 'Hoje', date: '03/06', tempMax: 26, tempMin: 17, rain: 10 },
          { label: 'Amanhã', date: '04/06', tempMax: 27, tempMin: 18, rain: 5 },
          { label: 'Terça', date: '05/06', tempMax: 25, tempMin: 17, rain: 5 },
          { label: 'Quarta', date: '06/06', tempMax: 26, tempMin: 18, rain: 10 }
        ],
        tips: 'Se possível, antecipe a aplicação para hoje pela manhã, antes do vento aumentar.'
      }
    },
    {
      id: 5,
      title: 'Temperatura muito alta',
      desc: 'Risco para a aplicação de defensivos.<br>Previsão para as próximas 24h.',
      type: 'Climático',
      culture: 'Milho',
      reference: 'Milho verão 25/26',
      talhao: 'Talhão 01',
      dateISO: '2025-06-06',
      dateDisplay: '06/06/2025',
      dateRelative: 'Em 3 dias',
      priority: 'Média',
      situacao: 'Lido',
      detail: {
        dataAlertaFull: '03/06/2025 16:40',
        origem: 'Previsão do tempo',
        impact: { activity: 'Aplicação de defensivos', reference: 'Milho Verão 2025/26 - Talhão 01', dateLabel: 'Em 3 dias - 06/06', tone: 'yellow' },
        recommendations: [
          'Evite aplicações no período de maior calor, entre 11h e 15h.',
          'Garanta que a cultura esteja bem hidratada nos dias de temperatura elevada.',
          'Monitore sinais de estresse térmico nas plantas.'
        ],
        forecast: [
          { label: 'Hoje', date: '03/06', tempMax: 32, tempMin: 22, rain: 5 },
          { label: 'Amanhã', date: '04/06', tempMax: 33, tempMin: 23, rain: 5 },
          { label: 'Terça', date: '05/06', tempMax: 34, tempMin: 23, rain: 0 },
          { label: 'Quarta', date: '06/06', tempMax: 31, tempMin: 21, rain: 10 }
        ],
        tips: 'As temperaturas devem reduzir levemente a partir de <span class="highlight">07/06</span>. Ajuste o horário das atividades de campo enquanto o calor persistir.'
      }
    },
    {
      id: 6,
      title: 'Irrigação recomendada',
      desc: 'Solo com baixa umidade.<br>Irrigação recomendada nas próximas 24h.',
      type: 'Atividade',
      culture: 'Soja',
      reference: 'Soja verão 25/26',
      talhao: 'Talhão 02',
      dateISO: '2025-05-28',
      dateDisplay: '28/05/2025',
      dateRelative: '4 dias de atraso',
      priority: 'Alta',
      situacao: 'Lido',
      detail: {
        dataAlertaFull: '28/05/2025 10:10',
        origem: 'Sistema de monitoramento',
        impact: { activity: 'Irrigação', reference: 'Soja Verão 2025/26 - Talhão 02', dateLabel: 'Concluída em 29/05', tone: 'green' },
        recommendations: [
          'Realize a irrigação assim que possível para evitar estresse hídrico.',
          'Verifique o funcionamento do sistema de irrigação antes de iniciar.',
          'Acompanhe a umidade do solo nos próximos dias.'
        ],
        forecast: [
          { label: 'Hoje', date: '28/05', tempMax: 29, tempMin: 20, rain: 5 },
          { label: 'Amanhã', date: '29/05', tempMax: 30, tempMin: 21, rain: 0 },
          { label: 'Terça', date: '30/05', tempMax: 28, tempMin: 19, rain: 10 },
          { label: 'Quarta', date: '31/05', tempMax: 27, tempMin: 18, rain: 15 }
        ],
        tips: 'Já regularizado — a irrigação foi concluída em 29/05 conforme registro de atividades.'
      }
    },
    {
      id: 7,
      title: 'Umidade elevada do solo',
      desc: 'Excesso de umidade pode favorecer<br>o surgimento de doenças fúngicas.',
      type: 'Climático',
      culture: 'Feijão',
      reference: 'Feijão inverno 25/26',
      talhao: 'Talhão 03',
      dateISO: '2025-05-27',
      dateDisplay: '27/05/2025',
      dateRelative: '5 dias de atraso',
      priority: 'Média',
      situacao: 'Lido',
      detail: {
        dataAlertaFull: '27/05/2025 6:50',
        origem: 'Previsão do tempo',
        impact: { activity: 'Monitoramento fitossanitário', reference: 'Feijão Inverno 2025/26 - Talhão 03', dateLabel: 'Concluído em 29/05', tone: 'green' },
        recommendations: [
          'Inspecione as folhas em busca de sinais de fungos, como manchas ou bolor.',
          'Considere aplicação preventiva de fungicida caso os sintomas apareçam.',
          'Melhore a drenagem da área, se possível.'
        ],
        forecast: [
          { label: 'Hoje', date: '27/05', tempMax: 24, tempMin: 17, rain: 40 },
          { label: 'Amanhã', date: '28/05', tempMax: 23, tempMin: 16, rain: 35 },
          { label: 'Terça', date: '29/05', tempMax: 25, tempMin: 17, rain: 20 },
          { label: 'Quarta', date: '30/05', tempMax: 26, tempMin: 18, rain: 15 }
        ],
        tips: 'Alerta resolvido — nenhuma ocorrência de doença foi identificada na última inspeção.'
      }
    },
    {
      id: 8,
      title: 'Colheita próxima do prazo',
      desc: 'Janela ideal de colheita se encerrando.<br>Risco de perda de qualidade.',
      type: 'Atividade',
      culture: 'Algodão',
      reference: 'Algodão 25/26',
      talhao: 'Talhão 04',
      dateISO: '2025-05-25',
      dateDisplay: '25/05/2025',
      dateRelative: '7 dias de atraso',
      priority: 'Crítico',
      situacao: 'Lido',
      detail: {
        dataAlertaFull: '25/05/2025 8:00',
        origem: 'Sistema de monitoramento',
        impact: { activity: 'Colheita', reference: 'Algodão 2025/26 - Talhão 04', dateLabel: 'Concluída em 30/05', tone: 'green' },
        recommendations: [
          'Priorize a colheita nos próximos dias para evitar perda de qualidade da fibra.',
          'Organize a logística de transporte e armazenamento com antecedência.',
          'Verifique a previsão do tempo para evitar colher em dias de chuva.'
        ],
        forecast: [
          { label: 'Hoje', date: '25/05', tempMax: 27, tempMin: 19, rain: 10 },
          { label: 'Amanhã', date: '26/05', tempMax: 28, tempMin: 20, rain: 5 },
          { label: 'Terça', date: '27/05', tempMax: 29, tempMin: 20, rain: 0 },
          { label: 'Quarta', date: '28/05', tempMax: 27, tempMin: 18, rain: 20 }
        ],
        tips: 'Colheita concluída dentro do prazo revisado, sem perdas relevantes de qualidade registradas.'
      }
    }
  ];

  // Os alertas climáticos antigos eram exemplos fixos. A partir daqui eles
  // são gerados pela previsão atual para a localização de cada plantio.
  alerts.splice(0, alerts.length, ...alerts.filter((alert) => alert.type !== 'Climático'));
  
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

  function renderTable() {
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
              <p class="alert-reference">${alert.reference}</p>
              <p class="alert-reference-sub">${alert.talhao}</p>
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
    const alertId = btn.dataset.alertId;
    openDetail(alertId);
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

  renderTable();
  carregarAlertasClimaticos();

  async function carregarAlertasClimaticos() {
    const API = window.location.port === '8080' ? '' : 'http://localhost:8080';
    let usuario;
    try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (e) {}
    if (!usuario || !usuario.codigo) return;

    try {
      const resposta = await fetch(API + '/api/plantios?usuarioCodigo=' + encodeURIComponent(usuario.codigo));
      const dados = await resposta.json();
      if (!resposta.ok || !dados.sucesso) throw new Error(dados.mensagem || 'Não foi possível carregar os plantios.');

      const plantiosComLocal = (dados.plantios || []).filter((p) => p.localizacao && p.localizacao.trim());
      const previsoes = await Promise.all(plantiosComLocal.map(async (plantio) => {
        try { return await buscarPrevisaoDoPlantio(plantio); }
        catch (e) { console.warn('Não foi possível consultar o clima de ' + plantio.nome, e); return []; }
      }));
      alerts.push(...previsoes.flat());
      renderTable();

      if (plantiosComLocal.length === 0) {
        const aviso = document.createElement('p');
        aviso.className = 'weather-source-note';
        aviso.textContent = 'Cadastre a localização do plantio para receber alertas climáticos.';
        tableBody.closest('table').insertAdjacentElement('afterend', aviso);
      }
    } catch (e) {
      console.error('Falha ao carregar alertas climáticos:', e);
    }
  }

  async function buscarPrevisaoDoPlantio(plantio) {
    const localizacao = plantio.localizacao.trim();
    const geocodeUrl = 'https://geocoding-api.open-meteo.com/v1/search?name=' + encodeURIComponent(localizacao) + '&count=1&language=pt&format=json';
    const geoResponse = await fetch(geocodeUrl);
    if (!geoResponse.ok) throw new Error('Falha ao localizar ' + localizacao);
    const geo = await geoResponse.json();
    const lugar = geo.results && geo.results[0];
    if (!lugar) return [];

    const query = new URLSearchParams({
      latitude: lugar.latitude,
      longitude: lugar.longitude,
      daily: 'temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max',
      forecast_days: '7',
      timezone: 'auto'

    });
    const weatherResponse = await fetch('https://api.open-meteo.com/v1/forecast?' + query.toString());
    if (!weatherResponse.ok) throw new Error('Falha ao consultar previsão do tempo');
    const weather = await weatherResponse.json();
    const dias = weather.daily;
    if (!dias || !dias.time) return [];

    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    return dias.time.flatMap((data, i) => {
      const chuva = Math.round(dias.precipitation_probability_max[i] || 0);
      const min = Math.round(dias.temperature_2m_min[i]);
      const max = Math.round(dias.temperature_2m_max[i]);
      const vento = Math.round(dias.wind_speed_10m_max[i] || 0);
      const riscos = [];
      if (chuva >= 60) riscos.push({
        title: 'Chuva intensa prevista',
        priority: chuva >= 80 ? 'Crítico' : 'Alta',
        desc: `Chance de chuva de ${chuva}% em ${escaparHtml(localizacao)}.`,
        recommendations: chuva >= 80
          ? [
              `A chance de chuva é de ${chuva}%. Adie pulverizações: a água pode reduzir a eficácia do produto.`,
              'Antes de reagendar uma aplicação, confira no rótulo o intervalo sem chuva indicado para o produto usado.',
              'Evite tráfego de máquinas em solo encharcado e verifique se os canais de drenagem estão desobstruídos.'
            ]
          : [
              `A chance de chuva é de ${chuva}%. Reavalie atividades ao ar livre e acompanhe atualizações da previsão.`,
              'Se houver aplicação planejada, confira o intervalo sem chuva indicado no rótulo do produto.',
              'Observe a drenagem do talhão para evitar acúmulo de água.'
            ]
      });
      if (min <= 3) riscos.push({
        title: 'Risco de geada',
        priority: min <= 0 ? 'Crítico' : 'Alta',
        desc: `Temperatura mínima prevista de ${min}°C em ${escaparHtml(localizacao)}.`,
        recommendations: min <= 0
          ? [
              `A previsão indica ${min}°C. Priorize a inspeção das áreas baixas, onde o frio pode se concentrar.`,
              'Identifique culturas em fase sensível e avalie medidas de proteção adequadas à cultura e à propriedade.',
              'Monitore a temperatura local antes do amanhecer; a previsão pode variar conforme o talhão.'
            ]
          : [
              `A mínima prevista é ${min}°C. Monitore mudas e culturas sensíveis ao frio, sobretudo em áreas baixas.`,
              'Adie atividades que possam aumentar o estresse das plantas até a condição melhorar.',
              'Confira a temperatura no próprio talhão durante a madrugada, pois ela pode diferir da previsão regional.'
            ]
      });
      if (max >= 35) riscos.push({
        title: 'Temperatura muito alta',
        priority: max >= 38 ? 'Crítico' : 'Alta',
        desc: `Temperatura máxima prevista de ${max}°C em ${escaparHtml(localizacao)}.`,
        recommendations: max >= 38
          ? [
              `Com máxima prevista de ${max}°C, evite atividades de campo nos horários mais quentes.`,
              'Monitore sinais de murcha ou estresse térmico e confira a umidade do solo antes de ajustar a irrigação.',
              'Se possível, transfira tarefas externas para o início da manhã ou o fim da tarde.'
            ]
          : [
              `A máxima prevista é ${max}°C. Planeje tarefas externas para horários mais frescos.`,
              'Acompanhe a umidade do solo e os sinais de estresse térmico da cultura.',
              'Evite alterar a irrigação sem avaliar a necessidade do solo e da cultura.'
            ]
      });
      if (vento >= 40) riscos.push({
        title: 'Ventos fortes previstos',
        priority: vento >= 60 ? 'Crítico' : 'Alta',
        desc: `Ventos de até ${vento} km/h previstos em ${escaparHtml(localizacao)}.`,
        recommendations: vento >= 60
          ? [
              `Rajadas de até ${vento} km/h podem aumentar a deriva. Adie pulverizações enquanto o vento estiver forte.`,
              'Proteja materiais leves e equipamentos que possam ser deslocados pelo vento.',
              'Retome aplicações somente quando as condições estiverem dentro das orientações do produto e do equipamento.'
            ]
          : [
              `Vento de até ${vento} km/h previsto. Reavalie pulverizações para reduzir o risco de deriva.`,
              'Considere adiar atividades com materiais leves ou equipamentos expostos.',
              'Confira as condições no talhão antes de iniciar uma aplicação.'
            ]
      });
      return riscos.map((risco, riscoIndex) => {
        const dataObj = new Date(data + 'T12:00:00');
        const diferenca = Math.round((dataObj - hoje) / 86400000);
        const label = diferenca === 0 ? 'Hoje' : diferenca === 1 ? 'Amanhã' : dataObj.toLocaleDateString('pt-BR', { weekday: 'long' });
        const display = dataObj.toLocaleDateString('pt-BR');
        const forecast = dias.time.slice(i, i + 4).map((dia, offset) => ({
          label: offset === 0 ? label : new Date(dia + 'T12:00:00').toLocaleDateString('pt-BR', { weekday: 'short' }),
          date: new Date(dia + 'T12:00:00').toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
          tempMax: Math.round(dias.temperature_2m_max[i + offset]),
          tempMin: Math.round(dias.temperature_2m_min[i + offset]),
          rain: Math.round(dias.precipitation_probability_max[i + offset] || 0)
        }));
        return {
          id: `weather-${plantio.codigo}-${data}-${riscoIndex}`,
          title: risco.title, desc: risco.desc, type: 'Climático', culture: plantio.cultura,
          reference: plantio.nome, talhao: plantio.talhao || 'Talhão não informado',
          dateISO: data, dateDisplay: display,
          dateRelative: diferenca === 0 ? 'Hoje' : diferenca === 1 ? 'Amanhã' : `Em ${diferenca} dias`,
          priority: risco.priority, situacao: 'Não Lido',
          detail: {
            dataAlertaFull: new Date().toLocaleString('pt-BR'), origem: 'Previsão do tempo (Open-Meteo)',
            impact: { activity: 'Atividades de campo', reference: `${plantio.nome} - ${plantio.talhao || 'Talhão'}`, dateLabel: `${label} ${display.slice(0, 5)}`, tone: risco.priority === 'Crítico' ? 'red' : 'amber' },
            recommendations: risco.recommendations,
            forecast,
            tips: `Previsão para ${escaparHtml(lugar.name || localizacao)}. Os dados são atualizados ao abrir esta tela.`
          }
        };
      });
    });
  }

  function escaparHtml(texto) {
    return String(texto == null ? '' : texto).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
});