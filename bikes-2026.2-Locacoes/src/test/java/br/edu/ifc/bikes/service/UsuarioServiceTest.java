package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.mapper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Regras de usuário: criação (username único) e alteração de senha.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UsuarioMapper usuarioMapper = Mappers.getMapper(UsuarioMapper.class);

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioExistente() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setUsername("zack@email.com");
        u.setPassword("hashAtual");
        return u;
    }

    // ---------------------------------------------------------------- criar

    @Test
    @DisplayName("create: dados válidos salvam o usuário com a senha criptografada")
    void create_dadosValidos_salvaComSenhaCriptografada() {
        var dto = new UsuarioRequestDTO("zack@email.com", "123456");
        when(passwordEncoder.encode("123456")).thenReturn("hashSenha");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioResponseDTO resposta = usuarioService.create(dto);

        assertEquals(1L, resposta.id());
        assertEquals("zack@email.com", resposta.username());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hashSenha", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("create: username duplicado lança UsernameUniqueViolationException")
    void create_usernameDuplicado_lancaExcecao() {
        var dto = new UsuarioRequestDTO("zack@email.com", "123456");
        when(passwordEncoder.encode("123456")).thenReturn("hashSenha");
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        UsernameUniqueViolationException ex = assertThrows(UsernameUniqueViolationException.class,
                () -> usuarioService.create(dto));

        assertTrue(ex.getMessage().contains("zack@email.com"));
    }

    // ---------------------------------------------------------------- buscar / listar

    @Test
    @DisplayName("getById: id existente retorna o usuário")
    void getById_existente_retornaUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente()));

        UsuarioResponseDTO resposta = usuarioService.getById(1L);

        assertEquals("zack@email.com", resposta.username());
    }

    @Test
    @DisplayName("getById: id inexistente lança EntityNotFoundException")
    void getById_inexistente_lancaNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> usuarioService.getById(99L));
    }

    @Test
    @DisplayName("getAll: retorna todos os usuários")
    void getAll_retornaTodos() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioExistente()));

        List<UsuarioResponseDTO> resposta = usuarioService.getAll();

        assertEquals(1, resposta.size());
    }

    // ---------------------------------------------------------------- alteração de senha

    @Test
    @DisplayName("updatePassword: senha atual correta grava o hash da nova senha")
    void updatePassword_dadosValidos_gravaNovoHash() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        when(passwordEncoder.matches("atual1", "hashAtual")).thenReturn(true);
        when(passwordEncoder.encode("nova12")).thenReturn("hashNova");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.updatePassword(1L, "nova12", "nova12", "atual1");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hashNova", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("updatePassword: confirmação diferente da nova senha lança PasswordInvalidException")
    void updatePassword_confirmacaoDivergente_lancaExcecao() {
        PasswordInvalidException ex = assertThrows(PasswordInvalidException.class,
                () -> usuarioService.updatePassword(1L, "nova12", "outra1", "atual1"));

        assertEquals("As senhas não coincidem", ex.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("updatePassword: senha atual incorreta lança PasswordInvalidException e não salva")
    void updatePassword_senhaAtualIncorreta_lancaExcecao() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente()));
        when(passwordEncoder.matches("errada", "hashAtual")).thenReturn(false);

        PasswordInvalidException ex = assertThrows(PasswordInvalidException.class,
                () -> usuarioService.updatePassword(1L, "nova12", "nova12", "errada"));

        assertEquals("Senha atual incorreta", ex.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePassword: usuário inexistente lança EntityNotFoundException")
    void updatePassword_usuarioInexistente_lancaNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> usuarioService.updatePassword(99L, "nova12", "nova12", "atual1"));

        verifyNoInteractions(passwordEncoder);
    }
}
