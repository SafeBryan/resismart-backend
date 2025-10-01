package com.resismart.backend.pagos.Repositories;

import com.resismart.backend.pagos.Entities.OrdenPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OrdenPagoRepository extends JpaRepository<OrdenPago, Integer> {

    boolean existsByContrato_IdAndPeriodo(Integer idContrato, LocalDate periodo);

    List<OrdenPago> findByContrato_Id(Integer idContrato);

    List<OrdenPago> findByPeriodo(LocalDate periodo);
}
