// toon-ui runtime for the ToonX.dc.html component library. Needs toon-engine.js loaded first.
// One shared rAF loop; each instance sleeps when idle. Theme: ToonKit.setTheme('light'|'night'|'cel') or per-instance `theme` prop.
(function () {
  const K = { slow: 1, clock: 0 };
  const MAP = { light: 'il', night: 'in', cel: 'bc', il: 'il', in: 'in', bc: 'bc' };
  K.eng = () => K._E || (window.ToonEngine ? (K._E = new window.ToonEngine()) : null);
  K.themeKey = p => MAP[p] || MAP[document.documentElement.dataset.toonTheme] || 'il';
  K.setTheme = t => { document.documentElement.dataset.toonTheme = t; window.dispatchEvent(new Event('toon-theme')); };
  K.setReduceMotion = on => { document.documentElement.dataset.toonReduce = on ? '1' : '0'; window.dispatchEvent(new Event('toon-theme')); };
  K.reduce = () => document.documentElement.dataset.toonReduce === '1' || (document.documentElement.dataset.toonReduce !== '0' && !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches));
  const live = new Set(); let raf = 0, last = 0;
  const loop = now => { const dt = last ? Math.min(50, now - last) : 16; last = now; K.clock += dt * K.slow; [...live].forEach(c => c.frame(dt * K.slow)); if (live.size) raf = requestAnimationFrame(loop); else { raf = 0; last = 0; } };
  K.wake = c => { live.add(c); if (!raf) raf = requestAnimationFrame(loop); };
  K.sleep = c => live.delete(c);
  const clampI = (v, n) => Math.max(0, Math.min(n - 1, v));
  const C = {};

  class Base {
    constructor(dc) {
      this.dc = dc; this.svg = null; this.last = ''; this.an = {}; this.sq = { p: 0, v: 0, mode: 'rest', pt: 0, p0: 0 };
      this.ref = e => { this.svg = e; this.last = ''; this.draw(); if (e) K.wake(this); };
      this.onTheme = () => { this.last = ''; this.dc.forceUpdate(); this.draw(); if (this.alive && this.alive()) K.wake(this); };
      window.addEventListener('toon-theme', this.onTheme);
      this.init && this.init();
    }
    get p() { return this.dc.props || {}; }
    get E() { return K.eng(); }
    get T() { return this.E.THEMES[K.themeKey(this.p.theme)]; }
    start(n, dur = 900, fade = false) { this.an[n] = { t: K.clock, dur }; if (fade && K.reduce()) { this.fade = { t: K.clock, from: this._sc || { L: [], X: [] } }; this.an._fade = { t: K.clock, dur: 140 }; } K.wake(this); }
    age(n) { const a = this.an[n]; if (!a) return null; return K.reduce() ? 1e6 : K.clock - a.t; }
    press() { if (this.p.disabled) return; const s = this.sq; s.mode = 'press'; s.pt = 0; s.p0 = s.p; K.wake(this); }
    release() { const s = this.sq; if (s.mode !== 'press') return false; s.mode = 'spring'; K.wake(this); return true; }
    stepSq(ms) {
      const s = this.sq; if (s.mode === 'rest') return;
      if (K.reduce()) { s.p = s.mode === 'press' ? 1 : 0; if (s.mode !== 'press') s.mode = 'rest'; return; }
      if (s.mode === 'press') { s.pt += ms; const u = Math.min(1, s.pt / 80); s.p = s.p0 + (1 - s.p0) * (1 - Math.pow(1 - u, 3)); s.v = 0; return; }
      const w = 45, z = .35, n = 4, h = ms / 1000 / n;
      for (let i = 0; i < n; i++) { const a = -w * w * s.p - 2 * z * w * s.v; s.v += a * h; s.p += s.v * h; }
      if (Math.abs(s.p) < .002 && Math.abs(s.v) < .02) { s.p = 0; s.v = 0; s.mode = 'rest'; }
    }
    frame(ms) {
      this.stepSq(ms); this.step && this.step(ms); this.draw();
      const busy = this.sq.mode !== 'rest' || Object.values(this.an).some(a => K.clock - a.t < a.dur) || (this.alive && this.alive());
      if (!busy) K.sleep(this);
    }
    draw() {
      if (!this.svg || !this.E) return; const sc = this.scene(); this._sc = sc; let out = sc;
      if (this.fade) { const u = (K.clock - this.fade.t) / 120; if (u < 1 && K.reduce()) out = this.E.cat(this.E.place(this.fade.from, 0, 0, 0, 0, 0, 1 - u), this.E.place(sc, 0, 0, 0, 0, 0, u)); else this.fade = null; }
      const s = this.E.toSvg(out); if (s !== this.last) { this.svg.innerHTML = s; this.last = s; }
    }
    detach() { K.sleep(this); window.removeEventListener('toon-theme', this.onTheme); }
    update(prev) { this.changed && this.changed(prev || {}); this.last = ''; this.draw(); if (this.alive && this.alive()) K.wake(this); }
    loc(e) { const r = e.currentTarget.getBoundingClientRect(), [w, h] = this.box(); return [(e.clientX - r.left) * w / (r.width || 1), (e.clientY - r.top) * h / (r.height || 1)]; }
    vals() {
      const [w, h, pd] = this.box();
      return { w, h, sw: w + pd[1] + pd[3], sh: h + pd[0] + pd[2], vb: `${-pd[3]} ${-pd[0]} ${w + pd[1] + pd[3]} ${h + pd[0] + pd[2]}`, sx: -pd[3], sy: -pd[0], ov: this.ov || 'visible',
        svgRef: this.ref, cursor: this.p.disabled ? 'default' : 'pointer', down: e => this.down && this.down(e), up: e => this.up && this.up(e),
        leave: () => { this.release(); this.leave && this.leave(); }, ...(this.extra ? this.extra() : {}) };
    }
  }
  const sel = (proto) => { proto.syncSel = function (prev, key = 'selected') { if (this.p[key] !== prev[key] && this.p[key] != null && this.p[key] !== this.sel) { this.from = this.sel; this.sel = this.p[key]; this.start('s', 700); } }; };

  C.button = class extends Base {
    box() { const w = this.p.width || Math.max(120, Math.round(this.E.tw(this.p.label || 'Button', this.T.body, 700, 18) + 56)); return [w, 52, [14, 16, 16, 14]]; }
    alive() { return !!this.p.loading || (!!this.p.idle && this.p.variant !== 'text' && !this.p.disabled && !K.reduce()); }
    changed(prev) { if (this.p.loading && !prev.loading) this.start('load', 400, true); }
    down() { if (this.p.disabled) { this.start('buzz', 340); return; } this.press(); }
    up() { if (this.release() && this.p.onClick && !this.p.loading) this.p.onClick(); }
    scene() {
      const [w] = this.box(), lt = this.p.loading ? (this.age('load') ?? 1e6) : null, idle = !!this.p.idle && !K.reduce();
      const br = idle ? 1 + .0075 * (1 - Math.cos(2 * Math.PI * K.clock / 2500)) : 1, fr = idle ? Math.floor(K.clock / 100) % 3 : 0;
      const sc = this.E.btn(this.T, { v: this.p.variant || 'filled', w, p: this.sq.p, label: this.p.label || 'Button', disabled: this.p.disabled, lt, spin: K.clock, br, seed: fr * 31 }), tb = this.age('buzz');
      if (tb == null || tb > 320) return sc;
      const v = this.E.vibe(this.T, tb / 320, 0, 0, w, 52); return this.E.cat({ L: v.L, X: [] }, this.E.place(sc, v.dx, 0));
    }
  };
  C.iconbutton = class extends Base {
    init() { this.sel = !!this.p.selected; }
    changed(prev) { if (this.p.selected !== prev.selected) this.sel = !!this.p.selected; }
    box() { return [48, 48, [10, 12, 10, 10]]; }
    down() { this.press(); }
    up() { if (!this.release()) return; this.start('b', 400); if (this.p.toggle !== false) { this.sel = !this.sel; if (this.sel) this.start('pop', 260); this.p.onChange && this.p.onChange(this.sel); } this.p.onClick && this.p.onClick(); }
    scene() { const b = this.age('b'), tp = this.age('pop'), fl = this.sel && tp != null && tp < 220 ? this.E.flash(this.T, tp / 220, 24, 24, 30) : { L: [], X: [] }; return this.E.cat(fl, this.E.iconBtn(this.T, { icon: this.p.icon || 'heart', variant: this.p.variant, p: this.sq.p, iy: b == null ? 0 : this.E.iconBounce(b), sel: this.sel, disabled: this.p.disabled })); }
  };
  C.fab = class extends Base {
    init() { if (this.p.land !== false) this.start('land', 700 + (this.p.delay || 0), true); }
    alive() { return this.p.idle !== false && !this.p.disabled && !K.reduce(); }
    box() { const f = this.E.fab(this.T, { label: this.p.label }); return [Math.ceil(f.w), 64, [84, 24, 18, 24]]; }
    down() { this.press(); }
    up() { if (this.release() && this.p.onClick) this.p.onClick(); }
    scene() {
      const E = this.E, T = this.T, t = this.age('land'), a = t == null ? { op: 1, dy: 0, sx: 1, sy: 1, du: -1 } : E.fabAppear(t - (this.p.delay || 0));
      const idle = this.alive(), br = idle ? 1 + .009 * (1 - Math.cos(2 * Math.PI * K.clock / 2200)) : 1, fr = idle ? Math.floor(K.clock / 100) % 3 : 0;
      const f = E.fab(T, { label: this.p.label, icon: this.p.icon, p: this.sq.p, sx: a.sx, sy: a.sy, disabled: this.p.disabled, br, seed: fr * 31 });
      return E.cat(E.place(f, 0, a.dy, 0, 0, 0, a.op), E.dust(T, a.du, f.w / 2, 66, 40));
    }
  };
  C.textfield = class extends Base {
    init() { this.val = this.p.value || ''; this.focus = false; }
    changed(prev) { if (this.p.value !== prev.value && this.p.value != null) this.val = this.p.value; if (this.p.error && !prev.error) this.start('err', 500); }
    box() { return [this.p.width || 342, 106, [6, 30, 6, 14]]; }
    alive() { return this.focus && !K.reduce(); }
    scene() {
      const E = this.E, T = this.T, w = this.box()[0], tf = this.age('f');
      const a = tf == null ? { lift: this.focus || this.val ? 1 : 0, thick: this.focus ? 1 : 0, bounce: 1 } : E.fldFocus(tf, this.focus);
      if (this.val) a.lift = 1;
      const te = this.age('err'), x = te == null ? 0 : E.shake(te), err = !!this.p.error;
      const fld = E.field(T, { w, label: this.p.label || 'Label', value: '', focus: this.focus, ...a, caret: false, error: err, helper: err ? this.p.error : (this.p.helper || ''), disabled: this.p.disabled });
      const ft = this.an.f ? this.an.f.t : 0;
      if (this.focus && (K.reduce() || ((K.clock - ft) % 800) < 500)) fld.L.push(E.L(`M${(22 + E.tw(this.val, T.body, 500, 16)).toFixed(1)} 18 h3.2 v22 h-3.2 Z`, T.primary));
      return E.place(E.cat(fld, te == null ? { L: [], X: [] } : E.stars(T, te / 350, w - 30, -4, 26)), x, 22);
    }
    extra() {
      const T = this.T;
      return { value: this.val, ff: T.body, tc: T.text, iw: this.box()[0], dis: !!this.p.disabled,
        onInput: e => { this.val = e.target.value; this.dc.forceUpdate(); this.draw(); this.p.onChange && this.p.onChange(this.val); },
        onFocus: () => { this.focus = true; this.start('f', 400); }, onBlur: () => { this.focus = false; this.start('f', 400); } };
    }
  };
  C.checkbox = class extends Base {
    init() { this.on = !!this.p.checked; }
    changed(prev) { if (this.p.checked !== prev.checked) this.on = !!this.p.checked; }
    box() { const l = this.p.label; return [l ? 58 + Math.ceil(this.E.tw(l, this.T.body, 600, 16)) + 4 : 48, 48, [8, 8, 8, 8]]; }
    up() { if (this.p.disabled) return; this.on = !this.on; this.start('t', 300, this.on); this.p.onChange && this.p.onChange(this.on); }
    scene() { const t = this.age('t'), a = t == null ? { on: this.on } : this.E.chkAnim(t, this.on); return this.E.check(this.T, { ...a, label: this.p.label, disabled: this.p.disabled, error: this.p.error }); }
  };
  C.radio = class extends Base {
    init() { this.sel = this.p.value ?? 0; this.prev = -1; }
    changed(prev) { if (this.p.value !== prev.value && this.p.value != null) this.sel = this.p.value; }
    opts() { return String(this.p.options || 'Morning,Noon,Evening').split(',').map(s => s.trim()).filter(Boolean); }
    box() { const o = this.opts(); return [54 + Math.ceil(Math.max(...o.map(s => this.E.tw(s, this.T.body, 600, 16)))) + 8, o.length * 52, [34, 8, 8, 8]]; }
    up(e) { if (this.p.disabled) return; const [, y] = this.loc(e), i = clampI(Math.floor(y / 52), this.opts().length); if (i === this.sel) return; this.prev = this.sel; this.sel = i; this.start('s', 450); this.p.onChange && this.p.onChange(i); }
    scene() {
      const E = this.E, t = this.age('s');
      return E.cat(...this.opts().map((lb, i) => { let o = { label: lb, disabled: this.p.disabled }; if (i === this.sel) o = { ...o, sel: true, ...(t != null && t < 400 ? E.radioSelect(t) : {}) }; else if (i === this.prev && t != null && t < 120) o = { ...o, ds: 1 - t / 120 }; return E.place(E.radio(this.T, o), 0, i * 52); }));
    }
  };
  C.switch = class extends Base {
    init() { this.on = !!this.p.checked; }
    changed(prev) { if (this.p.checked !== prev.checked) this.on = !!this.p.checked; }
    box() { const l = this.p.label, lw = l ? Math.ceil(this.E.tw(l, this.T.body, 600, 16)) + 16 : 0; return [(this.p.width || lw + 60), 48, [8, 14, 8, 10]]; }
    up() { if (this.p.disabled) return; this.on = !this.on; this.start('t', 520); this.p.onChange && this.p.onChange(this.on); }
    scene() {
      const E = this.E, T = this.T, [w] = this.box(), t = this.age('t'), a = t != null && t < 480 ? E.swAnim(t, this.on ? 0 : 1, this.on ? 1 : 0) : { pos: this.on ? 1 : 0, trackOn: this.on };
      const sc = E.place(E.sw(T, { ...a, on: this.on, disabled: this.p.disabled }), w - 60, 0);
      if (this.p.label) sc.X.push(E.X(this.p.label, 0, 30, { ff: T.body, fw: 600, fs: 16, fill: T.text, op: this.p.disabled ? .6 : 1 }));
      return sc;
    }
  };
  C.chip = class extends Base {
    init() { this.sel = !!this.p.selected; }
    changed(prev) { if (this.p.selected !== prev.selected) this.sel = !!this.p.selected; }
    opts() { return { variant: this.p.variant || 'filter', label: this.p.label || 'Chip', icon: this.p.icon, disabled: this.p.disabled }; }
    box() { const c = this.E.chip(this.T, { ...this.opts(), sel: this.sel }); return [Math.ceil(c.w), 48, [6, 14, 8, 12]]; }
    down() { this.press(); }
    up() { if (!this.release()) return; if (this.opts().variant === 'filter') { this.sel = !this.sel; if (this.sel) this.start('s', 700); this.dc.forceUpdate(); this.p.onChange && this.p.onChange(this.sel); } this.p.onClick && this.p.onClick(); }
    scene() { const t = this.age('s'), a = this.sel ? (t != null && t < 700 ? this.E.chipSelect(t) : { scale: 1, swipe: 1 }) : { scale: 1, swipe: 0 }; const c = this.E.chip(this.T, { ...this.opts(), sel: this.sel, ...a, p: this.sq.p }); return this.E.place(c, 0, 6, this.p.tilt || 0, c.w / 2, 18); }
  };
  C.card = class extends Base {
    init() { this.pr = 0; this.tgt = 0; this.drag = 0; this.dv = 0; this.gone = false; this.ov = 'visible'; if (this.p.appear) this.start('a', 700 + (this.p.delay || 0), true); }
    changed(prev) { if (this.p.title !== prev.title || (this.p.appear && !prev.appear)) { this.gone = false; delete this.an.d; if (this.p.appear) this.start('a', 700 + (this.p.delay || 0), true); } }
    box() { return [this.p.width || 342, 84, [34, 22, 26, 22]]; }
    step(ms) { this.pr += (this.tgt - this.pr) * Math.min(1, ms / 35); if (!this.dn && this.drag) { const w = 30, z = .55, h = ms / 1000; this.dv += (-w * w * this.drag - 2 * z * w * this.dv) * h; this.drag += this.dv * h; if (Math.abs(this.drag) < .3 && Math.abs(this.dv) < 3) { this.drag = 0; this.dv = 0; } } }
    alive() { return Math.abs(this.tgt - this.pr) > .01 || !!this.drag || !!this.dn; }
    down(e) { if (this.p.disabled || this.gone) return; const [x] = this.loc(e); this.x0 = x; this.dn = true; this.moved = false; this.lean = Math.max(-1, Math.min(1, (x / this.box()[0] - .5) * 2)); this.tgt = 1; K.wake(this); }
    move(e) { if (!this.dn || !this.p.dismissible) return; const [x] = this.loc(e), d = x - this.x0; if (Math.abs(d) > 6) { this.moved = true; this.tgt = 0; } if (this.moved) this.drag = d; }
    up() {
      if (!this.dn) return; this.dn = false; this.tgt = 0; K.wake(this);
      if (this.p.dismissible && Math.abs(this.drag) > 80) { this.dir = Math.sign(this.drag); this.d0 = this.drag; this.drag = 0; this.start('d', 520, true); clearTimeout(this._dt); this._dt = setTimeout(() => { this.gone = true; this.p.onDismiss && this.p.onDismiss(); }, K.reduce() ? 0 : 200 / (K.slow || 1)); return; }
      if (!this.moved) this.p.onClick && this.p.onClick();
    }
    leave() { if (this.dn) this.up(); this.tgt = 0; K.wake(this); }
    extra() { return { move: e => this.move(e) }; }
    scene() {
      const E = this.E, T = this.T, w = this.box()[0], i = (this.p.index || 0) % 3, rot0 = this.p.tilt === false ? 0 : T.rots[i];
      const t = this.age('a'), a = t == null ? { op: 1, dy: 0, rot: rot0, sx: 1, sy: 1, du: -1 } : E.cardAppear(t - (this.p.delay || 0), rot0);
      const c = E.card(T, { idx: i, w, title: this.p.title || 'Title', meta: this.p.meta || '', time: this.p.time || '', sx: a.sx, sy: a.sy, press: this.pr, sel: this.p.selected, dis: this.p.disabled });
      const td = this.age('d');
      if (td != null) {
        const d = E.cardDismiss(td), dir = this.dir || 1, dx = (this.d0 || 0) * Math.max(0, 1 - td / 70) + dir * Math.max(0, d.dx), hu = (td - 170) / 300;
        const hole = hu >= 0 && hu < 1 ? E.place(E.hole(T, hu, 0, 0, w, 84, [11, 17, 13, 19].map(v => v * T.rs), 5), 0, 0, rot0, w / 2, 42) : { L: [], X: [] };
        const lines = d.lu >= 0 && d.lu < 1 ? (dir > 0 ? E.speed(T, d.lu, Math.min(dx, w), 0, 84) : E.place(E.speed(T, d.lu, Math.min(-dx, w), 0, 84), w, 0, 0, 0, 0)) : { L: [], X: [] };
        if (dir < 0) lines.L = lines.L.map(l => ({ ...l, tf: `translate(${w} 0) scale(-1 1) ` + l.tf.replace(/^translate\([^)]*\)\s*/, '') }));
        const cardSc = d.op > .01 ? E.scaleAbout(E.place(c, dx, 0, rot0, w / 2, 42).L, w / 2 + dx, 42, d.sx, d.sy) : [];
        return E.cat(hole, lines, { L: cardSc, X: d.op > .01 ? E.place(c, dx, 0).X : [] });
      }
      if (this.gone) return { L: [], X: [] };
      return E.cat(E.place(c, this.drag, a.dy, a.rot + this.pr * 1.6 * (this.lean ?? -.6) + this.drag * .035, w / 2 + this.drag, 42, a.op * (1 - Math.min(.35, Math.abs(this.drag) / 400))), E.dust(T, a.du, 34, 86, 36), E.dust(T, a.du, w - 34, 86, 36));
    }
  };
  C.listitem = class extends Base {
    init() { this.on = !!this.p.on; this.pd = false; }
    changed(prev) { if (this.p.on !== prev.on) this.on = !!this.p.on; }
    box() { return [this.p.width || 342, 72, [6, 10, 10, 10]]; }
    down() { if (this.p.disabled) return; this.pd = true; this.start('pd', 200); }
    up() { if (!this.pd) return; this.pd = false; this.start('rel', 500); const tr = this.p.trailing; if (tr === 'switch' || tr === 'check') { this.on = !this.on; this.start('sw', 520); this.p.onChange && this.p.onChange(this.on); } this.p.onClick && this.p.onClick(); }
    leave() { if (this.pd) { this.pd = false; this.start('rel', 500); } }
    alive() { return this.pd; }
    scene() {
      const E = this.E, tp = this.age('pd'), tr = this.age('rel'); let sag = 0;
      if (this.pd) sag = E.listPress(tp ?? 1e6, true); else if (tr != null && tr < 500) sag = E.listPress(tr, false);
      const ts = this.age('sw'), swA = this.p.trailing === 'switch' && ts != null && ts < 480 ? E.swAnim(ts, this.on ? 0 : 1, this.on ? 1 : 0) : null;
      return E.listItem(this.T, { w: this.box()[0], icon: this.p.icon || 'star', title: this.p.title || 'Title', sub: this.p.sub, trailing: this.p.trailing || 'chevron', on: this.on, swA, sag, last: this.p.last, disabled: this.p.disabled, seed: this.p.index || 0 });
    }
  };
  C.divider = class extends Base {
    box() { return [this.p.width || 342, 12, [2, 2, 2, 2]]; }
    scene() { const w = this.box()[0]; return this.E.divider(this.T, { x0: this.p.inset ? 56 : 0, x1: w, th: { s: 2.4, m: 3, l: 4.6 }[this.p.weight || 'm'], seed: this.p.seed || 0 }); }
  };
  C.dialog = class extends Base {
    init() {
      this.W = 360; this.H = 640; this.open = !!this.p.open; if (this.open) this.start('o', 500, true);
      this.rootRef = e => { if (this.ro) this.ro.disconnect(); if (!e) return; const m = () => { const r = e.getBoundingClientRect(); if (r.width && (Math.abs(r.width - this.W) > 1 || Math.abs(r.height - this.H) > 1)) { this.W = r.width; this.H = r.height; this.dc.forceUpdate(); this.last = ''; this.draw(); } }; m(); this.ro = new ResizeObserver(m); this.ro.observe(e); };
    }
    detach() { super.detach(); this.ro && this.ro.disconnect(); }
    changed() { if (!!this.p.open !== this.open) { this.open = !!this.p.open; this.start(this.open ? 'o' : 'c', 700, true); this.dc.forceUpdate(); } }
    box() { return [this.W, this.H, [0, 0, 0, 0]]; }
    lines() { const E = this.E, T = this.T, out = []; let cur = ''; String(this.p.body || '').split(/\s+/).filter(Boolean).forEach(w => { const n = cur ? cur + ' ' + w : w; if (E.tw(n, T.body, 400, 15) > 252 && cur) { out.push(cur); cur = w; } else cur = n; }); if (cur) out.push(cur); return out; }
    dl() { return this.E.dialog(this.T, { w: 300, title: this.p.title || 'Title', body: this.lines(), confirm: this.p.confirm || 'OK', cancel: this.p.cancel || 'Cancel', destructive: this.p.destructive }); }
    up(e) {
      if (!this.open) return;
      const [x, y] = this.loc(e), d = this.dl(), x0 = (this.W - 300) / 2, y0 = this.H / 2 - d.h / 2, by = y0 + d.h - 72, okX = x0 + 300 - 24 - 128;
      if (y >= by && y <= by + 52 && x >= okX && x <= okX + 128) { this.p.onConfirm && this.p.onConfirm(); return; }
      if ((y >= by && y <= by + 52 && x >= okX - 110 && x < okX) || x < x0 || x > x0 + 300 || y < y0 || y > y0 + d.h) this.p.onCancel && this.p.onCancel();
    }
    scene() {
      const E = this.E, T = this.T, W = this.W, H = this.H, cx = W / 2, cy = H / 2, D = Math.hypot(cx, cy) + 10, d = this.dl(), R1 = Math.hypot(150, d.h / 2) + 16;
      const to = this.age('o'), tc = this.age('c'); let a;
      if (this.open) a = E.dialogOpen(to == null ? 1e6 : to); else if (tc != null && tc < 320) a = E.dialogClose(tc); else return { L: [], X: [] };
      const R = R1 + (D - R1) * Math.max(0, Math.min(1.2, (a.R - 190) / 140));
      const placed = E.place(d, (W - 300) / 2, cy - d.h / 2), s = a.s, tf = `translate(${cx} ${cy}) scale(${Math.max(.001, s).toFixed(3)}) translate(${-cx} ${-cy})`;
      const parts = [{ L: [E.iris(T, R, cx, cy, W, H)], X: [] }];
      if (a.op > .01 && s > .01) parts.push({ L: placed.L.map(l => ({ ...l, tf: tf + ' ' + l.tf, op: l.op * a.op })), X: placed.X.map(x => ({ ...x, op: x.op * a.textOp })) });
      if (a.stars > 0) parts.push(E.stars(T, a.stars, cx, cy, 70));
      return E.cat(...parts);
    }
    extra() { return { rootRef: this.rootRef, pe: this.open ? 'auto' : 'none' }; }
  };
  C.snackbar = class extends Base {
    init() { this.shown = this.p.shown !== false; if (this.shown) this.start('s', 600, true); }
    changed() { const s = this.p.shown !== false; if (s !== this.shown) { this.shown = s; this.start(s ? 's' : 'h', 400, true); } }
    box() { return [this.p.width || 342, 56, [36, 18, 110, 14]]; }
    up(e) { if (!this.shown || !this.p.action) return; const [x] = this.loc(e); if (x > this.box()[0] - 110) this.p.onAction && this.p.onAction(); }
    scene() {
      const E = this.E, w = this.box()[0], t = this.age(this.shown ? 's' : 'h'); let a;
      if (this.shown) a = t == null ? { dy: 0, rot: 0, sy: 1 } : E.snkShow(t); else { if (t == null || t > 260) return { L: [], X: [] }; a = E.snkHide(t); }
      return E.place(E.snack(this.T, { w, type: this.p.type || 'info', text: this.p.text || 'Saved', action: this.p.action, sy: a.sy, speedU: !this.shown && t > 70 ? (t - 70) / 190 : -1 }), 0, a.dy, a.rot, w / 2, -26);
    }
  };
  C.progress = class extends Base {
    box() { return this.p.kind === 'circular' ? [48, 48, [6, 6, 6, 6]] : [this.p.width || 280, 14, [18, 6, 6, 4]]; }
    alive() { const v = this.p.value; return v == null || v === '' || +v < 0; }
    scene() {
      const E = this.E, ind = this.alive(), v = Math.max(0, Math.min(1, +this.p.value || 0));
      if (this.p.kind === 'circular') return ind ? E.circular(this.T, { ph: K.reduce() ? 0 : K.clock }) : E.circular(this.T, { value: v });
      const w = this.box()[0]; return E.linear(this.T, ind ? { w, ...(K.reduce() ? { a: 0, b: .33 } : E.worm(K.clock)) } : { w, a: 0, b: v });
    }
  };
  C.badge = class extends Base {
    init() { this.n = this.p.count ?? 3; this.old = null; this.start('a', 400); }
    changed() { const n = this.p.count ?? 3, pn = this.n; if (n === pn) return; this.old = pn; if (pn <= 0 && n > 0) this.start('a', 400); else if (n <= 0) this.start('h', 200); else this.start('c', 400); this.n = n; }
    box() { return [28, 24, [12, 14, 12, 14]]; }
    scene() {
      const E = this.E, T = this.T, cx = 14, cy = 12;
      if (this.p.dot) { const t = this.age('a'); return E.place(E.badge(T, { dot: true, s: t == null ? 1 : E.badgeAppear(t).s }), cx, cy); }
      if (this.n <= 0) { const th = this.age('h'); if (th == null || th > 120) return { L: [], X: [] }; return E.place(E.badge(T, { n: this.old ?? 1, s: 1 - th / 120 }), cx, cy); }
      const ta = this.age('a'), tc = this.age('c'), ap = ta == null ? { s: 1, numOp: 1 } : E.badgeAppear(ta);
      let b = { n: this.n > 99 ? '99+' : this.n, s: ap.s, numOp: ap.numOp };
      if (tc != null && tc < 400) { const c = E.badgeCount(tc); b = { ...b, old: this.old, oldDy: c.oldDy, oldOp: c.oldOp, numDy: c.numDy, numOp: Math.min(b.numOp, c.numOp), dx: c.dx }; }
      return E.cat(ta != null && ta < 220 ? E.flash(T, ta / 220, cx, cy, 15) : { L: [], X: [] }, E.place(E.badge(T, b), cx, cy));
    }
  };
  C.topbar = class extends Base {
    init() { this.v0 = 0; }
    box() { return [this.p.width || 390, 76, [8, 8, 12, 8]]; }
    acts() { return String(this.p.actions ?? 'search,more').split(',').map(s => s.trim()).filter(Boolean); }
    kick() { this.v0 = (this.v0 > 0 ? -1 : 1) * 24; this.start('sw', 900); }
    changed(prev) { if (this.p.scrolled !== prev.scrolled) this.kick(); }
    target(x) { const w = this.box()[0], acts = this.acts(); for (let i = 0; i < acts.length; i++) { const ax = w - 12 - 16 - 24 - i * 40; if (x >= ax - 8 && x <= ax + 32) return i; } return x < 76 && this.p.nav !== false ? 'nav' : null; }
    down(e) { const [x] = this.loc(e); this.pk = this.target(x); this.draw(); }
    leave() { if (this.pk != null) { this.pk = null; this.draw(); } }
    up(e) {
      const [x] = this.loc(e), tg = this.target(x); this.pk = null; this.kick();
      if (tg === 'nav') this.p.onNav && this.p.onNav(); else if (tg != null) this.p.onAction && this.p.onAction(this.acts()[tg]);
    }
    scene() { const E = this.E, t = this.age('sw'), w = this.box()[0]; return E.place(E.topBar(this.T, { w, pressed: this.pk, title: this.p.title || 'Title', variant: this.p.variant, scrolled: this.p.scrolled, actions: this.acts(), nav: this.p.nav !== false }), 0, 0, t == null ? 0 : E.sway(t, this.v0), w / 2, -40); }
  };
  C.navbar = class extends Base {
    init() { this.sel = this.p.selected ?? 0; this.from = this.sel; this.ov = 'hidden'; }
    changed(prev) { this.syncSel(prev); }
    items() { return String(this.p.items || 'home:Home,search:Explore,heart:Saved,person:Profile').split(',').map(s => { const [icon, label] = s.split(':').map(x => x.trim()); return { icon, label: label || icon }; }); }
    box() { return [this.p.width || 390, 80, [8, 0, 0, 0]]; }
    leave() { if (this.pi != null) { this.pi = null; this.draw(); } }
    down(e) { const [x] = this.loc(e), n = this.items().length; this.pi = clampI(Math.floor(x / (this.box()[0] / n)), n); K.wake(this); this.draw(); }
    up(e) { this.pi = null; this.draw(); const [x] = this.loc(e), n = this.items().length, i = clampI(Math.floor(x / (this.box()[0] / n)), n); if (i === this.sel) return; this.from = this.sel; this.sel = i; this.start('s', 700); this.p.onChange && this.p.onChange(i); }
    scene() { const E = this.E, w = this.box()[0], it = this.items(), t = this.age('s'), a = t != null && t < 700 ? E.navSelect(t, this.from, this.sel, w / it.length) : {}, bi = this.p.badgeIndex; return E.navBar(this.T, { w, items: it, pressed: this.pi, sel: this.sel, ...a, badge: bi != null && bi >= 0 ? { i: bi, dot: true } : null }); }
  };
  sel(C.navbar.prototype);
  C.tabs = class extends Base {
    init() { this.sel = this.p.selected ?? 0; this.from = this.sel; }
    changed(prev) { this.syncSel(prev); }
    labels() { return String(this.p.labels || 'Today,Weekend,Someday').split(',').map(s => s.trim()).filter(Boolean); }
    box() { return [this.p.width || 342, 52, [6, 8, 8, 8]]; }
    leave() { if (this.pi != null) { this.pi = null; this.draw(); } }
    down(e) { const [x] = this.loc(e), n = this.labels().length; this.pi = clampI(Math.floor(x / (this.box()[0] / n)), n); K.wake(this); this.draw(); }
    up(e) { this.pi = null; this.draw(); const [x] = this.loc(e), n = this.labels().length, i = clampI(Math.floor(x / (this.box()[0] / n)), n); if (i === this.sel) return; this.from = this.sel; this.sel = i; this.start('s', 700); this.p.onChange && this.p.onChange(i); }
    scene() { const E = this.E, w = this.box()[0], lb = this.labels(), t = this.age('s'), a = t != null && t < 700 ? E.tabsSwitch(t, this.from, this.sel, w / lb.length) : { sel: this.sel }; return E.tabs(this.T, { w, labels: lb, pressed: this.pi, sel: a.sel, ia: a.ia, ib: a.ib }); }
  };
  sel(C.tabs.prototype);
  C.icon = class extends Base {
    box() { const s = this.p.size || 24; return [s, s, [2, 2, 2, 2]]; }
    scene() { const T = this.T, c = this.p.color; return { L: this.E.icon(T, this.p.icon || 'star', 0, 0, { size: this.p.size || 24, bold: this.p.weight === 'bold', fill: this.p.filled ? T.chipSel : null, color: c && T[c] ? T[c] : (c || T.text) }), X: [] }; }
  };


  // ---- v1 ----
  const fill = proto => { proto.initFill = function () { this.W = 360; this.H = 640; this.rootRef = e => { if (this.ro) this.ro.disconnect(); if (!e) return; const m = () => { const r = e.getBoundingClientRect(); if (r.width && (Math.abs(r.width - this.W) > 1 || Math.abs(r.height - this.H) > 1)) { this.W = r.width; this.H = r.height; this.dc.forceUpdate(); this.last = ''; this.draw(); } }; m(); this.ro = new ResizeObserver(m); this.ro.observe(e); }; }; proto.detach = function () { Base.prototype.detach.call(this); this.ro && this.ro.disconnect(); }; proto.box = function () { return [this.W, this.H, [0, 0, 0, 0]]; }; proto.extra = function () { return { rootRef: this.rootRef, pe: this.open || (this.an.c && K.clock - this.an.c.t < 300) ? 'auto' : 'none' }; }; };
  const list = (s, d) => String(s || d).split(',').map(x => { const [a, b] = x.split(':').map(y => y.trim()); return b ? [a, b] : [null, a]; });
  C.slider = class extends Base {
    init() { this.v = this.p.value ?? .4; this.g = false; }
    changed(prev) { if (this.p.value !== prev.value && this.p.value != null && !this.g) this.v = this.p.value; }
    box() { return [this.p.width || 300, 48, [10, 14, 10, 14]]; }
    set(e) { const [x] = this.loc(e), w = this.box()[0]; const st = this.p.steps || 0; let v = Math.max(0, Math.min(1, (x - 14) / (w - 28))); if (st > 1) v = Math.round(v * (st - 1)) / (st - 1); if (v !== this.v) { this.v = v; this.p.onChange && this.p.onChange(v); } }
    down(e) { if (this.p.disabled) return; this.g = true; this.start('g', 200); this.set(e); }
    move(e) { if (this.g) { this.set(e); this.draw(); } }
    up() { if (!this.g) return; this.g = false; this.start('r', 500); }
    leave() { this.up(); }
    alive() { return this.g; }
    extra() { return { move: e => this.move(e) }; }
    scene() { const E = this.E, tg = this.age('g'), tr = this.age('r'), q = this.g ? E.sliderGrab(tg ?? 1e6, true) : (tr != null && tr < 500 ? E.sliderGrab(tr, false) : 0); return E.slider(this.T, { w: this.box()[0], value: this.v, grab: q, disabled: this.p.disabled }); }
  };
  C.menu = class extends Base {
    init() { this.open = this.p.open !== false; if (this.open) this.start('o', 600, true); }
    changed() { const o = this.p.open !== false; if (o !== this.open) { this.open = o; this.start(o ? 'o' : 'c', 400, true); } }
    items() { return list(this.p.items, 'edit:Edit,share:Share,delete:Delete'); }
    box() { return [this.p.width || 200, 14 + this.items().length * 44 + 25, [4, 10, 12, 6]]; }
    down(e) { const [, y] = this.loc(e), i = Math.floor((y - 14) / 44); this.pi = i >= 0 && i < this.items().length ? i : null; this.draw(); }
    up() { const i = this.pi; this.pi = null; this.draw(); if (this.open && i != null) this.p.onSelect && this.p.onSelect(i); }
    leave() { this.pi = null; }
    scene() { const E = this.E, it = this.items(), t = this.age(this.open ? 'o' : 'c'), u = t == null ? (this.open ? 1 : 0) : E.menuUnroll(t, this.open); if (u < .01) return { L: [], X: [] }; return E.menu(this.T, { w: this.box()[0], items: it.map(x => x[1]), icons: it.map(x => x[0]), u, pressed: this.pi, sel: this.p.selected }); }
  };
  C.sheet = class extends Base {
    init() { this.initFill(); this.open = !!this.p.open; if (this.open) this.start('o', 600, true); }
    changed() { if (!!this.p.open !== this.open) { this.open = !!this.p.open; this.start(this.open ? 'o' : 'c', 400, true); this.dc.forceUpdate(); } }
    items() { return list(this.p.items, 'person:Family,share:Copy link'); }
    sh() { return Math.min(this.H - 80, 110 + this.items().length * 72); }
    up(e) { if (!this.open) return; const [, y] = this.loc(e), top = this.H - this.sh(); if (y < top - 16) { this.p.onClose && this.p.onClose(); return; } const i = Math.floor((y - top - 70) / 72); if (i >= 0 && i < this.items().length) this.p.onSelect && this.p.onSelect(i); }
    scene() {
      const E = this.E, T = this.T, h = this.sh(), t = this.age(this.open ? 'o' : 'c'); if (!this.open && (t == null || t > 260)) return { L: [], X: [] };
      const a = t == null ? { dy: 0, scrim: 1 } : E.sheetMove(t, this.open, h), s = E.sheet(T, { W: this.W, Hs: this.H, h, dy: a.dy, scrim: a.scrim, title: this.p.title || 'Share plan' });
      return E.cat(s, ...this.items().map(([ic, lb], i) => E.place(E.listItem(T, { w: this.W - 48, icon: ic || 'star', title: lb, trailing: 'chevron', last: i === this.items().length - 1, seed: i }), 24, s.y + 70 + i * 72)));
    }
  };
  fill(C.sheet.prototype);
  C.drawer = class extends Base {
    init() { this.initFill(); this.open = !!this.p.open; if (this.open) this.start('o', 600, true); }
    changed() { if (!!this.p.open !== this.open) { this.open = !!this.p.open; this.start(this.open ? 'o' : 'c', 400, true); this.dc.forceUpdate(); } }
    items() { return list(this.p.items, 'home:Home,calendar:Weekend,heart:Saved,settings:Settings'); }
    dw() { return Math.min(300, this.W - 60); }
    down(e) { const [x, y] = this.loc(e), i = Math.floor((y - 112) / 56); this.pi = this.open && x < this.dw() && i >= 0 && i < this.items().length ? i : null; this.draw(); }
    up(e) { const [x] = this.loc(e), i = this.pi; this.pi = null; if (!this.open) return; if (x > this.dw()) { this.p.onClose && this.p.onClose(); return; } if (i != null) this.p.onSelect && this.p.onSelect(i); this.draw(); }
    scene() { const E = this.E, t = this.age(this.open ? 'o' : 'c'); if (!this.open && (t == null || t > 240)) return { L: [], X: [] }; const a = t == null ? { sx: 1, rot: 0, scrim: 1, textOp: 1 } : (this.open ? E.drawerOpen(t) : E.drawerClose(t)); return E.drawer(this.T, { W: this.dw(), H: this.H, SW: this.W, ...a, title: this.p.title || 'Saturday', items: this.items().map(([a2, b]) => [a2 || 'star', b]), sel: this.p.selected ?? 0, pressed: this.pi }); }
  };
  fill(C.drawer.prototype);
  C.tooltip = class extends Base {
    init() { this.show = this.p.show !== false; if (this.show) this.start('s', 500, true); }
    changed() { const s = this.p.show !== false; if (s !== this.show) { this.show = s; this.start('s', 500, true); } }
    box() { const tp = this.E.tooltip(this.T, { text: this.p.text || 'Tap to add a plan' }); return [Math.ceil(tp.w), 46, [8, 10, 6, 8]]; }
    scene() { const E = this.E, w = this.box()[0], t = this.age('s'), s = t == null ? (this.show ? 1 : 0) : E.tipInflate(t, this.show); return E.place(E.tooltip(this.T, { text: this.p.text || 'Tap to add a plan', s }), w / 2, 46); }
  };
  C.segmented = class extends Base {
    init() { this.sel = this.p.selected ?? 0; this.from = this.sel; }
    changed(prev) { this.syncSel(prev); }
    labels() { return String(this.p.labels || 'Day,Week,Month').split(',').map(s => s.trim()).filter(Boolean); }
    box() { return [this.p.width || 300, 44, [8, 12, 12, 8]]; }
    down(e) { if (this.p.disabled) return; const [x] = this.loc(e), n = this.labels().length; this.pi = clampI(Math.floor(x / (this.box()[0] / n)), n); this.draw(); }
    up() { const i = this.pi; this.pi = null; if (i == null) return; if (i !== this.sel) { this.from = this.sel; this.sel = i; this.start('s', 700); this.p.onChange && this.p.onChange(i); } this.draw(); }
    leave() { this.pi = null; }
    scene() { const E = this.E, w = this.box()[0], lb = this.labels(), t = this.age('s'), a = t != null && t < 700 ? E.segSwitch(t, this.from, this.sel, w / lb.length) : { sel: this.sel }; return E.segmented(this.T, { w, labels: lb, sel: a.sel, ia: a.ia, ib: a.ib, sq: a.sq, pressed: this.pi, disabled: this.p.disabled }); }
  };
  sel(C.segmented.prototype);
  C.pullrefresh = class extends Base {
    init() { this.ov = 'hidden'; this.d = 0; this.dragging = false; this.ref = !!this.p.refreshing; }
    changed(prev) { if (this.p.refreshing !== prev.refreshing) { this.ref = !!this.p.refreshing; if (!this.ref) this.start('done', 300); } }
    box() { return [this.p.width || 360, 140, [0, 0, 0, 0]]; }
    down(e) { if (this.ref) return; this.dragging = true; this.y0 = this.loc(e)[1]; this.d = 0; K.wake(this); }
    move(e) { if (this.dragging) { this.d = Math.max(0, Math.min(120, (this.loc(e)[1] - this.y0) * .8)); this.p.onPull && this.p.onPull(this.d); } }
    up() { if (!this.dragging) return; this.dragging = false; if (this.d > 40) { this.d0 = this.d; this.ref = true; this.start('rel', 500); this.p.onRefresh && this.p.onRefresh(); } else this.start('back', 300); this.d = 0; }
    leave() { this.up(); }
    alive() { return this.dragging || this.ref; }
    extra() { return { move: e => this.move(e) }; }
    scene() {
      const E = this.E, T = this.T, W = this.box()[0];
      if (this.dragging) return E.pull(T, { W, d: this.d, bsy: 1 + this.d / 240, bsx: 1 - this.d / 600 });
      const tr = this.age('rel'); if (this.ref && tr != null && tr < 400) return E.pull(T, { W, ...E.pullRelease(tr, this.d0 || 80), ph: K.clock });
      if (this.ref) return E.pull(T, { W, band: false, blobY: 40, ph: K.clock });
      const td = this.age('done'); if (td != null && td < 260) { const s = 1 - td / 260; return E.pull(T, { W, band: false, blobY: 40, ph: K.clock, bsx: s, bsy: s }); }
      const tb = this.age('back'); return E.pull(T, { W, d: 0, by: tb != null && tb < 300 ? 6 : 6, band: true, blobY: -40 });
    }
  };
  C.titlecard = class extends Base {
    init() { this.ov = 'hidden'; this.start('in', 900, true); }
    box() { return [this.p.width || 360, this.p.height || 520, [0, 0, 0, 0]]; }
    up() { this.start('in', 900, true); this.p.onClick && this.p.onClick(); }
    scene() {
      const E = this.E, [W, H] = this.box(), t = this.age('in'), a = E.splashAt(t == null ? 1e6 : t), empty = this.p.variant === 'empty';
      const sc = E.titleCard(this.T, { W, H, cy: empty ? H * .4 : H * .42, pw: empty ? 260 : 280, ph: empty ? 110 : 120, fs: empty ? 34 : 44, title: this.p.title || (empty ? 'Nothing yet' : 'Saturday'), sub: this.p.sub ?? (empty ? 'Your weekend is wide open' : 'Plans for the good part of the week'), s: a.s, phase: a.phase, textOp: a.textOp });
      return E.cat(sc, E.stars(this.T, a.stars || 0, W / 2, H * .42 - 60, 90));
    }
  };

  K.vals = (dc, kind) => { if (!K.eng()) return null; if (!dc._toon) dc._toon = new C[kind](dc); return dc._toon.vals(); };
  K.attach = (dc, kind) => { const c = dc._toon || (dc._toon = new C[kind](dc)); c.last = ''; c.draw(); K.wake(c); return c; };
  K.controllers = C;
  window.ToonKit = K;
})();
