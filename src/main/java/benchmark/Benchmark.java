package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

public class Benchmark {
    private static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    private static final long[][] REFERENCE = {
        {147970, 27, 396274}, {30363, 1, 252411},
        {468911, 137, 1271911}, {97589, 1, 723157},
        {1486139, 373, 3941828}, {309550, 1, 2381342},
        {4715515, 1188, 12551891}, {968517, 1, 7376612},
        {15012094, 3923, 40096539}, {3056681, 1, 23512087}
    };

    private static long measure(PrintWriter csv, PrintWriter raw, String algorithm, String name,
                                Graph g, Function<Metrics, Long> action, long expected, long scans) {
        action.apply(new Metrics());
        double[] times = new double[5];
        Metrics first = null;
        long result = 0;
        for (int run = 0; run < 5; run++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            result = action.apply(metrics);
            times[run] = (System.nanoTime() - start) / 1_000_000.0;
            require(result == expected, algorithm + " reference mismatch: " + result + " != " + expected);
            if (scans >= 0) require(metrics.edgeScans == scans, algorithm + " edge scan mismatch");
            if (first == null) first = metrics;
            else require(metrics.dsuOps() == first.dsuOps() && metrics.edgeScans == first.edgeScans
                    && metrics.comparisons == first.comparisons && metrics.maxDepth == first.maxDepth,
                    "Counters changed between runs");
            raw.printf(Locale.US, "%s,%s,%d,%.6f%n", algorithm, name, run + 1, times[run]);
        }
        Arrays.sort(times);
        csv.printf(Locale.US, "%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d%n", algorithm, name, g.n,
                g.edges.size(), times[2], first.dsuOps(), first.edgeScans, first.comparisons, first.maxDepth, result);
        csv.flush();
        System.out.printf("%-16s n=%6d m=%7d result=%d%n", algorithm, g.n, g.edges.size(), result);
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("results"));
        Path pending = Path.of("results/results.pending.csv");
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(pending));
             PrintWriter raw = new PrintWriter(Files.newBufferedWriter(Path.of("results/raw_runs.csv")))) {
            csv.println("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
            raw.println("algorithm,graph,run,time_ms");
            int index = 0;
            for (int n : SIZES) {
                for (int density : new int[]{4, 20}) {
                    int m = density * n;
                    String name = n + "_" + (density == 4 ? "sparse" : "dense");
                    long[] expected = REFERENCE[index++];
                    Graph undirected = Generator.generate(n, m, "undirected");
                    long kruskal = measure(csv, raw, "Kruskal", name, undirected,
                            x -> MST.kruskal(undirected, x).weight(), expected[0], -1);
                    long prim = measure(csv, raw, "Prim", name, undirected,
                            x -> MST.prim(undirected, x).weight(), expected[0], 2L * m);
                    require(kruskal == prim, "MST algorithms disagree");
                    Graph directed = Generator.generate(n, m, "directed");
                    measure(csv, raw, "Kosaraju", name, directed,
                            x -> (long) Kosaraju.run(directed, x).count(), expected[1], 2L * m);
                    Graph dag = Generator.generate(n, m, "dag");
                    measure(csv, raw, "Kahn", name, dag, x -> {
                        int[] order = Kahn.run(dag, x);
                        require(Kahn.valid(dag, order), "Invalid topological order");
                        return (long) order.length;
                    }, n, m);
                    int[] order = Kahn.run(dag, new Metrics());
                    measure(csv, raw, "DagShortestPath", name, dag,
                            x -> DagShortestPath.run(dag, order, x).sum(), expected[2], m);
                }
            }
        }
        Files.move(pending, Path.of("results/results.csv"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(Path.of("results/environment.txt"), "Java: " + System.getProperty("java.version")
                + "\nVM: " + System.getProperty("java.vm.name") + "\nOS: " + System.getProperty("os.name")
                + "\nSeed: new Random(42) per graph\nRuns: one warm-up, five measured, median\n");
        System.out.println("All 50 rows passed reference and counter checks.");
    }
}
