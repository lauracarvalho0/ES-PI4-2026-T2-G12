document.addEventListener('DOMContentLoaded', function () {
  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const filterPlantio = document.getElementById('filterPlantioAtiv');
  const filterStatus = document.getElementById('filterStatusAtiv');
  const noResults = document.getElementById('noActivityResults');
  const lista = document.querySelector('.activity-list');

  if (!filterPlantio || !filterStatus || !lista || !noResults) return;

  const tipoTexto = {
    PLANTIO: 'Plantio',
    IRRIGACAO: 'Irrigação',
    ADUBACAO: 'Adubação de cobertura',
    TRATAMENTO: 'Tratos culturais',
    MONITORAMENTO: 'Monitoramento',
    COLHEITA: 'Colheita'
  };

  // Status do dicionario de dados -> texto da tela, valor do filtro e classe do CSS
  const statusInfo = {
    CONCLUIDA: { texto: 'Concluída', filtro: 'concluida', classe: 'badge-concluida' },
    PENDENTE: { texto: 'Pendente', filtro: 'pendente', classe: 'badge-pendente' },
    ATRASADA: { texto: 'Atrasada', filtro: 'atrasada', classe: 'badge-atrasada' }
  };

  const meses = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ'];

  // Escapa texto digitado pelo usuario antes de colocar no HTML (evita injecao de HTML/JS)
  function esc(texto) {
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function hojeIso() {
    const h = new Date();
    return h.getFullYear() + '-' + String(h.getMonth() + 1).padStart(2, '0') + '-' + String(h.getDate()).padStart(2, '0');
  }

  function montarLinha(a) {
    const st = statusInfo[a.status] || statusInfo.PENDENTE;
    const partes = a.data.split('-'); // [ano, mes, dia]
    const hoje = hojeIso();

    // bloco da data: "HOJE" no dia de hoje, senao o dia e o mes
    const ehHoje = a.data === hoje;
    const atrasada = a.status === 'ATRASADA';
    let mes = meses[parseInt(partes[1], 10) - 1];
    if (partes[0] !== hoje.substring(0, 4)) mes += ' ' + partes[0]; // outro ano: mostra o ano

    const dia = ehHoje ? 'HOJE' : partes[2];
    const diaClasse = 'activity-day' + (ehHoje ? ' is-today' : '') + (atrasada ? ' is-late' : '');
    const mesClasse = 'activity-month' + (atrasada ? ' is-late' : '');
    const mesTexto = ehHoje ? partes[2] + ' ' + mes : mes;

    let subtitulo = esc(a.plantioNome) + ' &nbsp;·&nbsp; ' + esc(a.talhao);
    if (a.quantidade) subtitulo += ' &nbsp;·&nbsp; ' + esc(a.quantidade);

    // so atividades ainda nao concluidas ganham o botao
    const botao = a.status === 'CONCLUIDA' ? '' :
      '<button type="button" class="btn-concluir" data-plantio="' + esc(a.plantioCodigo) +
      '" data-atividade="' + esc(a.codigo) + '" data-titulo="' + esc(tipoTexto[a.tipoAtividade] || a.tipoAtividade) +
      '">Concluir</button>';

    return '<div class="activity-row" data-plantio="' + esc(a.plantioCodigo) + '" data-status="' + esc(st.filtro) + '">' +
      '<div class="activity-date">' +
        '<span class="' + diaClasse + '">' + esc(dia) + '</span>' +
        '<span class="' + mesClasse + '">' + esc(mesTexto) + '</span>' +
      '</div>' +
      '<div class="activity-info">' +
        '<p class="activity-title">' + esc(tipoTexto[a.tipoAtividade] || a.tipoAtividade) + '</p>' +
        '<p class="activity-subtitle">' + subtitulo + '</p>' +
      '</div>' +
      botao +
      '<span class="badge-status ' + esc(st.classe) + '">' + esc(st.texto) + '</span>' +
    '</div>';
  }

  function linhas() {
    return Array.from(lista.querySelectorAll('.activity-row'));
  }

  function limparLinhas() {
    linhas().forEach(function (row) { row.remove(); });
  }

  function mostrarAviso(texto) {
    limparLinhas();
    noResults.textContent = texto;
    noResults.style.display = 'block';
  }

  // Coloca no filtro um item para cada plantio que tem atividade (sem repetir)
  function montarFiltroDePlantios(atividades) {
    while (filterPlantio.options.length > 1) filterPlantio.remove(1);

    const vistos = {};
    atividades.forEach(function (a) {
      if (vistos[a.plantioCodigo]) return;
      vistos[a.plantioCodigo] = true;
      const opt = document.createElement('option');
      opt.value = a.plantioCodigo;
      opt.textContent = a.plantioNome + ' - ' + a.talhao; // textContent: nao interpreta HTML
      filterPlantio.appendChild(opt);
    });
  }

  function aplicarFiltros() {
    const plantio = filterPlantio.value;
    const status = filterStatus.value;
    const todas = linhas();
    let visiveis = 0;

    todas.forEach(function (row) {
      const okPlantio = plantio === 'todos' || row.dataset.plantio === plantio;
      const okStatus = status === 'todos' || row.dataset.status === status;
      const mostrar = okPlantio && okStatus;

      row.style.display = mostrar ? '' : 'none';
      if (mostrar) visiveis++;
    });

    if (todas.length === 0) return; // aviso de "sem atividades" ja foi mostrado
    noResults.textContent = 'Nenhuma atividade encontrada para os filtros selecionados.';
    noResults.style.display = visiveis === 0 ? 'block' : 'none';
  }

  function mostrarAtividades(atividades) {
    limparLinhas();
    montarFiltroDePlantios(atividades);
    noResults.insertAdjacentHTML('beforebegin', atividades.map(montarLinha).join(''));
    aplicarFiltros();
  }

  filterPlantio.addEventListener('change', aplicarFiltros);
  filterStatus.addEventListener('change', aplicarFiltros);

  function concluirAtividade(botao) {
    if (!window.confirm('Marcar "' + botao.dataset.titulo + '" como concluída hoje?')) return;

    botao.disabled = true;
    fetch(API + '/api/atividades/concluir', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usuarioCodigo: usuario.codigo,
        plantioCodigo: parseInt(botao.dataset.plantio, 10),
        atividadeCodigo: parseInt(botao.dataset.atividade, 10)
      })
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
        if (r.sucesso) {
          carregarAtividades(); // recarrega a lista (a atividade agora aparece como Concluída)
        } else {
          botao.disabled = false;
          window.alert(r.mensagem);
        }
      });
  }

  // um unico "ouvinte" na lista cuida de todos os botoes (inclusive os criados depois)
  lista.addEventListener('click', function (e) {
    const botao = e.target.closest('.btn-concluir');
    if (botao) concluirAtividade(botao);
  });

  // ---------- busca as atividades do usuario logado ----------
  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  if (!usuario || !usuario.codigo) {
    mostrarAviso('Sua sessão expirou. Entre novamente.');
    setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
    return;
  }

  function carregarAtividades() {
    fetch(API + '/api/atividades?usuarioCodigo=' + encodeURIComponent(usuario.codigo))
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
        } else if (r.atividades.length === 0) {
          mostrarAviso('Você ainda não tem atividades. Cadastre um plantio ou clique em "Registrar atividade".');
        } else {
          mostrarAtividades(r.atividades);
        }
      });
  }

  mostrarAviso('Carregando atividades...');
  carregarAtividades();
});
