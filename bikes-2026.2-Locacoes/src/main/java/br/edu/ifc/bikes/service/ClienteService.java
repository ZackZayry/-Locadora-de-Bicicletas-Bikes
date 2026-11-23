package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.mapper.ClienteMapper;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.RecursoDuplicadoException;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;

    /**
     * Cadastro de cliente para o usuário autenticado. Regras:
     * 1. o usuário deve existir;
     * 2. um usuário só pode ter um cadastro de cliente;
     * 3. email e CPF não podem estar em uso (se o email não for informado, usa o username do usuário).
     */
    @Transactional
    public ClienteResponseDTO criar(Long usuarioId, ClienteRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(
                () -> new EntityNotFoundException(String.format("Usuário com id=%d não encontrado", usuarioId)));

        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new RecursoDuplicadoException("Este usuário já possui um cadastro de cliente");
        }

        String email = (dto.email() == null || dto.email().isBlank()) ? usuario.getUsername() : dto.email();
        if (clienteRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException(String.format("O email '%s' já está cadastrado", email));
        }
        if (clienteRepository.existsByCpf(dto.cpf())) {
            throw new RecursoDuplicadoException(String.format("O CPF '%s' já está cadastrado", dto.cpf()));
        }

        Cliente cliente = clienteMapper.toEntity(dto);
        cliente.setEmail(email);
        cliente.setUsuario(usuario);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listar() {
        return clienteMapper.toResponse(clienteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return clienteMapper.toResponse(buscarEntidade(id));
    }

    /** Dados do próprio cliente (a partir do usuário autenticado). */
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorUsuarioId(Long usuarioId) {
        return clienteMapper.toResponse(clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Cadastro de cliente não encontrado para o usuário id=%d", usuarioId))));
    }

    @Transactional
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO dto) {
        Cliente cliente = buscarEntidade(id);
        if (dto.email() != null && clienteRepository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new RecursoDuplicadoException(String.format("O email '%s' já está cadastrado", dto.email()));
        }
        if (clienteRepository.existsByCpfAndIdNot(dto.cpf(), id)) {
            throw new RecursoDuplicadoException(String.format("O CPF '%s' já está cadastrado", dto.cpf()));
        }
        clienteMapper.updateEntity(dto, cliente);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void deletar(Long id) {
        clienteRepository.delete(buscarEntidade(id));
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Cliente com id=%d não encontrado", id)));
    }
}
