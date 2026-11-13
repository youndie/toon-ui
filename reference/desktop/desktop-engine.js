// desktop — desktop extension for toon-ui. Loads after toon-engine.js and toon-kit.js and never edits them:
// new icons go into the shared ICONS table, new renderers onto ToonEngine.prototype, runtime helpers into window.DesktopKit.
(function () {
  const PK = window.DesktopKit = window.DesktopKit || { ok: false };
  PK.ready = () => !!(PK.ok && window.ToonKit && window.ToonKit.eng());
  const init = () => {
    if (!window.ToonEngine || !window.ToonKit) { setTimeout(init, 20); return; }
    if (PK.ok) return;
    const TE = window.ToonEngine, P = TE.prototype, ICONS = new TE().ICONS;

    // ---- icons: 24-dp centerline paths, width rule from the kit renderer ----
    Object.assign(ICONS, {
      play: { d: 'M7.6 5.3 L18.7 12.1 L7.8 18.9 Z', fill: 'M7.6 5.3 L18.7 12.1 L7.8 18.9 Z' },
      pause: { d: 'M8.7 5.7 V18.4 M15.3 5.6 V18.5' },
      stop: { d: 'M6.4 6.2 H17.7 V17.8 H6.3 Z', fill: 'M6.4 6.2 H17.7 V17.8 H6.3 Z' },
      rec: { d: 'M18.3 12.1 A6.3 6.1 0 1 1 5.7 11.9 A6.3 6.1 0 1 1 18.3 12.1 Z', fill: 'M18.3 12.1 A6.3 6.1 0 1 1 5.7 11.9 A6.3 6.1 0 1 1 18.3 12.1 Z' },
      mic: { d: 'M14.6 11.4 C14.6 13.2 13.5 14.3 12 14.3 C10.5 14.3 9.4 13.2 9.4 11.4 V7 C9.4 5.2 10.5 4.1 12 4.1 C13.5 4.1 14.6 5.2 14.6 7 Z M6.5 11.3 C6.6 14.8 8.9 17.3 12 17.3 C15.1 17.3 17.4 14.8 17.5 11.4 M12 17.4 V20.1 M8.9 20.2 H15.2', fill: 'M14.6 11.4 C14.6 13.2 13.5 14.3 12 14.3 C10.5 14.3 9.4 13.2 9.4 11.4 V7 C9.4 5.2 10.5 4.1 12 4.1 C13.5 4.1 14.6 5.2 14.6 7 Z' },
      wave: { d: 'M3.7 12.1 H4.6 M7.3 8.7 V15.4 M10.4 5.3 V18.8 M13.6 8.2 V15.9 M16.7 10.2 V13.9 M19.6 11.9 H20.4' },
      copy: { d: 'M8.7 8.5 H19.3 V19.6 H8.6 Z M15.5 8.3 V4.7 H4.7 V15.6 H8.4', fill: 'M8.7 8.5 H19.3 V19.6 H8.6 Z' },
      folder: { d: 'M3.8 6.3 H9.7 L11.5 8.4 H20.2 V18.9 H3.8 Z', fill: 'M3.8 6.3 H9.7 L11.5 8.4 H20.2 V18.9 H3.8 Z' },
      key: { d: 'M10.1 12.1 A3.8 3.7 0 1 1 2.6 11.9 A3.8 3.7 0 1 1 10.1 12.1 Z M10.2 12 H20.6 M17.7 12.1 V15.4 M14.6 12 V14.5', fill: 'M10.1 12.1 A3.8 3.7 0 1 1 2.6 11.9 A3.8 3.7 0 1 1 10.1 12.1 Z' },
      link: { d: 'M9.7 14.4 L14.4 9.7 M11.1 6.9 L12.6 5.4 C14.3 3.8 16.9 3.8 18.6 5.5 C20.2 7.1 20.2 9.7 18.6 11.4 L17.1 12.9 M12.9 17.1 L11.4 18.6 C9.7 20.2 7.1 20.2 5.5 18.6 C3.8 16.9 3.8 14.3 5.4 12.6 L6.9 11.1' },
      merge: { d: 'M6.3 4.4 V8.5 C6.3 11.6 12 12.3 12 15.4 V19.9 M17.7 4.4 V8.5 C17.7 11.6 12 12.3 12 15.4 M9.1 17.2 L12 20.1 L14.9 17.1' },
      download: { d: 'M12 4.2 V15 M7.4 10.7 L12 15.3 L16.6 10.6 M4.6 19.4 H19.4' },
    });

    // ---- tokens: layout, pointer, motion. Screens read every number from here. ----
    const TEX = { il: 'assets/textures/gouache-light.png', in: 'assets/textures/gouache-night.png', bc: null };
    P.DESKTOP = {
      layout: { edge: 8, inset: 14, titleH: 52, bottom: 20, right: 20, sidebar: { min: 240, def: 300, max: 360, compact: 248 }, split: 16, sheetPadX: 28, sheetPadTop: 22, textMax: 640, gutter: 44, playerH: 72, meetingHead: 148, radius: 12 },
      pointer: { min: 28, icon: 32, control: 40, row: 56, navRow: 44 },
      motion: { hoverIn: 80, hoverOut: 120, lift: 2, pulse: 1200 },
      sizes: { ref: [1280, 800], min: [960, 600] },
      themeNames: { light: 'InkGouacheLight', night: 'InkGouacheNight', cel: 'BrightCel' },
    };
    P.accents = function (T) { return T.key === 'bc' ? T.cardColors : T.sColors; };
    P.tok = function (T) {
      return { key: T.key, ink: T.ink, text: T.text, muted: T.muted, paper: T.paper, bg: T.bg, surface2: T.surface2, primary: T.primary, onPrimary: T.onPrimary, danger: T.danger, onDanger: T.onDanger, dangerText: T.dangerText,
        focus: T.focus, chipSel: T.chipSel, warn: T.warn, success: T.success, off: T.off, field: T.field, nail: T.nail,
        body: `'${T.body}', sans-serif`, display: `'${T.display}', '${T.body}', sans-serif`, selBg: T.chipSel, selFg: T.ink, tex: TEX[T.key] ? `url(${TEX[T.key]})` : 'none' };
    };

    // ---- motion curves (ms in, value out). Hover reuses the kit press ramp; pulse reuses the idle-breath cosine. ----
    P.hoverRamp = function (t, dur) { const u = this.c01(t / dur); return 1 - Math.pow(1 - u, 3); };
    P.recPulse = function (t) { return .5 - .5 * Math.cos(2 * Math.PI * t / 1200); };

    // ---- shapes ----
    P.blobPts = function (kind, cx, cy, R, n = 60) {
      if (kind === 'me') return this.rr(cx - R * .78, cy - R, R * 1.56, R * 2, [R * .78, R * .78, R * .78, R * .78], Math.max(1.2, R / 9));
      const poly = (k, th0, th) => { const s = 2 * Math.PI / k, a = (((th - th0) % s) + s) % s - s / 2; return Math.cos(Math.PI / k) / Math.cos(a); };
      const rf = [
        () => .93,
        th => .86 * Math.pow(Math.pow(Math.abs(Math.cos(th)), 4) + Math.pow(Math.abs(Math.sin(th)), 4), -.25),
        th => 1.22 * (.7 * poly(3, -Math.PI / 2, th) + .3 * .5),
        th => .98 * (.8 * poly(6, 0, th) + .2 * .866),
        th => .87 + .11 * Math.abs(Math.cos(4 * th)),
        th => { const t = th + .44, a = 1, b = .76; return .99 / Math.sqrt(Math.pow(Math.cos(t) / a, 2) + Math.pow(Math.sin(t) / b, 2)); },
        th => 1.1 * (.72 * poly(4, -Math.PI / 2, th) + .28 * .707),
        th => .81 + .19 * Math.cos(5 * (th + Math.PI / 2)),
      ][kind % 8];
      const dy = kind % 8 === 2 ? R * .12 : 0, pts = [];
      for (let i = 0; i < n; i++) { const th = -Math.PI / 2 + i / n * 2 * Math.PI, r = R * rf(th); pts.push([cx + Math.cos(th) * r, cy + dy + Math.sin(th) * r]); }
      return pts;
    };
    P.blobPath = function (pts, amp, seed) {
      const rnd = this.rng(seed), ph = [rnd() * 6.28, rnd() * 6.28, rnd() * 6.28], N = pts.length;
      return this.smooth(pts.map((p, i) => {
        const a = pts[(i - 1 + N) % N], b = pts[(i + 1) % N]; let nx = b[1] - a[1], ny = -(b[0] - a[0]); const L = Math.hypot(nx, ny) || 1; nx /= L; ny /= L;
        const s = i / N * 6.2832, d = amp * (.6 * Math.sin(3 * s + ph[0]) + .3 * Math.sin(7 * s + ph[1]) + .22 * Math.sin(13 * s + ph[2]));
        return [p[0] + nx * d, p[1] + ny * d];
      }));
    };
    // Speaker sticker: shape + letter carry identity, colour is the third cue. kind: named | unnamed | me
    P.sticker = function (T, o) {
      this._stc = this._stc || new Map();
      const s = o.size || 32, kind = o.kind || 'named', shp = kind === 'me' ? 'me' : ((o.shape || 0) % 8 + 8) % 8, key = [T.key, s, kind, shp, o.color || 0, o.letter].join('|');
      if (this._stc.has(key)) return this._stc.get(key);
      const k = s / 32, R = s / 2 - 1, cx = s / 2, cy = s / 2, amp = T.amp * .4 * k, seed = T.seed + 3000 + (shp === 'me' ? 200 : shp * 17);
      const heavy = kind === 'me' ? 3.1 : 2.3, outer = this.blobPath(this.blobPts(shp, cx, cy, R), amp, seed);
      const inner = this.blobPath(this.blobPts(shp, cx, cy - .75 * k, R - heavy * k), amp * .8, seed + 7), Ls = [];
      if (kind === 'unnamed') {
        Ls.push(this.L(inner, T.paper), this.L(this.blobPath(this.blobPts(shp, cx, cy, R - .9 * k), amp, seed), 'none', { stroke: T.ink, sw: 1.9 * k, dash: `${(3.6 * k).toFixed(1)} ${(2.8 * k).toFixed(1)}` }));
      } else {
        const fill = kind === 'me' ? T.primary : this.accents(T)[(o.color || 0) % 3];
        Ls.push(this.L(outer, T.ink, { tf: `translate(${this.f(2 * k)} ${this.f(2 * k)})` }), this.L(outer, T.ink));
        if (T.rim) Ls.push(this.L(inner, kind === 'me' ? T.btnHi : T.rim), this.L(inner, fill, { tf: `translate(0 ${this.f(1.3 * k)})` }));
        else Ls.push(this.L(inner, fill));
      }
      const fs = Math.round(s * (String(o.letter || '').length > 1 ? .38 : .5));
      const X = [this.X(String(o.letter ?? ''), cx, cy + fs * .36 + (shp === 2 ? 2.5 * k : 0), { ff: T.display, fw: 400, fs, fill: kind === 'unnamed' ? T.muted : T.ink, anchor: 'middle' })];
      const sc = { L: Ls, X }; this._stc.set(key, sc); return sc;
    };

    // Lift: hover raises a shadowed body 2 dp toward the light while its shadow stays on the table (4 → 6), press drops the shadow to 0.
    P.liftBody = function (T, x, y, w, h, r, o) {
      const p = this.c01(o.p || 0), lift = 2 * (o.hv || 0) * (1 - p), sh = (o.sh ?? T.sh) * (1 - p);
      const ins = (o.ins || T.ins).map(v => v * (o.insMul || 1)), g = this.shape(x, y, w, h, r, ins, o.amp ?? T.amp, o.seed, o.step), out = [];
      if (sh > .05) out.push(this.L(g.ink, T.ink, { tf: `translate(${this.f(sh)} ${this.f(sh)})`, op: o.inkOp ?? 1 }));
      const b = this.body(T, x, y, w, h, r, { ...o, sh: 0 });
      return out.concat(lift ? b.map(l => ({ ...l, tf: (`translate(${this.f(-lift)} ${this.f(-lift)}) ` + l.tf).trim() })) : b);
    };
    // Flat elements (rows, turns, text buttons) get a surface2 plate instead of a lift.
    P.plate = function (T, x, y, w, h, o = {}) { return this.L(this.wob(x, y, w, h, (o.r || [14, 10, 16, 11]).map(v => v * T.rs), o.amp ?? T.amp, T.seed + 3100 + (o.seed || 0), 6), o.fill || T.surface2, { op: o.op ?? 1 }); };
    // Apply the hover rule to any kit scene: body −2,−2, shadow layers stay put.
    P.hoverLift = function (sc, T, d = 2) {
      const isSh = l => l.fill === T.ink && /^translate\(([\d.]+) \1\)/.test(l.tf || '');
      return { ...sc, L: sc.L.map(l => isSh(l) ? l : { ...l, tf: (`translate(${-d} ${-d}) ` + l.tf).trim() }), X: sc.X.map(x => ({ ...x, x: x.x - d, y: x.y - d })) };
    };

    // 32-dp icon button for pointer UIs (turn actions, title bar). variant: standard | filled
    P.iconBtn32 = function (T, o) {
      const d = 32, p = this.c01(o.p || 0), sy = 1 - .15 * p, sx = 1 + .12 * p, bw = d * sx, bh = d * sy, x = (d - bw) / 2, y = d - bh, dis = o.disabled, filled = o.variant === 'filled', Ls = [];
      if (o.focus) Ls.push(this.ring(-5, -5, d + 10, d + 10, [16, 14, 17, 13], T, T.seed + 3201));
      Ls.push(...this.liftBody(T, x, y, bw, bh, [13, 11, 14, 10], { ins: T.ins.map(v => v * .7), amp: T.amp * .7, seed: T.seed + 3200 + (o.seed || 0), step: 3, fill: dis ? T.disabledFill : filled ? T.primary : T.paper, sh: dis ? 0 : 2.5, hv: dis ? 0 : o.hv, p, inkOp: dis ? .35 : 1, rimColor: filled ? T.btnHi : T.rim, noRim: dis }));
      const lift = 2 * (o.hv || 0) * (1 - p) * (dis ? 0 : 1);
      Ls.push(...this.icon(T, o.icon, 6 - lift, 6 + (d - bh) - lift + 1.2 * p, { size: 20, color: filled ? T.onPrimary : T.text, op: dis ? .4 : 1 }));
      return { L: Ls, X: [] };
    };
    // Record button. state: idle | recording | stopping. Labels are HTML on top; this draws shapes only.
    P.recBtn = function (T, o) {
      const w = o.w, H = 40, st = o.state || 'idle', p = this.c01(o.p || 0), sy = 1 - .15 * p, sx = 1 + .12 * p, bw = w * sx, bh = H * sy, x = (w - bw) / 2, y = H - bh, r = [16, 11, 18, 12].map(v => v * T.rs), Ls = [];
      const lift = 2 * (o.hv || 0) * (1 - p) * (st === 'stopping' ? 0 : 1);
      if (o.focus) Ls.push(this.ring(-6, -6, w + 12, H + 12, r.map(v => v + 6), T, T.seed + 3301));
      Ls.push(...this.liftBody(T, x, y, bw, bh, r, { seed: T.seed + 3300, fill: st === 'recording' ? T.danger : st === 'stopping' ? T.disabledFill : T.paper, sh: st === 'stopping' ? 0 : T.sh, hv: st === 'stopping' ? 0 : o.hv, p, insMul: st === 'recording' ? 1.3 : 1, rimColor: T.rim, noRim: st !== 'idle' }));
      const cx = 20 - lift, cy = y + bh / 2 - lift + 1.2 * p;
      if (st === 'stopping') Ls.push(...this.spinner(T, cx, cy, 7, (o.spin || 0) / 1000 * 2 * Math.PI, 1, 1, T.primary));
      else {
        const R = st === 'recording' ? 5.2 + 1.6 * (o.pulse || 0) : 6, g = this.shape(cx - R, cy - R, 2 * R, 2 * R, [R, R * .95, R, R * .9], [1.4, 1.6, 2.2, 1.4], .35, T.seed + 3310, 2);
        Ls.push(this.L(g.ink, T.ink), this.L(g.fill, st === 'recording' ? T.paper : T.danger));
      }
      return { L: Ls, X: [], lift, ty: (H - bh) + 1.2 * p };
    };
    P.searchBox = function (T, o) {
      const w = o.w, H = 40, f = this.c01(o.focus || 0), r = [12, 9, 14, 10].map(v => v * T.rs), Ls = [];
      if (o.ring) Ls.push(this.ring(-6, -6, w + 12, H + 12, r.map(v => v + 6), T, T.seed + 3401));
      Ls.push(...this.body(T, 0, 0, w, H, r, { ins: T.ins.map(v => v * (1 + .5 * f)), seed: T.seed + 3400, fill: (o.hv || 0) > .5 && f < .5 ? T.surface2 : T.field, rimColor: T.fieldRim, sh: 0 }));
      Ls.push(...this.icon(T, 'search', 11, 9, { size: 22, color: T.text }));
      if (o.kbdW) Ls.push(...this.body(T, w - o.kbdW - 9, 9, o.kbdW, 22, [7, 5, 8, 6], { ins: [1.1, 1.3, 2, 1.2], amp: .4, seed: T.seed + 3402, step: 3, fill: T.surface2, noRim: true }));
      return { L: Ls, X: [] };
    };
    P.navPlate = function (T, w, h, o) {
      const Ls = [];
      if (o.sel) Ls.push(this.L(this.wob(2, 3, w - 4, h - 6, [14, 10, 16, 11], 1.3, T.seed + 3500 + (o.seed || 0), 4), T.chipSel));
      else if ((o.hv || 0) > .01) Ls.push(this.plate(T, 2, 3, w - 4, h - 6, { seed: o.seed, op: o.hv }));
      if (o.focus) Ls.push(this.ring(-2, -1, w + 4, h + 2, [16, 12, 18, 13], T, T.seed + 3510));
      return Ls;
    };
    P.splitGrip = function (T, h, st) {
      const cx = 8, cy = h / 2, Ls = [], nail = (x, y, j) => { const g = this.shape(x - 3, y - 3, 6, 6, [3, 3, 3, 3], [1.1, 1.1, 1.5, 1.1], .25, T.seed + 3610 + j, 2); return [this.L(g.ink, T.ink), this.L(g.fill, T.nail)]; };
      if (st > 0) Ls.push(...this.liftBody(T, cx - 5, cy - 30, 10, 60, [5, 4, 5, 4], { ins: [1.4, 1.6, 2.4, 1.4], amp: .4, seed: T.seed + 3600, step: 3, fill: st === 2 ? T.chipSel : T.paper, sh: 2, hv: st === 1 ? 1 : 0, p: st === 2 ? 1 : 0, noRim: true }));
      const l = st === 1 ? 2 : 0;
      Ls.push(...nail(cx - l, cy - 12 - l, 0), ...nail(cx - l, cy + 12 - l, 1));
      return { L: Ls, X: [] };
    };
    P.scrollThumb = function (T, h, o) {
      const st = o.st || 0, a = this.c01(o.a), b = this.c01(o.b), w = st ? 10 : 7, x = 12 - w, y0 = a * h, y1 = Math.max(y0 + 28, b * h), Ls = [];
      if (st) Ls.push(this.L(`M7 4 L7.4 ${this.f(h - 4)}`, 'none', { stroke: T.ink, sw: 1.4, dash: '2 6', op: .5 }));
      Ls.push(...this.liftBody(T, x, Math.min(y0, h - 28), w, y1 - y0, [w / 2, w / 2 - .5, w / 2, w / 2 - 1], { ins: [1.2, 1.4, 2.2, 1.3], amp: .45, seed: T.seed + 3700, step: 4, fill: st === 2 ? T.chipSel : st === 1 ? T.paper : T.off, sh: st ? 2 : 0, hv: st === 1 ? 1 : 0, p: st === 2 ? 1 : 0, noRim: true }));
      return { L: Ls, X: [] };
    };
    // Level meter: a strip of cel blocks. state: live | silent | clip. level 0..1 drives how many blocks are lit.
    P.levelMeter = function (T, w, o = {}) {
      const n = Math.max(8, Math.floor((w + 3) / 13)), bw = (w - (n - 1) * 3) / n, H = 20, st = o.state || 'live', lit = st === 'silent' ? 0 : Math.round(this.c01(o.level ?? .5) * n), Ls = [];
      for (let i = 0; i < n; i++) {
        const on = i < lit, hot = i >= n - 3, fill = !on ? T.off : st === 'clip' && hot ? T.danger : hot ? T.warn : T.success;
        Ls.push(...this.body(T, i * (bw + 3), on ? 0 : 3, bw, on ? H : H - 3, [4, 3, 4, 3], { ins: [1.1, 1.2, 1.8, 1.1], amp: .3, seed: T.seed + 4000 + i, step: 3, fill, sh: 0, noRim: true, inkOp: on ? 1 : .45 }));
      }
      if (st === 'silent') Ls.push(this.L(this.inkLine(2, w - 2, H / 2 + 1, 2.2, T.seed + 4050), T.ink, { op: .8 }));
      if (st === 'clip') Ls.push(this.L(this.burst(9, 9, 7, 4060), T.ink, { tf: `translate(${this.f(w + 12)} ${H / 2})` }), this.L(this.burst(6.5, 6.5, 7, 4060), T.danger, { tf: `translate(${this.f(w + 12)} ${H / 2})` }));
      return { L: Ls, X: [] };
    };
    P.livelevel = function (t, seed = 0) { const s = t / 1000 + seed * 7.3; return this.c01(.42 + .22 * Math.sin(s * 5.1) + .14 * Math.sin(s * 13.7 + 1) + .08 * Math.sin(s * 29 + 2)); };
    P.sheet = function (T, w, h, o = {}) { return { L: this.body(T, 0, 0, w, h, (o.r || [20, 14, 24, 16]).map(v => v * T.rs), { seed: T.seed + 3800 + (o.seed || 0), fill: o.fill || T.paper, sh: o.sh ?? T.sh + 2 }), X: [] }; };
    P.trafficSlots = function (T) { return { L: [20, 40, 60].map((x, j) => this.L(this.wob(x - 6, 20, 12, 12, [6, 6, 6, 6], .25, T.seed + 3900 + j, 2), 'none', { stroke: T.ink, sw: 1.3, op: .45, dash: '2 2.4' })), X: [] }; };

    // ---- runtime: one controller per component, rides the kit rAF loop ----
    const K = window.ToonKit;
    PK.E = () => K.eng();
    PK.T = p => K.eng().THEMES[K.themeKey(p && p.theme)];
    PK.tok = p => K.eng().tok(PK.T(p));
    PK.paint = (el, sc) => { if (!el) return; const s = K.eng().toSvg(sc); if (el._ps !== s) { el.innerHTML = s; el._ps = s; } };
    class Ctl {
      constructor(dc, draw) { this.dc = dc; this.draw = draw; this.an = {}; this.hvOn = {}; this.hv0 = {}; this.onTheme = () => { dc.forceUpdate(); draw(); }; window.addEventListener('toon-theme', this.onTheme); }
      start(n, dur) { this.an[n] = { t: K.clock, dur }; K.wake(this); }
      age(n) { const a = this.an[n]; if (!a) return null; return K.reduce() ? 1e6 : K.clock - a.t; }
      hover(n, on) { on = !!on; if (!!this.hvOn[n] === on) return; this.hv0[n] = this.hv(n); this.hvOn[n] = on; this.start('hv:' + n, on ? 80 : 120); }
      hv(n) { const on = !!this.hvOn[n], t = this.age('hv:' + n); if (t == null) return on ? 1 : 0; const e = K.eng().hoverRamp(t, on ? 80 : 120), v0 = this.hv0[n] || 0; return v0 + ((on ? 1 : 0) - v0) * e; }
      press(n, on) { this.pr = this.pr || {}; const cur = this.prv(n); this.pr[n] = { on, v0: cur }; this.start('pr:' + n, on ? 80 : 400); }
      prv(n) { const s = this.pr && this.pr[n]; if (!s) return 0; const t = this.age('pr:' + n); if (s.on) return s.v0 + (1 - s.v0) * (1 - Math.pow(1 - Math.min(1, t / 80), 3)); return t > 400 ? 0 : s.v0 * K.eng().spr(t / 1000, .35, 45); }
      wake() { K.wake(this); }
      frame() { this.draw(); const busy = Object.values(this.an).some(a => K.clock - a.t < a.dur) || (this.alive && this.alive()); if (!busy) K.sleep(this); }
      detach() { K.sleep(this); window.removeEventListener('toon-theme', this.onTheme); }
    }
    PK.ctl = (dc, draw) => new Ctl(dc, draw);
    PK.clock = () => K.clock;
    PK.reduce = () => K.reduce();
    PK.ok = true;
    window.dispatchEvent(new Event('desktop-ready'));
  };
  init();
})();
