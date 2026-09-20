package br.com.mmgabri.application.services;

import br.com.mmgabri.grpc.autorizador.v1.AutorizadorRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenerateTransactionIdService {

    public String generateTransacionIdFinancial(AutorizadorRequest request) {
        //TODO gera de acordo com os campos da ISO - Financeira
        return UUID.randomUUID().toString();
    }

    public String generateTransacionIdReversal(AutorizadorRequest request) {
        //TODO gera de acordo com os campos da ISO - Reversal
        return request.getTransactionIdReversal();
    }

}
