package com.resismart.backend.avisos.Repositories;

import com.resismart.backend.avisos.Entities.Aviso;
import com.resismart.backend.avisos.Enums.AvisoDestino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findTop50ByDestinoOrderByCreadoEnDesc(AvisoDestino destino);

    List<Aviso> findTop50ByDestinoAndDestinoReferenciaOrderByCreadoEnDesc(
            AvisoDestino destino,
            String destinoReferencia
    );

    @Query("""
        SELECT a FROM Aviso a
        WHERE (a.sender.id_usuario = :u1 AND a.receiver.id_usuario = :u2)
           OR (a.sender.id_usuario = :u2 AND a.receiver.id_usuario = :u1)
        ORDER BY a.creadoEn ASC
    """)
    List<Aviso> findConversacion(@Param("u1") Integer usuario1, @Param("u2") Integer usuario2);
}

