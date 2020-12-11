package io.strimzi.operator.schemaregistry.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableSchemaRegistry extends CustomResourceDoneable<SchemaRegistry> {

    public DoneableSchemaRegistry(SchemaRegistry resource, Function function) {
        super(resource, function);
    }
}
