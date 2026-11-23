package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.StatusBicicleta;
import jakarta.validation.constraints.NotNull;

public record BicicletaStatusRequestDTO(
        @NotNull(message = "O status é obrigatório")
        StatusBicicleta status
) { }
