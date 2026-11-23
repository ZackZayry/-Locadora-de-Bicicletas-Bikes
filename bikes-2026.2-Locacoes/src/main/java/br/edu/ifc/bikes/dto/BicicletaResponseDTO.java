package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.StatusBicicleta;

public record BicicletaResponseDTO(
        Long id,
        String codigo,
        String modelo,
        StatusBicicleta status
) { }
