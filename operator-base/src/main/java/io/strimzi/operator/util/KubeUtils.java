package io.strimzi.operator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.operator.model.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubeUtils {

    private static final Logger log = LoggerFactory.getLogger(KubeUtils.class);

    private final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static String getYAML(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    /**
     * show resource to yaml
     */
    public static void showYAML(Object obj) {
        if (log.isDebugEnabled()) {
            try {
                log.debug("show {} yaml = \n{}", obj.getClass().getName(), getYAML(obj));
            } catch (JsonProcessingException e) {
                log.debug("show {} string = \n{}", obj.getClass().getName(), obj.toString());
            }
        }
    }

    /**
     * merge deployment
     */
    public static void mergeDeploymentResource(KubernetesClient kubernetesClient, Deployment deployment) {
        String namespace = deployment.getMetadata().getNamespace();

        log.info("Creating or updating deployment {} in {}", deployment.getMetadata().getName(), namespace);
        showYAML(deployment);

        // TODO In the future, a method to identify different deployment may be added.
        // If it is the same, it will not be replace. We haven't done it for the time being
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
            log.info("Creating service {} in {}", service.getMetadata().getName(), namespace);
            showYAML(service);
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
                        } else if (!newPort.getNodePort().equals(oldPort.getNodePort())) {
                            change = true;
                        }
                        break;
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
                showYAML(service);
                kubernetesClient.services().inNamespace(namespace).createOrReplace(service);
            } else {
                log.info("Service {} in {} may be not changed, passing replaced", service.getMetadata().getName(), namespace);
            }
        }
    }
}
