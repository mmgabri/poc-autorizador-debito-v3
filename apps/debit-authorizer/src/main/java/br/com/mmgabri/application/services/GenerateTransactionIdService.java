package br.com.mmgabri.application.services;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenerateTransactionIdService {

    public String generateFinancialTransactionId(AuthorizeTransactionRequest request) {
        // TODO generate from the ISO fields - financial
        return UUID.randomUUID().toString();
    }

    public String generateReversalTransactionId(AuthorizeTransactionRequest request) {
        // TODO generate from the ISO fields - reversal
        return request.getReversalTransactionId();
    }

}
