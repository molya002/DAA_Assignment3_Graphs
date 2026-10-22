from pathlib import Path
import csv
import math
import statistics
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

ROOT = Path(__file__).resolve().parents[1]
rows = list(csv.DictReader((ROOT / 'results/results.csv').open()))
assert len(rows) == 50
for row in rows:
    for key in ['n', 'm', 'dsu_ops', 'edge_scans', 'comparisons', 'max_depth', 'result']:
        row[key] = int(row[key])
    row['time_ms'] = float(row['time_ms'])
    row['density'] = 'sparse' if row['m'] == 4 * row['n'] else 'dense'
assert len({(r['algorithm'], r['n'], r['density']) for r in rows}) == 50
algorithms = ['Kruskal', 'Prim', 'Kosaraju', 'Kahn', 'DagShortestPath']
for r in rows:
    if r['algorithm'] in ['Kosaraju', 'Prim']:
        assert r['edge_scans'] == 2 * r['m']
    if r['algorithm'] in ['Kahn', 'DagShortestPath']:
        assert r['edge_scans'] == r['m']

def subset(algorithm, density):
    return sorted([r for r in rows if r['algorithm'] == algorithm and r['density'] == density], key=lambda r: r['n'])

def slope(points, key):
    x = [math.log(r['n']) for r in points]
    y = [math.log(r[key]) for r in points]
    mean_x, mean_y = statistics.mean(x), statistics.mean(y)
    return sum((a - mean_x) * (b - mean_y) for a, b in zip(x, y)) / sum((a - mean_x) ** 2 for a in x)

plots = ROOT / 'results/plots'
plots.mkdir(parents=True, exist_ok=True)
plt.rcParams.update({'font.size': 10, 'axes.grid': True, 'grid.alpha': .25})
palette = dict(zip(algorithms, ['#2563eb', '#dc2626', '#059669', '#7c3aed', '#d97706']))
fig, ax = plt.subplots(figsize=(10, 5))
for a in algorithms:
    for density, style in [('sparse', '-'), ('dense', '--')]:
        data = subset(a, density)
        work = [r['dsu_ops'] + r['edge_scans'] + r['comparisons'] for r in data]
        ax.loglog([r['n'] for r in data], work, style, marker='o', color=palette[a], label=f'{a}, {density}')
ax.set(xlabel='Vertices n', ylabel='Counted operations', title='Measured operations: scans + comparisons + DSU calls')
ax.legend(ncol=2, fontsize=8)
fig.tight_layout()
fig.savefig(plots / 'operations.png', dpi=180)
plt.close(fig)
fig, ax = plt.subplots(figsize=(10, 4.5))
for a in algorithms:
    for density, style in [('sparse', '-'), ('dense', '--')]:
        data = subset(a, density)
        ax.semilogx([r['n'] for r in data], [r['edge_scans'] / r['m'] for r in data],
                    style, marker='o', color=palette[a], label=f'{a}, {density}')
ax.set(xlabel='Vertices n', ylabel='edge_scans / m', title='Scans per input edge; coincident curves share the same ratio', ylim=(0, 2.2))
ax.legend(ncol=2, fontsize=8, loc='lower right')
fig.tight_layout()
fig.savefig(plots / 'scans_per_edge.png', dpi=180)
plt.close(fig)
fig, ax = plt.subplots(figsize=(10, 4.5))
for density, color in [('sparse', '#2563eb'), ('dense', '#d97706')]:
    data = subset('Kruskal', density)
    ax.semilogx([r['n'] for r in data], [r['edge_scans'] / r['m'] for r in data], marker='o', label=density, color=color)
ax.set(xlabel='Vertices n', ylabel='Examined edges / m', title='Kruskal selection stops when one component remains', ylim=(0, 1.08))
ax.legend()
fig.tight_layout()
fig.savefig(plots / 'kruskal_share.png', dpi=180)
plt.close(fig)

complexity = [
    ['DSU find / union', 'O(alpha(V)) amortized', 'O(V) total; O(1) per call', 'Union by size limits height; compression flattens paths.'],
    ['Kruskal', 'O(V + E log E)', 'O(V + E)', 'Sort edges, then near-constant amortized DSU operations.'],
    ['Prim (lazy heap)', 'O(V + E log(E+1))', 'O(V + E)', 'Up to E queued edges; each heap operation is logarithmic.'],
    ['Kosaraju', 'O(V + E)', 'O(V + E)', 'Two iterative DFS passes and one transpose graph.'],
    ['Kahn', 'O(V + E)', 'O(V)', 'Each vertex leaves the queue once; each edge updates a degree.'],
    ['DAG paths', 'O(V + E)', 'O(V)', 'Topological order, validation, degrees and one relaxation pass.'],
]

ratio_lines = []
for density in ['sparse', 'dense']:
    for a in algorithms:
        data = subset(a, density)
        if a == 'Kruskal':
            vals = [r['comparisons'] / (r['m'] * math.log2(r['m'])) for r in data]
            formula = 'comparisons / (E log2 E)'
        elif a == 'Prim':
            vals = [r['comparisons'] / (r['m'] * math.log2(r['m'])) for r in data]
            formula = 'comparisons / (E log2 E)'
        else:
            vals = [r['edge_scans'] / (r['n'] + r['m']) for r in data]
            formula = 'scans / (V + E)'
        ratio_lines.append([a, density, formula, f'{min(vals):.3f} - {max(vals):.3f}'])

slope_lines = []
for a in algorithms:
    for density in ['sparse', 'dense']:
        data = subset(a, density)
        slope_lines.append([a, density, f'{slope(data, "edge_scans"):.4f}',
                            f'{slope(data, "comparisons"):.4f}' if data[0]['comparisons'] else '-'])

cut = ('Consider a cut that respects the edges already chosen. Let e be a minimum-weight edge crossing it. '
       'Take an MST that does not contain e and add e, creating a cycle. This cycle contains another crossing edge f, '
       'whose weight is at least the weight of e. Replacing f with e preserves a spanning tree and cannot increase its weight. '
       'Thus there is an MST containing the chosen edges together with e. Kruskal uses cuts between DSU components, '
       'while Prim uses the cut between visited and unvisited vertices. Applying the same argument separately inside each '
       'connected component proves correctness of the returned forest.')
dag_proof = ('Every predecessor of a vertex occurs earlier in a topological order. Each indegree-zero vertex starts at zero, '
             'which is equivalent to a zero-weight edge from a virtual super-source. Assume that distances of all earlier vertices '
             'are already optimal. Every path to the current vertex ends with an incoming edge from one of those earlier vertices. '
             'Relaxing every such edge therefore considers the best possible last step. The current distance is consequently optimal '
             'before its outgoing edges are processed, completing the induction. Negative weights do not invalidate this argument '
             'because a DAG has no cycles; in a general graph negative cycles can make a finite shortest distance impossible, '
             'and the DAG ordering argument is unavailable.')
choices = [
    ['Sparse, undirected; edge list', 'Kruskal', 'Sorting and DSU are simple; no adjacency heap is needed.'],
    ['Undirected; adjacency lists available', 'Prim', 'Grow through local edges with a priority queue.'],
    ['Truly dense undirected graph', 'Array Prim alternative', 'An O(V^2) implementation can beat heap or sorting bounds; not implemented here.'],
    ['Directed; cycles possible', 'Kosaraju', 'Identify mutually reachable groups before scheduling.'],
    ['Need scheduling order or cycle detection', 'Kahn', 'Indegrees expose ready jobs and detect remaining cyclic jobs.'],
    ['Weighted DAG; negative edges allowed', 'DAG paths', 'One topological pass; no priority queue or repeated passes.'],
    ['General graph with negative edges', 'Bellman-Ford', 'Checks negative cycles; used as a small-test oracle here.'],
]

largest = [r for r in rows if r['n'] == 100000]
time_text = 'On this run, medians at n=100000 ranged from %.2f to %.2f ms across algorithms and densities. ' % (
    min(r['time_ms'] for r in largest), max(r['time_ms'] for r in largest))
time_text += ('Small cases are affected strongly by JIT compilation, timer noise, allocation and garbage collection. '
              'Larger graphs increase allocation pressure, particularly for transpose graphs and lazy heap entries. '
              'Five-run medians reduce isolated disturbances but do not remove JVM and hardware effects. '
              'The counters and exact reference checks are the main reproducible evidence; the times describe this run only.')
conventions = ('The generator resets Random(42) separately for each graph kind and each size/density case. '
               'When a=b, only b is sampled again; this detail is necessary to reproduce the reference values. '
               'Undirected generation shuffles vertices, creates a random spanning tree, then adds edges; DAG generation '
               'orients edges by shuffled rank. All generated weights are 1..1000. Ten cases produce 30 graph instances '
               'and 50 algorithm rows. Every row passes the assignment reference values, Kruskal/Prim agreement and scan checks.')
metric_text = ('Scans count main processing loops: Kosaraju DFS passes scan 2E; Kahn queue processing and DAG relaxation scan E each; '
               'Prim scans 2E adjacency entries. Transpose construction, indegree setup and validators are excluded from scans, '
               'although they remain linear work. Comparisons count sorting/heap weight comparisons and relaxation distance comparisons. '
               'DSU operations count find calls plus successful unions, including find calls made inside union. Stack depth is maximum '
               'explicit stack occupancy. The DAG benchmark reuses a precomputed order; run(g, metrics) also computes it when needed. '
               'Both public variants are O(V+E).')
slope_text = ('A log-log slope is fitted by least squares to log(n), log(counter). Traversal scans have slope exactly 1.0000 because '
              'E is a fixed multiple of n. Kruskal sorts every edge but may stop selection early: edge scans are at most E, so those '
              'scans cannot establish its E log E sorting cost. The table reports both scan slopes and weight-comparison slopes. '
              'For this bounded 1..1000 weight distribution, adaptive sorting and ties can also flatten comparison growth; '
              'the theoretical worst-case sorting bound remains O(E log E). A slightly steeper scan curve can reflect connectivity '
              'and early stopping over this finite range, rather than prove a sorting logarithm.')
dsu_text = ('Kruskal dsuOps/E ranges from %.3f to %.3f in sparse graphs and %.3f to %.3f in denser graphs. ' % tuple(
    value for density in ['sparse', 'dense'] for value in (
        min(r['dsu_ops']/r['m'] for r in subset('Kruskal', density)),
        max(r['dsu_ops']/r['m'] for r in subset('Kruskal', density)))))
dsu_text += ('The ratio is bounded because each examined edge causes two find calls and each accepted edge one successful union. '
             'Early stopping changes the ratio. These call counters do not count parent-pointer steps, so they confirm the number '
             'of calls rather than independently prove inverse-Ackermann amortized time. Constant-size initialized arrays add O(V) work.')

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='SmallBody', fontSize=9, leading=12, spaceAfter=7))
styles.add(ParagraphStyle(name='TitleSmall', fontSize=19, leading=23, spaceAfter=12, textColor=colors.HexColor('#16324f')))
story = []
md = ['# DAA Assignment 3 Graphs', 'Yerzhan Moldir | SE-2509 | 23 October 2026']

def heading(text):
    story.append(Paragraph(text, styles['TitleSmall']))
    md.append('\n## ' + text)

def paragraph(text):
    story.append(Paragraph(text, styles['SmallBody']))
    md.append('\n' + text)

def table(headers, data, widths):
    cells = [[Paragraph(str(x), styles['SmallBody']) for x in headers]]
    cells += [[Paragraph(str(x), styles['SmallBody']) for x in row] for row in data]
    t = Table(cells, colWidths=widths, repeatRows=1, hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#e5edf5')),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LINEBELOW', (0, 0), (-1, 0), .7, colors.HexColor('#a3b8cc')),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#f5f7fa')])]))
    story.extend([t, Spacer(1, 8)])
    md.append('\n| ' + ' | '.join(headers) + ' |')
    md.append('| ' + ' | '.join(['---'] * len(headers)) + ' |')
    md.extend('| ' + ' | '.join(str(x) for x in row) + ' |' for row in data)

def image(name, height):
    story.append(Image(str(plots / name), width=490, height=height))
    md.append(f'\n![{name}](results/plots/{name})')

heading('Graph algorithms and complexity')
paragraph('Yerzhan Moldir | SE-2509 | 23 October 2026. The project connects city districts with minimum road cost and analyzes dependent jobs using directed graphs. All required algorithms use one adjacency-list Graph class. The benchmark reproduces every reference result.')
table(['Algorithm', 'Time', 'Extra space', 'Reason'], complexity, [85, 112, 100, 193])
paragraph('V is the number of vertices and E the number of input edges. Graph storage itself is O(V+E) and is excluded from the extra-space column. Kruskal includes its sorted copy and output; lazy Prim can hold O(E) queued edges, including parallel edges. On simple connected graphs log E = O(log V), giving the familiar O(E log V) Prim bound. DSU initialization costs O(V). Empty and disconnected inputs still require vertex bookkeeping.')
paragraph(conventions)
paragraph('Validation uses JUnit 5: randomized DSU labels, 100 connected MST cross-checks, exhaustive tiny MST subsets, all-pairs BFS SCC checks up to 200 vertices, cycle/order checks, Bellman-Ford on 100 DAGs with negative weights, path restoration and long distances. The 100000-vertex chain completes iterative SCC with measured stack depth 100000. All three data files are used in tests.')
story.append(PageBreak())

heading('Correctness and algorithm selection')
paragraph('<b>Cut property.</b> ' + cut)
paragraph('<b>Induction on topological order.</b> ' + dag_proof)
table(['Situation', 'Choice', 'Reason'], choices, [165, 105, 220])
paragraph('Kruskal and Prim restart or retain separate components to return a minimum spanning forest. Self-loops cannot join DSU components and are never selected by Prim. Kahn reports a cycle if fewer than V vertices leave the queue. DAG parents identify the predecessor producing each improvement; following parents reaches a source because topological positions strictly decrease.')
story.append(PageBreak())

heading('Counters versus formulas')
paragraph(metric_text)
table(['Algorithm', 'Density', 'Normalization', 'Min - max'], ratio_lines, [100, 58, 205, 127])
paragraph('With E=4V or E=20V, the traversal ratios are exactly constant. Sorting and heap ratios remain in a bounded range over the measured sizes; finite measurements support the bounds but are not mathematical proofs. The denser family is still O(V) edges as V grows, rather than a truly quadratic dense graph.')
paragraph(dsu_text)
image('operations.png', 245)
story.append(PageBreak())

heading('Slope checks and scans per edge')
paragraph(slope_text)
table(['Algorithm', 'Density', 'Scan slope', 'Comparison slope'], slope_lines, [160, 95, 115, 120])
image('scans_per_edge.png', 220)
paragraph('Kosaraju and Prim overlap at 2; Kahn and DAG relaxation overlap at 1. The two densities overlap for these algorithms. Kruskal is below or at 1 because it can stop after V-1 accepted edges. A flat scans-per-edge line confirms linear scan work for the traversal phase; preprocessing is also linear but is tracked separately by the counter convention.')
story.append(PageBreak())

heading('Kruskal stopping and measured time')
image('kruskal_share.png', 220)
paragraph('Kruskal examines only a fraction of the denser edge list before all vertices connect; the sparse list often needs nearly every edge. Sorting still processes all E edges in both families. Prim scans all adjacency entries and performs many heap comparisons, so its heap work must also be considered when choosing between the algorithms. The input representation, available memory and actual counter values matter more than a blanket statement that one method is always faster.')
paragraph(time_text)
table(['Algorithm', 'Density', 'Median ms at n=100000'], [[r['algorithm'], r['density'], f'{r["time_ms"]:.3f}'] for r in largest], [180, 120, 190])
paragraph('Reproduction: mvn clean verify; mvn -q compile exec:java; python scripts/analyze.py. The CSV has 50 rows and raw_runs.csv contains 250 measured trials. One warm-up per algorithm/case is excluded. The benchmark fails on a reference mismatch, MST disagreement, invalid order or incorrect required scan count. Java and OS details are saved in results/environment.txt. Optional Tarjan and condensation bonuses are not included.')
paragraph('Sources: DAA Assignment 3 Graphs, sections 1-10 and reference table, supplied course PDF. Measurements: results/results.csv and results/raw_runs.csv produced by this project. AI assistance is acknowledged in README.md.')

def footer(canvas, doc):
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(colors.HexColor('#536579'))
    canvas.drawString(42, 27, 'DAA Assignment 3 | Graphs | SE-2509')
    canvas.drawRightString(553, 27, str(doc.page))

doc = SimpleDocTemplate(str(ROOT / 'REPORT.pdf'), pagesize=(595.28, 841.89),
                        rightMargin=42, leftMargin=42, topMargin=36, bottomMargin=42,
                        title='DAA Assignment 3 Graph Algorithms', author='Yerzhan Moldir')
doc.build(story, onFirstPage=footer, onLaterPages=footer)
(ROOT / 'REPORT.md').write_text('\n'.join(md) + '\n', encoding='utf-8')
print('Validated 50 CSV rows; wrote three plots, REPORT.md and REPORT.pdf')
