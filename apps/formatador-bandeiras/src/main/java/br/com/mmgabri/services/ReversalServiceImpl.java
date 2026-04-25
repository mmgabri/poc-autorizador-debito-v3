package br.com.mmgabri.services;

import br.com.mmgabri.domains.FormatadorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReversalServiceImpl {

    private final ReversalService reversalPublisher;

    public void publishReversal(FormatadorRequest request) {
        reversalPublisher.publish(request);
    }
}