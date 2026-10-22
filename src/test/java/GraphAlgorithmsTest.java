import algorithms.*;
import benchmark.Generator;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GraphAlgorithmsTest {
    @Test
    void dsuMatchesSimpleLabels() {
        Random random = new Random(17);
        int n = 100;
        int[] label = new int[n];
        for (int i = 0; i < n; i++) label[i] = i;
        Metrics metrics = new Metrics();
        DSU dsu = new DSU(n, metrics);
        int components = n;
        for (int step = 0; step < 500; step++) {
            int a = random.nextInt(n), b = random.nextInt(n);
            boolean different = label[a] != label[b];
            assertEquals(different, dsu.union(a, b));
            if (different) {
                int old = label[b], replacement = label[a];
                for (int i = 0; i < n; i++) if (label[i] == old) label[i] = replacement;
                components--;
            }
            assertEquals(components, dsu.components());
            for (int i = 0; i < n; i++) {
                int j = random.nextInt(n);
                assertEquals(label[i] == label[j], dsu.find(i) == dsu.find(j));
            }
        }
        assertEquals(n - components, metrics.successfulUnions);
        assertTrue(metrics.findCalls > 0);
        dsu.makeSet(3);
        assertEquals(3, dsu.components());
        assertNotEquals(dsu.find(0), dsu.find(1));
    }

    @Test
    void mstAgreementOnOneHundredConnectedGraphs() {
        Random random = new Random(71);
        for (int test = 0; test < 100; test++) {
            int n = 2 + random.nextInt(99);
            Graph g = new Graph(n, false);
            for (int u = 1; u < n; u++) g.addEdge(random.nextInt(u), u, random.nextInt(101) - 50);
            for (int i = 0; i < n * 4; i++) g.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 50);
            MST.Result a = MST.kruskal(g, new Metrics());
            MST.Result b = MST.prim(g, new Metrics());
            assertEquals(a.weight(), b.weight());
            validateForest(g, a, 1);
            validateForest(g, b, 1);
        }
    }

    private void validateForest(Graph g, MST.Result result, int components) {
        DSU dsu = new DSU(g.n, new Metrics());
        long weight = 0;
        for (Edge e : result.edges()) {
            assertTrue(dsu.union(e.from(), e.to()));
            weight += e.weight();
        }
        assertEquals(g.n - components, result.edges().size());
        assertEquals(components, result.components());
        assertEquals(components, dsu.components());
        assertEquals(weight, result.weight());
    }

    @Test
    void mstMatchesExhaustiveSubsets() {
        Random random = new Random(91);
        for (int test = 0; test < 40; test++) {
            Graph g = new Graph(5, false);
            for (int u = 1; u < 5; u++) g.addEdge(u - 1, u, random.nextInt(20) - 10);
            for (int i = 0; i < 5; i++) g.addEdge(random.nextInt(5), random.nextInt(5), random.nextInt(20) - 10);
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << g.edges.size()); mask++) {
                if (Integer.bitCount(mask) != g.n - 1) continue;
                DSU dsu = new DSU(g.n, new Metrics());
                long weight = 0;
                boolean tree = true;
                for (int i = 0; i < g.edges.size(); i++) if ((mask & (1 << i)) != 0) {
                    Edge e = g.edges.get(i);
                    if (!dsu.union(e.from(), e.to())) tree = false;
                    weight += e.weight();
                }
                if (tree && dsu.components() == 1) best = Math.min(best, weight);
            }
            assertEquals(best, MST.kruskal(g, new Metrics()).weight());
            assertEquals(best, MST.prim(g, new Metrics()).weight());
        }
    }

    @Test
    void inputGraphsAreUsedAndDisconnectedGraphReturnsForest() throws Exception {
        Graph g = Graph.read(Path.of("data/disconnected.txt"));
        MST.Result a = MST.kruskal(g, new Metrics());
        MST.Result b = MST.prim(g, new Metrics());
        assertEquals(5, a.weight());
        assertEquals(a.weight(), b.weight());
        validateForest(g, a, 3);
        validateForest(g, b, 3);
        Graph cycle = Graph.read(Path.of("data/cyclic.txt"));
        assertEquals(2, Kosaraju.run(cycle, new Metrics()).count());
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(cycle, new Metrics()));
        Graph dag = Graph.read(Path.of("data/dag.txt"));
        assertTrue(Kahn.valid(dag, Kahn.run(dag, new Metrics())));
        DagShortestPath.Result paths = DagShortestPath.run(dag, new Metrics());
        assertArrayEquals(new long[]{0, 0, -2, -2, 1, 0}, paths.distance());
        assertEquals(List.of(1, 4, 3, 5), paths.pathTo(5));
    }

    @Test
    void kosarajuMatchesAllPairsBfs() {
        Random random = new Random(34);
        for (int test = 0; test < 35; test++) {
            int n = test == 0 ? 200 : 1 + random.nextInt(70);
            Graph g = new Graph(n, true);
            for (int i = 0; i < 3 * n; i++) g.addEdge(random.nextInt(n), random.nextInt(n), 1);
            boolean[][] reachable = new boolean[n][];
            for (int u = 0; u < n; u++) reachable[u] = bfs(g, u);
            Metrics metrics = new Metrics();
            Kosaraju.Result result = Kosaraju.run(g, metrics);
            Set<Integer> ids = new HashSet<>();
            for (int u = 0; u < n; u++) {
                ids.add(result.component()[u]);
                for (int v = 0; v < n; v++) assertEquals(reachable[u][v] && reachable[v][u],
                        result.component()[u] == result.component()[v]);
            }
            assertEquals(ids.size(), result.count());
            assertEquals(2L * g.edges.size(), metrics.edgeScans);
        }
    }

    private boolean[] bfs(Graph g, int start) {
        boolean[] seen = new boolean[g.n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[start] = true;
        queue.add(start);
        while (!queue.isEmpty()) {
            for (Edge e : g.adj.get(queue.remove())) if (!seen[e.to()]) {
                seen[e.to()] = true;
                queue.add(e.to());
            }
        }
        return seen;
    }

    @Test
    void topologicalValidationAndCycles() {
        Graph g = Generator.generate(200, 1000, "dag");
        Metrics metrics = new Metrics();
        int[] order = Kahn.run(g, metrics);
        assertTrue(Kahn.valid(g, order));
        assertEquals(1000, metrics.edgeScans);
        assertFalse(Kahn.valid(g, new int[200]));
        assertFalse(Kahn.valid(g, new int[]{-1}));
        Graph cycle = new Graph(3, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 2, 1);
        cycle.addEdge(2, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(cycle, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(cycle, new Metrics()));
        Graph self = new Graph(1, true);
        self.addEdge(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(self, new Metrics()));
        assertEquals(1, Kosaraju.run(self, new Metrics()).count());
    }

    @Test
    void dagPathsMatchBellmanFordWithNegativeWeights() {
        Random random = new Random(63);
        for (int test = 0; test < 100; test++) {
            int n = 2 + random.nextInt(39);
            Graph g = new Graph(n, true);
            List<Integer> perm = new ArrayList<>();
            for (int u = 0; u < n; u++) perm.add(u);
            Collections.shuffle(perm, random);
            for (int i = 0; i < 4 * n; i++) {
                int a = random.nextInt(n), b = random.nextInt(n);
                if (a == b) continue;
                g.addEdge(perm.get(Math.min(a, b)), perm.get(Math.max(a, b)), random.nextInt(101) - 50);
            }
            long[] expected = new long[n];
            Arrays.fill(expected, Long.MAX_VALUE);
            int[] indegree = new int[n];
            for (Edge e : g.edges) indegree[e.to()]++;
            for (int u = 0; u < n; u++) if (indegree[u] == 0) expected[u] = 0;
            for (int pass = 0; pass < n - 1; pass++) {
                for (Edge e : g.edges) if (expected[e.from()] != Long.MAX_VALUE) {
                    expected[e.to()] = Math.min(expected[e.to()], expected[e.from()] + e.weight());
                }
            }
            Metrics metrics = new Metrics();
            DagShortestPath.Result result = DagShortestPath.run(g, metrics);
            assertArrayEquals(expected, result.distance());
            assertEquals(g.edges.size(), metrics.edgeScans);
            for (int v = 0; v < n; v++) {
                List<Integer> path = result.pathTo(v);
                assertEquals(v, path.get(path.size() - 1));
                assertEquals(0, indegree[path.get(0)]);
                assertTrue(path.size() <= n);
                long sum = 0;
                for (int i = 1; i < path.size(); i++) {
                    int a = path.get(i - 1), b = path.get(i);
                    long delta = result.distance()[b] - result.distance()[a];
                    assertTrue(g.adj.get(a).stream().anyMatch(e -> e.to() == b && e.weight() == delta));
                    sum += delta;
                }
                assertEquals(expected[v], sum);
            }
        }
    }

    @Test
    void pathOfOneHundredThousandVerticesIsStackSafe() {
        Graph g = new Graph(100000, true);
        for (int i = 1; i < g.n; i++) g.addEdge(i - 1, i, 1);
        Metrics metrics = new Metrics();
        assertEquals(g.n, Kosaraju.run(g, metrics).count());
        assertEquals(g.n, metrics.maxDepth);
        assertEquals(2L * (g.n - 1), metrics.edgeScans);
    }

    @Test
    void emptyGraphsParallelEdgesAndLargeDistances() {
        Graph empty = new Graph(0, false);
        assertEquals(0, MST.kruskal(empty, new Metrics()).components());
        assertEquals(0, MST.prim(empty, new Metrics()).weight());
        Graph directed = new Graph(0, true);
        assertEquals(0, Kosaraju.run(directed, new Metrics()).count());
        assertEquals(0, Kahn.run(directed, new Metrics()).length);
        assertEquals(0, DagShortestPath.run(directed, new Metrics()).sum());
        Graph g = new Graph(4, false);
        g.addEdge(0, 0, -100);
        g.addEdge(0, 1, 10);
        g.addEdge(0, 1, -5);
        assertEquals(-5, MST.kruskal(g, new Metrics()).weight());
        validateForest(g, MST.prim(g, new Metrics()), 3);
        Graph chain = new Graph(4, true);
        chain.addEdge(0, 1, Integer.MAX_VALUE);
        chain.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(4294967294L, DagShortestPath.run(chain, new Metrics()).distance()[2]);
        assertEquals(List.of(3), DagShortestPath.run(chain, new Metrics()).pathTo(3));
    }

    @Test
    void smallReferenceGraphsMatchAssignment() {
        for (int density : new int[]{4, 20}) {
            Graph u = Generator.generate(1000, 1000 * density, "undirected");
            Graph d = Generator.generate(1000, 1000 * density, "directed");
            Graph dag = Generator.generate(1000, 1000 * density, "dag");
            assertEquals(density == 4 ? 147970 : 30363, MST.kruskal(u, new Metrics()).weight());
            assertEquals(density == 4 ? 27 : 1, Kosaraju.run(d, new Metrics()).count());
            assertEquals(density == 4 ? 396274 : 252411, DagShortestPath.run(dag, new Metrics()).sum());
        }
    }
}
