package br.edu.iftm.edumetrics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidade Desempenho: a nota de um aluno em uma disciplina, em um semestre.
 *
 * <p>Os relacionamentos sao {@code LAZY} para evitar carregamentos desnecessarios;
 * as consultas que precisam da disciplina usam {@code JOIN FETCH} explicito para
 * prevenir o problema N+1.</p>
 */
@Entity
@Table(name = "desempenhos", indexes = {
        @Index(name = "idx_desemp_aluno", columnList = "aluno_id"),
        @Index(name = "idx_desemp_aluno_disc", columnList = "aluno_id, disciplina_id")
})
public class Desempenho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    @Column(precision = 5, scale = 2)
    private BigDecimal nota1;

    @Column(precision = 5, scale = 2)
    private BigDecimal nota2;

    @Column(precision = 5, scale = 2)
    private BigDecimal notaFinal;   // calculado: (nota1 + nota2) / 2

    @Column(nullable = false, length = 7)
    private String semestre;        // ex: "2026/1"

    protected Desempenho() {
        // exigido pelo JPA
    }

    public Desempenho(Aluno aluno, Disciplina disciplina,
                      BigDecimal nota1, BigDecimal nota2, String semestre) {
        this.aluno = aluno;
        this.disciplina = disciplina;
        this.nota1 = nota1;
        this.nota2 = nota2;
        this.semestre = semestre;
        recalcularNotaFinal();
    }

    /**
     * Calcula a nota final como a media aritmetica de nota1 e nota2,
     * arredondada para 2 casas decimais. Notas ausentes contam como zero
     * apenas quando a outra existir; se ambas forem nulas, a nota final e nula.
     */
    public void recalcularNotaFinal() {
        if (nota1 == null && nota2 == null) {
            this.notaFinal = null;
            return;
        }
        BigDecimal n1 = nota1 != null ? nota1 : BigDecimal.ZERO;
        BigDecimal n2 = nota2 != null ? nota2 : BigDecimal.ZERO;
        this.notaFinal = n1.add(n2)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public void setAluno(Aluno aluno) {
        this.aluno = aluno;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(Disciplina disciplina) {
        this.disciplina = disciplina;
    }

    public BigDecimal getNota1() {
        return nota1;
    }

    public void setNota1(BigDecimal nota1) {
        this.nota1 = nota1;
        recalcularNotaFinal();
    }

    public BigDecimal getNota2() {
        return nota2;
    }

    public void setNota2(BigDecimal nota2) {
        this.nota2 = nota2;
        recalcularNotaFinal();
    }

    public BigDecimal getNotaFinal() {
        return notaFinal;
    }

    public String getSemestre() {
        return semestre;
    }

    public void setSemestre(String semestre) {
        this.semestre = semestre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Desempenho other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
