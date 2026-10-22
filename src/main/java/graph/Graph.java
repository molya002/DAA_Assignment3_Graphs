package graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Graph {
    public record Edge(int from, int to, int weight) {}

    public final int n;
    public final boolean directed;
    public final List<List<Edge>> adj;
    public final List<Edge> edges = new ArrayList<>();

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.n = n;
        this.directed = directed;
        adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int u, int v, int weight) {
        if (u < 0 || v < 0 || u >= n || v >= n) throw new IllegalArgumentException("Invalid vertex");
        Edge e = new Edge(u, v, weight);
        edges.add(e);
        adj.get(u).add(e);
        if (!directed) adj.get(v).add(new Edge(v, u, weight));
    }

    public Graph reversed() {
        Graph result = new Graph(n, directed);
        for (Edge e : edges) result.addEdge(e.to(), e.from(), e.weight());
        return result;
    }

    public static Graph read(Path path) throws IOException {
        try (Scanner in = new Scanner(path)) {
            int n = in.nextInt();
            int m = in.nextInt();
            String type = in.next();
            if (m < 0 || (!type.equals("directed") && !type.equals("undirected"))) {
                throw new IllegalArgumentException("Invalid graph header");
            }
            Graph g = new Graph(n, type.equals("directed"));
            for (int i = 0; i < m; i++) g.addEdge(in.nextInt(), in.nextInt(), in.nextInt());
            if (in.hasNext()) throw new IllegalArgumentException("Unexpected data after edges");
            return g;
        }
    }
}
