// toon-ui shape + motion engine. Single source of truth for geometry; the Kotlin port mirrors these functions 1:1.
(function () {
  const THEMES = {
    il: { key: 'il', label: 'Ink & Gouache', sub: 'base · light', amp: 1.1, ins: [2, 2.6, 4, 2.2], btnR: [16, 11, 18, 12], sh: 4, seed: 11, rs: 1, rots: [-1.2, 0, 1.5],
      ink: '#2B211C', bg: '#CBC3A6', paper: '#F4EBD6', surface2: '#E6D9BC', cardColors: ['#F4EBD6', '#F4EBD6', '#F4EBD6'], sColors: ['#E9B53A', '#7FB2D6', '#E07F9C'],
      text: '#2B211C', muted: '#51463D', primary: '#E2643C', onPrimary: '#2B211C', btnHi: '#E2643C', danger: '#C23F2C', onDanger: '#F4EBD6', dangerText: '#8E2A1C',
      success: '#3E8F5E', successLight: '#8FC79E', warn: '#E9B53A', chipSel: '#E9B53A', field: '#F4EBD6', fieldRim: null, on: '#3E8F5E', off: '#DCD2B8', rim: null, thumb: '#F4EBD6',
      focus: '#2F6FB0', disabledFill: '#E0D6BE', dust: '#EFE6CF', sweat: '#A9D3E8', fxLine: '#2B211C', holeEdge: '#F4EBD6', nail: '#8A7D70', display: 'Shrikhand', body: 'Figtree' },
    in: { key: 'in', label: 'Ink & Gouache', sub: 'base · night', amp: 1.1, ins: [2, 2.6, 4, 2.2], btnR: [16, 11, 18, 12], sh: 4, seed: 11, rs: 1, rots: [-1.2, 0, 1.5],
      ink: '#120D0F', bg: '#1A2140', paper: '#2C3760', surface2: '#36426E', cardColors: ['#2C3760', '#2C3760', '#2C3760'], sColors: ['#D2A955', '#79A3C2', '#C27891'],
      text: '#F1E6CC', muted: '#BDB6CF', primary: '#D9693F', onPrimary: '#120D0F', btnHi: '#F2A57E', danger: '#E06A57', onDanger: '#120D0F', dangerText: '#F29A88',
      success: '#4E9A6C', successLight: '#79B98A', warn: '#D2A955', chipSel: '#D2A955', field: '#232D52', fieldRim: '#6E7BAA', on: '#4E9A6C', off: '#3A4570', rim: '#8392C4', thumb: '#F1E6CC',
      focus: '#9FB4F0', disabledFill: '#2A3355', dust: '#56608A', sweat: '#7FA9CF', fxLine: '#C9C2DA', holeEdge: '#8392C4', nail: '#8392C4', display: 'Shrikhand', body: 'Figtree' },
    bc: { key: 'bc', label: 'Bright Cel', sub: 'alt · light', amp: 0.55, ins: [2.6, 3, 4.6, 2.6], btnR: [22, 16, 24, 18], sh: 6, seed: 23, rs: 1.25, rots: [1, -1.5, 0],
      ink: '#231B17', bg: '#BCD3D6', paper: '#FFF6E2', surface2: '#FFE9B8', cardColors: ['#FFCE3A', '#FF8FB1', '#6EC3F0'], sColors: ['#FFF6E2', '#FFF6E2', '#FFF6E2'],
      text: '#231B17', muted: '#3A2C24', primary: '#FF5A36', onPrimary: '#231B17', btnHi: '#FF5A36', danger: '#D42A20', onDanger: '#FFF6E2', dangerText: '#A11D14',
      success: '#2FB35A', successLight: '#7FD69A', warn: '#FFCE3A', chipSel: '#FFCE3A', field: '#FFF6E2', fieldRim: null, on: '#2FB35A', off: '#EDE3CC', rim: null, thumb: '#FFF6E2',
      focus: '#2D6FD6', disabledFill: '#E4DCCB', dust: '#FFFFFF', sweat: '#BDE6F7', fxLine: '#231B17', holeEdge: '#FFF6E2', nail: '#6B5F55', display: 'Titan One', body: 'Fredoka' },
  };

  const starD = (cx, cy, R, r) => { const p = []; for (let i = 0; i < 10; i++) { const a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 ? r : R; p.push(`${(cx + Math.cos(a) * rr).toFixed(2)} ${(cy + Math.sin(a) * rr).toFixed(2)}`); } return 'M' + p.join(' L') + 'Z'; };
  const gearD = (cx, cy) => { const P = []; for (let i = 0; i < 6; i++) { const a0 = -Math.PI / 2 + i * Math.PI / 3; [[8.7, -.17], [8.7, .17], [6.4, .33], [6.4, .71]].forEach(([r, da]) => { const a = a0 + da, rr = r * (1 + .03 * Math.sin(i * 2.1)); P.push(`${(cx + Math.cos(a) * rr).toFixed(2)} ${(cy + Math.sin(a) * rr * .97).toFixed(2)}`); }); } return 'M' + P.join(' L') + 'Z'; };
  // 24-dp grid. d = stroke path (variable weight is added by the renderer), fill = optional cel fill region.
  const ICONS = {
    home: { d: 'M3.8 11.6 L12 4.4 L20.3 11.3 M6.2 9.8 V19.7 H17.9 V9.7 M10 19.7 V14.4 H14 V19.7', fill: 'M6.2 9.8 L12 4.9 L17.9 9.7 V19.7 H6.2 Z' },
    search: { d: 'M16.5 10.1 A6.3 5.8 -12 1 1 4.3 10.9 A6.3 5.8 -12 1 1 16.5 10.1 Z M14.9 15.2 L20.3 20.4', fill: 'M16.5 10.1 A6.3 5.8 -12 1 1 4.3 10.9 A6.3 5.8 -12 1 1 16.5 10.1 Z' },
    info: { d: 'M20.4 12.3 A8.5 8.2 8 1 1 3.5 11.7 A8.5 8.2 8 1 1 20.4 12.3 Z M12 11 V16.6 M12 7.6 L12.05 7.6', fill: 'M20.4 12.3 A8.5 8.2 8 1 1 3.5 11.7 A8.5 8.2 8 1 1 20.4 12.3 Z' },
    heart: { d: 'M12 19.6 C6 15.6 3.4 12.4 3.8 9 C4.1 6.4 6 4.7 8.3 4.9 C10 5.1 11.2 6.3 12 7.9 C12.8 6.3 14.1 5.1 15.8 5 C18.1 4.8 20 6.5 20.2 9.1 C20.5 12.5 18 15.6 12 19.6 Z', fill: 'M12 19.6 C6 15.6 3.4 12.4 3.8 9 C4.1 6.4 6 4.7 8.3 4.9 C10 5.1 11.2 6.3 12 7.9 C12.8 6.3 14.1 5.1 15.8 5 C18.1 4.8 20 6.5 20.2 9.1 C20.5 12.5 18 15.6 12 19.6 Z' },
    bell: { d: 'M5.8 16.6 C6.8 15.3 7 13.7 7 11.4 C7 7.9 9.1 5.5 12 5.4 C15 5.5 17.1 7.9 17 11.5 C17 13.8 17.3 15.3 18.3 16.6 Z M9.9 19.1 C10.6 20.5 13.5 20.5 14.1 19.1 M12 3.4 V5.4', fill: 'M5.8 16.6 C6.8 15.3 7 13.7 7 11.4 C7 7.9 9.1 5.5 12 5.4 C15 5.5 17.1 7.9 17 11.5 C17 13.8 17.3 15.3 18.3 16.6 Z' },
    person: { d: 'M15.5 7.9 A3.5 3.6 0 1 1 8.5 8.1 A3.5 3.6 0 1 1 15.5 7.9 Z M4.8 20.2 C5.4 16 8.2 13.8 12 13.8 C15.8 13.8 18.6 16 19.2 20.2', fill: 'M15.5 7.9 A3.5 3.6 0 1 1 8.5 8.1 A3.5 3.6 0 1 1 15.5 7.9 Z M4.8 20.2 C5.4 16 8.2 13.8 12 13.8 C15.8 13.8 18.6 16 19.2 20.2 Z' },
    plus: { d: 'M12 4.8 V19.3 M4.9 12.2 H19.2' },
    close: { d: 'M6.2 5.9 L18 18.1 M17.8 6.1 L6 17.9' },
    back: { d: 'M19.2 12.1 H5.4 M11.1 5.9 L5 12 L11.2 18.2' },
    chevron: { d: 'M9.4 5.8 L15.6 12 L9.5 18.2' },
    more: { d: 'M12 5.4 L12.05 5.4 M12 12 L12.05 12 M12 18.6 L12.05 18.6' },
    tune: { d: 'M4 7.1 H20 M4 16.9 H20 M9 4.4 V9.7 M15.1 14.3 V19.6' },
    star: { d: starD(12, 12.6, 8.8, 3.9), fill: starD(12, 12.6, 8.8, 3.9) },
    calendar: { d: 'M4.4 6.9 H19.6 V19.6 H4.4 Z M4.4 10.6 H19.6 M8.4 4.3 V8.2 M15.6 4.3 V8.2', fill: 'M4.4 6.9 H19.6 V10.6 H4.4 Z' },
    check: { d: 'M4.9 12.6 L9.9 17.6 L19.6 6.4' },
    warning: { d: 'M12 3.9 L21.2 19.7 H2.8 Z M12 9.6 V14.1 M12 17.1 L12.05 17.1', fill: 'M12 3.9 L21.2 19.7 H2.8 Z' },
    menu: { d: 'M4.2 6.9 H19.7 M4.4 12.1 H19.5 M4.3 17.3 H19.8' },
    settings: { d: gearD(12, 12.1) + ' M14.9 12.2 A2.9 2.7 10 1 1 9.1 12 A2.9 2.7 10 1 1 14.9 12.2 Z', fill: gearD(12, 12.1) },
    delete: { d: 'M4.9 7.2 H19.1 M9.6 7 V5.1 H14.4 V7 M6.6 7.4 L7.6 19.6 H16.4 L17.4 7.4 M10.2 10.6 V16.4 M13.8 10.6 V16.4', fill: 'M6.6 7.4 L7.6 19.6 H16.4 L17.4 7.4 Z' },
    share: { d: 'M20 6 A2.4 2.3 0 1 1 15.2 6.1 A2.4 2.3 0 1 1 20 6 Z M8.8 12 A2.4 2.3 0 1 1 4 12.1 A2.4 2.3 0 1 1 8.8 12 Z M20 18 A2.4 2.3 0 1 1 15.2 18.1 A2.4 2.3 0 1 1 20 18 Z M8.6 10.8 L15.3 7.2 M8.6 13.2 L15.3 16.9', fill: 'M20 6 A2.4 2.3 0 1 1 15.2 6.1 A2.4 2.3 0 1 1 20 6 Z M8.8 12 A2.4 2.3 0 1 1 4 12.1 A2.4 2.3 0 1 1 8.8 12 Z M20 18 A2.4 2.3 0 1 1 15.2 18.1 A2.4 2.3 0 1 1 20 18 Z' },
    filter: { d: 'M4 5.4 H20.1 L13.9 12.7 V18.9 L10.1 17.1 V12.7 Z', fill: 'M4 5.4 H20.1 L13.9 12.7 V18.9 L10.1 17.1 V12.7 Z' },
    refresh: { d: 'M18.8 9.1 A7.3 7.1 0 1 0 19.4 13.7 M19.3 4.7 V9.3 H14.7' },
    lock: { d: 'M6.1 10.6 H17.9 V19.7 H6.1 Z M8.6 10.4 V7.9 C8.6 5.6 10.1 4.3 12 4.3 C13.9 4.3 15.4 5.6 15.4 7.9 V10.4 M12 14.1 V16.3', fill: 'M6.1 10.6 H17.9 V19.7 H6.1 Z' },
    eye: { d: 'M2.8 12.1 C5.2 8 8.4 6.2 12 6.2 C15.6 6.2 18.8 8 21.2 12 C18.8 16 15.6 17.8 12 17.8 C8.4 17.8 5.2 16 2.8 12.1 Z M14.9 12 A2.9 3 0 1 1 9.1 12.1 A2.9 3 0 1 1 14.9 12 Z', fill: 'M2.8 12.1 C5.2 8 8.4 6.2 12 6.2 C15.6 6.2 18.8 8 21.2 12 C18.8 16 15.6 17.8 12 17.8 C8.4 17.8 5.2 16 2.8 12.1 Z' },
    edit: { d: 'M4.8 19.2 L5.5 15.3 L15.4 5.4 L18.6 8.6 L8.7 18.5 Z M13.4 7.4 L16.6 10.6', fill: 'M4.8 19.2 L5.5 15.3 L15.4 5.4 L18.6 8.6 L8.7 18.5 Z' },
  };

  class ToonEngine {
    constructor() { this.wc = new Map(); this.THEMES = THEMES; this.ICONS = ICONS; this._ctx = null; }
    f(v) { return (Math.round(v * 10) / 10).toString(); }
    c01(v) { return Math.max(0, Math.min(1, v)); }
    osc(t, z, w) { const wd = w * Math.sqrt(1 - z * z); return Math.exp(-z * w * t) * Math.cos(wd * t); }
    spr(t, z, w) { const wd = w * Math.sqrt(1 - z * z); return Math.exp(-z * w * t) * (Math.cos(wd * t) + z / Math.sqrt(1 - z * z) * Math.sin(wd * t)); }
    rng(seed) { let a = seed >>> 0; return () => { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
    tw(s, ff, fw, fs) {
      if (!this._ctx) { try { this._ctx = document.createElement('canvas').getContext('2d'); } catch (e) { } }
      if (!this._ctx) return s.length * fs * .55;
      this._ctx.font = `${fw} ${fs}px "${ff}"`; return this._ctx.measureText(s).width;
    }

    // ---- shape ----
    rr(x, y, w, h, r, step) {
      const P = Math.PI, pts = [];
      const seg = (x0, y0, x1, y1) => { const n = Math.max(1, Math.round(Math.hypot(x1 - x0, y1 - y0) / step)); for (let i = 0; i < n; i++) { const t = i / n; pts.push([x0 + (x1 - x0) * t, y0 + (y1 - y0) * t]); } };
      const arc = (cx, cy, rad, a0, a1) => { const n = Math.max(2, Math.round(Math.abs(a1 - a0) * rad / step)); for (let i = 0; i < n; i++) { const a = a0 + (a1 - a0) * i / n; pts.push([cx + rad * Math.cos(a), cy + rad * Math.sin(a)]); } };
      const m = Math.max(1, Math.min(w, h) / 2);
      const [tl, tr, br, bl] = r.map(v => Math.max(1, Math.min(v, m)));
      seg(x + tl, y, x + w - tr, y); arc(x + w - tr, y + tr, tr, -P / 2, 0);
      seg(x + w, y + tr, x + w, y + h - br); arc(x + w - br, y + h - br, br, 0, P / 2);
      seg(x + w - br, y + h, x + bl, y + h); arc(x + bl, y + h - bl, bl, P / 2, P);
      seg(x, y + h - bl, x, y + tl); arc(x + tl, y + tl, tl, P, 1.5 * P);
      return pts;
    }
    smooth(pts) {
      const f = this.f, N = pts.length, mid = (p, q) => [(p[0] + q[0]) / 2, (p[1] + q[1]) / 2];
      const m0 = mid(pts[N - 1], pts[0]); let d = `M${f(m0[0])} ${f(m0[1])}`;
      for (let i = 0; i < N; i++) { const n = mid(pts[i], pts[(i + 1) % N]); d += `Q${f(pts[i][0])} ${f(pts[i][1])} ${f(n[0])} ${f(n[1])}`; }
      return d + 'Z';
    }
    wob(x, y, w, h, r, amp, seed, step = 6) {
      const key = [x, y, w, h].map(v => v.toFixed(1)).join(',') + '|' + r.map(v => v.toFixed(1)).join(',') + '|' + amp + '|' + seed + '|' + step;
      const hit = this.wc.get(key); if (hit) return hit;
      const rnd = this.rng(seed), ph = [rnd() * 6.28, rnd() * 6.28, rnd() * 6.28];
      const pts = this.rr(x, y, w, h, r, step), N = pts.length;
      const out = pts.map((p, i) => {
        const a = pts[(i - 1 + N) % N], b = pts[(i + 1) % N];
        let nx = b[1] - a[1], ny = -(b[0] - a[0]); const L = Math.hypot(nx, ny) || 1; nx /= L; ny /= L;
        const s = i / N * 6.2832, d = amp * (0.6 * Math.sin(3 * s + ph[0]) + 0.3 * Math.sin(7 * s + ph[1]) + 0.22 * Math.sin(13 * s + ph[2]));
        return [p[0] + nx * d, p[1] + ny * d];
      });
      const d = this.smooth(out);
      if (this.wc.size > 8000) this.wc.clear();
      this.wc.set(key, d); return d;
    }
    shape(x, y, w, h, r, ins, amp, seed, step) {
      const [t, rr, b, l] = ins, ri = r.map(v => Math.max(2, v - (t + l) / 2));
      return { ink: this.wob(x, y, w, h, r, amp, seed, step), fill: this.wob(x + l, y + t, Math.max(1, w - l - rr), Math.max(1, h - t - b), ri, amp * 0.8, seed + 7, step) };
    }
    L(d, fill, o = {}) { return { d, fill, stroke: o.stroke || 'none', sw: o.sw || 0, tf: o.tf || '', op: o.op ?? 1, dash: o.dash || 'none', doff: o.doff || 0, fr: o.fr || 'nonzero' }; }
    X(s, x, y, o) { return { s, x, y, ff: o.ff, fw: o.fw || 600, fs: o.fs || 16, fill: o.fill, anchor: o.anchor || 'start', op: o.op ?? 1 }; }
    body(T, x, y, w, h, r, o) {
      const ins = (o.ins || T.ins).map(v => v * (o.insMul || 1));
      const g = this.shape(x, y, w, h, r, ins, o.amp ?? T.amp, o.seed, o.step), out = [];
      if (o.sh) out.push(this.L(g.ink, T.ink, { tf: `translate(${this.f(o.sh)} ${this.f(o.sh)})`, op: o.inkOp ?? 1 }));
      out.push(this.L(g.ink, o.inkColor || T.ink, { op: o.inkOp ?? 1 }));
      const rim = T.rim && !o.noRim ? (o.rimColor || T.rim) : null;
      if (rim) out.push(this.L(g.fill, rim));
      out.push(this.L(g.fill, o.fill, { tf: rim ? 'translate(0 1.6)' : '' }));
      return out;
    }
    place(sc, dx, dy, rot = 0, px = 0, py = 0, om = 1) {
      const f = this.f, pre = `translate(${f(dx)} ${f(dy)})` + (rot ? ` rotate(${rot.toFixed(2)} ${f(px)} ${f(py)})` : '');
      return { L: sc.L.map(l => ({ ...l, tf: (pre + ' ' + l.tf).trim(), op: l.op * om })), X: sc.X.map(t => ({ ...t, x: t.x + dx, y: t.y + dy, op: t.op * om })) };
    }
    scaleAbout(Ls, px, py, sx, sy) { if (sx === 1 && sy === 1) return Ls; const pre = `translate(${this.f(px)} ${this.f(py)}) scale(${sx.toFixed(3)} ${sy.toFixed(3)}) translate(${this.f(-px)} ${this.f(-py)})`; return Ls.map(l => ({ ...l, tf: (pre + ' ' + l.tf).trim() })); }
    cat(...s) { return { L: s.flatMap(x => x.L), X: s.flatMap(x => x.X) }; }
    ring(x, y, w, h, r, T, seed) { return this.L(this.wob(x, y, w, h, r, T.amp, seed), 'none', { stroke: T.focus, sw: 2.5, dash: '7 5' }); }
    spinner(T, cx, cy, R, ph, sx, sy, fill) {
      const ang = ph + 0.7 * Math.sin(ph);
      const mk = RR => { const pts = []; for (let i = 0; i < 30; i++) { const a = i / 30 * 2 * Math.PI, rr = RR * (1 + .2 * Math.sin(3 * a + ang * 2) + .08 * Math.sin(5 * a - ph)); pts.push([cx + Math.cos(a + ang) * rr * sx, cy + R * (1 - sy) + Math.sin(a + ang) * rr * sy]); } return this.smooth(pts); };
      return [this.L(mk(R + 3), T.ink), this.L(mk(R), fill)];
    }
    inkLine(x0, x1, y, th, seed, sag = 0) {
      const rnd = this.rng(seed), ph = rnd() * 6, n = 28, top = [], bot = [];
      for (let i = 0; i <= n; i++) { const u = i / n, x = x0 + (x1 - x0) * u, t = th * (0.25 + 0.75 * Math.sin(Math.PI * u)) * (1 + .2 * Math.sin(9 * u + ph)), w = 0.8 * Math.sin(5 * u + ph) + sag * Math.sin(Math.PI * u); top.push([x, y + w - t * .4]); bot.push([x, y + w + t * .6]); }
      const f = this.f; return 'M' + top.map(p => `${f(p[0])} ${f(p[1])}`).join(' L') + ' L' + bot.reverse().map(p => `${f(p[0])} ${f(p[1])}`).join(' L') + 'Z';
    }
    star(R, r, seed) { const rnd = this.rng(seed), pts = []; for (let i = 0; i < 10; i++) { const a = -Math.PI / 2 + i * Math.PI / 5, rr = (i % 2 ? r : R) * (0.88 + 0.24 * rnd()); pts.push(`${this.f(Math.cos(a) * rr)} ${this.f(Math.sin(a) * rr)}`); } return 'M' + pts.join(' L') + 'Z'; }
    burst(rx, ry, n, seed, k = .76) { const rnd = this.rng(seed), pts = []; for (let i = 0; i < n * 2; i++) { const a = -Math.PI / 2 + i * Math.PI / n, o = i % 2 === 0, j = .9 + .2 * rnd(); pts.push(`${this.f(Math.cos(a) * (o ? rx : rx * k) * j)} ${this.f(Math.sin(a) * (o ? ry : ry * k) * j)}`); } return 'M' + pts.join(' L') + 'Z'; }
    // Icon = one centerline path per icon. The renderer samples it and builds a filled outline whose width
    // follows the rule: thicker low in the 24 grid and on horizontal runs, thinner high and on verticals (≥ 1.5× range).
    sampleSub(d) {
      const p = document.createElementNS('http://www.w3.org/2000/svg', 'path'); p.setAttribute('d', d);
      const L = p.getTotalLength(), closed = /z\s*$/i.test(d.trim());
      if (L < .5) { const q = p.getPointAtLength(0); return { dot: [q.x, q.y] }; }
      const n = Math.max(10, Math.ceil(L / .3)), pts = [];
      for (let i = 0; i <= n; i++) { const q = p.getPointAtLength(L * i / n); pts.push([q.x, q.y]); }
      if (closed) pts.pop();
      return { pts, closed };
    }
    iconWidth(y, tx, base, s, ph) { return base * (0.72 + 0.42 * this.c01((y - 4) / 16) + 0.3 * tx * tx) * (1 + .06 * Math.sin(s * 1.3 + ph)); }
    iconOutline(name, base) {
      this._io = this._io || new Map(); const key = name + '|' + base; if (this._io.has(key)) return this._io.get(key);
      const ic = ICONS[name]; if (!ic || typeof document === 'undefined') return null;
      const g = v => v.toFixed(2), ph = this.rng(name.length * 31 + Math.round(base * 10))() * 6;
      const poly = P => 'M' + P.map(p => g(p[0]) + ' ' + g(p[1])).join(' L') + 'Z';
      let out = '';
      ic.d.split(/(?=M)/).map(x => x.trim()).filter(Boolean).forEach(sub => {
        const S = this.sampleSub(sub);
        if (S.dot) { const r = base * 1.0, [cx, cy] = S.dot, P = []; for (let i = 0; i < 16; i++) { const a = i / 16 * 2 * Math.PI; P.push([cx + Math.cos(a) * r * 1.05, cy + Math.sin(a) * r * .95]); } out += poly(P); return; }
        const pts = S.pts, N = pts.length, left = [], right = [], W = [], T = [];
        for (let i = 0; i < N; i++) {
          const a = S.closed ? pts[(i - 1 + N) % N] : pts[Math.max(0, i - 1)], b = S.closed ? pts[(i + 1) % N] : pts[Math.min(N - 1, i + 1)];
          let tx = b[0] - a[0], ty = b[1] - a[1]; const l = Math.hypot(tx, ty) || 1; tx /= l; ty /= l;
          const k = S.closed ? 1 : Math.min(1, .6 + .4 * Math.min(i, N - 1 - i) / 6), w = this.iconWidth(pts[i][1], tx, base, i * .3, ph) / 2 * k;
          const nx = -ty, ny = tx; left.push([pts[i][0] + nx * w, pts[i][1] + ny * w]); right.push([pts[i][0] - nx * w, pts[i][1] - ny * w]); W.push(w); T.push([tx, ty]);
        }
        if (S.closed) { out += poly(left) + poly(right.slice().reverse()); return; }
        const cap = (i, sgn) => { const [cx, cy] = pts[i], [tx, ty] = T[i], nx = -ty, ny = tx, r = W[i], P = []; for (let j = 1; j < 8; j++) { const a = j / 8 * Math.PI; P.push([cx + r * (Math.cos(a) * nx * sgn + Math.sin(a) * tx * sgn), cy + r * (Math.cos(a) * ny * sgn + Math.sin(a) * ty * sgn)]); } return P; };
        out += poly([...left, ...cap(N - 1, 1), ...right.slice().reverse(), ...cap(0, -1)]);
      });
      this._io.set(key, out); return out;
    }
    icon(T, name, x, y, o = {}) {
      const ic = ICONS[name]; if (!ic) return [];
      const s = (o.size || 24) / 24, base = o.bold ? 2.7 : 2.1, col = o.color || T.text, op = o.op ?? 1;
      const tf = `translate(${this.f(x)} ${this.f(y)}) scale(${s.toFixed(3)})`, out = [];
      if (o.fill && ic.fill) out.push(this.L(ic.fill, o.fill, { tf, op }));
      const od = this.iconOutline(name, base);
      if (od) out.push(this.L(od, col, { tf, op }));
      else out.push(this.L(ic.d, 'none', { stroke: col, sw: base, tf, op }));
      return out;
    }
    iconSvg(name, bold, fill) { const ic = ICONS[name], od = this.iconOutline(name, bold ? 2.7 : 2.1); return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">${fill && ic.fill ? `<path d="${ic.fill}" fill="${fill}"/>` : ''}<path d="${od}" fill="currentColor"/></svg>`; }

    // ---- core components ----
    btn(T, o) {
      const c01 = this.c01, f = this.f, w = o.w, H = 52, p = o.p || 0, pl = o.pl ?? p, br = o.br || 1;
      const sy = (p > 0 ? 1 - .15 * p : 1 - .26 * p) * br, sx = (p > 0 ? 1 + .12 * p : 1 + .1 * p) * br;
      const bw = w * sx, bh = H * sy, x = (w - bw) / 2, y = H - bh;
      const r = T.btnR.map(v => v * T.rs), seed = T.seed + 900 + (o.seed || 0);
      const ins = T.ins.slice(); ins[2] *= 1 + .3 * c01(p);
      const Ls = [], Xs = [], dis = o.disabled;
      if (o.focus) Ls.push(this.ring(-6, -6, w + 12, H + 12, r.map(v => v + 6), T, seed + 3));
      if (o.v !== 'text') {
        Ls.push(...this.body(T, x, y, bw, bh, r, { ins, seed, fill: dis ? T.disabledFill : o.fill || (o.v === 'filled' ? T.primary : T.paper),
          sh: dis ? 0 : (o.v === 'filled' ? T.sh : T.sh * .6) * (1 - c01(p)), inkOp: dis ? .35 : 1, rimColor: o.v === 'filled' ? T.btnHi : T.rim, noRim: dis || !!o.fill }));
      } else if (p > .02) Ls.push(this.L(this.wob(x + 8, y + 5, bw - 16, bh - 10, [14, 10, 16, 11], T.amp, seed), T.surface2, { op: c01(p * 1.5) }));
      const fs = 18, lc = dis ? T.muted : o.lc || (o.v === 'filled' ? T.onPrimary : T.text);
      const lh = H * (pl > 0 ? 1 - .15 * pl : 1 - .26 * pl), ly = H / 2 + fs * .35 + (H - lh) / 2 + 2 * c01(pl), lt = o.lt;
      let lOp = dis ? .6 : 1, lDy = 0;
      if (lt != null) { const u = c01(lt / 130); lDy = -26 * u * u; lOp *= 1 - u; }
      if (lOp > .01) Xs.push(this.X(o.label, w / 2, ly + lDy, { ff: T.body, fw: 700, fs, fill: lc, anchor: 'middle', op: lOp }));
      if (o.v === 'text') {
        const lw = this.tw(o.label, T.body, 700, fs), x0 = w / 2 - lw / 2, yy = ly + 7 + lDy, n = 8;
        let d = `M${f(x0)} ${f(yy)}`; for (let i = 1; i <= n; i++) { const xx = x0 + lw * i / n; d += ` Q${f(xx - lw / n / 2)} ${f(yy + (i % 2 ? 2.4 : -2.4))} ${f(xx)} ${f(yy)}`; }
        Ls.push(this.L(d, 'none', { stroke: dis ? T.muted : T.primary, sw: 3, op: (dis ? .5 : 1) * (lt != null ? lOp : 1) }));
      }
      if (lt != null && lt > 100) {
        const u = c01((lt - 100) / 100), cy0 = H / 2 - 34, cy1 = y + bh / 2, cy = cy0 + (cy1 - cy0) * u * u; let ssx = 1, ssy = 1;
        if (lt > 200) { const q = this.osc((lt - 200) / 1000, .35, 40); ssx = 1 + .22 * q; ssy = 1 - .2 * q; }
        Ls.push(...this.spinner(T, w / 2, cy, 10, (o.spin || 0) / 1000 * 2 * Math.PI, ssx, ssy, o.v === 'filled' ? T.paper : T.primary));
      }
      return { L: Ls, X: Xs };
    }
    check(T, o) {
      const s = 30, bx = 9, by = 9, sx = o.sx ?? 1, sy = o.sy ?? 1, dx = o.dx || 0, bw = s * sx, bh = s * sy, x = bx + (s - bw) / 2 + dx, y = by + (s - bh);
      const Ls = [], Xs = [], dis = o.disabled;
      if (o.focus) Ls.push(this.ring(bx - 6, by - 6, s + 12, s + 12, [12, 9, 13, 10], T, T.seed + 55));
      Ls.push(...this.body(T, x, y, bw, bh, [8, 5, 9, 6], { ins: T.ins.map(v => v * .85), seed: T.seed + 51, step: 3, fill: dis ? T.disabledFill : o.on ? T.primary : T.paper, inkColor: o.error ? T.danger : T.ink, inkOp: dis ? .35 : 1, sh: dis ? 0 : 2 }));
      const trim = o.trim ?? (o.on ? 1 : 0);
      if (trim > .005) { const len = 40.6, d = `M${15 + dx} 24 L${21.5 + dx} 31 L${41 + dx} 7`, doff = (len * (1 - trim)).toFixed(2), dash = `${len} 200`;
        Ls.push(this.L(d, 'none', { stroke: T.paper, sw: 8.5, dash, doff, op: dis ? .5 : 1 }), this.L(d, 'none', { stroke: T.ink, sw: 4.5, dash, doff, op: dis ? .45 : 1 }));
        const sp = o.sp ?? 0; if (sp > 0 && sp < 1) { const e2 = 1 - Math.pow(1 - sp, 2); [[1, -.55, 1.5], [.35, -1, 1.2], [1.1, .35, 1]].forEach(([vx, vy, r0], j) => { const px = 41 + dx + vx * (3 + 9 * e2), py = 7 + vy * (3 + 9 * e2), rr = r0 * (1 - sp * .6); Ls.push(this.L(this.wob(px - rr, py - rr, 2 * rr, 2 * rr, [rr, rr, rr, rr], .1, 60 + j, 1.2), T.fxLine || T.ink, { op: 1 - sp })); }); } }
      if (o.label) Xs.push(this.X(o.label, 58, 30, { ff: T.body, fw: 600, fs: 16, fill: o.error ? T.dangerText : T.text, op: dis ? .6 : 1 }));
      return { L: Ls, X: Xs };
    }
    sw(T, o) {
      const Ls = [], pos = o.pos ?? (o.on ? 1 : 0), sx = o.sx ?? 1, sy = o.sy ?? 1, org = o.origin || 'c', dis = o.disabled, f = this.f;
      if (o.focus) Ls.push(this.ring(-6, 1, 72, 46, [22, 20, 22, 19], T, T.seed + 205));
      const tOn = o.trackOn ?? o.on, wv = o.wave, waving = !dis && wv != null && wv < 1, tIns = T.ins.map(v => v * .75);
      Ls.push(...this.body(T, 0, 7, 60, 34, [17, 15, 17, 14], { ins: tIns, amp: T.amp * .7, seed: T.seed + 201, step: 4, fill: dis ? T.disabledFill : (waving ? !tOn : tOn) ? T.on : T.off, sh: dis ? 0 : T.sh - 1, inkOp: dis ? .35 : 1 }));
      if (waving) { const fx = tIns[3] + .5, fy = 7 + tIns[0] + .5, fw = 60 - tIns[1] - tIns[3] - 1, fh = 34 - tIns[0] - tIns[2] - 1, ww = Math.max(3, fw * (1 - Math.pow(1 - wv, 2))), x0 = org === 'r' ? fx + fw - ww : fx; Ls.push(this.L(this.wob(x0, fy, ww, fh, [fh / 2, fh / 2, fh / 2, fh / 2], .7, T.seed + 207 + Math.floor(wv * 4), 3), tOn ? T.on : T.off)); }
      const tx = 4 + pos * 26, ty = 11, ox = org === 'r' ? 26 : org === 'l' ? 0 : 13, oy = o.pressed ? 26 : 13;
      const tf = `translate(${f(tx)} ${ty}) translate(${ox} ${oy}) scale(${sx.toFixed(3)} ${sy.toFixed(3)}) translate(${-ox} ${-oy})`;
      const g = this.shape(0, 0, 26, 26, [13, 12, 13, 11], [1.8, 2.2, 3, 1.8], T.amp * .5, T.seed + 301, 3);
      if (o.echo && !dis) [[2, .14], [1, .3]].forEach(([k, op]) => { const gx = 4 + (pos - o.echo * .16 * k) * 26; Ls.push(this.L(g.fill, T.thumb, { tf: `translate(${f(gx)} ${ty}) translate(13 13) scale(${(sx * .96).toFixed(3)} ${(sy * .96).toFixed(3)}) translate(-13 -13)`, op: op * Math.abs(o.echo) })); });
      Ls.push(this.L(g.ink, T.ink, { tf, op: dis ? .35 : 1 }), this.L(g.fill, dis ? T.disabledFill : T.thumb, { tf }));
      return { L: Ls, X: [] };
    }
    card(T, o) {
      const w = o.w || 342, H = 84, i = o.idx || 0, press = o.press || 0;
      const cardR = [[16, 10, 18, 12], [11, 17, 13, 19], [18, 12, 15, 10]][i % 3].map(v => v * T.rs);
      if (o.ghost) return { L: [this.L(this.wob(0, 0, w, H, cardR, T.amp, T.seed + i * 13), T.ink, { op: .92 })], X: [] };
      const sx = (o.sx ?? 1) * (1 - .02 * press), sy = (o.sy ?? 1) * (1 - .02 * press), bw = w * sx, bh = H * sy, x = (w - bw) / 2, y = H - bh, dy = (H - bh) / 2, dis = o.dis;
      const Ls = this.body(T, x, y, bw, bh, cardR, { insMul: o.sel ? 1.6 : 1, seed: T.seed + i * 13, fill: T.cardColors[i % 3], sh: dis ? 0 : T.sh * (1 - .7 * press), inkOp: dis ? .4 : 1 });
      const stw = 64, sth = 40, stx = w - stw - 18, sty = (H - sth) / 2 - 2 + dy;
      Ls.push(...this.body(T, stx, sty, stw, sth, [14, 9, 15, 10], { ins: T.ins.map(v => v * .8), amp: T.amp * .8, seed: T.seed + i * 17 + 5, step: 4, fill: T.sColors[i % 3], noRim: true, inkOp: dis ? .4 : 1 }));
      const op = dis ? .5 : 1, bo = o.textOp ?? 1;
      return { L: Ls, X: [this.X(o.title, 22, 38 + dy, { ff: T.body, fw: 700, fs: 18, fill: T.text, op: op * bo }), this.X(o.meta, 22, 60 + dy, { ff: T.body, fw: 500, fs: 14, fill: T.muted, op: op * bo }), this.X(o.time, stx + stw / 2, sty + 25, { ff: T.body, fw: 700, fs: 15, fill: T.ink, anchor: 'middle', op: op * bo })] };
    }
    dust(T, u, cx, cy, s = 40, o = {}) {
      if (u <= 0 || u >= 1) return { L: [], X: [] };
      const e = 1 - Math.pow(1 - u, 2.2), fr = Math.min(3, Math.floor(u * 4)), Ls = [];
      (o.dirs || [[-1, -.25, 1], [-.55, -.85, .8], [.05, -1, .6], [.55, -.8, .85], [1, -.3, 1]]).forEach(([dx, dy, k], j) => {
        const R = (3 + 9 * Math.sqrt(u)) * k * (s / 40 * .5 + .5) * (o.k || 1), op = 1 - Math.pow(u, 1.6), gx = Math.sign(dx) * (o.gap || 0), px = cx + gx + dx * s * e, py = cy + dy * s * .55 * e;
        const g = this.shape(px - R, py - R, 2 * R, 2 * R, [R, R * .9, R, R * .85], [1.2, 1.4, 1.9, 1.2], .7, 700 + j * 7 + fr * 13, 3);
        Ls.push(this.L(g.ink, T.ink, { op }), this.L(g.fill, T.dust, { op }));
      });
      return { L: Ls, X: [] };
    }
    stars(T, u, cx, cy, s = 34) {
      if (u <= 0 || u >= 1) return { L: [], X: [] };
      const Ls = [], fr = Math.min(3, Math.floor(u * 4));
      [-150, -90, -30].forEach((a, j) => {
        const ar = a * Math.PI / 180, e = 1 - Math.pow(1 - u, 3), rr = s * (.35 + .65 * e), px = cx + Math.cos(ar) * rr * 1.3, py = cy + Math.sin(ar) * rr;
        const sc = u < .25 ? (u / .25) * 1.15 : 1.15 - .5 * (u - .25) / .75, op = u < .6 ? 1 : 1 - (u - .6) / .4;
        const tf = `translate(${this.f(px)} ${this.f(py)}) rotate(${(200 * u * (j % 2 ? -1 : 1)).toFixed(1)})`;
        Ls.push(this.L(this.star(9 * sc + 2.8, 4.2 * sc + 2.4, 40 + j + fr * 5), T.ink, { tf, op }), this.L(this.star(9 * sc, 4.2 * sc, 40 + j + fr * 5), T.warn, { tf, op }));
      });
      return { L: Ls, X: [] };
    }


    // ---- FX library: every effect keeps one point grid across its frames, so frames can morph ----
    shake(t) { return t < 400 ? 6 * Math.sin(2 * Math.PI * t / 120) * Math.exp(-t / 180) : 0; }
    lerpK(K, u) { let i = 0; while (i < K.length - 2 && u > K[i + 1][0]) i++; const a = K[i], b = K[i + 1], v = this.c01((u - a[0]) / (b[0] - a[0] || 1)); return a.map((x, j) => x + (b[j] - x) * v); }
    speed(T, u, x, y, h) {
      if (u < 0 || u >= 1) return { L: [], X: [] };
      const Ls = [], f = this.f;
      [.15, .32, .5, .68, .85].forEach((k, j) => { const len = (50 + 40 * ((j * 7) % 3)) * (1 - u), yy = y + h * k, x0 = x - 10; Ls.push(this.L(`M${f(x0)} ${f(yy - 1.8)} L${f(x0 - len)} ${f(yy)} L${f(x0)} ${f(yy + 1.8)}Z`, T.fxLine || T.ink, { op: 1 - u })); });
      return { L: Ls, X: [] };
    }
    flash(T, u, cx, cy, s = 44) {
      if (u <= 0 || u >= 1) return { L: [], X: [] };
      const N = 12, rnd = this.rng(77), jit = Array.from({ length: 2 * N }, () => .82 + .36 * rnd());
      const [, sc, kr] = this.lerpK([[0, .25, .55], [.18, 1.15, .42], [.4, 1, .6], [.7, .72, .78], [1, .2, .92]], u);
      const op = u > .7 ? 1 - (u - .7) / .3 : 1, rot = u * .35;
      const pts = (add, mul) => { const P = []; for (let i = 0; i < 2 * N; i++) { const a = rot - Math.PI / 2 + i * Math.PI / N, r = ((i % 2 ? kr : 1) * s * sc * jit[i]) * mul + add; P.push(`${this.f(cx + Math.cos(a) * r)} ${this.f(cy + Math.sin(a) * r)}`); } return 'M' + P.join(' L') + 'Z'; };
      const Ls = [this.L(pts(3.5, 1), T.ink, { op }), this.L(pts(0, 1), T.warn, { op })];
      if (u < .5) Ls.push(this.L(pts(0, .5), T.paper, { op: op * (1 - u / .5) }));
      return { L: Ls, X: [] };
    }
    vibe(T, u, x, y, w, h) {
      if (u <= 0 || u >= 1) return { dx: 0, L: [], X: [] };
      const ph = Math.floor(u * 8) % 2, a = 1 - u, dx = (ph ? 2.2 : -2.2) * a, op = 1 - u * u, Ls = [], f = this.f, cy = y + h / 2;
      [[0, 1], [1, .6]].forEach(([k, sz]) => {
        const off = 8 + k * 7 + (ph ? 2 : 0), hh = h * .34 * sz;
        Ls.push(this.L(`M${f(x - off)} ${f(cy - hh)} Q${f(x - off - 4)} ${f(cy)} ${f(x - off)} ${f(cy + hh)}`, 'none', { stroke: T.fxLine || T.ink, sw: 2.6, op }));
        Ls.push(this.L(`M${f(x + w + off)} ${f(cy - hh)} Q${f(x + w + off + 4)} ${f(cy)} ${f(x + w + off)} ${f(cy + hh)}`, 'none', { stroke: T.fxLine || T.ink, sw: 2.6, op }));
      });
      return { dx, L: Ls, X: [] };
    }
    irisAt(t) {
      // 600 ms: close with ease-in (snap), hold 80 on black, open with ease-out + overshoot
      if (t < 220) { const u = t / 220; return { k: 1 - u * u * u, scene: 0 }; }
      if (t < 300) return { k: 0, scene: 1 };
      const u = this.c01((t - 300) / 300); return { k: Math.min(1.04, 1 - this.spr(u * .3, .55, 22)), scene: 1 };
    }
    irisScene(T, a, o = {}) {
      const W = o.W || 180, H = o.H || 320, cx = o.cx ?? W / 2, cy = o.cy ?? H - 46, Rmax = Math.hypot(Math.max(cx, W - cx), Math.max(cy, H - cy)) + 8, R = Math.max(0, Rmax * a.k);
      const f = this.f, sc = a.scene ^ (o.from || 0), Ls = [];
      const bgc = sc ? T.surface2 : T.bg, blk = sc ? T.primary : T.paper;
      Ls.push(this.L(`M0 0 H${W} V${H} H0 Z`, bgc));
      const B = sc ? [[14, 24, W - 28, 70], [14, 108, (W - 38) / 2, 90], [24 + (W - 38) / 2, 108, (W - 38) / 2, 90]] : [[14, 24, W - 28, 46], [14, 82, W - 28, 46], [14, 140, W - 28, 46]];
      B.forEach(([x, y, w, h], i) => Ls.push(...this.body(T, x, y, w, h, [10, 7, 11, 8], { seed: T.seed + 980 + i + sc * 10, fill: blk, sh: 3, step: 5 })));
      Ls.push(...this.body(T, 14, H - 70, W - 28, 48, [14, 10, 16, 11], { seed: T.seed + 990, fill: sc ? T.paper : T.primary, sh: 3, rimColor: sc ? T.rim : T.btnHi }));
      if (R < Rmax) {
        Ls.push(this.L(`M0 0 H${W} V${H} H0 Z M${f(cx + R)} ${f(cy)} A${f(R)} ${f(R)} 0 1 0 ${f(cx - R)} ${f(cy)} A${f(R)} ${f(R)} 0 1 0 ${f(cx + R)} ${f(cy)} Z`, T.ink, { fr: 'evenodd' }));
        [6, 14].forEach((d, j) => { const rr = R + d; Ls.push(this.L(`M${f(cx + rr)} ${f(cy)} A${f(rr)} ${f(rr)} 0 1 0 ${f(cx - rr)} ${f(cy)} A${f(rr)} ${f(rr)} 0 1 0 ${f(cx + rr)} ${f(cy)}`, 'none', { stroke: T.rim || T.paper, sw: j ? .8 : 1.4, op: (j ? .35 : .6) * this.c01(R / 20) })); });
      }
      return { L: Ls, X: [] };
    }
    hole(T, u, x, y, w, h, r, seed = 5) {
      if (u < 0 || u >= 1) return { L: [], X: [] };
      const rnd = this.rng(seed), pts = this.rr(x, y, w, h, r, 4), N = pts.length, cx = x + w / 2, cy = y + h / 2;
      const sc = u < .15 ? 1 : 1 - .55 * Math.pow((u - .15) / .85, 2), op = u < .6 ? 1 : 1 - (u - .6) / .4;
      const P = pts.map((p, i) => { let dx = p[0] - cx, dy = p[1] - cy; const l = Math.hypot(dx, dy) || 1; const spike = i % 7 === 3 ? 4 + 5 * rnd() : (i % 7 === 2 || i % 7 === 4) ? 1.2 : -.6 * rnd(); return [cx + (dx + dx / l * spike) * sc, cy + (dy + dy / l * spike) * sc]; });
      const f = this.f, d = 'M' + P.map(p => f(p[0]) + ' ' + f(p[1])).join(' L') + 'Z', P2 = P.map(p => { const dx = p[0] - cx, dy = p[1] - cy, l = Math.hypot(dx, dy) || 1; return [p[0] + dx / l * 2.6, p[1] + dy / l * 2.6]; });
      const Ls = [this.L('M' + P2.map(p => f(p[0]) + ' ' + f(p[1])).join(' L') + 'Z', T.holeEdge || T.paper, { op }), this.L(d, T.ink, { op })];
      const e = 1 - Math.pow(1 - this.c01(u * 1.4), 2);
      [[-.9, -.6], [.8, -.8], [1, .3], [-1, .4]].forEach(([dx, dy], j) => {
        const px = cx + dx * (w / 2 + 6 + 34 * e), py = cy + dy * (h / 2 + 4 + 26 * e) + 30 * e * e, rot = (j % 2 ? 1 : -1) * 260 * u, s2 = 4 + j % 2 * 1.5;
        const tf = `translate(${f(px)} ${f(py)}) rotate(${rot.toFixed(1)})`, chip = `M${-s2} ${-s2 * .6} L${s2} ${-s2 * .9} L${s2 * .5} ${s2} Z`;
        Ls.push(this.L(chip, T.ink, { tf: tf + ' scale(1.5)', op: op * (1 - u) }), this.L(chip, T.holeEdge || T.paper, { tf, op: op * (1 - u) }));
      });
      return { L: Ls, X: [] };
    }
    sweat(T, u, x, y, w) {
      if (u <= 0 || u >= 1) return { L: [], X: [] };
      const drop = 'M0 -7 C2 -3 5 0 5 3 C5 6 2.6 8 0 8 C-2.6 8 -5 6 -5 3 C-5 0 -2 -3 0 -7 Z', Ls = [], f = this.f;
      [[x + w * .12, -1, 0, 1], [x + w * .88, 1, 0, .9], [x + w * .98, 1, .18, .75]].forEach(([x0, dir, dl, k]) => {
        const v = this.c01((u - dl) / (1 - dl)); if (v <= 0 || v >= 1) return;
        const sz = (v < .2 ? v / .2 : 1) * k, px = x0 + dir * 20 * v, py = y - 6 - 16 * Math.sin(Math.PI * Math.min(1, v * 1.1)) + 30 * Math.pow(Math.max(0, v - .45), 2) * 4;
        const rot = dir * (35 - 70 * v), st = 1 + .45 * Math.max(0, v - .5), op = v > .8 ? 1 - (v - .8) / .2 : 1;
        const tf = `translate(${f(px)} ${f(py)}) rotate(${rot.toFixed(1)}) scale(${(sz / st ** .5).toFixed(3)} ${(sz * st).toFixed(3)})`;
        Ls.push(this.L(drop, T.ink, { tf: tf + ' scale(1.32)', op }), this.L(drop, T.sweat, { tf, op }), this.L('M-1.8 1.2 A1.2 1.8 -20 1 1 -1.79 1.2 Z', T.paper, { tf, op: op * .9 }));
      });
      return { L: Ls, X: [] };
    }

    field(T, o) {
    const w = o.w, H = 56, thick = o.thick ?? (o.focus ? 1 : 0), bounce = o.bounce || 1;
    const bw = w * bounce, bh = H * (1 + (bounce - 1) * .6), x = (w - bw) / 2, y = (H - bh) / 2;
    const r = [14, 10, 16, 11].map(v => v * T.rs), red = o.error, dis = o.disabled;
    const ins = T.ins.map(v => v * (1 + .5 * (red ? 1 : thick)));
    const Ls = [], Xs = [];
    if (o.focusRing) Ls.push(this.ring(-6, -6, w + 12, H + 12, r.map(v => v + 6), T, T.seed + 99));
    Ls.push(...this.body(T, x, y, bw, bh, r, { ins, seed: T.seed + 101, fill: dis ? T.disabledFill : T.field, inkColor: red ? T.danger : T.ink, inkOp: dis ? .35 : 1, rimColor: T.fieldRim, sh: 0 }));
    const lift = o.lift ?? ((o.focus || o.value) ? 1 : 0);
    const lx = 20 + (4 - 20) * lift, ly = 34 + (-10 - 34) * lift, lfs = 16 - 3 * lift;
    Xs.push(this.X(o.label, lx, ly, { ff: T.body, fw: lift > .5 ? 700 : 500, fs: lfs, fill: lift > .5 ? (red ? T.dangerText : T.text) : T.muted, op: dis ? .6 : 1 }));
    if (o.value) Xs.push(this.X(o.value, 20, 34, { ff: T.body, fw: 500, fs: 16, fill: T.text }));
    if (o.caret) { const cx = 20 + (o.value || '').length * 8.6 + 2; Ls.push(this.L(`M${this.f(cx)} 18 h3.2 v22 h-3.2 Z`, T.primary)); }
    if (o.helper) Xs.push(this.X(o.helper, 4, 80, { ff: T.body, fw: 600, fs: 13, fill: red ? T.dangerText : T.muted }));
    return { L: Ls, X: Xs };
  }
    snack(T, o) {
    const w = o.w || 342, H = 56, sy = o.sy ?? 1, bh = H * sy;
    const pal = { info: [T.warn, T.ink], success: [T.successLight, T.ink], error: [T.danger, T.onDanger] }[o.type || 'info'], rope = T.fxLine || T.ink, hx = w / 2, hy = -26;
    const Ls = [];
    if (o.rope !== false) { Ls.push(this.L(`M14 12 Q${hx * .5} ${hy * .2} ${hx} ${hy}`, 'none', { stroke: rope, sw: 1.8 }), this.L(`M${w - 14} 12 Q${w - hx * .5} ${hy * .2} ${hx} ${hy}`, 'none', { stroke: rope, sw: 1.8 })); const g = this.shape(hx - 4, hy - 4, 8, 8, [4, 4, 4, 4], [1.3, 1.3, 1.8, 1.3], .3, T.seed + 640, 2); Ls.push(this.L(g.ink, T.ink), this.L(g.fill, T.nail)); }
    const su = o.speedU ?? -1; if (su >= 0 && su < 1) [.22, .4, .6, .78].forEach((k, j) => { const x = w * k, len = (26 + 14 * (j % 2)) * (1 - su); Ls.push(this.L(`M${this.f(x - 1.7)} -6 L${this.f(x)} ${this.f(-6 - len)} L${this.f(x + 1.7)} -6 Z`, rope, { op: 1 - su })); });
    Ls.push(...this.body(T, 0, 0, w, bh, [10, 14, 9, 13].map(v => v * T.rs), { seed: T.seed + 601, fill: pal[0], sh: T.sh, noRim: true }));
    [[14, 12], [w - 14, 12]].forEach(([cx, cy], j) => Ls.push(this.L(this.wob(cx - 3, cy - 3, 6, 6, [3, 3, 3, 3], .3, T.seed + 650 + j, 2), T.ink)));
    const Xs = [this.X(o.text, 22, bh / 2 + 6, { ff: T.body, fw: 700, fs: 16, fill: pal[1] })];
    if (o.action) {
      Xs.push(this.X(o.action, w - 22, bh / 2 + 6, { ff: T.body, fw: 800, fs: 16, fill: pal[1], anchor: 'end' }));
      const lw = o.action.length * 9, x1 = w - 22, yy = bh / 2 + 12;
      Ls.push(this.L(`M${x1 - lw} ${yy} Q${x1 - lw * .75} ${yy + 2.4} ${x1 - lw / 2} ${yy} Q${x1 - lw * .25} ${yy - 2.4} ${x1} ${yy}`, 'none', { stroke: pal[1], sw: 2.6 }));
    }
    return { L: Ls, X: Xs };
  }
    fldFocus(t, on) {
    if (on) return { lift: 1 - this.spr(t / 1000, .5, 32), thick: Math.min(1, t / 80), bounce: 1 + .035 * this.osc(t / 1000, .35, 45) * Math.min(1, t / 30) };
    return { lift: this.spr(t / 1000, .5, 32), thick: Math.max(0, 1 - t / 80), bounce: 1 };
  }
    cardAppear(t, rot) {
    if (t < 0) return { op: 0, dy: -24, rot: rot + 2, sx: 1, sy: 1, du: -1 };
    if (t < 120) { const u = t / 120; return { op: Math.min(1, u * 3), dy: -24 * (1 - u * u), rot: rot + 2 * (1 - u), sx: 1, sy: 1, du: -1 }; }
    const a = this.osc((t - 120) / 1000, .35, 38);
    return { op: 1, dy: 0, rot, sx: 1 + .06 * a, sy: 1 - .1 * a, du: (t - 120) / 300 };
  }
    cardDismiss(t) {
    let dx = 0, sx = 1, sy = 1, op = 1, lu = -1, n = 0;
    if (t < 70) { const u = t / 70; dx = -10 * (1 - Math.pow(1 - u, 2)); sx = 1 - .04 * u; }
    else if (t < 170) { const u = (t - 70) / 100; dx = -10 + 440 * u * u; sx = 1 + .5 * Math.sin(Math.PI * Math.min(1, u * 1.2)); sy = 1 - .15 * Math.sin(Math.PI * u); lu = u * .5; }
    else { dx = 440; op = 0; lu = .5 + (t - 170) / 200; }
    const ghost = t >= 170 && t < 212;
    if (t > 200) n = -(1 - this.spr((t - 200) / 1000, .55, 40));
    return { dx, sx, sy, op, lu, ghost, n };
  }
    snkShow(t) { return { dy: 90 * this.spr(t / 1000, .48, 24), rot: t > 160 ? 3 * Math.sin(2 * Math.PI * (t - 160) / 150) * Math.exp(-(t - 160) / 200) : 0, sy: 1 }; }
    snkHide(t) { if (t < 70) { const u = t / 70; return { dy: -8 * (1 - Math.pow(1 - u, 2)), rot: 0, sy: 1 }; } const u = Math.min(1, (t - 70) / 170); return { dy: -8 + 130 * u * u, rot: 0, sy: 1 + .3 * Math.sin(Math.PI * u) }; }

    // ---- MVP components ----
    iconBtn(T, o) {
      const p = o.p || 0, sy = p > 0 ? 1 - .15 * p : 1 - .26 * p, sx = p > 0 ? 1 + .12 * p : 1 + .1 * p, d = 44, bw = d * sx, bh = d * sy, x = 2 + (d - bw) / 2, y = 2 + d - bh, dis = o.disabled, filled = o.variant === 'filled';
      const Ls = [];
      if (o.focus) Ls.push(this.ring(-4, -4, 56, 56, [28, 26, 28, 25], T, T.seed + 401));
      Ls.push(...this.body(T, x, y, bw, bh, [22, 20, 23, 19], { ins: T.ins.map(v => v * .85), seed: T.seed + 400 + (o.seed || 0), step: 4, fill: dis ? T.disabledFill : filled ? T.primary : T.paper, sh: dis ? 0 : 3 * (1 - this.c01(p)), inkOp: dis ? .35 : 1, rimColor: filled ? T.btnHi : T.rim, noRim: dis }));
      const iy = 12 + (o.iy || 0) + (d - bh) / 2 + 1.5 * this.c01(p);
      Ls.push(...this.icon(T, o.icon, 12, iy, { fill: o.sel ? (filled ? T.paper : T.primary) : null, op: dis ? .4 : 1, color: filled ? T.onPrimary : T.text }));
      return { L: Ls, X: [] };
    }
    fab(T, o) {
      const br = o.br || 1, label = o.label, H = 64, fs = 17, tw = label ? this.tw(label, T.body, 700, fs) : 0, w = label ? 20 + 24 + 10 + tw + 24 : 64;
      const p = o.p || 0, sy = (p > 0 ? 1 - .15 * p : 1 - .26 * p) * (o.sy || 1) * br, sx = (p > 0 ? 1 + .12 * p : 1 + .1 * p) * (o.sx || 1) * br;
      const bw = w * sx, bh = H * sy, x = (w - bw) / 2, y = H - bh, r = [22, 16, 24, 18].map(v => v * T.rs), dis = o.disabled, Ls = [];
      if (o.focus) Ls.push(this.ring(-6, -6, w + 12, H + 12, r.map(v => v + 6), T, T.seed + 451));
      Ls.push(...this.body(T, x, y, bw, bh, r, { seed: T.seed + 450 + (o.seed || 0), fill: dis ? T.disabledFill : T.primary, sh: dis ? 0 : (T.sh + 2) * (1 - this.c01(p)), inkOp: dis ? .35 : 1, rimColor: T.btnHi, noRim: dis }));
      if (!dis) Ls.push(this.L(this.wob(x + 7, y + 7 * sy, bw - 14, bh - 14 * sy, r.map(v => Math.max(4, v - 7)), T.amp * .8, T.seed + 452), 'none', { stroke: T.ink, sw: 1.6, op: .5, dash: '3 4' }));
      const cy = y + bh / 2;
      Ls.push(...this.icon(T, o.icon || 'plus', 20, cy - 12, { bold: true, op: dis ? .4 : 1, color: T.onPrimary }));
      const Xs = label ? [this.X(label, 54, cy + fs * .35, { ff: T.body, fw: 700, fs, fill: dis ? T.muted : T.onPrimary })] : [];
      return { L: Ls, X: Xs, w };
    }
    radio(T, o) {
      const Ls = [], Xs = [], dis = o.disabled, cx = 24, cy = 24, sq = o.sq ?? 1, cw = 28 * (2 - sq), ch = 28 * sq;
      if (o.focus) Ls.push(this.ring(4, 4, 40, 40, [20, 19, 20, 18], T, T.seed + 501));
      Ls.push(...this.body(T, 10 + (28 - cw) / 2, 10 + (28 - ch), cw, ch, [14, 13, 14, 12.5], { ins: T.ins.map(v => v * .85), seed: T.seed + 500, step: 3, fill: dis ? T.disabledFill : T.paper, inkOp: dis ? .35 : 1, sh: dis ? 0 : 2, noRim: true }));
      const ds = o.ds ?? (o.sel ? 1 : 0);
      if (ds > .03) {
        const r = 6.5 * ds, rx = r * (o.dsx || 1), ry = r * (o.dsy || 1), by = cy + 6.5 + (o.dotY || 0) + (28 - ch) / 2;
        const g = this.shape(cx - rx, by - 2 * ry, 2 * rx, 2 * ry, [r, r * .95, r, r * .9], [1.4, 1.6, 2.2, 1.4], .4, T.seed + 502, 2.5);
        Ls.push(this.L(g.ink, T.ink, { op: dis ? .4 : 1 }), this.L(g.fill, dis ? T.disabledFill : T.primary));
        const sp = o.sp ?? -1;
        if (sp > 0 && sp < 1) [-1, 1].forEach((dir, j) => { const px = cx + dir * (7 + 8 * sp), py = by - 2 - 9 * Math.sin(Math.PI * Math.min(1, sp * 1.15)) + 3 * sp, rr = 1.7 * (1 - sp * .5), op = 1 - sp * sp, d = this.wob(px - rr, py - rr, 2 * rr, 2 * rr, [rr, rr, rr, rr], .1, 520 + j, 1.5), d2 = this.wob(px - rr - 1, py - rr - 1, 2 * rr + 2, 2 * rr + 2, [rr + 1, rr + 1, rr + 1, rr + 1], .1, 522 + j, 1.5); Ls.push(this.L(d2, T.ink, { op }), this.L(d, T.primary, { op })); });
      }
      if (o.label) Xs.push(this.X(o.label, 54, 30, { ff: T.body, fw: 600, fs: 16, fill: T.text, op: dis ? .6 : 1 }));
      return { L: Ls, X: Xs };
    }
    chip(T, o) {
      const fs = 15, H = 36, label = o.label, tw = this.tw(label, T.body, 600, fs);
      const lead = o.variant === 'assist' || (o.variant === 'filter' && o.sel) ? 24 : 0, trail = o.variant === 'input' ? 22 : 0, w = 16 + lead + tw + trail + 16;
      const sc = o.scale || 1, p = o.p || 0, bw = w * sc * (1 + .06 * p), bh = H * sc * (1 - .12 * p), x = (w - bw) / 2, y = (H - bh) / 2 + H * .06 * Math.max(0, p);
      const r = [9, 15, 8, 14].map(v => v * T.rs), dis = o.disabled, Ls = [], Xs = [];
      if (o.focus) Ls.push(this.ring(-5, -5, w + 10, H + 10, r.map(v => v + 5), T, T.seed + 551));
      const ins = T.ins.map(v => v * .8), g = this.shape(x, y, bw, bh, r, ins, T.amp * .8, T.seed + 550 + (o.seed || 0), 4);
      if (!dis) Ls.push(this.L(g.ink, T.ink, { tf: `translate(${3 * (1 - this.c01(p))} ${3 * (1 - this.c01(p))})` }));
      Ls.push(this.L(g.ink, T.ink, { op: dis ? .35 : 1 }), this.L(g.fill, dis ? T.disabledFill : T.paper));
      const swp = o.swipe ?? (o.sel ? 1 : 0);
      if (swp > .01 && !dis) { const fx = x + ins[3] + 1, fy = y + ins[0] + 1, fw = bw - ins[1] - ins[3] - 2, fh = bh - ins[0] - ins[2] - 2; Ls.push(this.L(this.wob(fx, fy, Math.max(3, fw * swp), fh, [5, Math.min(fh / 2, 13), Math.min(fh / 2, 11), 5], .7, T.seed + 553, 3), T.chipSel)); }
      const cy = y + bh / 2, op = dis ? .5 : 1, tc = swp > .5 ? T.ink : T.text; let tx = 16;
      if (lead) { if (o.variant === 'filter') Ls.push(this.L(`M${tx + 1} ${this.f(cy)} L${tx + 6} ${this.f(cy + 5)} L${tx + 16} ${this.f(cy - 6)}`, 'none', { stroke: tc, sw: 3, op })); else Ls.push(...this.icon(T, o.icon || 'calendar', tx - 2, cy - 10, { size: 20, op, color: tc })); tx += lead; }
      Xs.push(this.X(label, tx, cy + fs * .35, { ff: T.body, fw: 600, fs, fill: dis ? T.muted : tc, op }));
      if (trail) Ls.push(...this.icon(T, 'close', tx + tw + 6, cy - 8, { size: 16, op, color: tc }));
      return { L: Ls, X: Xs, w };
    }
    iris(T, R, cx, cy, W, H, op = .62) { const f = this.f; return this.L(`M0 0 H${W} V${H} H0 Z M${f(cx + R)} ${f(cy)} A${f(R)} ${f(R)} 0 1 0 ${f(cx - R)} ${f(cy)} A${f(R)} ${f(R)} 0 1 0 ${f(cx + R)} ${f(cy)} Z`, T.ink, { op, fr: 'evenodd' }); }
    dialog(T, o) {
      const w = o.w || 300, lines = o.body || [], H = 24 + 30 + 10 + lines.length * 22 + 22 + 52 + 20, r = [20, 14, 22, 16].map(v => v * T.rs);
      const Ls = this.body(T, 0, 0, w, H, r, { seed: T.seed + 650, fill: T.paper, sh: T.sh + 2 }), Xs = [this.X(o.title, 24, 48, { ff: T.display, fw: 400, fs: 24, fill: T.text })];
      lines.forEach((ln, i) => Xs.push(this.X(ln, 24, 80 + i * 22, { ff: T.body, fw: 400, fs: 15, fill: T.muted })));
      const by = H - 72, okW = 128, cW = 104;
      const sc = this.cat(this.place(this.btn(T, { v: 'text', w: cW, label: o.cancel || 'Cancel', focus: o.focus === 'cancel' }), w - 24 - okW - 6 - cW, by), this.place(this.btn(T, { v: 'filled', w: okW, focus: o.focus === 'confirm', label: o.confirm || 'OK', fill: o.destructive ? T.danger : null, lc: o.destructive ? T.onDanger : null }), w - 24 - okW, by));
      return { L: Ls.concat(sc.L), X: Xs.concat(sc.X), w, h: H };
    }
    dialogScene(T, a, o = {}) {
      const W = 360, H = 460, cx = W / 2, cy = 236, k = a.R < 300 ? .35 : 1;
      const it = [['Pancake breakfast', 'Kitchen, bring syrup', '9:00'], ['Bike to the lake', 'Check the tyre pump', '11:30']];
      const bg = this.cat(...it.map((v, i) => this.place(this.card(T, { idx: i, title: v[0], meta: v[1], time: v[2], textOp: k }), 9, 24 + i * 102, T.rots[i], 171, 42)), this.place(this.btn(T, { v: 'filled', w: 342, label: 'Add plan' }), 9, 384));
      const dl = this.dialog(T, { w: 300, focus: o.focus, title: o.title || 'Delete plan?', body: o.body || ['“Paint the fence” will be gone', 'for good.'], confirm: o.confirm || 'Delete', destructive: o.destructive ?? true });
      const up = a.op > .01 && a.s > .05, hh = dl.h / 2 * Math.max(1, a.s) + 8;
      bg.X = bg.X.filter(x => !(up && x.y > cy - hh && x.y < cy + hh + 16)).map(x => ({ ...x, op: x.op * k }));
      const placed = this.place(dl, (W - 300) / 2, cy - dl.h / 2), s = a.s, tf = `translate(${cx} ${cy}) scale(${Math.max(.001, s).toFixed(3)}) translate(${-cx} ${-cy})`;
      const parts = [bg, { L: [this.iris(T, a.R, cx, cy, W, H)], X: [] }];
      if (a.op > .01 && s > .01) parts.push({ L: placed.L.map(l => ({ ...l, tf: tf + ' ' + l.tf, op: l.op * a.op })), X: placed.X.map(x => ({ ...x, op: x.op * a.textOp })) });
      if (a.stars > 0) parts.push(this.stars(T, a.stars, cx, cy, 70));
      return this.cat(...parts);
    }
    linear(T, o) {
      const w = o.w || 300, h = 14, Ls = this.body(T, 0, 0, w, h, [7, 6, 7, 6], { ins: T.ins.map(v => v * .6), amp: T.amp * .6, seed: T.seed + 700, fill: T.off, sh: 2, noRim: true });
      const a = this.c01(o.a), b = this.c01(o.b);
      if (b - a > .01 && o.worm) {
        // inchworm: the shorter the body gets, the higher it arches out of the tube
        const f = this.f, x0 = 3 + (w - 6) * a + 4, x1 = Math.max(x0 + .5, 3 + (w - 6) * b - 4), len = x1 - x0, nat = (w - 6) * .34;
        const comp = this.c01(1 - len / nat), A = 11 * Math.pow(comp, .8), N = 24, P = [];
        for (let i = 0; i <= N; i++) { const u = i / N, k = Math.pow(Math.sin(Math.PI * u), 1.3); P.push([x0 + len * u, 7 - A * k + .6 * Math.sin(u * 9 + x0 * .05)]); }
        const d = 'M' + P.map(p => f(p[0]) + ' ' + f(p[1])).join(' L');
        Ls.push(this.L(d, 'none', { stroke: T.ink, sw: 11, tf: 'translate(0 .9)' }), this.L(d, 'none', { stroke: T.ink, sw: 10.4 }), this.L(d, 'none', { stroke: T.primary, sw: 7.4 }));
        const hp = P[N], hq = P[N - 2], ang = Math.atan2(hp[1] - hq[1], hp[0] - hq[0]);
        Ls.push(this.L(`M${f(hp[0] - Math.cos(ang) * .5 - Math.sin(ang) * 1.2)} ${f(hp[1] - Math.sin(ang) * .5 + Math.cos(ang) * -1.2)} h.01`, 'none', { stroke: T.ink, sw: 2.6 }));
      } else if (b - a > .01) Ls.push(...this.body(T, 3 + (w - 6) * a, 3, (w - 6) * (b - a), h - 6, [4, 4, 4, 4], { ins: [1, 1.2, 1.6, 1], amp: .3, seed: T.seed + 701, fill: T.primary, noRim: true, step: 3 }));
      return { L: Ls, X: [] };
    }
    circular(T, o) {
      if (o.value != null) return { L: [...this.body(T, 5, 5, 38, 38, [19, 18, 19, 17.5], { ins: T.ins.map(v => v * .7), seed: T.seed + 720, step: 3, fill: T.off, noRim: true }), this.L('M24 11 A13 13 0 1 1 23.99 11', 'none', { stroke: T.primary, sw: 7, dash: `${(2 * Math.PI * 13 * o.value).toFixed(1)} 200` })], X: [] };
      return { L: this.spinner(T, 24, 24, 13, (o.ph || 0) / 1000 * 2 * Math.PI, 1, 1, T.primary), X: [] };
    }
    topBar(T, o) {
      const w = o.w || 390, sx = 12, sy = 10, sw = w - 24, sh = 56, cy = sy + sh / 2, fs = 22, title = o.title || 'Saturday';
      const Ls = this.body(T, sx, sy, sw, sh, [10, 14, 9, 13].map(v => v * T.rs), { seed: T.seed + 750, fill: T.paper, sh: o.scrolled ? T.sh + 3 : T.sh });
      [[sx + 8, sy + 8], [sx + sw - 8, sy + 8]].forEach(([cx, cyy], j) => { const g = this.shape(cx - 3.5, cyy - 3.5, 7, 7, [3.5, 3.5, 3.5, 3.5], [1.2, 1.2, 1.6, 1.2], .3, T.seed + 760 + j, 2); Ls.push(this.L(g.ink, T.ink), this.L(g.fill, T.nail)); });
      const hit = (key, x) => { const pr = o.pressed === key; if (pr) Ls.push(this.L(this.wob(x - 8, cy - 20, 40, 40, [20, 18, 20, 17], .8, T.seed + 770, 3), T.surface2)); if (o.focus === key) Ls.push(this.ring(x - 10, cy - 22, 44, 44, [22, 20, 22, 19], T, T.seed + 771)); return pr ? 1.5 : 0; };
      if (o.nav !== false) { const dy = hit('nav', sx + 16); Ls.push(...this.icon(T, 'back', sx + 16, cy - 12 + dy)); }
      (o.actions || ['search', 'more']).forEach((a, i) => { const x = sx + sw - 16 - 24 - i * 40, dy = hit(i, x); Ls.push(...this.icon(T, a, x, cy - 12 + dy)); });
      const X = o.variant === 'center' ? this.X(title, w / 2, cy + fs * .36, { ff: T.display, fw: 400, fs, fill: T.text, anchor: 'middle' }) : this.X(title, sx + 56, cy + fs * .36, { ff: T.display, fw: 400, fs, fill: T.text });
      return { L: Ls, X: [X] };
    }
    navBar(T, o) {
      const w = o.w || 390, items = o.items, n = items.length, slot = w / n, Ls = [], Xs = [], over = [];
      Ls.push(...this.body(T, -8, 0, w + 16, 96, [22, 26, 0, 0], { seed: T.seed + 800, fill: T.paper, sh: 0 }));
      const ia = o.ia ?? (slot * o.sel + slot / 2 - 32), ib = o.ib ?? (ia + 64);
      Ls.push(this.L(this.wob(ia, 10, Math.max(8, ib - ia), 34, [17, 15, 17, 16], 1.6, T.seed + 801, 4), T.chipSel));
      items.forEach((it, i) => {
        const cx = slot * i + slot / 2, act = i === o.sel, pr = o.pressed === i, dis = o.disabled === true || o.disabled === i, iop = dis ? .4 : 1, iy = 15 + (act ? o.iy || 0 : 0) + (pr ? 2 : 0);
        if (o.focus === i) Ls.push(this.ring(cx - 34, 4, 68, 72, [16, 12, 18, 13], T, T.seed + 805 + i));
        if (pr && !act) Ls.push(this.L(this.wob(cx - 28, 10, 56, 34, [17, 15, 17, 16], 1.2, T.seed + 806, 4), T.surface2));
        if (act && o.du > 0 && o.du < 1) Ls.push(...this.dust(T, o.du, cx, 39, 20, { gap: 11, k: .55, dirs: [[-1, -.12, 1], [-.7, -.45, .75], [.7, -.45, .75], [1, -.12, 1]] }).L);
        Ls.push(...this.scaleAbout(this.icon(T, it.icon, cx - 12, iy, { fill: act ? T.primary : null, bold: act, color: act ? T.ink : T.text, op: iop }), cx, iy + 24, (act ? o.isx || 1 : 1) * (pr ? 1.12 : 1), (act ? o.isy || 1 : 1) * (pr ? .84 : 1)));
        Xs.push(this.X(it.label, cx, 66, { ff: T.body, fw: act ? 800 : 600, fs: 12, fill: T.text, anchor: 'middle', op: iop }));
        if (o.badge && o.badge.i === i) over.push(this.place(this.badge(T, o.badge), cx + 12, 16));
      });
      return this.cat({ L: Ls, X: Xs }, ...over);
    }
    tabs(T, o) {
      const w = o.w || 342, labels = o.labels, n = labels.length, slot = w / n, Ls = [], Xs = [];
      const ia = o.ia ?? slot * o.sel + 6, ib = o.ib ?? slot * (o.sel + 1) - 6;
      Ls.push(...this.body(T, ia, 4, Math.max(10, ib - ia), 46, [12, 9, 2, 2].map(v => v * T.rs), { ins: [2, 2.4, 0, 2.2], seed: T.seed + 850, fill: T.chipSel, noRim: true }));
      Ls.push(this.L(this.inkLine(-4, w + 4, 48, 3.6, T.seed + 851), T.ink));
      labels.forEach((lb, i) => { const act = i === o.sel, pr = o.pressed === i, dis = o.disabled === true || o.disabled === i; if (pr && !act) Ls.splice(0, 0, this.L(this.wob(slot * i + 8, 8, slot - 16, 40, [12, 9, 3, 3], T.amp, T.seed + 852 + i, 4), T.surface2)); if (o.focus === i) Ls.push(this.ring(slot * i + 2, -1, slot - 4, 54, [14, 11, 6, 6], T, T.seed + 853 + i)); Xs.push(this.X(lb, slot * i + slot / 2, 32 + (pr ? 2 : 0), { ff: T.body, fw: act ? 800 : 600, fs: 16, fill: act ? T.ink : T.text, anchor: 'middle', op: dis ? .45 : 1 })); });
      return { L: Ls, X: Xs };
    }
    listItem(T, o) {
      const w = o.w || 342, H = 72, sag = o.sag || 0, dy = 2 * Math.max(0, sag), dis = o.disabled, op = dis ? .5 : 1, sd = o.seed || 0, Ls = [], Xs = [];
      if (o.focus) Ls.push(this.ring(-10, 1, w + 20, H - 2, [16, 12, 18, 13], T, T.seed + 905 + sd));
      if (o.sel || sag > .01) Ls.push(this.L(this.wob(-8, 3, w + 16, H - 6, [14, 10, 16, 11], T.amp, T.seed + 900 + sd), T.surface2, { op: o.sel ? 1 : this.c01(sag * 1.4) }));
      Ls.push(...this.body(T, 0, 16 + dy, 40, 40, [20, 18, 20, 17], { ins: T.ins.map(v => v * .75), seed: T.seed + 910 + sd, step: 3, fill: o.lc || T.sColors[sd % 3], noRim: true, inkOp: dis ? .4 : 1 }));
      Ls.push(...this.icon(T, o.icon, 8, 24 + dy, { color: T.ink, op }));
      Xs.push(this.X(o.title, 56, (o.sub ? 33 : 42) + dy, { ff: T.body, fw: 700, fs: 16, fill: T.text, op }));
      if (o.sub) Xs.push(this.X(o.sub, 56, 54 + dy, { ff: T.body, fw: 500, fs: 14, fill: T.muted, op }));
      if (o.trailing === 'chevron') Ls.push(...this.icon(T, 'chevron', w - 28, 24 + dy, { op }));
      if (o.trailing === 'switch') Ls.push(...this.place(this.sw(T, { on: o.on, disabled: dis, ...(o.swA || {}) }), w - 60, 12 + dy).L);
      if (o.trailing === 'check') Ls.push(...this.place(this.check(T, { on: o.on, disabled: dis }), w - 48, 12 + dy).L);
      if (!o.last) Ls.push(this.L(this.inkLine(0, w, H, 2.6, T.seed + 920 + sd, 6 * sag), T.ink, { op: dis ? .4 : 1 }));
      return { L: Ls, X: Xs };
    }
    divider(T, o) { return { L: [this.L(this.inkLine(o.x0 || 0, o.x1, 6, o.th || 3, T.seed + 970 + (o.seed || 0)), T.ink)], X: [] }; }
    badge(T, o) {
      const s = o.s ?? 1, Ls = [], Xs = [];
      if (s < .02) return { L: [], X: [] };
      if (o.dot) { const r = 5.5 * s, g = this.shape(-r - 1.5, -r - 1.5, 2 * r + 3, 2 * r + 3, [r + 1.5, r + 1.5, r + 1.5, r + 1.5], [1.4, 1.4, 1.9, 1.4], .3, T.seed + 950, 2); return { L: [this.L(g.ink, T.ink), this.L(g.fill, T.danger)], X: [] }; }
      const txt = String(o.n), rx = (12 + (txt.length - 1) * 4.6) * s, ry = 12 * s, dx = o.dx || 0, tf = `translate(${this.f(dx)} 0)`;
      Ls.push(this.L(this.burst(rx + 3.2, ry + 3.2, 9, 960), T.ink, { tf }), this.L(this.burst(rx, ry, 9, 960), T.danger, { tf }));
      const ft = { ff: T.body, fw: 800, fs: 12, fill: T.onDanger, anchor: 'middle' };
      Xs.push(this.X(txt, dx, 4.3 + (o.numDy || 0), { ...ft, op: o.numOp ?? 1 }));
      if (o.old != null && (o.oldOp ?? 0) > .01) Xs.push(this.X(String(o.old), dx, 4.3 + (o.oldDy || 0), { ...ft, op: o.oldOp }));
      return { L: Ls, X: Xs };
    }

    // ---- motion curves (ms in, params out) ----
    squashOf(p) { return { sy: p > 0 ? 1 - .15 * p : 1 - .26 * p, sx: p > 0 ? 1 + .12 * p : 1 + .1 * p }; }
    chkAnim(t, on) {
      if (on) { const sq = t < 50 ? 1 - .14 * (t / 50) : 1 - .14 * this.osc((t - 50) / 1000, .35, 40), u = this.c01((t - 50) / 120); return { on: true, trim: 1 - Math.pow(1 - u, 3), sx: sq, sy: sq, dx: 0, sp: this.c01((t - 150) / 170) }; }
      const u = Math.min(1, t / 100), dx = t > 100 && t < 160 ? 1.5 * Math.sin((t - 100) / 60 * 2 * Math.PI) : 0; return { on: t < 100, trim: 1 - u * u, sx: 1, sy: 1, dx };
    }
    swAnim(t, from, to) {
      const d = to - from;
      if (t < 40) { const u = t / 40; return { pos: from - d * .12 * u, sx: 1 - .12 * u, sy: 1 + .08 * u, origin: 'c', trackOn: !!from }; }
      if (t < 140) { const u = (t - 40) / 100, e = u * u * (3 - 2 * u), s = 1 + .5 * Math.sin(Math.PI * u); return { pos: from - d * .12 + d * 1.12 * e, sx: s, sy: 1 / Math.sqrt(s), origin: 'c', trackOn: !!from, echo: d * Math.sin(Math.PI * u) }; }
      const a = this.osc((t - 140) / 1000, .35, 40); return { pos: to, sx: 1 - .28 * a, sy: 1 + .2 * a, origin: d > 0 ? 'r' : 'l', trackOn: !!to, wave: this.c01((t - 140) / 170) };
    }
    iconBounce(t) { if (t < 0) return 0; if (t < 140) return -7 * Math.sin(Math.PI * t / 140); return 1.6 * Math.exp(-(t - 140) / 60) * Math.sin((t - 140) / 22); }
    fabAppear(t) {
      if (t < 0) return { op: 0, dy: -70, sx: 1, sy: 1, du: -1 };
      if (t < 90) return { op: this.c01(t / 40), dy: -70 + 3 * Math.sin(t / 90 * Math.PI), sx: 1, sy: 1, du: -1 };
      if (t < 230) { const u = (t - 90) / 140; return { op: 1, dy: -70 * (1 - u * u), sx: 1 - .06 * u, sy: 1 + .1 * u, du: -1 }; }
      const a = this.osc((t - 230) / 1000, .35, 40); return { op: 1, dy: 0, sx: 1 + .16 * a, sy: 1 - .2 * a, du: (t - 230) / 300 };
    }
    radioSelect(t) {
      if (t < 60) return { ds: 1, dotY: -24, dsx: .9, dsy: 1.12, sp: -1 };
      if (t < 150) { const u = (t - 60) / 90; return { ds: 1, dotY: -24 * (1 - u * u), dsx: .9 - .12 * u, dsy: 1.12 + .2 * u, sp: -1 }; }
      const a = this.osc((t - 150) / 1000, .32, 38); return { ds: 1, dotY: 0, dsx: 1 + .45 * a, dsy: 1 - .4 * a, sp: (t - 150) / 220 };
    }
    chipSelect(t) { const sc = t < 90 ? 1 + .15 * Math.sin(Math.PI / 2 * t / 90) : 1 + .15 * this.osc((t - 90) / 1000, .25, 55), u = this.c01(t / 180); return { scale: sc, swipe: 1 - Math.pow(1 - u, 2) }; }
    dialogOpen(t) { const u = this.c01(t / 260), e = u < .5 ? 2 * u * u : 1 - Math.pow(-2 * u + 2, 2) / 2, s = t < 120 ? .6 + .5 * (t / 120) : 1 + .1 * this.osc((t - 120) / 1000, .4, 40); return { R: 330 + (190 - 330) * e, s, op: this.c01(t / 60), textOp: this.c01((t - 140) / 80), stars: 0 }; }
    dialogClose(t) { let s; if (t < 80) s = 1 + .05 * t / 80; else { const u = this.c01((t - 80) / 100); s = 1.05 * (1 - u * u); } const u2 = this.c01((t - 120) / 180); return { R: 190 + 140 * u2 * u2, s, op: t < 180 ? 1 : 0, textOp: 1 - this.c01(t / 60), stars: t >= 170 ? (t - 170) / 350 : 0 }; }
    worm(t) { const u = ((t % 1200) + 1200) % 1200 / 1200, J = v => v - Math.sin(4 * Math.PI * v) / (4 * Math.PI) * .9; return { a: -.15 + 1.35 * J(Math.max(0, u - .2)), b: -.15 + 1.35 * J(u), worm: true }; }
    navSelect(t, from, to, slot) {
      const x0 = slot * from + slot / 2 - 32, x1 = slot * to + slot / 2 - 32, dir = Math.sign(to - from) || 1;
      const lead = 1 - this.spr(t / 1000, .55, 34), trail = 1 - this.spr(Math.max(0, t - 70) / 1000, .55, 34);
      const ia = x0 + (x1 - x0) * (dir > 0 ? trail : lead), ib = x0 + 64 + (x1 - x0) * (dir > 0 ? lead : trail);
      let iy = 0, isx = 1, isy = 1, du = -1;
      if (t < 180) iy = -8 * Math.sin(Math.PI * t / 180); else { const a = this.osc((t - 180) / 1000, .35, 45); isx = 1 + .2 * a; isy = 1 - .24 * a; du = (t - 180) / 300; }
      return { ia, ib, iy, isx, isy, du };
    }
    tabsSwitch(t, from, to, slot) {
      const a0 = slot * from + 6, a1 = slot * to + 6, wd = slot - 12, dir = Math.sign(to - from) || 1;
      const lead = 1 - this.spr(t / 1000, .5, 40), trail = 1 - this.spr(Math.max(0, t - 80) / 1000, .5, 40);
      return { ia: a0 + (a1 - a0) * (dir > 0 ? trail : lead), ib: a0 + wd + (a1 - a0) * (dir > 0 ? lead : trail), cx: dir * 40 * this.spr(t / 1000, .45, 30), sel: t > 100 ? to : from };
    }
    listPress(t, down) { return down ? 1 - Math.pow(1 - this.c01(t / 100), 3) : this.spr(t / 1000, .3, 40); }
    badgeAppear(t) { if (t < 0) return { s: 0, numOp: 0 }; const s = t < 90 ? 1.3 * (1 - Math.pow(1 - t / 90, 2)) : 1 + .3 * this.osc((t - 90) / 1000, .4, 45); return { s, numOp: t > 42 ? 1 : 0 }; }
    badgeCount(t) { const u = this.c01(t / 100), v = this.c01((t - 60) / 120); return { oldDy: -14 * u * u, oldOp: 1 - u, numDy: -12 * Math.pow(1 - v, 2), numOp: this.c01((t - 60) / 40), dx: t < 240 ? 2 * Math.sin(2 * Math.PI * t / 60) * Math.exp(-t / 90) : 0 }; }
    sway(t, v0) { const w = 14, z = .25, wd = w * Math.sqrt(1 - z * z); return v0 / wd * Math.exp(-z * w * t / 1000) * Math.sin(wd * t / 1000); }


    // ---- v1 components ----
    rope(T, x0, x1, y, sx, sag, color, seed) {
      const f = this.f, n = 40, P = [], Y = x => y + sag * Math.exp(-Math.pow((x - sx) / 46, 2));
      for (let i = 0; i <= n; i++) { const u = i / n, x = x0 + (x1 - x0) * u; P.push([x, Y(x) + .5 * Math.sin(u * 11 + seed)]); }
      const d = 'M' + P.map(p => f(p[0]) + ' ' + f(p[1])).join(' L'); let h = '';
      for (let x = x0 + 4; x < x1 - 2; x += 6) h += `M${f(x - 1.6)} ${f(Y(x) + 1.8)} L${f(x + 1.6)} ${f(Y(x) - 1.8)} `;
      return [this.L(d, 'none', { stroke: T.ink, sw: 7.5 }), this.L(d, 'none', { stroke: color, sw: 4 }), this.L(h || 'M0 0', 'none', { stroke: T.ink, sw: 1, op: .45 })];
    }
    slider(T, o) {
      const w = o.w || 300, y = 24, v = this.c01(o.value ?? .4), q = o.grab || 0, x0 = 14, x1 = w - 14, tx = x0 + (x1 - x0) * v, sag = 5 * Math.max(0, q), dis = o.disabled, Ls = [];
      if (o.focus) Ls.push(this.ring(tx - 22, 2, 44, 44, [22, 20, 22, 19], T, T.seed + 1501));
      Ls.push(...this.rope(T, tx, x1, y, tx, sag, dis ? T.disabledFill : T.off, 3), ...this.rope(T, x0, tx, y, tx, sag, dis ? T.disabledFill : T.primary, 1));
      [x0 - 4, x1 + 4].forEach((px, j) => { const g = this.shape(px - 4, y - 8, 8, 16, [4, 4, 4, 4], [1.3, 1.3, 1.8, 1.3], .3, T.seed + 1510 + j, 2); Ls.push(this.L(g.ink, T.ink), this.L(g.fill, T.nail)); });
      const R = 12, sx = 1 + .28 * q, sy = 1 - .22 * q, by = y + sag;
      const g = this.shape(tx - R * sx, by - R * sy, 2 * R * sx, 2 * R * sy, [R, R * .95, R, R * .9].map(r => r * Math.min(sx, sy)), [1.8, 2.2, 3, 1.8], .5, T.seed + 1520, 3);
      if (!dis) Ls.push(this.L(g.ink, T.ink, { tf: `translate(${2 * (1 - this.c01(q))} ${2 * (1 - this.c01(q))})` }));
      Ls.push(this.L(g.ink, T.ink, { op: dis ? .4 : 1 }), this.L(g.fill, dis ? T.disabledFill : T.thumb));
      return { L: Ls, X: [], tx };
    }
    sliderGrab(t, down) { return down ? 1 - Math.pow(1 - this.c01(t / 80), 3) : this.spr(t / 1000, .35, 45); }
    menu(T, o) {
      const w = o.w || 220, items = o.items || ['Edit', 'Share', 'Delete'], ih = 44, H = 14 + items.length * ih + 10, h = Math.max(0, H * (o.u ?? 1)), Ls = [], Xs = [];
      const roll = (y, seed) => { const g = this.shape(0, y - 8, w, 16, [8, 7, 8, 7], [2, 2.2, 3, 2.2], T.amp * .6, seed, 4); return [this.L(g.ink, T.ink, { tf: `translate(${T.sh * .6} ${T.sh * .6})` }), this.L(g.ink, T.ink), this.L(g.fill, T.surface2), this.L(`M8 ${this.f(y + 1)} H${w - 8}`, 'none', { stroke: T.ink, sw: 1, op: .35 })]; };
      if (h > 2) Ls.push(...this.body(T, 6, 7, w - 12, h, [4, 4, 10, 8], { seed: T.seed + 1600, fill: T.paper, sh: T.sh, noRim: true }));
      Ls.push(...roll(7, T.seed + 1601));
      items.forEach((it, i) => {
        const iy = 14 + i * ih; if (iy + ih * .7 > h) return; const dz = /delete|remove/i.test(it), c = dz ? T.dangerText : T.text;
        if (o.sel === i || o.pressed === i) Ls.push(this.L(this.wob(14, iy + 4, w - 28, ih - 8, [10, 7, 11, 8], .8, T.seed + 1610 + i, 4), T.surface2));
        if (o.focus === i) Ls.push(this.ring(10, iy + 1, w - 20, ih - 2, [12, 9, 13, 10], T, T.seed + 1620 + i));
        const ic = (o.icons || [])[i]; if (ic) Ls.push(...this.icon(T, ic, 22, iy + 11, { size: 22, color: c, op: o.disabled === i ? .4 : 1 }));
        Xs.push(this.X(it, ic ? 54 : 24, iy + 28, { ff: T.body, fw: 600, fs: 16, fill: c, op: this.c01((h - iy - ih * .7) / 20) * (o.disabled === i ? .45 : 1) }));
      });
      Ls.push(...roll(7 + h, T.seed + 1602));
      return { L: Ls, X: Xs, h: H + 15 };
    }
    menuUnroll(t, open) { return open ? 1 - this.spr(t / 1000, .42, 24) : Math.max(0, 1 - Math.pow(this.c01(t / 160), 2)); }
    sheet(T, o) {
      const W = o.W || 360, Hs = o.Hs || 640, h = o.h || 300, y = Hs - h + (o.dy || 0), Ls = [], Xs = [];
      if (o.scrim > 0) Ls.push(this.L(`M0 0 H${W} V${Hs} H0 Z`, T.ink, { op: .5 * o.scrim }));
      const tg = this.shape(W / 2 - 30, y - 16, 60, 30, [14, 12, 4, 4], T.ins.map(v => v * .8), T.amp * .7, T.seed + 1701, 4);
      Ls.push(this.L(tg.ink, T.ink), this.L(tg.fill, T.chipSel), this.L(`M${W / 2 - 12} ${this.f(y - 7)} H${W / 2 + 12}`, 'none', { stroke: T.ink, sw: 2.6 }));
      Ls.push(...this.body(T, -6, y, W + 12, h + 40, [28, 24, 0, 0], { seed: T.seed + 1700, fill: T.paper, sh: 0 }));
      Xs.push(this.X(o.title || 'Share plan', 24, y + 46, { ff: T.display, fw: 400, fs: 24, fill: T.text }));
      return { L: Ls, X: Xs, y };
    }
    sheetMove(t, open, h) { if (open) return { dy: h * this.spr(t / 1000, .5, 22), scrim: this.c01(t / 200) }; const u = this.c01(t / 220); return { dy: h * u * u * 1.1, scrim: 1 - u }; }
    drawer(T, o) {
      const W = o.W || 300, H = o.H || 640, sx = Math.max(.02, o.sx ?? 1), k = (1 - Math.min(1, sx)) * .3, rot = o.rot || 0, sel = o.sel ?? 0, Ls = [], Xs = [], door = [];
      if (o.scrim > 0) Ls.push(this.L(`M0 0 H${o.SW || 360} V${H} H0 Z`, T.ink, { op: .45 * o.scrim }));
      door.push(...this.body(T, 0, 0, W, H, [0, 18, 22, 0], { seed: T.seed + 1800, fill: T.paper, sh: T.sh + 2 }));
      [90, H / 2, H - 90].forEach((hy, j) => { const g = this.shape(-4, hy - 16, 12, 32, [3, 4, 3, 4], [1.4, 1.6, 2, 1.4], .3, T.seed + 1810 + j, 2); door.push(this.L(g.ink, T.ink), this.L(g.fill, T.nail)); });
      const kn = this.shape(W - 30, H / 2 - 9, 18, 18, [9, 9, 9, 9], [1.8, 2, 2.6, 1.8], .4, T.seed + 1820, 3); door.push(this.L(kn.ink, T.ink), this.L(kn.fill, T.warn));
      const items = o.items || [['home', 'Home'], ['calendar', 'Weekend'], ['heart', 'Saved'], ['settings', 'Settings']];
      items.forEach(([ic], i) => { const iy = 120 + i * 56; if (i === sel) door.push(this.L(this.wob(16, iy - 8, W - 64, 42, [14, 10, 16, 11], 1.4, T.seed + 1830, 4), T.chipSel)); if (o.pressed === i && i !== sel) door.push(this.L(this.wob(16, iy - 8, W - 64, 42, [14, 10, 16, 11], 1, T.seed + 1831, 4), T.surface2)); door.push(...this.icon(T, ic, 28, iy + 1, { color: i === sel ? T.ink : T.text })); });
      const tf = `matrix(${sx.toFixed(3)} ${k.toFixed(3)} 0 1 0 0) rotate(${rot.toFixed(2)} 0 ${H / 2})`;
      Ls.push(...door.map(l => ({ ...l, tf: (tf + ' ' + l.tf).trim() })));
      const top = o.textOp ?? 1;
      Xs.push(this.X(o.title || 'Saturday', 24, 72, { ff: T.display, fw: 400, fs: 30, fill: T.text, op: top }));
      items.forEach(([, lb], i) => Xs.push(this.X(lb, 64, 120 + i * 56 + 19, { ff: T.body, fw: 700, fs: 16, fill: i === sel ? T.ink : T.text, op: top })));
      return { L: Ls, X: Xs };
    }
    drawerOpen(t) { const sx = 1 - this.spr(t / 1000, .35, 26), sh = t > 160 ? .7 * Math.sin((t - 160) / 16) * Math.exp(-(t - 160) / 90) : 0; return { sx: Math.max(.02, sx), rot: sh, scrim: this.c01(t / 220), textOp: this.c01((t - 180) / 100) }; }
    drawerClose(t) { const u = this.c01(t / 220); return { sx: Math.max(.02, 1 - u * u), rot: 0, scrim: 1 - u, textOp: 1 - this.c01(t / 60) }; }
    tooltip(T, o) {
      const s = o.s ?? 1, text = o.text || 'Tap to add a plan', fs = 14, w = this.tw(text, T.body, 600, fs) + 28, h = 34, bx = -w / 2, by = -h - 12;
      if (s < .02) return { L: [], X: [], w, h };
      const g = this.shape(bx, by, w, h, [14, 16, 13, 15], T.ins.map(v => v * .8), T.amp * .8, T.seed + 1900, 4), tf = `scale(${s.toFixed(3)})`;
      const tail = 'M-7 -14 Q-2 -6 1 0 Q4 -7 9 -14 Z', tailIn = 'M-4.6 -15.5 Q-1.5 -8 .9 -3.4 Q3 -8.5 6.6 -15.5 Z';
      return { L: [this.L(g.ink, T.ink, { tf: tf + ' translate(3 3)' }), this.L(tail, T.ink, { tf }), this.L(g.ink, T.ink, { tf }), this.L(g.fill, T.paper, { tf }), this.L(tailIn, T.paper, { tf })],
        X: [this.X(text, 0, by + 22, { ff: T.body, fw: 600, fs, fill: T.text, anchor: 'middle', op: this.c01((s - .85) / .15) })], w, h };
    }
    tipInflate(t, show) { return show ? Math.max(0, 1 - this.spr(t / 1000, .3, 30)) : Math.max(0, 1 - this.c01(t / 120)); }
    segmented(T, o) {
      const w = o.w || 300, H = 44, labels = o.labels || ['Day', 'Week', 'Month'], n = labels.length, slot = w / n, r = [14, 10, 16, 11].map(v => v * T.rs), Ls = [], Xs = [];
      const g = this.shape(0, 0, w, H, r, T.ins, T.amp, T.seed + 2000), dis = o.disabled;
      Ls.push(this.L(g.ink, T.ink, { tf: `translate(${T.sh} ${T.sh})`, op: dis ? 0 : 1 }), this.L(g.ink, T.ink, { op: dis ? .35 : 1 }), this.L(g.fill, dis ? T.disabledFill : T.paper));
      for (let i = 1; i < n; i++) Ls.push(this.L(`M${this.f(slot * i)} 9 L${this.f(slot * i + .6)} ${H - 9}`, 'none', { stroke: T.ink, sw: 2, op: .45 }));
      labels.forEach((lb, i) => { if (o.pressed === i && i !== o.sel) Ls.push(this.L(this.wob(slot * i + 6, 7, slot - 12, H - 14, [14, 12, 14, 13], .9, T.seed + 2005 + i, 4), T.surface2)); });
      const ia = o.ia ?? slot * o.sel + 5, ib = o.ib ?? slot * (o.sel + 1) - 5, sq = o.sq || 0;
      if (!dis || o.sel != null) Ls.push(...this.body(T, ia, 6 + 3 * sq, Math.max(10, ib - ia), H - 12 - 6 * sq, [16, 14, 16, 15], { ins: [1.6, 1.8, 2.6, 1.6], amp: 1.1, seed: T.seed + 2001, fill: dis ? T.disabledFill : T.chipSel, noRim: true, step: 4, inkOp: dis ? .35 : 1 }));
      labels.forEach((lb, i) => { const act = i === o.sel; if (o.focus === i) Ls.push(this.ring(slot * i + 1, -5, slot - 2, H + 10, [14, 11, 15, 12], T, T.seed + 2010 + i)); Xs.push(this.X(lb, slot * i + slot / 2, H / 2 + 5.5 + (o.pressed === i ? 1.5 : 0), { ff: T.body, fw: act ? 800 : 600, fs: 15, fill: act ? T.ink : T.text, anchor: 'middle', op: dis ? .5 : 1 })); });
      return { L: Ls, X: Xs };
    }
    segSwitch(t, from, to, slot) { const a = this.tabsSwitch(t, from, to, slot), base = slot - 12; return { ia: a.ia - 1, ib: a.ib + 1, sq: this.c01((a.ib - a.ia - base) / base) * .8, sel: a.sel }; }
    pull(T, o) {
      const W = o.W || 360, d = o.d || 0, cx = W / 2, f = this.f, Ls = [], by = o.by ?? d;
      [cx - 46, cx + 46].forEach((px, j) => { const g = this.shape(px - 5, -2, 10, 12, [4, 4, 5, 5], [1.3, 1.3, 1.8, 1.3], .3, T.seed + 2100 + j, 2); Ls.push(this.L(g.ink, T.ink), this.L(g.fill, T.nail)); });
      if (o.band !== false) { const band = `M${cx - 46} 6 Q${cx - 16} ${f(by * .92)} ${cx} ${f(by)} Q${cx + 16} ${f(by * .92)} ${cx + 46} 6`, th = 3.8 - Math.min(2.2, Math.max(0, by) / 55); Ls.push(this.L(band, 'none', { stroke: T.ink, sw: th + 2.6 }), this.L(band, 'none', { stroke: T.primary, sw: th })); }
      Ls.push(...this.spinner(T, cx, o.blobY ?? by + 14, 11, (o.ph || 0) / 1000 * 2 * Math.PI, o.bsx || 1, o.bsy || 1, T.primary));
      return { L: Ls, X: [] };
    }
    pullRelease(t, d0) { const u = this.c01(t / 120), st = 1 + .5 * Math.max(0, 1 - t / 160); return { by: Math.max(6, d0 * (1 - u) * (1 - u) - 12 * Math.sin(Math.PI * u)), blobY: 40 + (d0 + 14 - 40) * this.spr(t / 1000, .4, 30), bsx: 1 / Math.sqrt(st), bsy: st, band: t < 160 }; }
    titleCard(T, o) {
      const W = o.W || 360, H = o.H || 640, cx = W / 2, cy = o.cy ?? H * .42, f = this.f, Ls = [], Xs = [], ph = (o.phase || 0) % 34;
      for (let i = 0, r = Math.hypot(W, H) * .62 + ph; r > 24; i++, r -= 34) Ls.push(this.L(`M${f(cx + r)} ${f(cy)} A${f(r)} ${f(r * .96)} 0 1 0 ${f(cx - r)} ${f(cy)} A${f(r)} ${f(r * .96)} 0 1 0 ${f(cx + r)} ${f(cy)} Z`, i % 2 ? T.bg : T.surface2));
      const pw = o.pw || 280, phh = o.ph || 120, px = cx - pw / 2, py = cy - phh / 2 + (o.dy || 0), s = o.s ?? 1, tf = `translate(${cx} ${f(cy + (o.dy || 0))}) scale(${s.toFixed(3)}) translate(${-cx} ${f(-(cy + (o.dy || 0)))})`;
      const plq = [...this.body(T, px, py, pw, phh, [26, 18, 30, 20], { seed: T.seed + 2200, fill: T.paper, sh: T.sh + 3 }), this.L(this.wob(px + 9, py + 9, pw - 18, phh - 18, [18, 12, 22, 14], T.amp * .6, T.seed + 2201), 'none', { stroke: T.ink, sw: 1.6, dash: '2 5', op: .55 })];
      [[px + 20, py + 20, 0], [px + pw - 20, py + phh - 20, 1]].forEach(([sx, sy, j]) => plq.push(this.L(this.star(7.5, 3.4, 90 + j), T.ink, { tf: `translate(${f(sx)} ${f(sy)})` }), this.L(this.star(5.4, 2.4, 90 + j), T.warn, { tf: `translate(${f(sx)} ${f(sy)})` })));
      Ls.push(...plq.map(l => ({ ...l, tf: (tf + ' ' + l.tf).trim() })));
      const top = (o.textOp ?? 1) * this.c01((s - .9) / .1);
      Xs.push(this.X(o.title || 'Saturday', cx, py + phh / 2 + (o.sub ? 4 : 15), { ff: T.display, fw: 400, fs: o.fs || 44, fill: T.text, anchor: 'middle', op: top }));
      if (o.sub) Xs.push(this.X(o.sub, cx, py + phh / 2 + 32, { ff: T.body, fw: 600, fs: 14, fill: T.muted, anchor: 'middle', op: top }));
      return { L: Ls, X: Xs };
    }
    splashAt(t) { const s = t < 0 ? 0 : Math.max(0, 1 - this.spr(t / 1000, .4, 20)); return { s, phase: t * .04, textOp: this.c01((t - 160) / 120), stars: t > 120 && t < 470 ? (t - 120) / 350 : 0 }; }
    miniScreen(T, W, H, k) {
      const Ls = [this.L(`M0 0 H${W} V${H} H0 Z`, k ? T.surface2 : T.bg)];
      const B = k ? [[14, 24, W - 28, 70, T.primary], [14, 108, W - 28, 30, T.paper], [14, 150, W - 28, 30, T.paper], [14, 192, (W - 38) / 2, 30, T.paper]] : [[14, 24, W - 28, 46, T.paper], [14, 82, W - 28, 46, T.paper], [14, 140, W - 28, 46, T.paper], [14, 198, W - 28, 46, T.paper]];
      B.forEach(([x, y, w, h, c], i) => Ls.push(...this.body(T, x, y, w, h, [10, 7, 11, 8], { seed: T.seed + 2300 + i + k * 10, fill: c, sh: 3, noRim: true, step: 5 })));
      Ls.push(...this.body(T, 14, H - 66, W - 28, 44, [14, 10, 16, 11], { seed: T.seed + 2320 + k, fill: k ? T.paper : T.primary, sh: 3, noRim: true }));
      return Ls;
    }
    pushAt(t, W) { let ax = 0, asx = 1; if (t < 60) ax = 6 * t / 60; else { const u = this.c01((t - 60) / 180); ax = 6 - (W + 6) * u * u; asx = 1 + .35 * Math.sin(Math.PI * u); } return { ax, asx, bx: t < 90 ? W : W * this.spr((t - 90) / 1000, .74, 24) }; }
    pushScene(T, t, W = 180, H = 320) { const a = this.pushAt(t, W), f = this.f; return { L: [...this.scaleAbout(this.miniScreen(T, W, H, 0), W, H / 2, a.asx, 1).map(l => ({ ...l, tf: `translate(${f(a.ax)} 0) ` + l.tf })), ...this.miniScreen(T, W, H, 1).map(l => ({ ...l, tf: `translate(${f(a.bx)} 0) ` + l.tf }))], X: [] }; }
    holeScene(T, t, W = 180, H = 320) {
      const Ls = [...this.miniScreen(T, W, H, 0)], x0 = 14, y0 = 82, w0 = W - 28, h0 = 46, cx = x0 + w0 / 2, cy = y0 + h0 / 2, f = this.f;
      if (t > 0 && t < 420) {
        const u = this.c01(t / 380), e = u < .15 ? 0 : Math.pow((u - .15) / .85, 2.2), S = 1 + e * 9, tf = `translate(${f(cx)} ${f(cy)}) scale(${S.toFixed(3)}) translate(${f(-cx)} ${f(-cy)})`;
        this.hole(T, .05, x0, y0, w0, h0, [10, 7, 11, 8], 5).L.slice(0, 2).forEach((l, i) => Ls.push({ ...l, tf: tf + ' ' + l.tf, fill: i === 1 ? T.surface2 : l.fill, op: 1 }));
        Ls.push(...this.stars(T, t / 380, cx, y0, 30).L);
      }
      if (t >= 380) Ls.push(...this.miniScreen(T, W, H, 1).map(l => ({ ...l, op: this.c01((t - 380) / 80) })));
      return { L: Ls, X: [] };
    }

    // ---- output ----
    cell(name, sc, w, h, pad, scale = 1) {
      const [t, r, b, l] = pad, tr = { start: '0', middle: '-50%', end: '-100%' };
      const X = sc.X.filter(x => x.op > .01).map(x => ({ s: x.s, ff: x.ff, fw: x.fw, fill: x.fill, op: x.op, left: ((x.x + l) * scale).toFixed(1), top: ((x.y + t) * scale - x.fs * scale * .8).toFixed(1), fs: (x.fs * scale).toFixed(1), tx: tr[x.anchor] }));
      return { name, ov: 'visible', L: sc.L, X, vb: `${-l} ${-t} ${w + l + r} ${h + t + b}`, sw: Math.round((w + l + r) * scale), sh: Math.round((h + t + b) * scale) };
    }
    esc(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;'); }
    toSvg(sc) {
      let s = '';
      sc.L.forEach(l => { s += `<path d="${l.d}" fill="${l.fill}" fill-rule="${l.fr}" stroke="${l.stroke}" stroke-width="${l.sw}" transform="${l.tf}" opacity="${l.op}" stroke-dasharray="${l.dash}" stroke-dashoffset="${l.doff}" stroke-linecap="round" stroke-linejoin="round"></path>`; });
      sc.X.forEach(t => { if (t.op > .01) s += `<text x="${this.f(t.x)}" y="${this.f(t.y)}" font-family="${this.esc(t.ff)}" font-weight="${t.fw}" font-size="${t.fs}" fill="${t.fill}" text-anchor="${t.anchor}" opacity="${t.op}">${this.esc(t.s)}</text>`; });
      return s;
    }
  }
  window.ToonEngine = ToonEngine;
})();
