package io.strimzi.operator.schemaregistry.controller;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import io.strimzi.operator.config.OperatorConfig;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SchemaRegistryController {

    private final BlockingQueue<String> workqueue;
    private final SharedIndexInformer<SchemaRegistry> srInformer;
    private final SharedIndexInformer<Pod> podInformer;
    private final SharedIndexInformer<Service> serviceInformer;
    private final Lister<SchemaRegistry> schemaRegistryLister;
    private final Lister<Pod> podLister;
    private final Lister<Service> serviceLister;
    private final KubernetesClient kubernetesClient;
    private final MixedOperation<SchemaRegistry, SchemaRegistryList, DoneableSchemaRegistry, Resource<SchemaRegistry, DoneableSchemaRegistry>> srClient;
    private final OperatorConfig operatorConfig;

    public static final String APP_LABEL = "app";

    public static final Logger logger = LoggerFactory.getLogger(SchemaRegistryController.class.getName());

    public SchemaRegistryController(KubernetesClient kubernetesClient,
                                    MixedOperation<SchemaRegistry, SchemaRegistryList, DoneableSchemaRegistry, Resource<SchemaRegistry, DoneableSchemaRegistry>> schemaRegistryClient,
                                    SharedIndexInformer<Pod> podInformer,
                                    SharedIndexInformer<Service> serviceInformer,
                                    SharedIndexInformer<SchemaRegistry> srInformer,
                                    String namespace, OperatorConfig operatorConfig) {
        this.kubernetesClient = kubernetesClient;
        this.srClient = schemaRegistryClient;
        this.schemaRegistryLister = new Lister<>(srInformer.getIndexer(), namespace);
        this.srInformer = srInformer;
        this.podLister = new Lister<>(podInformer.getIndexer(), namespace);
        this.serviceLister = new Lister<>(serviceInformer.getIndexer(), namespace);
        this.podInformer = podInformer;
        this.serviceInformer = serviceInformer;
        this.workqueue = new ArrayBlockingQueue<>(1024);
        this.operatorConfig = operatorConfig;
    }

    public void create() {

        srInformer.addEventHandler(new ResourceEventHandler<SchemaRegistry>() {
            @Override
            public void onAdd(SchemaRegistry schemaRegistry) {
                enqueueSchemaRegistry(schemaRegistry);
            }

            @Override
            public void onUpdate(SchemaRegistry schemaRegistry, SchemaRegistry newSchemaRegistry) {
                enqueueSchemaRegistry(newSchemaRegistry);
            }

            @Override
            public void onDelete(SchemaRegistry schemaRegistry, boolean b) {
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

        serviceInformer.addEventHandler(new ResourceEventHandler<Service>() {
            @Override
            public void onAdd(Service service) {
                //todo handle service
            }

            @Override
            public void onUpdate(Service oldService, Service newService) {
                if (oldService.getMetadata().getResourceVersion().equals(newService.getMetadata().getResourceVersion())) {
                    return;
                }
                //todo handle service
            }

            @Override
            public void onDelete(Service pod, boolean b) {
                // Do nothing
            }
        });
    }

    private void enqueueSchemaRegistry(SchemaRegistry SchemaRegistry) {
        logger.info("enqueueSchemaRegistry (" + SchemaRegistry.getMetadata().getName() + ")");
        String key = Cache.metaNamespaceKeyFunc(SchemaRegistry);
        logger.info("Going to enqueue key {}", key);

        if (StringUtils.isNoneEmpty(key)) {
            logger.info("Adding item to workqueue");
            workqueue.add(key);
        }
    }

    // ---------------- handle pod ----------------
    private void handlePodObject(Pod pod) {
        logger.debug("handlePodObject({})", pod.getMetadata().getName());
        OwnerReference ownerReference = getControllerOf(pod);
        Objects.requireNonNull(ownerReference);

        if (!ownerReference.getKind().equalsIgnoreCase("SchemaRegistry")) {
            return;
        }

        SchemaRegistry schemaRegistry = schemaRegistryLister.get(ownerReference.getName());
        if (schemaRegistry != null) {
            enqueueSchemaRegistry(schemaRegistry);
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
        logger.info("Starting SchemaRegistry Controller");
        while (!podInformer.hasSynced() || !srInformer.hasSynced() || !serviceInformer.hasSynced()) {
            // Wait till Informer syncs
        }

        while (true) {
            try {
                logger.info("trying to fetch item from work queue...");
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
                SchemaRegistry schemaRegistry = schemaRegistryLister.get(key.split("/")[1]);
                if (schemaRegistry == null) {
                    logger.error(String.format("SchemaRegistry %s in work queue no longer exists", name));
                    return;
                }

                // reconcile SchemaRegistry
                reconcile(schemaRegistry);

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

    private List<String> podCountByLabel(String label, String schemaRegistryName) {
        List<String> podNames = new ArrayList<>();
        List<Pod> pods = podLister.list();

        for (Pod pod : pods) {
            if (pod.getMetadata().getLabels().entrySet().contains(new AbstractMap.SimpleEntry<>(label, schemaRegistryName))) {
                if (pod.getStatus().getPhase().equals("Running") || pod.getStatus().getPhase().equals("Pending")) {
                    podNames.add(pod.getMetadata().getName());
                }
            }
        }

        logger.info("count: {}", podNames.size());
        return podNames;
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
}
