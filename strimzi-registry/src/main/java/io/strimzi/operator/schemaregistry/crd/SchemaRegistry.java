package io.strimzi.operator.schemaregistry.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("kafka.strimzi.io")
@Version("v1")
public class SchemaRegistry extends CustomResource<SchemaRegistrySpec, SchemaRegistryStatus>
        implements Namespaced {

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
