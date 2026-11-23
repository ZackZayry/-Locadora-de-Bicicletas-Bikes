package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.UsuarioLoginDto;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;
import java.util.function.Consumer;

// Base dos testes ponta a ponta: sobe a aplicação em porta aleatória (H2), cria usuários direto no
// repositório (inclusive ADMIN, que não pode ser criado pela API) e obtém o token pelo endpoint de login.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestSupport {

    static final String SENHA = "123456";

    @LocalServerPort
    int port;

    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    ClienteRepository clienteRepository;
    @Autowired
    BicicletaRepository bicicletaRepository;
    @Autowired
    LocacaoRepository locacaoRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    WebTestClient testClient;

    @BeforeEach
    void configurarClient() {
        limparBanco();
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @AfterEach
    void limparAposTeste() {
        limparBanco();
    }

    private void limparBanco() {
        locacaoRepository.deleteAll();
        bicicletaRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    Usuario criarUsuario(String username, Usuario.Role role) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(SENHA));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    String token(String username) {
        Map<String, Object> corpo = testClient.post().uri("/api/v1/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UsuarioLoginDto(username, SENHA))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody();
        return (String) corpo.get("token");
    }

    String criarAdminEToken() {
        criarUsuario("admin@email.com", Usuario.Role.ROLE_ADMIN);
        return token("admin@email.com");
    }

    String criarClienteUsuarioEToken(String username) {
        criarUsuario(username, Usuario.Role.ROLE_CLIENTE);
        return token(username);
    }

    static Consumer<HttpHeaders> bearer(String token) {
        return headers -> headers.setBearerAuth(token);
    }

    // --- atalhos de API usados por mais de um teste ---

    Map<String, Object> cadastrarCliente(String token, String nome, String cpf) {
        return testClient.post().uri("/api/v1/clientes")
                .headers(bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nome", nome, "cpf", cpf))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody();
    }

    Map<String, Object> criarBicicleta(String tokenAdmin, String codigo) {
        return testClient.post().uri("/api/v1/bicicletas")
                .headers(bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("codigo", codigo, "modelo", "Explorer"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() { })
                .returnResult().getResponseBody();
    }
}
