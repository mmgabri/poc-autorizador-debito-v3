# Este módulo cria apenas as roles do control plane e dos nós, que não
# dependem do cluster já existir. A role IRSA da aplicação fica no módulo
# "irsa", criado depois do cluster (precisa do OIDC issuer do cluster).
