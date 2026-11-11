# Projeto Integrador 4

## Descrição do Projeto - AgroMonitor

O AgroMonitor é uma aplicação web voltada para produtores rurais, que permite planejar, acompanhar e otimizar cada etapa do ciclo de suas plantações. A partir do cadastro de um plantio (cultura, talhão e data), o sistema calcula automaticamente os prazos ideais de irrigação, adubação e colheita, e organiza tudo em uma linha do tempo com alertas de risco climático.

## Objetivo

Desenvolver um sistema web que permita:

* Cadastrar e acompanhar plantios (cultura, talhão, data de plantio)
* Calcular automaticamente prazos de irrigação, adubação e colheita
* Emitir alertas e avisos de risco climático
* Visualizar o andamento de cada plantio em uma linha do tempo
* Gerenciar o progresso e status dos plantios (fase inicial, desenvolvimento, fase final)

## Tecnologias Utilizadas

**Frontend**: HTML5, CSS3, JavaScript

**Backend**: Java, Spring Boot

**Banco de Dados**: MongoDB

**Servidor**: Java

**Controle de Versão**: Git, GitHub

## Como executar o projeto

### Pré-requisitos

* JDK 21
* Apache Maven 3.9 ou superior
* Uma conta gratuita no [MongoDB Atlas](https://www.mongodb.com/atlas) (ou um MongoDB próprio)

### 1. Configure o banco de dados (MongoDB Atlas)

A string de conexão **não está no repositório** (por segurança). Cada pessoa precisa usar a sua:

1. Crie um cluster gratuito no MongoDB Atlas.
2. Em **Database Access**, crie um usuário e uma senha.
3. Em **Network Access**, libere o seu IP (ou `0.0.0.0/0` apenas para testes).
4. Em **Connect → Drivers**, copie a string de conexão, que tem este formato:
   `mongodb+srv://USUARIO:SENHA@cluster.xxxxx.mongodb.net/`
5. Troque `USUARIO` e `SENHA` pelos dados criados no passo 2.

O banco `agromonitor` e as coleções são criados automaticamente na primeira execução.


### 2. Inicie o servidor Java

Abra um terminal PowerShell na pasta principal do projeto (a que contém `servidor`, `backend` e `front`):

```powershell
$env:MONGO_URI = "mongodb+srv://USUARIO:SENHA@cluster.xxxxx.mongodb.net/"
cd .\servidor
mvn compile exec:java
```

Mantenha o terminal aberto. O servidor usa a porta `3000`. Para desligá-lo, digite `desativar`.

**Opcional: e-mail** (recuperação de senha e alertas). Sem isso o sistema funciona, mas não envia e-mails. Defina antes de iniciar o servidor:

```powershell
$env:SMTP_USER = "seu-email@gmail.com"
$env:SMTP_PASS = "senha-de-app-do-gmail"
```

(No Gmail, use uma *senha de app*, não a senha normal da conta.)

### 3. Inicie o backend

Em um segundo terminal PowerShell, na pasta principal:

```powershell
cd .\backend
mvn spring-boot:run
```

O backend usa a porta `8080` e serve os arquivos do frontend.

### 4. Acesse a aplicação

Abra http://localhost:8080 no navegador.

## Integrantes do Grupo

* Beatriz Costa Leme – 25015554
* Camila Fernandes Costacurta – 25012949
* Giovana Uchelli – 25008818
* Laura Cristina de Carvalho – 25014543
