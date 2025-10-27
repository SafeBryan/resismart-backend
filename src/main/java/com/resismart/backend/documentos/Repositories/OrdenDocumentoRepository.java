package com.resismart.backend.documentos.Repositories;

import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.documentos.Entities.OrdenDocumento;
import com.resismart.backend.documentos.Enums.TipoRelacion;
import com.resismart.backend.pagos.Entities.OrdenPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenDocumentoRepository extends JpaRepository<OrdenDocumento, Integer> {

    List<OrdenDocumento> findByOrden(OrdenPago orden);

    List<OrdenDocumento> findByDocumento(Documento documento);

    List<OrdenDocumento> findByOrdenId(Integer idOrden);

    List<OrdenDocumento> findByOrdenIdAndTipoRelacion(Integer idOrden, TipoRelacion tipoRelacion);

    boolean existsByOrdenIdAndDocumentoId(Integer idOrden, Integer idDocumento);

    void deleteByOrdenIdAndDocumentoId(Integer idOrden, Integer idDocumento);
}
