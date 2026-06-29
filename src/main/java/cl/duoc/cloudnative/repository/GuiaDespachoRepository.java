package cl.duoc.cloudnative.repository;

import cl.duoc.cloudnative.model.GuiaDespacho;
import cl.duoc.cloudnative.model.EstadoGuia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    List<GuiaDespacho> findByTransportistaAliasAndFechaAndEstadoNotOrderByTimestampDesc(
            String alias,
            String fecha,
            EstadoGuia estado
    );
}
