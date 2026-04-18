import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

export let options = {
    stages: [
        { duration: '1m', target: 2 }, 
      ],
    thresholds: {
        grpc_req_duration: ['p(95)<600'], // Exemplo de threshold correto para gRPC
  },
};


// Função para gerar UUID
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Função para gerar timestamp no formato "YYYY-MM-DD HH:mm:ss"
function getFormattedTimestamp() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

const client = new grpc.Client();
client.load(['./definitions'], 'Autorizador.proto');  // Certifique-se de usar o caminho correto para o proto

export default () => {
    client.connect('localhost:9092', {
         timeout: '160s',
        plaintext: true,
    });

    const header = {
        transactionId: generateUUID(),
        correlationId: generateUUID(),
        clearingId: generateUUID(),
        bandeira: 'Mastercard ',
        plataforma: 'SINGLE_MESSAGE',
        timestamp: getFormattedTimestamp(),
        message: '',
        isReversal: false
    };

    const data = {
        header,
        de02: "5899168602146263",
        de03: "002000",
        de04: "000000010050",
        de07: "0220170800",
        de12: "0222",
        de18: "5118",
        de22: "051",
        de43: "pao de acucar",
        de49: "986 ",
        de52: "123456",
        de55: "55A0A161A0011221C0110A50006020501070100002501000226009F3B9F409F419F529F539F549F559F569F579F589F599F609F619F629F63",
        de63: "123456789",
        customReturnSeguranca: "000",
        customReturnLimitePortador: "000",
        customReturnLimite: "000",
        customReturnLancamentoConta: "000",
        customReturnFraude: "000",
    };
    
    const response = client.invoke('br.com.mmgabri.grpc.autorizador.AutorizadorService/AutorizarTransacao', data);

    //console.log('response: ', response);

    check(response, {
        'Status Ok Sucesso': (r) => r && r.status === grpc.StatusOK,
    });

    client.close();
    sleep(1);
};
