package io.automatiko.addon.usertasks.index;

import java.util.List;
import java.util.Map;

public interface CustomQueryBuilder<QueryType> {

    String id();

    QueryType build(Map<String, List<String>> parameters);
}
