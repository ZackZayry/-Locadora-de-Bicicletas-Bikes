package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BicicletaMapper {

    // o status nunca vem do DTO: nasce DISPONIVEL e muda pelas regras de locação/devolução/manutenção
    Bicicleta toEntity(BicicletaRequestDTO dto);

    BicicletaResponseDTO toResponse(Bicicleta bicicleta);

    List<BicicletaResponseDTO> toResponse(List<Bicicleta> bicicletas);

    void updateEntity(BicicletaRequestDTO dto, @MappingTarget Bicicleta bicicleta);
}
