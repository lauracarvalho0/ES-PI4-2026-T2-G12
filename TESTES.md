# Como rodar os testes

Os testes ficam no módulo `servidor`:

    cd servidor
    mvn test

O que eles cobrem:

- Validadores de usuário, plantio e atividade.
- Regras de alerta de atividade (`RegrasDeAlerta`).
- Cadastro de plantio de ponta a ponta (validação, `PlantioDAO` e geração de alertas).

Usam um MongoDB em memória (não precisa instalar nada) e um serviço de e-mail falso (Mockito): nenhum e-mail real é enviado e o seu banco (Atlas) não é tocado.

Para rodar uma classe específica:

    mvn test -Dtest=ValidadorPlantioTest

Relatórios dos testes: `servidor/target/surefire-reports/`.
 
