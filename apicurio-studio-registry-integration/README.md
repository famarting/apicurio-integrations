# Apicurio Studio integration with Apicurio Registry

This project allows to integrate Apicurio Studio and Apicurio Registry using CloudEvents. Apicurio Studio have to be configured to produce http events
to this service and this service will consume the events and create, update or delete corresponding artifacts in Apicurio Registry.

## How to run it?
For now I only tested this integration in a local environment, but it should be possible to run this service in Kubernetes or Openshift, as well as compiling this service
to a native executable in order to deploy this service in a FaaS environment.

### Run apicurio studio
In apicurio-studio project
`export APICURIO_HUB_INTEGRATIONS_HTTP_REGISTRY_INTEGRATION=http://localhost:8888`
`export APICURIO_SHARE_FOR_EVERYONE=true`
`./distro/quickstart/target/apicurio-studio-*-SNAPSHOT/bin/standalone.sh -c standalone-apicurio.xml`

### Run apicurio registry
In apicurio-registry project, inside app folder
`mvn quarkus:dev -Dquarkus.http.port=9090`

# Run this function
In this project
`mvn quarkus:dev`
