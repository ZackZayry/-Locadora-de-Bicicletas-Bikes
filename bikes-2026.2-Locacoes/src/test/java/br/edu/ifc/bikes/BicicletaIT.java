package br.edu.ifc.bikes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BicicletaIT extends IntegrationTestSupport {

    @Test
    @DisplayName("Administrador cria bicicleta: 201 com status DISPONIVEL")
    void criar_comoAdmin_retorna201Disponivel() {
        String token = criarAdminEToken();

        testClient.post().uri("/api/v1/bicicletas")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("codigo", "BIKE-001", "modelo", "Explorer"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.codigo").isEqualTo("BIKE-001")
                .jsonPath("$.status").isEqualTo("DISPONIVEL");
    }

    @Test
    @DisplayName("Cliente não pode criar bicicleta: 403")
    void criar_comoCliente_retorna403() {
        String token = criarClienteUsuarioEToken("maria@email.com");

        testClient.post().uri("/api/v1/bicicletas")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("codigo", "BIKE-001", "modelo", "Explorer"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Cliente não pode listar bicicletas: 403")
    void listar_comoCliente_retorna403() {
        String token = criarClienteUsuarioEToken("maria@email.com");

        testClient.get().uri("/api/v1/bicicletas")
                .headers(bearer(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Código da bicicleta deve ser único: 409")
    void criar_codigoDuplicado_retorna409() {
        String token = criarAdminEToken();
        criarBicicleta(token, "BIKE-001");

        testClient.post().uri("/api/v1/bicicletas")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("codigo", "BIKE-001", "modelo", "Outro"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("Código em branco: 422")
    void criar_codigoEmBranco_retorna422() {
        String token = criarAdminEToken();

        testClient.post().uri("/api/v1/bicicletas")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("codigo", "", "modelo", "Explorer"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    @DisplayName("Bicicleta é localizada pelo código: 200")
    void buscarPorCodigo_existente_retorna200() {
        String token = criarAdminEToken();
        criarBicicleta(token, "BIKE-001");

        testClient.get().uri("/api/v1/bicicletas/codigo/{codigo}", "BIKE-001")
                .headers(bearer(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.codigo").isEqualTo("BIKE-001");
    }

    @Test
    @DisplayName("Código inexistente: 404")
    void buscarPorCodigo_inexistente_retorna404() {
        String token = criarAdminEToken();

        testClient.get().uri("/api/v1/bicicletas/codigo/{codigo}", "XXX")
                .headers(bearer(token))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Administrador envia a bicicleta para manutenção: 200")
    void alterarStatus_paraManutencao_retorna200() {
        String token = criarAdminEToken();
        Number id = (Number) criarBicicleta(token, "BIKE-001").get("id");

        testClient.patch().uri("/api/v1/bicicletas/{id}/status", id)
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "MANUTENCAO"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("MANUTENCAO");
    }

    @Test
    @DisplayName("Status ALUGADA não pode ser definido manualmente: 409")
    void alterarStatus_paraAlugada_retorna409() {
        String token = criarAdminEToken();
        Number id = (Number) criarBicicleta(token, "BIKE-001").get("id");

        testClient.patch().uri("/api/v1/bicicletas/{id}/status", id)
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "ALUGADA"))
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(bicicletaRepository.findByCodigo("BIKE-001")).isPresent();
    }
}
