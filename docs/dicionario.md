# Dicionário de Dados: AgroMonitor – Sistema de Gestão de Plantios

## 1. Histórico de versões

| Data | Autor | Versão | Comentários |
| --- | --- | --- | --- |
| 23/09/2026 | Giovana | 1.0.0| Versão inicial do dicionário de dados do AgroMonitor |

## 2. Entidades e atributos conceituais

### 2.1. Usuário

Usuário representa o produtor rural que utiliza o sistema (ex.: João da Silva). É a entidade responsável pelo login, pelas preferências de conta e pela titularidade dos plantios cadastrados.

#### Atributos de Usuário

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 25, 103 | vazio, nulo | Código interno do usuário. |
| nome | Texto  | Sim | Simples | João da Silva | "", "A" | Nome completo do produtor. |
| email | Texto | Sim | Simples | joao.silva@gmail.com | email sem formato válido | Usado para login e envio de alertas por e-mail. Deve ser único. |
| telefone | Texto | Não | Simples | (19) 998765647 | texto sem número algum | Telefone de contato. |
| documento | Documento de identificação | Não | Composto (C) | CPF ou CNPJ válido | documento vazio ou inválido | Usado na cobrança de planos pagos. Ver item 3.1. |
| senhaHash | Texto | Sim | Simples | hash gerado pelo sistema | senha em texto puro | A senha nunca deve ser armazenada em texto plano, apenas seu hash. |
| fusoHorario | Texto enumerado | Sim | Simples | (GMT-03:00) Brasília | valor fora da lista de fusos suportados | Usado para exibir datas e horários corretamente. |
| idioma | Texto enumerado | Sim | Simples | Português, English, Español | qualquer valor fora da lista | Idioma de exibição do sistema. |
| receberAlertasPorEmail | Booleano | Sim | Simples | true, false | sim, não | Preferência de notificação (tela Configurações). |
| ativo | Booleano | Sim | Simples | true, false | sim, não | Falso quando o usuário exclui a conta (exclusão lógica). |
| dataCriacao | Data e hora | Sim | Simples | 2026-01-10 09:00:00 | data impossível | Data de criação da conta. |

### 2.2. Plano

Plano representa cada opção de assinatura oferecida pelo sistema (Gratuito, Essencial, Avançado), com seus respectivos limites e recursos.

#### Atributos de Plano

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 2, 3 | vazio, nulo | Código interno do plano. |
| nome | Texto | Sim | Simples | Gratuito, Essencial, Avançado | "" | Nome comercial do plano. Deve ser único. |
| preco | Número decimal | Sim | Simples | 0.00, 29.90, 59.90 | valor negativo | Valor mensal cobrado. |
| limitePlantiosAtivos | Número inteiro | Não | Simples | 1, 5 | valor negativo | Nulo significa "ilimitado" (caso do Plano Avançado). |
| suportePrioritario | Booleano | Sim | Simples | true, false | sim, não | Recurso exclusivo de planos pagos superiores. |
| acessoAntecipadoNovidades | Booleano | Sim | Simples | true, false | sim, não | Recurso exclusivo do Plano Avançado. |
| ativo | Booleano | Sim | Simples | true, false | sim, não | Permite descontinuar um plano sem apagar seu histórico de assinaturas. |

### 2.3. Assinatura

Assinatura representa o vínculo entre um usuário e um plano ao longo do tempo. Essa entidade existe porque um usuário pode trocar de plano diversas vezes, e o sistema precisa manter o histórico de qual plano estava ativo em cada período (necessário para a tela de Configurações/Planos mostrar corretamente "Plano atual").

#### Atributos de Assinatura

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 40, 91 | vazio, nulo | Código interno da assinatura. |
| usuário | Usuário | Sim | Relacionamento | João da Silva | nulo | Usuário titular da assinatura. |
| plano | Plano | Sim | Relacionamento | Plano Essencial | nulo | Plano contratado. |
| dataInicio | Data | Sim | Simples | 2026-09-01 | data impossível | Início da vigência do plano. |
| dataFim | Data | Não | Simples | 2026-10-01 | data anterior ao início | Preenchida quando o usuário troca ou cancela o plano. |
| status | Texto enumerado | Sim | Simples | ATIVA, CANCELADA, EXPIRADA | qualquer valor fora da lista | Somente uma assinatura por usuário deve estar com status ATIVA. |
| formaPagamento | Texto enumerado | Não | Simples | CARTAO_CREDITO, BOLETO, PIX | valor fora da lista | Não se aplica ao Plano Gratuito. |

### 2.4. Cultura

Cultura representa o tipo de cultivo plantado (Milho, Soja, Feijão, Algodão etc.). É usada tanto para classificar os plantios quanto para definir o cronograma automático de atividades sugerido no assistente "Novo Plantio".

#### Atributos de Cultura

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 2, 3, 4 | vazio, nulo | Código interno da cultura. |
| nome | Texto alfanumérico | Sim | Simples | Milho, Soja, Feijão, Algodão | "" | Nome da cultura. Deve ser único. |
| ativo | Booleano | Sim | Simples | true, false | sim, não | Indica se a cultura pode ser selecionada em novos plantios. |

### 2.5. Atividade Padrão

Atividade Padrão representa o cronograma modelo de cada cultura: quais atividades costumam ocorrer e quantos dias após o plantio elas são esperadas. É essa entidade que alimenta o cálculo automático mostrado na etapa "Datas e Previsões" do assistente de novo plantio (ex.: Irrigação em +7 dias, Adubação de cobertura em +20 dias).

#### Atributos de Atividade Padrão

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 12, 30 | vazio, nulo | Código interno do registro. |
| cultura | Cultura | Sim | Relacionamento | Soja | nulo | Cultura à qual o cronograma modelo pertence. |
| tipoAtividade | Texto enumerado | Sim | Simples | PLANTIO, IRRIGACAO, ADUBACAO, TRATAMENTO, MONITORAMENTO, COLHEITA | qualquer valor fora da lista | Tipo da atividade prevista. |
| offsetDias | Número inteiro | Sim | Simples | 0, 7, 20, 35 | valor negativo | Quantidade de dias após a data de plantio em que a atividade é prevista. |
| ordem | Número inteiro | Sim | Simples | 1, 2, 3 | valor negativo | Ordem de exibição na tabela de previsão. |

### 2.6. Talhão

Talhão representa uma área física da propriedade rural (ex.: Talhão 01, Talhão 02). Um talhão pode ser reaproveitado por diferentes plantios ao longo de diferentes safras.

#### Atributos de Talhão

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 2, 3, 4 | vazio, nulo | Código interno do talhão. |
| nome | Texto alfanumérico | Sim | Simples | Talhão 01, Talhão 02 | "" | Identificação do talhão. Deve ser único por usuário. |
| areaTotal | Número decimal | Não | Simples | 12.5 | valor negativo | Área total do talhão, em hectares. |
| ativo | Booleano | Sim | Simples | true, false | sim, não | Indica se o talhão está disponível para novos plantios. |

### 2.7. Plantio

Plantio é a entidade central do sistema, representa cada "linha do tempo inteligente" cadastrada pelo produtor, reunindo cultura, talhão, datas e status de acompanhamento. É o que aparece na tela "Meus Plantios" e é criado através do assistente "Novo Plantio".

#### Atributos de Plantio

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 55, 120 | vazio, nulo | Código interno do plantio. |
| usuário | Usuário | Sim | Relacionamento | João da Silva | nulo | Dono do plantio. |
| cultura | Cultura | Sim | Relacionamento | Soja | nulo | Cultura plantada. |
| talhão | Talhão | Sim | Relacionamento | Talhão 01 | nulo | Talhão onde o plantio ocorre. |
| nome | Texto alfanumérico | Sim | Simples | Milho Verão 2025/26 | "" | Nome dado ao plantio pelo produtor. |
| safra | Texto | Sim | Simples | 25/26 | vazio | Identificação da safra. |
| descricao | Texto | Não | Simples | Plantio em área de sequeiro | texto ofensivo ou inconsistente | Campo livre (etapa "Informações básicas"). |
| sistemaCultivo | Texto enumerado | Sim | Simples | SEQUEIRO, IRRIGADO | qualquer valor fora da lista | Definido na etapa "Talhão e área". |
| areaPlantada | Número decimal | Sim | Simples | 12.5 | valor negativo | Área efetivamente plantada, em hectares. |
| localizacao | Texto | Não | Simples | Campinas, SP | texto inconsistente | Localização informada na etapa "Talhão e área". |
| altitude | Número inteiro | Não | Simples | 680 | valor negativo | Altitude, em metros. |
| dataPlantio | Data | Sim | Simples | 2026-05-15 | data impossível | Data em que o plantio foi (ou será) realizado. |
| metodoPlantio | Texto enumerado | Não | Simples | PLANTIO_DIRETO, CONVENCIONAL, MINIMO | qualquer valor fora da lista | Método usado no plantio. |
| status | Texto enumerado | Sim | Simples | FASE_INICIAL, DESENVOLVIMENTO, FASE_FINAL, CONCLUIDO | qualquer valor fora da lista | Usado nos badges de status da listagem de plantios. |
| progresso | Número inteiro | Sim | Simples | 0 a 100 | valor fora do intervalo | Percentual exibido na barra de progresso. |
| ativo | Booleano | Sim | Simples | true, false | sim, não | Permite ocultar plantios sem excluir seu histórico. |
| dataCriacao | Data e hora | Sim | Simples | 2026-05-10 08:00:00 | data impossível | Data de criação do registro. |

### 2.8. Atividade

Atividade representa cada tarefa prevista (ou já realizada) dentro da linha do tempo de um plantio, como plantio em si, irrigação, adubação de cobertura, tratos culturais ou colheita. É essa entidade que alimenta o Calendário e a coluna "Próxima Atividade" da tela Meus Plantios.

#### Atributos de Atividade

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 800, 921 | vazio, nulo | Código interno da atividade. |
| plantio | Plantio | Sim | Relacionamento | Milho Verão 2025/26 | nulo | Plantio ao qual a atividade pertence. |
| tipoAtividade | Texto enumerado | Sim | Simples | PLANTIO, IRRIGACAO, ADUBACAO, TRATAMENTO, MONITORAMENTO, COLHEITA | qualquer valor fora da lista | Categoria exibida com cor própria no Calendário. |
| dataPrevista | Data | Sim | Simples | 2026-06-04 | data impossível | Data prevista, calculada a partir da Atividade Padrão ou ajustada manualmente. |
| dataRealizada | Data | Não | Simples | 2026-06-05 | data muito distante da prevista sem justificativa | Preenchida quando a atividade é concluída. |
| status | Texto enumerado | Sim | Simples | PENDENTE, CONCLUIDA, ATRASADA | qualquer valor fora da lista | Calculado com base na data prevista/realizada. |
| observacoes | Texto | Não | Simples | Adubação de cobertura atrasada | texto ofensivo ou inconsistente | Campo livre. |

### 2.9. Alerta

Alerta representa um aviso gerado para o produtor, seja climático (ex.: previsão de chuva forte) ou relacionado a uma atividade (ex.: atividade atrasada). É a entidade por trás da tela Alertas, tanto na listagem quanto na tela de detalhe.

#### Atributos de Alerta

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 900, 1050 | vazio, nulo | Código interno do alerta. |
| usuário | Usuário | Sim | Relacionamento | João da Silva | nulo | Destinatário do alerta. |
| plantio | Plantio | Não | Relacionamento | Milho Verão 2025/26 | plantio inexistente | Nulo quando o alerta não está associado a um plantio específico. |
| talhão | Talhão | Não | Relacionamento | Talhão 01 | talhão inexistente | Referência opcional ao talhão afetado. |
| tipoAlerta | Texto enumerado | Sim | Simples | CLIMATICO, ATIVIDADE | qualquer valor fora da lista | Define o ícone/categoria exibida na listagem. |
| titulo | Texto alfanumérico | Sim | Simples | Chuva intensa prevista | "" | Título curto do alerta. |
| descricao | Texto | Sim | Simples | Risco para a aplicação de defensivos. | texto ofensivo ou inconsistente | Descrição exibida na listagem e no topo do detalhe. |
| prioridade | Texto enumerado | Sim | Simples | CRITICO, ALTA, MEDIA | qualquer valor fora da lista | Define a cor do rótulo de prioridade. |
| situacao | Texto enumerado | Sim | Simples | LIDO, NAO_LIDO | qualquer valor fora da lista | Alterada pelo botão "Marcar como lido". |
| dataAlerta | Data e hora | Sim | Simples | 2026-06-01 08:30:00 | data impossível | Momento em que o alerta foi gerado. |
| origem | Texto enumerado | Sim | Simples | PREVISAO_TEMPO, SISTEMA_MONITORAMENTO | qualquer valor fora da lista | Origem exibida na tela de detalhe. |
| impactoAtividade | Texto | Não | Simples | Aplicação de defensivos | texto inconsistente | Nome da atividade impactada (painel "Impacto nos plantios"). |
| impactoDataLabel | Texto | Não | Simples | Hoje 01/06 | texto inconsistente | Rótulo de data exibido junto ao impacto. |

### 2.10. Recomendação de Alerta

Recomendação de Alerta representa cada item da lista de recomendações mostrada no painel "Recomendações" da tela de detalhe do alerta. Foi modelada como entidade própria (e não como texto único) porque é, por natureza, **multivalorada**: um alerta pode ter várias recomendações.

#### Atributos de Recomendação de Alerta

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 15, 30 | vazio, nulo | Código interno da recomendação. |
| alerta | Alerta | Sim | Relacionamento | Chuva intensa prevista | nulo | Alerta ao qual a recomendação pertence. |
| texto | Texto | Sim | Simples | Evite aplicar defensivos nas próximas 24 horas devido ao risco de chuva. | vazio | Texto exibido com o ícone de check verde. |
| ordem | Número inteiro | Sim | Simples | 1, 2, 3 | valor negativo | Ordem de exibição na lista. |

### 2.11. Previsão do Tempo do Alerta

Previsão do Tempo do Alerta representa cada dia exibido no painel "Previsão do tempo" da tela de detalhe do alerta (ex.: Hoje, Amanhã, Terça, Quarta). Também é multivalorada e composta, já que cada dia carrega várias informações (temperatura máxima/mínima e chance de chuva).

#### Atributos de Previsão do Tempo do Alerta

| Atributo / Dado | Tipo conceitual | Obrigatório | Característica | Ex. aceitos | Ex. não aceitos | Comentários |
| :--- | :---: | :---: | :---: | :--- | :--- | :--- |
| código | Código | Sim | Simples | 1, 22, 300 | vazio, nulo | Código interno do registro. |
| alerta | Alerta | Sim | Relacionamento | Chuva intensa prevista | nulo | Alerta ao qual a previsão pertence. |
| diaLabel | Texto | Sim | Simples | Hoje, Amanhã, Terça, Quarta | "" | Rótulo do dia exibido no card. |
| data | Data | Sim | Simples | 2026-05-01 | data impossível | Data correspondente ao rótulo. |
| temperaturaMax | Número inteiro | Sim | Simples | 28 | valor abaixo da mínima | Temperatura máxima prevista, em °C. |
| temperaturaMin | Número inteiro | Sim | Simples | 20 | valor acima da máxima | Temperatura mínima prevista, em °C. |
| chanceChuva | Número inteiro | Sim | Simples | 0 a 100 | valor fora do intervalo | Percentual de chance de chuva. |

## 3. Tipos especiais, compostos ou específicos

### 3.1. Documento de identificação

Documento de identificação é uma estrutura composta por tipo e valor, usada no cadastro do usuário para fins de cobrança dos planos pagos.

| Atributo / Dado | Tipo conceitual | Obrigatório | Comentários |
| :--- | :--- | :---: | :--- |
| tipo | Texto enumerado | Sim | Valores previstos: CPF ou CNPJ. |
| valor | Texto | Sim | Valor do documento. Deve respeitar a regra do tipo informado quando houver validação disponível. |

### 3.2. Padrões de atributos especiais

| Código | Significado | Uso no dicionário |
| :---: | :--- | :--- |
| C | Composto | Usado quando um atributo possui partes internas (ex.: `documento` do Usuário). |
| M | Multivalorado | Usado quando um atributo pode ter vários valores. |
| MC | Multivalorado e composto | Usado quando há uma lista de estruturas compostas (ex.: recomendações e previsão do tempo de um Alerta). |

## 4. Desdobramento físico ou tecnológico

### 4.1. Desdobramento para banco não relacional orientado a documentos: MongoDB

Em um banco orientado a documentos, a coleção `plantios` pode concentrar os dados do plantio junto com um resumo de suas atividades (já que a tela "Meus Plantios" sempre exibe a "Próxima Atividade" junto com o plantio). Já a coleção `alertas` pode embutir as recomendações e a previsão do tempo, pois esses dados só fazem sentido quando consultados junto com o alerta que os gerou.

#### Coleções sugeridas

| Coleção | Finalidade | Estratégia documental |
| :--- | :--- | :--- |
| `usuarios` | Armazenar conta, preferências e plano atual | Embute a assinatura ativa; histórico de assinaturas fica em coleção própria. |
| `plantios` | Armazenar plantios e suas atividades | Embute a lista de atividades do plantio. |
| `alertas` | Armazenar alertas com seus detalhes | Embute recomendações e previsão do tempo. |
| `assinaturas_historico` | Manter histórico de planos contratados | Documentos próprios, um por período de vigência. |


#### Observações sobre a modelagem documental

- Dados sempre consultados juntos (plantio + suas atividades; alerta + suas recomendações/previsão) podem ser embutidos no mesmo documento.
- Dados com ciclo de vida próprio e reaproveitados por vários registros — como o histórico de assinaturas de um usuário — devem ficar em coleção própria.
- Para consultas frequentes, seriam bons candidatos a índice: `plantios.usuario.codigo`, `plantios.status`, `alertas.usuario.codigo`, `alertas.prioridade`, `alertas.situacao` e `alertas.dataAlerta`.


## 5. Consultas, índices e observações de arquitetura

### 5.1. Consultas esperadas


### 5.2. Regras e observações

- Um plantio deve pertencer a exatamente um usuário, uma cultura e um talhão.
- Um talhão pode ser reutilizado por vários plantios ao longo de diferentes safras, por isso `TALHOES` não guarda dados específicos de uma safra (esses ficam em `PLANTIOS`).
- Uma atividade deve pertencer a um único plantio; seu tipo e data prevista podem ser sugeridos automaticamente a partir de `ATIVIDADES_PADRAO`, mas podem ser ajustados manualmente pelo usuário.
- Um alerta pode ou não estar vinculado a um plantio ou talhão específico.
- Um usuário deve possuir no máximo uma assinatura com status ATIVA por vez; trocas de plano encerram a assinatura anterior (preenchendo `data_fim`) e criam uma nova.
- A situação de um alerta (`LIDO`/`NAO_LIDO`) deve ser apenas atualizada, nunca duplicada em um novo registro.
- Recomendações e previsão do tempo de um alerta representam o estado do momento em que o alerta foi gerado e não devem ser recalculadas retroativamente quando o alerta já foi criado.
- A senha do usuário deve ser armazenada apenas como hash, nunca em texto plano.