package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.CartaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.mapper.LocacaoMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.entity.StatusBicicleta;
import br.edu.ifc.bikes.entity.StatusLocacao;
import br.edu.ifc.bikes.exception.CartaoInvalidoException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.RegraNegocioException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocacaoServiceTest {

    // "agora" fixo da retirada: 18/11/2026 10:00
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 18, 10, 0);
    private static final BigDecimal VALOR_HORA = new BigDecimal("10.00");
    private static final Long USUARIO_ID = 10L;

    @Mock
    private LocacaoRepository locacaoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    private final LocacaoMapper locacaoMapper = Mappers.getMapper(LocacaoMapper.class);

    // monta o service com o relógio parado em um instante qualquer
    private LocacaoService service(LocalDateTime agora) {
        Clock clock = Clock.fixed(agora.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new LocacaoService(locacaoRepository, clienteRepository, bicicletaRepository,
                locacaoMapper, clock, VALOR_HORA);
    }

    // ---------------------------------------------------------------- helpers

    private Cliente cliente(Long id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome("Maria Silva");
        c.setEmail("maria@email.com");
        c.setCpf("12345678901");
        return c;
    }

    private Bicicleta bicicleta(Long id, StatusBicicleta status) {
        Bicicleta b = new Bicicleta();
        b.setId(id);
        b.setCodigo("BIKE-" + id);
        b.setModelo("Explorer");
        b.setStatus(status);
        return b;
    }

    // número de teste padrão (passa no algoritmo de Luhn)
    private CartaoRequestDTO cartao() {
        return new CartaoRequestDTO("MARIA SILVA", "4111111111111111", "12/30", "123");
    }

    private LocacaoRequestDTO pedido(Long bicicletaId) {
        return new LocacaoRequestDTO(bicicletaId, AGORA.plusHours(2), cartao());
    }

    private Locacao locacaoAtiva(Bicicleta bicicleta) {
        Locacao l = new Locacao();
        l.setId(100L);
        l.setCliente(cliente(1L));
        l.setBicicleta(bicicleta);
        l.setStatus(StatusLocacao.ATIVA);
        l.setDataHoraRetirada(AGORA);
        l.setDataHoraPrevistaDevolucao(AGORA.plusHours(2));
        l.setCartaoTitular("MARIA SILVA");
        l.setCartaoFinal("1111");
        return l;
    }

    // ================================================================ solicitar

    @Test
    @DisplayName("solicitar: bicicleta disponível gera locação ativa e marca a bicicleta como ALUGADA")
    void solicitar_bicicletaDisponivel_registraLocacaoEAlugaBicicleta() {
        Bicicleta bike = bicicleta(5L, StatusBicicleta.DISPONIVEL);
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente(1L)));
        when(bicicletaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(bike));
        when(locacaoRepository.existsByBicicletaIdAndStatus(5L, StatusLocacao.ATIVA)).thenReturn(false);
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(inv -> {
            Locacao l = inv.getArgument(0);
            l.setId(100L);
            return l;
        });

        LocacaoResponseDTO resposta = service(AGORA).solicitar(USUARIO_ID, pedido(5L));

        assertEquals(100L, resposta.id());
        assertEquals(1L, resposta.clienteId());
        assertEquals(5L, resposta.bicicletaId());
        assertEquals(AGORA, resposta.dataHoraRetirada());
        assertEquals(AGORA.plusHours(2), resposta.dataHoraPrevistaDevolucao());
        assertNull(resposta.dataHoraDevolucaoEfetiva());
        assertNull(resposta.valorTotal());
        assertEquals(StatusLocacao.ATIVA, resposta.status());
        // só o titular e o final do cartão ficam registrados
        assertEquals("MARIA SILVA", resposta.cartaoTitular());
        assertEquals("1111", resposta.cartaoFinal());

        assertEquals(StatusBicicleta.ALUGADA, bike.getStatus());
        verify(bicicletaRepository).save(bike);
    }

    @Test
    @DisplayName("solicitar: sem bicicletaId usa a primeira bicicleta disponível")
    void solicitar_semBicicletaId_usaPrimeiraDisponivel() {
        Bicicleta bike = bicicleta(7L, StatusBicicleta.DISPONIVEL);
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente(1L)));
        when(bicicletaRepository.findFirstDisponivelForUpdate()).thenReturn(Optional.of(bike));
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(inv -> inv.getArgument(0));

        LocacaoResponseDTO resposta = service(AGORA).solicitar(USUARIO_ID, pedido(null));

        assertEquals(7L, resposta.bicicletaId());
        assertEquals(StatusBicicleta.ALUGADA, bike.getStatus());
        verify(bicicletaRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("solicitar: sem bicicletas disponíveis lança RegraNegocioException")
    void solicitar_semBicicletasDisponiveis_lancaExcecao() {
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente(1L)));
        when(bicicletaRepository.findFirstDisponivelForUpdate()).thenReturn(Optional.empty());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service(AGORA).solicitar(USUARIO_ID, pedido(null)));

        assertTrue(ex.getMessage().contains("disponíveis"));
        verify(locacaoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusBicicleta.class, names = {"ALUGADA", "MANUTENCAO"})
    @DisplayName("solicitar: bicicleta que não está DISPONIVEL lança RegraNegocioException")
    void solicitar_bicicletaIndisponivel_lancaExcecao(StatusBicicleta status) {
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente(1L)));
        when(bicicletaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(bicicleta(5L, status)));

        assertThrows(RegraNegocioException.class, () -> service(AGORA).solicitar(USUARIO_ID, pedido(5L)));

        verify(locacaoRepository, never()).save(any());
        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitar: bicicleta com locação ativa não pode ser alugada de novo, mesmo com status DISPONIVEL")
    void solicitar_bicicletaComLocacaoAtiva_lancaExcecao() {
        Bicicleta bike = bicicleta(5L, StatusBicicleta.DISPONIVEL);
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente(1L)));
        when(bicicletaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(bike));
        when(locacaoRepository.existsByBicicletaIdAndStatus(5L, StatusLocacao.ATIVA)).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service(AGORA).solicitar(USUARIO_ID, pedido(5L)));

        assertTrue(ex.getMessage().contains("locação ativa"));
        assertEquals(StatusBicicleta.DISPONIVEL, bike.getStatus());
        verify(locacaoRepository, never()).save(any());
        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitar: usuário sem cadastro de cliente lança RegraNegocioException")
    void solicitar_usuarioSemCadastroDeCliente_lancaExcecao() {
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service(AGORA).solicitar(USUARIO_ID, pedido(5L)));

        assertTrue(ex.getMessage().contains("cadastro de cliente"));
        verifyNoInteractions(bicicletaRepository);
        verify(locacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitar: bicicleta inexistente lança EntityNotFoundException")
    void solicitar_bicicletaInexistente_lancaNotFound() {
        when(clienteRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(cliente(1L)));
        when(bicicletaRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service(AGORA).solicitar(USUARIO_ID, pedido(99L)));

        verify(locacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitar: devolução prevista anterior à retirada lança RegraNegocioException")
    void solicitar_previsaoNoPassado_lancaExcecao() {
        var dto = new LocacaoRequestDTO(5L, AGORA.minusHours(1), cartao());

        assertThrows(RegraNegocioException.class, () -> service(AGORA).solicitar(USUARIO_ID, dto));

        verifyNoInteractions(clienteRepository, bicicletaRepository, locacaoRepository);
    }

    @Test
    @DisplayName("solicitar: cartão vencido lança CartaoInvalidoException")
    void solicitar_cartaoVencido_lancaExcecao() {
        var cartaoVencido = new CartaoRequestDTO("MARIA SILVA", "4111111111111111", "10/26", "123");
        var dto = new LocacaoRequestDTO(5L, AGORA.plusHours(2), cartaoVencido);

        CartaoInvalidoException ex = assertThrows(CartaoInvalidoException.class,
                () -> service(AGORA).solicitar(USUARIO_ID, dto));

        assertEquals("Cartão vencido", ex.getMessage());
        verifyNoInteractions(clienteRepository, bicicletaRepository, locacaoRepository);
    }

    // ================================================================ devolver

    @ParameterizedTest(name = "{0} minutos de uso -> R$ {1}")
    @CsvSource({
            "10,10.00",    // menos de 1h: cobra o mínimo de 1 hora
            "60,10.00",    // exatamente 1h
            "61,20.00",    // hora iniciada conta como cheia
            "90,20.00",
            "120,20.00",
            "121,30.00"
    })
    @DisplayName("devolver: calcula o tempo de uso e o valor por hora iniciada")
    void devolver_calculaTempoEValor(long minutos, String valorEsperado) {
        Bicicleta bike = bicicleta(5L, StatusBicicleta.ALUGADA);
        Locacao locacao = locacaoAtiva(bike);
        LocalDateTime devolucao = AGORA.plusMinutes(minutos);
        when(locacaoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(locacao));
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(inv -> inv.getArgument(0));

        LocacaoResponseDTO resposta = service(devolucao).devolver(100L, false);

        assertEquals(minutos, resposta.minutosUtilizados());
        assertEquals(new BigDecimal(valorEsperado), resposta.valorTotal());
        assertEquals(devolucao, resposta.dataHoraDevolucaoEfetiva());
        assertEquals(StatusLocacao.FINALIZADA, resposta.status());
        assertEquals("1111", resposta.cartaoFinal());
    }

    @Test
    @DisplayName("devolver: sem problema libera a bicicleta (DISPONIVEL)")
    void devolver_semProblema_bicicletaFicaDisponivel() {
        Bicicleta bike = bicicleta(5L, StatusBicicleta.ALUGADA);
        when(locacaoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(locacaoAtiva(bike)));
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(inv -> inv.getArgument(0));

        service(AGORA.plusMinutes(30)).devolver(100L, false);

        assertEquals(StatusBicicleta.DISPONIVEL, bike.getStatus());
        verify(bicicletaRepository).save(bike);
    }

    @Test
    @DisplayName("devolver: com problema envia a bicicleta para MANUTENCAO")
    void devolver_comProblema_bicicletaVaiParaManutencao() {
        Bicicleta bike = bicicleta(5L, StatusBicicleta.ALUGADA);
        when(locacaoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(locacaoAtiva(bike)));
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(inv -> inv.getArgument(0));

        LocacaoResponseDTO resposta = service(AGORA.plusMinutes(30)).devolver(100L, true);

        assertEquals(StatusBicicleta.MANUTENCAO, bike.getStatus());
        assertEquals(StatusLocacao.FINALIZADA, resposta.status()); // a locação é encerrada e cobrada normalmente
        verify(bicicletaRepository).save(bike);
    }

    @Test
    @DisplayName("devolver: locação já devolvida lança RegraNegocioException e não altera a bicicleta")
    void devolver_locacaoJaDevolvida_lancaExcecao() {
        Bicicleta bike = bicicleta(5L, StatusBicicleta.DISPONIVEL);
        Locacao locacao = locacaoAtiva(bike);
        locacao.setStatus(StatusLocacao.FINALIZADA);
        locacao.setDataHoraDevolucaoEfetiva(AGORA.plusHours(1));
        when(locacaoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(locacao));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service(AGORA.plusHours(2)).devolver(100L, false));

        assertTrue(ex.getMessage().contains("já foi devolvida"));
        verify(bicicletaRepository, never()).save(any());
        verify(locacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("devolver: locação inexistente lança EntityNotFoundException")
    void devolver_locacaoInexistente_lancaNotFound() {
        when(locacaoRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service(AGORA).devolver(99L, false));

        verifyNoInteractions(bicicletaRepository);
    }
}
