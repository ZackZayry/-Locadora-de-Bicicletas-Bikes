package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.LocacaoService;
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

@Tag(name = "Locações", description = "Solicitação de aluguel de bicicletas e registro de devoluções.")
@RestController
@RequestMapping("api/v1/locacoes")
@RequiredArgsConstructor
public class LocacaoController {

    private final LocacaoService locacaoService;

    @Operation(summary = "Solicitar o aluguel de uma bicicleta (apenas CLIENTE)",
            description = "O cliente é o usuário autenticado (precisa ter cadastro de cliente). "
                    + "Se bicicletaId não for informado, a primeira bicicleta disponível é utilizada. "
                    + "O número completo e o CVV do cartão não são armazenados.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Locação registrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Sem cadastro de cliente, sem bicicleta disponível ou bicicleta já alugada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Dados inválidos ou cartão vencido",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> solicitar(@AuthenticationPrincipal JwtUserDetails usuario,
                                                        @Valid @RequestBody LocacaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locacaoService.solicitar(usuario.getId(), dto));
    }

    @Operation(summary = "Registrar a devolução da bicicleta (apenas ADMIN)",
            description = "Registra a data/hora efetiva, calcula o tempo de uso e o valor a debitar no cartão "
                    + "(por hora iniciada, mínimo de 1 hora) e libera a bicicleta. "
                    + "Com {\"problema\": true} a bicicleta vai para MANUTENCAO.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Devolução registrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Locação não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Locação já devolvida",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocacaoResponseDTO> devolver(@PathVariable Long id,
                                                       @RequestBody(required = false) DevolucaoRequestDTO dto) {
        boolean problema = dto != null && Boolean.TRUE.equals(dto.problema());
        return ResponseEntity.ok(locacaoService.devolver(id, problema));
    }

    @Operation(summary = "Listar locações (apenas ADMIN)", description = "Todas ou, com clienteId, apenas as do cliente")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LocacaoResponseDTO>> listar(@RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(locacaoService.listar(clienteId));
    }

    @Operation(summary = "Buscar uma locação pelo id (apenas ADMIN)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Locação encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Locação não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocacaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(locacaoService.buscarPorId(id));
    }
}
