package io.strimzi.operator.schemaregistry.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class SchemaRegistrySpec implements KubernetesResource {
    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    private int replicas;

    @Override
    public String toString() {
        return "SchemaRegistrySpec{" +
                "replicas=" + replicas +
                '}';
    }
}
