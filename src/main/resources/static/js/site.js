const themeButton = typeof document === 'undefined' ? null : document.querySelector('#theme-toggle');

export function nextTheme(current) {
  return current === 'dark' ? 'light' : 'dark';
}

export function applyTheme(theme) {
  document.documentElement.dataset.bsTheme = theme;
  localStorage.setItem('pembana-theme', theme);
}

if (typeof document !== 'undefined') {
  const saved = localStorage.getItem('pembana-theme');
  if (saved === 'dark' || saved === 'light') {
    applyTheme(saved);
  }
  themeButton?.addEventListener('click', () => {
    applyTheme(nextTheme(document.documentElement.dataset.bsTheme || 'light'));
  });
}
