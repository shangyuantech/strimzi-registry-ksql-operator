package io.strimzi.operator.schemaregistry.crd;

import io.fabric8.kubernetes.client.CustomResource;

public class SchemaRegistry extends CustomResource {

    private SchemaRegistrySpec spec;

    private SchemaRegistryStatus status;

    public SchemaRegistrySpec getSpec() {
        return spec;
    }

    public void setSpec(SchemaRegistrySpec spec) {
        this.spec = spec;
    }

    public SchemaRegistryStatus getStatus() {
        return status;
    }

    public void setStatus(SchemaRegistryStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SchemaRegistry {" + "\n" +
                "spec = " + spec + "\n" +
                "status = " + status +
                "} \n"
                + super.toString();
    }
}
