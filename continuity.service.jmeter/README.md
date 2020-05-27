# JMeter

This service can transform a `workload-model` of any type into a [JMeter](https://jmeter.apache.org/) load test. Also, it can execute the load test. However, for performance reasons, we recommend downloading the load test and executing it locally.

THe JMeter service considers the IDPA stored at the [IDPA service](../continuity.service.idpa) for parameterizing the generated load tests. Please see [this demo](https://github.com/ContinuITy-Project/idpa-demo) for details.