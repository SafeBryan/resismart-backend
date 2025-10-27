package com.resismart.backend.documentos.Repositories;

import com.resismart.backend.documentos.Entities.AuditoriaDocumentos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaDocumentosRepository extends JpaRepository<AuditoriaDocumentos, Integer> {

    List<AuditoriaDocumentos> findByDocumentoIdOrderByFechaDesc(Integer idDocumento);

    List<AuditoriaDocumentos> findByRealizadoPorOrderByFechaDesc(Integer realizadoPor);

    List<AuditoriaDocumentos> findByDocumento_IdOrderByFechaDesc(Integer idDocumento);

}
