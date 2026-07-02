package br.edu.iftm.edumetrics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Entidade Disciplina. O campo {@code nome} e inserido na Trie no startup
 * (ver {@code AutocompletarService}) para habilitar o autocompletar O(|prefixo|).
 */
@Entity
@Table(name = "disciplinas", indexes = {
        @Index(name = "idx_disc_codigo", columnList = "codigo", unique = true)
})
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;   // ex: "ED01"

    @Column(nullable = false, length = 100)
    private String nome;     // inserido na Trie no startup

    @Column(nullable = false)
    private Integer creditos;

    protected Disciplina() {
        // exigido pelo JPA
    }

    public Disciplina(String codigo, String nome, Integer creditos) {
        this.codigo = codigo;
        this.nome = nome;
        this.creditos = creditos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getCreditos() {
        return creditos;
    }

    public void setCreditos(Integer creditos) {
        this.creditos = creditos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Disciplina other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Disciplina{id=%d, codigo='%s', nome='%s'}".formatted(id, codigo, nome);
    }
}
