package com.resismart.backend.users.Controllers;


import com.resismart.backend.users.DTO.UsuarioClienteCredencialesDTO;
import com.resismart.backend.users.DTO.UsuarioCrearRequest;
import com.resismart.backend.users.DTO.UsuarioEditarRequest;
import com.resismart.backend.users.Entities.Usuario;
import com.resismart.backend.users.Services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/Usuarios")
public class UsuarioController {
@Autowired
    private UsuarioService usuarioService;


    @PostMapping("/guardar")
    public ResponseEntity<?> postUsuario(@RequestBody UsuarioCrearRequest usuario){
        try {
            Usuario u=usuarioService.register(usuario);
            return ResponseEntity.created(new URI("/Usuarios/"+u.getId_usuario())).body(u);
        } catch (URISyntaxException | RuntimeException  e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> putUsuario(@RequestBody UsuarioEditarRequest usuario){
        try {
            Usuario u=usuarioService.putUsuario(usuario);
            return ResponseEntity.created(new URI("/Usuarios/"+u.getId_usuario())).body(u);
        } catch (URISyntaxException | RuntimeException  e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @PutMapping("/credencialesCliente")
    public ResponseEntity<?> putCredencialesUsuarioCliente(@RequestBody @Valid UsuarioClienteCredencialesDTO usuario){
        System.out.println("ID_Usuario recibido: " + usuario.getIdUsuario());
        try {
            Usuario u=usuarioService.actualizarCredencialesUsuarioCliente(usuario);
            return ResponseEntity.created(new URI("/Usuarios/"+u.getId_usuario())).body(u);
        } catch (URISyntaxException | RuntimeException  e) {
            return ResponseEntity.internalServerError().body("Error: "+e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> getAll(){
        return ResponseEntity.ok(usuarioService.getUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id){
        Usuario c = usuarioService.getUsuarioById(id);
        if(c!=null)
            return ResponseEntity.ok(c);
        else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable int id){
        Usuario result = usuarioService.getUsuarioById(id);
        if(result!=null){
            usuarioService.deleteUsuario(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado");
        }
    }

}
