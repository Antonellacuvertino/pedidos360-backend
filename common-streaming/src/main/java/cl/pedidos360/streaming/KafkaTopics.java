package cl.pedidos360.streaming;

public final class KafkaTopics {
    public static final String PAYMENT_ATTEMPTS = "payment-attempts";
    public static final String PRODUCT_VIEWS = "product-views";
    public static final String PAYMENT_ATTEMPTS_DLT = "payment-attempts-dlt";
    public static final String PRODUCT_VIEWS_DLT = "product-views-dlt";

    private KafkaTopics() {
    }
}
