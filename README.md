# KDA Test Projects

## Purpose

This project validates the Kotlin Data Analysis (KDA) stack — a set of JetBrains libraries for data processing and visualization in Kotlin:

- **kotlinx.dataframe** — typed DataFrame API for data loading, transformation, and export
- **Kandy** — Kotlin-native charting library built on top of Lets-Plot
- **dataframe-geo / kandy-geo** — geospatial extensions for reading GeoJSON/Shapefiles and rendering maps
- **DataFrame Compiler Plugin** — Kotlin compiler plugin that generates typed accessors for DataFrame columns
- **Database integration** — reading data from relational databases via JDBC

Each Gradle subproject covers a specific area of the stack with automated tests that verify end-to-end scenarios: data is loaded, processed, and the output (file, chart, or printed result) is validated.

---

## Test Checklist

### files

| Test | Scenario | Assertion |
|------|----------|-----------|
| `basicCsvReadWriteTest` | Read a CSV file into a DataFrame and write the result back | File `jetbrains_repositories_new.csv` exists and is non-empty |
| `basicJsonReadWriteTest` | Read a JSON file into a DataFrame and write the result back | File `simple_new.json` exists and is non-empty |
| `helloWorldTest` | Basic smoke test | Executes without exception |

---

### geo

| Test | Scenario | Assertion |
|------|----------|-----------|
| `buildGeoGuideTest` | Load USA GeoJSON and world cities shapefile from remote URLs, build a map plot, export to JPEG | File `lets-plot-images/usaStates.jpg` exists and is non-empty |

---

### kandy

| Test | Scenario | Assertion |
|------|----------|-----------|
| `buildPlotOnCollectionTest` | Build a Kandy chart from a plain Kotlin collection | File `lets-plot-images/plotOnCollection.jpg` exists and is non-empty |
| `buildPlotOnDataFrameTest` | Build a Kandy chart from a DataFrame | File `lets-plot-images/plotOnDataFrame.jpg` exists and is non-empty |
| `buildPlotOnDataFrameWithCompilerPluginTest` | Build a Kandy chart from a DataFrame using compiler-plugin-generated typed accessors | File `lets-plot-images/plotOnDataFrameWithCompilerPlugin.jpg` exists and is non-empty |

---

### compilerPlugin

| Test | Scenario | Assertion |
|------|----------|-----------|
| `readCsvAndTransformDataTest` | Read a CSV file and transform data using compiler-plugin-generated column accessors | Executes without exception |

---

### databases

| Test | Scenario | Assertion |
|------|----------|-----------|
| `readTableFromMariaDBTest` | Connect to MariaDB via JDBC and read a table into a DataFrame | Executes without exception |
