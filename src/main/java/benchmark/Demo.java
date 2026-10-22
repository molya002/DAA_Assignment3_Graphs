package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.Path;
import java.util.Arrays;

public class Demo {
    public static void main(String[] args) throws Exception {
        Graph roads = Graph.read(Path.of("data/disconnected.txt"));
        MST.Result kruskal = MST.kruskal(roads, new Metrics());
        MST.Result prim = MST.prim(roads, new Metrics());
        System.out.println("Kruskal forest weight: " + kruskal.weight());
        System.out.println("Prim forest weight: " + prim.weight());
        System.out.println("Forest components: " + kruskal.components());
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        Kosaraju.Result scc = Kosaraju.run(cyclic, new Metrics());
        System.out.println("SCC count: " + scc.count());
        System.out.println("SCC labels: " + Arrays.toString(scc.component()));
        try {
            Kahn.run(cyclic, new Metrics());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        Graph dag = Graph.read(Path.of("data/dag.txt"));
        System.out.println("Topological order: " + Arrays.toString(Kahn.run(dag, new Metrics())));
        DagShortestPath.Result paths = DagShortestPath.run(dag, new Metrics());
        System.out.println("DAG distances: " + Arrays.toString(paths.distance()));
        for (int u = 0; u < dag.n; u++) System.out.println("Path to " + u + ": " + paths.pathTo(u));
    }
}
