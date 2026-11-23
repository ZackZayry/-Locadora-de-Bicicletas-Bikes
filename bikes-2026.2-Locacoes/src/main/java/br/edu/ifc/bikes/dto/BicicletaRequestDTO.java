package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Usado na criação (POST) e na atualização (PUT) de bicicletas. O status é controlado pelo sistema.
public record BicicletaRequestDTO(
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 50, message = "O código deve ter no máximo 50 caracteres")
        String codigo,

        @NotBlank(message = "O modelo é obrigatório")
        @Size(max = 100, message = "O modelo deve ter no máximo 100 caracteres")
        String modelo
) { }
