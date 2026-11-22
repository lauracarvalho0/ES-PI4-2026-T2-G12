// Giovana Uchelli - 25008818

package agromonitor.backend; // Pasta (pacote) onde a classe fica no projeto

import org.springframework.boot.SpringApplication; // Classe que liga/inicia a aplicação Spring Boot
import org.springframework.boot.autoconfigure.SpringBootApplication; // Anotação que ativa as configurações automáticas do spring

// Marca esta como a classe principal do projeto
@SpringBootApplication
public class BackendApplication
{
    public static void main(String[] args)
    {
        // Liga o servidor do backend
        SpringApplication.run(BackendApplication.class, args);
    }
}
