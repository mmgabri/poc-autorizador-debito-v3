//Bateria 1 – 900 TPS | 300 ms de latência dos microsserviços

import http from 'k6/http';
import { check } from 'k6';

export const options = {
  discardResponseBodies: true,

  scenarios: {
    autorizador_fast_no_throttle: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 1200,
      maxVUs: 12000,

      stages: [
        // Warm-up curto (deixa o Dynamo "acordar")
        { target: 200, duration: '60s' },

        // Sobe rápido até perto do seu limite
        { target: 350, duration: '60s' },
        { target: 450, duration: '60s' },
        { target: 550, duration: '60s' },

        // Zona crítica: degraus menores (evita throttling)
        { target: 600, duration: '60s' },
        { target: 625, duration: '30s' },
        { target: 650, duration: '30s' },
        { target: 675, duration: '30s' },
        { target: 700, duration: '30s' },
        { target: 725, duration: '30s' },
        { target: 750, duration: '30s' },
        { target: 775, duration: '30s' },
        { target: 800, duration: '30s' },
        { target: 825, duration: '30s' },
        { target: 850, duration: '30s' },
        { target: 875, duration: '30s' },
        { target: 900, duration: '2m' },
      ],
    },
  },

  thresholds: {
    dropped_iterations: ['count==0'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const url =
    'http://autorizador-debito-nlb-bd1c5b6aa854cdbf.elb.us-east-1.amazonaws.com:8080/authorization';

  const payloadObj = {
    messageIso: {
      mti: '0200',
      '002': '5899168602146263',
      '003': '002000',
      '004': '000000010050',
      '007': '0220170800',
      '012': '0222',
      '018': '5118',
      '022': '051',
      '043': 'pao de acucar',
      '049': '986',
      '052': '123456',
      '055': '55A0A161A0011221C0110A50006020501070100002501000226009F3B9F409F419F529F539F549F559F569F579F589F599F609F619F629F63',
      '063': '123456789',
    },

    customReturnDataEnrichment: '000',
    customReturnSeguranca: '000',
    customReturnLimitePortador: '000',
    customReturnLimite: '000',
    customReturnLancamentoConta: '000',
    customReturnFraude: '000',

    sleepDataEnrichment: 50,
    sleepSeguranca: 50,
    sleepLimitePortador: 50,
    sleepLimite: 50,
    sleepLancamentoConta: 50,
    sleepFraude: 50,
  };

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const res = http.post(url, JSON.stringify(payloadObj), params);

  check(res, {
    'status é 200': (r) => r.status === 200,
  });
}
