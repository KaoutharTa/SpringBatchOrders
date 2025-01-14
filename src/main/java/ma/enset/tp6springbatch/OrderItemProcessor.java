package ma.enset.tp6springbatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class OrderItemProcessor implements ItemProcessor<Order, Order> {
    private static final Logger log = LoggerFactory.getLogger(OrderItemProcessor.class);
    private static final double DISCOUNT = 0.10; // 10% discount

    @Override
    public Order process(Order order) {
        final Double originalAmount = order.getAmount();
        final Double discountedAmount = originalAmount * (1 - DISCOUNT);
        order.setAmount(discountedAmount);

        log.info("Processing order: {} -> Discounted amount: {}", order.getOrderId(), discountedAmount);
        return order;
    }
}
