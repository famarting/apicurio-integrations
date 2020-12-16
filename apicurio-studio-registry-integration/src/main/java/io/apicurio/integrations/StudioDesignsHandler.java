package io.apicurio.integrations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.client.RegistryRestClient;
import io.apicurio.registry.client.RegistryRestClientFactory;
import io.apicurio.registry.rest.beans.ArtifactMetaData;
import io.apicurio.registry.rest.beans.ArtifactSearchResults;
import io.apicurio.registry.rest.beans.EditableMetaData;
import io.apicurio.registry.rest.beans.IfExistsType;
import io.apicurio.registry.rest.beans.SearchOver;
import io.apicurio.registry.rest.beans.SearchedArtifact;
import io.apicurio.registry.rest.beans.SortOrder;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class StudioDesignsHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private static final String CREATE_EVENT = "io.apicurio.hub.design-created";
    private static final String UPDATE_EVENT = "io.apicurio.hub.design-updated";
    private static final String DELETE_EVENT = "io.apicurio.hub.design-deleted";

    @ConfigProperty(name = "studio.api.url")
    String studioApiUrl;

    @Inject
    StudioTokenClient studioToken;

    @Inject
    Vertx vertx;

    @Inject
    RegistryRestClient registryClient;

    WebClient client;

    @PostConstruct
    void init() {
        this.client = WebClient.create(vertx);
    }

    @Funq
    @CloudEventMapping(trigger = CREATE_EVENT)
    public void handleCreate(Map<String, Object> input) {
        log.info("Design {} created", input.get("id"));

        handleDesign(input.get("id").toString(), input.get("name").toString(), (designName, content) -> {
            InputStream data = new ByteArrayInputStream(content);
            String artifactId = designNameToArtifactId(designName);
            try {
                ArtifactMetaData artifact = registryClient.createArtifact(artifactId, null, IfExistsType.FAIL, data);
                updateDesignIdProperty(artifact, input.get("id").toString());
                log.info("Artifact {} created for design {}", artifact.getId(), designName);
            } catch (Exception e) {
                log.error("Error creating artifact", e);
            }
        });

    }

    @Funq
    @CloudEventMapping(trigger = UPDATE_EVENT)
    public void handleUpdate(Map<String, Object> input) {
        log.info("Design {} updated", input.get("id"));

        handleDesign(input.get("id").toString(), input.get("name").toString(), (designName, content) -> {
            InputStream data = new ByteArrayInputStream(content);
            String artifactId = designNameToArtifactId(designName);
            try {
                ArtifactMetaData artifact = registryClient.updateArtifact(artifactId, null, data);
                updateDesignIdProperty(artifact, input.get("id").toString());
                log.info("Artifact {} updated for design {}", artifact.getId(), designName);
            } catch (Exception e) {
                log.error("Error updated artifact", e);
            }
        });
    }

    private void handleDesign(String designId, String designName, BiConsumer<String, byte[]> artifactHandler) {
        studioToken.getToken()
            .onSuccess(studioAuthToken -> {
                client.getAbs(studioApiUrl + "/designs/" + designId + "/content")
                    .putHeader("authorization", "bearer "+studioAuthToken)
                    .send(ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> res = ar.result();
                            if (res.statusCode() == 200) {
                                artifactHandler.accept(designName, res.body().getBytes());
                            } else {
                                log.info("Error fetching design {} {}", res.statusCode(), res.statusMessage());
                            }
                        } else {
                            log.error("Error fetching design exception", ar.cause());
                        }
                    });
            });
    }

    private void updateDesignIdProperty(ArtifactMetaData artifact, String designId) {
        EditableMetaData metaData = new EditableMetaData();
        metaData.setLabels(Arrays.asList("design"+designId));
        registryClient.updateArtifactVersionMetaData(artifact.getId(), artifact.getVersion(), metaData);
    }

    @Funq
    @CloudEventMapping(trigger = DELETE_EVENT)
    public void handleDelete(Map<String, Object> input) {
        log.info("Design {} deleted", input.get("id"));
        ArtifactSearchResults results = registryClient.searchArtifacts("design"+input.get("id").toString(), SearchOver.labels, SortOrder.asc, 0, 1);
        Optional<SearchedArtifact> artifact = results.getArtifacts().stream().findFirst();
        if (artifact.isPresent()) {
            String artifactId = artifact.get().getId();
            registryClient.deleteArtifact(artifactId);
            log.info("Artifact {} deleted for design {}", artifactId, input.get("id"));
        } else {
            log.warn("Artifact not found, designId {}", input.get("id"));
        }
    }

    private String designNameToArtifactId(String designName) {
        return designName.toString().toLowerCase().replaceAll(" ", "-");
    }
}
