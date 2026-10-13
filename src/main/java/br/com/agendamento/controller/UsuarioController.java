package br.com.agendamento.controller;
import br.com.agendamento.dto.Requests.UsuarioRequest;
import br.com.agendamento.model.*;
import br.com.agendamento.repository.UsuarioRepository;
import br.com.agendamento.service.AuthService;
import br.com.agendamento.security.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final AuthService auth;
    private final UsuarioRepository usuarios;
    public UsuarioController(AuthService auth, UsuarioRepository usuarios) { this.auth = auth; this.usuarios = usuarios; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario cadastrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsuarioRequest request) {
        return auth.cadastrarUsuario(UsuarioAtual.de(jwt).empresaId(), request);
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listar(@AuthenticationPrincipal Jwt jwt) { return usuarios.listar(UsuarioAtual.de(jwt).empresaId()); }
    @GetMapping("/profissionais")
    public List<ProfissionalResponse> profissionais(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.listarProfissionais(UsuarioAtual.de(jwt).empresaId()).stream()
            .map(u -> new ProfissionalResponse(u.id(), u.nome())).toList();
    }
    public record ProfissionalResponse(UUID id, String nome) {}
}
