package com.resismart.backend.contratos.Repositories;

import com.resismart.backend.contratos.Entities.Contrato;
import com.resismart.backend.contratos.Enums.EstadoContrato;
import com.resismart.backend.condominios.Entities.Condominio;
import com.resismart.backend.condominios.Entities.Unidad;
import com.resismart.backend.condominios.Enums.UnidadEstado;
import com.resismart.backend.residentes.Entities.Residente;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Enums.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ContratoRepositoryTest {

    @Autowired
    private ContratoRepository contratoRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllByResidenteIdWithJoinsDevuelveContratosConUnidadYUsuario() {
        DatosContrato datos = persistirContrato(
                "residente@resismart.com",
                "C-101",
                UnidadEstado.OCUPADA,
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                EstadoContrato.ACTIVO
        );

        entityManager.flush();
        entityManager.clear();

        List<Contrato> contratos = contratoRepository.findAllByResidenteIdWithJoins(datos.residente.getId());

        assertThat(contratos).hasSize(1);
        Contrato recuperado = contratos.get(0);
        assertThat(recuperado.getUnidad().getNumero()).isEqualTo("C-101");
        assertThat(recuperado.getResidente().getUsuario().getCorreo()).isEqualTo("residente@resismart.com");
    }

    @Test
    void findActivosVigentesEnFiltraPorRangoFechasYEstado() {
        persistirContrato(
                "act@resismart.com",
                "D-201",
                UnidadEstado.OCUPADA,
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.JUNE, 30),
                EstadoContrato.ACTIVO
        );
        persistirContrato(
                "res@resismart.com",
                "D-202",
                UnidadEstado.OCUPADA,
                LocalDate.of(2023, Month.JANUARY, 1),
                LocalDate.of(2023, Month.DECEMBER, 31),
                EstadoContrato.RESCINDIDO
        );

        entityManager.flush();
        entityManager.clear();

        LocalDate inicioMes = LocalDate.of(2024, Month.MARCH, 1);
        LocalDate finMes = LocalDate.of(2024, Month.MARCH, 31);

        List<Contrato> contratos = contratoRepository.findActivosVigentesEn(
                EstadoContrato.ACTIVO, inicioMes, finMes);

        assertThat(contratos)
                .hasSize(1)
                .allMatch(c -> c.getEstado() == EstadoContrato.ACTIVO);
        assertThat(contratos.get(0).getUnidad().getNumero()).isEqualTo("D-201");
    }

    private DatosContrato persistirContrato(
            String correoResidente,
            String numeroUnidad,
            UnidadEstado estadoUnidad,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoContrato estadoContrato
    ) {
        Usuario dueno = entityManager.persist(usuario("dueno+" + numeroUnidad + "@resismart.com", Rol.valueOf("DUE\u00D1O")));
        Condominio condominio = entityManager.persist(condominio("Condominio " + numeroUnidad, dueno));

        Unidad unidad = Unidad.builder()
                .numero(numeroUnidad)
                .estado(estadoUnidad)
                .condominio(condominio)
                .build();
        unidad = entityManager.persist(unidad);

        Usuario residenteUsuario = entityManager.persist(usuario(correoResidente, Rol.RESIDENTE));
        Residente residente = Residente.builder()
                .cedula("CED" + numeroUnidad)
                .telefono("555-" + numeroUnidad.replace("-", ""))
                .usuario(residenteUsuario)
                .build();
        residente = entityManager.persist(residente);

        Contrato contrato = Contrato.builder()
                .unidad(unidad)
                .residente(residente)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .monto(BigDecimal.valueOf(1000))
                .estado(estadoContrato)
                .build();
        contrato = entityManager.persist(contrato);

        return new DatosContrato(residente, contrato);
    }

    private Usuario usuario(String correo, Rol rol) {
        return Usuario.builder()
                .correo(correo)
                .password_hash("pwd")
                .rol(rol)
                .nombres("Nombre")
                .apellidos("Apellido")
                .estado(true)
                .build();
    }

    private Condominio condominio(String nombre, Usuario dueno) {
        return Condominio.builder()
                .nombre(nombre)
                .direccion("Direccion " + nombre)
                .telefono("555-5555")
                .correo(nombre.toLowerCase().replace(" ", "") + "@condo.com")
                .dueno(dueno)
                .build();
    }

    private record DatosContrato(Residente residente, Contrato contrato) {}
}




