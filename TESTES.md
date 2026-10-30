# Como rodar todos os testes

Os testes ficam em dois lugares. Cada um roda sozinho.

## 1) Servidor (Java): validadores, regras de alerta, DAOs, supervisora, e-mail...
    cd servidor
    mvn test

Usa um MongoDB em memoria (nao precisa instalar nada) e um servico de e-mail falso:
nenhum e-mail real e enviado e o seu banco (Atlas) nao e tocado.

## 2) Backend (Java): controllers REST, conexao por socket e aplicacao inteira
    cd backend
    mvn test

Relatorios dos testes Java: `target/surefire-reports/`.
