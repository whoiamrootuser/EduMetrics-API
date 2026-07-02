package br.edu.iftm.edumetrics.config;

import br.edu.iftm.edumetrics.domain.Aluno;
import br.edu.iftm.edumetrics.domain.Desempenho;
import br.edu.iftm.edumetrics.domain.Disciplina;
import br.edu.iftm.edumetrics.repository.AlunoRepository;
import br.edu.iftm.edumetrics.repository.DesempenhoRepository;
import br.edu.iftm.edumetrics.repository.DisciplinaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Popula o banco H2 com dados iniciais (disciplinas, alunos e desempenhos)
 * no startup. Roda em {@code @PostConstruct} para que as disciplinas existam
 * antes de a {@code AutocompletarService} carregar a Trie (ordenado via
 * {@code @DependsOn("dataLoader")}).
 *
 * <p>Usa os repositorios diretamente (e nao os Services) para evitar acionar o
 * cache Redis durante a carga -- assim a aplicacao inicia mesmo sem o Redis no ar.</p>
 */
@Component
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final String SEMESTRE = "2026/1";

    private final DisciplinaRepository disciplinaRepo;
    private final AlunoRepository alunoRepo;
    private final DesempenhoRepository desempenhoRepo;

    public DataLoader(DisciplinaRepository disciplinaRepo,
                      AlunoRepository alunoRepo,
                      DesempenhoRepository desempenhoRepo) {
        this.disciplinaRepo = disciplinaRepo;
        this.alunoRepo = alunoRepo;
        this.desempenhoRepo = desempenhoRepo;
    }

    @PostConstruct
    public void carregar() {
        if (alunoRepo.count() > 0) {
            return;   // idempotente: nao recarrega se ja houver dados
        }

        List<Disciplina> disciplinas = disciplinaRepo.saveAll(List.of(
                new Disciplina("ED01", "Estrutura de Dados", 4),
                new Disciplina("ALG1", "Algoritmos", 4),
                new Disciplina("PWEB", "Programacao Web", 4),
                new Disciplina("MD01", "Matematica Discreta", 4),
                new Disciplina("BD01", "Banco de Dados", 4),
                new Disciplina("SO01", "Sistemas Operacionais", 4)
        ));

        String[] nomes = {
                "Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Alves",
                "Eduarda Rocha", "Felipe Castro", "Gabriela Nunes", "Henrique Dias",
                "Isabela Pires", "Joao Pedro", "Larissa Gomes", "Marcos Vinicius"
        };

        List<Aluno> alunos = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            String matricula = String.format("20260%04d", i + 1);
            String email = "aluno" + (i + 1) + "@iftm.edu.br";
            alunos.add(new Aluno(matricula, nomes[i], email, "TSI", 4));
        }
        alunos = alunoRepo.saveAll(alunos);

        // Notas determinísticas (seed fixa) para gerar um ranking estavel.
        Random rand = new Random(42);
        List<Desempenho> desempenhos = new ArrayList<>();
        for (Aluno aluno : alunos) {
            for (Disciplina disciplina : disciplinas) {
                // cada aluno cursa ~4 das 6 disciplinas
                if (rand.nextInt(3) == 0) {
                    continue;
                }
                BigDecimal nota1 = nota(rand);
                BigDecimal nota2 = nota(rand);
                desempenhos.add(new Desempenho(aluno, disciplina, nota1, nota2, SEMESTRE));
            }
        }
        desempenhoRepo.saveAll(desempenhos);

        log.info("DataLoader: {} disciplinas, {} alunos e {} desempenhos carregados.",
                disciplinas.size(), alunos.size(), desempenhos.size());
    }

    /** Gera uma nota entre 4.0 e 10.0 com 1 casa decimal. */
    private BigDecimal nota(Random rand) {
        double valor = 4.0 + rand.nextDouble() * 6.0;
        return BigDecimal.valueOf(Math.round(valor * 10.0) / 10.0);
    }
}
