package br.com.mmgabri.services;

import br.com.mmgabri.domains.AuthorizationRequest;

public interface ReversalService {
    void publish(AuthorizationRequest request);
}