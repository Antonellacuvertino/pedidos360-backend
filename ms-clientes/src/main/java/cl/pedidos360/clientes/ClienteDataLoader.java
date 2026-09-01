package cl.pedidos360.clientes;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClienteDataLoader {
    @Bean
    CommandLineRunner cargarClientes(ClienteRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Cliente("Comercial Norte SPA", "contacto@comercialnorte.cl", "Empresa"));
                repository.save(new Cliente("Maria Gonzalez", "maria.gonzalez@example.com", "Retail"));
                repository.save(new Cliente("Servicios Andes", "compras@andes.cl", "Empresa"));
            }
        };
    }
}
