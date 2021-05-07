package io.strimzi.operator.schemaregistry.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class SchemaRegistryStatus {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer availableReplicas;

    public Integer getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(Integer availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<SchemaRegistryService> schemaRegistryService;

    public List<SchemaRegistryService> getSchemaRegistryService() {
        return schemaRegistryService;
    }

    public void setSchemaRegistryService(List<SchemaRegistryService> schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorMsg;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String[] stackTrace;

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String[] getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        return "SchemaRegistryStatus {" + "\n" +
                "   availableReplicas = " + availableReplicas +
                ",schemaRegistryService = '" + schemaRegistryService + "'\n" +
                "   errorMsg = '" + errorMsg + "'\n" +
                "   stackTrace = '" + ArrayUtils.toString(stackTrace) + '}';
    }
}
