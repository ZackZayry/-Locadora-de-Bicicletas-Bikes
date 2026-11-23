package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
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
public class ClienteMapperImpl implements ClienteMapper {

    @Override
    public Cliente toEntity(ClienteRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Cliente cliente = new Cliente();

        cliente.setNome( dto.nome() );
        cliente.setCpf( dto.cpf() );
        cliente.setTelefone( dto.telefone() );
        cliente.setEmail( dto.email() );

        return cliente;
    }

    @Override
    public ClienteResponseDTO toResponse(Cliente cliente) {
        if ( cliente == null ) {
            return null;
        }

        Long usuarioId = null;
        Long id = null;
        String nome = null;
        String cpf = null;
        String telefone = null;
        String email = null;

        usuarioId = clienteUsuarioId( cliente );
        id = cliente.getId();
        nome = cliente.getNome();
        cpf = cliente.getCpf();
        telefone = cliente.getTelefone();
        email = cliente.getEmail();

        ClienteResponseDTO clienteResponseDTO = new ClienteResponseDTO( id, nome, cpf, telefone, email, usuarioId );

        return clienteResponseDTO;
    }

    @Override
    public List<ClienteResponseDTO> toResponse(List<Cliente> clientes) {
        if ( clientes == null ) {
            return null;
        }

        List<ClienteResponseDTO> list = new ArrayList<ClienteResponseDTO>( clientes.size() );
        for ( Cliente cliente : clientes ) {
            list.add( toResponse( cliente ) );
        }

        return list;
    }

    @Override
    public void updateEntity(ClienteRequestDTO dto, Cliente cliente) {
        if ( dto == null ) {
            return;
        }

        if ( dto.nome() != null ) {
            cliente.setNome( dto.nome() );
        }
        if ( dto.cpf() != null ) {
            cliente.setCpf( dto.cpf() );
        }
        if ( dto.telefone() != null ) {
            cliente.setTelefone( dto.telefone() );
        }
        if ( dto.email() != null ) {
            cliente.setEmail( dto.email() );
        }
    }

    private Long clienteUsuarioId(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        if ( usuario == null ) {
            return null;
        }
        return usuario.getId();
    }
}
