SP-Solver
=====================================================

Framework providing different strategies to solve stacking problems (SP).

**********************************

Stacking problems describe situations in which a set of items has to be assigned to feasible
positions in stacks, such that certain constraints are respected and, if necessary, an objective function is optimized.
In practice, such problems for example occur in warehouses and container terminals.
In the present work heuristic approaches are developed for various stacking problems, where the focus is on
minimizing transport costs. MIP formulations are used for experimental comparison.

### DEPENDENCIES
- **CPLEX (latest - academic license)**
- **jgrapht-core-1.3.1**
- **Google Guava (latest)**

### BUILD PROCESS (IntelliJ IDEA)
```
Build -> Build Artifacts -> StorageLoadingProblems.jar
```

### RUN .jar and dynamically link CPLEX
```
$ java -jar -Djava.library.path="/opt/ibm/ILOG/CPLEX_Studio128/opl/bin/x86-64_linux/" StorageLoadingProblems.jar
```
