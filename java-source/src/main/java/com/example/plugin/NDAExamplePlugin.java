package com.example.plugin;

import com.example.api.NDAExampleApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.serialization.SerializationWhitelist;
import net.corda.webserver.services.WebServerPluginRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class NDAExamplePlugin  implements WebServerPluginRegistry, SerializationWhitelist {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(NDAExampleApi::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            // This will serve the exampleWeb directory in resources to /web/example
            "NDAexample", getClass().getClassLoader().getResource("exampleWeb").toExternalForm()
    );

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, String> getStaticServeDirs() { return staticServeDirs; }
    @Override public void customizeJSONSerialization(ObjectMapper objectMapper) { }

    @NotNull
    @Override
    public List<Class<?>> getWhitelist() {
        return Arrays.asList(Date.class);
    }
}
