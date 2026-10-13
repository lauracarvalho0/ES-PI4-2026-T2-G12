document.addEventListener('DOMContentLoaded', function () {
  const searchInput = document.getElementById('searchPlantio');
  const filterStatus = document.getElementById('filterStatus');
  const filterCultura = document.getElementById('filterCultura');
  const tableBody = document.querySelector('.plantios-table tbody');
  const noResultsRow = document.getElementById('noResultsRow');

  if (!tableBody) return;

  const rows = Array.from(tableBody.querySelectorAll('tr:not(.no-results-row)'));

  function aplicarFiltros() {
    const termo = searchInput.value.trim().toLowerCase();
    const status = filterStatus.value;
    const cultura = filterCultura.value;
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

    noResultsRow.style.display = visiveis === 0 ? 'table-row' : 'none';
  }

  searchInput.addEventListener('input', aplicarFiltros);
  filterStatus.addEventListener('change', aplicarFiltros);
  filterCultura.addEventListener('change', aplicarFiltros);

  // Links "Ver todos" dos cards de resumo aplicam o filtro de status e rolam até a tabela
  document.querySelectorAll('.stat-link[data-status]').forEach(function (link) {
    link.addEventListener('click', function (e) {
      e.preventDefault();
      filterStatus.value = link.dataset.status;
      aplicarFiltros();
      document.querySelector('.table-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
});