package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.BicicletaStatusRequestDTO;
import br.edu.ifc.bikes.service.BicicletaService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Todas as ações exigem autenticação e são restritas ao administrador.
@Tag(name = "Bicicletas", description = "Cadastro e manutenção de bicicletas (apenas ADMIN).")
@RestController
@RequestMapping("api/v1/bicicletas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BicicletaController {

    private final BicicletaService bicicletaService;

    @Operation(summary = "Criar uma nova bicicleta",
            description = "O código deve ser único. A bicicleta nasce com status DISPONIVEL.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Bicicleta criada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "409", description = "Código já cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    public ResponseEntity<BicicletaResponseDTO> criar(@Valid @RequestBody BicicletaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bicicletaService.criar(dto));
    }

    @Operation(summary = "Listar todas as bicicletas")
    @GetMapping
    public ResponseEntity<List<BicicletaResponseDTO>> listar() {
        return ResponseEntity.ok(bicicletaService.listar());
    }

    @Operation(summary = "Listar bicicletas disponíveis", description = "Bicicletas com status DISPONIVEL, aptas a serem alugadas")
    @GetMapping("/disponiveis")
    public ResponseEntity<List<BicicletaResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(bicicletaService.listarDisponiveis());
    }

    @Operation(summary = "Buscar uma bicicleta pelo código",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bicicleta encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<BicicletaResponseDTO> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(bicicletaService.buscarPorCodigo(codigo));
    }

    @Operation(summary = "Buscar uma bicicleta pelo id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bicicleta encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    public ResponseEntity<BicicletaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bicicletaService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar código e modelo da bicicleta",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bicicleta atualizada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Código já cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PutMapping("/{id}")
    public ResponseEntity<BicicletaResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody BicicletaRequestDTO dto) {
        return ResponseEntity.ok(bicicletaService.atualizar(id, dto));
    }

    @Operation(summary = "Alterar o status da bicicleta",
            description = "Permite enviar para MANUTENCAO ou liberar (DISPONIVEL). ALUGADA é definido apenas pela locação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status alterado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Bicicleta em locação ativa ou status inválido",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PatchMapping("/{id}/status")
    public ResponseEntity<BicicletaResponseDTO> alterarStatus(@PathVariable Long id,
                                                              @Valid @RequestBody BicicletaStatusRequestDTO dto) {
        return ResponseEntity.ok(bicicletaService.alterarStatus(id, dto.status()));
    }

    @Operation(summary = "Remover uma bicicleta",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Bicicleta removida"),
                    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Bicicleta possui locações registradas",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        bicicletaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
