package io.apicurio.integrations;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.apicurio.registry.client.RegistryRestClient;
import io.apicurio.registry.client.RegistryRestClientFactory;

@ApplicationScoped
public class RegistryClientProducer {
    
    @ConfigProperty(name = "registry.api.url")
    String registryUrl;

    @Produces
    public RegistryRestClient registryClient() {
        return RegistryRestClientFactory.create(registryUrl);
    }

}
