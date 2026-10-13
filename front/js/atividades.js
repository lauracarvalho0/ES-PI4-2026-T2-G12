document.addEventListener('DOMContentLoaded', function () {
  const filterPlantio = document.getElementById('filterPlantioAtiv');
  const filterStatus = document.getElementById('filterStatusAtiv');
  const noResults = document.getElementById('noActivityResults');
  const rows = Array.from(document.querySelectorAll('.activity-row'));

  if (!filterPlantio || !filterStatus) return;

  function aplicarFiltros() {
    const plantio = filterPlantio.value;
    const status = filterStatus.value;
    let visiveis = 0;

    rows.forEach(function (row) {
      const okPlantio = plantio === 'todos' || row.dataset.plantio === plantio;
      const okStatus = status === 'todos' || row.dataset.status === status;
      const mostrar = okPlantio && okStatus;

      row.style.display = mostrar ? '' : 'none';
      if (mostrar) visiveis++;
    });

    if (noResults) {
      noResults.style.display = visiveis === 0 ? 'block' : 'none';
    }
  }

  filterPlantio.addEventListener('change', aplicarFiltros);
  filterStatus.addEventListener('change', aplicarFiltros);
});