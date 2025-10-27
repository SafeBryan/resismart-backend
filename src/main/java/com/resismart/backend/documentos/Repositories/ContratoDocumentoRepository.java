package com.resismart.backend.documentos.Repositories;

import com.resismart.backend.documentos.Entities.ContratoDocumento;
import com.resismart.backend.documentos.Entities.Documento;
import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.documentos.Enums.TipoRelacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratoDocumentoRepository extends JpaRepository<ContratoDocumento, Integer> {

    List<ContratoDocumento> findByContrato(Contrato contrato);

    List<ContratoDocumento> findByDocumento(Documento documento);

    List<ContratoDocumento> findByContratoId(Integer idContrato);

    List<ContratoDocumento> findByContratoIdAndTipoRelacion(Integer idContrato, TipoRelacion tipoRelacion);

    boolean existsByContratoIdAndDocumentoId(Integer idContrato, Integer idDocumento);

    void deleteByContratoIdAndDocumentoId(Integer idContrato, Integer idDocumento);
}
