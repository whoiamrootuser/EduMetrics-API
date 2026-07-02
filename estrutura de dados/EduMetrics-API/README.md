# EduMetrics API

**Sistema de Acompanhamento de Desempenho Acadêmico com Análise de Performance e Benchmarking**

Projeto Integrador — Estruturas de Dados 2026/1 — Curso Superior de Tecnologia em Sistemas para Internet
Instituto Federal do Triângulo Mineiro (IFTM) — Campus Uberlândia Centro

---

## Ficha de Identificação do Grupo

| Campo | Valor |
|-------|-------|
| **Integrante 1** | Matheus Henrique Soares de Oliveira |
| **Integrante 2** | Alexandre Silva Froes |
| **Integrante 3** | Laura Gabriely Barbosa Freitas |
| **Integrante 4** | João Henrique Batista Junior |
| **Repositório GitHub** | https://github.com/Suethamh/Back-End (pasta `estrutura de dados/EduMetrics-API`) |
| **Data de entrega** | 02 / 07 / 2026 |

---

## 1. Visão Geral

O **EduMetrics API** é um sistema RESTful que integra, em um único projeto, todas as estruturas de dados e
técnicas de performance estudadas no semestre. Mais do que funcionar, o objetivo é **funcionar bem**: cada
decisão de estrutura é justificada por complexidade teórica e comprovada por benchmark **JMH**.

O sistema resolve quatro problemas reais de performance:

| Problema de produção | Estrutura aplicada | Resultado esperado |
|----------------------|--------------------|--------------------|
| Latência alta em consultas de aluno | Redis + `@Cacheable` | de ~500 ms para < 5 ms (cache hit) |
| Busca de disciplina lenta (varredura linear) | **Trie** em memória | `O(\|prefixo\|)`, independe de _n_ |
| Relatórios bloqueiam endpoints síncronos | RabbitMQ + `@RabbitListener` | endpoint retorna em < 20 ms (HTTP 202) |
| Sem proteção contra abuso de API | **Rate Limiter** Sliding Window | 100 req/min por IP |

### Conexão com as Unidades do semestre

| Unidade | Estruturas | Onde aparecem |
|---------|-----------|---------------|
| I — Fundamentos | ArrayList, Deque | `ArrayList` nos repositórios; `ArrayDeque` no Rate Limiter |
| II — Árvores/Heaps | Heap / PriorityQueue | ranking Top-K de alunos (`RankingService`) |
| III — Hash Tables | HashMap, HashSet | índice matrícula→aluno; nós da Trie; `Set` de tipos válidos |
| IV — Web/Performance | Redis, RabbitMQ, Trie, LRU, JMH | todo o sistema |

---

## 2. Arquitetura

### Estrutura de pacotes

```
br.edu.iftm.edumetrics
├── EduMetricsApplication.java        # @SpringBootApplication + @EnableCaching
├── config/
│   ├── CacheConfig.java              # RedisCacheManager, TTL, serializer
│   ├── RabbitMQConfig.java           # Exchange, Queue, Binding, DLQ, conversor JSON
│   └── DataLoader.java               # @PostConstruct: popula H2 e (via Trie) o startup
├── controller/
│   ├── AlunoController.java          # CRUD de alunos
│   ├── DisciplinaController.java     # cadastro + autocompletar (Trie)
│   ├── DesempenhoController.java     # notas, desempenho e ranking
│   ├── RelatorioController.java      # POST assíncrono → RabbitMQ
│   └── AdminController.java          # cache stats, rate-limiter stats, health
├── service/
│   ├── AlunoService.java             # @Cacheable/@CachePut/@CacheEvict + LRUCache local
│   ├── DesempenhoService.java        # cálculo de média; cache de resultados
│   ├── RankingService.java           # PriorityQueue (min-heap) para Top-K
│   └── AutocompletarService.java     # Trie carregada no startup
├── messaging/
│   ├── RelatorioProducer.java        # RabbitTemplate.convertAndSend
│   └── RelatorioConsumer.java        # @RabbitListener + handler de DLQ
├── domain/
│   ├── Aluno.java / Disciplina.java / Desempenho.java   # @Entity com @Index
│   └── dto/                          # records imutáveis (ideais para cache)
├── repository/                       # JpaRepository + JOIN FETCH (sem N+1)
├── estruturas/                       # ★ implementadas manualmente
│   ├── LRUCache.java                 # extends LinkedHashMap (accessOrder)
│   ├── Trie.java                     # árvore de prefixos
│   └── RateLimiter.java              # Sliding Window com Deque<Long>
├── security/
│   └── RateLimitFilter.java          # OncePerRequestFilter → HTTP 429
└── benchmark/
    └── EduMetricsBenchmark.java      # suíte JMH
```

### Fluxo das duas camadas de cache de aluno

```
GET /api/alunos/matricula/{mat}   →  LRUCache local (O(1), em memória)  →  miss?  →  Banco (H2 + índice B+)
GET /api/alunos/{id}              →  Redis (@Cacheable, TTL 5 min)      →  miss?  →  Banco
```

---

## 3. Stack

Java 21 · Spring Boot 3.3.5 · Spring Data JPA · Spring Cache · Spring Data Redis · Spring AMQP ·
H2 (dev/test) · Redis 7 (Docker) · RabbitMQ 3 (Docker) · JMH 1.37 · JUnit 5 + Mockito · JaCoCo · Maven.

---

## 4. Como executar

### Pré-requisitos
- **JDK 21+**
- **Docker** + **Docker Compose** (para Redis e RabbitMQ)
- Maven 3.9+ **ou** use o Maven Wrapper incluído (`./mvnw`)

### Passo a passo

```bash
# 1. Subir a infraestrutura (Redis + RabbitMQ)
docker compose up -d

# 2. Compilar e rodar os testes
./mvnw clean verify          # Windows: mvnw.cmd clean verify

# 3. Iniciar a aplicação
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

| Recurso | URL |
|---------|-----|
| Swagger UI (testar a API) | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console (JDBC: `jdbc:h2:mem:edumetrics`, user `sa`) |
| Painel RabbitMQ | http://localhost:15672 (guest/guest) |
| Health (Actuator) | http://localhost:8080/actuator/health |

### Rodar SEM Docker (perfil `local`)

Se não tiver Docker instalado, use o perfil **`local`**: o app roda inteiro sem Redis e sem RabbitMQ —
cache em memória (o "cache hit" continua funcionando) e `POST /api/relatorios` responde 202 com o envio
apenas **simulado**.

```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
# ou, com o jar já empacotado:
java -jar target/edumetrics-api.jar --spring.profiles.active=local
```

> ⚠️ **Para a apresentação oficial** use o Docker (`docker compose up -d`): a rubrica pede demonstrar o
> cache real no Redis, o painel do RabbitMQ e o fluxo da DLQ, que só existem com a infraestrutura no ar.
> O perfil `local` serve para desenvolver/testar rapidamente sem instalar nada.

---

## 5. Endpoints REST

| Método | Endpoint | Status | Descrição |
|--------|----------|--------|-----------|
| POST | `/api/alunos` | 201 | Cadastrar aluno. Valida matrícula única. Indexa o nome na Trie. |
| GET | `/api/alunos/{id}` | 200 | `@Cacheable("alunos")`. 2º acesso vem do Redis. |
| GET | `/api/alunos/matricula/{mat}` | 200 | Busca via **LRUCache** local — O(1). Miss consulta o BD. |
| PUT | `/api/alunos/{id}` | 200 | `@CachePut`: atualiza BD e cache simultaneamente. |
| DELETE | `/api/alunos/{id}` | 204 | `@CacheEvict`: remove do BD e invalida cache. |
| POST | `/api/disciplinas` | 201 | Cadastra disciplina e indexa o nome na Trie. |
| GET | `/api/disciplinas/autocompletar?q={q}` | 200 | `Trie.autocompletar(q, 10)` — O(\|q\|). |
| POST | `/api/desempenhos` | 201 | Registra nota. Invalida cache de ranking (`@CacheEvict`). |
| GET | `/api/alunos/{id}/desempenho` | 200 | `@Cacheable("desempenhos")`. JOIN FETCH (sem N+1). |
| GET | `/api/ranking?top={k}` | 200 | PriorityQueue min-heap O(n log k). `@Cacheable("ranking")`. |
| POST | `/api/relatorios` | 202 | Publica `EventoRelatorio` no RabbitMQ. Retorna `correlationId`. |
| GET | `/api/admin/cache/stats` | 200 | hits, misses, hitRate, entradas do LRUCache. |
| DELETE | `/api/admin/cache` | 204 | `@CacheEvict(allEntries=true)`. |
| GET | `/api/admin/rate-limiter/stats` | 200 | Uso atual do rate limiter para o IP. |
| GET | `/api/admin/health` | 200 | Status de Redis, RabbitMQ e total de termos da Trie. |

### Exemplos `curl`

```bash
# Cadastrar aluno
curl -X POST http://localhost:8080/api/alunos -H "Content-Type: application/json" \
  -d '{"matricula":"20269999","nome":"Novo Aluno","email":"novo@iftm.edu.br","curso":"TSI","periodo":4}'

# Autocompletar disciplinas
curl "http://localhost:8080/api/disciplinas/autocompletar?q=Est"

# Ranking Top-5
curl "http://localhost:8080/api/ranking?top=5"

# Relatório assíncrono (202 Accepted)
curl -X POST http://localhost:8080/api/relatorios -H "Content-Type: application/json" \
  -d '{"alunoId":1,"tipo":"BOLETIM","semestre":"2026/1"}'

# Demonstrar a DLQ: tipo inválido vai para relatorios.dlq após as tentativas
curl -X POST http://localhost:8080/api/relatorios -H "Content-Type: application/json" \
  -d '{"alunoId":1,"tipo":"ERRO","semestre":"2026/1"}'
```

---

## 6. Estruturas de dados customizadas

As três estruturas foram **implementadas manualmente** (nenhuma biblioteca externa para a lógica principal).

| Estrutura | Operação | Complexidade | Como foi obtida |
|-----------|----------|--------------|-----------------|
| `LRUCache<K,V>` | `get` / `put` / evicção | **O(1)** amortizado | `LinkedHashMap(accessOrder=true)` + `removeEldestEntry` |
| `Trie` | `inserir` | **O(\|palavra\|)** | mapa de filhos por caractere |
| `Trie` | `autocompletar` | **O(\|prefixo\| + k)** | descida pelo prefixo + DFS limitado a _k_ |
| `RateLimiter` | `permitir` | **O(1)** amortizado | `Deque<Long>` por cliente (cada timestamp entra/sai 1x) |

---

## 7. Benchmarking JMH

### Como executar

```bash
# Gera o uber-jar com a suíte de benchmarks
./mvnw clean package -Pbenchmark -DskipTests

# Roda todos os benchmarks
java -jar target/benchmarks.jar

# Salva o resultado em JSON (entregável jmh-resultado.json)
java -jar target/benchmarks.jar -rf json -rff jmh-resultado.json

# Roda um benchmark específico
java -jar target/benchmarks.jar EduMetricsBenchmark.buscaLRUCache
```

> Também é possível rodar pela IDE executando o `main` de `EduMetricsBenchmark`.

### Resultados

> Medição de referência neste projeto: JDK 21 (G1GC), `Mode.AverageTime`, execução rápida
> `-f 1 -wi 3 -i 5 -w 1s -r 1s` (1 fork, 3 warmup + 5 medição). Arquivo bruto: [`jmh-resultado.json`](jmh-resultado.json).
> Para o resultado oficial com mais estabilidade, rode a configuração anotada na classe
> (`@Fork(2)`, `@Warmup(5×1s)`, `@Measurement(10×1s)`) — os **valores variam conforme o hardware**.

| Benchmark | Score | Erro ± | Unidade | Análise |
|-----------|------:|-------:|---------|---------|
| `buscaLRUCache` | **0,163** | ±0,011 | µs/op | O(1); overhead da ordenação imperceptível |
| `buscaHashMapDireto` | **0,191** | ±0,077 | µs/op | baseline O(1) (com mais ruído nesta corrida curta) |
| `autocompletarTrie` | **0,801** | ±0,089 | µs/op | O(\|prefixo\|) — independe de _n_ |
| `autocompletarLinear` | **7159,188** | ±282,799 | µs/op | O(n) sobre 50.000 chaves |
| `rateLimiterCheck` | **0,154** | ±0,011 | µs/op | O(1) amortizado |

### Análise qualitativa

1. **LRU Cache vs HashMap direto.** Ambos são O(1) e, como esperado, ficaram na **mesma ordem de grandeza**
   (0,163 vs 0,191 µs/op). Na prática o `LRUCache` ficou até ligeiramente à frente, mas a diferença está
   **dentro da margem de erro** (o HashMap apresentou erro de ±0,077, ~40% do score, indicando ruído na
   corrida curta). A conclusão técnica é: o overhead da lista duplamente encadeada que mantém a ordem de
   acesso (cada `get` reposiciona um nó) e do `synchronizedMap` é **desprezível** frente ao benefício da
   política de evicção — que dá controle de memória (capacidade fixa) sem custo assintótico extra.

2. **Trie vs varredura linear.** Resultado mais expressivo da suíte: **0,801 µs vs 7.159 µs**, ou seja, a Trie
   foi **~8.900× mais rápida**. É a complexidade aparecendo: a Trie é `O(|prefixo|)` — **independe do total de
   palavras** —, enquanto a varredura é `O(n)` e precisa testar `startsWith` em cada uma das 50.000 chaves.
   Com 10.000 disciplinas a varredura continuaria crescendo linearmente, ao passo que a Trie custaria apenas o
   tamanho do prefixo digitado. É exatamente o cenário que justifica a estrutura.

3. **Rate Limiter.** Custou **0,154 µs/op** — bem abaixo de 1 µs, confirmando o O(1) amortizado: cada timestamp
   é inserido (`addLast`) e removido (`pollFirst`) no máximo uma vez. Como o score ficou baixo e estável, o
   `synchronized` por cliente **não** foi gargalo nesta carga. Sob altíssima contenção em um único cliente,
   uma alternativa seria `StampedLock` (lock otimista) ou estruturas lock-free, ao custo de mais complexidade.

4. **Variância (erro ±).** O erro de `autocompletarLinear` (±282 sobre 7.159 ≈ **4%**) e dos benchmarks O(1)
   estáveis (`buscaLRUCache`, `rateLimiterCheck` ≈ 7%) é pequeno e confiável. Já `buscaHashMapDireto` teve erro
   de ~40% — sinal de instabilidade típico de corrida curta (JIT/GC ainda se acomodando). Aumentar forks e
   iterações (configuração anotada) reduz essa variância.

---

## 8. Testes e cobertura (JaCoCo)

```bash
./mvnw clean test            # roda todos os testes
./mvnw clean verify          # testes + verificação de cobertura (>= 80% em domain/service/estruturas)
```

Relatório HTML: `target/site/jacoco/index.html`.

Cobertura por componente (testes unitários com JUnit 5 + Mockito, sem dependência de Docker):

| Classe de teste | Cenários |
|-----------------|----------|
| `LRUCacheTest` | armazenar/recuperar; capacidade; preservar recém-acessado; sequência LRU; thread-safety com contenção real |
| `TrieTest` | prefixo exato; inexistente; limite; case-insensitive; duplicata; preserva formatação |
| `RateLimiterTest` | dentro do limite; bloquear (N+1); independência por cliente; janela deslizante |
| `AlunoServiceTest` | cache hit local (não vai ao banco — `verify(times(1))`); evict do LRU; exceção; indexa Trie ao salvar |
| `RankingServiceTest` | Top-3; k=1; empate; k inválido; média nula; `invalidarRanking()` |
| `DesempenhoServiceTest` | cálculo da nota final; invalida ranking; aluno/disciplina inexistentes |
| `RelatorioProducerTest` | publica no RabbitTemplate (mock); correlationId não-nulo; payload correto |
| `DomainModelTest` | entidades (getters/setters, `equals`/`hashCode`, cálculo da nota); DTOs |
| `CacheIntegrationTest` | **`@SpringBootTest` + `@SpyBean`**: prova `@Cacheable` (2ª chamada não vai ao banco) e `@CacheEvict` (re-consulta após `limparCache`) |
| `EduMetricsApplicationTests` | contexto sobe; DataLoader semeia; Trie carregada; ranking funciona |

| `AutocompletarServiceTest` | `carregarDisciplinas`; `criarDisciplina` (sucesso e código duplicado); `indexar` |

> **63 testes**, 0 falhas (`mvn verify`). Cobertura JaCoCo dos pacotes avaliados (`domain` + `service` + `estruturas`): **98,5%** (bem acima dos 80% exigidos). Toda a suíte roda **sem Docker** (cache em memória, listeners do RabbitMQ desligados no perfil de teste).

---

## 9. Decisões de projeto

- **Maven Wrapper** (`mvnw`) incluído: o projeto compila sem Maven instalado globalmente.
- **`MediaAlunoDTO`** (projeção): a consulta de agregação do ranking usa _constructor expression_ com tipos
  casados ao retorno do JPA (`avg`→`Double`, `count`→`Long`), evitando incompatibilidades de tipo.
- **Cache condicional**: o `RedisCacheManager` só é criado com `spring.cache.type=redis`. No perfil de teste
  (`simple`) o Spring usa cache em memória — por isso a suíte de testes roda **sem Redis**.
- **DLQ demonstrável**: um relatório com `tipo` inválido (ex.: `"ERRO"`) força a exceção no consumer e, após
  as tentativas de reentrega, a mensagem cai em `relatorios.dlq`.
- **Suíte JMH em profile separado** (`-Pbenchmark`): evita conflito entre o _uber-jar_ do shade-plugin e o
  _repackage_ do Spring Boot.
- **`RateLimiter.agora()`**: a fonte de tempo é isolada em método `protected`, permitindo testes
  determinísticos da janela deslizante sem `Thread.sleep`.

---

## 10. Mapa do checklist de avaliação

| Critério | Onde está |
|----------|-----------|
| 3 estruturas sem lib externa | `estruturas/` (LRUCache, Trie, RateLimiter) |
| LRUCache evicção + thread-safety | `LRUCacheTest` |
| Trie no `@PostConstruct` | `AutocompletarService.carregarDisciplinas()` |
| RateLimiter 429 | `RateLimitFilter` |
| `@Cacheable`/`@CacheEvict` | `AlunoService` |
| POST relatório 202 + Consumer + DLQ | `RelatorioController`, `RelatorioConsumer`, `RabbitMQConfig` |
| Sem N+1 | `DesempenhoRepository.findByAlunoIdFetchDisciplina` (JOIN FETCH) |
| Cobertura ≥ 80% | JaCoCo (`./mvnw verify`) |
| Suíte JMH (3 benchmarks) | `EduMetricsBenchmark` (5 benchmarks) |

---

## 11. Referências

1. BLOCH, J. *Effective Java*. 3. ed. Addison-Wesley, 2018.
2. CORMEN, T. H. et al. *Introduction to Algorithms*. 4. ed. MIT Press, 2022.
3. ORACLE. *JMH: Java Microbenchmark Harness*. https://openjdk.org/projects/code-tools/jmh/
4. SPRING. *Cache Abstraction Reference*.
5. REDIS. *Redis Documentation*. https://redis.io/docs/
6. RABBITMQ. *Spring AMQP Reference*.
7. BAUER, C.; KING, G. *Java Persistence with Hibernate*. 2. ed. Manning, 2015. (Cap. 12: N+1)
8. KNUTH, D. E. *The Art of Computer Programming, Vol. 3*. 2. ed. Addison-Wesley, 1998. (Tries)
