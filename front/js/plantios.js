// Camila Fernandes Costacurta RA:25012949 
document.addEventListener('DOMContentLoaded', function () {
  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const searchInput = document.getElementById('searchPlantio');
  const filterStatus = document.getElementById('filterStatus');
  const filterCultura = document.getElementById('filterCultura');
  const tableBody = document.querySelector('.plantios-table tbody');
  const noResultsRow = document.getElementById('noResultsRow');

  if (!tableBody) return;

  // Status do dicionario de dados -> texto da tela, valor do filtro e classe do CSS
  const statusInfo = {
    FASE_INICIAL: { texto: 'Em fase inicial', filtro: 'inicial', classe: 'status-inicial' },
    DESENVOLVIMENTO: { texto: 'Em desenvolvimento', filtro: 'desenvolvimento', classe: 'status-desenvolvimento' },
    FASE_FINAL: { texto: 'Em fase final', filtro: 'final', classe: 'status-final' },
    CONCLUIDO: { texto: 'Concluído', filtro: 'concluido', classe: 'status-concluido' }
  };

  const atividadeTexto = {
    PLANTIO: 'Plantio',
    IRRIGACAO: 'Irrigação',
    ADUBACAO: 'Adubação de cobertura',
    TRATAMENTO: 'Tratos culturais',
    MONITORAMENTO: 'Monitoramento',
    COLHEITA: 'Colheita'
  };

  // Escapa texto digitado pelo usuario antes de colocar no HTML (evita injecao de HTML/JS)
  function esc(texto) {
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // "Soja", "Café" -> "soja", "cafe" (usado no filtro e na cor da bolinha)
  function normalizar(texto) {
    return String(texto).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  }

  // "2026-10-20" -> "20/10/2026"
  function formatarData(iso) {
    const partes = iso.split('-');
    return partes[2] + '/' + partes[1] + '/' + partes[0];
  }

  // Quantos dias faltam ate a data (negativo = atrasada)
  function diasAte(iso) {
    const partes = iso.split('-').map(Number);
    const alvo = new Date(partes[0], partes[1] - 1, partes[2]);
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    return Math.round((alvo - hoje) / 86400000);
  }

  function textoPrazo(iso) {
    const dias = diasAte(iso);
    if (dias === 0) return 'Hoje';
    if (dias === 1) return 'Amanhã';
    if (dias > 1) return 'Em ' + dias + ' dias';
    return 'Atrasada há ' + Math.abs(dias) + (dias === -1 ? ' dia' : ' dias');
  }

  function montarLinha(p) {
    const st = statusInfo[p.status] || statusInfo.FASE_INICIAL;
    const cultura = normalizar(p.cultura);

    let proxima = '<p class="next-activity">Sem atividades pendentes</p>';
    if (p.proximaAtividadeTipo) {
      proxima =
        '<p class="next-activity">' + esc(atividadeTexto[p.proximaAtividadeTipo] || p.proximaAtividadeTipo) + '</p>' +
        '<p class="next-activity-time">' + esc(textoPrazo(p.proximaAtividadeData)) + '</p>';
    }

    return '<tr data-status="' + esc(st.filtro) + '" data-cultura="' + esc(cultura) + '">' +
      '<td>' +
        '<p class="plantio-name">' + esc(p.nome) + '</p>' +
        '<p class="plantio-culture"><span class="culture-dot ' + esc(cultura) + '"></span>' + esc(p.cultura) + '</p>' +
        '<p class="plantio-meta">Plantio padrão &nbsp;-&nbsp; ' + esc(p.quantidadeAtividades) + ' Atividades</p>' +
      '</td>' +
      '<td>' +
        '<p class="talhao-name">' + esc(p.talhao) + '</p>' +
        '<p class="talhao-area">Área: ' + esc(p.areaPlantada) + ' ha</p>' +
      '</td>' +
      '<td><span class="plantio-date">' + esc(formatarData(p.dataPlantio)) + '</span></td>' +
      '<td>' + proxima + '</td>' +
      '<td>' +
        '<div class="progress-wrap">' +
          '<span class="progress-percent">' + esc(p.progresso) + '%</span>' +
          '<div class="progress-bar"><div class="progress-fill" style="width: ' + esc(p.progresso) + '%;"></div></div>' +
        '</div>' +
      '</td>' +
      '<td><span class="status-pill ' + esc(st.classe) + '">' + esc(st.texto) + '</span></td>' +
    '</tr>';
  }

  function contar(plantios, status) {
    return plantios.filter(function (p) { return p.status === status; }).length;
  }

  function mostrarPlantios(plantios) {
    // "Plantios ativos" = os que ainda estao em andamento (nao conta os concluidos)
    document.getElementById('statTotal').textContent = plantios.length - contar(plantios, 'CONCLUIDO');
    document.getElementById('statInicial').textContent = contar(plantios, 'FASE_INICIAL');
    document.getElementById('statDesenvolvimento').textContent = contar(plantios, 'DESENVOLVIMENTO');
    document.getElementById('statFinal').textContent = contar(plantios, 'FASE_FINAL');

    // troca as linhas antigas pelas novas (a linha "nenhum resultado" fica)
    tableBody.querySelectorAll('tr:not(.no-results-row)').forEach(function (tr) { tr.remove(); });
    noResultsRow.insertAdjacentHTML('beforebegin', plantios.map(montarLinha).join(''));

    aplicarFiltros();
  }

  function mostrarAviso(texto) {
    tableBody.querySelectorAll('tr:not(.no-results-row)').forEach(function (tr) { tr.remove(); });
    noResultsRow.firstElementChild.textContent = texto;
    noResultsRow.style.display = 'table-row';
  }

  function aplicarFiltros() {
    const termo = searchInput.value.trim().toLowerCase();
    const status = filterStatus.value;
    const cultura = filterCultura.value;
    const rows = Array.from(tableBody.querySelectorAll('tr:not(.no-results-row)'));
    let visiveis = 0;

    rows.forEach(function (row) {
      const texto = row.textContent.toLowerCase();
      const okTexto = termo === '' || texto.indexOf(termo) !== -1;
      const okStatus = status === 'todos' || row.dataset.status === status;
      const okCultura = cultura === 'todas' || row.dataset.cultura === cultura;

      const mostrar = okTexto && okStatus && okCultura;
      row.style.display = mostrar ? '' : 'none';
      if (mostrar) visiveis++;
    });

    if (rows.length === 0) return; // aviso de "sem plantios" ja foi mostrado
    noResultsRow.firstElementChild.textContent = 'Nenhum plantio encontrado para os filtros selecionados.';
    noResultsRow.style.display = visiveis === 0 ? 'table-row' : 'none';
  }

  searchInput.addEventListener('input', aplicarFiltros);
  filterStatus.addEventListener('change', aplicarFiltros);
  filterCultura.addEventListener('change', aplicarFiltros);

  // Localiza o botao que limpa os filtros.
  const limparFiltros = document.getElementById('limparFiltros');
  if (limparFiltros) {
    // Executa a limpeza ao clicar no botao.
    limparFiltros.addEventListener('click', function () {
      // Apaga o texto da busca.
      searchInput.value = '';
      // Restaura o filtro de status.
      filterStatus.value = 'todos';
      // Restaura o filtro de cultura.
      filterCultura.value = 'todas';
      // Atualiza a tabela depois da limpeza.
      aplicarFiltros();
      // Volta o foco para a busca.
      searchInput.focus();
    });
  }

  // Links "Ver todos" dos cards de resumo aplicam o filtro de status e rolam até a tabela
  document.querySelectorAll('.stat-link[data-status]').forEach(function (link) {
    link.addEventListener('click', function (e) {
      e.preventDefault();
      filterStatus.value = link.dataset.status;
      aplicarFiltros();
      document.querySelector('.table-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });

  // ---------- busca os plantios do usuario logado ----------
  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  if (!usuario || !usuario.codigo) {
    mostrarAviso('Sua sessão expirou. Entre novamente.');
    setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
    return;
  }

  mostrarAviso('Carregando plantios...');

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
      if (!r.sucesso) {
        mostrarAviso(r.mensagem);
      } else if (r.plantios.length === 0) {
        mostrarPlantios([]);
        mostrarAviso('Você ainda não cadastrou nenhum plantio. Clique em "Novo Plantio" para começar.');
      } else {
        mostrarPlantios(r.plantios);
      }
    });
});
