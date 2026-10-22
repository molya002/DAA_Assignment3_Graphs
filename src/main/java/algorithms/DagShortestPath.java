package algorithms;

import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DagShortestPath {
    public record Result(long[] distance, int[] parent) {
        public List<Integer> pathTo(int vertex) {
            List<Integer> path = new ArrayList<>();
            for (int u = vertex; u != -1; u = parent[u]) path.add(u);
            Collections.reverse(path);
            return path;
        }

        public long sum() {
            long sum = 0;
            for (long d : distance) sum += d;
            return sum;
        }
    }

    public static Result run(Graph g, Metrics metrics) {
        return run(g, Kahn.run(g, new Metrics()), metrics);
    }

    public static Result run(Graph g, int[] order, Metrics metrics) {
        if (!g.directed || !Kahn.valid(g, order)) throw new IllegalArgumentException("Invalid DAG order");
        long[] distance = new long[g.n];
        int[] parent = new int[g.n];
        int[] degree = new int[g.n];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(parent, -1);
        for (Edge e : g.edges) degree[e.to()]++;
        for (int u = 0; u < g.n; u++) if (degree[u] == 0) distance[u] = 0;
        for (int u : order) {
            for (Edge e : g.adj.get(u)) {
                metrics.edgeScans++;
                metrics.comparisons++;
                long candidate = distance[u] + e.weight();
                if (candidate < distance[e.to()]) {
                    distance[e.to()] = candidate;
                    parent[e.to()] = u;
                }
            }
        }
        return new Result(distance, parent);
    }
}
