# Bikes — API de locação de bicicletas

API REST (Spring Boot + JWT) para gerenciar clientes, bicicletas, locações e devoluções.
Projeto da disciplina *WS com Spring Boot* — IFC Campus Videira.

Este guia mostra **como rodar o projeto e quais requisições enviar no Postman** para cada interação possível com o banco.

---

## Sumário
1. [Como rodar](#1-como-rodar)
2. [Perfis e permissões](#2-perfis-e-permissões)
3. [Configurando o Postman](#3-configurando-o-postman)
4. [Primeiros passos (usuário, login e administrador)](#4-primeiros-passos)
5. [Requisições — Usuários](#5-requisições--usuários)
6. [Requisições — Clientes](#6-requisições--clientes)
7. [Requisições — Bicicletas](#7-requisições--bicicletas)
8. [Requisições — Locações e devoluções](#8-requisições--locações-e-devoluções)
9. [Fluxo completo de exemplo](#9-fluxo-completo-de-exemplo)
10. [Respostas de erro](#10-respostas-de-erro)
11. [Regras de negócio](#11-regras-de-negócio)
12. [Consultas SQL úteis](#12-consultas-sql-úteis)
13. [Relatório de locações (Jasper)](#13-relatório-de-locações-jasper)
14. [Testes automatizados](#14-testes-automatizados)

---

## 1. Como rodar

**Requisitos:** Java 25, MySQL ou MariaDB (XAMPP serve) e Maven (o projeto traz o `mvnw`).

1. Crie o banco (as tabelas são criadas sozinhas na primeira execução):
   ```sql
   CREATE DATABASE bikes;
   ```
2. Confira usuário e senha do banco em `src/main/resources/application.properties`
   (`spring.datasource.username` / `spring.datasource.password`; o padrão do projeto é `root`/`root`).
3. Suba a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
4. A API fica em **`http://localhost:8080`** e a documentação interativa (Swagger) em
   **`http://localhost:8080/docs-bikes.html`**.

> Se mudar as entidades e o banco já tiver tabelas antigas, apague-as (veja a [seção 12](#12-consultas-sql-úteis)); o Hibernate não remove colunas antigas sozinho.

---

## 2. Perfis e permissões

Existem dois perfis: **ADMIN** e **CLIENTE**. Todo usuário criado pela API nasce como `CLIENTE`
(o primeiro administrador é promovido direto no banco, veja a [seção 4](#4-primeiros-passos)).

| Recurso | Rota | Quem pode |
|---|---|---|
| Criar usuário | `POST /api/v1/usuarios` | Público (sem token) |
| Login | `POST /api/v1/auth` | Público (sem token) |
| Buscar usuário por id | `GET /api/v1/usuarios/{id}` | ADMIN, ou o próprio usuário |
| Listar usuários | `GET /api/v1/usuarios` | ADMIN |
| Alterar a própria senha | `PATCH /api/v1/usuarios/{id}` | O próprio usuário |
| Cadastrar-se como cliente | `POST /api/v1/clientes` | CLIENTE |
| Meus dados de cliente | `GET /api/v1/clientes/me` | CLIENTE |
| Listar / buscar / editar / remover cliente | `GET`, `PUT`, `DELETE /api/v1/clientes[/{id}]` | ADMIN |
| Todas as rotas de bicicleta | `/api/v1/bicicletas/**` | ADMIN |
| Solicitar aluguel | `POST /api/v1/locacoes` | CLIENTE |
| Registrar devolução | `POST /api/v1/locacoes/{id}/devolucao` | ADMIN |
| Listar / buscar locações | `GET /api/v1/locacoes[/{id}]` | ADMIN |

Sem token ou com o perfil errado a API responde **403**.

---

## 3. Configurando o Postman

**Opção rápida:** importe a coleção pronta `docs/postman/bikes-povoar-locacoes.postman_collection.json`
(*Import* → selecione o arquivo). Ela cria 1 admin, 4 clientes, 6 bicicletas e 5 locações e já guarda os tokens e ids em variáveis.
Para rodar: *Run collection* (veja a [seção 4](#4-primeiros-passos) para o passo do administrador).

**Opção manual** (para testar uma requisição de cada vez):

1. Crie variáveis de coleção/ambiente: `baseUrl = http://localhost:8080`, `tokenAdmin` e `tokenCliente`.
2. Nas requisições que exigem login, abra a aba **Authorization**, escolha **Bearer Token** e use `{{tokenAdmin}}` ou `{{tokenCliente}}`.
3. Nas requisições com corpo, use **Body → raw → JSON** (o Postman adiciona `Content-Type: application/json`).
4. O token expira em 20 minutos; se vier **403** sem motivo, faça o login de novo.

Nos exemplos abaixo, `{{baseUrl}}` é o endereço da API.

---

## 4. Primeiros passos

### 4.1 Criar o usuário administrador e um usuário cliente
`POST {{baseUrl}}/api/v1/usuarios` — *sem token*
```json
{ "username": "admin@bikes.com", "password": "123456" }
```
```json
{ "username": "maria@email.com", "password": "123456" }
```
> O `username` precisa ser um e-mail único e a senha tem **exatamente 6 caracteres**.

### 4.2 Promover o administrador (uma vez, no banco)
A API não cria administradores. Rode no MySQL:
```sql
UPDATE bikes.usuarios SET role = 'ROLE_ADMIN' WHERE username = 'admin@bikes.com';
```

### 4.3 Fazer login e guardar o token
`POST {{baseUrl}}/api/v1/auth` — *sem token*
```json
{ "username": "admin@bikes.com", "password": "123456" }
```
Resposta:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```
Copie o valor para `tokenAdmin`. Repita com `maria@email.com` para obter `tokenCliente`.

> Dica: no Postman, na aba **Tests** da requisição de login, cole
> `pm.collectionVariables.set('tokenAdmin', pm.response.json().token);` para salvar o token automaticamente.

---

## 5. Requisições — Usuários

| Ação | Requisição | Token |
|---|---|---|
| Criar usuário | `POST {{baseUrl}}/api/v1/usuarios` | — |
| Login | `POST {{baseUrl}}/api/v1/auth` | — |
| Buscar por id | `GET {{baseUrl}}/api/v1/usuarios/1` | admin, ou o próprio usuário |
| Listar todos | `GET {{baseUrl}}/api/v1/usuarios` | admin |
| Alterar a senha | `PATCH {{baseUrl}}/api/v1/usuarios/1` | o próprio usuário (o `1` deve ser o seu id) |

**Alterar a própria senha** — `PATCH {{baseUrl}}/api/v1/usuarios/{id}`
```json
{
  "senhaAtual": "123456",
  "novaSenha": "654321",
  "confirmaSenha": "654321"
}
```
Resposta `200` com os dados do usuário. Erros: `400` se a confirmação não conferir ou a senha atual estiver errada; `403` se o `{id}` não for o seu.

---

## 6. Requisições — Clientes

O cadastro de cliente é vinculado ao **usuário logado**: faça a requisição com o token de um usuário `CLIENTE`.
Cada usuário pode ter **um único** cadastro, e o CPF é único.

**Cadastrar-se como cliente** — `POST {{baseUrl}}/api/v1/clientes` (token do cliente)
```json
{
  "nome": "Maria Silva",
  "cpf": "12345678901",
  "telefone": "47999998888",
  "email": "maria@email.com"
}
```
- Obrigatórios: `nome` (3 a 150 caracteres) e `cpf` (11 dígitos, só números).
- Opcionais: `telefone` (10 ou 11 dígitos) e `email` (se omitido, usa o username do usuário).
- Resposta `201`:
```json
{ "id": 1, "nome": "Maria Silva", "cpf": "12345678901", "telefone": "47999998888", "email": "maria@email.com", "usuarioId": 2 }
```

| Ação | Requisição | Token |
|---|---|---|
| Ver meus dados | `GET {{baseUrl}}/api/v1/clientes/me` | cliente |
| Listar todos | `GET {{baseUrl}}/api/v1/clientes` | admin |
| Buscar por id | `GET {{baseUrl}}/api/v1/clientes/1` | admin |
| Atualizar | `PUT {{baseUrl}}/api/v1/clientes/1` | admin |
| Remover | `DELETE {{baseUrl}}/api/v1/clientes/1` | admin |

**Atualizar cliente** — `PUT {{baseUrl}}/api/v1/clientes/1` (mesmo corpo do cadastro; campos não enviados — `telefone`, `email` — mantêm o valor atual)
```json
{ "nome": "Maria Souza", "cpf": "12345678901", "telefone": "47988887777" }
```

> Remover um cliente que já tem locações retorna **409** (o banco protege o histórico).

---

## 7. Requisições — Bicicletas

Todas exigem o **token do administrador**.

**Criar bicicleta** — `POST {{baseUrl}}/api/v1/bicicletas`
```json
{ "codigo": "BIKE-001", "modelo": "Caloi Explorer" }
```
Resposta `201` (a bicicleta nasce `DISPONIVEL`):
```json
{ "id": 1, "codigo": "BIKE-001", "modelo": "Caloi Explorer", "status": "DISPONIVEL" }
```
O `codigo` é único (diferente do `id`); repetir retorna **409**.

| Ação | Requisição |
|---|---|
| Listar todas | `GET {{baseUrl}}/api/v1/bicicletas` |
| Listar só as disponíveis | `GET {{baseUrl}}/api/v1/bicicletas/disponiveis` |
| Buscar pelo **código** | `GET {{baseUrl}}/api/v1/bicicletas/codigo/BIKE-001` |
| Buscar pelo id | `GET {{baseUrl}}/api/v1/bicicletas/1` |
| Atualizar código/modelo | `PUT {{baseUrl}}/api/v1/bicicletas/1` |
| Alterar o status | `PATCH {{baseUrl}}/api/v1/bicicletas/1/status` |
| Remover | `DELETE {{baseUrl}}/api/v1/bicicletas/1` |

**Atualizar** — `PUT {{baseUrl}}/api/v1/bicicletas/1` (o status não muda por aqui)
```json
{ "codigo": "BIKE-001", "modelo": "Caloi Explorer 2026" }
```

**Enviar para manutenção** — `PATCH {{baseUrl}}/api/v1/bicicletas/1/status`
```json
{ "status": "MANUTENCAO" }
```
**Liberar de volta** — mesmo endereço:
```json
{ "status": "DISPONIVEL" }
```
Regras: `ALUGADA` não pode ser definido manualmente (é automático na locação) e uma bicicleta **alugada** não muda de status até a devolução — ambos retornam **409**.

Status possíveis: `DISPONIVEL`, `ALUGADA`, `MANUTENCAO`.

---

## 8. Requisições — Locações e devoluções

### 8.1 Solicitar um aluguel
`POST {{baseUrl}}/api/v1/locacoes` — **token do cliente** (precisa ter feito o cadastro de cliente antes)

Escolhendo a bicicleta:
```json
{
  "bicicletaId": 1,
  "dataHoraPrevistaDevolucao": "2026-12-01T18:00:00",
  "cartao": {
    "titular": "MARIA SILVA",
    "numero": "4111111111111111",
    "validade": "12/30",
    "cvv": "123"
  }
}
```
Deixando o sistema escolher a primeira bicicleta disponível (omita `bicicletaId`):
```json
{
  "dataHoraPrevistaDevolucao": "2026-12-01T18:00:00",
  "cartao": { "titular": "MARIA SILVA", "numero": "4111111111111111", "validade": "12/30", "cvv": "123" }
}
```
- `dataHoraPrevistaDevolucao`: precisa ser **no futuro**.
- Cartão: número válido (13 a 19 dígitos, passa na verificação de Luhn), `validade` no formato `MM/AA` e **não vencida**, `cvv` com 3 ou 4 dígitos. Cartões de teste: `4111111111111111`, `5555555555554444`, `378282246310005`.
- O **número completo e o CVV não são gravados**; só o titular e os 4 últimos dígitos.

Resposta `201`:
```json
{
  "id": 1, "clienteId": 1, "bicicletaId": 1,
  "dataHoraRetirada": "2026-11-22T16:30:12",
  "dataHoraPrevistaDevolucao": "2026-12-01T18:00:00",
  "dataHoraDevolucaoEfetiva": null,
  "cartaoTitular": "MARIA SILVA", "cartaoFinal": "1111",
  "minutosUtilizados": null, "valorTotal": null,
  "status": "ATIVA"
}
```
A bicicleta passa a `ALUGADA`. Pedir uma bicicleta já alugada, em manutenção, ou quando não há nenhuma disponível retorna **409**.

### 8.2 Registrar a devolução
`POST {{baseUrl}}/api/v1/locacoes/{id}/devolucao` — **token do administrador**

Devolução normal (sem corpo): a bicicleta volta a `DISPONIVEL`.

Devolução com problema — a bicicleta vai para `MANUTENCAO`:
```json
{ "problema": true }
```
Resposta `200`, já com o tempo e o valor a debitar no cartão:
```json
{
  "id": 1, "status": "FINALIZADA",
  "dataHoraDevolucaoEfetiva": "2026-11-22T18:05:00",
  "minutosUtilizados": 95,
  "valorTotal": 20.00,
  "cartaoFinal": "1111"
}
```
Devolver uma locação que já foi devolvida retorna **409**.

### 8.3 Consultar locações (admin)
| Ação | Requisição |
|---|---|
| Listar todas | `GET {{baseUrl}}/api/v1/locacoes` |
| Listar as de um cliente | `GET {{baseUrl}}/api/v1/locacoes?clienteId=1` |
| Buscar por id | `GET {{baseUrl}}/api/v1/locacoes/1` |

---

## 9. Fluxo completo de exemplo

Sequência para ver o sistema inteiro funcionando (cada passo indica o token):

1. `POST /api/v1/usuarios` → cria `admin@bikes.com` e `maria@email.com` *(sem token)*
2. SQL de promoção do admin (seção 4.2)
3. `POST /api/v1/auth` → token do admin e token da Maria
4. `POST /api/v1/clientes` → Maria se cadastra *(token da Maria)*
5. `POST /api/v1/bicicletas` → cria `BIKE-001` *(admin)*
6. `GET /api/v1/bicicletas/disponiveis` → confere que ela está livre *(admin)*
7. `POST /api/v1/locacoes` → Maria aluga *(token da Maria)*
8. `GET /api/v1/bicicletas/codigo/BIKE-001` → status `ALUGADA` *(admin)*
9. `POST /api/v1/locacoes/1/devolucao` → devolução, mostra tempo e valor *(admin)*
10. `GET /api/v1/bicicletas/codigo/BIKE-001` → volta a `DISPONIVEL` *(admin)*
11. `GET /api/v1/locacoes` → histórico *(admin)*

---

## 10. Respostas de erro

Todos os erros têm o mesmo formato:
```json
{
  "path": "/api/v1/clientes",
  "method": "POST",
  "status": 422,
  "statusMessage": "Unprocessable Content",
  "message": "Campo(s) inválido(s)",
  "errors": { "cpf": "O CPF deve conter 11 dígitos numéricos" }
}
```
O campo `errors` (campo → mensagem) só aparece nos erros de validação.

| Código | Quando acontece |
|---|---|
| **400** | JSON malformado, id inválido na URL, senha atual errada / confirmação diferente |
| **403** | Sem token, token expirado ou perfil sem permissão para a rota |
| **404** | Registro não encontrado (cliente, bicicleta, locação, usuário) |
| **409** | Duplicidade (e-mail, CPF, código da bicicleta, usuário já é cliente) ou regra de negócio (bicicleta indisponível, locação já devolvida) |
| **422** | Campos inválidos (a resposta lista quais) ou cartão vencido |
| **500** | Erro inesperado no servidor |

---

## 11. Regras de negócio

- Um usuário tem **no máximo um** cadastro de cliente; o CPF é único.
- A bicicleta tem **código único** e nunca participa de **duas locações ao mesmo tempo** (a linha é travada no banco durante o aluguel).
- **Valor da locação:** R$ 10,00 por **hora iniciada**, com mínimo de 1 hora (ex.: 61 min = 2 h = R$ 20,00).
  O valor por hora fica em `bikes.locacao.valor-hora` (`application.properties`).
- O sistema **apresenta** o valor a debitar; não há integração com operadora de cartão.
- Na devolução a bicicleta volta a `DISPONIVEL`, ou vai para `MANUTENCAO` se houver problema.
- Fuso horário `America/Sao_Paulo` e locale `pt_BR`; as tabelas guardam data/hora de criação e modificação e quem fez a alteração.

---

## 12. Consultas SQL úteis

Para conferir o que a API gravou:
```sql
SELECT * FROM bikes.usuarios;
SELECT * FROM bikes.clientes;
SELECT id, codigo, modelo, status FROM bikes.bicicletas;
SELECT id, cliente_id, bicicleta_id, status, data_hora_retirada,
       data_hora_devolucao_efetiva, minutos_utilizados, valor_total
  FROM bikes.locacoes;
```
Promover um usuário a administrador:
```sql
UPDATE bikes.usuarios SET role = 'ROLE_ADMIN' WHERE username = 'admin@bikes.com';
```
Recomeçar do zero (apaga clientes, bicicletas e locações; mantém os usuários):
```sql
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS locacoes;
DROP TABLE IF EXISTS bicicletas;
DROP TABLE IF EXISTS clientes;
SET FOREIGN_KEY_CHECKS = 1;
```
(reinicie a aplicação para as tabelas serem recriadas).

---

## 13. Relatório de locações (Jasper)

O arquivo `docs/relatorios/relatorio-locacoes.jrxml` abre no **Jaspersoft Studio** (data adapter apontando para o banco `bikes`).
Ele lista: nome do cliente, código da bicicleta, data de retirada e devolução, duração (`hh:mm`) e status.

- O parâmetro de entrada **`LOCACAO_ID`** filtra uma locação específica; deixe vazio para listar todas.
- O relatório lê direto do banco: crie/devolva locações pela API e rode o **Preview** de novo para ver a atualização.

---

## 14. Testes automatizados

```bash
./mvnw test
```
- **Unitários** (JUnit + Mockito): `ClienteServiceTest`, `BicicletaServiceTest`, `LocacaoServiceTest`, `UsuarioServiceTest`.
- **Integração ponta a ponta** (banco H2 em memória): `UsuarioIT`, `ClienteIT`, `BicicletaIT`, `LocacaoIT`.

---

# Anexo — notas sobre a documentação com SpringDoc

(Conteúdo original deste README, mantido como referência.)

### Documentação com SpringDoc OpenAPI 3.1 e Swagger 3

- [Site oficial](https://springdoc.org/) - [Configuração](https://springdoc.org/#getting-started)

Dependência (`pom.xml`):
```
   <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>3.1.1</version>
   </dependency>
```

`Application.properties`
```
   springdoc.swagger-ui.path=/docs-bikes.html
   # Código gerado para acessar a documentação da API no Swagger UI: http://localhost:8080/docs-bikes.html
   springdoc.api-docs.path=/docs-bikes
   springdoc.packages-to-scan=br.edu.ifc.bikes.web.controller
```

- Criar a classe [SpringDocOpenApiConfig](src/main/java/br/edu/ifc/bikes/config/SpringDocOpenApiConfig.java) no pacote `config`.

- Acessar a [documentação](http://localhost:8080/docs-bikes.html).

#### Acrescentando informações em Usuário

- Em [UsuarioController](src/main/java/br/edu/ifc//bikes/web/controller/UsuarioController.java):
   - No cabeçalho
   ```
   @Tag(name = "Usuarios", description = "Contém todas as operações relativas aos recursos para cadastro, edição e leitura de um usuário.")
   ```

   - Em `create()`
   ```
   @Operation(summary = "Criar um novo usuário", description = "Recurso para criar um novo usuário",
            responses = {
                @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
                @ApiResponse(responseCode = "409", description = "Usuário e-mail já cadastrado no sistema",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada invalidos",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
   ```

   - Em `getById()`
   ```
   @Operation(summary = "Recuperar um usuário pelo id", description = "Recuperar um usuário pelo id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
   ```

   - Em `updatePassword()`
   ```
   @Operation(summary = "Atualizar senha", description = "Atualizar senha",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Senha atualizada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class))),
                    @ApiResponse(responseCode = "400", description = "Senha não confere",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
   ```

   - Em `getAll()`
   ```
   @Operation(summary = "Listar todos os usuários", description = "Listar todos os usuários",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista com todos os usuários cadastrados",
                                    content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = UsuarioResponseDto.class))))
            })
   ```
  
## Atividade 01 - Alterar Senha

  - Modificar a **alteração de senha** para que, além do campo `nova senha`, seja informado também a `confirmação de senha` e a `senha atual`.
  - Será necessário criar a classe `UsuarioSenhaDTO.java` para requisição.
    - Não esquecer de validar os campos.
  - Fazer o tratamento de exceção criando a classe `PasswordInvalidException` para validar a senha atual e comparar a nova senha com a confirmação.


## Atividade 02 - Teste de Alterar Senha

  - Desenvolver métodos de teste para as 04 possibilidades: sucesso, senha atual inválida, senha e confirmação não conferem e campos inválidos (vazios, senhas com menos e mais que 6 caracteres).
      - Observar (no Controller) que alteração de senha não retorna um corpo de mensagem, ou seja, o teste não terá retorno.
          - Como não tem retorno, os testes de valores não são necessários.
      - O método para o status será o `isNoContent()`.

## Atividade 03 - Recurso Cliente
Um cliente só pode ser cadastrado se tiver um `usuário` associado. Ou seja, para se cadastrar,
o cliente deve ter um `usuário` cadastrado e autenticado.

**Recurso `Cliente`**:
- id (Long)
- nome (String)
- cpf (String) - deve ser único
- método `equals`

**Funcionalidades**:
- O cadastro só pode ser feito se o usuário estiver autenticado.
- O cliente pode consultar apenas os seus próprios dados.
- O Admin pode consultar os dados de todos os clientes.

**Classes a serem criadas:**
- Classes de DTO
- Controller
- Service
- Repository

**Requisitos**:
- O recurso `Cliente` deve ser protegido com autenticação.
- O recurso `Cliente` deve ser documentado com SpringDoc OpenAPI 3.1 e Swagger 3.
- Fazer validação dos campos de entrada.
- Campos de auditoria (os mesmos de `Usuário`)
