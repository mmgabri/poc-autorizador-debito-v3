package br.com.mmgabri.application.services;

import br.com.mmgabri.application.domains.Payload;
import org.springframework.stereotype.Service;

@Service
public class LimitePortadorService {

    public boolean shouldExecute(Payload payload) {
        // Simulando 3% apenas transações de clientes PJ
       // return Math.random() < 0.03;
       return true;
    }
}