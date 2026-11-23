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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    // mapper real (gerado pelo MapStruct): testa o service sem simular a conversão
    @Spy
    private ClienteMapper clienteMapper = Mappers.getMapper(ClienteMapper.class);

    @InjectMocks
    private ClienteService clienteService;

    private Usuario usuario(Long id, String username) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("hash");
        return u;
    }

    private Cliente clienteExistente() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Maria Silva");
        c.setCpf("12345678901");
        c.setTelefone("47999998888");
        c.setEmail("maria@email.com");
        c.setUsuario(usuario(10L, "maria@email.com"));
        return c;
    }

    // ================================================================ criar

    @Test
    @DisplayName("criar: dados válidos salvam o cliente vinculado ao usuário")
    void criar_dadosValidos_salvaVinculadoAoUsuario() {
        Usuario usuario = usuario(10L, "maria@email.com");
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", "47999998888", "contato@email.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.existsByUsuarioId(10L)).thenReturn(false);
        when(clienteRepository.existsByEmail("contato@email.com")).thenReturn(false);
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ClienteResponseDTO resposta = clienteService.criar(10L, dto);

        assertEquals(1L, resposta.id());
        assertEquals("Maria Silva", resposta.nome());
        assertEquals("12345678901", resposta.cpf());
        assertEquals("47999998888", resposta.telefone());
        assertEquals("contato@email.com", resposta.email());
        assertEquals(10L, resposta.usuarioId());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertSame(usuario, captor.getValue().getUsuario());
    }

    @Test
    @DisplayName("criar: sem email informado usa o username do usuário")
    void criar_semEmail_usaUsernameDoUsuario() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, null);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "maria@email.com")));
        when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resposta = clienteService.criar(10L, dto);

        assertEquals("maria@email.com", resposta.email());
    }

    @Test
    @DisplayName("criar: usuário inexistente lança EntityNotFoundException")
    void criar_usuarioInexistente_lancaNotFound() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, null);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.criar(99L, dto));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar: usuário que já tem cadastro de cliente lança RecursoDuplicadoException")
    void criar_usuarioJaPossuiCliente_lancaExcecao() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, null);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "maria@email.com")));
        when(clienteRepository.existsByUsuarioId(10L)).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> clienteService.criar(10L, dto));

        assertTrue(ex.getMessage().contains("já possui"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar: email duplicado lança RecursoDuplicadoException e não salva")
    void criar_emailDuplicado_lancaExcecaoENaoSalva() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, "maria@email.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "maria@email.com")));
        when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> clienteService.criar(10L, dto));

        assertTrue(ex.getMessage().contains("maria@email.com"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar: CPF duplicado lança RecursoDuplicadoException e não salva")
    void criar_cpfDuplicado_lancaExcecaoENaoSalva() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, "maria@email.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "maria@email.com")));
        when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> clienteService.criar(10L, dto));

        verify(clienteRepository, never()).save(any());
    }

    // ================================================================ listar / buscar

    @Test
    @DisplayName("listar: retorna todos os clientes como DTOs")
    void listar_retornaTodos() {
        Cliente outro = clienteExistente();
        outro.setId(2L);
        outro.setEmail("joao@email.com");
        when(clienteRepository.findAll()).thenReturn(List.of(clienteExistente(), outro));

        List<ClienteResponseDTO> resposta = clienteService.listar();

        assertEquals(2, resposta.size());
        assertEquals("maria@email.com", resposta.get(0).email());
        assertEquals("joao@email.com", resposta.get(1).email());
    }

    @Test
    @DisplayName("buscarPorId: id existente retorna o cliente")
    void buscarPorId_existente_retornaCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente()));

        ClienteResponseDTO resposta = clienteService.buscarPorId(1L);

        assertEquals(1L, resposta.id());
        assertEquals("Maria Silva", resposta.nome());
    }

    @Test
    @DisplayName("buscarPorId: id inexistente lança EntityNotFoundException")
    void buscarPorId_inexistente_lancaNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> clienteService.buscarPorId(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("buscarPorUsuarioId: retorna os dados do próprio cliente")
    void buscarPorUsuarioId_existente_retornaCliente() {
        when(clienteRepository.findByUsuarioId(10L)).thenReturn(Optional.of(clienteExistente()));

        ClienteResponseDTO resposta = clienteService.buscarPorUsuarioId(10L);

        assertEquals(1L, resposta.id());
        assertEquals(10L, resposta.usuarioId());
    }

    @Test
    @DisplayName("buscarPorUsuarioId: usuário sem cadastro lança EntityNotFoundException")
    void buscarPorUsuarioId_semCadastro_lancaNotFound() {
        when(clienteRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.buscarPorUsuarioId(10L));
    }

    // ================================================================ atualizar

    @Test
    @DisplayName("atualizar: dados válidos alteram o cadastro e mantêm o vínculo com o usuário")
    void atualizar_dadosValidos_atualizaMantendoUsuario() {
        var dto = new ClienteRequestDTO("Maria Souza", "11122233344", "47988887777", "maria.souza@email.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente()));
        when(clienteRepository.existsByEmailAndIdNot("maria.souza@email.com", 1L)).thenReturn(false);
        when(clienteRepository.existsByCpfAndIdNot("11122233344", 1L)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resposta = clienteService.atualizar(1L, dto);

        assertEquals("Maria Souza", resposta.nome());
        assertEquals("11122233344", resposta.cpf());
        assertEquals("47988887777", resposta.telefone());
        assertEquals("maria.souza@email.com", resposta.email());
        assertEquals(10L, resposta.usuarioId());
    }

    @Test
    @DisplayName("atualizar: sem email no DTO mantém o email atual e não consulta duplicidade de email")
    void atualizar_semEmail_mantemEmailAtual() {
        var dto = new ClienteRequestDTO("Maria Souza", "12345678901", null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente()));
        when(clienteRepository.existsByCpfAndIdNot("12345678901", 1L)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO resposta = clienteService.atualizar(1L, dto);

        assertEquals("maria@email.com", resposta.email());
        assertEquals("47999998888", resposta.telefone());
        verify(clienteRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    @DisplayName("atualizar: email de outro cliente lança RecursoDuplicadoException")
    void atualizar_emailDeOutroCliente_lancaExcecao() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, "joao@email.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente()));
        when(clienteRepository.existsByEmailAndIdNot("joao@email.com", 1L)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> clienteService.atualizar(1L, dto));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: CPF de outro cliente lança RecursoDuplicadoException")
    void atualizar_cpfDeOutroCliente_lancaExcecao() {
        var dto = new ClienteRequestDTO("Maria Silva", "99999999999", null, null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteExistente()));
        when(clienteRepository.existsByCpfAndIdNot("99999999999", 1L)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> clienteService.atualizar(1L, dto));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: id inexistente lança EntityNotFoundException")
    void atualizar_inexistente_lancaNotFound() {
        var dto = new ClienteRequestDTO("Maria Silva", "12345678901", null, null);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.atualizar(99L, dto));

        verify(clienteRepository, never()).save(any());
    }

    // ================================================================ deletar

    @Test
    @DisplayName("deletar: id existente remove o cliente")
    void deletar_existente_removeCliente() {
        Cliente cliente = clienteExistente();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.deletar(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    @DisplayName("deletar: id inexistente lança EntityNotFoundException e não remove")
    void deletar_inexistente_lancaNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.deletar(99L));

        verify(clienteRepository, never()).delete(any());
    }
}
