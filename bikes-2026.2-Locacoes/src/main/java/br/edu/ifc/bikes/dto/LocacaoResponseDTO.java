package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.StatusLocacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocacaoResponseDTO(
        Long id,
        Long clienteId,
        Long bicicletaId,
        LocalDateTime dataHoraRetirada,
        LocalDateTime dataHoraPrevistaDevolucao,
        LocalDateTime dataHoraDevolucaoEfetiva,
        String cartaoTitular,
        String cartaoFinal,
        Long minutosUtilizados,   // só após a devolução
        BigDecimal valorTotal,    // valor a ser debitado no cartão (só após a devolução)
        StatusLocacao status      // ATIVA ou FINALIZADA
) { }
