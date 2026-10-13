// ===== DADOS MOCK (substitua por dados vindos do backend/API) =====
const plantiosData = [
  {
    id: 1,
    nome: 'Milho Verão 2025/26',
    cultura: 'Milho',
    culturaClass: 'milho',
    talhao: 'Talhão 01',
    dataPlantio: '20/05/2026',
    concluidas: 7,
    pendentes: 2,
    atrasadas: 0,
    progresso: 78
  },
  {
    id: 2,
    nome: 'Soja 2025/26',
    cultura: 'Soja',
    culturaClass: 'soja',
    talhao: 'Talhão 02',
    dataPlantio: '15/05/2026',
    concluidas: 6,
    pendentes: 1,
    atrasadas: 1,
    progresso: 60
  },
  {
    id: 3,
    nome: 'Milho Safrinha 2025/26',
    cultura: 'Milho',
    culturaClass: 'milho',
    talhao: 'Talhão 03',
    dataPlantio: '28/04/2026',
    concluidas: 2,
    pendentes: 2,
    atrasadas: 1,
    progresso: 40
  },
  {
    id: 4,
    nome: 'Feijão 2025/26',
    cultura: 'Feijão',
    culturaClass: 'feijao',
    talhao: 'Talhão 01',
    dataPlantio: '10/05/2026',
    concluidas: 0,
    pendentes: 0,
    atrasadas: 0,
    progresso: 0
  }
];

const atividadesProximasData = [
  { plantioId: 2, dia: '03', mes: 'JUN', titulo: 'Adubação de cobertura', status: 'pendente' },
  { plantioId: 3, dia: '05', mes: 'JUN', titulo: 'Aplicação de defensivos', status: 'pendente' },
  { plantioId: 2, dia: '07', mes: 'JUN', titulo: 'Monitoramento', status: 'atrasada' },
  { plantioId: 1, dia: '10', mes: 'JUN', titulo: 'Tratos culturais', status: 'pendente' }
];

// Estado atual dos filtros
const filtros = {
  periodo: '01/05/2026 - 31/05/2026',
  plantio: 'Todos os plantios',
  cultura: 'Todas as culturas',
  talhao: 'Todos os talhões'
};

document.addEventListener('DOMContentLoaded', function () {
  setupDropdowns();
  setupExportar();
  render();
});

// ===== DROPDOWNS DOS FILTROS =====
function setupDropdowns() {
  document.querySelectorAll('.rel-native-select').forEach(function (select) {
    select.addEventListener('change', function () {
      filtros[select.dataset.filter] = select.value;
      render();
    });
  });
}

// ===== FILTRAGEM =====
function getPlantiosFiltrados() {
  return plantiosData.filter(function (p) {
    const okPlantio = filtros.plantio === 'Todos os plantios' || p.nome === filtros.plantio;
    const okCultura = filtros.cultura === 'Todas as culturas' || p.cultura === filtros.cultura;
    const okTalhao = filtros.talhao === 'Todos os talhões' || p.talhao === filtros.talhao;
    return okPlantio && okCultura && okTalhao;
  });
}

// ===== RENDERIZAÇÃO GERAL =====
function render() {
  const plantiosFiltrados = getPlantiosFiltrados();

  renderResumo(plantiosFiltrados);
  renderDonut(plantiosFiltrados);
  renderProximasAtividades(plantiosFiltrados);
  renderTabela(plantiosFiltrados);
}

// ===== RESUMO (cards no topo) =====
function renderResumo(lista) {
  const totais = calcularTotais(lista);
  const totalAtividades = totais.concluidas + totais.pendentes + totais.atrasadas;

  document.getElementById('numPlantios').textContent = lista.length;
  document.getElementById('numTotal').textContent = totalAtividades;
  document.getElementById('numConcluidas').textContent = totais.concluidas;
  document.getElementById('numPendentes').textContent = totais.pendentes;
  document.getElementById('numAtrasadas').textContent = totais.atrasadas;

  const pctConcluidas = pct(totais.concluidas, totalAtividades);
  const pctPendentes = pct(totais.pendentes, totalAtividades);
  const pctAtrasadas = pct(totais.atrasadas, totalAtividades);

  document.getElementById('pctConcluidas').textContent = pctConcluidas + '% do total';
  document.getElementById('pctPendentes').textContent = pctPendentes + '% do total';
  document.getElementById('pctAtrasadas').textContent = pctAtrasadas + '% no total';
}

function calcularTotais(lista) {
  return lista.reduce(function (acc, p) {
    acc.concluidas += p.concluidas;
    acc.pendentes += p.pendentes;
    acc.atrasadas += p.atrasadas;
    return acc;
  }, { concluidas: 0, pendentes: 0, atrasadas: 0 });
}

function pct(valor, total) {
  if (total === 0) return 0;
  return Math.round((valor / total) * 100);
}

// ===== GRÁFICO DE ROSCA (status das atividades) =====
function renderDonut(lista) {
  const totais = calcularTotais(lista);
  const planejadas = 0; // reservado para status "planejada"
  const total = totais.concluidas + totais.pendentes + totais.atrasadas + planejadas;

  const donut = document.getElementById('donutChart');
  document.getElementById('donutTotal').textContent = total;

  if (total === 0) {
    donut.style.background = 'conic-gradient(var(--gray-bg) 0 100%)';
  } else {
    const pConcluidas = (totais.concluidas / total) * 100;
    const pPendentes = (totais.pendentes / total) * 100;
    const pAtrasadas = (totais.atrasadas / total) * 100;

    const fim1 = pConcluidas;
    const fim2 = fim1 + pPendentes;
    const fim3 = fim2 + pAtrasadas;

    donut.style.background =
      'conic-gradient(' +
      'var(--accent-green) 0% ' + fim1 + '%, ' +
      'var(--amber) ' + fim1 + '% ' + fim2 + '%, ' +
      'var(--red) ' + fim2 + '% ' + fim3 + '%, ' +
      'var(--gray-bg) ' + fim3 + '% 100%)';
  }

  document.getElementById('legendConcluidas').textContent = totais.concluidas;
  document.getElementById('legendPendentes').textContent = totais.pendentes;
  document.getElementById('legendAtrasadas').textContent = totais.atrasadas;
  document.getElementById('legendPlanejadas').textContent = planejadas;

  document.getElementById('legendConcluidasPct').textContent = pct(totais.concluidas, total) + '%';
  document.getElementById('legendPendentesPct').textContent = pct(totais.pendentes, total) + '%';
  document.getElementById('legendAtrasadasPct').textContent = pct(totais.atrasadas, total) + '%';
  document.getElementById('legendPlanejadasPct').textContent = pct(planejadas, total) + '%';
}

// ===== PRÓXIMAS ATIVIDADES =====
function renderProximasAtividades(lista) {
  const idsPermitidos = lista.map(function (p) { return p.id; });
  const container = document.getElementById('proximasList');
  container.innerHTML = '';

  const filtradas = atividadesProximasData.filter(function (a) {
    return idsPermitidos.indexOf(a.plantioId) !== -1;
  });

  if (filtradas.length === 0) {
    container.innerHTML = '<p class="rel-empty-state">Nenhuma atividade encontrada para os filtros selecionados.</p>';
    return;
  }

  filtradas.forEach(function (a) {
    const plantio = plantiosData.find(function (p) { return p.id === a.plantioId; });
    const item = document.createElement('div');
    item.className = 'rel-next-item';

    item.innerHTML =
      '<div class="rel-next-date">' +
        '<span class="rel-next-day">' + a.dia + '</span>' +
        '<span class="rel-next-month">' + a.mes + '</span>' +
      '</div>' +
      '<div class="rel-next-info">' +
        '<p class="rel-next-title">' + a.titulo + '</p>' +
        '<p class="rel-next-subtitle">' + plantio.cultura + ' · ' + plantio.talhao + '</p>' +
      '</div>' +
      '<span class="badge-status badge-' + a.status + '">' + capitalizar(a.status) + '</span>';

    container.appendChild(item);
  });
}

function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

// ===== TABELA DE RESUMO DOS PLANTIOS =====
function renderTabela(lista) {
  const tbody = document.getElementById('plantiosTableBody');
  const emptyState = document.getElementById('emptyState');
  tbody.innerHTML = '';

  if (lista.length === 0) {
    emptyState.style.display = 'block';
    return;
  }
  emptyState.style.display = 'none';

  lista.forEach(function (p) {
    const tr = document.createElement('tr');

    tr.innerHTML =
      '<td>' +
        '<div class="plantio-culture">' +
          '<span class="culture-dot ' + p.culturaClass + '"></span>' +
          '<span class="plantio-name" style="font-size:14px;">' + p.nome + '</span>' +
        '</div>' +
      '</td>' +
      '<td>' + p.cultura + '</td>' +
      '<td>' + p.talhao + '</td>' +
      '<td>' + p.dataPlantio + '</td>' +
      '<td style="color: var(--accent-green); font-weight:600;">' + p.concluidas + '</td>' +
      '<td style="color: var(--amber); font-weight:600;">' + p.pendentes + '</td>' +
      '<td style="color: var(--red); font-weight:600;">' + p.atrasadas + '</td>' +
      '<td>' +
        '<div class="progress-wrap">' +
          '<span class="progress-percent">' + p.progresso + '%</span>' +
          '<div class="progress-bar">' +
            '<div class="progress-fill" style="width:' + p.progresso + '%;"></div>' +
          '</div>' +
        '</div>' +
      '</td>';

    tbody.appendChild(tr);
  });
}

// ===== EXPORTAR RELATÓRIO (CSV) =====
function setupExportar() {
  const btn = document.getElementById('btnExportar');
  if (!btn) return;

  btn.addEventListener('click', function () {
    const lista = getPlantiosFiltrados();

    if (lista.length === 0) {
      alert('Não há dados para exportar com os filtros selecionados.');
      return;
    }

    const cabecalho = ['Plantio', 'Cultura', 'Talhão', 'Data do plantio', 'Concluídas', 'Pendentes', 'Atrasadas', 'Progresso (%)'];
    const linhas = lista.map(function (p) {
      return [p.nome, p.cultura, p.talhao, p.dataPlantio, p.concluidas, p.pendentes, p.atrasadas, p.progresso].join(';');
    });

    const csv = [cabecalho.join(';')].concat(linhas).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = 'relatorio-agromonitor.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  });
}