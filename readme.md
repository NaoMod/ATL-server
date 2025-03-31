### ATL Transformation Server


This repository contains artifacts for the paper *"LLM-based Agents for Model-to-Model Transformation in ATL,"* to be submitted at MODELS 2025.

This project is a REST API server designed to manage and execute ATL transformations. It integrates with the `atl_zoo` repository to access transformation definitions and provides endpoints to manage, search, apply, and chain transformations. 



#### Features

- Add, list, and delete ATL transformations. 
- Apply transformations using uploaded input models. 

- Chain multiple transformations. 
- Search within ATL transformation files.

#### API Endpoints

**List all transformations:**

```bash
curl localhost:8080/transformations | jq
```

**List enabled transformations:**

```bash
curl localhost:8080/transformations/enabled | jq
```

**Get a Specific Transformation:**

```bash
curl localhost:8080/transformation/Class2Relational

```

**Apply a Transformation:**

`Unique Metamodels (source & target)`

```bash
curl localhost:8080/transformation/Class2Relational/apply -F IN="@./Class.xmi"        
```

`Multiple source Metamodels`

```bash
curl localhost:8080/transformation/Maven2Ant/apply \
  -F INMaven="@./example/mavenFile.xmi" \
  -F INProject="@./example/projectFile.xmi"

```

**Search Transformations:**

```bash
curl localhost:8080/transformations/search?query=<search_term>
```

**Categorize Transformations by their InputMetamodels:**

```bash
curl localhost:8080/transformations/byInputMetamodel | jq
```

**Search transformations by their Input and output metamodels:**

```bash
curl "http://localhost:8080/transformation/hasTransformation?inputMetamodel=Ant.ecore&outputMetamodel=Maven.ecore"
```
