package io.strimzi.operator;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.strimzi.operator.controller.DefaultController;
import io.strimzi.operator.resource.CRDDef;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operator<T extends HasMetadata, L extends KubernetesResourceList<T>, D extends Doneable<T>> {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);

    private static final String defaultNamespace = "default";

    private final KubernetesClient client;

    private final CRDDef crdDef;

    private final String namespace;

    private final CustomResourceDefinitionContext context;

    private final SharedInformerFactory informerFactory;

    private final MixedOperation<T, L, D, Resource<T, D>> mixedOperation;

    private final SharedIndexInformer<T> tSharedIndexInformer;

    private final SharedIndexInformer<Pod> podSharedIndexInformer;

    public Operator(CRDDef crdDef, Class<T> t, Class<L> l, Class<D> d) throws KubernetesClientException {
        this.client = new DefaultKubernetesClient();
        String namespace = client.getNamespace();
        if (StringUtils.isEmpty(namespace)) {
            logger.warn("No namespace found via config, assuming default.");
            this.namespace = defaultNamespace;
        } else {
            this.namespace = namespace;
        }
        logger.info("Using namespace : " + this.namespace);

        this.crdDef = crdDef;
        this.context = new CustomResourceDefinitionContext.Builder()
                        .withVersion(crdDef.getVersion())
                        .withScope(crdDef.getScope())
                        .withGroup(crdDef.getGroup())
                        .withPlural(crdDef.getPlural())
                        .build();
        this.mixedOperation = client.customResources(context, t, l, d);
        this.informerFactory = client.informers();
        this.tSharedIndexInformer = informerFactory
                .sharedIndexInformerForCustomResource(context, t, l, 10 * 60 * 1000);
        this.podSharedIndexInformer = informerFactory
                .sharedIndexInformerFor(Pod.class, PodList.class, 10 * 60 * 1000);
    }

    public void registryOperator(DefaultController<T> controller) {
        controller.setNamespace(namespace);
        controller.setKubernetesClient(client);
        controller.setMixedOperation(mixedOperation);
        controller.settInformer(tSharedIndexInformer);
        controller.setPodInformer(podSharedIndexInformer);
        controller.setCrdDef(crdDef);
        controller.create();

        informerFactory.startAllRegisteredInformers();
        informerFactory.addSharedInformerEventListener(exception ->
                logger.error("Exception occurred, but caught", exception));

        controller.run();
    }
}
