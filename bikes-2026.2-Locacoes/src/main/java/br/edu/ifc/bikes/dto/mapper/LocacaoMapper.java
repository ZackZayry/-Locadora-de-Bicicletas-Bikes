package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Locacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LocacaoMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "bicicletaId", source = "bicicleta.id")
    LocacaoResponseDTO toResponse(Locacao locacao);

    List<LocacaoResponseDTO> toResponse(List<Locacao> locacoes);
}
