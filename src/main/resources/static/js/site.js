const themeButton = typeof document === 'undefined' ? null : document.querySelector('#theme-toggle');

export function nextTheme(current) {
  return current === 'dark' ? 'light' : 'dark';
}

export function applyTheme(theme) {
  document.documentElement.dataset.bsTheme = theme;
  localStorage.setItem('pembana-theme', theme);
}

export function setButtonLoading(button, loading) {
  if (!button || loading === (button.getAttribute('aria-disabled') === 'true')) return;
  const status = document.querySelector('#request-status');
  if (loading) {
    const spinner = document.createElement('span');
    spinner.className = 'spinner-border spinner-border-sm';
    spinner.setAttribute('aria-hidden', 'true');
    button.prepend(spinner);
    button.setAttribute('aria-disabled', 'true');
    status.textContent = button.dataset.loadingMessage;
  } else {
    button.querySelector('.spinner-border').remove();
    button.removeAttribute('aria-disabled');
    status.textContent = '';
  }
}

function connectNavigationLoading() {
  const pending = new Set();
  const startNavigation = (event, button) => {
    if (event.defaultPrevented || !button?.matches('[data-loading-message]')) return;
    if (pending.has(button)) {
      event.preventDefault();
      return;
    }
    pending.add(button);
    setButtonLoading(button, true);
  };

  document.addEventListener('submit', (event) => startNavigation(event, event.submitter));
  document.addEventListener('click', (event) => {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    startNavigation(event, event.target.closest('a[data-loading-message]'));
  });
  window.addEventListener('pageshow', (event) => {
    if (!event.persisted) return;
    for (const button of pending) setButtonLoading(button, false);
    pending.clear();
  });
}

if (typeof document !== 'undefined') {
  connectNavigationLoading();
  const saved = localStorage.getItem('pembana-theme');
  if (saved === 'dark' || saved === 'light') {
    applyTheme(saved);
  }
  themeButton?.addEventListener('click', () => {
    applyTheme(nextTheme(document.documentElement.dataset.bsTheme || 'light'));
  });
}
