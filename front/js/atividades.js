// Laura Carvalho - 25014543

// só roda depois que o HTML inteiro carregou
document.addEventListener('DOMContentLoaded', function () {
  // Se o front foi aberto pelo Spring (porta 8080) usa caminho relativo;
  // se foi aberto por outro lugar (ex.: Live Server), aponta para o backend.
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const filterPlantio = document.getElementById('filterPlantioAtiv');
  const filterStatus = document.getElementById('filterStatusAtiv');
  const chkConcluidos = document.getElementById('mostrarPlantiosConcluidos');
  const noResults = document.getElementById('noActivityResults');
  const lista = document.querySelector('.activity-list');

  // se faltar algum elemento, para aqui pra não dar erro
  if (!filterPlantio || !filterStatus || !chkConcluidos || !lista || !noResults) return;

  // código do banco -> texto que aparece na tela
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
      .replace(/&/g, '&amp;') // o & tem que ser o primeiro
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // data de hoje no formato "2026-11-22" (igual ao que vem do backend)
  function hojeIso() {
    const h = new Date();
    // getMonth() começa em 0, por isso o +1; padStart completa com zero ("3" -> "03")
    return h.getFullYear() + '-' + String(h.getMonth() + 1).padStart(2, '0') + '-' + String(h.getDate()).padStart(2, '0');
  }

  // plantio concluído = todas as atividades dele estão concluídas
  let plantiosConcluidos = {};
  let ultimas = []; // última lista recebida, pra redesenhar sem chamar o backend de novo

  // conta total e concluídas de cada plantio; concluído quando os dois são iguais
  function calcularPlantiosConcluidos(atividades) {
    const info = {};
    atividades.forEach(function (a) {
      const i = info[a.plantioCodigo] || (info[a.plantioCodigo] = { total: 0, concluidas: 0 });
      i.total++;
      if (a.status === 'CONCLUIDA') i.concluidas++;
    });
    const r = {};
    Object.keys(info).forEach(function (k) { r[k] = info[k].total === info[k].concluidas; });
    return r;
  }

  // recebe uma atividade e devolve o HTML da linha dela
  function montarLinha(a) {
    const st = statusInfo[a.status] || statusInfo.PENDENTE; // status estranho vira pendente
    const partes = a.data.split('-'); // [ano, mes, dia]
    const hoje = hojeIso();

    // bloco da data: "HOJE" no dia de hoje, senao o dia e o mes
    const ehHoje = a.data === hoje;
    const atrasada = a.status === 'ATRASADA';
    let mes = meses[parseInt(partes[1], 10) - 1]; // "11" -> índice 10 -> "NOV"
    if (partes[0] !== hoje.substring(0, 4)) mes += ' ' + partes[0]; // outro ano: mostra o ano

    // classes CSS mudam conforme seja hoje e/ou atrasada
    const dia = ehHoje ? 'HOJE' : partes[2];
    const diaClasse = 'activity-day' + (ehHoje ? ' is-today' : '') + (atrasada ? ' is-late' : '');
    const mesClasse = 'activity-month' + (atrasada ? ' is-late' : '');
    const mesTexto = ehHoje ? partes[2] + ' ' + mes : mes;

    // &nbsp; = espaço que não quebra linha; quantidade é opcional
    let subtitulo = esc(a.plantioNome) + ' &nbsp;·&nbsp; ' + esc(a.talhao);
    if (a.quantidade) subtitulo += ' &nbsp;·&nbsp; ' + esc(a.quantidade);

    // so atividades ainda nao concluidas ganham o botao
    // os data-* guardam os códigos no próprio botão, pra ler quando clicarem
    const botao = a.status === 'CONCLUIDA' ? '' :
      '<button type="button" class="btn-concluir" data-plantio="' + esc(a.plantioCodigo) +
      '" data-atividade="' + esc(a.codigo) + '" data-titulo="' + esc(tipoTexto[a.tipoAtividade] || a.tipoAtividade) +
      '">Concluir</button>';

    // os data-* da linha são lidos pelos filtros
    return '<div class="activity-row" data-plantio="' + esc(a.plantioCodigo) + '" data-status="' + esc(st.filtro) + '" data-plantio-concluido="' + (plantiosConcluidos[a.plantioCodigo] ? '1' : '0') + '">' +
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

  // todas as linhas que estão na tela agora
  function linhas() {
    return Array.from(lista.querySelectorAll('.activity-row'));
  }

  function limparLinhas() {
    linhas().forEach(function (row) { row.remove(); });
  }

  // mostra uma mensagem no lugar da lista (carregando, erro, vazio...)
  function mostrarAviso(texto) {
    limparLinhas();
    noResults.textContent = texto;
    noResults.style.display = 'block';
  }

  // Coloca no filtro um item para cada plantio que tem atividade (sem repetir)
  function montarFiltroDePlantios(atividades) {
    while (filterPlantio.options.length > 1) filterPlantio.remove(1); // mantém só "Todos"

    const vistos = {};
    atividades.forEach(function (a) {
      if (vistos[a.plantioCodigo]) return;
      // esconde plantios concluídos se o checkbox estiver desmarcado
      if (plantiosConcluidos[a.plantioCodigo] && !chkConcluidos.checked) return;
      vistos[a.plantioCodigo] = true;
      const opt = document.createElement('option');
      opt.value = a.plantioCodigo;
      opt.textContent = a.plantioNome + ' - ' + a.talhao; // textContent: nao interpreta HTML
      filterPlantio.appendChild(opt);
    });
  }

  // esconde/mostra as linhas conforme os filtros (sem buscar nada no backend)
  function aplicarFiltros() {
    const plantio = filterPlantio.value;
    const status = filterStatus.value;
    const todas = linhas();
    let visiveis = 0;

    todas.forEach(function (row) {
      // a linha só aparece se passar nos três filtros
      const okPlantio = plantio === 'todos' || row.dataset.plantio === plantio;
      const okStatus = status === 'todos' || row.dataset.status === status;
      const okConcluido = chkConcluidos.checked || row.dataset.plantioConcluido !== '1';
      const mostrar = okPlantio && okStatus && okConcluido;

      row.style.display = mostrar ? '' : 'none';
      if (mostrar) visiveis++;
    });

    if (todas.length === 0) return; // aviso de "sem atividades" ja foi mostrado
    // se os filtros esconderam tudo, mostra "nenhuma encontrada"
    noResults.textContent = 'Nenhuma atividade encontrada para os filtros selecionados.';
    noResults.style.display = visiveis === 0 ? 'block' : 'none';
  }

  // recebe a lista do backend e desenha tudo na tela
  function mostrarAtividades(atividades) {
    ultimas = atividades;
    plantiosConcluidos = calcularPlantiosConcluidos(atividades);
    limparLinhas();
    montarFiltroDePlantios(atividades);
    // gera o HTML de cada atividade e insere antes do aviso "nenhuma atividade"
    noResults.insertAdjacentHTML('beforebegin', atividades.map(montarLinha).join(''));
    aplicarFiltros();
  }

  filterPlantio.addEventListener('change', aplicarFiltros);
  filterStatus.addEventListener('change', aplicarFiltros);
  // o checkbox muda quais plantios aparecem no select, então redesenha tudo
  chkConcluidos.addEventListener('change', function () {
    filterPlantio.value = 'todos';
    mostrarAtividades(ultimas);
  });

  // confirma com o usuário, manda pro backend e recarrega a lista
  function concluirAtividade(botao) {
    if (!window.confirm('Marcar "' + botao.dataset.titulo + '" como concluída hoje?')) return;

    botao.disabled = true; // evita clique duplo
    fetch(API + '/api/atividades/concluir', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usuarioCodigo: usuario.codigo,
        plantioCodigo: parseInt(botao.dataset.plantio, 10), // data-* vem como texto
        atividadeCodigo: parseInt(botao.dataset.atividade, 10)
      })
    })
      .then(function (resp) {
        // se a resposta não for JSON, cria um erro no mesmo formato
        return resp.json().catch(function () {
          return { sucesso: false, mensagem: 'Resposta inesperada do servidor.' };
        });
      })
      .catch(function () {
        // fetch falhou (servidor desligado, sem internet...)
        return { sucesso: false, mensagem: 'Não foi possível conectar ao servidor.' };
      })
      .then(function (r) {
        // aqui sempre chega { sucesso, mensagem }, deu certo ou não
        if (r.sucesso) {
          carregarAtividades(); // recarrega a lista (a atividade agora aparece como Concluída)
        } else {
          botao.disabled = false;
          window.alert(r.mensagem);
        }
      });
  }

  // um unico "ouvinte" na lista cuida de todos os botoes (inclusive os criados depois)
  // as linhas são recriadas toda hora, então não dá pra pôr um listener em cada botão
  lista.addEventListener('click', function (e) {
    const botao = e.target.closest('.btn-concluir');
    if (botao) concluirAtividade(botao);
  });

  // ---------- busca as atividades do usuario logado ----------
  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (err) {}

  // sem usuário logado: avisa e manda pro login depois de 1,5 s
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
          mostrarAviso(r.mensagem); // erro do backend ou de conexão
        } else if (r.atividades.length === 0) {
          mostrarAviso('Você ainda não tem atividades. Cadastre um plantio ou clique em "Registrar atividade".');
        } else {
          mostrarAtividades(r.atividades);
        }
      });
  }

  // início da página: mostra "Carregando..." e dispara a busca
  mostrarAviso('Carregando atividades...');
  carregarAtividades();
});