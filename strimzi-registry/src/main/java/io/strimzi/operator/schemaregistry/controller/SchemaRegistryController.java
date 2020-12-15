package io.strimzi.operator.schemaregistry.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.*;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@Controller(crdName = "schemaregistries.kafka.strimzi.io")
public class SchemaRegistryController implements ResourceController<SchemaRegistry> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    public SchemaRegistryController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public DeleteControl deleteResource(SchemaRegistry schemaRegistry, Context<SchemaRegistry> context) {
        return null;
    }

    @Override
    public UpdateControl<SchemaRegistry> createOrUpdateResource(SchemaRegistry schemaRegistry, Context<SchemaRegistry> context) {
        log.debug("update {}", schemaRegistry);
        return null;
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = getClass().getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}
