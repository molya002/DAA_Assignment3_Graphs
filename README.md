# DAA Assignment 3 Graphs

Student: Yerzhan Moldir, SE-2509.

Repository: https://github.com/molya002/DAA_Assignment3_Graphs

Java 17+ and Maven project. Graph storage uses adjacency lists. All algorithms are implemented in this project; Java collections are used for lists, queues and sorting.

## IntelliJ IDEA

Open this folder or `pom.xml` as a Maven project. Select a JDK 17 or newer and allow Maven to load dependencies. Run `GraphAlgorithmsTest` for tests, `benchmark.Demo` for small examples, or `benchmark.Benchmark` for the complete experiment. The working directory must be this project folder.

## Commands

```sh
mvn clean verify
mvn -q compile exec:java -Dexec.mainClass=benchmark.Demo
mvn -q compile exec:java
python -m pip install -r requirements.txt
python scripts/analyze.py
```

In PowerShell, quote the Demo option: `mvn -q compile exec:java '-Dexec.mainClass=benchmark.Demo'`.

The benchmark generates all graphs from a fresh `Random(42)` per graph. Each algorithm has one warm-up and five measured runs. The CSV stores the median time and counters from one run, never the sum of five runs. Every measured result is checked against the assignment's reference table; any mismatch throws an exception. The final CSV is replaced only after the whole benchmark succeeds. Plotting and report generation read that CSV.

## Files

| Path | Contents |
|---|---|
| `src/main/java/graph` | Graph representation and text input |
| `src/main/java/algorithms` | DSU, Kruskal, Prim, Kosaraju, Kahn, DAG shortest paths |
| `src/main/java/metrics` | Operation counters |
| `src/main/java/benchmark` | Exact generator, benchmark and demonstration |
| `src/test/java` | JUnit 5 randomized and edge case tests |
| `data` | Three small graphs used by tests and Demo |
| `results/results.csv` | 50 checked results |
| `results/raw_runs.csv` | 250 individual measured times |
| `results/plots` | Three required PNG plots |
| `REPORT.pdf` | Five-page report |
| `REPORT.md` | Text version of report |

## Input

The first line is `n m directed` or `n m undirected`. Each of the next `m` lines is `u v weight`. Vertices are numbered from 0. Parallel edges, loops, isolated vertices and disconnected graphs are supported. MST methods require undirected input; SCC and topological methods require directed input. Cycles cause topological sorting and DAG shortest paths to throw a clear exception.

## Metrics

`dsu_ops` is public find calls plus successful unions. `edge_scans` counts edges processed by the algorithm's main traversal or Kruskal selection loop. Each undirected adjacency entry counts separately in Prim. For Kosaraju it counts the two DFS passes, excluding constructing the transpose. Kahn's indegree initialization, order validation and DAG indegree initialization are preprocessing and excluded from this counter. DAG timing uses a precomputed order, while its validation is inside the timed call. These conventions give exactly 2m for Kosaraju, m for Kahn and m for DAG relaxation. `comparisons` counts weight comparisons in sorting/priority queues and distance comparisons during relaxation. `max_depth` records the largest explicit stack occupancy in either Kosaraju pass. Other algorithms leave it zero.

The supplied graph families have m=4n and m=20n; both are asymptotically sparse. Here "dense" means the denser required family. The report separates Kruskal edge scans from sorting comparisons: the sorting logarithm does not imply a logarithmic factor in the number of edges selected for examination. The bound of DSU is amortized; counting calls alone does not measure parent-pointer traversals or prove that bound.

## Git and submission

Branches: `main`, `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, `feature/metrics`; release tag: `v1.0`. The repository bundle included in the submission preserves these branches and history: `git clone repository.bundle DAA_Assignment3` restores it. The same project is published in the GitHub repository linked above.

The assignment's detailed benchmark specifies ten size/density cases and five algorithms, so this project writes 50 rows. The later reference to "four generated graphs" is treated as an editing error. Optional bonus tasks are outside this implementation.

## AI use acknowledgement

OpenAI Codex assisted with implementation, tests, benchmark analysis and documentation. The submitted results are produced by the included executable code. The student should review and understand the code before the defense.
