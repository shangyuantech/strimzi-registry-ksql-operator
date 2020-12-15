package io.strimzi.operator.util;

import io.fabric8.kubernetes.api.model.EnvVar;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class EnvUtils {

    /**
     * transform kafka properties key to env key
     */
    public static String transformEnv(String kafkaPropsKey) {
        String[] splits = kafkaPropsKey.split("\\.");
        StringBuilder transKey = new StringBuilder();

        for (String split : splits) {
            transKey.append("_").append(split.toUpperCase());
        }
        return transKey.substring(1);
    }

    /**
     * print env vars to multiply lines string
     */
    public static String toPrintString(List<EnvVar> envVars) {
        StringBuilder print = new StringBuilder();
        if (CollectionUtils.isEmpty(envVars)) {
            return "[]";
        }

        for (EnvVar envVar : envVars) {
            print.append(String.format("- name: %s, value: %s", envVar.getName(), envVar.getValue()));
            if (envVar.getValueFrom() != null) {
                print.append(", value from: ").append(envVar.getValueFrom());
            }
            print.append("\n");
        }

        return print.toString();
    }
}
