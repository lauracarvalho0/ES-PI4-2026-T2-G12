# Projeto Integrador 4

## Descrição do Projeto - AgroMonitor

O AgroMonitor é uma aplicação web voltada para produtores rurais, que permite planejar, acompanhar e otimizar cada etapa do ciclo de suas plantações. A partir do cadastro de um plantio (cultura, talhão e data), o sistema calcula automaticamente os prazos de plantio, irrigação, adubação de cobertura e tratos culturais, e organiza tudo em uma linha do tempo com alertas de atividades e de risco climático.

## Objetivo

Desenvolver um sistema web que permita:

* Cadastrar e acompanhar plantios (cultura, talhão, data de plantio)
* Calcular automaticamente os prazos de plantio, irrigação, adubação e tratos culturais
* Emitir alertas e avisos de risco climático
* Visualizar o andamento de cada plantio em uma linha do tempo
* Gerenciar o progresso e status dos plantios (fase inicial, desenvolvimento, fase final)

## Funcionalidades

**Conta e acesso**

* Cadastro de produtor (nome, e-mail, telefone e CPF; telefone e CPF são opcionais), com senha guardada em hash BCrypt
* Login e logout
* Recuperação e redefinição de senha por link enviado por e-mail
* Configurações: editar perfil, alterar senha, preferência de alertas por e-mail e exclusão de conta

**Plantios**

* Assistente "Novo Plantio": cultura, talhão, safra, sistema de cultivo (sequeiro/irrigado), área, localização, altitude, método e data de plantio
* Cronograma automático: ao cadastrar, o sistema cria as atividades de plantio, irrigação (+7 dias), adubação de cobertura (+20 dias) e tratos culturais (+35 dias), com datas ajustáveis
* Lista "Meus Plantios" com progresso, próxima atividade e status calculado (fase inicial, desenvolvimento, fase final e concluído)

**Atividades e calendário**

* Registro de atividades (plantio, irrigação, adubação, monitoramento, tratos culturais, aplicação de defensivo e colheita) com quantidade, responsável e observações. A aplicação de defensivo é gravada como tratos culturais
* Conclusão de atividades, com recálculo automático do progresso do plantio
* Atividades pendentes exibidas como atrasadas quando a data prevista já passou
* Calendário com todas as atividades e filtro por categoria

**Alertas**

* Alertas de atividade (gerados automaticamente a cada hora pelo servidor): atividade se aproxima, para hoje ou atrasada, com prioridade média, alta ou crítica
* Alertas de risco climático calculados no navegador a partir da previsão da Open-Meteo para a localização do plantio: chuva intensa, geada, calor extremo e ventos fortes. O servidor apenas evita o envio duplicado do e-mail desses alertas
* Recomendações em cada alerta e marcação como lido
* Envio de alertas por e-mail, sem duplicidade, respeitando a preferência do usuário

**Relatórios e painel**

* Painel inicial com visão geral dos plantios, atividades e alertas
* Relatórios com filtros por período, plantio, cultura e talhão, gráfico de status das atividades e próximas atividades
* Exportação do resumo dos plantios em CSV

## Tecnologias Utilizadas

* **Frontend:** HTML5, CSS3, JavaScript
* **Backend:** Java, Spring Boot (API REST) e servidor Java próprio (sockets)
* **Banco de Dados:** MongoDB
* **Servidor:** Java
* **Testes:** JUnit 5, Mockito e MongoDB em memória
* **Integrações:** Open-Meteo (previsão do tempo) e SMTP (e-mails)
* **Controle de Versão:** Git, GitHub

## Estrutura do projeto

```
ES-PI4-2026-T2-G12/
├── front/                    # Interface: HTML, CSS e JavaScript puros
│   ├── index.html            # Página inicial (landing)
│   ├── pages/
│   │   ├── auth/             # login, cadastro, recuperar e redefinir senha
│   │   └── dashboard/        # início, plantios, novo plantio, atividades,
│   │                         # nova atividade, calendário, alertas, relatórios, configurações
│   ├── js/                   # um script por tela + clima.js (alertas climáticos)
│   ├── css/                  # style.css (base) e um estilo por tela
│   └── assets/images/        # imagens e ícone
├── backend/                  # Spring Boot (porta 8080): API REST em /api/** + serve o front
│   └── src/main/java/agromonitor/
│       ├── backend/          # BackendApplication, ConexaoComServidor e controllers
│       │                     # (Auth, Plantio, Atividade, Alerta, Conta)
│       └── comum/            # Pedidos/Respostas trocados com o servidor (por módulo)
├── servidor/                 # Servidor Java (porta 3000): regras de negócio e acesso ao MongoDB
│   └── src/
│       ├── main/java/agromonitor/
│       │   ├── servidor/     # Servidor, conexões, DAOs, vigia de alertas, e-mail
│       │   │   ├── dao/      # UsuarioDAO, PlantioDAO, AlertaDAO
│       │   │   ├── modelo/   # Usuario, Perfil, TokenRecuperacao
│       │   │   ├── validacao/# ValidadorUsuario, ValidadorPlantio, ValidadorAtividade
│       │   │   └── email/    # EmailService (SMTP)
│       │   └── comum/        # mesmas classes de Pedido/Resposta do backend
│       └── test/java/        # testes JUnit (validadores, regras de alerta e cadastro de plantio)
├── docs/                     # dicionário de dados, relatório e prototipação
├── TESTES.md                 # guia resumido de testes
└── README.md
```

Como as partes conversam: navegador → backend (HTTP, porta 8080) → servidor (socket, porta 3000) → MongoDB.

## Como executar o projeto

### Pré-requisitos

* JDK 21
* Apache Maven 3.9 ou superior
* Uma conta gratuita no [MongoDB Atlas](https://www.mongodb.com/atlas) (ou um MongoDB próprio)

### 1. Clone o repositório

```bash
git clone https://github.com/lauracarvalho0/ES-PI4-2026-T2-G12.git
cd ES-PI4-2026-T2-G12
```

### 2. Configure o banco de dados (MongoDB Atlas)

A string de conexão não está no repositório (por segurança). Cada pessoa precisa usar a sua:

1. Crie um cluster gratuito no MongoDB Atlas.
2. Em **Database Access**, crie um usuário e uma senha.
3. Em **Network Access**, libere o seu IP (ou `0.0.0.0/0` apenas para testes).
4. Em **Connect → Drivers**, copie a string de conexão, que tem este formato:
   `mongodb+srv://USUARIO:SENHA@cluster.xxxxx.mongodb.net/`
5. Troque `USUARIO` e `SENHA` pelos dados criados no passo 2.

O banco `agromonitor` e as coleções são criados automaticamente na primeira execução.

### 3. Inicie o servidor Java

Abra um terminal PowerShell na pasta principal do projeto (a que contém `servidor`, `backend` e `front`, ou seja, a pasta do clone):

```powershell
$env:MONGO_URI = "mongodb+srv://USUARIO:SENHA@cluster.xxxxx.mongodb.net/"
cd .\servidor
mvn compile exec:java
```

Mantenha o terminal aberto. O servidor usa a porta 3000. Para desligá-lo, digite `desativar`.

**Opcional: e-mail (recuperação de senha e alertas).** Sem isso o sistema funciona, mas não envia e-mails (o link de recuperação é mostrado no console do servidor). Defina antes de iniciar o servidor:

```powershell
$env:SMTP_USER = "seu-email@gmail.com"
$env:SMTP_PASS = "senha-de-app-do-gmail"
```

(No Gmail, use uma senha de app, não a senha normal da conta.)

### 4. Inicie o backend

Em um **segundo** terminal PowerShell, na pasta principal:

```powershell
cd .\backend
mvn spring-boot:run
```

O backend usa a porta 8080 e serve os arquivos do frontend. Ele precisa ser iniciado de dentro da pasta `backend`, pois encontra o `front` pelo caminho relativo `../front/`.

### 5. Acesse a aplicação

Abra [http://localhost:8080](http://localhost:8080) no navegador.

## Testes

Os testes automatizados ficam no módulo `servidor` (JUnit 5 + Mockito). Eles usam um MongoDB em memória e não enviam e-mails reais, então não precisam de internet nem tocam no seu banco do Atlas, e não exigem `MONGO_URI`.

```bash
cd servidor
mvn test
```

Para rodar uma classe específica:

```bash
mvn test -Dtest=ValidadorPlantioTest
```

Os relatórios detalhados ficam em `servidor/target/surefire-reports/`. Mais detalhes em [TESTES.md](TESTES.md).

## Integrantes do Grupo

* Beatriz Costa Leme – 25015554
* Camila Fernandes Costacurta – 25012949
* Giovana Uchelli – 25008818
* Laura Cristina de Carvalho – 25014543
