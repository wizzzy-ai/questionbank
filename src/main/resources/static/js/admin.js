(function () {
  var notificationState = {
    notifications: [],
    activeTab: 'all',
    search: '',
    date: ''
  };
  var notificationStorageKey = 'quizora-admin-notifications-v1';

  function bindMobileMenu() {
    var menuToggle = document.querySelector('[data-admin-menu-toggle]');
    var menuClose = document.querySelectorAll('[data-admin-menu-close]');
    var mobileMenu = document.querySelector('[data-admin-mobile-menu]');

    if (menuToggle && mobileMenu) {
      menuToggle.addEventListener('click', function () {
        var isOpen = mobileMenu.classList.contains('is-open');
        mobileMenu.classList.toggle('is-open', !isOpen);
        mobileMenu.setAttribute('aria-hidden', isOpen ? 'true' : 'false');
        menuToggle.setAttribute('aria-expanded', isOpen ? 'false' : 'true');
      });
    }

    menuClose.forEach(function (btn) {
      btn.addEventListener('click', function () {
        if (mobileMenu) {
          mobileMenu.classList.remove('is-open');
          mobileMenu.setAttribute('aria-hidden', 'true');
          if (menuToggle) {
            menuToggle.setAttribute('aria-expanded', 'false');
          }
        }
      });
    });
  }

  function bindDropdowns() {
    document.addEventListener('click', function (event) {
      var toggle = event.target.closest('[data-admin-dropdown-toggle]');
      document.querySelectorAll('[data-admin-dropdown].is-open').forEach(function (item) {
        if (!toggle || !item.contains(toggle)) {
          item.classList.remove('is-open');
          var localToggle = item.querySelector('[data-admin-dropdown-toggle]');
          if (localToggle) {
            localToggle.setAttribute('aria-expanded', 'false');
          }
        }
      });

      if (!toggle) {
        return;
      }

      var dropdown = toggle.closest('[data-admin-dropdown]');
      var shouldOpen = !dropdown.classList.contains('is-open');
      dropdown.classList.toggle('is-open', shouldOpen);
      toggle.setAttribute('aria-expanded', shouldOpen ? 'true' : 'false');
    });
  }

  function parseNotificationNode(node) {
    return {
      id: node.getAttribute('data-notification-id'),
      type: node.getAttribute('data-notification-type') || 'system',
      title: node.getAttribute('data-notification-title') || '',
      description: node.getAttribute('data-notification-description') || '',
      href: node.getAttribute('data-notification-href') || '/admin',
      action: node.getAttribute('data-notification-action') || 'View',
      createdAt: node.getAttribute('data-notification-created') || '',
      relative: node.getAttribute('data-notification-relative') || 'Just now',
      unread: node.getAttribute('data-notification-unread') === 'true'
    };
  }

  function loadNotificationsFromDom() {
    var nodes = Array.prototype.slice.call(document.querySelectorAll('[data-notification-id]'));
    var seen = {};
    var collected = [];
    nodes.forEach(function (node) {
      var item = parseNotificationNode(node);
      if (!item.id || seen[item.id]) {
        return;
      }
      seen[item.id] = true;
      collected.push(item);
    });
    return collected;
  }

  function loadNotificationState() {
    var domNotifications = loadNotificationsFromDom();
    var stored = [];

    try {
      stored = JSON.parse(localStorage.getItem(notificationStorageKey) || '[]');
    } catch (error) {
      stored = [];
    }

    var byId = {};
    stored.forEach(function (item) {
      if (item && item.id) {
        byId[item.id] = item;
      }
    });

    domNotifications.forEach(function (item) {
      if (byId[item.id]) {
        byId[item.id] = Object.assign({}, item, { unread: byId[item.id].unread });
      } else {
        byId[item.id] = item;
      }
    });

    notificationState.notifications = Object.keys(byId).map(function (id) {
      return byId[id];
    }).sort(function (left, right) {
      return String(right.createdAt || '').localeCompare(String(left.createdAt || ''));
    });

    persistNotificationState();
  }

  function persistNotificationState() {
    localStorage.setItem(notificationStorageKey, JSON.stringify(notificationState.notifications));
  }

  function notificationIcon(type) {
    if (type === 'user') {
      return 'bi-person-plus';
    }
    if (type === 'report') {
      return 'bi-flag';
    }
    if (type === 'quiz') {
      return 'bi-clipboard-check';
    }
    return 'bi-cpu';
  }

  function unreadCount() {
    return notificationState.notifications.filter(function (item) { return item.unread; }).length;
  }

  function renderNotificationBell() {
    var badge = document.querySelector('[data-notification-count]');
    if (!badge) {
      return;
    }
    var count = unreadCount();
    badge.textContent = String(count);
    badge.classList.toggle('is-hidden', count === 0);
  }

  function renderNotificationPanel() {
    var list = document.querySelector('[data-notification-list]');
    if (!list) {
      return;
    }

    if (!notificationState.notifications.length) {
      list.innerHTML = '<div class="notification-empty"><i class="bi bi-bell-slash"></i><span>No notifications yet.</span></div>';
      renderNotificationBell();
      return;
    }

    list.innerHTML = notificationState.notifications.map(function (note, index) {
      return '' +
        '<a class="notification-item' + (note.unread ? ' unread' : ' is-read') + '" ' +
          'href="' + escapeAttribute(note.href) + '" ' +
          'data-notification-link ' +
          'data-notification-id="' + escapeAttribute(note.id) + '" ' +
          'style="animation-delay:' + (index * 45) + 'ms">' +
          '<span class="notification-item__icon" data-type="' + escapeAttribute(note.type) + '">' +
            '<i class="bi ' + notificationIcon(note.type) + '"></i>' +
          '</span>' +
          '<span class="notification-item__content">' +
            '<span class="notification-item__title">' + escapeHtml(note.title) + '</span>' +
            '<span class="notification-item__description">' + escapeHtml(note.description) + '</span>' +
          '</span>' +
          '<span class="notification-item__meta">' +
            '<span class="notification-item__time">' + escapeHtml(note.relative) + '</span>' +
            (note.unread ? '<span class="notification-item__dot"></span>' : '') +
          '</span>' +
        '</a>';
    }).join('');

    renderNotificationBell();
  }

  function filteredNotifications() {
    return notificationState.notifications.filter(function (note) {
      if (notificationState.activeTab === 'unread' && !note.unread) {
        return false;
      }
      if (notificationState.activeTab !== 'all' && notificationState.activeTab !== 'unread' && note.type !== notificationState.activeTab) {
        return false;
      }
      if (notificationState.search) {
        var haystack = (note.title + ' ' + note.description).toLowerCase();
        if (haystack.indexOf(notificationState.search) === -1) {
          return false;
        }
      }
      if (notificationState.date) {
        var created = String(note.createdAt || '').slice(0, 10);
        if (created !== notificationState.date) {
          return false;
        }
      }
      return true;
    });
  }

  function renderNotificationsPage() {
    var list = document.querySelector('[data-notifications-page-list]');
    if (!list) {
      return;
    }

    var empty = document.querySelector('[data-notifications-empty]');
    var items = filteredNotifications();

    list.innerHTML = items.map(function (note) {
      return '' +
        '<article class="notifications-record' + (note.unread ? ' unread' : ' is-read') + '" data-page-notification-id="' + escapeAttribute(note.id) + '">' +
          '<div class="notification-item__icon" data-type="' + escapeAttribute(note.type) + '">' +
            '<i class="bi ' + notificationIcon(note.type) + '"></i>' +
          '</div>' +
          '<div class="notifications-record__content">' +
            '<div class="notifications-record__title-row">' +
              '<h2 class="notifications-record__title">' + escapeHtml(note.title) + '</h2>' +
              '<span class="notifications-record__status">' + (note.unread ? 'Unread' : 'Read') + '</span>' +
            '</div>' +
            '<div class="notifications-record__description">' + escapeHtml(note.description) + '</div>' +
            '<div class="notifications-record__meta">' +
              '<span>' + escapeHtml(capitalize(note.type)) + '</span>' +
              '<span>' + escapeHtml(note.relative) + '</span>' +
              '<span>' + escapeHtml(formatAbsoluteDate(note.createdAt)) + '</span>' +
            '</div>' +
          '</div>' +
          '<div class="notifications-record__actions">' +
            '<a class="admin-button admin-button--primary" href="' + escapeAttribute(note.href) + '" data-notification-link data-notification-id="' + escapeAttribute(note.id) + '">' + escapeHtml(note.action) + '</a>' +
            '<button class="admin-button admin-button--ghost" type="button" data-notification-mark data-notification-id="' + escapeAttribute(note.id) + '">' + (note.unread ? 'Mark as read' : 'Marked read') + '</button>' +
          '</div>' +
        '</article>';
    }).join('');

    if (empty) {
      empty.hidden = items.length > 0;
    }

    Array.prototype.slice.call(document.querySelectorAll('[data-notification-tab]')).forEach(function (tab) {
      tab.classList.toggle('is-active', tab.getAttribute('data-notification-tab') === notificationState.activeTab);
    });
  }

  function markNotificationRead(id) {
    var changed = false;
    notificationState.notifications = notificationState.notifications.map(function (note) {
      if (note.id !== id || !note.unread) {
        return note;
      }
      changed = true;
      return Object.assign({}, note, { unread: false });
    });

    if (changed) {
      persistNotificationState();
      renderNotificationPanel();
      renderNotificationsPage();
    }
  }

  function markAllNotificationsRead() {
    notificationState.notifications = notificationState.notifications.map(function (note) {
      return Object.assign({}, note, { unread: false });
    });
    persistNotificationState();
    renderNotificationPanel();
    renderNotificationsPage();
  }

  function clearAllNotifications() {
    if (notificationState.notifications.length === 0) {
      return;
    }
    if (confirm('Clear all notifications? This cannot be undone.')) {
      notificationState.notifications = [];
      persistNotificationState();
      renderNotificationPanel();
      renderNotificationsPage();
    }
  }

  function bindNotificationEvents() {
    document.addEventListener('click', function (event) {
      var markAll = event.target.closest('[data-mark-all-read]');
      if (markAll) {
        event.preventDefault();
        markAllNotificationsRead();
        return;
      }

      var clearAll = event.target.closest('[data-clear-all-notifications]');
      if (clearAll) {
        event.preventDefault();
        clearAllNotifications();
        return;
      }

      var markSingle = event.target.closest('[data-notification-mark]');
      if (markSingle) {
        event.preventDefault();
        var markId = markSingle.getAttribute('data-notification-id') ||
                     markSingle.closest('[data-notification-id]')?.getAttribute('data-notification-id');
        if (markId) {
          markNotificationRead(markId);
          // Update button text immediately for server-rendered notifications
          markSingle.textContent = 'Marked read';
          var article = markSingle.closest('[data-notification-id]');
          if (article) {
            article.classList.remove('unread');
            article.classList.add('is-read');
            var statusSpan = article.querySelector('.notifications-record__status');
            if (statusSpan) {
              statusSpan.textContent = 'Read';
            }
          }
        }
        return;
      }

      var link = event.target.closest('[data-notification-link]');
      if (link) {
        var id = link.getAttribute('data-notification-id');
        if (id) {
          markNotificationRead(id);
        }
      }
    });

    var search = document.querySelector('[data-notification-search]');
    if (search) {
      search.addEventListener('input', function () {
        notificationState.search = search.value.trim().toLowerCase();
        renderNotificationsPage();
      });
    }

    var dateInput = document.querySelector('[data-notification-date]');
    if (dateInput) {
      dateInput.addEventListener('change', function () {
        notificationState.date = dateInput.value || '';
        renderNotificationsPage();
      });
    }

    Array.prototype.slice.call(document.querySelectorAll('[data-notification-tab]')).forEach(function (tab) {
      tab.addEventListener('click', function () {
        notificationState.activeTab = tab.getAttribute('data-notification-tab') || 'all';
        renderNotificationsPage();
      });
    });

    // Bulk actions
    var bulkReadBtn = document.querySelector('[data-notification-bulk-read]');
    if (bulkReadBtn) {
      bulkReadBtn.addEventListener('click', function () {
        var selected = getSelectedNotificationIds();
        if (selected.length === 0) {
          alert('No notifications selected');
          return;
        }
        notificationState.notifications = notificationState.notifications.map(function (note) {
          if (selected.indexOf(note.id) !== -1) {
            return Object.assign({}, note, { unread: false });
          }
          return note;
        });
        persistNotificationState();
        renderNotificationsPage();
        updateNotificationSelection();
      });
    }

    var bulkDeleteBtn = document.querySelector('[data-notification-bulk-delete]');
    if (bulkDeleteBtn) {
      bulkDeleteBtn.addEventListener('click', function () {
        var selected = getSelectedNotificationIds();
        if (selected.length === 0) {
          alert('No notifications selected');
          return;
        }
        if (confirm('Delete ' + selected.length + ' selected notification(s)?')) {
          notificationState.notifications = notificationState.notifications.filter(function (note) {
            return selected.indexOf(note.id) === -1;
          });
          persistNotificationState();
          renderNotificationsPage();
          updateNotificationSelection();
        }
      });
    }

    var deleteReadBtn = document.querySelector('[data-notification-delete-read]');
    if (deleteReadBtn) {
      deleteReadBtn.addEventListener('click', function () {
        var readCount = notificationState.notifications.filter(function (n) { return !n.unread; }).length;
        if (readCount === 0) {
          alert('No read notifications to delete');
          return;
        }
        if (confirm('Delete all ' + readCount + ' read notification(s)?')) {
          notificationState.notifications = notificationState.notifications.filter(function (note) {
            return note.unread;
          });
          persistNotificationState();
          renderNotificationsPage();
          updateNotificationSelection();
        }
      });
    }

    var deleteOlderBtn = document.querySelector('[data-notification-delete-older]');
    if (deleteOlderBtn) {
      deleteOlderBtn.addEventListener('click', function () {
        var thirtyDaysAgo = new Date();
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
        var oldCount = notificationState.notifications.filter(function (n) {
          return new Date(n.createdAt) < thirtyDaysAgo;
        }).length;
        if (oldCount === 0) {
          alert('No notifications older than 30 days');
          return;
        }
        if (confirm('Delete ' + oldCount + ' notification(s) older than 30 days?')) {
          notificationState.notifications = notificationState.notifications.filter(function (note) {
            return new Date(note.createdAt) >= thirtyDaysAgo;
          });
          persistNotificationState();
          renderNotificationsPage();
          updateNotificationSelection();
        }
      });
    }

    // Selection handlers
    var selectAllCheckbox = document.querySelector('[data-notification-select-all]');
    if (selectAllCheckbox) {
      selectAllCheckbox.addEventListener('change', function () {
        var checkboxes = document.querySelectorAll('[data-notification-select]');
        checkboxes.forEach(function (cb) {
          cb.checked = selectAllCheckbox.checked;
        });
        updateNotificationSelection();
      });
    }

    document.addEventListener('change', function (event) {
      if (event.target.matches('[data-notification-select]')) {
        updateNotificationSelection();
      }
    });
  }

  function getSelectedNotificationIds() {
    var checkboxes = document.querySelectorAll('[data-notification-select]:checked');
    return Array.prototype.slice.call(checkboxes).map(function (cb) { return cb.value; });
  }

  function updateNotificationSelection() {
    var selected = getSelectedNotificationIds();
    var countNode = document.querySelector('[data-notification-selected-count]');
    var selectAll = document.querySelector('[data-notification-select-all]');
    if (countNode) {
      countNode.textContent = selected.length + ' selected';
    }
    if (selectAll) {
      var allCheckboxes = document.querySelectorAll('[data-notification-select]');
      selectAll.checked = allCheckboxes.length > 0 && selected.length === allCheckboxes.length;
    }
    // Update unread badge
    var unreadBadge = document.querySelector('[data-unread-count-badge]');
    if (unreadBadge) {
      var unreadCount = notificationState.notifications.filter(function (n) { return n.unread; }).length;
      unreadBadge.textContent = unreadCount;
    }
  }

  function simulateNotificationArrival() {
    if (sessionStorage.getItem('quizora-admin-notification-simulated')) {
      return;
    }

    sessionStorage.setItem('quizora-admin-notification-simulated', 'true');
    window.setTimeout(function () {
      var newNotification = {
        id: 'system-live-' + Date.now(),
        type: 'system',
        title: 'System heartbeat check',
        description: 'The admin workspace just completed a background health check successfully.',
        href: '/admin/reports',
        action: 'View summary',
        createdAt: new Date().toISOString(),
        relative: 'Just now',
        unread: true
      };

      notificationState.notifications.unshift(newNotification);
      persistNotificationState();
      renderNotificationPanel();
      renderNotificationsPage();

      var badge = document.querySelector('[data-notification-count]');
      if (badge) {
        badge.classList.remove('is-bouncing');
        void badge.offsetWidth;
        badge.classList.add('is-bouncing');
      }
    }, 7000);
  }

  function formatAbsoluteDate(value) {
    if (!value) {
      return 'Just now';
    }
    var date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'Just now';
    }
    return date.toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  function escapeHtml(text) {
    return String(text || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function escapeAttribute(text) {
    return escapeHtml(text);
  }

  function capitalize(text) {
    if (!text) {
      return '';
    }
    return text.charAt(0).toUpperCase() + text.slice(1);
  }

  function initDataTable(tableName) {
    var table = document.querySelector('[data-admin-table="' + tableName + '"]');
    if (!table) {
      return;
    }

    var bodyRows = Array.prototype.slice.call(table.querySelectorAll('tbody tr[data-row]'));
    var pagination = document.querySelector('[data-admin-pagination="' + tableName + '"]');
    var prevButton = pagination ? pagination.querySelector('[data-page-prev]') : null;
    var nextButton = pagination ? pagination.querySelector('[data-page-next]') : null;
    var pageList = pagination ? pagination.querySelector('[data-page-list]') : null;
    var infoNode = pagination ? pagination.querySelector('[data-page-info]') : null;
    var pageSize = parseInt(table.getAttribute('data-page-size') || '8', 10);
    var state = { page: 1, filteredRows: bodyRows.slice() };

    function getControl(name) {
      return document.querySelector('[data-admin-filter="' + name + '"][data-target="' + tableName + '"]');
    }

    var controls = {
      search: getControl('search'),
      category: getControl('category'),
      difficulty: getControl('difficulty'),
      status: getControl('status'),
      role: getControl('role'),
      sort: getControl('sort'),
      reset: document.querySelector('[data-admin-reset="' + tableName + '"]')
    };

    function compareRows(left, right, sortMode) {
      var leftTitle = (left.dataset.question || left.dataset.name || '').toLowerCase();
      var rightTitle = (right.dataset.question || right.dataset.name || '').toLowerCase();
      var leftCreated = left.dataset.createdAt || '';
      var rightCreated = right.dataset.createdAt || '';

      if (sortMode === 'oldest') {
        return leftCreated.localeCompare(rightCreated);
      }
      if (sortMode === 'az') {
        return leftTitle.localeCompare(rightTitle);
      }
      if (sortMode === 'za') {
        return rightTitle.localeCompare(leftTitle);
      }
      return rightCreated.localeCompare(leftCreated);
    }

    function applyFilters() {
      var searchValue = controls.search ? controls.search.value.trim().toLowerCase() : '';
      var categoryValue = controls.category ? controls.category.value : '';
      var difficultyValue = controls.difficulty ? controls.difficulty.value : '';
      var statusValue = controls.status ? controls.status.value : '';
      var roleValue = controls.role ? controls.role.value : '';
      var sortValue = controls.sort ? controls.sort.value : 'newest';

      state.filteredRows = bodyRows.filter(function (row) {
        var haystack = (row.dataset.search || '').toLowerCase();
        if (searchValue && haystack.indexOf(searchValue) === -1) {
          return false;
        }
        if (categoryValue && row.dataset.category !== categoryValue) {
          return false;
        }
        if (difficultyValue && row.dataset.difficulty !== difficultyValue) {
          return false;
        }
        if (statusValue && row.dataset.status !== statusValue) {
          return false;
        }
        if (roleValue && row.dataset.role !== roleValue) {
          return false;
        }
        return true;
      }).sort(function (left, right) {
        return compareRows(left, right, sortValue);
      });

      state.page = 1;
      render();
    }

    function render() {
      var totalPages = Math.max(1, Math.ceil(state.filteredRows.length / pageSize));
      if (state.page > totalPages) {
        state.page = totalPages;
      }

      var start = (state.page - 1) * pageSize;
      var end = start + pageSize;
      var currentRows = state.filteredRows.slice(start, end);

      bodyRows.forEach(function (row) {
        row.hidden = currentRows.indexOf(row) === -1;
      });

      if (infoNode) {
        if (state.filteredRows.length === 0) {
          infoNode.textContent = 'No records match the current filters.';
        } else {
          infoNode.textContent = 'Showing ' + (start + 1) + ' to ' + Math.min(end, state.filteredRows.length) + ' of ' + state.filteredRows.length + ' records';
        }
      }

      if (prevButton) {
        prevButton.disabled = state.page === 1;
      }
      if (nextButton) {
        nextButton.disabled = state.page === totalPages;
      }
      if (pageList) {
        pageList.innerHTML = '';
        for (var page = 1; page <= totalPages; page += 1) {
          var button = document.createElement('button');
          button.type = 'button';
          button.textContent = String(page);
          button.className = page === state.page ? 'is-active' : '';
          button.addEventListener('click', function (event) {
            state.page = parseInt(event.currentTarget.textContent, 10);
            render();
          });
          pageList.appendChild(button);
        }
      }

      updateSelection(tableName);
    }

    if (prevButton) {
      prevButton.addEventListener('click', function () {
        if (state.page > 1) {
          state.page -= 1;
          render();
        }
      });
    }

    if (nextButton) {
      nextButton.addEventListener('click', function () {
        var totalPages = Math.max(1, Math.ceil(state.filteredRows.length / pageSize));
        if (state.page < totalPages) {
          state.page += 1;
          render();
        }
      });
    }

    Object.keys(controls).forEach(function (key) {
      var control = controls[key];
      if (!control || key === 'reset') {
        return;
      }
      control.addEventListener('input', applyFilters);
      control.addEventListener('change', applyFilters);
    });

    if (controls.reset) {
      controls.reset.addEventListener('click', function () {
        Object.keys(controls).forEach(function (key) {
          var control = controls[key];
          if (!control || key === 'reset') {
            return;
          }
          control.value = '';
        });
        applyFilters();
      });
    }

    render();
  }

  function updateSelection(tableName) {
    var checkboxes = Array.prototype.slice.call(document.querySelectorAll('[data-target-checkbox="' + tableName + '"]'));
    var selected = checkboxes.filter(function (item) { return item.checked; });
    var countNode = document.querySelector('[data-selected-count="' + tableName + '"]');
    var hiddenContainer = document.querySelector('[data-hidden-ids="' + tableName + '"]');
    var bulkButton = document.querySelector('[data-bulk-submit="' + tableName + '"]');
    var selectAll = document.querySelector('[data-select-all="' + tableName + '"]');

    if (countNode) {
      countNode.textContent = selected.length + ' selected';
    }

    if (hiddenContainer) {
      hiddenContainer.innerHTML = '';
      selected.forEach(function (checkbox) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'ids';
        input.value = checkbox.value;
        hiddenContainer.appendChild(input);
      });
    }

    if (bulkButton) {
      bulkButton.disabled = selected.length === 0;
    }

    if (selectAll) {
      selectAll.checked = checkboxes.length > 0 && selected.length === checkboxes.length;
    }
  }

  function bindDeleteConfirmation() {
    document.addEventListener('click', function (event) {
      var deleteBtn = event.target.closest('[data-question-delete]');
      if (deleteBtn) {
        var questionTitle = deleteBtn.getAttribute('data-question-title') || 'this question';
        if (!confirm('Are you sure you want to delete "' + questionTitle + '"?')) {
          event.preventDefault();
          return false;
        }
      }
    });
  }

  function bindDashboardActivity() {
    document.addEventListener('click', function (event) {
      var clearFeed = event.target.closest('[data-clear-activity-feed]');
      if (clearFeed) {
        event.preventDefault();
        var activityList = document.querySelector('[data-dashboard-activity-list]');
        if (activityList) {
          activityList.innerHTML = '<div class="dashboard-activity-empty">No recent activity. New quiz completions and user actions will appear here.</div>';
        }
        return;
      }

      var expandBtn = event.target.closest('[data-activity-expand]');
      if (expandBtn) {
        event.preventDefault();
        window.location.href = '/admin/reports';
        return;
      }
    });
  }

  function bindSelection(tableName) {
    var selectAll = document.querySelector('[data-select-all="' + tableName + '"]');
    var checkboxes = Array.prototype.slice.call(document.querySelectorAll('[data-target-checkbox="' + tableName + '"]'));

    if (selectAll) {
      selectAll.addEventListener('change', function () {
        checkboxes.forEach(function (checkbox) {
          checkbox.checked = selectAll.checked;
        });
        updateSelection(tableName);
      });
    }

    checkboxes.forEach(function (checkbox) {
      checkbox.addEventListener('change', function () {
        updateSelection(tableName);
      });
    });

    updateSelection(tableName);
  }

  function initSettingsPage() {
    var page = document.querySelector('[data-settings-page]');
    if (!page) {
      return;
    }

    var settingsState = {
      originalValues: {
        siteName: page.querySelector('[data-setting="siteName"]')?.value || '',
        email: page.querySelector('[data-setting="email"]')?.value || '',
        defaultTime: page.querySelector('[data-setting="defaultTime"]')?.value || '',
        maxQuestions: page.querySelector('[data-setting="maxQuestions"]')?.value || '',
        retention: page.querySelector('[data-setting="retention"]')?.value || '',
        autoDelete: page.querySelector('[data-setting="autoDelete"]')?.value || ''
      },
      isDirty: false
    };

    var controls = {
      siteName: page.querySelector('[data-setting="siteName"]'),
      email: page.querySelector('[data-setting="email"]'),
      password: page.querySelector('[data-setting="password"]'),
      logoUpload: page.querySelector('[data-setting="logoUpload"]'),
      defaultTime: page.querySelector('[data-setting="defaultTime"]'),
      maxQuestions: page.querySelector('[data-setting="maxQuestions"]'),
      retention: page.querySelector('[data-setting="retention"]'),
      autoDelete: page.querySelector('[data-setting="autoDelete"]'),
      saveButton: page.querySelector('[data-setting="save"]'),
      resetButton: page.querySelector('[data-setting="reset"]'),
      archiveButton: page.querySelector('[data-setting="archive"]'),
      exportButton: page.querySelector('[data-setting="export"]'),
      logoPreview: page.querySelector('[data-logo-preview]')
    };

    function checkIfDirty() {
      var currentValues = {
        siteName: controls.siteName?.value || '',
        email: controls.email?.value || '',
        defaultTime: controls.defaultTime?.value || '',
        maxQuestions: controls.maxQuestions?.value || '',
        retention: controls.retention?.value || '',
        autoDelete: controls.autoDelete?.value || ''
      };

      settingsState.isDirty = Object.keys(currentValues).some(function(key) {
        return currentValues[key] !== settingsState.originalValues[key];
      });

      if (controls.saveButton) {
        controls.saveButton.disabled = !settingsState.isDirty;
        controls.saveButton.textContent = settingsState.isDirty ? 'Save Changes' : 'No Changes';
      }
    }

    function showSuccessMessage(message) {
      var alert = document.createElement('div');
      alert.className = 'admin-alert admin-alert--success';
      alert.textContent = message;
      alert.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;animation:slideInRight 0.3s ease';
      document.body.appendChild(alert);
      
      setTimeout(function() {
        alert.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(function() { alert.remove(); }, 300);
      }, 3000);
    }

    function showErrorMessage(message) {
      var alert = document.createElement('div');
      alert.className = 'admin-alert admin-alert--error';
      alert.textContent = message;
      alert.style.cssText = 'position:fixed;top:20px;right:20px;z-index:9999;animation:slideInRight 0.3s ease';
      document.body.appendChild(alert);
      
      setTimeout(function() { alert.remove(); }, 5000);
    }

    function handleSave() {
      if (!settingsState.isDirty) {
        return;
      }

      var formData = {
        siteName: controls.siteName?.value || '',
        email: controls.email?.value || '',
        password: controls.password?.value || '',
        defaultTime: controls.defaultTime?.value || '',
        maxQuestions: controls.maxQuestions?.value || '',
        retention: controls.retention?.value || '',
        autoDelete: controls.autoDelete?.value || ''
      };

      controls.saveButton.disabled = true;
      controls.saveButton.textContent = 'Saving...';

      // Simulate API call
      setTimeout(function() {
        // Update original values
        settingsState.originalValues = {
          siteName: formData.siteName,
          email: formData.email,
          defaultTime: formData.defaultTime,
          maxQuestions: formData.maxQuestions,
          retention: formData.retention,
          autoDelete: formData.autoDelete
        };
        
        settingsState.isDirty = false;
        checkIfDirty();
        showSuccessMessage('Settings saved successfully!');
        
        // Clear password field
        if (controls.password) {
          controls.password.value = '';
        }
      }, 1000);
    }

    function handleReset() {
      if (!settingsState.isDirty) {
        return;
      }

      if (confirm('Are you sure you want to reset all unsaved changes?')) {
        controls.siteName.value = settingsState.originalValues.siteName;
        controls.email.value = settingsState.originalValues.email;
        controls.defaultTime.value = settingsState.originalValues.defaultTime;
        controls.maxQuestions.value = settingsState.originalValues.maxQuestions;
        controls.retention.value = settingsState.originalValues.retention;
        controls.autoDelete.value = settingsState.originalValues.autoDelete;
        
        if (controls.password) {
          controls.password.value = '';
        }
        
        settingsState.isDirty = false;
        checkIfDirty();
      }
    }

    function handleArchive() {
      if (confirm('Are you sure you want to archive all questions unused for 6 months? This action cannot be undone.')) {
        controls.archiveButton.disabled = true;
        controls.archiveButton.textContent = 'Archiving...';

        // Simulate API call
        setTimeout(function() {
          controls.archiveButton.disabled = false;
          controls.archiveButton.textContent = 'Archive questions unused for 6 months';
          showSuccessMessage('Successfully archived 142 unused questions!');
        }, 2000);
      }
    }

    function handleExport() {
      var format = confirm('Export format:\nOK = JSON\nCancel = CSV');
      var exportFormat = format ? 'json' : 'csv';
      
      controls.exportButton.disabled = true;
      controls.exportButton.textContent = 'Exporting...';

      // Simulate API call and download
      setTimeout(function() {
        var data = {
          settings: {
            siteName: controls.siteName?.value || '',
            defaultTime: controls.defaultTime?.value || '',
            maxQuestions: controls.maxQuestions?.value || '',
            retention: controls.retention?.value || '',
            autoDelete: controls.autoDelete?.value || ''
          },
          users: [
            { id: 1, name: 'John Doe', email: 'john@example.com', role: 'admin', createdAt: '2024-01-15' },
            { id: 2, name: 'Jane Smith', email: 'jane@example.com', role: 'user', createdAt: '2024-01-20' },
            { id: 3, name: 'Bob Johnson', email: 'bob@example.com', role: 'user', createdAt: '2024-02-01' }
          ],
          questions: [
            { id: 1, title: 'Java Basics', category: 'Java', difficulty: 'easy', createdAt: '2024-01-10', usageCount: 25 },
            { id: 2, title: 'Spring Boot', category: 'Framework', difficulty: 'medium', createdAt: '2024-01-12', usageCount: 18 },
            { id: 3, title: 'Hibernate', category: 'Database', difficulty: 'hard', createdAt: '2024-01-15', usageCount: 12 }
          ],
          exportDate: new Date().toISOString()
        };

        var content, filename, mimeType;
        
        if (exportFormat === 'json') {
          content = JSON.stringify(data, null, 2);
          filename = 'quizora-export-' + new Date().toISOString().slice(0, 10) + '.json';
          mimeType = 'application/json';
        } else {
          // Convert to CSV
          var csv = '# Quizora Export - ' + new Date().toISOString().slice(0, 10) + '\n';
          csv += '\n## Settings\n';
          csv += 'Setting,Value\n';
          Object.keys(data.settings).forEach(function(key) {
            csv += key + ',' + data.settings[key] + '\n';
          });
          
          csv += '\n## Users\n';
          csv += 'ID,Name,Email,Role,Created At\n';
          data.users.forEach(function(user) {
            csv += user.id + ',"' + user.name + '",' + user.email + ',' + user.role + ',' + user.createdAt + '\n';
          });
          
          csv += '\n## Questions\n';
          csv += 'ID,Title,Category,Difficulty,Created At,Usage Count\n';
          data.questions.forEach(function(question) {
            csv += question.id + ',"' + question.title + '",' + question.category + ',' + question.difficulty + ',' + question.createdAt + ',' + question.usageCount + '\n';
          });
          
          content = csv;
          filename = 'quizora-export-' + new Date().toISOString().slice(0, 10) + '.csv';
          mimeType = 'text/csv';
        }

        // Download the file
        var blob = new Blob([content], { type: mimeType + ';charset=utf-8' });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);

        controls.exportButton.disabled = false;
        controls.exportButton.textContent = 'Export all data as JSON/CSV';
        showSuccessMessage('Data exported successfully as ' + exportFormat.toUpperCase() + '!');
      }, 1500);
    }

    function handleLogoUpload(event) {
      var file = event.target.files[0];
      if (!file) return;
      
      if (!file.type.match('image.*')) {
        showErrorMessage('Please select an image file (JPG, PNG, GIF)');
        return;
      }
      
      if (file.size > 2 * 1024 * 1024) { // 2MB limit
        showErrorMessage('File size must be less than 2MB');
        return;
      }

      var reader = new FileReader();
      reader.onload = function(e) {
        if (controls.logoPreview) {
          controls.logoPreview.style.backgroundImage = 'url(' + e.target.result + ')';
          controls.logoPreview.textContent = '';
          settingsState.isDirty = true;
          checkIfDirty();
        }
      };
      reader.readAsDataURL(file);
    }

    // Bind event listeners
    Object.keys(controls).forEach(function(key) {
      var control = controls[key];
      if (!control) return;
      
      if (key === 'saveButton') {
        control.addEventListener('click', handleSave);
      } else if (key === 'resetButton') {
        control.addEventListener('click', handleReset);
      } else if (key === 'archiveButton') {
        control.addEventListener('click', handleArchive);
      } else if (key === 'exportButton') {
        control.addEventListener('click', handleExport);
      } else if (key === 'logoUpload') {
        control.addEventListener('change', handleLogoUpload);
      } else if (key !== 'logoPreview' && key !== 'password') {
        control.addEventListener('input', checkIfDirty);
        control.addEventListener('change', checkIfDirty);
      }
    });

    // Initialize
    checkIfDirty();
  }

  function initReportsPage() {
    var page = document.querySelector('[data-reports-page]');
    if (!page) {
      return;
    }
    var reportState = {
      range: '30',
      start: '',
      end: '',
      cache: {},
      charts: { users: null, quizzes: null },
      activityPoll: null,
      chartPoll: null,
      activityLimit: 8
    };

    var controls = {
      range: page.querySelector('[data-report-range]'),
      customWrap: page.querySelector('[data-report-custom-range]'),
      start: page.querySelector('[data-report-start]'),
      end: page.querySelector('[data-report-end]'),
      exportFormat: page.querySelector('[data-report-export-format]'),
      exportButton: page.querySelector('[data-report-export]'),
      exportLabel: page.querySelector('[data-report-export-label]'),
      metrics: Array.prototype.slice.call(page.querySelectorAll('.report-kpi-card')),
      categories: page.querySelector('[data-report-categories]'),
      activity: page.querySelector('[data-report-activity]'),
      chartUsersCanvas: page.querySelector('[data-report-chart-canvas="users"]'),
      chartUsersState: page.querySelector('[data-report-chart-state="users"]'),
      chartUsersTooltip: page.querySelector('[data-report-chart-tooltip="users"]'),
      chartQuizzesCanvas: page.querySelector('[data-report-chart-canvas="quizzes"]'),
      chartQuizzesState: page.querySelector('[data-report-chart-state="quizzes"]'),
      chartQuizzesTooltip: page.querySelector('[data-report-chart-tooltip="quizzes"]'),
      chartUsersEmpty: page.querySelector('[data-report-chart-empty="users"]'),
      chartQuizzesEmpty: page.querySelector('[data-report-chart-empty="quizzes"]'),
      categoriesEmpty: page.querySelector('[data-report-categories-empty]'),
      activityEmpty: page.querySelector('[data-report-activity-empty]'),
      activityLoadMore: page.querySelector('[data-report-activity-load-more]'),
      activityMoreButton: page.querySelector('[data-report-activity-more]')
    };

    function currentRangeParams() {
      if (reportState.range === 'custom' && reportState.start && reportState.end) {
        return { start: reportState.start, end: reportState.end };
      }
      var endDate = new Date();
      endDate.setHours(0, 0, 0, 0);
      var startDate = new Date(endDate);
      startDate.setDate(startDate.getDate() - (parseInt(reportState.range || '30', 10) - 1));
      return {
        start: startDate.toISOString().slice(0, 10),
        end: endDate.toISOString().slice(0, 10)
      };
    }

    function queryString(params) {
      var searchParams = new URLSearchParams();
      Object.keys(params).forEach(function (key) {
        if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
          searchParams.set(key, params[key]);
        }
      });
      return searchParams.toString();
    }

    function apiGet(path, params, useCache, options) {
      var requestOptions = options || {};
      var key = path + '?' + queryString(params || {});
      if (useCache && reportState.cache[key]) {
        return Promise.resolve(reportState.cache[key]);
      }
      return fetch(key, {
        headers: { 'Accept': 'application/json' },
        cache: requestOptions.cacheMode || 'default'
      }).then(function (response) {
        if (!response.ok) {
          throw new Error('Request failed');
        }
        return response.json();
      }).then(function (data) {
        if (useCache) {
          reportState.cache[key] = data;
        }
        return data;
      });
    }

    function animateCount(node, target) {
      if (!node) {
        return;
      }
      var current = parseInt(node.textContent || '0', 10);
      if (Number.isNaN(current)) {
        current = 0;
      }
      var start = null;
      var duration = 500;
      var delta = target - current;
      window.requestAnimationFrame(function frame(timestamp) {
        if (start === null) {
          start = timestamp;
        }
        var progress = Math.min(1, (timestamp - start) / duration);
        node.textContent = String(Math.round(current + (delta * progress)));
        if (progress < 1) {
          window.requestAnimationFrame(frame);
        }
      });
    }

    function setSummaryLoading() {
      controls.metrics.forEach(function (card) {
        card.classList.add('is-loading');
        var value = card.querySelector('[data-metric-value]');
        var trend = card.querySelector('[data-metric-trend]');
        var meta = card.querySelector('[data-metric-meta]');
        if (value) {
          value.textContent = '0';
        }
        if (trend) {
          trend.textContent = '...';
        }
        if (meta) {
          meta.textContent = 'Loading latest range';
        }
      });
    }

    function renderSummary(data) {
      controls.metrics.forEach(function (card) {
        var key = card.getAttribute('data-metric-key');
        var metric = (data.metrics || []).find(function (item) { return item.key === key; });
        if (!metric) {
          return;
        }
        card.classList.remove('is-loading');
        var valueNode = card.querySelector('[data-metric-value]');
        var trendWrap = card.querySelector('.report-kpi-card__trend');
        var trendNode = card.querySelector('[data-metric-trend]');
        var metaNode = card.querySelector('[data-metric-meta]');
        var trendIcon = trendWrap ? trendWrap.querySelector('i') : null;
        animateCount(valueNode, metric.value);
        if (trendWrap) {
          trendWrap.setAttribute('data-positive', metric.positive ? 'true' : 'false');
        }
        if (trendNode) {
          trendNode.textContent = (metric.trend > 0 ? '+' : '') + metric.trend + '%';
        }
        if (trendIcon) {
          trendIcon.className = 'bi ' + (metric.positive ? 'bi-arrow-up-right' : 'bi-arrow-down-right');
        }
        if (metaNode) {
          metaNode.textContent = metric.meta;
        }
      });
    }

    function renderChartError(kind) {
      var stateNode = kind === 'users' ? controls.chartUsersState : controls.chartQuizzesState;
      if (stateNode) {
        stateNode.innerHTML = '<div class="report-state report-state--error"><p>We could not load this chart.</p><button class="admin-button admin-button--ghost" type="button" data-report-retry="' + kind + '">Retry</button></div>';
      }
    }

    function renderChartLoading(kind) {
      var stateNode = kind === 'users' ? controls.chartUsersState : controls.chartQuizzesState;
      if (stateNode) {
        stateNode.innerHTML = '<div class="report-state report-state--loading"><span class="report-skeleton report-skeleton--chart"></span></div>';
      }
    }

    function buildChart(kind, points) {
      var canvas = kind === 'users' ? controls.chartUsersCanvas : controls.chartQuizzesCanvas;
      var stateNode = kind === 'users' ? controls.chartUsersState : controls.chartQuizzesState;
      var tooltip = kind === 'users' ? controls.chartUsersTooltip : controls.chartQuizzesTooltip;
      var emptyNode = kind === 'users' ? controls.chartUsersEmpty : controls.chartQuizzesEmpty;
      if (!canvas) {
        return;
      }
      if (stateNode) {
        stateNode.innerHTML = '';
      }
      if (tooltip) {
        tooltip.hidden = true;
      }
      if (emptyNode) {
        emptyNode.hidden = points.length > 0;
      }
      if (points.length === 0) {
        canvas.hidden = true;
        return;
      }
      canvas.hidden = false;
      reportState.charts[kind] = { points: points.slice() };
      console.info('[reports] rendering chart', kind, points);
      resizeReportCanvas(canvas);
      if (kind === 'users') {
        drawReportLineChart(canvas, points, tooltip);
      } else {
        drawReportBarChart(canvas, points, tooltip);
      }
    }

    function resizeReportCanvas(canvas) {
      var rect = canvas.getBoundingClientRect();
      var ratio = window.devicePixelRatio || 1;
      canvas.width = Math.max(320, Math.round(rect.width * ratio));
      canvas.height = Math.max(220, Math.round(rect.height * ratio));
      var context = canvas.getContext('2d');
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
    }

    function drawReportGrid(ctx, width, height, padding) {
      ctx.save();
      ctx.strokeStyle = 'rgba(31, 41, 51, 0.08)';
      ctx.lineWidth = 1;
      [0.25, 0.5, 0.75, 1].forEach(function (fraction) {
        var y = padding.top + ((height - padding.top - padding.bottom) * fraction);
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(width - padding.right, y);
        ctx.stroke();
      });
      ctx.restore();
    }

    function drawReportLineChart(canvas, points, tooltip) {
      var ctx = canvas.getContext('2d');
      var width = canvas.getBoundingClientRect().width;
      var height = canvas.getBoundingClientRect().height;
      var padding = { top: 18, right: 18, bottom: 28, left: 22 };
      var max = Math.max(1, points.reduce(function (value, point) { return Math.max(value, point.value); }, 0));
      var stepX = points.length > 1 ? (width - padding.left - padding.right) / (points.length - 1) : 0;
      var hitPoints = [];
      ctx.clearRect(0, 0, width, height);
      drawReportGrid(ctx, width, height, padding);
      ctx.beginPath();
      points.forEach(function (point, index) {
        var x = padding.left + (stepX * index);
        var y = height - padding.bottom - (((height - padding.top - padding.bottom) * point.value) / max);
        hitPoints.push({ x: x, y: y, point: point });
        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.strokeStyle = '#ff7a3d';
      ctx.lineWidth = 3;
      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      ctx.stroke();

      ctx.lineTo(width - padding.right, height - padding.bottom);
      ctx.lineTo(padding.left, height - padding.bottom);
      ctx.closePath();
      ctx.fillStyle = 'rgba(255, 122, 61, 0.12)';
      ctx.fill();

      hitPoints.forEach(function (entry) {
        ctx.beginPath();
        ctx.arc(entry.x, entry.y, 4, 0, Math.PI * 2);
        ctx.fillStyle = '#ffffff';
        ctx.fill();
        ctx.strokeStyle = '#ff7a3d';
        ctx.lineWidth = 2;
        ctx.stroke();
      });

      bindReportCanvasTooltip(canvas, tooltip, hitPoints, function (entry) {
        return '<strong>' + escapeHtml(formatShortDate(entry.point.date)) + '</strong><span>' + escapeHtml(entry.point.value) + ' users</span>';
      }, false);
    }

    function drawReportBarChart(canvas, points, tooltip) {
      var ctx = canvas.getContext('2d');
      var width = canvas.getBoundingClientRect().width;
      var height = canvas.getBoundingClientRect().height;
      var padding = { top: 18, right: 18, bottom: 28, left: 22 };
      var max = Math.max(1, points.reduce(function (value, point) { return Math.max(value, point.value); }, 0));
      var areaWidth = width - padding.left - padding.right;
      var barWidth = Math.max(10, (areaWidth / Math.max(points.length, 1)) - 8);
      var gap = points.length > 1 ? (areaWidth - (barWidth * points.length)) / (points.length - 1) : 0;
      var hitBars = [];
      ctx.clearRect(0, 0, width, height);
      drawReportGrid(ctx, width, height, padding);

      points.forEach(function (point, index) {
        var x = padding.left + (index * (barWidth + gap));
        var barHeight = ((height - padding.top - padding.bottom) * point.value) / max;
        var y = height - padding.bottom - barHeight;
        drawRoundedRect(ctx, x, y, barWidth, barHeight, 10);
        ctx.fillStyle = '#ff9d6d';
        ctx.fill();
        hitBars.push({ x: x, y: y, width: barWidth, height: barHeight, point: point });
      });

      bindReportCanvasTooltip(canvas, tooltip, hitBars, function (entry) {
        return '<strong>' + escapeHtml(formatShortDate(entry.point.date)) + '</strong><span>' + escapeHtml(entry.point.value) + ' completions</span>';
      }, true);
    }

    function drawRoundedRect(ctx, x, y, width, height, radius) {
      var r = Math.min(radius, width / 2, height / 2);
      ctx.beginPath();
      ctx.moveTo(x + r, y);
      ctx.arcTo(x + width, y, x + width, y + height, r);
      ctx.arcTo(x + width, y + height, x, y + height, r);
      ctx.arcTo(x, y + height, x, y, r);
      ctx.arcTo(x, y, x + width, y, r);
      ctx.closePath();
    }

    function bindReportCanvasTooltip(canvas, tooltip, entries, formatter, isRect) {
      canvas.onmousemove = function (event) {
        if (!tooltip) {
          return;
        }
        var rect = canvas.getBoundingClientRect();
        var x = event.clientX - rect.left;
        var y = event.clientY - rect.top;
        var active = entries.find(function (entry) {
          if (isRect) {
            return x >= entry.x && x <= entry.x + entry.width && y >= entry.y && y <= entry.y + entry.height;
          }
          var dx = entry.x - x;
          var dy = entry.y - y;
          return Math.sqrt((dx * dx) + (dy * dy)) <= 10;
        });
        if (!active) {
          tooltip.hidden = true;
          return;
        }
        tooltip.hidden = false;
        tooltip.innerHTML = formatter(active);
        tooltip.style.left = active.x + 'px';
        tooltip.style.top = active.y + 'px';
      };
      canvas.onmouseleave = function () {
        if (tooltip) {
          tooltip.hidden = true;
        }
      };
    }

    function setListLoading(container, type) {
      if (!container) {
        return;
      }
      var rows = [];
      for (var i = 0; i < 4; i += 1) {
        rows.push('<div class="' + type + '"><span class="report-skeleton report-skeleton--line"></span><span class="report-skeleton report-skeleton--line short"></span></div>');
      }
      container.innerHTML = '<div class="report-list-loading">' + rows.join('') + '</div>';
    }

    function setListError(container, key) {
      if (!container) {
        return;
      }
      container.innerHTML = '<div class="report-state report-state--error"><p>We could not load this section.</p><button class="admin-button admin-button--ghost" type="button" data-report-retry="' + key + '">Retry</button></div>';
    }

    function renderCategories(items) {
      if (!controls.categories) {
        return;
      }
      if (controls.categoriesEmpty) {
        controls.categoriesEmpty.hidden = items.length > 0;
      }
      if (!items.length) {
        controls.categories.innerHTML = '';
        return;
      }
      controls.categories.innerHTML = items.map(function (item, index) {
        return '' +
          '<div class="report-category-row" style="animation-delay:' + (index * 45) + 'ms">' +
            '<div class="report-category-row__label">' +
              '<span>' + escapeHtml(item.name) + '</span>' +
              '<strong>' + escapeHtml(item.questionCount) + '</strong>' +
            '</div>' +
            '<div class="report-category-row__track">' +
              '<span class="report-category-row__fill" style="width:' + escapeAttribute(item.percent) + '%;background:' + escapeAttribute(item.color || '#ff7a3d') + '"></span>' +
            '</div>' +
          '</div>';
      }).join('');
    }

    function relativeTime(value) {
      if (!value) {
        return 'Just now';
      }
      var then = new Date(value);
      var diff = Math.max(1, Math.round((Date.now() - then.getTime()) / 60000));
      if (diff < 60) {
        return diff + 'm ago';
      }
      if (diff < 1440) {
        return Math.round(diff / 60) + 'h ago';
      }
      return Math.round(diff / 1440) + 'd ago';
    }

    function renderActivity(items) {
      if (!controls.activity) {
        return;
      }
      if (controls.activityEmpty) {
        controls.activityEmpty.hidden = items.length > 0;
      }
      if (controls.activityLoadMore) {
        controls.activityLoadMore.hidden = items.length < reportState.activityLimit;
      }
      if (!items.length) {
        controls.activity.innerHTML = '';
        return;
      }
      controls.activity.innerHTML = items.map(function (item, index) {
        return '' +
          '<a class="report-activity-item" href="' + escapeAttribute(item.href) + '" style="animation-delay:' + (index * 40) + 'ms">' +
            '<span class="report-activity-item__icon" data-type="' + escapeAttribute(item.type) + '">' +
              '<i class="bi ' + activityIcon(item.type) + '"></i>' +
            '</span>' +
            '<span class="report-activity-item__copy">' +
              '<strong>' + escapeHtml(item.actor) + '</strong>' +
              '<span>' + escapeHtml(item.action) + '</span>' +
            '</span>' +
            '<span class="report-activity-item__time">' + escapeHtml(relativeTime(item.time || item.createdAt)) + '</span>' +
          '</a>';
      }).join('');
    }

    function activityIcon(type) {
      if (type === 'user') {
        return 'bi-person-plus';
      }
      if (type === 'quiz') {
        return 'bi-clipboard-check';
      }
      return 'bi-patch-question';
    }

    function loadSummary() {
      setSummaryLoading();
      return apiGet('/admin/api/reports/summary', currentRangeParams(), true).then(renderSummary).catch(function () {
        controls.metrics.forEach(function (card) {
          card.classList.remove('is-loading');
          var meta = card.querySelector('[data-metric-meta]');
          if (meta) {
            meta.innerHTML = 'Could not load data. <button class="report-inline-retry" type="button" data-report-retry="summary">Retry</button>';
          }
        });
      });
    }

    function loadCharts(showLoading) {
      if (showLoading !== false) {
        renderChartLoading('users');
        renderChartLoading('quizzes');
      }
      var params = currentRangeParams();
      return apiGet('/admin/api/reports/charts', params, false, { cacheMode: 'no-store' }).then(function (data) {
        var userGrowth = Array.isArray(data && data.userGrowth) ? data.userGrowth : [];
        var quizCompletions = Array.isArray(data && data.quizCompletions) ? data.quizCompletions : [];
        console.info('[reports] received chart data', {
          range: params,
          userGrowthPoints: userGrowth.length,
          quizCompletionPoints: quizCompletions.length,
          payload: data
        });
        buildChart('users', userGrowth);
        buildChart('quizzes', quizCompletions);
      }).catch(function (error) {
        console.error('[reports] failed to load chart data', error);
        renderChartError('users');
        renderChartError('quizzes');
      });
    }

    function loadCategories() {
      setListLoading(controls.categories, 'report-category-row');
      return apiGet('/admin/api/reports/categories', currentRangeParams(), true).then(renderCategories).catch(function () {
        setListError(controls.categories, 'categories');
      });
    }

    function loadActivity() {
      setListLoading(controls.activity, 'report-activity-item');
      var params = currentRangeParams();
      params.limit = reportState.activityLimit;
      return apiGet('/admin/api/reports/activity', params, false).then(renderActivity).catch(function () {
        setListError(controls.activity, 'activity');
      });
    }

    function refreshAll() {
      loadSummary();
      loadCharts(true);
      loadCategories();
      loadActivity();
      restartActivityPolling();
      restartChartPolling();
    }

    function restartActivityPolling() {
      if (reportState.activityPoll) {
        window.clearInterval(reportState.activityPoll);
      }
      reportState.activityPoll = window.setInterval(function () {
        loadActivity();
      }, 45000);
    }

    function restartChartPolling() {
      if (reportState.chartPoll) {
        window.clearInterval(reportState.chartPoll);
      }
      reportState.chartPoll = window.setInterval(function () {
        loadCharts(false);
      }, 10000);
    }

    function exportReports() {
      if (!controls.exportButton || !controls.exportLabel) {
        return;
      }
      controls.exportButton.disabled = true;
      controls.exportLabel.textContent = 'Exporting...';
      var params = currentRangeParams();
      Promise.all([
        apiGet('/admin/api/reports/summary', params, true),
        apiGet('/admin/api/reports/categories', params, true),
        apiGet('/admin/api/reports/activity', params, false)
      ]).then(function (responses) {
        var summary = responses[0] || {};
        var categories = responses[1] || [];
        var activity = responses[2] || [];
        var rows = [
          ['Filter Range', params.start + ' to ' + params.end],
          [],
          ['Summary'],
          ['Metric', 'Value', 'Trend', 'Direction', 'Helper']
        ];
        (summary.metrics || []).forEach(function (metric) {
          rows.push([
            metric.label,
            metric.value,
            metric.trendPercent + '%',
            metric.positive ? 'Positive' : 'Negative',
            metric.helper
          ]);
        });

        rows.push([]);
        rows.push(['Top Categories']);
        rows.push(['Category', 'Questions', 'Relative Share']);
        categories.forEach(function (item) {
          rows.push([item.name, item.questionCount, item.percent + '%']);
        });

        rows.push([]);
        rows.push(['Recent Activity']);
        rows.push(['Actor', 'Action', 'Time', 'Link']);
        activity.forEach(function (item) {
          rows.push([
            item.actor,
            item.action,
            item.relativeTime || relativeTime(item.time || item.createdAt),
            item.href
          ]);
        });

        var csv = rows.map(function (row) {
          return row.map(csvCell).join(',');
        }).join('\n');
        var selectedFormat = controls.exportFormat ? controls.exportFormat.value : 'csv';
        downloadBlob(csv, 'report-export.' + (selectedFormat === 'pdf' ? 'csv' : selectedFormat), 'text/csv;charset=utf-8');
      }).finally(function () {
        controls.exportButton.disabled = false;
        controls.exportLabel.textContent = 'Export';
      });
    }

    function csvCell(value) {
      return '"' + String(value == null ? '' : value).replace(/"/g, '""') + '"';
    }

    function downloadBlob(content, filename, type) {
      var blob = new Blob([content], { type: type });
      var url = URL.createObjectURL(blob);
      var link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    }

    function formatShortDate(value) {
      var date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return value;
      }
      return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
    }

      if (controls.range) {
      controls.range.addEventListener('change', function () {
        reportState.range = controls.range.value;
        if (controls.customWrap) {
          controls.customWrap.hidden = reportState.range !== 'custom';
        }
        refreshAll();
      });
    }

    if (controls.start) {
      controls.start.addEventListener('change', function () {
        reportState.start = controls.start.value || '';
        if (reportState.range === 'custom') {
          refreshAll();
        }
      });
    }

    if (controls.end) {
      controls.end.addEventListener('change', function () {
        reportState.end = controls.end.value || '';
        if (reportState.range === 'custom') {
          refreshAll();
        }
      });
    }

    if (controls.exportButton) {
      controls.exportButton.addEventListener('click', exportReports);
    }

    if (controls.activityMoreButton) {
      controls.activityMoreButton.addEventListener('click', function () {
        reportState.activityLimit += 8;
        loadActivity();
      });
    }

    page.addEventListener('click', function (event) {
      var retry = event.target.closest('[data-report-retry]');
      if (retry) {
        var target = retry.getAttribute('data-report-retry');
        if (target === 'summary') {
          loadSummary();
        } else if (target === 'users' || target === 'quizzes') {
          loadCharts(true);
        } else if (target === 'categories') {
          loadCategories();
        } else if (target === 'activity') {
          loadActivity();
        }
      }
    });

    document.addEventListener('visibilitychange', function () {
      if (document.hidden) {
        if (reportState.chartPoll) {
          window.clearInterval(reportState.chartPoll);
          reportState.chartPoll = null;
        }
        return;
      }
      loadCharts(false);
      restartChartPolling();
    });

    window.addEventListener('beforeunload', function () {
      if (reportState.activityPoll) {
        window.clearInterval(reportState.activityPoll);
      }
      if (reportState.chartPoll) {
        window.clearInterval(reportState.chartPoll);
      }
    });

    refreshAll();
  }

  document.addEventListener('DOMContentLoaded', function () {
    bindMobileMenu();
    bindDropdowns();
    loadNotificationState();
    renderNotificationPanel();
    renderNotificationsPage();
    bindNotificationEvents();
    simulateNotificationArrival();
    initSettingsPage();
    initReportsPage();
    initDataTable('questions');
    initDataTable('users');
    bindSelection('questions');
    bindSelection('users');
    bindDeleteConfirmation();
    bindDashboardActivity();
  });
})();
