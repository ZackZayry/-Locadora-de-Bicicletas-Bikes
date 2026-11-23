package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.StatusBicicleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    Optional<Bicicleta> findByCodigo(String codigo);
    List<Bicicleta> findByStatus(StatusBicicleta status);

    // Os métodos abaixo travam a linha (SELECT ... FOR UPDATE): duas locações simultâneas
    // não conseguem pegar a mesma bicicleta ao mesmo tempo.
    // São queries nativas de propósito: o @Lock do JPA gera "FOR UPDATE OF <tabela>", sintaxe que
    // o MariaDB (XAMPP) não aceita; o "FOR UPDATE" simples funciona em MariaDB, MySQL e H2.
    @Query(value = "select * from bicicletas where id = :id for update", nativeQuery = true)
    Optional<Bicicleta> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "select * from bicicletas where status = 'DISPONIVEL' order by id limit 1 for update", nativeQuery = true)
    Optional<Bicicleta> findFirstDisponivelForUpdate();
}
