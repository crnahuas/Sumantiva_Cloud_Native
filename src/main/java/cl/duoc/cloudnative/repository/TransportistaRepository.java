package cl.duoc.cloudnative.repository;

import cl.duoc.cloudnative.model.Transportista;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    Optional<Transportista> findByAlias(String alias);
}
