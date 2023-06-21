package org.acme.orders;

import java.util.Random;

import org.acme.orders.demo.Order;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CalculationService {

    private Random random = new Random();

    public Order calculateTotal(Order order) {
        order.setTotal(random.nextDouble(0.2, 0.5));

        return order;
    }
}
