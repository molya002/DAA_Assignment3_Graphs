package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.Arrays;
import java.util.List;

public class Kosaraju {
    public record Result(int count, int[] component) {}

    public static Result run(Graph g, Metrics metrics) {
        if (!g.directed) throw new IllegalArgumentException("SCC needs a directed graph");
        boolean[] seen = new boolean[g.n];
        int[] stack = new int[g.n];
        int[] next = new int[g.n];
        int[] order = new int[g.n];
        int finished = 0;
        for (int start = 0; start < g.n; start++) {
            if (seen[start]) continue;
            int depth = 1;
            stack[0] = start;
            seen[start] = true;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int u = stack[depth - 1];
                List<Edge> edges = g.adj.get(u);
                if (next[u] < edges.size()) {
                    int v = edges.get(next[u]++).to();
                    metrics.edgeScans++;
                    if (!seen[v]) {
                        seen[v] = true;
                        stack[depth++] = v;
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    }
                } else {
                    order[finished++] = u;
                    depth--;
                }
            }
        }
        Graph reverse = g.reversed();
        int[] component = new int[g.n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = finished - 1; i >= 0; i--) {
            int start = order[i];
            if (component[start] != -1) continue;
            int depth = 1;
            stack[0] = start;
            component[start] = count;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int u = stack[--depth];
                for (Edge e : reverse.adj.get(u)) {
                    metrics.edgeScans++;
                    if (component[e.to()] == -1) {
                        component[e.to()] = count;
                        stack[depth++] = e.to();
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    }
                }
            }
            count++;
        }
        return new Result(count, component);
    }
}
