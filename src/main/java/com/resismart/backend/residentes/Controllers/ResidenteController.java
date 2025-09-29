package com.resismart.backend.residentes.Controllers;


import com.resismart.backend.residentes.DTO.ResidenteDTO;
import com.resismart.backend.residentes.DTO.ResidenteRespuestaDTO;
import com.resismart.backend.residentes.Services.ResidenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Residentes")
public class ResidenteController {
    @Autowired
    private ResidenteService residenteService;

    @PostMapping
    public ResponseEntity<?> saveCliente(@Valid @RequestBody ResidenteDTO residenteDTO){
        try {
            return ResponseEntity.ok(residenteService.saveCliente(residenteDTO));
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public  ResponseEntity<ResidenteRespuestaDTO> getClienteById(@PathVariable Long id){
        return residenteService.getCliente(id)
                .map(ResponseEntity::ok)
                .orElseGet(()-> ResponseEntity.notFound().build());
    }

    @GetMapping
    public  ResponseEntity<List<ResidenteRespuestaDTO>> getAllClientes(){
        return ResponseEntity.ok(residenteService.getAllClientes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResidenteRespuestaDTO> updateCliente(@PathVariable Long id, @Valid @RequestBody ResidenteDTO residenteDTO){
        return  ResponseEntity.ok(residenteService.updateCliente(id, residenteDTO));

    }

    @GetMapping("/condominio/{id}")
    public  ResponseEntity<List<ResidenteRespuestaDTO>> getAllClientesPorCondominio(@PathVariable Integer id){
        return ResponseEntity.ok(residenteService.getAllClientesPorCondominio(id));
    }
}
