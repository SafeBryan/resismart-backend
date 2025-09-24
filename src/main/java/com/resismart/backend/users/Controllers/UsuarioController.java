package com.resismart.backend.users.Controllers;


import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UsuarioController {
@Autowired
    private UsuarioService usuarioService;

@GetMapping
    public ResponseEntity<List<Usuario>> getAllUsers(){
    return ResponseEntity.ok(usuarioService.getAllUsers());
}

}
