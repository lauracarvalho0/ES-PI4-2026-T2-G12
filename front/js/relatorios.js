// Laura Carvalho - 25014543

// ===== DADOS (vindos do banco, pelo backend) =====
// Nada aqui e fixo: tudo e carregado de GET /api/plantios e GET /api/atividades
// e as contas (concluidas, pendentes, atrasadas...) sao feitas no navegador.
let plantiosData = [];    // lista de plantios do usuario (PlantioResumo)
let atividadesData = [];  // lista de atividades de todos os plantios (AtividadeResumo)

// Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
// se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

// Estado atual dos filtros ('todos' / 'todas' = sem filtro)
const filtros = {
  periodo: 'todo',
  plantio: 'todos',
  cultura: 'todas',
  talhao: 'todos'
};

// só roda depois que o HTML inteiro carregou
document.addEventListener('DOMContentLoaded', function () {
  setupDropdowns();
  setupExportar();
  carregarDados();
});

// ===== CARREGAMENTO =====
function carregarDados() {
  // usuário logado, salvo no sessionStorage pelo login
  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  // sem usuário logado: avisa e manda pro login depois de 1,5 s
  if (!usuario || !usuario.codigo) {
    mostrarErro('Sua sessão expirou. Entre novamente.');
    setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
    return;
  }

  mostrarErro('Carregando relatório...');

  const codigo = encodeURIComponent(usuario.codigo);
  // faz as duas buscas ao mesmo tempo e só continua quando as duas terminarem
  Promise.all([
    buscarJson(API + '/api/plantios?usuarioCodigo=' + codigo),
    buscarJson(API + '/api/atividades?usuarioCodigo=' + codigo)
  ]).then(function (respostas) {
    const rPlantios = respostas[0];
    const rAtividades = respostas[1];

    if (!rPlantios.sucesso) { mostrarErro(rPlantios.mensagem); return; }
    if (!rAtividades.sucesso) { mostrarErro(rAtividades.mensagem); return; }

    plantiosData = rPlantios.plantios;
    atividadesData = rAtividades.atividades;

    montarFiltros();
    render();

    // render() esconde o aviso; se não tem plantio, mostra de novo
    if (plantiosData.length === 0) {
      mostrarErro('Você ainda não tem plantios. Cadastre um plantio para ver o relatório.');
    }
  });
}

// Faz o GET e sempre devolve um objeto {sucesso, mensagem, ...}, mesmo se der erro de rede
function buscarJson(url) {
  return fetch(url)
    .then(function (resp) {
      // se a resposta não for JSON, cria um erro no mesmo formato
      return resp.json().catch(function () {
        return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
      });
    })
    .catch(function () {
      // fetch falhou (servidor desligado, sem internet...)
      return { sucesso: false, mensagem: 'Não foi possível conectar ao servidor.' };
    });
}

// Mostra uma mensagem no lugar da tabela e esvazia o resto
function mostrarErro(texto) {
  plantiosData = [];
  atividadesData = [];
  render(); // redesenha tudo zerado
  const emptyState = document.getElementById('emptyState');
  emptyState.textContent = texto;
  emptyState.style.display = 'block';
}

// ===== FUNCOES AUXILIARES =====

// Escapa texto digitado pelo usuario antes de colocar no HTML (evita injecao de HTML/JS)
function esc(texto) {
  return String(texto == null ? '' : texto)
    .replace(/&/g, '&amp;') // o & tem que ser o primeiro
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// "Café" -> "cafe" (usado na cor da bolinha da cultura)
function normalizar(texto) {
  // NFD separa a letra do acento, e o replace apaga os acentos
  return String(texto).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

// Date -> "yyyy-MM-dd" (no fuso do computador)
function paraIso(d) {
  // getMonth() começa em 0, por isso o +1; padStart completa com zero
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}

// "2026-10-20" -> "20/10/2026"
function formatarData(iso) {
  const p = iso.split('-');
  return p[2] + '/' + p[1] + '/' + p[0];
}

// código do banco -> texto que aparece na tela
const tipoTexto = {
  PLANTIO: 'Plantio',
  IRRIGACAO: 'Irrigação',
  ADUBACAO: 'Adubação de cobertura',
  TRATAMENTO: 'Tratos culturais',
  MONITORAMENTO: 'Monitoramento',
  COLHEITA: 'Colheita'
};

const meses = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ'];

// Intervalo [inicio, fim] em "yyyy-MM-dd" do periodo escolhido, ou null para "todo o periodo"
function intervaloDoPeriodo(periodo) {
  const hoje = new Date();
  if (periodo === 'mes') {
    // dia 1 do mês até o dia 0 do mês seguinte (= último dia do mês atual)
    return [paraIso(new Date(hoje.getFullYear(), hoje.getMonth(), 1)),
            paraIso(new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0))];
  }
  if (periodo === 'ultimos30') {
    return [paraIso(new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 30)), paraIso(hoje)];
  }
  if (periodo === 'proximos30') {
    return [paraIso(hoje), paraIso(new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + 30))];
  }
  return null;
}

// ===== FILTROS =====

// Preenche os selects de plantio, cultura e talhao com o que existe no banco
function montarFiltros() {
  preencherSelect('filtroPlantio', plantiosData.map(function (p) {
    return { valor: String(p.codigo), texto: p.nome };
  }));
  preencherSelect('filtroCultura', unicos(plantiosData.map(function (p) { return p.cultura; })).map(function (c) {
    return { valor: c, texto: c };
  }));
  preencherSelect('filtroTalhao', unicos(plantiosData.map(function (p) { return p.talhao; })).map(function (t) {
    return { valor: t, texto: t };
  }));
}

// tira os repetidos e ordena (mantém só a primeira ocorrência de cada valor)
function unicos(lista) {
  return lista.filter(function (x, i) { return lista.indexOf(x) === i; }).sort();
}

// Mantem a primeira opcao ("Todos...") e acrescenta as demais
function preencherSelect(id, opcoes) {
  const select = document.getElementById(id);
  while (select.options.length > 1) select.remove(1);
  opcoes.forEach(function (o) {
    const opt = document.createElement('option');
    opt.value = o.valor;
    opt.textContent = o.texto; // textContent: nao interpreta HTML
    select.appendChild(opt);
  });
  select.value = select.options[0].value; // volta pra "Todos"
}

// cada select guarda no data-filter qual filtro ele controla; ao mudar, atualiza e redesenha
function setupDropdowns() {
  document.querySelectorAll('.rel-native-select').forEach(function (select) {
    select.addEventListener('change', function () {
      filtros[select.dataset.filter] = select.value;
      render();
    });
  });
}

// Plantios que passam nos filtros de plantio, cultura e talhao
function getPlantiosFiltrados() {
  return plantiosData.filter(function (p) {
    const okPlantio = filtros.plantio === 'todos' || String(p.codigo) === filtros.plantio;
    const okCultura = filtros.cultura === 'todas' || p.cultura === filtros.cultura;
    const okTalhao = filtros.talhao === 'todos' || p.talhao === filtros.talhao;
    return okPlantio && okCultura && okTalhao;
  });
}

// Atividades dos plantios filtrados; se comPeriodo, so as que caem no periodo escolhido
function getAtividadesFiltradas(plantios, comPeriodo) {
  const codigos = plantios.map(function (p) { return p.codigo; });
  const intervalo = comPeriodo ? intervaloDoPeriodo(filtros.periodo) : null;

  return atividadesData.filter(function (a) {
    if (codigos.indexOf(a.plantioCodigo) === -1) return false; // não é de um plantio filtrado
    if (intervalo && (a.data < intervalo[0] || a.data > intervalo[1])) return false; // "yyyy-MM-dd" compara como texto
    return true;
  });
}

// ===== RENDERIZAÇÃO GERAL =====
// chamada sempre que algo muda: filtra os dados e redesenha cada parte da tela
function render() {
  const plantiosFiltrados = getPlantiosFiltrados();
  const atividadesNoPeriodo = getAtividadesFiltradas(plantiosFiltrados, true);

  renderResumo(plantiosFiltrados, atividadesNoPeriodo);
  renderDonut(atividadesNoPeriodo);
  // as próximas atividades ignoram o período (olham pra frente de qualquer jeito)
  renderProximasAtividades(getAtividadesFiltradas(plantiosFiltrados, false));
  renderTabela(plantiosFiltrados, atividadesNoPeriodo);
}

// ===== RESUMO (cards no topo) =====
function renderResumo(plantios, atividades) {
  const totais = calcularTotais(atividades);
  const totalAtividades = atividades.length;

  // "Plantios ativos" = os que ainda estao em andamento (nao conta os concluidos)
  document.getElementById('numPlantios').textContent = plantios.filter(function (p) { return p.status !== 'CONCLUIDO'; }).length;
  document.getElementById('numTotal').textContent = totalAtividades;
  document.getElementById('numConcluidas').textContent = totais.concluidas;
  document.getElementById('numPendentes').textContent = totais.pendentes;
  document.getElementById('numAtrasadas').textContent = totais.atrasadas;

  document.getElementById('pctConcluidas').textContent = pct(totais.concluidas, totalAtividades) + '% do total';
  document.getElementById('pctPendentes').textContent = pct(totais.pendentes, totalAtividades) + '% do total';
  document.getElementById('pctAtrasadas').textContent = pct(totais.atrasadas, totalAtividades) + '% do total';
}

// Conta as atividades por status (CONCLUIDA, PENDENTE, ATRASADA)
function calcularTotais(atividades) {
  // reduce percorre a lista acumulando os contadores no objeto "acc"
  return atividades.reduce(function (acc, a) {
    if (a.status === 'CONCLUIDA') acc.concluidas++;
    else if (a.status === 'ATRASADA') acc.atrasadas++;
    else acc.pendentes++;
    return acc;
  }, { concluidas: 0, pendentes: 0, atrasadas: 0 });
}

// porcentagem arredondada; evita dividir por zero
function pct(valor, total) {
  if (total === 0) return 0;
  return Math.round((valor / total) * 100);
}

// ===== GRÁFICO DE ROSCA (status das atividades) =====
function renderDonut(atividades) {
  const totais = calcularTotais(atividades);
  const total = atividades.length;

  const donut = document.getElementById('donutChart');
  document.getElementById('donutTotal').textContent = total;

  if (total === 0) {
    donut.style.background = 'conic-gradient(var(--gray-bg) 0 100%)'; // anel todo cinza
  } else {
    // onde cada fatia termina (em %), somando as anteriores
    const fim1 = (totais.concluidas / total) * 100;
    const fim2 = fim1 + (totais.pendentes / total) * 100;
    const fim3 = fim2 + (totais.atrasadas / total) * 100;

    // conic-gradient desenha o anel em fatias; o "furo" vem do CSS
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

  document.getElementById('legendConcluidasPct').textContent = pct(totais.concluidas, total) + '%';
  document.getElementById('legendPendentesPct').textContent = pct(totais.pendentes, total) + '%';
  document.getElementById('legendAtrasadasPct').textContent = pct(totais.atrasadas, total) + '%';
}

// ===== PRÓXIMAS ATIVIDADES =====
// As 5 atividades ainda nao concluidas com as datas mais proximas (as atrasadas aparecem primeiro)
function renderProximasAtividades(atividades) {
  const container = document.getElementById('proximasList');
  container.innerHTML = '';

  // tira as concluídas, ordena por data (mais antiga primeiro) e pega 5
  const proximas = atividades
    .filter(function (a) { return a.status !== 'CONCLUIDA'; })
    .sort(function (x, y) { return x.data < y.data ? -1 : (x.data > y.data ? 1 : 0); })
    .slice(0, 5);

  if (proximas.length === 0) {
    container.innerHTML = '<p class="rel-empty-state">Nenhuma atividade pendente para os filtros selecionados.</p>';
    return;
  }

  proximas.forEach(function (a) {
    const partes = a.data.split('-'); // [ano, mes, dia]
    const status = a.status === 'ATRASADA' ? 'atrasada' : 'pendente'; // vira parte do nome da classe CSS
    const item = document.createElement('div');
    item.className = 'rel-next-item';

    item.innerHTML =
      '<div class="rel-next-date">' +
        '<span class="rel-next-day">' + esc(partes[2]) + '</span>' +
        '<span class="rel-next-month">' + esc(meses[parseInt(partes[1], 10) - 1]) + '</span>' +
      '</div>' +
      '<div class="rel-next-info">' +
        '<p class="rel-next-title">' + esc(tipoTexto[a.tipoAtividade] || a.tipoAtividade) + '</p>' +
        '<p class="rel-next-subtitle">' + esc(a.plantioNome) + ' · ' + esc(a.talhao) + '</p>' +
      '</div>' +
      '<span class="badge-status badge-' + status + '">' + capitalizar(status) + '</span>';

    container.appendChild(item);
  });
}

// "pendente" -> "Pendente"
function capitalizar(texto) {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

// Uma linha da tabela: dados do plantio + contagem das atividades dele no periodo
// (também é usada na exportação, pra tela e CSV mostrarem os mesmos números)
function linhaDoPlantio(p, atividades) {
  const dele = atividades.filter(function (a) { return a.plantioCodigo === p.codigo; });
  const totais = calcularTotais(dele);
  return {
    nome: p.nome,
    cultura: p.cultura,
    talhao: p.talhao,
    dataPlantio: formatarData(p.dataPlantio),
    concluidas: totais.concluidas,
    pendentes: totais.pendentes,
    atrasadas: totais.atrasadas,
    progresso: p.progresso
  };
}

// ===== TABELA DE RESUMO DOS PLANTIOS =====
function renderTabela(plantios, atividades) {
  const tbody = document.getElementById('plantiosTableBody');
  const emptyState = document.getElementById('emptyState');
  tbody.innerHTML = '';

  if (plantios.length === 0) {
    emptyState.textContent = 'Nenhum plantio encontrado para os filtros selecionados.';
    emptyState.style.display = 'block';
    return;
  }
  emptyState.style.display = 'none';

  plantios.forEach(function (p) {
    const l = linhaDoPlantio(p, atividades);
    const tr = document.createElement('tr');

    tr.innerHTML =
      '<td>' +
        '<div class="plantio-culture">' +
          '<span class="culture-dot ' + esc(normalizar(l.cultura)) + '"></span>' +
          '<span class="plantio-name" style="font-size:14px;">' + esc(l.nome) + '</span>' +
        '</div>' +
      '</td>' +
      '<td>' + esc(l.cultura) + '</td>' +
      '<td>' + esc(l.talhao) + '</td>' +
      '<td>' + esc(l.dataPlantio) + '</td>' +
      '<td style="color: var(--accent-green); font-weight:600;">' + l.concluidas + '</td>' +
      '<td style="color: var(--amber); font-weight:600;">' + l.pendentes + '</td>' +
      '<td style="color: var(--red); font-weight:600;">' + l.atrasadas + '</td>' +
      '<td>' +
        '<div class="progress-wrap">' +
          '<span class="progress-percent">' + l.progresso + '%</span>' +
          '<div class="progress-bar">' +
            '<div class="progress-fill" style="width:' + l.progresso + '%;"></div>' +
          '</div>' +
        '</div>' +
      '</td>';

    tbody.appendChild(tr);
  });
}

// ===== EXPORTAR RELATÓRIO (CSV) =====
// Coloca aspas se o texto tiver ; ou " (senao o CSV quebra as colunas)
function csvCampo(valor) {
  const texto = String(valor);
  return /[;"\n]/.test(texto) ? '"' + texto.replace(/"/g, '""') + '"' : texto;
}

function setupExportar() {
  const btn = document.getElementById('btnExportar');
  if (!btn) return;

  btn.addEventListener('click', function () {
    const plantios = getPlantiosFiltrados();

    if (plantios.length === 0) {
      alert('Não há dados para exportar com os filtros selecionados.');
      return;
    }

    const atividades = getAtividadesFiltradas(plantios, true);
    const cabecalho = ['Plantio', 'Cultura', 'Talhão', 'Data do plantio', 'Concluídas', 'Pendentes', 'Atrasadas', 'Progresso (%)'];
    // uma linha de texto por plantio, com as colunas separadas por ";" (padrão do Excel em pt-BR)
    const linhas = plantios.map(function (p) {
      const l = linhaDoPlantio(p, atividades);
      return [l.nome, l.cultura, l.talhao, l.dataPlantio, l.concluidas, l.pendentes, l.atrasadas, l.progresso].map(csvCampo).join(';');
    });

    const csv = [cabecalho.join(';')].concat(linhas).join('\n');
    // o \uFEFF no começo (BOM) faz o Excel entender os acentos (UTF-8)
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    // truque do download: cria um link invisível, clica nele e remove
    const link = document.createElement('a');
    link.href = url;
    link.download = 'relatorio-agromonitor.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url); // libera a memória do arquivo temporário
  });
}