/**
 * BOLT — Navigation Controller
 * Handles glass sidebar collapsing, active menu states, mobile slideout,
 * breadcrumbs, and search shortcut listeners.
 */

document.addEventListener('DOMContentLoaded', () => {
  initSidebar();
  initMobileDrawer();
  initSearchShortcut();
  highlightActiveNav();
});

// Sidebar Collapse / Expand Toggle
function initSidebar() {
  const sidebar = document.querySelector('.sidebar');
  const collapseBtn = document.querySelector('.sidebar-collapse-btn');
  if (!sidebar) return;

  function updateCollapseBtnTooltip(collapsed) {
    if (collapseBtn) {
      collapseBtn.setAttribute('title', collapsed ? 'Expand Sidebar' : 'Collapse Sidebar');
      collapseBtn.setAttribute('aria-label', collapsed ? 'Expand Sidebar' : 'Collapse Sidebar');
    }
  }

  // Restore collapsed state from localStorage
  const isCollapsed = localStorage.getItem('bolt_sidebar_collapsed') === 'true';
  if (isCollapsed) {
    sidebar.classList.add('collapsed');
    updateCollapseBtnTooltip(true);
  }

  const toggleSidebar = () => {
    sidebar.classList.toggle('collapsed');
    const collapsed = sidebar.classList.contains('collapsed');
    localStorage.setItem('bolt_sidebar_collapsed', collapsed);
    updateCollapseBtnTooltip(collapsed);
  };

  if (collapseBtn) {
    collapseBtn.addEventListener('click', toggleSidebar);
  }

  // Also bind to any topbar sidebar toggle button
  document.querySelectorAll('.sidebar-toggle-topbar-btn').forEach(btn => {
    btn.addEventListener('click', toggleSidebar);
  });

  // Clicking brand logo in collapsed sidebar also expands it
  const brandLink = sidebar.querySelector('.brand-link');
  if (brandLink) {
    brandLink.addEventListener('click', (e) => {
      if (sidebar.classList.contains('collapsed')) {
        e.preventDefault();
        toggleSidebar();
      }
    });
  }
}

// Mobile Slide-Out Drawer and Backdrop
function initMobileDrawer() {
  const mobileBtn = document.querySelector('.mobile-menu-btn');
  const sidebar = document.querySelector('.sidebar');
  const backdrop = document.querySelector('.sidebar-backdrop');
  if (!mobileBtn || !sidebar || !backdrop) return;

  mobileBtn.addEventListener('click', () => {
    sidebar.classList.add('mobile-open');
    backdrop.classList.add('active');
    document.body.style.overflow = 'hidden';
  });

  const closeDrawer = () => {
    sidebar.classList.remove('mobile-open');
    backdrop.classList.remove('active');
    document.body.style.overflow = '';
  };

  backdrop.addEventListener('click', closeDrawer);

  // Close drawer when clicking any nav item on mobile
  sidebar.querySelectorAll('.nav-item a').forEach(link => {
    link.addEventListener('click', () => {
      if (window.innerWidth <= 992) closeDrawer();
    });
  });
}

// Highlight Current Page in Sidebar
function highlightActiveNav() {
  let currentPath = window.location.pathname.split('/').pop() || 'index.html';
  if (!currentPath || !currentPath.endsWith('.html')) currentPath = 'index.html';
  const navLinks = document.querySelectorAll('.nav-item a');

  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (href === currentPath || (currentPath === 'index.html' && (href === 'index.html' || href === './'))) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });
}

// Global Search Shortcut (Cmd+K / Ctrl+K)
function initSearchShortcut() {
  const searchInput = document.querySelector('.search-input');
  if (!searchInput) return;

  document.addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      searchInput.focus();
      searchInput.select();
    }
    if (e.key === 'Escape' && document.activeElement === searchInput) {
      searchInput.blur();
    }
  });

  searchInput.addEventListener('input', (e) => {
    const query = e.target.value.toLowerCase().trim();
    if (typeof handleGlobalSearch === 'function') {
      handleGlobalSearch(query);
    }
  });
}
