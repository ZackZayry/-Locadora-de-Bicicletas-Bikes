package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.entity.StatusLocacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T16:53:29-0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class LocacaoMapperImpl implements LocacaoMapper {

    @Override
    public LocacaoResponseDTO toResponse(Locacao locacao) {
        if ( locacao == null ) {
            return null;
        }

        Long clienteId = null;
        Long bicicletaId = null;
        Long id = null;
        LocalDateTime dataHoraRetirada = null;
        LocalDateTime dataHoraPrevistaDevolucao = null;
        LocalDateTime dataHoraDevolucaoEfetiva = null;
        String cartaoTitular = null;
        String cartaoFinal = null;
        Long minutosUtilizados = null;
        BigDecimal valorTotal = null;
        StatusLocacao status = null;

        clienteId = locacaoClienteId( locacao );
        bicicletaId = locacaoBicicletaId( locacao );
        id = locacao.getId();
        dataHoraRetirada = locacao.getDataHoraRetirada();
        dataHoraPrevistaDevolucao = locacao.getDataHoraPrevistaDevolucao();
        dataHoraDevolucaoEfetiva = locacao.getDataHoraDevolucaoEfetiva();
        cartaoTitular = locacao.getCartaoTitular();
        cartaoFinal = locacao.getCartaoFinal();
        minutosUtilizados = locacao.getMinutosUtilizados();
        valorTotal = locacao.getValorTotal();
        status = locacao.getStatus();

        LocacaoResponseDTO locacaoResponseDTO = new LocacaoResponseDTO( id, clienteId, bicicletaId, dataHoraRetirada, dataHoraPrevistaDevolucao, dataHoraDevolucaoEfetiva, cartaoTitular, cartaoFinal, minutosUtilizados, valorTotal, status );

        return locacaoResponseDTO;
    }

    @Override
    public List<LocacaoResponseDTO> toResponse(List<Locacao> locacoes) {
        if ( locacoes == null ) {
            return null;
        }

        List<LocacaoResponseDTO> list = new ArrayList<LocacaoResponseDTO>( locacoes.size() );
        for ( Locacao locacao : locacoes ) {
            list.add( toResponse( locacao ) );
        }

        return list;
    }

    private Long locacaoClienteId(Locacao locacao) {
        Cliente cliente = locacao.getCliente();
        if ( cliente == null ) {
            return null;
        }
        return cliente.getId();
    }

    private Long locacaoBicicletaId(Locacao locacao) {
        Bicicleta bicicleta = locacao.getBicicleta();
        if ( bicicleta == null ) {
            return null;
        }
        return bicicleta.getId();
    }
}
