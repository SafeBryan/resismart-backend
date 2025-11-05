package com.resismart.backend.avisos.Repositories;

import com.resismart.backend.avisos.Entities.Aviso;
import com.resismart.backend.avisos.Enums.AvisoDestino;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findTop50ByDestinoOrderByCreadoEnDesc(AvisoDestino destino);

    List<Aviso> findTop50ByDestinoAndDestinoReferenciaOrderByCreadoEnDesc(
            AvisoDestino destino,
            String destinoReferencia
    );
}

