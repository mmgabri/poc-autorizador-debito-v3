package br.com.mmgabri.services;

import br.com.mmgabri.domains.AuthorizationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReversalServiceImpl {

    private final ReversalService reversalPublisher;

    public void publishReversal(AuthorizationRequest request) {
        reversalPublisher.publish(request);
    }
}