package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.ArrayDeque;

public class Kahn {
    public static int[] run(Graph g, Metrics metrics) {
        if (!g.directed) throw new IllegalArgumentException("Topological order needs a directed graph");
        int[] degree = new int[g.n];
        for (Edge e : g.edges) degree[e.to()]++;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int u = 0; u < g.n; u++) if (degree[u] == 0) queue.add(u);
        int[] order = new int[g.n];
        int size = 0;
        while (!queue.isEmpty()) {
            int u = queue.remove();
            order[size++] = u;
            for (Edge e : g.adj.get(u)) {
                metrics.edgeScans++;
                if (--degree[e.to()] == 0) queue.add(e.to());
            }
        }
        if (size != g.n) throw new IllegalArgumentException("Graph has a cycle");
        return order;
    }

    public static boolean valid(Graph g, int[] order) {
        if (order.length != g.n) return false;
        int[] pos = new int[g.n];
        boolean[] seen = new boolean[g.n];
        for (int i = 0; i < order.length; i++) {
            int v = order[i];
            if (v < 0 || v >= g.n || seen[v]) return false;
            seen[v] = true;
            pos[v] = i;
        }
        for (Edge e : g.edges) if (pos[e.from()] >= pos[e.to()]) return false;
        return true;
    }
}
