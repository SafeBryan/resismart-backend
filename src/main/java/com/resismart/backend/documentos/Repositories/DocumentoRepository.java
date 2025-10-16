package com.resismart.backend.documentos.Repositories;

import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Enums.EstadoValidacion;
import com.resismart.backend.documentos.Enums.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<Documento, Integer> {

    Page<Documento> findByTipo(TipoDocumento tipo, Pageable pageable);

    Page<Documento> findByEstadoValidacion(EstadoValidacion estado, Pageable pageable);

    Page<Documento> findByTipoAndEstadoValidacion(TipoDocumento tipo, EstadoValidacion estado, Pageable pageable);

    Optional<Documento> findBySha256AndSizeBytes(String sha256, Long sizeBytes);
}
