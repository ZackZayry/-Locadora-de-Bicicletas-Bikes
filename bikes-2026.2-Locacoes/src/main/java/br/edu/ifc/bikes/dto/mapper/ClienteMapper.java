package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.entity.Cliente;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClienteMapper {

    // id, usuario e campos de auditoria não vêm do DTO (o service associa o usuário)
    Cliente toEntity(ClienteRequestDTO dto);

    @Mapping(target = "usuarioId", source = "usuario.id")
    ClienteResponseDTO toResponse(Cliente cliente);

    List<ClienteResponseDTO> toResponse(List<Cliente> clientes);

    // campos nulos no DTO (ex.: email/telefone não informados) não apagam o que já existe
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ClienteRequestDTO dto, @MappingTarget Cliente cliente);
}
