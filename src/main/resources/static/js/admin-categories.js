/**
 * Categories Management Page JavaScript
 * Handles modals, drawer, search, sort, pagination, and bulk actions
 */
(function() {
  'use strict';

  // State management
  const state = {
    categories: window.categoryData || [],
    colors: window.categoryColors || {},
    filteredCategories: [],
    selectedIds: new Set(),
    currentPage: 1,
    pageSize: 9,
    sortBy: 'mostQuestions',
    filterBy: 'all',
    searchQuery: '',
    currentCategory: null,
    deleteTargetId: null,
    deleteTargetCount: 0
  };

  // Ensure categories is an array
  if (!Array.isArray(state.categories)) {
    state.categories = [];
  }

  // DOM Elements
  const elements = {};
  const csrf = {
    parameter: document.querySelector('meta[name="_csrf_parameter"]')?.getAttribute('content') || '_csrf',
    token: document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '',
    header: document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN'
  };

  // Initialize when DOM is ready
  document.addEventListener('DOMContentLoaded', function() {
    initializeElements();
    initializeEventListeners();
    initializeState();
    render();
  });

  function initializeElements() {
    // Header & Controls
    elements.addCategoryBtn = document.getElementById('addCategoryBtn');
    elements.searchInput = document.getElementById('searchInput');
    elements.sortSelect = document.getElementById('sortSelect');
    elements.filterSelect = document.getElementById('filterSelect');
    elements.resetFilters = document.getElementById('resetFilters');

    // Bulk Actions
    elements.bulkActionsBar = document.getElementById('bulkActionsBar');
    elements.selectAllCheckbox = document.getElementById('selectAllCheckbox');
    elements.selectedCount = document.getElementById('selectedCount');
    elements.bulkActionsButtons = document.getElementById('bulkActionsButtons');
    elements.mergeSelectedBtn = document.getElementById('mergeSelectedBtn');
    elements.deleteSelectedBtn = document.getElementById('deleteSelectedBtn');

    // Grid & Pagination
    elements.categoriesGrid = document.getElementById('categoriesGrid');
    elements.paginationInfo = document.getElementById('paginationInfo');
    elements.paginationControls = document.getElementById('paginationControls');
    elements.prevPage = document.getElementById('prevPage');
    elements.nextPage = document.getElementById('nextPage');
    elements.pageNumbers = document.getElementById('pageNumbers');

    // Category Modal
    elements.categoryModal = document.getElementById('categoryModal');
    elements.categoryModalTitle = document.getElementById('categoryModalTitle');
    elements.closeCategoryModal = document.getElementById('closeCategoryModal');
    elements.cancelCategoryBtn = document.getElementById('cancelCategoryBtn');
    elements.saveCategoryBtn = document.getElementById('saveCategoryBtn');
    elements.categoryForm = document.getElementById('categoryForm');
    elements.categoryId = document.getElementById('categoryId');
    elements.categoryName = document.getElementById('categoryName');
    elements.categoryDescription = document.getElementById('categoryDescription');
    elements.categoryColor = document.getElementById('categoryColor');
    elements.colorOptions = document.querySelectorAll('.color-option');

    // Delete Modal
    elements.deleteModal = document.getElementById('deleteModal');
    elements.closeDeleteModal = document.getElementById('closeDeleteModal');
    elements.cancelDeleteBtn = document.getElementById('cancelDeleteBtn');
    elements.confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    elements.deleteMessage = document.getElementById('deleteMessage');
    elements.targetCategorySelect = document.getElementById('targetCategorySelect');
    elements.moveQuestionsRadio = document.getElementById('moveQuestionsRadio');
    elements.deleteQuestionsRadio = document.getElementById('deleteQuestionsRadio');

    // Bulk Delete Modal
    elements.bulkDeleteModal = document.getElementById('bulkDeleteModal');
    elements.closeBulkDeleteModal = document.getElementById('closeBulkDeleteModal');
    elements.cancelBulkDeleteBtn = document.getElementById('cancelBulkDeleteBtn');
    elements.confirmBulkDeleteBtn = document.getElementById('confirmBulkDeleteBtn');
    elements.bulkDeleteCount = document.getElementById('bulkDeleteCount');

    // Merge Modal
    elements.mergeModal = document.getElementById('mergeModal');
    elements.closeMergeModal = document.getElementById('closeMergeModal');
    elements.cancelMergeBtn = document.getElementById('cancelMergeBtn');
    elements.confirmMergeBtn = document.getElementById('confirmMergeBtn');
    elements.mergeSourceList = document.getElementById('mergeSourceList');
    elements.mergeTargetSelect = document.getElementById('mergeTargetSelect');
    elements.mergePreview = document.getElementById('mergePreview');

    // Drawer
    elements.drawer = document.getElementById('categoryDrawer');
    elements.drawerOverlay = document.getElementById('drawerOverlay');
    elements.closeDrawer = document.getElementById('closeDrawer');
    elements.drawerCategoryTag = document.getElementById('drawerCategoryTag');
    elements.drawerCategoryName = document.getElementById('drawerCategoryName');
    elements.drawerQuestionCount = document.getElementById('drawerQuestionCount');
    elements.drawerCreatedDate = document.getElementById('drawerCreatedDate');
    elements.drawerDescription = document.getElementById('drawerDescription');
    elements.questionsList = document.getElementById('questionsList');
    elements.viewAllCount = document.getElementById('viewAllCount');
    elements.viewAllQuestions = document.getElementById('viewAllQuestions');
    elements.drawerEditBtn = document.getElementById('drawerEditBtn');
    elements.drawerMergeBtn = document.getElementById('drawerMergeBtn');
    elements.drawerDeleteBtn = document.getElementById('drawerDeleteBtn');

    // Success Alert
    elements.successAlert = document.getElementById('successAlert');
    elements.totalCategoriesCount = document.getElementById('totalCategoriesCount');
    elements.mostUsedCategory = document.getElementById('mostUsedCategory');
    elements.emptyCategoriesCount = document.getElementById('emptyCategoriesCount');
  }

  function initializeEventListeners() {
    // Header & Controls
    elements.addCategoryBtn?.addEventListener('click', () => openCategoryModal());
    elements.searchInput?.addEventListener('input', handleSearch);
    elements.sortSelect?.addEventListener('change', handleSort);
    elements.filterSelect?.addEventListener('change', handleFilter);
    elements.resetFilters?.addEventListener('click', resetFilters);

    // Bulk Actions
    elements.selectAllCheckbox?.addEventListener('change', handleSelectAll);
    elements.mergeSelectedBtn?.addEventListener('click', openMergeModal);
    elements.deleteSelectedBtn?.addEventListener('click', openBulkDeleteModal);

    // Pagination
    elements.prevPage?.addEventListener('click', () => changePage(-1));
    elements.nextPage?.addEventListener('click', () => changePage(1));

    // Category Modal
    elements.closeCategoryModal?.addEventListener('click', closeCategoryModal);
    elements.cancelCategoryBtn?.addEventListener('click', closeCategoryModal);
    elements.saveCategoryBtn?.addEventListener('click', saveCategory);
    elements.categoryName?.addEventListener('input', () => elements.categoryName.setCustomValidity(''));
    elements.colorOptions?.forEach(btn => {
      btn.addEventListener('click', () => selectColor(btn));
    });

    // Delete Modal
    elements.closeDeleteModal?.addEventListener('click', closeDeleteModal);
    elements.cancelDeleteBtn?.addEventListener('click', closeDeleteModal);
    elements.confirmDeleteBtn?.addEventListener('click', confirmDelete);

    // Bulk Delete Modal
    elements.closeBulkDeleteModal?.addEventListener('click', closeBulkDeleteModal);
    elements.cancelBulkDeleteBtn?.addEventListener('click', closeBulkDeleteModal);
    elements.confirmBulkDeleteBtn?.addEventListener('click', confirmBulkDelete);

    // Merge Modal
    elements.closeMergeModal?.addEventListener('click', closeMergeModal);
    elements.cancelMergeBtn?.addEventListener('click', closeMergeModal);
    elements.confirmMergeBtn?.addEventListener('click', confirmMerge);
    elements.mergeTargetSelect?.addEventListener('change', updateMergePreview);

    // Drawer
    elements.closeDrawer?.addEventListener('click', closeDrawer);
    elements.drawerOverlay?.addEventListener('click', closeDrawer);
    elements.drawerEditBtn?.addEventListener('click', () => {
      const category = state.currentCategory;
      closeDrawer();
      if (category) {
        openCategoryModal(category);
      }
    });
    elements.drawerMergeBtn?.addEventListener('click', () => {
      const category = state.currentCategory;
      closeDrawer();
      if (category) {
        state.selectedIds = new Set([category.id]);
        updateBulkActions();
        openMergeModal();
      }
    });
    elements.drawerDeleteBtn?.addEventListener('click', () => {
      const category = state.currentCategory;
      closeDrawer();
      if (category) {
        openDeleteModal(category.id, category.questionCount);
      }
    });
    elements.viewAllQuestions?.addEventListener('click', handleViewAllQuestions);
    document.getElementById('addQuestionToCategory')?.addEventListener('click', handleAddQuestionToCategory);

    // Close modals on backdrop click
    document.querySelectorAll('.modal__backdrop').forEach(backdrop => {
      backdrop.addEventListener('click', function() {
        const modal = this.closest('.modal');
        if (modal) {
          modal.classList.remove('is-open');
        }
      });
    });

    // Close modals on Escape key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        closeAllModals();
        closeDrawer();
      }
    });

    // Card actions (delegated)
    elements.categoriesGrid?.addEventListener('click', handleCardClick);
    elements.questionsList?.addEventListener('click', handleQuestionListClick);
  }

  function initializeState() {
    hydrateCategoriesFromDom();
    state.filteredCategories = [...state.categories];
    refreshStats();
    applyFilters();
  }

  function hydrateCategoriesFromDom() {
    const cards = Array.from(document.querySelectorAll('.category-card'));
    if (cards.length === 0) {
      return;
    }

    const domCategories = cards.map(card => {
      const id = parseInt(card.dataset.id, 10);
      if (!Number.isFinite(id)) {
        return null;
      }

      const name = card.dataset.name || card.querySelector('.category-tag')?.textContent?.trim() || '';
      const questionCount = parseInt(card.dataset.count, 10) || 0;
      const createdAt = card.dataset.created || null;
      const description = card.dataset.description || '';
      const color = card.dataset.color || card.querySelector('.category-tag')?.dataset.color || '#ff7a3d';
      const isProtected = card.dataset.protected === 'true';

      state.colors[name] = state.colors[name] || color;

      return {
        id,
        name,
        questionCount,
        createdAt,
        description,
        color,
        isProtected
      };
    }).filter(Boolean);

    if (domCategories.length > 0) {
      state.categories = domCategories;
    }
  }

  // ====================
  // Filtering & Sorting
  // ====================

  function handleSearch(e) {
    state.searchQuery = e.target.value.toLowerCase().trim();
    state.currentPage = 1;
    applyFilters();
  }

  function handleSort(e) {
    state.sortBy = e.target.value;
    applyFilters();
  }

  function handleFilter(e) {
    state.filterBy = e.target.value;
    state.currentPage = 1;
    applyFilters();
  }

  function resetFilters() {
    state.searchQuery = '';
    state.sortBy = 'mostQuestions';
    state.filterBy = 'all';
    state.currentPage = 1;

    elements.searchInput.value = '';
    elements.sortSelect.value = 'mostQuestions';
    elements.filterSelect.value = 'all';

    applyFilters();
  }

  function applyFilters() {
    let result = state.categories.filter(cat => {
      // Search filter
      if (state.searchQuery) {
        const searchName = (cat.name || '').toLowerCase();
        if (!searchName.includes(state.searchQuery)) {
          return false;
        }
      }

      // Category filter
      if (state.filterBy === 'empty' && cat.questionCount > 0) {
        return false;
      }
      if (state.filterBy === 'nonEmpty' && cat.questionCount === 0) {
        return false;
      }

      return true;
    });

    // Sorting
    result.sort((a, b) => {
      switch (state.sortBy) {
        case 'mostQuestions':
          return (b.questionCount || 0) - (a.questionCount || 0) || (a.name || '').localeCompare(b.name || '');
        case 'newest':
          return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
        case 'alphabetical':
          return (a.name || '').localeCompare(b.name || '');
        default:
          return 0;
      }
    });

    state.filteredCategories = result;
    render();
  }

  // ====================
  // Rendering
  // ====================

  function render() {
    renderGrid();
    renderPagination();
    updateBulkActions();
  }

  function renderGrid() {
    if (!elements.categoriesGrid) return;

    const start = (state.currentPage - 1) * state.pageSize;
    const end = start + state.pageSize;
    const pageCategories = state.filteredCategories.slice(start, end);
    const orderedIds = state.filteredCategories.map(category => category.id);
    const orderedCards = orderedIds
      .map(id => elements.categoriesGrid.querySelector(`.category-card[data-id="${id}"]`))
      .filter(Boolean);

    // Update card visibility
    const allCards = elements.categoriesGrid.querySelectorAll('.category-card');
    allCards.forEach(card => {
      const id = parseInt(card.dataset.id);
      const isVisible = pageCategories.some(c => c.id === id);
      card.classList.toggle('is-hidden', !isVisible);

      // Update selection state
      const checkbox = card.querySelector('.category-select');
      if (checkbox) {
        checkbox.checked = state.selectedIds.has(id);
      }
      card.classList.toggle('is-selected', state.selectedIds.has(id));
    });

    orderedCards.forEach(card => {
      elements.categoriesGrid.appendChild(card);
    });

    // Update "Select All" checkbox state
    const visibleIds = pageCategories.filter(c => !c.isProtected).map(c => c.id);
    const allSelected = visibleIds.length > 0 && visibleIds.every(id => state.selectedIds.has(id));
    const someSelected = visibleIds.some(id => state.selectedIds.has(id));

    if (elements.selectAllCheckbox) {
      elements.selectAllCheckbox.checked = allSelected;
      elements.selectAllCheckbox.indeterminate = someSelected && !allSelected;
      elements.selectAllCheckbox.disabled = visibleIds.length === 0;
    }
  }

  function renderPagination() {
    const totalItems = state.filteredCategories.length;
    const totalPages = Math.max(1, Math.ceil(totalItems / state.pageSize));

    // Update info text
    if (elements.paginationInfo) {
      if (totalItems === 0) {
        elements.paginationInfo.textContent = 'No categories found';
      } else {
        const start = (state.currentPage - 1) * state.pageSize + 1;
        const end = Math.min(start + state.pageSize - 1, totalItems);
        elements.paginationInfo.textContent = `Showing ${start} to ${end} of ${totalItems} categor${totalItems === 1 ? 'y' : 'ies'}`;
      }
    }

    // Update button states
    if (elements.prevPage) {
      elements.prevPage.disabled = state.currentPage === 1;
    }
    if (elements.nextPage) {
      elements.nextPage.disabled = state.currentPage >= totalPages;
    }

    // Render page numbers
    if (elements.pageNumbers) {
      elements.pageNumbers.innerHTML = '';
      for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement('button');
        btn.className = `page-number ${i === state.currentPage ? 'is-active' : ''}`;
        btn.textContent = i;
        btn.addEventListener('click', () => {
          state.currentPage = i;
          render();
        });
        elements.pageNumbers.appendChild(btn);
      }
    }
  }

  function changePage(delta) {
    const totalPages = Math.ceil(state.filteredCategories.length / state.pageSize);
    const newPage = state.currentPage + delta;

    if (newPage >= 1 && newPage <= totalPages) {
      state.currentPage = newPage;
      render();
    }
  }

  // ====================
  // Card Interactions
  // ====================

  function handleCardClick(e) {
    const card = e.target.closest('.category-card');
    if (!card) return;

    const id = parseInt(card.dataset.id);
    const category = state.categories.find(c => c.id === id);
    if (!category) return;

    // Checkbox click
    if (e.target.closest('.category-select')) {
      e.stopPropagation();
      toggleSelection(id);
      return;
    }

    // Action buttons
    const viewBtn = e.target.closest('.icon-action--view');
    const editBtn = e.target.closest('.icon-action--edit');
    const deleteBtn = e.target.closest('.icon-action--delete');

    if (viewBtn) {
      e.stopPropagation();
      openDrawer(category);
      return;
    }

    if (editBtn) {
      e.stopPropagation();
      openCategoryModal(category);
      return;
    }

    if (deleteBtn) {
      e.stopPropagation();
      const count = parseInt(deleteBtn.dataset.count) || 0;
      openDeleteModal(id, count);
      return;
    }
  }

  // ====================
  // Bulk Actions
  // ====================

  function toggleSelection(id) {
    const category = state.categories.find(c => c.id === id);
    if (category?.isProtected) {
      return;
    }
    if (state.selectedIds.has(id)) {
      state.selectedIds.delete(id);
    } else {
      state.selectedIds.add(id);
    }
    updateBulkActions();
    renderGrid();
  }

  function handleSelectAll(e) {
    const start = (state.currentPage - 1) * state.pageSize;
    const end = start + state.pageSize;
    const pageCategories = state.filteredCategories.slice(start, end);

    if (e.target.checked) {
      pageCategories.filter(cat => !cat.isProtected).forEach(cat => state.selectedIds.add(cat.id));
    } else {
      pageCategories.forEach(cat => state.selectedIds.delete(cat.id));
    }

    updateBulkActions();
    renderGrid();
  }

  function updateBulkActions() {
    const selectableIds = Array.from(state.selectedIds).filter(id => !state.categories.find(cat => cat.id === id)?.isProtected);
    state.selectedIds = new Set(selectableIds);
    const count = state.selectedIds.size;

    if (elements.selectedCount) {
      elements.selectedCount.textContent = `${count} selected`;
    }

    if (elements.bulkActionsBar) {
      elements.bulkActionsBar.classList.toggle('is-visible', count > 0);
    }

    if (elements.bulkActionsButtons) {
      elements.bulkActionsButtons.style.display = count > 0 ? 'flex' : 'none';
    }

    if (elements.mergeSelectedBtn) {
      elements.mergeSelectedBtn.disabled = count === 0;
    }

    if (elements.deleteSelectedBtn) {
      elements.deleteSelectedBtn.disabled = count === 0;
    }
  }

  // ====================
  // Modals
  // ====================

  function openCategoryModal(category = null) {
    if (!elements.categoryModal) return;

    const isEdit = !!category;
    elements.categoryModalTitle.textContent = isEdit ? 'Edit Category' : 'Add Category';

    if (isEdit) {
      elements.categoryId.value = category.id;
      elements.categoryName.value = category.name || '';
      elements.categoryDescription.value = category.description || '';
      elements.categoryColor.value = category.color || state.colors[category.name] || '#ff7a3d';
      elements.categoryName.readOnly = !!category.isProtected;
    } else {
      elements.categoryForm.reset();
      elements.categoryId.value = '';
      elements.categoryColor.value = '#ff7a3d';
      elements.categoryName.readOnly = false;
    }

    // Update color picker selection
    elements.colorOptions.forEach(btn => {
      btn.classList.toggle('is-selected', btn.dataset.color === elements.categoryColor.value);
    });

    elements.categoryModal.classList.add('is-open');
    elements.categoryName.focus();
  }

  function closeCategoryModal() {
    elements.categoryModal?.classList.remove('is-open');
  }

  function selectColor(btn) {
    elements.colorOptions.forEach(b => b.classList.remove('is-selected'));
    btn.classList.add('is-selected');
    elements.categoryColor.value = btn.dataset.color;
  }

  function saveCategory() {
    if (!elements.categoryForm.checkValidity()) {
      elements.categoryForm.reportValidity();
      return;
    }

    const currentId = parseInt(elements.categoryId.value, 10);
    const submittedName = (elements.categoryName.value || '').trim().toLowerCase();
    const duplicate = state.categories.find(category =>
      category.name.toLowerCase() === submittedName &&
      (!Number.isFinite(currentId) || category.id !== currentId)
    );

    if (duplicate) {
      elements.categoryName.setCustomValidity('Category already exists');
      elements.categoryForm.reportValidity();
      return;
    }

    elements.categoryName.setCustomValidity('');

    elements.categoryForm.submit();
  }

  function openDeleteModal(id, count) {
    if (!elements.deleteModal) return;

    const category = state.categories.find(c => c.id === id);
    if (category?.isProtected) {
      state.deleteTargetId = null;
      state.deleteTargetCount = 0;
      return;
    }
    state.deleteTargetId = id;
    state.deleteTargetCount = count;
    const name = category?.name || 'this category';

    elements.deleteMessage.textContent = count > 0
      ? `"${name}" contains ${count} question${count === 1 ? '' : 's'}. What do you want to do?`
      : `Are you sure you want to delete "${name}"?`;

    // Show/hide move options based on count
    const moveOption = elements.moveQuestionsRadio?.closest('.radio-option');
    if (moveOption) {
      moveOption.style.display = count > 0 ? 'flex' : 'none';
    }

    if (count === 0 && elements.deleteQuestionsRadio) {
      elements.deleteQuestionsRadio.checked = true;
    } else if (elements.moveQuestionsRadio) {
      elements.moveQuestionsRadio.checked = true;
    }

    if (elements.targetCategorySelect) {
      elements.targetCategorySelect.value = '';
    }

    // Remove current category from target select
    if (elements.targetCategorySelect) {
      const options = elements.targetCategorySelect.querySelectorAll('option');
      options.forEach(opt => {
        opt.disabled = parseInt(opt.value) === id;
      });
    }

    elements.deleteModal.classList.add('is-open');
  }

  function closeDeleteModal() {
    elements.deleteModal?.classList.remove('is-open');
    state.deleteTargetId = null;
    state.deleteTargetCount = 0;
  }

  function confirmDelete() {
    if (!state.deleteTargetId) return;

    const action = document.querySelector('input[name="deleteAction"]:checked')?.value;

    if (action === 'move' && elements.targetCategorySelect) {
      const targetId = elements.targetCategorySelect.value;
      if (!targetId) {
        shakeModal(elements.deleteModal);
        return;
      }
      // Submit form to move questions and delete
      submitDeleteWithMove(state.deleteTargetId, targetId);
    } else {
      // Submit form to delete questions with category
      submitDeleteWithDelete(state.deleteTargetId);
    }
  }

  function shakeModal(modal) {
    const content = modal?.querySelector('.modal__content');
    if (content) {
      content.classList.remove('is-shaking');
      void content.offsetWidth; // Trigger reflow
      content.classList.add('is-shaking');
    }
  }

  function openBulkDeleteModal() {
    if (!elements.bulkDeleteModal || state.selectedIds.size === 0) return;

    elements.bulkDeleteCount.textContent = `${state.selectedIds.size} categor${state.selectedIds.size === 1 ? 'y' : 'ies'}`;
    elements.bulkDeleteModal.classList.add('is-open');
  }

  function closeBulkDeleteModal() {
    elements.bulkDeleteModal?.classList.remove('is-open');
  }

  function confirmBulkDelete() {
    if (state.selectedIds.size === 0) return;

    // Create form and submit
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/admin/categories/bulk-delete';
    appendCsrfToken(form);

    state.selectedIds.forEach(id => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = 'ids';
      input.value = id;
      form.appendChild(input);
    });

    document.body.appendChild(form);
    form.submit();
  }

  function openMergeModal() {
    if (!elements.mergeModal || state.selectedIds.size === 0) return;

    // Build source list
    const sources = state.categories.filter(c => state.selectedIds.has(c.id) && !c.isProtected);
    if (sources.length === 0) {
      return;
    }
    state.selectedIds = new Set(sources.map(c => c.id));
    elements.mergeSourceList.innerHTML = sources.map(c => `
      <span class="merge-source-tag">${escapeHtml(c.name)}</span>
    `).join('');

    // Update target select to exclude selected
    const options = elements.mergeTargetSelect?.querySelectorAll('option');
    options?.forEach(opt => {
      const id = parseInt(opt.value);
      opt.disabled = state.selectedIds.has(id);
    });

    elements.mergeTargetSelect.value = '';
    elements.mergePreview.classList.remove('is-visible');

    elements.mergeModal.classList.add('is-open');
  }

  function closeMergeModal() {
    elements.mergeModal?.classList.remove('is-open');
  }

  function updateMergePreview() {
    const targetId = elements.mergeTargetSelect?.value;
    const target = state.categories.find(c => c.id === parseInt(targetId));

    if (!target) {
      elements.mergePreview?.classList.remove('is-visible');
      return;
    }

    const sources = state.categories.filter(c => state.selectedIds.has(c.id));
    const totalQuestions = sources.reduce((sum, c) => sum + (c.questionCount || 0), 0) + (target.questionCount || 0);

    elements.mergePreview.innerHTML = `
      <strong>Preview:</strong> "${escapeHtml(target.name)}" will contain ${totalQuestions} questions after merge.
    `;
    elements.mergePreview.classList.add('is-visible');
  }

  function confirmMerge() {
    const targetId = elements.mergeTargetSelect?.value;
    if (!targetId) {
      shakeModal(elements.mergeModal);
      return;
    }

    // Create form and submit
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/admin/categories/merge';
    appendCsrfToken(form);

    const targetInput = document.createElement('input');
    targetInput.type = 'hidden';
    targetInput.name = 'targetId';
    targetInput.value = targetId;
    form.appendChild(targetInput);

    state.selectedIds.forEach(id => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = 'sourceIds';
      input.value = id;
      form.appendChild(input);
    });

    document.body.appendChild(form);
    form.submit();
  }

  function closeAllModals() {
    document.querySelectorAll('.modal.is-open').forEach(modal => {
      modal.classList.remove('is-open');
    });
  }

  // ====================
  // Drawer
  // ====================

  function openDrawer(category) {
    if (!elements.drawer || !category) return;

    state.currentCategory = category;

    // Update drawer content
    const color = category.color || state.colors[category.name] || '#ff7a3d';
    elements.drawerCategoryTag.textContent = category.name;
    elements.drawerCategoryTag.setAttribute('data-color', color);

    elements.drawerCategoryName.textContent = category.name;
    elements.drawerQuestionCount.textContent = `${category.questionCount || 0} question${(category.questionCount || 0) === 1 ? '' : 's'}`;

    const date = category.createdAt
      ? new Date(category.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
      : 'Unknown';
    elements.drawerCreatedDate.textContent = `Created ${date}`;

    elements.drawerDescription.textContent = category.description || `All questions related to ${category.name}.`;
    elements.viewAllCount.textContent = category.questionCount || 0;
    elements.drawerDeleteBtn.disabled = !!category.isProtected;
    elements.drawerMergeBtn.disabled = !!category.isProtected;

    const addQuestionLink = document.getElementById('addQuestionToCategory');
    if (addQuestionLink) {
      addQuestionLink.href = `/admin/questions/new?category=${encodeURIComponent(category.name)}`;
    }

    loadCategoryQuestions(category);

    elements.drawer.classList.add('is-open');
    elements.drawerOverlay?.classList.add('is-open');
  }

  function loadCategoryQuestions(category) {
    if (!elements.questionsList) {
      return;
    }

    elements.questionsList.innerHTML = '<div class="admin-empty">Loading questions...</div>';

    fetch(`/admin/categories/${category.id}/questions`, {
      headers: {
        Accept: 'application/json'
      }
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`Failed to load questions (${response.status})`);
        }
        return response.json();
      })
      .then(payload => {
        renderDrawerQuestions(category, payload.questions || []);
        updateCategoryQuestionCount(category.id, payload.totalQuestions ?? category.questionCount);
      })
      .catch(() => {
        elements.questionsList.innerHTML = '<div class="admin-empty">Unable to load questions for this category right now.</div>';
      });
  }

  function renderDrawerQuestions(category, questions) {
    if (!elements.questionsList) {
      return;
    }

    if (!questions.length) {
      elements.questionsList.innerHTML = '<div class="admin-empty">No questions in this category yet.</div>';
      return;
    }

    elements.questionsList.innerHTML = questions.map(question => {
      const difficulty = (question.difficulty || 'MEDIUM').toLowerCase();
      const difficultyLabel = capitalize(difficulty);
      return `
        <div class="question-item" data-question-id="${question.id}">
          <span class="question-item__text">${escapeHtml(question.questionText)}</span>
          <span class="question-item__difficulty question-item__difficulty--${difficulty}">${difficultyLabel}</span>
          <div class="question-item__actions">
            <a href="/admin/questions/${question.id}" title="View question">
              <i class="bi bi-box-arrow-up-right"></i>
            </a>
            <button title="Remove from category" data-question-id="${question.id}" ${category.isProtected ? 'disabled' : ''}>
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  function closeDrawer() {
    elements.drawer?.classList.remove('is-open');
    elements.drawerOverlay?.classList.remove('is-open');
    state.currentCategory = null;
  }

  function handleAddQuestionToCategory(e) {
    if (!state.currentCategory) {
      e.preventDefault();
      return;
    }

    e.currentTarget.href = `/admin/questions/new?category=${encodeURIComponent(state.currentCategory.name)}`;
  }

  function handleViewAllQuestions() {
    if (!state.currentCategory) {
      return;
    }

    window.location.href = `/admin/questions?category=${encodeURIComponent(state.currentCategory.name)}`;
  }

  function handleQuestionListClick(e) {
    const removeBtn = e.target.closest('.question-item__actions button');
    if (!removeBtn || !state.currentCategory) {
      return;
    }

    e.preventDefault();
    removeQuestionFromCurrentCategory(removeBtn.dataset.questionId);
  }

  // ====================
  // Form Submissions
  // ====================

  function submitDeleteWithMove(categoryId, targetId) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/admin/categories/${categoryId}/delete`;
    appendCsrfToken(form);

    const targetInput = document.createElement('input');
    targetInput.type = 'hidden';
    targetInput.name = 'moveToCategoryId';
    targetInput.value = targetId;
    form.appendChild(targetInput);

    document.body.appendChild(form);
    form.submit();
  }

  function submitDeleteWithDelete(categoryId) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/admin/categories/${categoryId}/delete`;
    appendCsrfToken(form);

    const deleteInput = document.createElement('input');
    deleteInput.type = 'hidden';
    deleteInput.name = 'deleteQuestions';
    deleteInput.value = 'true';
    form.appendChild(deleteInput);

    document.body.appendChild(form);
    form.submit();
  }

  // ====================
  // Utilities
  // ====================

  function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function removeQuestionFromCurrentCategory(questionId) {
    if (!state.currentCategory || !questionId) {
      return;
    }

    fetch(`/admin/categories/${state.currentCategory.id}/questions/${questionId}/remove`, {
      method: 'POST',
      headers: {
        [csrf.header]: csrf.token,
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        Accept: 'application/json'
      },
      body: `${encodeURIComponent(csrf.parameter)}=${encodeURIComponent(csrf.token)}`
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`Failed to remove question (${response.status})`);
        }
        return response.json();
      })
      .then(payload => {
        if (!payload.ok) {
          throw new Error('Remove request was not accepted');
        }
        const row = elements.questionsList?.querySelector(`[data-question-id="${payload.removedQuestionId}"]`);
        row?.remove();
        updateCategoryQuestionCount(state.currentCategory.id, payload.remainingCount);
        if (elements.questionsList && !elements.questionsList.children.length) {
          elements.questionsList.innerHTML = '<div class="admin-empty">No questions in this category yet.</div>';
        }
      })
      .catch(() => {
        shakeModal(elements.drawer);
      });
  }

  function updateCategoryQuestionCount(categoryId, nextCount) {
    const normalizedCount = Math.max(0, Number(nextCount) || 0);
    const category = state.categories.find(item => item.id === categoryId);
    if (category) {
      category.questionCount = normalizedCount;
    }

    const card = elements.categoriesGrid?.querySelector(`.category-card[data-id="${categoryId}"]`);
    if (card) {
      card.dataset.count = String(normalizedCount);
      const numberNode = card.querySelector('.question-count__number');
      const labelNode = card.querySelector('.question-count__label');
      if (numberNode) {
        numberNode.textContent = String(normalizedCount);
      }
      if (labelNode) {
        labelNode.textContent = normalizedCount === 1 ? 'question' : 'questions';
      }
      card.classList.toggle('category-card--empty', normalizedCount === 0);
      let badge = card.querySelector('.empty-badge');
      if (normalizedCount === 0 && !badge) {
        badge = document.createElement('div');
        badge.className = 'empty-badge';
        badge.innerHTML = '<i class="bi bi-exclamation-circle"></i> Empty';
        card.appendChild(badge);
      }
      if (badge) {
        badge.style.display = normalizedCount === 0 ? 'flex' : 'none';
      }
      const deleteButton = card.querySelector('.icon-action--delete');
      if (deleteButton) {
        deleteButton.dataset.count = String(normalizedCount);
      }
    }

    if (state.currentCategory && state.currentCategory.id === categoryId) {
      state.currentCategory.questionCount = normalizedCount;
      elements.drawerQuestionCount.textContent = `${normalizedCount} question${normalizedCount === 1 ? '' : 's'}`;
      elements.viewAllCount.textContent = String(normalizedCount);
    }

    refreshStats();
    applyFilters();
  }

  function refreshStats() {
    const totalCount = state.categories.length;
    const emptyCount = state.categories.filter(c => (c.questionCount || 0) === 0).length;
    const mostUsed = state.categories.length > 0
      ? state.categories.reduce((max, c) => (c.questionCount || 0) > (max.questionCount || 0) ? c : max, state.categories[0])
      : null;

    if (elements.totalCategoriesCount) {
      elements.totalCategoriesCount.textContent = String(totalCount);
    }
    if (elements.emptyCategoriesCount) {
      elements.emptyCategoriesCount.textContent = String(emptyCount);
    }
    if (elements.mostUsedCategory) {
      elements.mostUsedCategory.textContent = mostUsed ? `${mostUsed.name} (${mostUsed.questionCount || 0})` : 'None';
    }
  }

  function capitalize(text) {
    return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
  }

  function appendCsrfToken(form) {
    if (!form || !csrf.token) {
      return;
    }

    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = csrf.parameter;
    input.value = csrf.token;
    form.appendChild(input);
  }

})();
