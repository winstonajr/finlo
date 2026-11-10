# Finlo - Personal Finance Offline-First

Finlo é um sistema de controle financeiro pessoal completo, operando sob o paradigma **Offline-First**. O aplicativo Android funciona integralmente sem conexão e sincroniza dados bidirecionalmente com o backend em Go através de protocolo incremental com resolução determinística de conflitos (LWW - Last Write Wins).

---

## Estrutura do Monorepo

```
/home/win/Documentos/MyProjects/finlo/
├── Makefile               # Automação de compilação, testes e containers
├── docker-compose.yml     # Orquestração do PostgreSQL, MinIO e Backend Go
├── api/
│   └── openapi.yaml       # Especificação OpenAPI 3.0.3 completa
├── backend/               # Backend em Go (net/http, PostgreSQL, Argon2id, JWT)
│   ├── cmd/server/        # Entrypoint do servidor HTTP
│   ├── internal/
│   │   ├── auth/          # Argon2id, JWT HS256 e rotação de Refresh Token
│   │   ├── config/        # Carregamento de variáveis de ambiente
│   │   ├── domain/        # Entidades, DTOs e erros de domínio
│   │   ├── handler/       # Handlers HTTP, middlewares (CORS, Auth, Logger)
│   │   ├── repository/    # Repositório Postgres e In-Memory (fallback/testes)
│   │   ├── storage/       # S3 / MinIO presigned URL generator para comprovantes
│   │   └── sync/          # Motor de sincronização incremental (Push/Pull, LWW)
│   ├── migrations/        # Migração SQL inicial (000001_init_schema.sql)
│   └── Dockerfile         # Multi-stage build otimizado em Alpine Linux
└── android/               # App Android nativo (Kotlin 2.x, Jetpack Compose)
    ├── app/
    │   ├── src/main/java/com/seunome/finlo/
    │   │   ├── core/
    │   │   │   ├── database/     # Room DB, SQLCipher, DAOs e Entidades
    │   │   │   ├── designsystem/ # Material 3, Paleta Finlo, Tipografia
    │   │   │   ├── export/       # Gerador local de CSV, PDF (PdfDocument) e Backup JSON
    │   │   │   ├── network/      # Retrofit, DTOs, AuthInterceptor
    │   │   │   ├── notification/ # Notificações locais de contas a vencer e diário
    │   │   │   ├── security/     # PIN hash, BiometricPrompt, SecureStorage DataStore
    │   │   │   └── sync/         # SyncManager, ConflictResolver LWW, WorkManager
    │   │   ├── data/             # Implementações dos repositórios
    │   │   ├── domain/           # Modelos de domínio e interfaces dos repositórios
    │   │   ├── di/               # Módulos de injeção Dagger Hilt
    │   │   └── presentation/     # Jetpack Compose UI, ViewModels, Canvas Charts
    │   └── src/test/             # Testes unitários de regras de negócio (RN01, RN02, RN03, RN06, RN10)
```

---

## Backend Go

### Tecnologias
- **Linguagem**: Go 1.24+
- **HTTP**: `net/http` nativo de alto desempenho com roteamento por método
- **Autenticação**: Hash de senhas com `argon2id` (RFC 9106) e tokens JWT HS256 com pares de Access Token (15 min) e Refresh Token (30 dias) rotacionáveis
- **Banco de Dados**: PostgreSQL com migrations e suporte a `PostgresRepository` e `MemoryRepository` para testes ultra-rápidos
- **Armazenamento de Anexos**: Geração de URLs pré-assinadas (HMAC-SHA256) compatíveis com AWS S3 e MinIO
- **Sincronização Incremental**:
  - `POST /v1/sync/push`: Lote idempotente com cabeçalho `Idempotency-Key`
  - `GET /v1/sync/pull`: Mudanças monotônicas acima do cursor `server_version`
  - Resolução LWW (`updatedAt` maior vence; desempate por `deviceId`)
  - Proteção contra relógio fraudulento/futuro no cliente

### Como Executar o Backend Localmente

```bash
cd backend
go test -v ./...       # Executa a suíte de testes de integração e unitários
go build ./cmd/server  # Compila o executável do servidor
./server               # Inicia o servidor na porta 8080 (fallback automático para memory se postgres offline)
```

### Executar via Docker Compose

```bash
docker compose up -d --build
```

---

## App Android

### Tecnologias
- **Kotlin 2.x** com Jetpack Compose & Material 3
- **Room Database** com suporte a criptografia SQLCipher
- **WorkManager**: Sincronização periódica em background (15 min) e lembretes diários
- **Canvas Customizado**: Gráficos nativos de Donut (`drawArc`) para despesas por categoria e Barras duplas (`drawRoundRect`) para evolução de receitas vs despesas
- **Segurança**: Autenticação biométrica (`BiometricPrompt`) e PIN de 4 dígitos com salt SHA-256
- **Relatórios**: Geração local de PDF (`android.graphics.pdf.PdfDocument`) e exportação CSV a partir do Room DB

---

## Conformidade com Regras de Negócio
- **RN01**: Saldo calculado como `receitas - despesas - transferências_saída + transferências_entrada`.
- **RN02**: Transferências não entram nas métricas de receita/despesa dos relatórios.
- **RN03**: Valores monetários rigorosamente em centavos (`int64`/`Long`), nunca float.
- **RN04**: Exclusão de categoria com movimentação exige reatribuição transacional no Room.
- **RN05 & RN06**: Transações parceladas e recorrentes geradas no cliente preservando todos os centavos.
- **RN07 & RN08**: Soft delete com `deletedAt` tombstone e IDs UUIDv7.
- **RN09 a RN11**: Servidor monotonicamente incrementa `server_version`; desempate determinístico LWW.
- **RN12**: Relatórios calculados no dispositivo a partir das transações sincronizadas.
