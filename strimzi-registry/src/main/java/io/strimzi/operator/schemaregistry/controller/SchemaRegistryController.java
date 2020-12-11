package io.strimzi.operator.schemaregistry.controller;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import io.strimzi.operator.schemaregistry.crd.DoneableSchemaRegistry;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistry;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistryList;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SchemaRegistryController {

    private final BlockingQueue<String> workqueue;
    private final SharedIndexInformer<SchemaRegistry> srInformer;
    private final SharedIndexInformer<Pod> podInformer;
    private final Lister<SchemaRegistry> SchemaRegistryLister;
    private final Lister<Pod> podLister;
    private final KubernetesClient kubernetesClient;
    private final MixedOperation<SchemaRegistry, SchemaRegistryList, DoneableSchemaRegistry, Resource<SchemaRegistry, DoneableSchemaRegistry>> srClient;
    public static final Logger logger = LoggerFactory.getLogger(SchemaRegistryController.class.getName());
    public static final String APP_LABEL = "app";

    public SchemaRegistryController(KubernetesClient kubernetesClient, 
                            MixedOperation<SchemaRegistry, SchemaRegistryList, DoneableSchemaRegistry, Resource<SchemaRegistry, DoneableSchemaRegistry>> SchemaRegistryClient, 
                            SharedIndexInformer<Pod> podInformer, SharedIndexInformer<SchemaRegistry> srInformer, 
                            String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.srClient = SchemaRegistryClient;
        this.SchemaRegistryLister = new Lister<>(srInformer.getIndexer(), namespace);
        this.srInformer = srInformer;
        this.podLister = new Lister<>(podInformer.getIndexer(), namespace);
        this.podInformer = podInformer;
        this.workqueue = new ArrayBlockingQueue<>(1024);
    }

    public void create() {
        srInformer.addEventHandler(new ResourceEventHandler<SchemaRegistry>() {
            @Override
            public void onAdd(SchemaRegistry SchemaRegistry) {
                enqueueSchemaRegistry(SchemaRegistry);
            }

            @Override
            public void onUpdate(SchemaRegistry SchemaRegistry, SchemaRegistry newSchemaRegistry) {
                enqueueSchemaRegistry(newSchemaRegistry);
            }

            @Override
            public void onDelete(SchemaRegistry SchemaRegistry, boolean b) {
                // Do nothing
            }
        });

        podInformer.addEventHandler(new ResourceEventHandler<Pod>() {
            @Override
            public void onAdd(Pod pod) {
                handlePodObject(pod);
            }

            @Override
            public void onUpdate(Pod oldPod, Pod newPod) {
                if (oldPod.getMetadata().getResourceVersion().equals(newPod.getMetadata().getResourceVersion())) {
                    return;
                }
                handlePodObject(newPod);
            }

            @Override
            public void onDelete(Pod pod, boolean b) {
                // Do nothing
            }
        });
    }

    public void run() {
        logger.info( "Starting SchemaRegistry controller");
        while (!podInformer.hasSynced() || !srInformer.hasSynced()) {
            // Wait till Informer syncs
        }

        while (true) {
            try {
                logger.info("trying to fetch item from workqueue...");
                if (workqueue.isEmpty()) {
                    logger.info("Work Queue is empty");
                }
                String key = workqueue.take();
                Objects.requireNonNull(key, "key can't be null");
                logger.info(String.format("Got %s", key));
                if (key.isEmpty() || (!key.contains("/"))) {
                    logger.warn(String.format("invalid resource key: %s", key));
                }

                // Get the SchemaRegistry resource's name from key which is in format namespace/name
                String name = key.split("/")[1];
                SchemaRegistry SchemaRegistry = SchemaRegistryLister.get(key.split("/")[1]);
                if (SchemaRegistry == null) {
                    logger.error(String.format("SchemaRegistry %s in workqueue no longer exists", name));
                    return;
                }
                reconcile(SchemaRegistry);

            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                logger.error("controller interrupted..");
            }
        }
    }

    /**
     * Tries to achieve the desired state for SchemaRegistry.
     *
     * @param schemaRegistry specified SchemaRegistry
     */
    protected void reconcile(SchemaRegistry schemaRegistry) {
        List<String> pods = podCountByLabel(APP_LABEL, schemaRegistry.getMetadata().getName());
        if (pods.isEmpty()) {
            createPods(schemaRegistry.getSpec().getReplicas(), schemaRegistry);
            return;
        }
        int existingPods = pods.size();

        // Compare it with desired state i.e spec.replicas
        // if less then spin up pods
        if (existingPods < schemaRegistry.getSpec().getReplicas()) {
            createPods(schemaRegistry.getSpec().getReplicas() - existingPods, schemaRegistry);
        }

        // If more pods then delete the pods
        int diff = existingPods - schemaRegistry.getSpec().getReplicas();
        for (; diff > 0; diff--) {
            String podName = pods.remove(0);
            kubernetesClient.pods().inNamespace(schemaRegistry.getMetadata().getNamespace()).withName(podName).delete();
        }
    }

    private void createPods(int numberOfPods, SchemaRegistry SchemaRegistry) {
        for (int index = 0; index < numberOfPods; index++) {
            Pod pod = createNewPod(SchemaRegistry);
            kubernetesClient.pods().inNamespace(SchemaRegistry.getMetadata().getNamespace()).create(pod);
        }
    }

    private List<String> podCountByLabel(String label, String SchemaRegistryName) {
        List<String> podNames = new ArrayList<>();
        List<Pod> pods = podLister.list();

        for (Pod pod : pods) {
            if (pod.getMetadata().getLabels().entrySet().contains(new AbstractMap.SimpleEntry<>(label, SchemaRegistryName))) {
                if (pod.getStatus().getPhase().equals("Running") || pod.getStatus().getPhase().equals("Pending")) {
                    podNames.add(pod.getMetadata().getName());
                }
            }
        }

        logger.info(String.format("count: %d", podNames.size()));
        return podNames;
    }

    private void enqueueSchemaRegistry(SchemaRegistry SchemaRegistry) {
        logger.info("enqueueSchemaRegistry(" + SchemaRegistry.getMetadata().getName() + ")");
        String key = Cache.metaNamespaceKeyFunc(SchemaRegistry);
        logger.info(String.format("Going to enqueue key %s", key));
        if (key != null && !key.isEmpty()) {
            logger.info("Adding item to workqueue");
            workqueue.add(key);
        }
    }

    private void handlePodObject(Pod pod) {
        logger.info("handlePodObject(" + pod.getMetadata().getName() + ")");
        OwnerReference ownerReference = getControllerOf(pod);
        Objects.requireNonNull(ownerReference);
        if (!ownerReference.getKind().equalsIgnoreCase("SchemaRegistry")) {
            return;
        }
        SchemaRegistry SchemaRegistry = SchemaRegistryLister.get(ownerReference.getName());
        if (SchemaRegistry != null) {
            enqueueSchemaRegistry(SchemaRegistry);
        }
    }

    private Pod createNewPod(SchemaRegistry SchemaRegistry) {
        return new PodBuilder()
                .withNewMetadata()
                .withGenerateName(SchemaRegistry.getMetadata().getName() + "-pod")
                .withNamespace(SchemaRegistry.getMetadata().getNamespace())
                .withLabels(Collections.singletonMap(APP_LABEL, SchemaRegistry.getMetadata().getName()))
                .addNewOwnerReference()
                .withController(true)
                .withKind("SchemaRegistry")
                .withApiVersion("kafka.strimzi.io")
                .withName(SchemaRegistry.getMetadata().getName()).withNewUid(SchemaRegistry.getMetadata().getUid())
                .endOwnerReference()
                .endMetadata()
                .withNewSpec()
                .addNewContainer().withName("busybox").withImage("busybox").withCommand("sleep", "3600").endContainer()
                .endSpec()
                .build();
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
}
