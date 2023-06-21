package io.automatiko.addons.usertasks.index.db;

import java.util.Map;

public class DbQueryFilter {

    private final String queryFilter;

    private final Map<String, Object> parameters;

    public DbQueryFilter(String queryFilter, Map<String, Object> parameters) {
        this.queryFilter = queryFilter;
        this.parameters = parameters;
    }

    public String queryFilter() {
        return queryFilter;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

}
