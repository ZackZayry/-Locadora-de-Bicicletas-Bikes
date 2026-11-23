package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.entity.StatusLocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocacaoRepository extends JpaRepository<Locacao, Long> {

    // a bicicleta já participa de alguma locação nesse status (ATIVA)?
    boolean existsByBicicletaIdAndStatus(Long bicicletaId, StatusLocacao status);

    List<Locacao> findByClienteId(Long clienteId);

    // trava a locação para evitar duas devoluções simultâneas (query nativa: ver comentário em BicicletaRepository)
    @Query(value = "select * from locacoes where id = :id for update", nativeQuery = true)
    Optional<Locacao> findByIdForUpdate(@Param("id") Long id);
}
