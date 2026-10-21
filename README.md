# Projeto Integrador 4

## Descrição do Projeto

O AgroMonitor é uma aplicação web voltada para produtores rurais, que permite planejar, acompanhar e otimizar cada etapa do ciclo de suas plantações. A partir do cadastro de um plantio (cultura, talhão e data), o sistema calcula automaticamente os prazos ideais de irrigação, adubação e colheita, e organiza tudo em uma linha do tempo com alertas de risco climático.

## Objetivo

Desenvolver um sistema web que permita:

* Cadastrar e acompanhar plantios (cultura, talhão, data de plantio)
* Calcular automaticamente prazos de irrigação, adubação e colheita
* Emitir alertas e avisos de risco climático
* Visualizar o andamento de cada plantio em uma linha do tempo
* Gerenciar o progresso e status dos plantios (fase inicial, desenvolvimento, fase final)

## Tecnologias Utilizadas

**Frontend**

* HTML5
* CSS3
* JavaScript

**Backend**

* Java
* Spring Boot

**Banco de Dados**

* MongoDB

**Servidor**
* Java

**Controle de Versão**

* Git
* GitHub

## Como executar o projeto

**Pré-requisitos**

- JDK 21;
- Apache Maven 3.9 ou superior;
- Acesso ao MongoDB Atlas e uma string de conexão válida.

**1. Inicie o servidor Java**

Abra um terminal PowerShell na pasta principal do projeto — a pasta que contém `servidor`, `backend` e `front`.

Configure a variável `MONGO_URI` com a string de conexão do MongoDB e inicie o servidor:

```powershell
$env:MONGO_URI = "sua-string-de-conexao-do-mongodb"
cd .\servidor
mvn compile exec:java
```

Mantenha esse terminal aberto. O servidor Java utiliza a porta `3000`.

**2. Inicie o backend**

Abra um segundo terminal PowerShell na pasta principal do projeto e execute:

```powershell
cd .\backend
mvn spring-boot:run
```

Mantenha esse terminal aberto também. O backend utiliza a porta `8080` e serve os arquivos do frontend.

**3. Acesse a aplicação**

Abra [http://localhost:8080](http://localhost:8080) no navegador.

## Integrantes do Grupo

* Beatriz Costa Leme – 25015554
* Camila Fernandes Costacurta – 25012949
* Giovana Uchelli – 25008818
* Laura Cristina de Carvalho – 25014543
