package org.acme;

import java.util.List;
import java.util.Map;

import io.automatiko.addons.usertasks.index.db.DbCustomQueryBuilder;
import io.automatiko.addons.usertasks.index.db.DbQueryFilter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ByProcessIdDbCustomQueryBuilder extends DbCustomQueryBuilder {

    @Override
    public DbQueryFilter build(Map<String, List<String>> parameters) {

        return new DbQueryFilter("t.processId = :process", Map.of("process", parameters.get("process").get(0)));
    }

    @Override
    public String id() {
        return "byProcess";
    }

}
