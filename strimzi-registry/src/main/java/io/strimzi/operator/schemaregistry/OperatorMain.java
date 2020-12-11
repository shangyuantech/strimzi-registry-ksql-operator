package io.strimzi.operator.schemaregistry;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.strimzi.operator.schemaregistry.controller.SchemaRegistryController;
import io.strimzi.operator.schemaregistry.crd.DoneableSchemaRegistry;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistry;
import io.strimzi.operator.schemaregistry.crd.SchemaRegistryList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperatorMain {

    public static final Logger logger = LoggerFactory.getLogger(OperatorMain.class.getName());

    public static void main(String args[]) throws InterruptedException {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            String namespace = client.getNamespace();
            if (namespace == null) {
                logger.warn("No namespace found via config, assuming default.");
                namespace = "default";
            }

            logger.info("Using namespace : " + namespace);
            CustomResourceDefinitionContext SchemaRegistryCustomResourceDefinitionContext = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1beta1")
                    .withScope("Namespaced")
                    .withGroup("kafka.strimzi.io")
                    .withPlural("schemaregistries")
                    .build();

            SharedInformerFactory informerFactory = client.informers();

            MixedOperation<SchemaRegistry, SchemaRegistryList, DoneableSchemaRegistry, Resource<SchemaRegistry, DoneableSchemaRegistry>>
                    SchemaRegistryClient = client.customResources(
                            SchemaRegistryCustomResourceDefinitionContext, SchemaRegistry.class, SchemaRegistryList.class,
                            DoneableSchemaRegistry.class);

            SharedIndexInformer<Pod> podSharedIndexInformer = informerFactory
                    .sharedIndexInformerFor(Pod.class, PodList.class, 10 * 60 * 1000);
            SharedIndexInformer<SchemaRegistry> SchemaRegistrySharedIndexInformer = informerFactory
                    .sharedIndexInformerForCustomResource(
                            SchemaRegistryCustomResourceDefinitionContext, SchemaRegistry.class,
                            SchemaRegistryList.class, 10 * 60 * 1000);

            SchemaRegistryController SchemaRegistryController = new SchemaRegistryController(
                    client, SchemaRegistryClient, podSharedIndexInformer,
                    SchemaRegistrySharedIndexInformer, namespace);

            SchemaRegistryController.create();
            informerFactory.startAllRegisteredInformers();
            informerFactory.addSharedInformerEventListener(exception ->
                    logger.error("Exception occurred, but caught", exception));

            SchemaRegistryController.run();
        } catch (KubernetesClientException exception) {
            logger.error( "Kubernetes Client Exception : {}", exception.getMessage());
        }
    }
}
