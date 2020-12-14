package io.strimzi.operator.schemaregistry.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.strimzi.operator.resource.DefaultStatusResource;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class SchemaRegistryStatus extends DefaultStatusResource {

    private int availableReplicas;

    public int getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(int availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    @Override
    public String toString() {
        return "SchemaRegistryStatus{" +
                "availableReplicas=" + availableReplicas +
                '}';
    }
}
