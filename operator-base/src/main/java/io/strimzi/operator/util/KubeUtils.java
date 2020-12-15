package io.strimzi.operator.util;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.operator.model.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubeUtils {

    private static final Logger log = LoggerFactory.getLogger(KubeUtils.class);

    /**
     * merge deployment
     */
    public static void mergeDeploymentResource(KubernetesClient kubernetesClient, Deployment deployment) {
        String namespace = deployment.getMetadata().getNamespace();
        // TODO In the future, a method to identify different services may be added.
        // If it is the same, it will not be created. We haven't done it for the time being
        log.info("Creating or updating Deployment {} in {}", deployment.getMetadata().getName(), namespace);
        kubernetesClient.apps().deployments().inNamespace(namespace).createOrReplace(deployment);
    }

    /**
     * merge service
     */
    public static void mergeServiceResource(KubernetesClient kubernetesClient, Service service) {
        String namespace = service.getMetadata().getNamespace();
        Service old = kubernetesClient.services().inNamespace(namespace)
                .withName(service.getMetadata().getName()).get();

        if (old == null) {
            log.info("Creating Service {} in {}", service.getMetadata().getName(), namespace);
            kubernetesClient.services().inNamespace(namespace).create(service);
        } else {
            ServiceType serviceType = ServiceType.valueOf(service.getSpec().getType());
            ServicePort newPort = service.getSpec().getPorts().get(0);
            ServicePort oldPort = old.getSpec().getPorts().get(0);
            boolean change = false;
            if (!ValidationUtils.isObjectSame(service.getSpec().getType(), old.getSpec().getType())) {
                change = true;
            } else if (old.getSpec().getPorts().size() != 1) {
                change = true;
            } else {
                switch (serviceType) {
                    case LoadBalancer:
                        if (!ValidationUtils.isObjectSame(service.getSpec().getLoadBalancerIP(),
                                old.getSpec().getLoadBalancerIP())) {
                            change = true;
                        } else if (!ValidationUtils.isObjectSame(service.getSpec().getLoadBalancerSourceRanges(),
                                old.getSpec().getLoadBalancerSourceRanges())) {
                            change = true;
                        }
                    case NodePort:
                        if (!newPort.getNodePort().equals(oldPort.getNodePort())) {
                            change = true;
                        }
                        break;
                    default:
                        if (!newPort.getPort().equals(oldPort.getPort())) {
                            change = true;
                        } else if (!newPort.getTargetPort().equals(oldPort.getTargetPort())) {
                            change = true;
                        } else if (!newPort.getProtocol().equals(oldPort.getProtocol())) {
                            change = true;
                        }
                        break;
                }
            }

            if (change) {
                log.info("Service {} in {} have been exists, need to be replaced", service.getMetadata().getName(), namespace);
            } else {
                log.info("Service {} in {} may be not changed, passing replaced", service.getMetadata().getName(), namespace);
            }
        }
    }
}
