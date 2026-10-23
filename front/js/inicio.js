// Tela Inicio: alertas climaticos recentes (usa Clima, de clima.js)
document.addEventListener('DOMContentLoaded', async function () {
  var box = document.getElementById('alertasRecentes');
  var icone = '<svg class="alert-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.5 21.5 20h-19L12 3.5Z"></path><path d="M12 10v4"></path><path d="M12 17.5h.01"></path></svg>';
  try {
    var r = await Clima.carregar();
    var lista = r.alertas.slice().sort(function (a, b) {
      return Clima.prioridadeOrdem[a.priority] - Clima.prioridadeOrdem[b.priority] || a.dateISO.localeCompare(b.dateISO);
    });
    document.getElementById('statAvisosClimaticos').textContent = lista.length;
    document.getElementById('statAlertaImportante').textContent = lista.filter(function (a) { return a.priority === 'Crítico'; }).length;
    if (!lista.length) {
      box.innerHTML = '<p class="alert-text">' + (r.semLocalizacao
        ? 'Cadastre a localização do plantio para receber alertas climáticos.'
        : 'Nenhum alerta climático para os próximos dias.') + '</p>';
      return;
    }
    box.innerHTML = lista.slice(0, 3).map(function (a) {
      return '<div class="alert-box">' + icone + '<div><p class="alert-title">' + Clima.escaparHtml(a.title) +
        '</p><p class="alert-text">' + Clima.escaparHtml(a.reference) + ' · ' + a.dateDisplay + '</p></div></div>';
    }).join('');
  } catch (e) {
    console.error('Falha ao carregar alertas:', e);
    box.innerHTML = '<p class="alert-text">Não foi possível carregar os alertas agora.</p>';
  }
});