package cl.pedidos360.messaging;

import java.util.Set;

public final class RabbitNames {
    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "dlx.exchange";

    public static final String ORDERS_QUEUE = "orders.queue";
    public static final String NOTIFICATIONS_QUEUE = "notifications.queue";
    public static final String STOCK_QUEUE = "stock.queue";
    public static final String AUDIT_QUEUE = "audit.queue";

    public static final String ORDERS_DLQ = "orders.dlq";
    public static final String NOTIFICATIONS_DLQ = "notifications.dlq";
    public static final String STOCK_DLQ = "stock.dlq";
    public static final String AUDIT_DLQ = "audit.dlq";

    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String NOTIFICATION_EMAIL_KEY = "notification.email";

    public static final Set<String> PROTECTED_QUEUES = Set.of(
            ORDERS_QUEUE, NOTIFICATIONS_QUEUE, STOCK_QUEUE, AUDIT_QUEUE,
            ORDERS_DLQ, NOTIFICATIONS_DLQ, STOCK_DLQ, AUDIT_DLQ);

    private RabbitNames() {
    }
}
