package com.resismart.backend.condominios.DTO;

import com.resismart.backend.condominios.Enums.UnidadEstado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnidadDetalleDTO {
    private Integer id;
    private String numero;
    private UnidadEstado estado;
    private Integer condominioId;
    private String condominioNombre;
    private String condominioDireccion;
}
