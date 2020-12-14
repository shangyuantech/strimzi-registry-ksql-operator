package io.strimzi.operator.schemaregistry.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.strimzi.operator.controller.DefaultController;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchemaRegistryCustomController extends DefaultController<SchemaRegistry> {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryCustomController.class);

    public static final String APP_LABEL = "app";

    @Override
    public void reconcile(SchemaRegistry schemaRegistry) {
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
            client.pods().inNamespace(schemaRegistry.getMetadata().getNamespace()).withName(podName).delete();
        }
    }

    private void createPods(int numberOfPods, SchemaRegistry schemaRegistry) {
        for (int index = 0; index < numberOfPods; index++) {
            Pod pod = createNewPod(schemaRegistry);
            client.pods().inNamespace(schemaRegistry.getMetadata().getNamespace()).create(pod);
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

    private Pod createNewPod(SchemaRegistry schemaRegistry) {
        return new PodBuilder()
                .withNewMetadata()
                .withGenerateName(schemaRegistry.getMetadata().getName() + "-pod")
                .withNamespace(schemaRegistry.getMetadata().getNamespace())
                .withLabels(Collections.singletonMap(APP_LABEL, schemaRegistry.getMetadata().getName()))
                .addNewOwnerReference()
                .withController(true)
                .withKind(crdDef.getKind())
                .withApiVersion(crdDef.getVersion())
                .withName(schemaRegistry.getMetadata().getName()).withNewUid(schemaRegistry.getMetadata().getUid())
                .endOwnerReference()
                .endMetadata()
                .withNewSpec()
                .addNewContainer().withName("busybox").withImage("busybox").withCommand("sleep", "3600").endContainer()
                .endSpec()
                .build();
    }
}
