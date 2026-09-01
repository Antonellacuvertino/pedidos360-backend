package cl.pedidos360.productos;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProductoDataLoader {
    @Bean
    CommandLineRunner cargarProductos(ProductoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Producto("Notebook Lenovo ThinkPad", "Computacion", BigDecimal.valueOf(749990), 12));
                repository.save(new Producto("Monitor Samsung 27", "Perifericos", BigDecimal.valueOf(189990), 18));
                repository.save(new Producto("Mouse Logitech MX", "Perifericos", BigDecimal.valueOf(69990), 30));
            }
        };
    }
}
