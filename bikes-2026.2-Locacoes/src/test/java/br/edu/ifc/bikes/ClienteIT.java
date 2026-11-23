package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteIT extends IntegrationTestSupport {

    private static final String CPF = "12345678901";

    @Test
    @DisplayName("Cliente cadastra-se: 201, vinculado ao usuário autenticado e com email do usuário")
    void cadastrar_comoCliente_retorna201VinculadoAoUsuario() {
        Usuario usuario = criarUsuario("maria@email.com", Usuario.Role.ROLE_CLIENTE);
        String token = token("maria@email.com");

        Map<String, Object> corpo = testClient.post().uri("/api/v1/clientes")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", "Maria Silva", "cpf", CPF, "telefone", "47999998888"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody();

        assertThat(corpo).isNotNull();
        assertThat(corpo.get("id")).isNotNull();
        assertThat(corpo.get("nome")).isEqualTo("Maria Silva");
        assertThat(corpo.get("email")).isEqualTo("maria@email.com");
        assertThat(((Number) corpo.get("usuarioId")).longValue()).isEqualTo(usuario.getId());
    }

    @Test
    @DisplayName("Administrador não pode se cadastrar como cliente: 403")
    void cadastrar_comoAdmin_retorna403() {
        String token = criarAdminEToken();

        testClient.post().uri("/api/v1/clientes")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", "Maria Silva", "cpf", CPF))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Cadastro sem autenticação: 403")
    void cadastrar_semToken_retorna403() {
        testClient.post().uri("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", "Maria Silva", "cpf", CPF))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Um usuário só pode ter um cadastro de cliente: 409 no segundo")
    void cadastrar_duasVezesMesmoUsuario_retorna409() {
        String token = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(token, "Maria Silva", CPF);

        testClient.post().uri("/api/v1/clientes")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", "Maria Silva", "cpf", "98765432100"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("CPF deve ser único: 409 quando outro cliente já usa o CPF")
    void cadastrar_cpfDuplicado_retorna409() {
        cadastrarCliente(criarClienteUsuarioEToken("maria@email.com"), "Maria Silva", CPF);
        String tokenJoao = criarClienteUsuarioEToken("joao@email.com");

        testClient.post().uri("/api/v1/clientes")
                .headers(bearer(tokenJoao))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", "João Souza", "cpf", CPF))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("CPF inválido: 422")
    void cadastrar_cpfInvalido_retorna422() {
        String token = criarClienteUsuarioEToken("maria@email.com");

        testClient.post().uri("/api/v1/clientes")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", "Maria Silva", "cpf", "123"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    @DisplayName("Administrador localiza cliente pelo id: 200")
    void buscarPorId_comoAdmin_retorna200() {
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        Number id = (Number) cadastrarCliente(tokenCliente, "Maria Silva", CPF).get("id");
        String tokenAdmin = criarAdminEToken();

        testClient.get().uri("/api/v1/clientes/{id}", id)
                .headers(bearer(tokenAdmin))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.cpf").isEqualTo(CPF);
    }

    @Test
    @DisplayName("Cliente não pode localizar cliente pelo id: 403")
    void buscarPorId_comoCliente_retorna403() {
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        Number id = (Number) cadastrarCliente(tokenCliente, "Maria Silva", CPF).get("id");

        testClient.get().uri("/api/v1/clientes/{id}", id)
                .headers(bearer(tokenCliente))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Administrador lista todos os clientes: 200")
    void listar_comoAdmin_retorna200() {
        cadastrarCliente(criarClienteUsuarioEToken("maria@email.com"), "Maria Silva", CPF);
        cadastrarCliente(criarClienteUsuarioEToken("joao@email.com"), "João Souza", "98765432100");
        String tokenAdmin = criarAdminEToken();

        List<Map<String, Object>> clientes = testClient.get().uri("/api/v1/clientes")
                .headers(bearer(tokenAdmin))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() { })
                .returnResult().getResponseBody();

        assertThat(clientes).hasSize(2);
    }

    @Test
    @DisplayName("Cliente não pode listar todos os clientes: 403")
    void listar_comoCliente_retorna403() {
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");

        testClient.get().uri("/api/v1/clientes")
                .headers(bearer(tokenCliente))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Cliente recupera os próprios dados: 200")
    void meusDados_comoCliente_retorna200() {
        String token = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(token, "Maria Silva", CPF);

        testClient.get().uri("/api/v1/clientes/me")
                .headers(bearer(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nome").isEqualTo("Maria Silva")
                .jsonPath("$.email").isEqualTo("maria@email.com");
    }

    @Test
    @DisplayName("Usuário sem cadastro de cliente consultando os próprios dados: 404")
    void meusDados_semCadastro_retorna404() {
        String token = criarClienteUsuarioEToken("maria@email.com");

        testClient.get().uri("/api/v1/clientes/me")
                .headers(bearer(token))
                .exchange()
                .expectStatus().isNotFound();
    }
}
