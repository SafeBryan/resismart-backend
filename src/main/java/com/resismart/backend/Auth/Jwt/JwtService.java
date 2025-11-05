package com.resismart.backend.Auth.Jwt;

import com.resismart.backend.users.Entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;


@Service
public class JwtService {

    private static final String SECRET_KEY="aeb645db84c54a67220e93f0e63c0bb3e2a0d2b7fcc723c8bedd327ffefb8bc6c3998a1d120d2dee1b2440970522518b27e64d73306d6df9f3359e7b5671117f23b18b11691f8ea4f798e881f26e3366c62fb7a86f6d315debb25ddf74caaad1fd256bac8087d914ddd158bd1dcb9bcecc4d1f465fd0bcecdb524d37e9204a47c3998cc6f272529c6f2bac2c48fd1525ff24e6ff0f60562cbce5fbc9011d3f2cf1c48ee0163b96e20cb8a9bbbc5c0dea7ad5de0c597111cee7962ed52e2102dbb9a9295b3283269df7621d87b6bdb53837f48f9b23a133addef74319309f197c98b8bdb756f81866badfcc6c1dfc01d8755e6982e629fe3dc82de14a7d17d31d";
    /*@Autowired
    ResidenteRepository clienteRepository;*/

    public String getToken(Usuario user) {
        String rol = user.getAuthorities().iterator().next().getAuthority();

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        claims.put("apellido", user.getApellidos());
        claims.put("nombre", user.getNombres());
        claims.put("idUsuario", user.getId_usuario());
        /*if (rol.equals("Cliente")) {
            clienteRepository.findByEmail(user.getEmail()).ifPresent(cliente -> {
                        claims.put("cedula", cliente.getCedula());
                        claims.put("idCliente", cliente.getId());
                    }
            );

        }*/
        return getToken(claims, user);
    }


    private String getToken(HashMap<String,Object> extraClaims, UserDetails user) {
    return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(user.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis()+1000*60*60*24))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Key getKey() {
        byte[] keyBytes= Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromToken(String token) {
        return getClaim(token,Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username=getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Claims getAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    public <T> T getClaim(String token, Function<Claims,T> claimsResolver){
        final Claims claims=getAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Date getExpiration(String token){
        return getClaim(token,Claims::getExpiration);
    }
    private boolean isTokenExpired(String token){
        return getExpiration(token).before(new Date());
    }
}
