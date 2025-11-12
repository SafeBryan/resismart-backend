package com.resismart.backend.avisos.Repositories;

import com.resismart.backend.avisos.Entities.AvisoUsuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AvisoUsuarioRepository extends CrudRepository<AvisoUsuario, Long> {

    @Query("""
        SELECT au
        FROM AvisoUsuario au
        JOIN FETCH au.aviso aviso
        WHERE au.usuario.id_usuario = :usuarioId
        ORDER BY aviso.creadoEn DESC
    """)
    List<AvisoUsuario> buscarPorUsuario(@Param("usuarioId") Integer usuarioId, Pageable pageable);

    @Query("""
        SELECT au
        FROM AvisoUsuario au
        JOIN FETCH au.aviso aviso
        WHERE au.usuario.id_usuario = :usuarioId
          AND au.entregadoEn IS NULL
        ORDER BY aviso.creadoEn ASC
    """)
    List<AvisoUsuario> pendientesPorUsuario(@Param("usuarioId") Integer usuarioId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AvisoUsuario au
        SET au.entregadoEn = :momento
        WHERE au.aviso.id = :avisoId
          AND au.usuario.id_usuario = :usuarioId
          AND au.entregadoEn IS NULL
    """)
    void marcarEntregado(@Param("avisoId") Long avisoId,
                         @Param("usuarioId") Integer usuarioId,
                         @Param("momento") Instant momento);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AvisoUsuario au
        SET au.leido = true,
            au.entregadoEn = COALESCE(au.entregadoEn, :momento)
        WHERE au.usuario.id_usuario = :usuarioId
          AND au.aviso.id IN :avisoIds
          AND au.leido = false
    """)
    int marcarLeidos(@Param("usuarioId") Integer usuarioId,
                     @Param("avisoIds") List<Long> avisoIds,
                     @Param("momento") Instant momento);
}

