package cl.pedidos360.pagos;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

record TarjetaPrueba(String numero) {
    static TarjetaPrueba validar(String numero) {
        if (numero == null || !numero.matches("0{15}[1-9]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa solo un numero ficticio de Pedidos360");
        }
        return new TarjetaPrueba(numero);
    }

    boolean rechazada() {
        return (numero.charAt(15) - '0') % 2 == 0;
    }

    @Override
    public String toString() {
        return "TarjetaPrueba";
    }
}
