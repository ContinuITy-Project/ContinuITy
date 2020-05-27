# Cobra

The Cobra service prepares the data required for generating load tests. Users can upload data and later refer to them.

## Uploading Data

The data can be uploaded via the REST endpoint `POST /measurement-data/{app-id}/{version}/{type}`. The `app-id` and `version` identify the application to which the data belongs. The `type` can be `open-xtrace` (see [OPEN.xtrace](https://github.com/spec-rgdevops/OPEN.xtrace)), `access-logs` ([Apache common log format](https://httpd.apache.org/docs/1.3/logs.html#common)), `csv` (CSV file with named columns), or `session-logs` (see [here](https://github.com/ContinuITy-Project/wessbas.behaviorModelExtractor/tree/master/examples/specj/input) for an example).

Cobra automatically learns workload models and intensities (numbers of concurrent users) from the data, which it stores into an [Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html). For the clustering, it utilizes the [Clustinator](https://github.com/ContinuITy-Project/clustinator) service.

## Artifact Creation

When being triggered, Cobra can generate the artifacts `traces`, `sessions`, and `behavior-model`. In addition, it always sets the `intensity` artifact. For the intensity calculation (which can include time series forecasting), the [Forecastic](https://github.com/ContinuITy-Project/forecastic) service is used.

## Configuration

The service can be configured using the [configuration management](../../..#configuration-management) of the Orchestrator. The following example illustrates the configuration space. We recommend to use the [CLI](../continuity.cli) for creating and editing the configurations.

```yaml
---
service: cobra
app-id: sis
time-zone: Europe/Berlin
traces:
  retention: P14D
  map-to-idpa: true
  discard-unmapped: true
  log-unmapped: false
  stop-on-failure: true
tailoring:
- - all
sessions:
  timeout: PT30M
  omit: false
  hash-id: false
  ignore-redirects: true
clustering:
  interval: P7D
  overlap: P21D
  lookback: 200
  initial:
    strategy: kmeans
    k: 20
    parallelize: -2
    num-seedings: 30
    convergence-tolerance: 1.0E-5
    quantile-range: 0.95
  append:
    strategy: minimum-distance
    min-sample-size: 500
    radius-factor: 1.1
  omit: false
intensity:
  resolution: PT1M
context:
  auto-detect: true
  variables:
    black_friday:
      type: boolean
      ignore-by-default: false
```

## Publications

The `tailoring` field in the configuration relates to *log-based service-tailoring*, whis is introduced in the following publication:

Henning Schulz, Tobias Angerstein, Dušan Okanović, and André van Hoorn: *Microservice-tailored generation of session-based workload models for representative load testing* ([full paper](https://continuity-project.github.io/files/SchulzAngersteinOkanovicvanHoorn2019MicroserviceTailoredGenerationOfSessionBasedWorkloadModelsForRepresentativeLoadTesting-camera-ready-stamped.pdf)), Proceedings of the 27th IEEE International Symposium on the Modeling, Analysis, and Simulation of Computer and Telecommunication Systems (MASCOTS 2019), 2019