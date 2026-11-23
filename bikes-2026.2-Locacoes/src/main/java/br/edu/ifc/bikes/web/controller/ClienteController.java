package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.ClienteService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clientes", description = "Cadastro de clientes. O cliente é vinculado ao usuário autenticado.")
@RestController
@RequestMapping("api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Cadastrar-se como cliente (apenas CLIENTE)",
            description = "Vincula o cadastro ao usuário autenticado. Um usuário só pode ter um cadastro de cliente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Cliente cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Apenas usuários com perfil CLIENTE podem se cadastrar"),
                    @ApiResponse(responseCode = "409", description = "Usuário já possui cadastro, ou email/CPF já cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> criar(@AuthenticationPrincipal JwtUserDetails usuario,
                                                    @Valid @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(usuario.getId(), dto));
    }

    @Operation(summary = "Recuperar os próprios dados (apenas CLIENTE)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dados do cliente autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "O usuário ainda não possui cadastro de cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> meusDados(@AuthenticationPrincipal JwtUserDetails usuario) {
        return ResponseEntity.ok(clienteService.buscarPorUsuarioId(usuario.getId()));
    }

    @Operation(summary = "Listar todos os clientes (apenas ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @Operation(summary = "Buscar um cliente pelo id (apenas ADMIN)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar dados de um cliente (apenas ADMIN)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cliente atualizado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Email ou CPF já cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(id, dto));
    }

    @Operation(summary = "Remover um cliente (apenas ADMIN)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Cliente removido"),
                    @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Cliente possui locações registradas",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
