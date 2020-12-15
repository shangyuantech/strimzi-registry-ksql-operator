package io.strimzi.operator.schemaregistry.crd;

public class SchemaRegistryStatus {

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
