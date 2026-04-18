# k6 na EC2 (Ubuntu) — Instalação + Ajustes + Upload + Execução

```bash
# 1) Instalar o k6 (repo oficial) — dentro da EC2**
sudo apt-get update && \
sudo apt-get install -y gpg ca-certificates && \
sudo mkdir -p /etc/apt/keyrings && \
curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor -o /etc/apt/keyrings/k6.gpg && \
echo "deb [signed-by=/etc/apt/keyrings/k6.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list > /dev/null && \
sudo apt-get update && \
sudo apt-get install -y k6 && \
k6 version

# 2) Ajustes rápidos do sistema (TPS alto) — dentro da EC2
ulimit -n 100000 && \
sudo sysctl -w net.ipv4.ip_local_port_range="10240 65535" && \
sudo sysctl -w net.core.somaxconn=65535 && \
sudo sysctl -w net.core.netdev_max_backlog=250000 && \
sudo sysctl -w net.ipv4.tcp_tw_reuse=1

# 3) Subir seu script pra EC2 — no Windows (PowerShell)
# (ajuste teste.js pro nome real)
# scp -i .\keypair2.pem .\teste.js ubuntu@ec2-3-91-236-6.compute-1.amazonaws.com:/home/ubuntu/

# 4) Rodar o teste — dentro da EC2
# (depois do upload, na EC2)
k6 inspect teste.js && \
k6 run teste.js
