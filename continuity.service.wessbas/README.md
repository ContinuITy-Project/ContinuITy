# WESSBAS

This service is a wrapper around the [WESSBAS](https://github.com/Wessbas) approach. It can transform `sessions` or a `behavior-model` into a `workload-model` of the WESSBAS DSL. Such workload models are based on [Markov chains](https://en.wikipedia.org/wiki/Markov_chain), whereas each state represents one endpoint of the application to be tested. In addition, the model has think time specifications per Markov transition controlling the inter-request timing.

Please note we are using slightly modified forks of the main repositories:

* [wessbas.behaviorModelExtractor](https://github.com/ContinuITy-Project/wessbas.behaviorModelExtractor)
* [wessbas.dslModelGenerator](https://github.com/ContinuITy-Project/wessbas.dslModelGenerator)
* [wessbas.testPlanGenerator](https://github.com/ContinuITy-Project/wessbas.testPlanGenerator)

## Publications

The WESSBAS approach has been published in the following paper:

Christian Vögele, André van Hoorn, Eike Schulz, Wilhelm Hasselbring, and Helmut Krcmar: *WESSBAS: extraction of probabilistic workload specifications for load testing and performance prediction - a model-driven approach for session-based application systems*, Software and Systems Modeling 17(2): 443-477 (2018)