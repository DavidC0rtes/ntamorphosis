# NTAMorphosis
Prototype CLI tool to perform Model-Based Mutation Testing between 
Networks of Timed Automata (NTA) in UPPAAL

## Prerequisites
- Unix-like OS
- Uppaal 4.1
- Java 11+
- [TraceMatcher](https://github.com/DavidC0rtes/SimmDiffUppaal)
- [UppalMutants](https://github.com/DavidC0rtes/UppaalMutants/tree/nta)
- [Juppaal](https://github.com/DavidC0rtes/juppaal)

## Usage
```
Usage: NTAMorphosis [-hV] [-dup] [-eq] [--[no-]gui] [-csv=<csvPath>]
                    [-csvb=<csvBisim>] [-dir=<outPath>] [--how=<strategy>]
                    [--model=<model>] [-op=<operators>]...
      -csv, --csv-path=<csvPath>
                           Name and path to csv with TraceMatcher's result.
      -csvb, --csv-bisim=<csvBisim>
                           Name and path to csv for bisimulation result.
      -dir, --mutants-dir=<outPath>
                           Path to directory where the mutants are.
      -dup, --duplicates   Compute bisimulation between each mutant.
      -eq, --equivalent    Compute bisimulation w/ respect to the original
                             model.
      --[no-]gui           Use the gui. True by default.
  -h, --help               Show this help message and exit.
      --how=<strategy>     How to generate traces, one of: random, biased
      --model=<model>      Path to model's file.
      -op, --operators=<operators>

  -V, --version            Print version information and exit.
```