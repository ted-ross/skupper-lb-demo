package io.skupper.restclient;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.axle.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;

@Path("/set_load")
public class LoadGen {

    int concurrency = 0;
    int inFlight    = 0;
    int total       = 0;
    String lastStatus = "<none>";

    @Inject
    Vertx vertx;

    private WebClient client;

    @PostConstruct
    void initialize() {
        client = WebClient.create(vertx,
            new WebClientOptions()
                .setDefaultHost("greeting")
                .setDefaultPort(8080));
    }

    private void sendRequest() {
        inFlight++;
        total++;
        client.get("/hello")
            .send()
            .thenAccept(resp -> {
                lastStatus = resp.statusMessage();
                inFlight--;
                if (inFlight < concurrency) {
                    sendRequest();
                }
            });
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{val}")
    public String setLoad(@PathParam("val") String val) {
        int newVal;
        try {
            newVal = Integer.parseInt(val);
        } catch (Exception e) {
            newVal = concurrency;
        }

        concurrency = newVal;

        while (concurrency > inFlight) {
            sendRequest();
        }

        return String.format("Load set to %d (in-flight: %d, total: %d, last_status: %s)",
            concurrency, inFlight, total, lastStatus);
    }
}

