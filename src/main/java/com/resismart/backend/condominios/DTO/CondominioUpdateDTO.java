package com.resismart.backend.condominios.DTO;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class CondominioUpdateDTO {
    private String nombre;
    private String direccion;
    private String telefono;
    @Email private String correo;
    private Integer idDueno; // transferir titularidad (opcional)
    private Integer maxUsuarios;
}
