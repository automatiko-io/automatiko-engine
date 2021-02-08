package org.acme.travels;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ElasticContainer implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticContainer.class);
    private GenericContainer<?> elastic;

    @Override
    public Map<String, String> start() {
        elastic = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch-oss").withTag("7.10.2"));
        elastic.start();
        System.out.println(elastic.getHost());
        System.out.println(elastic.getFirstMappedPort());
        return Collections.singletonMap("quarkus.elasticsearch.hosts", elastic.getHost() + ":" + elastic.getFirstMappedPort());

    }

    @Override
    public void stop() {
        elastic.stop();
    }

}
