package br.com.mmgabri.adapters.rest;

import br.com.mmgabri.application.domains.FraudesRequest;
import br.com.mmgabri.application.domains.FraudesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "fraudesClient",
        url = "${rest.clients.fraudes.url}"
)
public interface FraudesClient {
    @PostMapping("/validar")
    FraudesResponse validarFraude(@RequestBody FraudesRequest request);
}