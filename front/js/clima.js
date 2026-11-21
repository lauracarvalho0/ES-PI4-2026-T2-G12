// Alertas climaticos gerados a partir da previsao do Open-Meteo para a
// localizacao de cada plantio. Usado pela tela de Alertas e pelo Inicio.
(function () {
  const API = window.location.port === '8080' ? '' : 'http://localhost:8080';
  const CHAVE_LIDOS = 'agromonitor.alertasLidos';

  // Limites dos alertas
  const LIMITE_CHUVA_PROB = 60;   // % de probabilidade
  const LIMITE_CHUVA_MM = 10;     // mm no dia (alem da probabilidade)
  const LIMITE_GEADA = 3;         // °C minima
  const LIMITE_CALOR = 35;        // °C maxima
  const LIMITE_VENTO = 40;        // km/h

  // Open-Meteo entrega no maximo 16 dias de previsao.
  // A previsao completa mostra todos; os alertas so olham os primeiros dias,
  // porque a precisao cai bastante depois disso.
  const DIAS_PREVISAO = 16;
  const DIAS_ALERTA = 7;

  const ESTADOS = {
    AC: 'Acre', AL: 'Alagoas', AP: 'Amapá', AM: 'Amazonas', BA: 'Bahia', CE: 'Ceará',
    DF: 'Distrito Federal', ES: 'Espírito Santo', GO: 'Goiás', MA: 'Maranhão',
    MT: 'Mato Grosso', MS: 'Mato Grosso do Sul', MG: 'Minas Gerais', PA: 'Pará',
    PB: 'Paraíba', PR: 'Paraná', PE: 'Pernambuco', PI: 'Piauí', RJ: 'Rio de Janeiro',
    RN: 'Rio Grande do Norte', RS: 'Rio Grande do Sul', RO: 'Rondônia', RR: 'Roraima',
    SC: 'Santa Catarina', SP: 'São Paulo', SE: 'Sergipe', TO: 'Tocantins'
  };

  const prioridadeOrdem = { 'Crítico': 0, 'Alta': 1, 'Média': 2 };

  function normalizar(t) {
    return String(t == null ? '' : t).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }

  function escaparHtml(texto) {
    return String(texto == null ? '' : texto).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // "2026-10-23" -> Date local a meia-noite (sem deslocamento de fuso)
  function dataLocal(iso) {
    const p = iso.split('-').map(Number);
    return new Date(p[0], p[1] - 1, p[2]);
  }

  function ddmm(d) { return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }); }

  function capitalizar(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

  // ---- lidos (persistidos no navegador) ----
  function lerLidos() {
    try { return new Set(JSON.parse(localStorage.getItem(CHAVE_LIDOS)) || []); }
    catch (e) { return new Set(); }
  }
  function marcarLido(id) {
    try {
      const lidos = lerLidos();
      lidos.add(String(id));
      localStorage.setItem(CHAVE_LIDOS, JSON.stringify([...lidos]));
    } catch (e) { /* sem storage: vale so nesta sessao */ }
  }

  // ---- geocodificacao ("Cidade, UF" ou "Cidade") ----
  const cacheGeo = new Map();

  async function localizar(texto) {
    // Reaproveita coordenadas já consultadas para evitar chamadas repetidas ao serviço geográfico.
    const chave = normalizar(texto);
    if (!cacheGeo.has(chave)) cacheGeo.set(chave, localizarSemCache(texto));
    return cacheGeo.get(chave);
  }

  async function localizarSemCache(texto) {
    const partes = texto.split(/\s*,\s*|\s+-\s+|\s*\/\s*/).map((p) => p.trim()).filter(Boolean);
    const cidade = partes[0];
    const ufTexto = partes[1] ? partes[1].toUpperCase() : '';
    const estado = ESTADOS[ufTexto] || (partes[1] || '');

    const url = 'https://geocoding-api.open-meteo.com/v1/search?name=' + encodeURIComponent(cidade) +
      '&count=10&language=pt&countryCode=BR&format=json';
    const resp = await fetch(url);
    if (!resp.ok) throw new Error('Falha ao localizar ' + texto);
    const json = await resp.json();
    const resultados = json.results || [];
    if (!resultados.length) return null;
    if (estado) {
      const alvo = normalizar(estado);
      const achado = resultados.find((r) => normalizar(r.admin1) === alvo);
      if (achado) return achado;
    }
    return resultados[0];
  }

  // ---- previsao (uma consulta por coordenada) ----
  const cachePrevisao = new Map();

  async function previsao(lugar) {
    const chave = lugar.latitude + ',' + lugar.longitude;
    if (!cachePrevisao.has(chave)) cachePrevisao.set(chave, previsaoSemCache(lugar));
    return cachePrevisao.get(chave);
  }

  async function previsaoSemCache(lugar) {
    const query = new URLSearchParams({
      latitude: lugar.latitude,
      longitude: lugar.longitude,
      daily: 'temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max',
      forecast_days: String(DIAS_PREVISAO),
      timezone: 'auto'
    });
    const resp = await fetch('https://api.open-meteo.com/v1/forecast?' + query.toString());
    if (!resp.ok) throw new Error('Falha ao consultar previsão do tempo');
    const json = await resp.json();
    if (!json.daily || !json.daily.time) throw new Error('Resposta de previsão inválida');
    return json.daily;
  }

  // ---- regras de risco, avaliadas dia a dia ----
  function riscosDoDia(dias, i, local) {
    // Converte os indicadores da previsão diária em riscos que podem virar alertas para o plantio.
    const chuva = Math.round(dias.precipitation_probability_max[i] || 0);
    const mm = Math.round((dias.precipitation_sum[i] || 0) * 10) / 10;
    const min = Math.round(dias.temperature_2m_min[i]);
    const max = Math.round(dias.temperature_2m_max[i]);
    const vento = Math.round(dias.wind_speed_10m_max[i] || 0);
    const loc = escaparHtml(local);
    const r = [];

    if (chuva >= LIMITE_CHUVA_PROB && mm >= LIMITE_CHUVA_MM)
      r.push({ tipo: 'chuva', title: 'Chuva intensa prevista', nivel: mm >= 30 ? 0 : 1,
        desc: `Chance de chuva de ${chuva}% (cerca de ${mm} mm) em ${loc}.`,
        recommendation: 'Evite pulverizações e planeje atividades de campo considerando a chuva prevista.' });
    if (min <= LIMITE_GEADA)
      r.push({ tipo: 'geada', title: 'Risco de geada', nivel: min <= 0 ? 0 : 1,
        desc: `Temperatura mínima prevista de ${min}°C em ${loc}.`,
        recommendation: 'Monitore as áreas mais sensíveis e avalie medidas de proteção contra o frio.' });
    if (max >= LIMITE_CALOR)
      r.push({ tipo: 'calor', title: 'Temperatura muito alta', nivel: max >= 38 ? 0 : 1,
        desc: `Temperatura máxima prevista de ${max}°C em ${loc}.`,
        recommendation: 'Evite atividades nos horários mais quentes e monitore sinais de estresse térmico.' });
    if (vento >= LIMITE_VENTO)
      r.push({ tipo: 'vento', title: 'Ventos fortes previstos', nivel: vento >= 60 ? 0 : 1,
        desc: `Ventos de até ${vento} km/h previstos em ${loc}.`,
        recommendation: 'Adie pulverizações para reduzir deriva e aguarde condições de vento mais fracas.' });
    return r;
  }

  // Junta dias consecutivos do mesmo tipo de risco em um unico alerta.
  function alertasDoPlantio(plantio, lugar, dias) {
    const local = plantio.localizacao.trim();
    const hoje = new Date(); hoje.setHours(0, 0, 0, 0);
    const porTipo = {};
    dias.time.slice(0, DIAS_ALERTA).forEach((_, i) => {
      riscosDoDia(dias, i, local).forEach((r) => {
        const lista = porTipo[r.tipo] = porTipo[r.tipo] || [];
        const ultimo = lista[lista.length - 1];
        if (ultimo && ultimo.fim === i - 1) {
          ultimo.fim = i;
          if (r.nivel < ultimo.risco.nivel) ultimo.risco = r;  // mantem o pior dia
        } else {
          lista.push({ inicio: i, fim: i, risco: r });
        }
      });
    });

    const saida = [];
    Object.keys(porTipo).forEach((tipo) => {
      porTipo[tipo].forEach((ev) => {
        const dIni = dataLocal(dias.time[ev.inicio]);
        const dFim = dataLocal(dias.time[ev.fim]);
        const diff = Math.max(0, Math.round((dIni - hoje) / 86400000));
        const label = diff === 0 ? 'Hoje' : diff === 1 ? 'Amanhã'
          : capitalizar(dIni.toLocaleDateString('pt-BR', { weekday: 'long' }));
        const periodo = ev.fim > ev.inicio ? `${ddmm(dIni)} a ${ddmm(dFim)}` : ddmm(dIni);
        const prioridade = ev.risco.nivel === 0 ? 'Crítico' : 'Alta';
        const relativo = diff === 0 ? 'Hoje' : diff === 1 ? 'Amanhã' : `Em ${diff} dias`;

        // Previsao completa (todos os dias disponiveis); marca os dias do alerta
        const forecast = dias.time.map((iso, k) => {
          const d = dataLocal(iso);
          const diffK = Math.round((d - hoje) / 86400000);
          return {
            label: diffK === 0 ? 'Hoje' : diffK === 1 ? 'Amanhã'
              : capitalizar(d.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')),
            date: ddmm(d),
            tempMax: Math.round(dias.temperature_2m_max[k]),
            tempMin: Math.round(dias.temperature_2m_min[k]),
            rain: Math.round(dias.precipitation_probability_max[k] || 0),
            emAlerta: k >= ev.inicio && k <= ev.fim
          };
        });

        const desc = ev.fim > ev.inicio
          ? `${ev.risco.desc}<br>Condição prevista de ${periodo}.`
          : ev.risco.desc;

        saida.push({
          id: `weather-${plantio.codigo}-${tipo}-${dias.time[ev.inicio]}`,
          plantioCodigo: plantio.codigo,
          title: ev.risco.title, desc, type: 'Climático', culture: plantio.cultura,
          reference: plantio.nome, talhao: plantio.talhao || 'Talhão não informado',
          dateISO: dias.time[ev.inicio], dateDisplay: periodo, dateRelative: relativo,
          priority: prioridade, situacao: 'Não Lido',
          detail: {
            dataAlertaFull: new Date().toLocaleString('pt-BR'),
            origem: 'Previsão do tempo (Open-Meteo)',
            impact: {
              activity: 'Atividades de campo',
              reference: `${plantio.nome} - ${plantio.talhao || 'Talhão'}`,
              dateLabel: `${label} ${periodo}`,
              tone: prioridade === 'Crítico' ? 'red' : 'amber'
            },
            recommendations: [ev.risco.recommendation, 'Confira novamente as condições antes de realizar atividades no campo.'],
            forecast
          }
        });
      });
    });
    return saida;
  }

  // Devolve { alertas, plantios, semLocalizacao, naoEncontrados, falhas }
  async function carregar() {
    // Busca plantios do usuário, consulta previsão por localização e monta os alertas climáticos.
    const resultado = { alertas: [], plantios: [], semLocalizacao: false, naoEncontrados: [], falhas: [] };
    let usuario;
    try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (e) { /* sem sessao */ }
    if (!usuario || !usuario.codigo) return resultado;

    const resp = await fetch(API + '/api/plantios?usuarioCodigo=' + encodeURIComponent(usuario.codigo));
    const dados = await resp.json();
    if (!resp.ok || !dados.sucesso) throw new Error(dados.mensagem || 'Não foi possível carregar os plantios.');

    const ativos = (dados.plantios || []).filter((p) => p.status !== 'CONCLUIDO');
    const comLocal = ativos.filter((p) => p.localizacao && p.localizacao.trim());
    resultado.semLocalizacao = ativos.length > 0 && comLocal.length === 0;

    // Plantios cadastrados (usado para montar os filtros de cultura e talhão)
    resultado.plantios = ativos.map((p) => ({
      nome: p.nome,
      cultura: p.cultura,
      talhao: p.talhao || 'Talhão não informado'
    }));

    const lidos = lerLidos();
    await Promise.all(comLocal.map(async (plantio) => {
      try {
        const lugar = await localizar(plantio.localizacao);
        if (!lugar) { resultado.naoEncontrados.push(plantio.localizacao.trim()); return; }
        const dias = await previsao(lugar);
        resultado.alertas.push(...alertasDoPlantio(plantio, lugar, dias));
      } catch (e) {
        console.warn('Não foi possível consultar o clima de ' + plantio.nome, e);
        resultado.falhas.push(plantio.nome);
      }
    }));

    resultado.alertas.forEach((a) => { if (lidos.has(a.id)) a.situacao = 'Lido'; });
    resultado.naoEncontrados = [...new Set(resultado.naoEncontrados)];

    // O alerta e calculado no navegador; envia o evento ao servidor para disparo
    // do e-mail e deduplicacao entre as telas Inicio e Alertas.
    resultado.alertas.forEach((a) => {
      const descricao = String(a.desc || '').replace(/<br\s*\/?\s*>/gi, ' ').replace(/<[^>]*>/g, ' ');
      fetch(API + '/api/alertas/climaticos/email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          usuarioCodigo: usuario.codigo,
          plantioCodigo: a.plantioCodigo,
          chave: a.id,
          titulo: a.title,
          descricao: descricao,
          prioridade: a.priority,
          data: a.dateISO
        })
      }).catch((erro) => console.warn('Não foi possível registrar o alerta climático para e-mail.', erro));
    });

    return resultado;
  }

  window.Clima = { carregar, marcarLido, prioridadeOrdem, escaparHtml };
})();
