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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class LocacaoService {

    private static final DateTimeFormatter VALIDADE_CARTAO = DateTimeFormatter.ofPattern("MM/yy");

    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final BicicletaRepository bicicletaRepository;
    private final LocacaoMapper locacaoMapper;
    private final Clock clock;
    private final BigDecimal valorHora;

    public LocacaoService(LocacaoRepository locacaoRepository,
                          ClienteRepository clienteRepository,
                          BicicletaRepository bicicletaRepository,
                          LocacaoMapper locacaoMapper,
                          Clock clock,
                          @Value("${bikes.locacao.valor-hora:10.00}") BigDecimal valorHora) {
        this.locacaoRepository = locacaoRepository;
        this.clienteRepository = clienteRepository;
        this.bicicletaRepository = bicicletaRepository;
        this.locacaoMapper = locacaoMapper;
        this.clock = clock;
        this.valorHora = valorHora;
    }

    /**
     * Solicitação de aluguel. Regras:
     * 1. a devolução prevista deve ser posterior à retirada (agora);
     * 2. o cartão deve estar dentro da validade;
     * 3. o usuário autenticado deve ter cadastro de cliente;
     * 4. deve haver bicicleta disponível (a informada ou, se nenhuma for informada, a primeira livre);
     * 5. a bicicleta não pode ter outra locação ativa.
     */
    @Transactional
    public LocacaoResponseDTO solicitar(Long usuarioId, LocacaoRequestDTO dto) {
        LocalDateTime agora = LocalDateTime.now(clock);

        if (!dto.dataHoraPrevistaDevolucao().isAfter(agora)) {
            throw new RegraNegocioException("A data/hora prevista de devolução deve ser posterior à retirada");
        }
        validarValidadeCartao(dto.cartao(), agora);

        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new RegraNegocioException("Realize o cadastro de cliente antes de alugar uma bicicleta"));

        Bicicleta bicicleta = obterBicicletaDisponivel(dto.bicicletaId());

        // segunda barreira: mesmo com status DISPONIVEL, não pode haver locação ativa para a bicicleta
        if (locacaoRepository.existsByBicicletaIdAndStatus(bicicleta.getId(), StatusLocacao.ATIVA)) {
            throw new RegraNegocioException(
                    String.format("A bicicleta id=%d já possui uma locação ativa", bicicleta.getId()));
        }

        bicicleta.setStatus(StatusBicicleta.ALUGADA);
        bicicletaRepository.save(bicicleta);

        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBicicleta(bicicleta);
        locacao.setStatus(StatusLocacao.ATIVA);
        locacao.setDataHoraRetirada(agora);
        locacao.setDataHoraPrevistaDevolucao(dto.dataHoraPrevistaDevolucao());
        locacao.setCartaoTitular(dto.cartao().titular());
        locacao.setCartaoFinal(ultimosQuatroDigitos(dto.cartao().numero()));
        return locacaoMapper.toResponse(locacaoRepository.save(locacao));
    }

    /**
     * Devolução. Registra a data/hora efetiva, calcula o tempo de utilização e o valor a debitar
     * (por hora iniciada, mínimo de 1 hora) e libera a bicicleta (DISPONIVEL) ou, se houve problema,
     * envia para MANUTENCAO.
     */
    @Transactional
    public LocacaoResponseDTO devolver(Long id, boolean problema) {
        Locacao locacao = locacaoRepository.findByIdForUpdate(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Locação com id=%d não encontrada", id)));

        if (locacao.getStatus() != StatusLocacao.ATIVA) {
            throw new RegraNegocioException(String.format("A locação id=%d já foi devolvida", id));
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        long minutos = Math.max(0, Duration.between(locacao.getDataHoraRetirada(), agora).toMinutes());
        long horasCobradas = Math.max(1, (minutos + 59) / 60);
        BigDecimal valor = valorHora.multiply(BigDecimal.valueOf(horasCobradas)).setScale(2, RoundingMode.HALF_UP);

        locacao.setStatus(StatusLocacao.FINALIZADA);
        locacao.setDataHoraDevolucaoEfetiva(agora);
        locacao.setMinutosUtilizados(minutos);
        locacao.setValorTotal(valor);

        Bicicleta bicicleta = locacao.getBicicleta();
        bicicleta.setStatus(problema ? StatusBicicleta.MANUTENCAO : StatusBicicleta.DISPONIVEL);
        bicicletaRepository.save(bicicleta);

        return locacaoMapper.toResponse(locacaoRepository.save(locacao));
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> listar(Long clienteId) {
        List<Locacao> locacoes = (clienteId == null)
                ? locacaoRepository.findAll()
                : locacaoRepository.findByClienteId(clienteId);
        return locacaoMapper.toResponse(locacoes);
    }

    @Transactional(readOnly = true)
    public LocacaoResponseDTO buscarPorId(Long id) {
        return locacaoMapper.toResponse(locacaoRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Locação com id=%d não encontrada", id))));
    }

    private Bicicleta obterBicicletaDisponivel(Long bicicletaId) {
        if (bicicletaId == null) {
            return bicicletaRepository.findFirstDisponivelForUpdate()
                    .orElseThrow(() -> new RegraNegocioException("Não há bicicletas disponíveis no momento"));
        }
        Bicicleta bicicleta = bicicletaRepository.findByIdForUpdate(bicicletaId).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com id=%d não encontrada", bicicletaId)));
        if (bicicleta.getStatus() != StatusBicicleta.DISPONIVEL) {
            throw new RegraNegocioException(String.format(
                    "A bicicleta id=%d não está disponível (status: %s)", bicicletaId, bicicleta.getStatus()));
        }
        return bicicleta;
    }

    private void validarValidadeCartao(CartaoRequestDTO cartao, LocalDateTime agora) {
        try {
            YearMonth validade = YearMonth.parse(cartao.validade(), VALIDADE_CARTAO);
            if (validade.isBefore(YearMonth.from(agora))) {
                throw new CartaoInvalidoException("Cartão vencido");
            }
        } catch (DateTimeParseException ex) {
            throw new CartaoInvalidoException("Validade do cartão inválida");
        }
    }

    private String ultimosQuatroDigitos(String numero) {
        return numero.substring(numero.length() - 4);
    }
}
