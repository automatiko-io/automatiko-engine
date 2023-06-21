package com.acme.usertasks;

import static com.googlecode.cqengine.query.QueryFactory.in;

import java.util.List;
import java.util.Map;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;

import io.automatiko.addons.usertasks.index.fs.CQEngineBasedIndex;
import io.automatiko.addons.usertasks.index.fs.CQEngineCustomQueryBuilder;
import io.automatiko.addons.usertasks.index.fs.CQEngineUserTaskInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ByProcessCustomQueryBuilder extends CQEngineCustomQueryBuilder {

    public static final SimpleAttribute<CQEngineUserTaskInfo, String> PROCESS_ID = new SimpleAttribute<>("processId") {
        public String getValue(CQEngineUserTaskInfo task, QueryOptions queryOptions) {
            return task.getProcessId();
        }
    };

    public ByProcessCustomQueryBuilder() {
    }

    @Inject
    public ByProcessCustomQueryBuilder(CQEngineBasedIndex index) {
        index.get().addIndex(SuffixTreeIndex.onAttribute(PROCESS_ID));
    }

    @Override
    public Query<CQEngineUserTaskInfo> build(Map<String, List<String>> parameters) {

        return in(PROCESS_ID, parameters.get("pid"));
    }

    @Override
    public String id() {
        return "byprocess";
    }

}
