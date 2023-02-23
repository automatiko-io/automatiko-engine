package org.acme.orders;

import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;

import org.acme.orders.demo.Order;

@ApplicationScoped
public class CalculationService {

    private Random random = new Random();
    
    public Order calculateTotal(Order order) {        
        order.setTotal(random.nextDouble());
        
        return order;
    }
}
