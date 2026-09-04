document.addEventListener('DOMContentLoaded', function () {

  // ===== DADOS DE EXEMPLO =====
  // Em produção isso viria da API, indexado por data "YYYY-MM-DD".
  const events = {
    '2025-05-01': [{ title: 'Adubação de cobertura', location: 'Talhão 01 - Milho', category: 'adubacao' }],
    '2025-05-05': [{ title: 'Irrigação', location: 'Talhão 03 - Feijão', category: 'irrigacao' }],
    '2025-05-07': [{ title: 'Inoculação', location: 'Talhão 02 - Soja', category: 'tratamento' }],
    '2025-05-10': [{ title: 'Monitoramento', location: 'Talhão 02 - Soja', category: 'monitoramento' }],
    '2025-05-13': [{ title: 'Aplicação de defensivo', location: 'Talhão 01 - Milho', category: 'tratamento', warning: true }],
    '2025-05-15': [{ title: 'Irrigação', location: 'Talhão 03 - Feijão', category: 'irrigacao' }],
    '2025-05-20': [{ title: 'Adubação de cobertura', location: 'Talhão 01 - Milho', category: 'adubacao' }],
    '2025-05-21': [{ title: 'Inoculação', location: 'Talhão 02 - Soja', category: 'tratamento' }],
    '2025-05-26': [{ title: 'Monitoramento', location: 'Talhão 02 - Soja', category: 'monitoramento' }],
    '2025-05-28': [{ title: 'Irrigação', location: 'Talhão 03 - Feijão', category: 'irrigacao' }],
    '2025-05-30': [{ title: 'Colheita', location: 'Talhão 04 - Algodão', category: 'colheita' }]
  };

  const monthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];
  const monthNamesShort = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
  const weekdayNames = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'];

  // Data de referência inicial (a base de exemplo tem eventos em maio/2025)
  let currentDate = new Date(2025, 4, 1);
  let currentView = 'month';
  let activeCategories = new Set(['adubacao', 'irrigacao', 'tratamento', 'monitoramento', 'colheita']);
  let pickerYear = currentDate.getFullYear();

  const calendarGrid = document.getElementById('calendarGrid');
  const gridView = document.getElementById('gridView');
  const listView = document.getElementById('listView');
  const monthLabelText = document.getElementById('monthLabelText');
  const monthsGrid = document.getElementById('monthsGrid');
  const yearPickerLabel = document.getElementById('yearPickerLabel');
  const monthPicker = document.getElementById('monthPicker');
  const filtersWrap = document.getElementById('filtersWrap');

  function pad(n) {
    return String(n).padStart(2, '0');
  }

  function dateKey(year, month, day) {
    return `${year}-${pad(month + 1)}-${pad(day)}`;
  }

  function isSameDay(a, b) {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  function getEventsFor(year, month, day) {
    const key = dateKey(year, month, day);
    const dayEvents = events[key] || [];
    return dayEvents.filter((ev) => activeCategories.has(ev.category));
  }

  function warningIconSvg() {
    return `<svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.5 2.5 20h19L12 3.5Z"></path><path d="M12 10v4"></path><path d="M12 17h.01"></path></svg>`;
  }

  function buildEventChip(ev) {
    const warning = ev.warning ? warningIconSvg() : '';
    return `
      <div class="calendar-event cat-${ev.category}">
        <p class="calendar-event-title">${warning}<span>${ev.title}</span></p>
        <p class="calendar-event-location"><span class="culture-dot"></span>${ev.location}</p>
      </div>
    `;
  }

  function buildDayCell(year, month, day, isOutside) {
    const today = new Date();
    const cellDate = new Date(year, month, day);
    const dayEvents = getEventsFor(year, month, day);
    const maxVisible = 3;
    const visibleEvents = dayEvents.slice(0, maxVisible);
    const extraCount = dayEvents.length - visibleEvents.length;

    const numberClass = !isOutside && isSameDay(cellDate, today) ? 'calendar-day-number is-today' : 'calendar-day-number';

    return `
      <div class="calendar-day-cell${isOutside ? ' is-outside' : ''}">
        <span class="${numberClass}">${day}</span>
        ${visibleEvents.map(buildEventChip).join('')}
        ${extraCount > 0 ? `<span class="calendar-more-events">+${extraCount} evento${extraCount > 1 ? 's' : ''}</span>` : ''}
      </div>
    `;
  }

  function renderMonthGrid() {
    calendarGrid.classList.remove('is-week-view');
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    const firstDayOfMonth = new Date(year, month, 1);
    const leadingDays = firstDayOfMonth.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const totalCells = Math.ceil((leadingDays + daysInMonth) / 7) * 7;
    const trailingDays = totalCells - leadingDays - daysInMonth;

    const prevMonthDays = new Date(year, month, 0).getDate();

    let html = '';

    for (let i = leadingDays; i > 0; i--) {
      const day = prevMonthDays - i + 1;
      const prevMonthDate = new Date(year, month - 1, day);
      html += buildDayCell(prevMonthDate.getFullYear(), prevMonthDate.getMonth(), day, true);
    }

    for (let day = 1; day <= daysInMonth; day++) {
      html += buildDayCell(year, month, day, false);
    }

    for (let day = 1; day <= trailingDays; day++) {
      const nextMonthDate = new Date(year, month + 1, day);
      html += buildDayCell(nextMonthDate.getFullYear(), nextMonthDate.getMonth(), day, true);
    }

    calendarGrid.innerHTML = html;
  }

  function renderWeekGrid() {
    calendarGrid.classList.add('is-week-view');
    const startOfWeek = new Date(currentDate);
    startOfWeek.setDate(currentDate.getDate() - currentDate.getDay());

    let html = '';
    for (let i = 0; i < 7; i++) {
      const d = new Date(startOfWeek);
      d.setDate(startOfWeek.getDate() + i);
      html += buildDayCell(d.getFullYear(), d.getMonth(), d.getDate(), d.getMonth() !== currentDate.getMonth());
    }
    calendarGrid.innerHTML = html;
  }

  function renderListView() {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const groups = [];
    for (let day = 1; day <= daysInMonth; day++) {
      const dayEvents = getEventsFor(year, month, day);
      if (dayEvents.length > 0) {
        groups.push({ day, events: dayEvents });
      }
    }

    if (groups.length === 0) {
      listView.innerHTML = '<p class="calendar-list-empty">Nenhuma atividade programada para este período.</p>';
      return;
    }

    listView.innerHTML = groups.map((group) => {
      const d = new Date(year, month, group.day);
      const itemsHtml = group.events.map((ev) => `
        <div class="calendar-list-item">
          <span class="calendar-list-item-dot cat-${ev.category}" style="background-color: var(--${categoryColorVar(ev.category)});"></span>
          <div>
            <p class="calendar-list-item-title">${ev.title}</p>
            <p class="calendar-list-item-meta">${ev.location}</p>
          </div>
        </div>
      `).join('');

      return `
        <div class="calendar-list-group">
          <div class="calendar-list-date">
            <span class="calendar-list-day">${pad(group.day)}</span>
            <span class="calendar-list-weekday">${weekdayNames[d.getDay()]}</span>
          </div>
          ${itemsHtml}
        </div>
      `;
    }).join('');
  }

  function categoryColorVar(category) {
    const map = {
      adubacao: 'accent-green',
      irrigacao: 'blue',
      tratamento: 'amber',
      monitoramento: 'purple',
      colheita: 'pink'
    };
    return map[category] || 'text-gray';
  }

  function renderMonthLabel() {
    monthLabelText.textContent = `${monthNames[currentDate.getMonth()]} de ${currentDate.getFullYear()}`;
  }

  function renderMonthsPickerGrid() {
    yearPickerLabel.textContent = pickerYear;
    monthsGrid.innerHTML = monthNamesShort.map((name, index) => {
      const isActive = pickerYear === currentDate.getFullYear() && index === currentDate.getMonth();
      return `<button type="button" class="${isActive ? 'active' : ''}" data-month-index="${index}">${name}</button>`;
    }).join('');
  }

  function render() {
    renderMonthLabel();

    if (currentView === 'list') {
      gridView.style.display = 'none';
      listView.classList.add('active');
      renderListView();
    } else {
      gridView.style.display = 'block';
      listView.classList.remove('active');
      if (currentView === 'week') {
        renderWeekGrid();
      } else {
        renderMonthGrid();
      }
    }
  }

  // ===== NAVEGAÇÃO =====
  document.getElementById('btnPrev').addEventListener('click', function () {
    if (currentView === 'week') {
      currentDate.setDate(currentDate.getDate() - 7);
    } else {
      currentDate.setMonth(currentDate.getMonth() - 1);
    }
    pickerYear = currentDate.getFullYear();
    render();
  });

  document.getElementById('btnNext').addEventListener('click', function () {
    if (currentView === 'week') {
      currentDate.setDate(currentDate.getDate() + 7);
    } else {
      currentDate.setMonth(currentDate.getMonth() + 1);
    }
    pickerYear = currentDate.getFullYear();
    render();
  });

  document.getElementById('btnHoje').addEventListener('click', function () {
    currentDate = new Date();
    pickerYear = currentDate.getFullYear();
    render();
  });

  // ===== TOGGLE DE VISÃO =====
  document.querySelectorAll('.calendar-view-toggle button').forEach((btn) => {
    btn.addEventListener('click', function () {
      document.querySelectorAll('.calendar-view-toggle button').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      currentView = btn.dataset.view;
      render();
    });
  });

  // ===== SELETOR DE MÊS/ANO =====
  document.getElementById('monthLabel').addEventListener('click', function (e) {
    e.stopPropagation();
    pickerYear = currentDate.getFullYear();
    renderMonthsPickerGrid();
    monthPicker.classList.toggle('is-open');
    filtersWrap.classList.remove('is-open');
  });

  document.getElementById('btnYearPrev').addEventListener('click', function (e) {
    e.stopPropagation();
    pickerYear -= 1;
    renderMonthsPickerGrid();
  });

  document.getElementById('btnYearNext').addEventListener('click', function (e) {
    e.stopPropagation();
    pickerYear += 1;
    renderMonthsPickerGrid();
  });

  monthsGrid.addEventListener('click', function (e) {
    const btn = e.target.closest('button[data-month-index]');
    if (!btn) return;
    const monthIndex = parseInt(btn.dataset.monthIndex, 10);
    currentDate = new Date(pickerYear, monthIndex, 1);
    monthPicker.classList.remove('is-open');
    render();
  });

  // ===== FILTROS =====
  document.getElementById('btnFiltros').addEventListener('click', function (e) {
    e.stopPropagation();
    filtersWrap.classList.toggle('is-open');
    monthPicker.classList.remove('is-open');
  });

  document.querySelectorAll('.calendar-filter-option input').forEach((checkbox) => {
    checkbox.addEventListener('change', function () {
      if (checkbox.checked) {
        activeCategories.add(checkbox.value);
      } else {
        activeCategories.delete(checkbox.value);
      }
      render();
    });
  });

  // Fecha os dropdowns ao clicar fora
  document.addEventListener('click', function () {
    monthPicker.classList.remove('is-open');
    filtersWrap.classList.remove('is-open');
  });

  render();
});