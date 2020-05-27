# IDPA

This service is responsible for managing instances of the Input Data and Properties Annotation (IDPA) (see publication below for details). An IDPA describes the target application's API and how to parameterize generated load tests, e.g., to use specific credentials when sending requests to the `/login` endpoint.

The service provides the REST endpoints `/application/{app-id}` and `/annotation/{app-id}` (both with `GET` and `POST`) for uploading or retrieving IDPAs. The endpoints can both be used by users or other services.

We recommend using the [CLI](../continuity.cli) when working with IDPAs. [This repository](https://github.com/ContinuITy-Project/idpa-demo) demonstrates how to do that.

## Publications

The IDPA is introduced in the following scientific publication:

Henning Schulz, Tobias Angerstein, Dušan Okanović, and André van Hoorn: *Microservice-tailored generation of session-based workload models for representative load testing* ([full paper](https://continuity-project.github.io/files/SchulzAngersteinOkanovicvanHoorn2019MicroserviceTailoredGenerationOfSessionBasedWorkloadModelsForRepresentativeLoadTesting-camera-ready-stamped.pdf)), Proceedings of the 27th IEEE International Symposium on the Modeling, Analysis, and Simulation of Computer and Telecommunication Systems (MASCOTS 2019), 2019