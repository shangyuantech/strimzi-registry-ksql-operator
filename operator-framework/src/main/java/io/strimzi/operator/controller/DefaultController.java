package io.strimzi.operator.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import io.strimzi.operator.config.OperatorConfig;
import io.strimzi.operator.resource.CRDDef;
import io.strimzi.operator.resource.Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public abstract class DefaultController<T extends HasMetadata> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);

    protected String namespace;
    protected BlockingQueue<Request> workQueue = new ArrayBlockingQueue<>(1024);
    protected SharedIndexInformer<T> tInformer;
    protected SharedIndexInformer<Pod> podInformer;
    protected Lister<T> tLister;
    protected Lister<Pod> podLister;
    protected KubernetesClient client;
    protected CRDDef crdDef;
    protected OperatorConfig operatorConfig;

    public DefaultController() {
        this.operatorConfig = OperatorConfig.fromMap(System.getenv());
    }

    public SharedIndexInformer<T> gettInformer() {
        return tInformer;
    }

    public void settInformer(SharedIndexInformer<T> tInformer) {
        this.tInformer = tInformer;
    }

    public KubernetesClient getKubernetesClient() {
        return client;
    }

    public void setKubernetesClient(KubernetesClient client) {
        this.client = client;
    }

    public CRDDef getCrdDef() {
        return crdDef;
    }

    public void setCrdDef(CRDDef crdDef) {
        this.crdDef = crdDef;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public SharedIndexInformer<Pod> getPodInformer() {
        return podInformer;
    }

    public void setPodInformer(SharedIndexInformer<Pod> podInformer) {
        this.podInformer = podInformer;
    }

    public void create() {
        this.tLister = new Lister<>(tInformer.getIndexer(), namespace);
        this.podLister = new Lister<>(podInformer.getIndexer(), namespace);

        tInformer.addEventHandler(new ResourceEventHandler<T>() {
            @Override
            public void onAdd(T t) {
                enqueueResource(t, "add");
            }

            @Override
            public void onUpdate(T t, T newt) {
                enqueueResource(newt, "update");
            }

            @Override
            public void onDelete(T t, boolean b) {
                // Do nothing
            }
        });

        podInformer.addEventHandler(new ResourceEventHandler<Pod>() {
            @Override
            public void onAdd(Pod pod) {
                handlePodObject(pod, "add");
            }

            @Override
            public void onUpdate(Pod oldPod, Pod newPod) {
                if (oldPod.getMetadata().getResourceVersion().equals(newPod.getMetadata().getResourceVersion())) {
                    return;
                }
                handlePodObject(newPod, "update");
            }

            @Override
            public void onDelete(Pod pod, boolean b) {
                // Do nothing
            }
        });
    }

    private void enqueueResource(T dcr, String method) {
        logger.info("enqueue resource {} with {}", dcr.getMetadata().getName(), method);
        String key = Cache.metaNamespaceKeyFunc(dcr);
        logger.info("Going to enqueue key {}", key);

        if (StringUtils.isNoneEmpty(key)) {
            workQueue.add(new Request(dcr.getMetadata().getNamespace(), dcr.getMetadata().getName()));
        }
    }

    // ---------------- handle pod ----------------
    private void handlePodObject(Pod pod, String method) {
        logger.debug("handlePodObject({})", pod.getMetadata().getName());
        OwnerReference ownerReference = getControllerOf(pod);
        Objects.requireNonNull(ownerReference);

        if (!ownerReference.getKind().equalsIgnoreCase("SchemaRegistry")) {
            return;
        }

        T t = tLister.get(ownerReference.getName());
        if (t != null) {
            enqueueResource(t, method);
        }
    }

    private OwnerReference getControllerOf(Pod pod) {
        List<OwnerReference> ownerReferences = pod.getMetadata().getOwnerReferences();
        for (OwnerReference ownerReference : ownerReferences) {
            if (ownerReference.getController().equals(Boolean.TRUE)) {
                return ownerReference;
            }
        }
        return null;
    }

    public void run() {
        logger.info("Starting {} ...", this.getClass());
        while (!podInformer.hasSynced() || !tInformer.hasSynced()) {
            // Wait till Informer syncs
        }

        while (true) {
            try {
                logger.info("trying to fetch item from work queue...");
                if (workQueue.isEmpty()) {
                    logger.info("Work Queue is empty");
                }

                Request request = workQueue.take();
                String name = request.getName();
                Objects.requireNonNull(name, "name can't be null");
                logger.info(String.format("Got %s", name));

                T crds = tLister.get(name);
                if (crds == null) {
                    logger.error("{} in work queue no longer exists", name);
                    return;
                }

                reconcile(crds);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                logger.error("controller interrupted..");
            }
        }
    }

    public abstract void reconcile(T t);
}
