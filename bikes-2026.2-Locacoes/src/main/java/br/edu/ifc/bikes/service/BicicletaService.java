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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BicicletaService {

    private final BicicletaRepository bicicletaRepository;
    private final BicicletaMapper bicicletaMapper;

    @Transactional
    public BicicletaResponseDTO criar(BicicletaRequestDTO dto) {
        if (bicicletaRepository.existsByCodigo(dto.codigo())) {
            throw new RecursoDuplicadoException(String.format("O código '%s' já está cadastrado", dto.codigo()));
        }
        // a bicicleta nasce com status DISPONIVEL
        return bicicletaMapper.toResponse(bicicletaRepository.save(bicicletaMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<BicicletaResponseDTO> listar() {
        return bicicletaMapper.toResponse(bicicletaRepository.findAll());
    }

    /** Bicicletas com status DISPONIVEL, aptas a serem alugadas. */
    @Transactional(readOnly = true)
    public List<BicicletaResponseDTO> listarDisponiveis() {
        return bicicletaMapper.toResponse(bicicletaRepository.findByStatus(StatusBicicleta.DISPONIVEL));
    }

    @Transactional(readOnly = true)
    public BicicletaResponseDTO buscarPorId(Long id) {
        return bicicletaMapper.toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public BicicletaResponseDTO buscarPorCodigo(String codigo) {
        return bicicletaMapper.toResponse(bicicletaRepository.findByCodigo(codigo).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com código '%s' não encontrada", codigo))));
    }

    @Transactional
    public BicicletaResponseDTO atualizar(Long id, BicicletaRequestDTO dto) {
        Bicicleta bicicleta = buscarEntidade(id);
        if (bicicletaRepository.existsByCodigoAndIdNot(dto.codigo(), id)) {
            throw new RecursoDuplicadoException(String.format("O código '%s' já está cadastrado", dto.codigo()));
        }
        bicicletaMapper.updateEntity(dto, bicicleta);
        return bicicletaMapper.toResponse(bicicletaRepository.save(bicicleta));
    }

    /**
     * Altera manualmente o status (DISPONIVEL <-> MANUTENCAO). ALUGADA é definido apenas pela locação,
     * e uma bicicleta alugada só muda de status pela devolução.
     */
    @Transactional
    public BicicletaResponseDTO alterarStatus(Long id, StatusBicicleta novoStatus) {
        Bicicleta bicicleta = bicicletaRepository.findByIdForUpdate(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com id=%d não encontrada", id)));
        if (novoStatus == StatusBicicleta.ALUGADA) {
            throw new RegraNegocioException("O status ALUGADA é definido automaticamente ao realizar uma locação");
        }
        if (bicicleta.getStatus() == StatusBicicleta.ALUGADA) {
            throw new RegraNegocioException(
                    "A bicicleta está em uma locação ativa; registre a devolução antes de alterar o status");
        }
        bicicleta.setStatus(novoStatus);
        return bicicletaMapper.toResponse(bicicletaRepository.save(bicicleta));
    }

    @Transactional
    public void deletar(Long id) {
        bicicletaRepository.delete(buscarEntidade(id));
    }

    private Bicicleta buscarEntidade(Long id) {
        return bicicletaRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com id=%d não encontrada", id)));
    }
}
