package cl.pedidos360.bff;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.pedidos360.bff.model.ResumenDto;

@RestController
@RequestMapping("/api/v2")
public class VersionController {
    private final GatewayController gatewayController;

    public VersionController(GatewayController gatewayController) {
        this.gatewayController = gatewayController;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumenV2(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ResumenDto resumen = gatewayController.resumen(authorization);
        return Map.of(
                "version", "v2",
                "fechaConsulta", OffsetDateTime.now().toString(),
                "datos", resumen);
    }
}
