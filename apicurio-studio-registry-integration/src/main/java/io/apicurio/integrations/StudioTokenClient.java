package io.apicurio.integrations;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

@ApplicationScoped
public class StudioTokenClient {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @ConfigProperty(name = "studio.auth.url")
    private String studioAuthUrl;
    
    @ConfigProperty(name = "studio.auth.realm")
    private String studioAuthRealm;
    
    @ConfigProperty(name = "studio.auth.client-id")
    private String studioAuthClientId;
    
    @ConfigProperty(name = "studio.auth.username")
    private String studioAuthUsername;
    
    @ConfigProperty(name = "studio.auth.password")
    private String studioAuthPassword;

    @Inject
    Vertx vertx;

    WebClient client;

    @PostConstruct
    void init() {
        this.client = WebClient.create(vertx);
    }

    private String token;

    public Future<String> getToken() {
        
        if (this.token != null) {
            return Future.succeededFuture(this.token);
        }

        Promise<String> resultToken = Promise.promise();

        client.postAbs(studioAuthUrl + "/auth/realms/" + studioAuthRealm + "/protocol/openid-connect/token")
            .as(BodyCodec.jsonObject())
            .sendForm(MultiMap.caseInsensitiveMultiMap()
                        .add("client_id", studioAuthClientId)
                        .add("grant_type", "password")
                        .add("scope", "openid")
                        .add("username", studioAuthUsername)
                        .add("password", studioAuthPassword),
                    ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<JsonObject> res = ar.result();
                            this.token = res.body().getString("access_token");
                            resultToken.complete(token);
                        } else {
                            log.error("Error retrieving oauth token", ar.cause());
                            resultToken.fail(ar.cause());
                        }
                    });
        
        return resultToken.future();

    }

}
