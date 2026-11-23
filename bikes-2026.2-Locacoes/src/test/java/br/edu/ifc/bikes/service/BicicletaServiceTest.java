package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.StatusBicicleta;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.RecursoDuplicadoException;
import br.edu.ifc.bikes.exception.RegraNegocioException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
class BicicletaServiceTest {

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Spy
    private BicicletaMapper bicicletaMapper = Mappers.getMapper(BicicletaMapper.class);

    @InjectMocks
    private BicicletaService bicicletaService;

    private Bicicleta bicicleta(Long id, String codigo, StatusBicicleta status) {
        Bicicleta b = new Bicicleta();
        b.setId(id);
        b.setCodigo(codigo);
        b.setModelo("Explorer");
        b.setStatus(status);
        return b;
    }

    // ---------------------------------------------------------------- criar

    @Test
    @DisplayName("criar: dados válidos salvam a bicicleta com status DISPONIVEL")
    void criar_dadosValidos_salvaComoDisponivel() {
        var dto = new BicicletaRequestDTO("BIKE-001", "Explorer");
        when(bicicletaRepository.existsByCodigo("BIKE-001")).thenReturn(false);
        when(bicicletaRepository.save(any(Bicicleta.class))).thenAnswer(inv -> {
            Bicicleta b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        BicicletaResponseDTO resposta = bicicletaService.criar(dto);

        assertEquals(10L, resposta.id());
        assertEquals("BIKE-001", resposta.codigo());
        assertEquals("Explorer", resposta.modelo());
        assertEquals(StatusBicicleta.DISPONIVEL, resposta.status());
    }

    @Test
    @DisplayName("criar: código duplicado lança RecursoDuplicadoException e não salva")
    void criar_codigoDuplicado_lancaExcecao() {
        var dto = new BicicletaRequestDTO("BIKE-001", "Explorer");
        when(bicicletaRepository.existsByCodigo("BIKE-001")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> bicicletaService.criar(dto));

        assertTrue(ex.getMessage().contains("BIKE-001"));
        verify(bicicletaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- listar / buscar

    @Test
    @DisplayName("listar: retorna todas as bicicletas")
    void listar_retornaTodas() {
        when(bicicletaRepository.findAll()).thenReturn(List.of(
                bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL),
                bicicleta(11L, "BIKE-002", StatusBicicleta.ALUGADA)));

        List<BicicletaResponseDTO> resposta = bicicletaService.listar();

        assertEquals(2, resposta.size());
    }

    @Test
    @DisplayName("listarDisponiveis: retorna apenas bicicletas DISPONIVEIS")
    void listarDisponiveis_retornaBicicletasDisponiveis() {
        when(bicicletaRepository.findByStatus(StatusBicicleta.DISPONIVEL))
                .thenReturn(List.of(bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL)));

        List<BicicletaResponseDTO> resposta = bicicletaService.listarDisponiveis();

        assertEquals(1, resposta.size());
        assertEquals(StatusBicicleta.DISPONIVEL, resposta.get(0).status());
    }

    @Test
    @DisplayName("buscarPorId: id existente retorna a bicicleta")
    void buscarPorId_existente_retornaBicicleta() {
        when(bicicletaRepository.findById(10L))
                .thenReturn(Optional.of(bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL)));

        BicicletaResponseDTO resposta = bicicletaService.buscarPorId(10L);

        assertEquals(10L, resposta.id());
        assertEquals("BIKE-001", resposta.codigo());
    }

    @Test
    @DisplayName("buscarPorId: id inexistente lança EntityNotFoundException")
    void buscarPorId_inexistente_lancaNotFound() {
        when(bicicletaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bicicletaService.buscarPorId(99L));
    }

    @Test
    @DisplayName("buscarPorCodigo: código existente retorna a bicicleta")
    void buscarPorCodigo_existente_retornaBicicleta() {
        when(bicicletaRepository.findByCodigo("BIKE-001"))
                .thenReturn(Optional.of(bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL)));

        BicicletaResponseDTO resposta = bicicletaService.buscarPorCodigo("BIKE-001");

        assertEquals(10L, resposta.id());
        assertEquals("BIKE-001", resposta.codigo());
    }

    @Test
    @DisplayName("buscarPorCodigo: código inexistente lança EntityNotFoundException")
    void buscarPorCodigo_inexistente_lancaNotFound() {
        when(bicicletaRepository.findByCodigo("XXX")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bicicletaService.buscarPorCodigo("XXX"));

        assertTrue(ex.getMessage().contains("XXX"));
    }

    // ---------------------------------------------------------------- atualizar

    @Test
    @DisplayName("atualizar: altera código e modelo sem mexer no status")
    void atualizar_dadosValidos_naoAlteraStatus() {
        Bicicleta existente = bicicleta(10L, "BIKE-001", StatusBicicleta.MANUTENCAO);
        var dto = new BicicletaRequestDTO("BIKE-009", "Barra Circular");
        when(bicicletaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(bicicletaRepository.existsByCodigoAndIdNot("BIKE-009", 10L)).thenReturn(false);
        when(bicicletaRepository.save(any(Bicicleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BicicletaResponseDTO resposta = bicicletaService.atualizar(10L, dto);

        assertEquals("BIKE-009", resposta.codigo());
        assertEquals("Barra Circular", resposta.modelo());
        assertEquals(StatusBicicleta.MANUTENCAO, resposta.status());
    }

    @Test
    @DisplayName("atualizar: código de outra bicicleta lança RecursoDuplicadoException")
    void atualizar_codigoDeOutraBicicleta_lancaExcecao() {
        var dto = new BicicletaRequestDTO("BIKE-002", "Explorer");
        when(bicicletaRepository.findById(10L))
                .thenReturn(Optional.of(bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL)));
        when(bicicletaRepository.existsByCodigoAndIdNot("BIKE-002", 10L)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> bicicletaService.atualizar(10L, dto));

        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: bicicleta inexistente lança EntityNotFoundException")
    void atualizar_inexistente_lancaNotFound() {
        var dto = new BicicletaRequestDTO("BIKE-001", "Explorer");
        when(bicicletaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bicicletaService.atualizar(99L, dto));

        verify(bicicletaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- status

    @Test
    @DisplayName("alterarStatus: bicicleta disponível pode ir para MANUTENCAO")
    void alterarStatus_disponivelParaManutencao_atualiza() {
        Bicicleta existente = bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL);
        when(bicicletaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(existente));
        when(bicicletaRepository.save(any(Bicicleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BicicletaResponseDTO resposta = bicicletaService.alterarStatus(10L, StatusBicicleta.MANUTENCAO);

        assertEquals(StatusBicicleta.MANUTENCAO, resposta.status());
    }

    @Test
    @DisplayName("alterarStatus: bicicleta em MANUTENCAO pode voltar a ficar DISPONIVEL")
    void alterarStatus_manutencaoParaDisponivel_atualiza() {
        Bicicleta existente = bicicleta(10L, "BIKE-001", StatusBicicleta.MANUTENCAO);
        when(bicicletaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(existente));
        when(bicicletaRepository.save(any(Bicicleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BicicletaResponseDTO resposta = bicicletaService.alterarStatus(10L, StatusBicicleta.DISPONIVEL);

        assertEquals(StatusBicicleta.DISPONIVEL, resposta.status());
    }

    @Test
    @DisplayName("alterarStatus: bicicleta ALUGADA não pode ter o status alterado manualmente")
    void alterarStatus_bicicletaAlugada_lancaExcecao() {
        Bicicleta existente = bicicleta(10L, "BIKE-001", StatusBicicleta.ALUGADA);
        when(bicicletaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(existente));

        assertThrows(RegraNegocioException.class,
                () -> bicicletaService.alterarStatus(10L, StatusBicicleta.MANUTENCAO));

        assertEquals(StatusBicicleta.ALUGADA, existente.getStatus());
        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    @DisplayName("alterarStatus: ALUGADA não pode ser definido manualmente")
    void alterarStatus_paraAlugada_lancaExcecao() {
        Bicicleta existente = bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL);
        when(bicicletaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(existente));

        assertThrows(RegraNegocioException.class,
                () -> bicicletaService.alterarStatus(10L, StatusBicicleta.ALUGADA));

        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    @DisplayName("alterarStatus: bicicleta inexistente lança EntityNotFoundException")
    void alterarStatus_inexistente_lancaNotFound() {
        when(bicicletaRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> bicicletaService.alterarStatus(99L, StatusBicicleta.MANUTENCAO));
    }

    // ---------------------------------------------------------------- deletar

    @Test
    @DisplayName("deletar: id existente remove a bicicleta")
    void deletar_existente_removeBicicleta() {
        Bicicleta existente = bicicleta(10L, "BIKE-001", StatusBicicleta.DISPONIVEL);
        when(bicicletaRepository.findById(10L)).thenReturn(Optional.of(existente));

        bicicletaService.deletar(10L);

        verify(bicicletaRepository).delete(existente);
    }

    @Test
    @DisplayName("deletar: id inexistente lança EntityNotFoundException e não remove")
    void deletar_inexistente_lancaNotFound() {
        when(bicicletaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bicicletaService.deletar(99L));

        verify(bicicletaRepository, never()).delete(any());
    }
}
