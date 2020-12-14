package io.strimzi.operator.schemaregistry;

import io.strimzi.operator.Operator;
import io.strimzi.operator.resource.CRDDef;
import io.strimzi.operator.schemaregistry.controller.SchemaRegistryCustomController;
import io.strimzi.operator.schemaregistry.crd.DoneableSchemaRegistry;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistry;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistryList;

public class SchemaRegistryOperatorMain {

    public static void main(String[] args) {
        CRDDef crdDef = new CRDDef("SchemaRegistry", "schemaregistries");

        Operator<SchemaRegistry, SchemaRegistryList, DoneableSchemaRegistry> operator
                = new Operator<>(crdDef, SchemaRegistry.class, SchemaRegistryList.class, DoneableSchemaRegistry.class);
        operator.registryOperator(new SchemaRegistryCustomController());
    }
}
