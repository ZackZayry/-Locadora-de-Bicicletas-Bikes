package br.edu.ifc.bikes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocacaoIT extends IntegrationTestSupport {

    // corpo em texto: evita depender da (de)serialização de LocalDateTime no cliente de teste
    private String pedido(Long bicicletaId, String numeroCartao) {
        String prevista = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).plusHours(3).withNano(0).toString();
        String bicicleta = bicicletaId == null ? "" : "\"bicicletaId\": " + bicicletaId + ",";
        return "{" + bicicleta
                + "\"dataHoraPrevistaDevolucao\": \"" + prevista + "\","
                + "\"cartao\": {\"titular\": \"MARIA SILVA\", \"numero\": \"" + numeroCartao
                + "\", \"validade\": \"12/30\", \"cvv\": \"123\"}}";
    }

    private static final String CARTAO_VALIDO = "4111111111111111";

    private WebTestClient.ResponseSpec alugar(String token, Long bicicletaId, String numeroCartao) {
        return testClient.post().uri("/api/v1/locacoes")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(pedido(bicicletaId, numeroCartao))
                .exchange();
    }

    private String statusDaBicicleta(String tokenAdmin, String codigo) {
        Map<String, Object> corpo = testClient.get().uri("/api/v1/bicicletas/codigo/{codigo}", codigo)
                .headers(bearer(tokenAdmin))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody();
        return (String) corpo.get("status");
    }

    @Test
    @DisplayName("Fluxo completo: aluga, bloqueia segunda locação, devolve e libera a bicicleta")
    void alugarEDevolver_fluxoCompleto() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");
        Number bikeId = (Number) criarBicicleta(tokenAdmin, "BIKE-001").get("id");

        // 1) cliente aluga
        Map<String, Object> locacao = alugar(tokenCliente, bikeId.longValue(), CARTAO_VALIDO)
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody();
        assertThat(locacao.get("status")).isEqualTo("ATIVA");
        assertThat(locacao.get("cartaoFinal")).isEqualTo("1111");
        assertThat(locacao.get("dataHoraRetirada")).isNotNull();
        assertThat(locacao.get("dataHoraDevolucaoEfetiva")).isNull();
        Number locacaoId = (Number) locacao.get("id");
        assertThat(statusDaBicicleta(tokenAdmin, "BIKE-001")).isEqualTo("ALUGADA");

        // 2) a mesma bicicleta não pode participar de duas locações simultâneas
        alugar(tokenCliente, bikeId.longValue(), CARTAO_VALIDO)
                .expectStatus().isEqualTo(409);

        // 3) administrador registra a devolução: tempo, valor (mínimo de 1h = 10,00) e data efetiva
        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", locacaoId)
                .headers(bearer(tokenAdmin))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FINALIZADA")
                .jsonPath("$.dataHoraDevolucaoEfetiva").isNotEmpty()
                .jsonPath("$.minutosUtilizados").isNotEmpty()
                .jsonPath("$.valorTotal").isEqualTo(10.0);

        // 4) a bicicleta volta a ficar disponível
        assertThat(statusDaBicicleta(tokenAdmin, "BIKE-001")).isEqualTo("DISPONIVEL");
    }

    @Test
    @DisplayName("Devolução com problema envia a bicicleta para MANUTENCAO")
    void devolver_comProblema_bicicletaVaiParaManutencao() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");
        Number bikeId = (Number) criarBicicleta(tokenAdmin, "BIKE-001").get("id");
        Number locacaoId = (Number) alugar(tokenCliente, bikeId.longValue(), CARTAO_VALIDO)
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody().get("id");

        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", locacaoId)
                .headers(bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("problema", true))
                .exchange()
                .expectStatus().isOk();

        assertThat(statusDaBicicleta(tokenAdmin, "BIKE-001")).isEqualTo("MANUTENCAO");
    }

    @Test
    @DisplayName("Devolver duas vezes a mesma locação: 409")
    void devolver_duasVezes_retorna409() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");
        Number bikeId = (Number) criarBicicleta(tokenAdmin, "BIKE-001").get("id");
        Number locacaoId = (Number) alugar(tokenCliente, bikeId.longValue(), CARTAO_VALIDO)
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody().get("id");

        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", locacaoId)
                .headers(bearer(tokenAdmin)).exchange().expectStatus().isOk();

        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", locacaoId)
                .headers(bearer(tokenAdmin)).exchange().expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("Sem informar a bicicleta, o sistema usa a primeira disponível")
    void alugar_semBicicletaId_usaPrimeiraDisponivel() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");
        criarBicicleta(tokenAdmin, "BIKE-001");

        alugar(tokenCliente, null, CARTAO_VALIDO).expectStatus().isCreated();

        assertThat(statusDaBicicleta(tokenAdmin, "BIKE-001")).isEqualTo("ALUGADA");
    }

    @Test
    @DisplayName("Sem bicicletas disponíveis: 409")
    void alugar_semBicicletasDisponiveis_retorna409() {
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");

        alugar(tokenCliente, null, CARTAO_VALIDO).expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("Usuário sem cadastro de cliente não aluga: 409")
    void alugar_semCadastroDeCliente_retorna409() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        criarBicicleta(tokenAdmin, "BIKE-001");

        alugar(tokenCliente, null, CARTAO_VALIDO).expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("Administrador não pode solicitar aluguel: 403")
    void alugar_comoAdmin_retorna403() {
        String tokenAdmin = criarAdminEToken();
        criarBicicleta(tokenAdmin, "BIKE-001");

        alugar(tokenAdmin, null, CARTAO_VALIDO).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Cartão com número inválido: 422")
    void alugar_cartaoInvalido_retorna422() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");
        criarBicicleta(tokenAdmin, "BIKE-001");

        alugar(tokenCliente, null, "1234567890123456").expectStatus().isEqualTo(422);
    }

    @Test
    @DisplayName("Cliente não pode registrar devolução: 403")
    void devolver_comoCliente_retorna403() {
        String tokenAdmin = criarAdminEToken();
        String tokenCliente = criarClienteUsuarioEToken("maria@email.com");
        cadastrarCliente(tokenCliente, "Maria Silva", "12345678901");
        Number bikeId = (Number) criarBicicleta(tokenAdmin, "BIKE-001").get("id");
        Number locacaoId = (Number) alugar(tokenCliente, bikeId.longValue(), CARTAO_VALIDO)
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody().get("id");

        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", locacaoId)
                .headers(bearer(tokenCliente)).exchange().expectStatus().isForbidden();
    }
}
