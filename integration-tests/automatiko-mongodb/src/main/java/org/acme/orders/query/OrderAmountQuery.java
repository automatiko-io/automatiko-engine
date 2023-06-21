package org.acme.orders.query;

import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.automatiko.addons.usertasks.index.mongo.MongoDBCustomQueryBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderAmountQuery extends MongoDBCustomQueryBuilder {

    @Override
    public Bson build(Map<String, List<String>> parameters) {
        Bson filter = Filters.and(Filters.eq("processId", "orderItems"),
                Filters.gt("inputs.order.total", Double.parseDouble(parameters.get("amount").get(0))));
        return filter;
    }

    @Override
    public String id() {
        return "orderAmount";
    }

}
