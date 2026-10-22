package benchmark;

import graph.Graph;
import java.util.Random;

public class Generator {
    public static Graph generate(int n, int m, String kind) {
        if (n < 2 || m < 0 || (!kind.equals("undirected") && !kind.equals("directed") && !kind.equals("dag"))) {
            throw new IllegalArgumentException("Invalid generator parameters");
        }
        Random random = new Random(42);
        Graph g = new Graph(n, !kind.equals("undirected"));
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[i] = i;
        if (!kind.equals("directed")) {
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = rank[i];
                rank[i] = rank[j];
                rank[j] = temp;
            }
        }
        if (kind.equals("undirected")) {
            if (m < n - 1) throw new IllegalArgumentException("Not enough edges for spanning tree");
            for (int i = 1; i < n; i++) {
                int a = rank[random.nextInt(i)];
                g.addEdge(a, rank[i], 1 + random.nextInt(1000));
            }
        }
        while (g.edges.size() < m) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (a == b) b = random.nextInt(n);
            if (kind.equals("dag") && rank[a] > rank[b]) {
                int temp = a;
                a = b;
                b = temp;
            }
            g.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return g;
    }
}
