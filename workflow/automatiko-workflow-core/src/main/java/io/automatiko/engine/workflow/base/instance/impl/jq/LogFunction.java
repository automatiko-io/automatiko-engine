package io.automatiko.engine.workflow.base.instance.impl.jq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction({ "log/0", "log/1", "log/2", "log/3", "log/4", "log/5", "log/6", "log/7", "log/8", "log/9", "log/10" })
public class LogFunction implements Function {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogFunction.class);

    @Override
    public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {

        if (args.size() > 0) {
            List<Object> toLog = new ArrayList<>();
            StringBuilder pattern = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {

                try {
                    List<JsonNode> results = args.get(i).apply(scope, in);
                    if (results.size() == 0) {
                        pattern.append("{} ");
                        toLog.add(null);
                    } else if (results.size() == 1) {
                        pattern.append("{} ");
                        toLog.add(results.get(0).textValue());
                    } else {
                        for (JsonNode result : results) {
                            pattern.append("{} ");
                            toLog.add(result.textValue());
                        }
                    }

                } catch (JsonQueryException e) {
                    pattern.append("{} ");
                    toLog.add(in.get(args.get(i).toString()));
                }
            }

            LOGGER.info(pattern.toString(), toLog.toArray());
        } else {

            LOGGER.info(in.toPrettyString());
        }
        return Collections.emptyList();
    }

}
