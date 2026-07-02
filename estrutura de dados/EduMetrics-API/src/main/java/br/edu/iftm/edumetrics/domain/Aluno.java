package br.edu.iftm.edumetrics.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Aluno.
 *
 * <p>Os indices declarados em {@link Table} criam arvores B+ nas colunas
 * <code>matricula</code> e <code>nome</code> no banco (busca O(log n)),
 * complementando o {@code HashMap} em memoria usado pelo {@code AlunoService}
 * para a busca O(1) por matricula.</p>
 */
@Entity
@Table(name = "alunos", indexes = {
        // Cria arvore B+ na coluna matricula -- busca O(log n) no BD
        @Index(name = "idx_aluno_matricula", columnList = "matricula", unique = true),
        @Index(name = "idx_aluno_nome", columnList = "nome")
})
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String matricula;   // chave do HashMap em memoria

    @Column(nullable = false, length = 120)
    private String nome;        // indexado na Trie

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String curso;

    @Column(nullable = false)
    private Integer periodo;

    @OneToMany(mappedBy = "aluno", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Desempenho> desempenhos = new ArrayList<>();

    protected Aluno() {
        // exigido pelo JPA
    }

    public Aluno(String matricula, String nome, String email, String curso, Integer periodo) {
        this.matricula = matricula;
        this.nome = nome;
        this.email = email;
        this.curso = curso;
        this.periodo = periodo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public Integer getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Integer periodo) {
        this.periodo = periodo;
    }

    public List<Desempenho> getDesempenhos() {
        return desempenhos;
    }

    public void adicionarDesempenho(Desempenho desempenho) {
        desempenhos.add(desempenho);
        desempenho.setAluno(this);
    }

    /**
     * Igualdade baseada na chave primaria (padrao recomendado para entidades JPA).
     * Usa {@code instanceof} para suportar proxies do Hibernate.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Aluno other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    /**
     * hashCode estavel (independe do id, que so e atribuido apos o persist).
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Aluno{id=%d, matricula='%s', nome='%s'}".formatted(id, matricula, nome);
    }
}
