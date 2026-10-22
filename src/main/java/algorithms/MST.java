package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class MST {
    public record Result(long weight, List<Edge> edges, int components) {}

    public static Result kruskal(Graph g, Metrics metrics) {
        if (g.directed) throw new IllegalArgumentException("MST needs an undirected graph");
        List<Edge> sorted = new ArrayList<>(g.edges);
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight(), b.weight());
        });
        DSU dsu = new DSU(g.n, metrics);
        List<Edge> tree = new ArrayList<>();
        long weight = 0;
        for (Edge e : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            if (dsu.union(e.from(), e.to())) {
                tree.add(e);
                weight += e.weight();
            }
        }
        return new Result(weight, tree, dsu.components());
    }

    public static Result prim(Graph g, Metrics metrics) {
        if (g.directed) throw new IllegalArgumentException("MST needs an undirected graph");
        boolean[] used = new boolean[g.n];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight(), b.weight());
        });
        List<Edge> tree = new ArrayList<>();
        long weight = 0;
        int components = 0;
        for (int start = 0; start < g.n; start++) {
            if (used[start]) continue;
            components++;
            queue.add(new Edge(start, start, 0));
            while (!queue.isEmpty()) {
                Edge e = queue.remove();
                int u = e.to();
                if (used[u]) continue;
                used[u] = true;
                if (e.from() != u) {
                    tree.add(e);
                    weight += e.weight();
                }
                for (Edge next : g.adj.get(u)) {
                    metrics.edgeScans++;
                    if (!used[next.to()]) queue.add(next);
                }
            }
        }
        return new Result(weight, tree, components);
    }
}
