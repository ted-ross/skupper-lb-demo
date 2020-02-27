package io.skupper;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("/hello")
public class ThrottleService {

    @Inject
    Vertx vertx;

    private int ratePerTenth;
    private String hostname  = System.getenv("HOSTNAME");
    private int sendSlots    = 1;
    private int count        = 0;
    private List<CompletableFuture<String>> pendingFutures   = new ArrayList<CompletableFuture<String>>();
    private List<String>                    pendingResponses = new ArrayList<String>();
    
    public void SetupTimer() {
        try {
            ratePerTenth = Integer.parseInt(System.getenv("RATE_LIMIT"));
        } catch (NumberFormatException e) {
            ratePerTenth = 1;
        }
        vertx.setPeriodic(100, l -> {
            sendSlots = ratePerTenth;
            while (sendSlots > 0 && !pendingResponses.isEmpty()) {
                sendSlots--;
                String                    response = pendingResponses.remove(0);
                CompletableFuture<String> future   = pendingFutures.remove(0);
                future.complete(response);
            }
        });
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> hello() {
        if (count == 0)
            SetupTimer();

        count++;
        String response = String.format("Hello from %s, count=%d", hostname, count);
        CompletableFuture<String> future = new CompletableFuture<>();

        if (sendSlots > 0) {
            sendSlots--;
            future.complete(response);
        } else {
            pendingResponses.add(response);
            pendingFutures.add(future);
        }

        return future;
    }
}
