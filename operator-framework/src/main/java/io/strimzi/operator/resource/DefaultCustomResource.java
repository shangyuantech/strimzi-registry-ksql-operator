package io.strimzi.operator.resource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;

public class DefaultCustomResource<T extends DefaultSpecResource, R extends DefaultStatusResource> extends CustomResource {

    private T spec;

    private R status;

    public T getSpec() {
        return spec;
    }

    public void setSpec(T spec) {
        this.spec = spec;
    }

    public R getStatus() {
        return status;
    }

    public void setStatus(R status) {
        this.status = status;
    }

    @Override
    public ObjectMeta getMetadata() {
        return super.getMetadata();
    }
}
