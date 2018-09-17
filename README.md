# ![logo](/images/infinity-small.png) ContinuITy
[![Build Status](https://travis-ci.org/ContinuITy-Project/ContinuITy.svg?branch=master)](https://travis-ci.org/ContinuITy-Project/ContinuITy)

ContinuITy is a research project on “Automated Performance Testing in Continuous Software Engineering”, launched by [NovaTec Consulting GmbH](https://www.novatec-gmbh.de/) and the [University of Stuttgart](https://www.uni-stuttgart.de/) ([Reliable Software Systems Group](https://www.iste.uni-stuttgart.de/rss/)). ContinuITy started in September 2017 with a duration of two years. It is funded by the [German Federal Ministry of Education and Research (BMBF)](https://www.bmbf.de/). For details about the research project, please refer to our [Web site](https://continuity-project.github.io/).

## Architecture Overview

:exclamation: **TODO: draft**

The architectural pattern is an orchestrator that receives orders at /order/submit and orchestrates the other services to generate the required output. The other services are called via RabbitMQ exchanges. The created artifacts are shared between the services by providing a link that can be called to retrieve the artifact. The following provides an overview of the application.

<img src="/images/architecture-overview.png" align="center" width="550" >

### Involved Services

#### SessionLogs

The SessionLogs service creates so-called session logs from request logs or traces. Currently, we support [inspectIT data](http://www.inspectit.rocks/) and [OPEN.xtrace](https://github.com/spec-rgdevops/OPEN.xtrace).

#### WorkloadModel

WorkloadModel stands for a set of services that generate workload models from session logs. Currently, we provide an implementation for [Wessbas models](https://github.com/Wessbas).

#### LoadTest

LoadTest stands for a set of services that generate (and potenitally execute) load tests from workload models. Currently, we provide implementations for [JMeter](http://jmeter.apache.org/) and [BenchFlow](https://github.com/benchflow/benchflow). While the BenchFlow service can only generate load tests, the JMeter service is also capable to execute.

#### IDPAApplication

Manages IDPA applications (TODO: details) to be used for session logs and load test generation.

#### IDPAAnnotation

Manages IDPA annotations (TODO: details) to be used for load test generation.

### Exchanges

Label | Exchange Name | Type | Purpose
--- | --- | --- | ---
 / | continuity.event.global.finished | event | A task has finished
X<sub>a</sub> | continuity.orchestrator.event.finished | event | An order has been processed
X<sub>b</sub> | continuity.task.sessionlogs.create | task | Create new session logs from monitoring data
X<sub>c</sub> | continuity.task.workloadmodel.create | task | Create a new workload model from session logs
X<sub>d</sub> | continuity.task.loadtest.create | task | Create a new load test from a workload model
X<sub>e</sub> | continuity.task.loadtest.execute | task | Execute a load test that already has been created
X<sub>f</sub> | continuity.event.workloadmodel.created | event | A new workload model has been created
X<sub>g</sub> | continuity.event.idpaapplication.changed | event | An IDPA application changed

### Orders

Orders that can be submitted via /order/submit. A goal defines the required outcome (e.g., ```execute-load-test```, or ```create-workload-model```). A tag defines the application and the IDPA to be used. Options such as the duration of a load test of the load test implementation can be defined in an options field. Finally, the source data to be used for the generation has to be defined in the source field.

An example is provided below:

```json
{
  "goal": "execute-load-test",
  "tag": "my-application",
  "options": {
    "load-test-type": "jmeter",
    "duration": 30,
    "num-users": 1,
    "rampup": 1
  },
  "source": {
    "external-data": {
      "link": "http://inspectit:8182/rest/data/invocations?fromDate=2018/07/06/08:21:00&toDate=2018/07/06/08:21:20&limit=10",
      "timestamp": "2018-07-06T08-21-00-000Z"
    }
  }
}
```

### Orchestration

Based on a submitted order, the orchestrator determines the required steps (e.g., session logs creation, workload model creation, load test creation, and load test execution for the full path from monitoring data to test execution) and submits tasks to the services, e.g., to the SessionLogs service via ```continuity.task.sessionlogs.create```. The SessionLogs service will then generate the session logs.

When a service is finished with a task, it publishes an event to ```continuity.event.global.finished```. The orchestrator is notified and checks whether the task was successfully processed. If so, it submits the next task to the next service, e.g., to the Wessbas service. In case of an error, the order is aborted and the error reported to the user.

When an order is finished - either successfully processed or aborted - it will be published to he ```continuity.orchestrator.event.finished```exchange and can be retrieved via /order/wait. This endpoint can already be called before to wait for the order for a given time span.
