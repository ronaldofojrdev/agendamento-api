# Agendamento API

API REST para empresas que trabalham com serviços agendados. Cada empresa tem seus usuários, serviços e agenda. O mesmo banco atende todas as empresas, com os dados separados pelo `empresa_id`.

## Tecnologias

- Java 17 e Spring Boot 3.5
- Spring Security e JWT
- Spring JDBC e PostgreSQL 17
- Flyway para versionar o banco
- Swagger / OpenAPI
- JUnit 5, MockMvc e H2 nos testes locais
- Docker Compose e GitHub Actions

## Executar com Docker

É necessário ter Docker e Docker Compose em execução.

1. Copie `.env.example` para `.env`.
2. Defina `DB_PASSWORD` e `JWT_SECRET`. A chave JWT deve ser aleatória e ter pelo menos 32 bytes. Não publique o `.env`.
3. Execute:

```bash
docker compose up --build
```

A API fica em `http://localhost:8080` e o Swagger em [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

O Flyway cria as tabelas na primeira inicialização. Os dados do PostgreSQL ficam em um volume do Docker.

## Executar sem Docker para a API

Com Java 17, Maven 3.9+ e um PostgreSQL disponível, configure as variáveis no terminal:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/agendamento"
$env:DB_USER = "agendamento"
$env:DB_PASSWORD = "sua-senha-local"
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
mvn spring-boot:run
```

O Spring não carrega o arquivo `.env` automaticamente nesse modo; ele é usado pelo Docker Compose.

## Fluxo de uso

### 1. Cadastrar a empresa

`POST /api/empresas` é público e cria a empresa junto com seu primeiro administrador.

```json
{
  "nome": "Barbearia Centro",
  "slug": "barbearia-centro",
  "adminNome": "Rafael",
  "adminEmail": "rafael@exemplo.com",
  "adminSenha": "SenhaLocal123"
}
```

### 2. Fazer login

`POST /api/auth/login`:

```json
{
  "empresaSlug": "barbearia-centro",
  "email": "rafael@exemplo.com",
  "senha": "SenhaLocal123"
}
```

A resposta contém `accessToken`, `tokenType` e `expiresIn`. O token dura uma hora. Envie `Authorization: Bearer <accessToken>` nas demais chamadas. No Swagger, clique em **Authorize** e informe apenas o token.

### 3. Cadastrar profissional e cliente

Use o token de admin em `POST /api/usuarios`:

```json
{
  "nome": "Lucas",
  "email": "lucas@exemplo.com",
  "senha": "SenhaLocal123",
  "papel": "PROFISSIONAL"
}
```

Repita com outro nome e e-mail usando `"papel": "CLIENTE"`. Guarde os IDs retornados. Não é preciso informar a empresa: ela vem do token.

### 4. Cadastrar serviço

`POST /api/servicos`, com token de admin:

```json
{
  "nome": "Corte de cabelo",
  "duracaoMinutos": 30,
  "preco": 45.00
}
```

### 5. Agendar

`POST /api/agendamentos`, com token de admin ou do profissional:

```json
{
  "servicoId": "UUID-DO-SERVICO",
  "profissionalId": "UUID-DO-PROFISSIONAL",
  "clienteId": "UUID-DO-CLIENTE",
  "inicio": "2030-06-10T14:00:00-03:00"
}
```

Substitua os IDs pelos valores das respostas e use uma data futura. Com token de cliente, omita `clienteId`: o agendamento será feito para o próprio usuário. O fim é calculado pela duração do serviço.

### 6. Consultar e cancelar

```text
GET /api/agendamentos?de=2030-06-10T00:00:00-03:00&ate=2030-06-11T00:00:00-03:00
PATCH /api/agendamentos/{id}/cancelar
```

O filtro inclui `de` e exclui `ate`. O período máximo é de 31 dias e cada listagem retorna até 200 registros.

## Permissões

| Ação | Admin | Profissional | Cliente |
| --- | --- | --- | --- |
| Cadastrar e listar usuários | Sim | Não | Não |
| Cadastrar, editar e desativar serviços | Sim | Não | Não |
| Consultar serviços e profissionais | Sim | Sim | Sim |
| Criar agendamentos | Para clientes da empresa | Na própria agenda | Para si |
| Consultar e cancelar agendamentos | Da empresa | Da própria agenda | Próprios |

O cadastro de usuários é feito pelo administrador. O e-mail é único dentro da empresa, podendo existir em empresas diferentes.

## Endpoints

| Método | Rota | Finalidade |
| --- | --- | --- |
| POST | `/api/empresas` | Criar empresa e primeiro admin |
| GET | `/api/empresas/atual` | Consultar a empresa do token |
| POST | `/api/auth/login` | Obter JWT |
| POST / GET | `/api/usuarios` | Cadastrar / listar usuários |
| GET | `/api/usuarios/profissionais` | Listar IDs e nomes dos profissionais |
| POST / GET | `/api/servicos` | Cadastrar / listar serviços ativos |
| PUT | `/api/servicos/{id}` | Editar serviço |
| DELETE | `/api/servicos/{id}` | Desativar serviço |
| POST / GET | `/api/agendamentos` | Criar / consultar agenda |
| PATCH | `/api/agendamentos/{id}/cancelar` | Cancelar agendamento |

## Regras da agenda

- O início precisa estar no futuro, com fuso horário informado.
- Serviços duram entre 5 e 480 minutos. O preço não pode ser negativo.
- Um profissional não pode ter horários sobrepostos. Um atendimento pode começar exatamente quando o anterior termina.
- O cadastro bloqueia a linha do profissional durante a transação. Assim, duas requisições simultâneas não aprovam o mesmo horário, mesmo com mais de uma instância da API.
- Agendamentos cancelados liberam o horário e permanecem no histórico.
- Desativar um serviço mantém os agendamentos anteriores. Alterar sua duração não muda o fim das reservas já feitas.
- A empresa é obtida do JWT. Consultas e alterações usam esse identificador; IDs de outra empresa retornam 404.

Os conflitos retornam `409`, dados inválidos retornam `400`, ausência de autenticação retorna `401` e falta de permissão retorna `403`.

## Organização

```text
src/main/java/br/com/agendamento
  controller/   endpoints HTTP
  service/      regras de negócio e transações
  repository/   consultas SQL
  model/        dados da aplicação
  dto/          entradas e validações
  security/     JWT e configuração de acesso
  exception/    respostas de erro
  config/       documentação OpenAPI
src/main/resources/db/migration
  V1__criar_tabelas.sql
```

As consultas usam Spring JDBC e SQL explícito. Os dados são representados por records, sem Lombok.

## Testes

```bash
mvn verify
```

Localmente, os testes sobem a aplicação com H2 em modo PostgreSQL, executam a migração Flyway e fazem chamadas com MockMvc. Não é necessário banco externo. A suíte verifica autenticação, papéis, isolamento entre empresas, conflitos, concorrência, cancelamento, validação e Swagger.

O GitHub Actions executa a mesma suíte com PostgreSQL 17. Para rodá-la contra um banco PostgreSQL local dedicado aos testes, defina `TEST_DB_URL`, `TEST_DB_USER` e `TEST_DB_PASSWORD` antes de executar `mvn verify`.

## Escopo desta versão

Não há controle de expediente, feriados, associação de especialidades a profissionais, pagamentos, recuperação de senha ou refresh token. Qualquer profissional da empresa pode atender qualquer serviço ativo. O conflito é controlado por profissional; um cliente pode ter reservas simultâneas com profissionais diferentes. A listagem possui limite de 200 itens, sem paginação.

Para disponibilizar na internet, configure HTTPS, uma chave JWT própria e limites de requisição no proxy, especialmente nas rotas públicas de cadastro e login.
