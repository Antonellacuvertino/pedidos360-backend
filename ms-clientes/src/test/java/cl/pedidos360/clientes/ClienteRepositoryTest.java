package cl.pedidos360.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ClienteRepositoryTest {
    @Autowired
    private ClienteRepository repository;

    @Test
    void guardaCliente() {
        Cliente cliente = repository.save(new Cliente("Cliente Demo", "demo@example.com", "Retail"));

        assertThat(cliente.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(1);
    }
}
