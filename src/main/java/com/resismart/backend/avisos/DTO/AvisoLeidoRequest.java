package com.resismart.backend.avisos.DTO;

import lombok.Data;

import java.util.List;

@Data
public class AvisoLeidoRequest {
    private List<Long> avisoIds;
}
