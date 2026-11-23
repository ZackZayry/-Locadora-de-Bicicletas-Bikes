package br.edu.ifc.bikes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// O cliente é o dono do usuário autenticado (não vem no corpo).
public record LocacaoRequestDTO(
        // opcional: se não for informada, o sistema escolhe a primeira bicicleta disponível
        Long bicicletaId,

        @NotNull(message = "A data/hora prevista de devolução é obrigatória")
        @Future(message = "A data/hora prevista de devolução deve estar no futuro")
        LocalDateTime dataHoraPrevistaDevolucao,

        @NotNull(message = "Os dados do cartão de crédito são obrigatórios")
        @Valid
        CartaoRequestDTO cartao
) { }
