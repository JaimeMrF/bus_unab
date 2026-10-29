(() => {
  const nav = document.getElementById('nav');
  const onScroll = () => nav.classList.toggle('is-stuck', scrollY > 24);
  addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  // Revelado al entrar en pantalla, con escalonado dentro de cada rejilla.
  const items = [...document.querySelectorAll('.reveal')];
  items.forEach((el) => {
    const hermanos = [...el.parentElement.children].filter((n) => n.classList.contains('reveal'));
    el.style.setProperty('--d', `${Math.min(hermanos.indexOf(el), 5) * 0.08}s`);
  });
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } });
    }, { threshold: 0.12, rootMargin: '0px 0px -6% 0px' });
    items.forEach((el) => io.observe(el));
  } else {
    items.forEach((el) => el.classList.add('in'));
  }

  // Pestañas de paneles.
  const img = document.getElementById('panelImg');
  const cap = document.getElementById('panelCap');
  const base = img.src.replace(/[^/]+$/, '');
  const tabs = [...document.querySelectorAll('.tabs [role="tab"]')];
  tabs.forEach((t) => t.addEventListener('click', () => {
    tabs.forEach((o) => o.setAttribute('aria-selected', String(o === t)));
    img.classList.add('swap');
    setTimeout(() => {
      img.src = `${base}${t.dataset.img}.webp`;
      img.alt = t.textContent.trim();
      cap.textContent = t.dataset.cap;
      img.onload = () => img.classList.remove('swap');
    }, 180);
  }));
})();
