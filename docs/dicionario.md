# Dicionário de Dados: AgroMonitor – Sistema de Gestão de Plantios

## 1. Histórico de versões

| Data | Autor | Versão | Comentários |
| --- | --- | --- | --- |
| 23/09/2026 | Giovana | 1.0.0 | Versão inicial do dicionário de dados do AgroMonitor |
| 28/09/2026 | Laura | 1.0.1 | Revisão e validação do conteúdo existente |


## 2. Entidades e atributos conceituais

### 2.1. Usuário

Usuário representa o produtor rural que utiliza o sistema (ex.: João da Silva). É a entidade responsável pelo login, pelas preferências de conta e pela titularidade dos plantios cadastrados. Fica na coleção `usuarios`.

#### Atributos de Usuário

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| codigo | Número inteiro | Sim | Simples | 1, 25, 103 | vazio, nulo, 0, negativo | Código interno sequencial, gerado pelo servidor a partir da coleção `contadores`. Índice único. |
| nome | Texto | Sim | Simples | João da Silva | "", "A" | Nome completo do produtor. Mínimo de 2 caracteres (após `trim`). Não há limite máximo validado. |
| email | Texto | Sim | Simples | joao.silva@gmail.com | joao.silva, joao@, @gmail.com | Usado no login e no envio de alertas por e-mail. Validado por expressão regular `usuario@dominio.tld`; é gravado em minúsculas e sem espaços nas pontas. Índice único. |
| telefone | Texto | Não | Simples | (19) 998765647 | texto sem nenhum dígito (ex.: "abc") | Telefone de contato. Se vier vazio, é gravado como `null`. Só exige ao menos um dígito; não há máscara obrigatória. |
| documento | Documento de identificação | Não | Composto (C) | `{ tipo: "CPF", valor: "52998224725" }` | CPF com dígitos verificadores incorretos, 11 dígitos repetidos (111.111.111-11), menos/mais de 11 dígitos | Gravado como `null` quando o CPF não é informado. Somente CPF é aceito hoje (CNPJ não é validado nem aceito no cadastro). O valor é guardado só com dígitos. Ver item 3.1. |
| senhaHash | Texto | Sim | Simples | `$2a$12$...` (hash BCrypt) | senha em texto puro | Hash BCrypt com custo 12. A senha original (mínimo de 6 caracteres) trafega só até o servidor e nunca é armazenada. |
| receberAlertasPorEmail | Booleano | Sim | Simples | true, false | "sim", "não" | Preferência única para todos os alertas por e-mail (tela Configurações). Valor inicial: `true`. |
| ativo | Booleano | Sim | Simples | true, false | "sim", "não" | `false` quando o usuário exclui a conta (exclusão lógica). Usuários inativos não fazem login, não recebem alertas e seus plantios deixam de gerar alertas. |
| dataCriacao | Data e hora | Sim | Simples | 2026-01-10 09:00:00 | data impossível (2026-02-31) | Data e hora de criação da conta, gerada pelo servidor. |
| assinaturaAtiva | Assinatura (embutida) | Sim | Composto (C) | `{ plano: "Gratuito", dataInicio: ..., status: "ATIVA" }` | — | Assinatura vigente embutida no documento do usuário. Ver item 2.2. |
| recuperacaoSenha | Recuperação de senha (embutida) | Não | Composto (C) | `{ tokenHash, expiraEm, criadoEm }` | token em texto puro | Existe apenas enquanto há um link de recuperação pendente. Removido ao redefinir/alterar a senha ou ao excluir a conta. Ver item 2.6. |

### 2.2. Assinatura

Assinatura representa o plano do usuário ao longo do tempo. **Hoje todo usuário nasce com o plano "Gratuito"**: não existe entidade/coleção de Planos nem fluxo de troca de plano no servidor. Os nomes dos planos (Gratuito, Essencial, Avançado) existem apenas como rótulos no front (`configuracoes.js`, guardados em `localStorage`), sem preço, limite de plantios ou recursos persistidos. Por isso o `plano` da assinatura é um texto simples, e não uma referência a um cadastro de planos.

A assinatura aparece em dois lugares: embutida em `usuarios.assinaturaAtiva` (estado atual) e na coleção `assinaturas_historico` (um documento por período de vigência).

#### Atributos de Assinatura

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| usuarioCodigo | Número inteiro | Sim | Relacionamento | 1, 25 | nulo, 0 | Código do usuário titular. Existe **apenas** em `assinaturas_historico` (no documento embutido em `usuarios` o vínculo é implícito). |
| plano | Texto | Sim | Simples | Gratuito | "" | Nome do plano. No estado atual do sistema, sempre "Gratuito". |
| dataInicio | Data e hora | Sim | Simples | 2026-09-01 10:00:00 | data impossível | Início da vigência; igual à data de criação da conta. |
| dataFim | Data e hora | Não | Simples | 2026-10-01 10:00:00 | data anterior ao início | Só existe no histórico (`assinaturas_historico`); `null` enquanto a assinatura está vigente e preenchida quando a conta é excluída. |
| status | Texto enumerado | Sim | Simples | ATIVA, CANCELADA | EXPIRADA, qualquer valor fora da lista | `ATIVA` ao cadastrar; passa para `CANCELADA` quando a conta é excluída. O valor `EXPIRADA` não é usado. |
| formaPagamento | Texto | Não | Simples | `null` | — | Campo reservado no histórico; sempre `null` (não há cobrança implementada). |

### 2.3. Plantio

Plantio é a entidade central do sistema: representa cada "linha do tempo inteligente" cadastrada pelo produtor, reunindo cultura, talhão, datas e atividades. É o que aparece na tela "Meus Plantios" e é criado pelo assistente "Novo Plantio". Fica na coleção `plantios`, com as atividades **embutidas** no documento.

Cultura e talhão **não são entidades separadas**: são listas fixas do front (ver item 3.3) e ficam gravadas dentro do plantio apenas pelo nome.

#### Atributos de Plantio

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| codigo | Número inteiro | Sim | Simples | 1, 55, 120 | vazio, nulo | Código sequencial gerado pelo servidor (`contadores`). Índice único. |
| usuario | Referência ao usuário | Sim | Relacionamento (C) | `{ codigo: 1 }` | usuário inexistente ou inativo | Dono do plantio. Guarda só o `codigo` do usuário. Índice em `usuario.codigo`. |
| cultura | Cultura (embutida) | Sim | Composto (C) | `{ nome: "Soja" }` | "" | Guarda só o `nome`. Valores oferecidos pelo front: Soja, Milho, Trigo, Café, Algodão. O servidor exige apenas texto não vazio. |
| talhao | Talhão (embutido) | Sim | Composto (C) | `{ nome: "Talhão 01" }` | "" | Guarda só o `nome`. Valores oferecidos pelo front: Talhão 01, 02 e 03. O servidor exige apenas texto não vazio. |
| nome | Texto alfanumérico | Sim | Simples | Milho Verão 2025/26 | "", só espaços, mais de 100 caracteres | Nome dado ao plantio pelo produtor. Máximo de 100 caracteres. |
| safra | Texto | Sim | Simples | 25/26, 24/25, 26/27 | "2025/26", "25-26", "", "25" | Formato `AA/AA` (dois dígitos / dois dígitos). O front oferece 24/25, 25/26 e 26/27. |
| descricao | Texto | Não | Simples | Plantio em área de sequeiro | mais de 200 caracteres | Campo livre. Máximo de 200 caracteres. Vazio é gravado como `null`. |
| sistemaCultivo | Texto enumerado | Sim | Simples | SEQUEIRO, IRRIGADO | Sequeiro, irrigado, qualquer valor fora da lista | O front mostra "Sequeiro"/"Irrigado" e envia o código em maiúsculas. |
| areaPlantada | Número decimal | Sim | Simples | 12.5, 0.3 | 0, valor negativo, vazio, NaN | Área plantada, em hectares. Precisa ser maior que zero. |
| localizacao | Texto | Não | Simples | Campinas, SP | — | Cidade (e UF) usada para consultar a previsão do tempo; vazio é gravado como `null`. Sem localização, o plantio não gera alertas climáticos. Não há validação de formato no servidor. |
| altitude | Número inteiro | Não | Simples | 680, 0 | -10 | Altitude, em metros. `null` quando não informada. |
| dataPlantio | Data | Sim | Simples | 2026-05-15 | 2026-02-31, "", 15/05/2026 | Formato ISO `yyyy-MM-dd`. Gravada como meia-noite UTC. Pode ser passada ou futura. |
| metodoPlantio | Texto enumerado | Não | Simples | PLANTIO_DIRETO, CONVENCIONAL, MINIMO | qualquer valor fora da lista | Vazio é gravado como `null`. O front mostra "Plantio direto", "Plantio convencional" e "Plantio mínimo". |
| status | Texto enumerado | Sim | Simples | FASE_INICIAL, DESENVOLVIMENTO, FASE_FINAL, CONCLUIDO | qualquer valor fora da lista | **Calculado** a partir do `progresso`: 0–33% FASE_INICIAL; 34–66% DESENVOLVIMENTO; 67–99% FASE_FINAL; 100% CONCLUIDO. Índice em `status`. |
| progresso | Número inteiro | Sim | Simples | 0, 25, 50, 100 | -1, 101 | Percentual = atividades `CONCLUIDA` ÷ total de atividades do plantio (arredondado para baixo). Recalculado sempre que uma atividade é registrada ou concluída. |
| ativo | Booleano | Sim | Simples | true, false | "sim", "não" | Plantios com `false` não aparecem nas listagens, calendário, atividades nem geram alertas. Hoje não há tela para inativar um plantio. |
| dataCriacao | Data e hora | Sim | Simples | 2026-05-10 08:00:00 | data impossível | Gerada pelo servidor. Usada para ordenar "Meus Plantios" (mais recentes primeiro). |
| atividades | Lista de Atividade | Sim | Multivalorado e composto (MC) | 4 atividades iniciais | — | Lista embutida. O plantio já nasce com 4 atividades previstas (ver item 2.4 e cronograma no item 3.4). |

### 2.4. Atividade

Atividade representa cada tarefa prevista (ou já realizada) dentro da linha do tempo de um plantio: plantio em si, irrigação, adubação de cobertura, tratos culturais etc. Não tem coleção própria: é um **subdocumento embutido** em `plantios.atividades`. Alimenta o Calendário, a tela Atividades, a coluna "Próxima Atividade" de Meus Plantios e os alertas de atividade.

#### Atributos de Atividade

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| codigo | Número inteiro | Sim | Simples | 1, 800, 921 | vazio, nulo | Código sequencial global (coleção `contadores`, chave `atividades`), único entre todos os plantios. |
| tipoAtividade | Texto enumerado | Sim | Simples | PLANTIO, IRRIGACAO, ADUBACAO, TRATAMENTO, MONITORAMENTO, COLHEITA | qualquer valor fora da lista | No servidor existem 6 tipos. O formulário "Nova atividade" oferece 5 opções: "Irrigação", "Adubação de cobertura", "Monitoramento", "Tratos culturais" e "Aplicação de defensivo" — as duas últimas são enviadas como `TRATAMENTO`. `PLANTIO` e `COLHEITA` não são selecionáveis nesse formulário (PLANTIO é criado automaticamente com o plantio). |
| dataPrevista | Data | Sim | Simples | 2026-06-04 | 2026-02-31, vazio | Nas 4 atividades iniciais é calculada a partir da data de plantio (ou do ajuste manual do usuário). Em atividade registrada manualmente, é a data informada em "data de execução". Meia-noite UTC. |
| dataRealizada | Data | Não | Simples | 2026-06-05 | data futura | `null` enquanto não concluída. Quando a atividade é registrada já como CONCLUIDA recebe a mesma data informada; ao concluir uma atividade existente recebe a data de hoje. |
| status | Texto enumerado | Sim | Simples | PENDENTE, CONCLUIDA, ATRASADA | Planejada, qualquer valor fora da lista | As 4 atividades iniciais nascem `PENDENTE`. Na listagem, uma `PENDENTE` com data prevista anterior a hoje é **exibida** como `ATRASADA` (cálculo na leitura, não gravado). No registro manual vale: CONCLUIDA → data de hoje ou anterior; ATRASADA → data anterior a hoje; PENDENTE → data de hoje ou futura. O rótulo "Planejada" do formulário é enviado como `PENDENTE`. |
| quantidade | Texto | Não | Simples | 22 mm, 40 kg/ha | mais de 30 caracteres | Texto livre (não numérico). Presente apenas em atividades registradas manualmente; vazio é `null`. |
| responsavel | Texto | Não | Simples | Carlos Souza | mais de 100 caracteres | Presente apenas em atividades registradas manualmente; vazio é `null`. |
| observacoes | Texto | Não | Simples | Adubação de cobertura atrasada | mais de 200 caracteres | Campo livre; vazio é `null`. |
| ordem | Número inteiro | Não | Simples | 1, 2, 3, 4 | valor negativo | Posição no cronograma inicial. Gravada **somente** nas 4 atividades criadas junto com o plantio; atividades registradas manualmente não têm `ordem`. |

### 2.5. Alerta (de atividade)

Alerta representa um aviso gerado automaticamente pelo servidor a partir de uma atividade ainda não concluída (próxima, para hoje ou atrasada). Fica na coleção `alertas`. As recomendações ficam embutidas como lista de textos.

Os **alertas climáticos** (chuva, geada, calor, vento) **não são gravados em `alertas`**: são calculados no front a cada visita, a partir da previsão do Open-Meteo (ver item 2.7).

#### Atributos de Alerta

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| codigo | Número inteiro | Sim | Simples | 1, 900, 1050 | vazio, nulo | Código sequencial (`contadores`, chave `alertas`). Índice único. |
| usuario | Referência ao usuário | Sim | Relacionamento (C) | `{ codigo: 1 }` | nulo | Destinatário do alerta. Guarda só o `codigo`. |
| plantio | Plantio (resumo embutido) | Sim | Composto (C) | `{ codigo: 55, nome: "Soja 25/26", cultura: "Soja" }` | nulo | Cópia do código, nome e cultura do plantio no momento do alerta. |
| talhao | Talhão (embutido) | Sim | Composto (C) | `{ nome: "Talhão 01" }` | nulo | Cópia do nome do talhão do plantio. |
| atividade | Atividade (resumo embutido) | Sim | Composto (C) | `{ codigo: 921, tipoAtividade: "ADUBACAO" }` | nulo | Atividade que originou o alerta. |
| motivo | Texto enumerado | Sim | Simples | PROXIMA, ATRASADA | qualquer valor fora da lista | `PROXIMA` = atividade hoje, amanhã ou depois de amanhã; `ATRASADA` = data prevista já passou. Cada atividade tem no máximo 1 alerta por motivo (índice único `atividade.codigo` + `motivo`). |
| tipoAlerta | Texto enumerado | Sim | Simples | ATIVIDADE | CLIMATICO | Na coleção `alertas`, sempre `ATIVIDADE`. O tipo "Climático" existe só na tela (alertas calculados no front). |
| titulo | Texto | Sim | Simples | Atividade atrasada, Atividade para hoje, Atividade se aproxima | "" | Definido pela regra: atrasada → "Atividade atrasada"; hoje → "Atividade para hoje"; em 1–2 dias → "Atividade se aproxima". |
| descricao | Texto | Sim | Simples | A atividade "Irrigação" do plantio Soja 25/26 estava prevista para 04/06 e ainda não foi concluída. | "" | Texto gerado automaticamente pelo servidor (não é digitado pelo usuário). |
| prioridade | Texto enumerado | Sim | Simples | CRITICO, ALTA, MEDIA | BAIXA, qualquer valor fora da lista | Atrasada: ALTA (CRITICO com 3 dias ou mais de atraso); para hoje: ALTA; em 1–2 dias: MEDIA. Um alerta ATRASADA/ALTA ainda NAO_LIDO sobe para CRITICO quando completa 3 dias de atraso. |
| situacao | Texto enumerado | Sim | Simples | NAO_LIDO, LIDO | qualquer valor fora da lista | Nasce `NAO_LIDO`. Muda para `LIDO` pelo botão "Marcar como lido" ou automaticamente quando a atividade é concluída. É apenas atualizada, nunca duplicada. |
| dataAlerta | Data e hora | Sim | Simples | 2026-06-01 08:30:00 | data impossível | Momento em que o servidor gerou o alerta. Usada para ordenar a listagem (mais recentes primeiro). |
| dataAtividade | Data | Sim | Simples | 2026-06-04 | data impossível | Cópia da data prevista da atividade que gerou o alerta. |
| origem | Texto enumerado | Sim | Simples | SISTEMA_MONITORAMENTO | PREVISAO_TEMPO | Nos alertas gravados, sempre `SISTEMA_MONITORAMENTO` (vigia automático). |
| impactoAtividade | Texto | Sim | Simples | Irrigação, Adubação de cobertura, Tratos culturais | — | Nome amigável do tipo da atividade (painel "Impacto nos plantios"). |
| impactoDataLabel | Texto | Sim | Simples | Prevista para 04/06/2026 | — | Rótulo de data do painel de impacto. Usa data absoluta (não "hoje"/"amanhã") para não ficar desatualizado. |
| recomendacoes | Lista de texto | Sim | Multivalorado (M) | ["Realize a atividade hoje, nos melhores horários do dia.", "Confira a previsão do tempo antes de ir a campo.", "Registre a atividade assim que concluir."] | lista vazia | Sempre 3 textos fixos por motivo (hoje / próxima / atrasada), embutidos como array de strings. Não são entidade separada nem têm `ordem`: a ordem é a posição na lista. |
| emailEnviado | Booleano | Sim | Simples | false, true | — | `false` ao criar; `true` quando o e-mail foi enviado **ou** descartado (usuário inativo ou que optou por não receber). Alertas só entram na fila de e-mail se forem NAO_LIDO, `tipoAlerta = ATIVIDADE` e tiverem sido criados nas últimas 24 horas. |

### 2.6. Recuperação de senha

Recuperação de senha representa o pedido pendente de "Esqueci minha senha". Não tem coleção própria: é um subdocumento opcional em `usuarios.recuperacaoSenha`.

#### Atributos de Recuperação de senha

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| tokenHash | Texto | Sim | Simples | 9f86d081884c7d65... (SHA-256 em hexadecimal) | token em texto puro | Apenas o hash SHA-256 do token é gravado. O token original (32 bytes aleatórios, Base64 URL) vai só no link enviado por e-mail. Índice em `recuperacaoSenha.tokenHash`. |
| expiraEm | Data e hora | Sim | Simples | 2026-06-01 09:00:00 | data no passado | Validade de 30 minutos após a criação. |
| criadoEm | Data e hora | Sim | Simples | 2026-06-01 08:30:00 | data impossível | Usada para o intervalo mínimo de 60 segundos entre dois pedidos para o mesmo e-mail. |

### 2.7. Alerta climático (calculado no front) e registro de e-mail climático

Os alertas climáticos **não são persistidos como alertas**. A tela de Alertas e o Início consultam a API Open-Meteo (geocodificação + previsão de até 16 dias) para a `localizacao` de cada plantio não concluído e geram, no navegador, alertas para os 7 primeiros dias. Dias consecutivos com o mesmo risco são agrupados em um único alerta. As **recomendações** e a **previsão do tempo** exibidas no detalhe também são montadas no front e **não ficam guardadas no banco** (por isso não existem entidades "Recomendação de Alerta" ou "Previsão do Tempo" no modelo persistido).

Regras de risco aplicadas (constantes em `clima.js`):

| Tipo de risco | Condição | Prioridade |
| :--- | :--- | :--- |
| Chuva intensa | probabilidade ≥ 60% **e** precipitação ≥ 10 mm no dia | Crítico se ≥ 30 mm; senão Alta |
| Geada | temperatura mínima ≤ 3 °C | Crítico se ≤ 0 °C; senão Alta |
| Temperatura muito alta | temperatura máxima ≥ 35 °C | Crítico se ≥ 38 °C; senão Alta |
| Ventos fortes | vento máximo ≥ 40 km/h | Crítico se ≥ 60 km/h; senão Alta |

A prioridade `Média` não é usada em alertas climáticos. A condição "lido" desses alertas é guardada apenas no `localStorage` do navegador (`agromonitor.alertasLidos`).

Para evitar e-mails duplicados, o servidor mantém a coleção `alertas_climaticos_email`, com um documento por evento climático já notificado:

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| chave | Texto | Sim | Simples | weather-55-chuva-2026-06-01 | "" | Identificador do evento (`weather-<plantio>-<risco>-<data inicial>`), gerado pelo front. Índice único. |
| usuarioCodigo | Número inteiro | Sim | Relacionamento | 1 | 0, nulo | Usuário destinatário. |
| plantioCodigo | Número inteiro | Sim | Relacionamento | 55 | plantio inexistente ou de outro usuário | O servidor confere se o plantio é do usuário e está ativo. |
| plantio | Texto | Sim | Simples | Soja 25/26 | — | Nome do plantio, lido do banco (não do pedido). |
| titulo | Texto | Sim | Simples | Chuva intensa prevista | "" | Título do alerta calculado no front. |
| descricao | Texto | Sim | Simples | Chance de chuva de 80% (cerca de 22 mm) em Campinas, SP. | "" | Descrição do alerta calculado no front. |
| prioridade | Texto | Sim | Simples | Crítico, Alta | — | Prioridade enviada pelo front. |
| data | Texto (data) | Sim | Simples | 2026-06-01 | data impossível | Data inicial do evento (`yyyy-MM-dd`). |
| criadoEm | Data e hora | Sim | Simples | 2026-05-31 20:00:00 | — | Quando o evento foi registrado. |
| emailProcessado | Booleano | Sim | Simples | false, true | — | `true` quando o e-mail foi enviado ou descartado (usuário não quer e-mails). Enquanto `false`, uma nova visita tenta reenviar. |

### 2.8. Contador

Contador é uma coleção técnica (`contadores`) que gera os códigos sequenciais (1, 2, 3…) usados por usuários, plantios, atividades e alertas, já que o MongoDB não possui auto-incremento.

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| _id | Texto | Sim | Simples | usuarios, plantios, atividades, alertas | — | Nome da sequência. |
| seq | Número inteiro | Sim | Simples | 0, 15, 1200 | valor negativo | Último código já usado. Incrementado de forma atômica (`$inc`); pode "pular" números (ex.: um alerta duplicado barrado pelo índice único consome um código). |

## 3. Tipos especiais, compostos ou específicos

### 3.1. Documento de identificação

Estrutura composta por tipo e valor, gravada em `usuarios.documento`. No estado atual, apenas o CPF é suportado, é opcional no cadastro e não é editável depois (a tela Configurações altera só nome, e-mail e telefone). Não é usada para cobrança, pois não há cobrança implementada.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :--- | :--- | :---: | :--- |
| tipo | Texto enumerado | Sim | Valor usado: `CPF`. (`CNPJ` não é aceito.) |
| valor | Texto | Sim | 11 dígitos, sem pontuação, com dígitos verificadores válidos e sem todos os dígitos iguais. |

### 3.2. Padrões de atributos especiais

| Código | Significado | Uso no dicionário |
| :---: | :--- | :--- |
| C | Composto | Atributo com partes internas ou subdocumento (ex.: `documento`, `cultura`, `talhao`, `assinaturaAtiva`). |
| M | Multivalorado | Atributo com vários valores simples (ex.: `recomendacoes` do Alerta, lista de textos). |
| MC | Multivalorado e composto | Lista de estruturas compostas (ex.: `atividades` do Plantio). |

### 3.3. Listas de valores fixas (sem coleção no banco)

Estas listas existem somente no front-end (`novo-plantio.html`); o servidor grava o texto recebido, sem conferir se pertence à lista.

| Lista | Valores oferecidos |
| :--- | :--- |
| Cultura | Soja, Milho, Trigo, Café, Algodão |
| Talhão | Talhão 01, Talhão 02, Talhão 03 |
| Safra | 24/25, 25/26 (padrão), 26/27 |
| Sistema de cultivo | Sequeiro (padrão), Irrigado |
| Método de plantio | Plantio direto (padrão), Plantio convencional, Plantio mínimo |

### 3.4. Cronograma padrão de atividades

Ao cadastrar um plantio, o servidor cria 4 atividades com as datas abaixo, **iguais para todas as culturas** (o cronograma está fixo no código de `PlantioDAO`, não varia por cultura nem é cadastrável). O usuário pode ajustar cada data no assistente; se não ajustar, vale a previsão automática.

| Ordem | tipoAtividade | Nome na tela | Dias após a data de plantio |
| :---: | :--- | :--- | :---: |
| 1 | PLANTIO | Plantio | 0 |
| 2 | IRRIGACAO | Irrigação | 7 |
| 3 | ADUBACAO | Adubação de cobertura | 20 |
| 4 | TRATAMENTO | Tratos culturais | 35 |

### 3.5. Correspondência entre rótulos da tela e códigos gravados

| Campo | Rótulo na tela | Código gravado |
| :--- | :--- | :--- |
| sistemaCultivo | Sequeiro / Irrigado | SEQUEIRO / IRRIGADO |
| metodoPlantio | Plantio direto / Plantio convencional / Plantio mínimo | PLANTIO_DIRETO / CONVENCIONAL / MINIMO |
| tipoAtividade | Irrigação | IRRIGACAO |
| tipoAtividade | Adubação de cobertura | ADUBACAO |
| tipoAtividade | Tratos culturais **e** Aplicação de defensivo | TRATAMENTO |
| tipoAtividade | Monitoramento | MONITORAMENTO |
| status (atividade) | Concluída / Pendente **e** Planejada / Atrasada | CONCLUIDA / PENDENTE / ATRASADA |
| prioridade (alerta) | Crítico / Alta / Média | CRITICO / ALTA / MEDIA |
| situacao (alerta) | Lido / Não lido | LIDO / NAO_LIDO |

## 4. Desdobramento físico ou tecnológico

### 4.1. Banco não relacional orientado a documentos: MongoDB

O sistema usa MongoDB (driver Java, acesso exclusivo via DAOs). As coleções são criadas automaticamente na primeira inserção e os índices são garantidos na inicialização do servidor (`garantirIndices()`).

#### Coleções

| Coleção | Finalidade | Estratégia documental |
| :--- | :--- | :--- |
| `usuarios` | Conta, preferências, assinatura atual e token de recuperação de senha | Embute `assinaturaAtiva`, `documento` e `recuperacaoSenha`. |
| `plantios` | Plantios do usuário e suas atividades | Embute `cultura`, `talhao` (só nome) e a lista `atividades`. |
| `alertas` | Alertas de atividade gerados pelo vigia | Embute `plantio`, `talhao`, `atividade` (resumos) e a lista `recomendacoes`. |
| `assinaturas_historico` | Histórico de assinaturas por período de vigência | Um documento por período, ligado ao usuário por `usuarioCodigo`. |
| `alertas_climaticos_email` | Controle de e-mails de alertas climáticos já tratados | Um documento por evento climático (`chave`). |
| `contadores` | Geração de códigos sequenciais | Um documento por sequência (`_id`: usuarios, plantios, atividades, alertas). |

#### Índices existentes

| Coleção | Índice | Tipo |
| :--- | :--- | :--- |
| `usuarios` | `email` | Único |
| `usuarios` | `codigo` | Único |
| `usuarios` | `recuperacaoSenha.tokenHash` | Comum |
| `plantios` | `codigo` | Único |
| `plantios` | `usuario.codigo` | Comum |
| `plantios` | `status` | Comum |
| `alertas` | `codigo` | Único |
| `alertas` | `usuario.codigo` + `situacao` | Composto |
| `alertas` | `atividade.codigo` + `motivo` | Composto único (impede alertas duplicados) |
| `alertas_climaticos_email` | `chave` | Único |

#### Exemplo de documento da coleção `plantios`

```json
{
  "codigo": 55,
  "usuario": { "codigo": 1 },
  "cultura": { "nome": "Soja" },
  "talhao": { "nome": "Talhão 01" },
  "nome": "Soja Verão 25/26",
  "safra": "25/26",
  "descricao": null,
  "sistemaCultivo": "SEQUEIRO",
  "areaPlantada": 12.5,
  "localizacao": "Campinas, SP",
  "altitude": 680,
  "dataPlantio": "2026-05-15T00:00:00Z",
  "metodoPlantio": "PLANTIO_DIRETO",
  "status": "FASE_INICIAL",
  "progresso": 0,
  "ativo": true,
  "dataCriacao": "2026-05-10T08:00:00Z",
  "atividades": [
    { "codigo": 921, "tipoAtividade": "PLANTIO", "dataPrevista": "2026-05-15T00:00:00Z",
      "dataRealizada": null, "status": "PENDENTE", "observacoes": null, "ordem": 1 }
  ]
}
```

#### Exemplo de documento da coleção `alertas`

```json
{
  "codigo": 900,
  "usuario": { "codigo": 1 },
  "plantio": { "codigo": 55, "nome": "Soja Verão 25/26", "cultura": "Soja" },
  "talhao": { "nome": "Talhão 01" },
  "atividade": { "codigo": 922, "tipoAtividade": "IRRIGACAO" },
  "motivo": "PROXIMA",
  "tipoAlerta": "ATIVIDADE",
  "titulo": "Atividade se aproxima",
  "descricao": "A atividade \"Irrigação\" do plantio Soja Verão 25/26 está prevista para 22/05.",
  "prioridade": "MEDIA",
  "situacao": "NAO_LIDO",
  "dataAlerta": "2026-05-20T08:30:00Z",
  "dataAtividade": "2026-05-22T00:00:00Z",
  "origem": "SISTEMA_MONITORAMENTO",
  "impactoAtividade": "Irrigação",
  "impactoDataLabel": "Prevista para 22/05/2026",
  "recomendacoes": ["Organize insumos, equipamentos e mão de obra antes da data prevista.", "..."],
  "emailEnviado": false
}
```

#### Observações sobre a modelagem documental

- Dados sempre consultados juntos (plantio + suas atividades) ficam no mesmo documento; por isso não existe coleção de atividades.
- O alerta guarda uma **cópia** de nome/cultura/talhão do plantio (desnormalização) para listar sem consultar `plantios`; se o plantio for alterado depois, o alerta já gerado não é atualizado.
- Datas sem hora (`dataPlantio`, `dataPrevista`, `dataRealizada`, `dataAtividade`) são gravadas como meia-noite UTC; datas com hora (`dataCriacao`, `dataAlerta` etc.) usam o horário do servidor.
- Nos DTOs entre servidor e front as datas viajam como texto (`yyyy-MM-dd`, ou `yyyy-MM-dd HH:mm` para `dataAlerta`).

## 5. Consultas, índices e observações de arquitetura

### 5.1. Consultas esperadas

| Consulta | Coleção | Filtro / ordenação | Índice usado |
| :--- | :--- | :--- | :--- |
| Login | `usuarios` | `email` = informado (minúsculas) e `ativo` = true | `email` |
| Listar "Meus Plantios" | `plantios` | `usuario.codigo` = X e `ativo` = true; ordena por `dataCriacao` decrescente | `usuario.codigo` |
| Próxima atividade de um plantio | `plantios` | Em memória: atividade não concluída com menor `dataPrevista` | — |
| Listar atividades do usuário (Atividades, Calendário, Início) | `plantios` | `usuario.codigo` = X e `ativo` = true; atividades ordenadas por data e código | `usuario.codigo` |
| Registrar atividade | `plantios` | `codigo` = P, `usuario.codigo` = X, `ativo` = true; `$push` em `atividades` | `codigo` |
| Concluir atividade | `plantios` | Igual ao anterior + `elemMatch` na atividade com `status` ≠ CONCLUIDA | `codigo` |
| Listar alertas do usuário | `alertas` | `usuario.codigo` = X; ordena por `dataAlerta` decrescente | `usuario.codigo` + `situacao` |
| Marcar alerta como lido | `alertas` | `codigo` = A e `usuario.codigo` = X | `codigo` |
| Gerar alertas (vigia, de hora em hora e após cadastrar plantio/atividade) | `usuarios`, `plantios`, `alertas` | Atividades não concluídas de plantios ativos de usuários ativos; verifica `atividade.codigo` + `motivo` | `atividade.codigo` + `motivo` |
| Fila de e-mails de alerta | `alertas` | `tipoAlerta` = ATIVIDADE, `situacao` = NAO_LIDO, `dataAlerta` nas últimas 24 h, `emailEnviado` ≠ true | — |
| Validar link de recuperação | `usuarios` | `recuperacaoSenha.tokenHash` = hash, `expiraEm` > agora, `ativo` = true | `recuperacaoSenha.tokenHash` |

### 5.2. Regras e observações

- Um plantio pertence a exatamente um usuário; cultura e talhão são guardados por nome dentro do plantio (não há cadastro de culturas ou talhões no banco).
- Uma atividade pertence a um único plantio (está embutida nele). As 4 atividades iniciais são criadas automaticamente com as datas do cronograma padrão (item 3.4) e podem ter a data ajustada pelo usuário já no cadastro.
- O `progresso` e o `status` do plantio são sempre calculados a partir das atividades concluídas e nunca informados pelo usuário.
- O `status` ATRASADA de uma atividade pendente é calculado na leitura (data prevista anterior a hoje); no banco ela permanece `PENDENTE`, exceto quando o usuário a registra explicitamente como ATRASADA.
- Cada atividade gera no máximo um alerta `PROXIMA` e um alerta `ATRASADA`. Ao concluir a atividade, os alertas ainda não lidos dela passam a `LIDO`.
- A situação de um alerta (`LIDO`/`NAO_LIDO`) é apenas atualizada, nunca duplicada em um novo registro.
- O texto, as recomendações e a prioridade de um alerta representam o momento em que ele foi gerado (a única exceção é a subida automática de ALTA para CRITICO em atrasos de 3 dias ou mais, enquanto o alerta não foi lido).
- Alertas só são enviados por e-mail se o usuário estiver ativo e com `receberAlertasPorEmail` = true; caso contrário o alerta é marcado como processado sem envio.
- Um usuário deve possuir no máximo uma assinatura com status ATIVA por vez. Na exclusão da conta, `usuarios.assinaturaAtiva.status` e o registro correspondente em `assinaturas_historico` passam a CANCELADA (com `dataFim` preenchida no histórico).
- Exclusão de conta é lógica (`ativo` = false). Plantios e alertas do usuário permanecem no banco, mas deixam de ser exibidos e processados.
- A senha é armazenada apenas como hash BCrypt (custo 12). O token de recuperação é armazenado apenas como hash SHA-256, vale 30 minutos, só pode ser usado uma vez e só pode ser solicitado novamente após 60 segundos.
- A resposta do pedido de recuperação de senha é a mesma exista ou não o e-mail cadastrado, para não revelar quais e-mails estão no sistema.
- **Previsto, mas ainda não implementado:** planos pagos (preço, limite de plantios, recursos), troca de plano, forma de pagamento, CNPJ, cadastro de culturas/talhões e cronograma por cultura. Se forem implementados, este dicionário deve ganhar as entidades correspondentes.