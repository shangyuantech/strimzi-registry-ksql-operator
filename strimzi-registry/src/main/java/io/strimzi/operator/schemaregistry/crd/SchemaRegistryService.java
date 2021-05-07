package io.strimzi.operator.schemaregistry.crd;

import java.util.ArrayList;
import java.util.List;

public class SchemaRegistryService {

    public SchemaRegistryService() {
    }

    public SchemaRegistryService(String type) {
        this.type = type;
    }

    private String type;

    private List<Addresses> addresses = new ArrayList<>();

    public String getType() {
        return type;
    }

    public SchemaRegistryService setType(String type) {
        this.type = type;
        return this;
    }

    public List<Addresses> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Addresses> addresses) {
        this.addresses = addresses;
    }

    public SchemaRegistryService addAddress( String host, Integer port) {
        this.addresses.add(new Addresses(host, port));
        return this;
    }

    public static class Addresses {

        public Addresses() {
        }

        public Addresses(String host, Integer port) {
            this.host = host;
            this.port = port;
        }

        String host;

        Integer port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "Addresses{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SchemaRegistryService{" +
                "type='" + type + '\'' +
                ", addresses=" + addresses +
                '}';
    }
}
