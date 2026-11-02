# B-05: summarise the raw logs in this directory. Usage: python3 summarise.py
import csv, re, statistics as st

def q(xs, p):
    xs = sorted(xs); k = (len(xs) - 1) * p; f = int(k); c = min(f + 1, len(xs) - 1)
    return xs[f] + (xs[c] - xs[f]) * (k - f)

def phone(path):
    rows = {}
    for line in open(path):
        m = re.search(r'ToonBench: frame,(\w+),(\d+),(.*)', line)
        if m:
            rows.setdefault(m.group(1), []).append((int(m.group(2)), [int(v) for v in m.group(3).split(',')]))
    print(path)
    for arm, xs in rows.items():
        tot = [v[1] / 1e6 for _, v in xs]
        over = sum(t > 16.667 for t in tot)
        missed = 0; slots = 0
        for rep in sorted({r for r, _ in xs}):
            vs = sorted(v[0] for r, v in xs if r == rep)
            slots += round((vs[-1] - vs[0]) / 16.667e6) + 1
            missed += sum(max(0, round((b - a) / 16.667e6) - 1) for a, b in zip(vs, vs[1:]))
        line = f'  {arm:8} n={len(tot):4} total p50={q(tot,.5):5.2f} p95={q(tot,.95):5.2f} p99={q(tot,.99):5.2f} >16.7ms={100*over/len(tot):4.1f}%  missed vsync slots={missed}/{slots} ({100*missed/slots:.1f}%)'
        if len(xs[0][1]) > 2:
            parts = list(zip(*[v[2:] for _, v in xs]))
            names = ['draw', 'sync', 'issue', 'gpu', 'scene']
            line += '\n           ' + '  '.join(f'{n} p50={q([x/1e6 for x in col],.5):.2f} p95={q([x/1e6 for x in col],.95):.2f}' for n, col in zip(names, parts))
        print(line)

def desktop(path):
    d = {}; built = {}
    for r in csv.DictReader(open(path)):
        d.setdefault(r['arm'], []).append(int(r['draw_ns']) / 1e6); built.setdefault(r['arm'], []).append(int(r['shapes_built']))
    print(path, '(CPU raster, ms per frame)')
    for arm, xs in d.items():
        print(f'  {arm:8} n={len(xs):4} p50={q(xs,.5):5.2f} p95={q(xs,.95):5.2f} max={max(xs):6.2f} shapes built/frame mean={st.mean(built[arm]):.1f} max={max(built[arm])}')

phone('pixel6a-logcat.txt')
phone('pixel6a-logcat-run2.txt')
desktop('desktop-frames.csv')
