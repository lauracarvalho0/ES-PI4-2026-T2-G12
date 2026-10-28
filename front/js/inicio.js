// Resumo da tela inicial: busca dados do usuario e monta os cards, a linha do
// tempo e os alertas com informacoes reais do servidor e da previsao do tempo.
document.addEventListener('DOMContentLoaded', function () {
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';
  const timeline = document.getElementById('proximasAtividades');
  const alertasBox = document.getElementById('alertasRecentes');
  const subtitle = document.querySelector('.main-subtitle');
  if (subtitle) {
    subtitle.textContent = 'Resumo de ' + new Date().toLocaleDateString('pt-BR', {
      weekday: 'long', day: 'numeric', month: 'long'
    }) + '.';
  }
  const tiposAtividade = {
    PLANTIO: 'Plantio', IRRIGACAO: 'Irrigação', ADUBACAO: 'Adubação de cobertura',
    TRATAMENTO: 'Tratos culturais', MONITORAMENTO: 'Monitoramento', COLHEITA: 'Colheita'
  };
  const prioridadeOrdem = { 'Crítico': 0, 'Alta': 1, 'Média': 2 };

  function escapar(texto) {
    return String(texto == null ? '' : texto).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function hojeISO() {
    const hoje = new Date();
    return hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0') + '-' +
      String(hoje.getDate()).padStart(2, '0');
  }

  function diferencaDias(iso) {
    const partes = iso.split('-').map(Number);
    const data = new Date(partes[0], partes[1] - 1, partes[2]);
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    return Math.round((data - hoje) / 86400000);
  }

  function dataCurta(iso) {
    const partes = iso.split('-');
    return { dia: partes[2], mes: new Date(Number(partes[0]), Number(partes[1]) - 1, 1)
      .toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '') };
  }

  async function buscarJson(url) {
    const resposta = await fetch(url);
    const dados = await resposta.json().catch(function () { return {}; });
    if (!resposta.ok || !dados.sucesso) {
      throw new Error(dados.mensagem || 'Não foi possível carregar os dados.');
    }
    return dados;
  }

  function montarTimeline(atividades) {
    const pendentes = atividades.filter(function (a) {
      return a.status !== 'CONCLUIDA' && a.data;
    }).sort(function (a, b) {
      return a.data.localeCompare(b.data) || a.codigo - b.codigo;
    }).slice(0, 4);

    if (!pendentes.length) {
      timeline.innerHTML = '<p class="alert-text">Você não tem atividades pendentes. Cadastre um plantio para começar.</p>';
      return;
    }

    timeline.innerHTML = pendentes.map(function (atividade, indice) {
      const diff = diferencaDias(atividade.data);
      const data = dataCurta(atividade.data);
      const hoje = diff === 0;
      const atrasada = atividade.status === 'ATRASADA' || diff < 0;
      const marcador = hoje ? 'is-today' : (atrasada || diff <= 2 ? 'is-amber' : 'is-blue');
      const classeBadge = hoje ? 'badge-green' : (atrasada || diff <= 2 ? 'badge-amber' : 'badge-blue');
      const prazo = atrasada ? 'Atrasada há ' + Math.abs(diff) + (Math.abs(diff) === 1 ? ' dia' : ' dias')
        : hoje ? 'Hoje' : diff === 1 ? 'Amanhã' : 'Em ' + diff + ' dias';
      const nomeAtividade = tiposAtividade[atividade.tipoAtividade] || atividade.tipoAtividade || 'Atividade';
      const talhao = [atividade.plantioNome, atividade.talhao].filter(Boolean).join(' - ');
      return '<div class="timeline-item">' +
        '<div class="timeline-marker"><div class="timeline-dot ' + marcador + '"></div>' +
        (indice < pendentes.length - 1 ? '<div class="timeline-line"></div>' : '') +
        '<span class="timeline-date"><span class="timeline-day">' + (hoje ? 'Hoje' : escapar(data.dia)) +
        '</span><span class="timeline-month">' + escapar(data.mes) + '</span></span></div>' +
        '<div class="timeline-content"><div><p class="timeline-activity">' + escapar(nomeAtividade) +
        '</p><p class="timeline-detail">' + escapar(talhao || 'Plantio não informado') + '</p></div>' +
        '<span class="badge ' + classeBadge + '">' + escapar(prazo) + '</span></div></div>';
    }).join('');
  }

  function normalizarAlertaAtividade(alerta) {
    const prioridade = { CRITICO: 'Crítico', ALTA: 'Alta', MEDIA: 'Média' }[alerta.prioridade] || 'Média';
    return {
      title: alerta.titulo || 'Alerta de atividade',
      reference: [alerta.plantioNome, alerta.talhao].filter(Boolean).join(' · '),
      dateISO: alerta.dataAtividade || '',
      dateDisplay: alerta.dataAtividade ? new Date(alerta.dataAtividade + 'T12:00:00').toLocaleDateString('pt-BR') : '',
      priority: prioridade,
      situacao: alerta.situacao
    };
  }

  function montarAlertas(alertas, erro) {
    const icone = '<svg class="alert-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.5 21.5 20h-19L12 3.5Z"></path><path d="M12 10v4"></path><path d="M12 17.5h.01"></path></svg>';
    const ordenar = function (a, b) {
      return prioridadeOrdem[a.priority] - prioridadeOrdem[b.priority] || a.dateISO.localeCompare(b.dateISO);
    };
    const abertos = alertas.filter(function (a) { return a.situacao !== 'Lido' && a.situacao !== 'LIDO'; });
    const importantes = abertos.filter(function (a) { return a.priority === 'Crítico'; }).length;
    document.getElementById('statAlertaImportante').textContent = String(importantes);
    const notificationDot = document.getElementById('notificationDot');
    if (notificationDot) notificationDot.hidden = abertos.length === 0;

    if (!alertas.length) {
      alertasBox.innerHTML = '<p class="alert-text">' + escapar(erro || 'Nenhum alerta recente. Tudo tranquilo por aqui!') + '</p>';
      return;
    }

    alertasBox.innerHTML = alertas.slice().sort(ordenar).slice(0, 3).map(function (alerta) {
      const quando = alerta.dateDisplay ? ' · ' + alerta.dateDisplay : '';
      return '<div class="alert-box">' + icone + '<div><p class="alert-title">' + escapar(alerta.title) +
        '</p><p class="alert-text">' + escapar(alerta.reference || 'Alerta do sistema') + escapar(quando) + '</p></div></div>';
    }).join('');
  }

  let usuario = null;
  try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (e) { /* sessao ausente */ }
  if (!usuario || !usuario.codigo) {
    timeline.innerHTML = '<p class="alert-text">Sua sessão expirou. Entre novamente.</p>';
    alertasBox.innerHTML = '<p class="alert-text">Sua sessão expirou. Entre novamente.</p>';
    setTimeout(function () { window.location.href = '../auth/login.html'; }, 1500);
    return;
  }

  const codigoUsuario = encodeURIComponent(usuario.codigo);
  const carregamentos = Promise.allSettled([
    buscarJson(API + '/api/plantios?usuarioCodigo=' + codigoUsuario),
    buscarJson(API + '/api/atividades?usuarioCodigo=' + codigoUsuario),
    buscarJson(API + '/api/alertas?usuarioCodigo=' + codigoUsuario),
    window.Clima ? window.Clima.carregar() : Promise.resolve({ alertas: [], semLocalizacao: false })
  ]);

  carregamentos.then(function (resultados) {
    const [plantiosR, atividadesR, alertasR, climaR] = resultados;
    if (plantiosR.status === 'fulfilled') {
      const ativos = (plantiosR.value.plantios || []).filter(function (p) { return p.status !== 'CONCLUIDO'; });
      document.getElementById('statPlantiosAtivos').textContent = String(ativos.length);
    } else {
      document.getElementById('statPlantiosAtivos').textContent = '—';
      console.error('Falha ao carregar plantios:', plantiosR.reason);
    }

    if (atividadesR.status === 'fulfilled') {
      const atividades = atividadesR.value.atividades || [];
      document.getElementById('statAtividadesHoje').textContent = String(atividades.filter(function (a) {
        return a.data === hojeISO() && a.status !== 'CONCLUIDA';
      }).length);
      montarTimeline(atividades);
    } else {
      document.getElementById('statAtividadesHoje').textContent = '—';
      timeline.innerHTML = '<p class="alert-text">Não foi possível carregar suas atividades. Confira se o servidor está ativo e atualize a página.</p>';
      console.error('Falha ao carregar atividades:', atividadesR.reason);
    }

    const alertasAtividade = alertasR.status === 'fulfilled'
      ? (alertasR.value.alertas || []).map(normalizarAlertaAtividade) : [];
    const alertasClima = climaR.status === 'fulfilled' ? (climaR.value.alertas || []) : [];
    document.getElementById('statAvisosClimaticos').textContent = climaR.status === 'fulfilled'
      ? String(alertasClima.length) : '—';

    const mensagensErro = [];
    if (alertasR.status === 'rejected') {
      mensagensErro.push('Não foi possível carregar os alertas de atividades.');
      console.error('Falha ao carregar alertas de atividades:', alertasR.reason);
    }
    if (climaR.status === 'rejected') {
      mensagensErro.push('Não foi possível carregar os alertas climáticos.');
      console.error('Falha ao carregar alertas climáticos:', climaR.reason);
    }
    montarAlertas(alertasAtividade.concat(alertasClima), mensagensErro.join(' '));
  });
});
