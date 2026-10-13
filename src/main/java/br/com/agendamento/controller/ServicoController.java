package br.com.agendamento.controller;
import br.com.agendamento.dto.Requests.ServicoRequest;
import br.com.agendamento.model.Servico;
import br.com.agendamento.service.ServicoService;
import br.com.agendamento.security.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {
    private final ServicoService service;
    public ServicoController(ServicoService service) { this.service = service; }
    @GetMapping
    public List<Servico> listar(@AuthenticationPrincipal Jwt jwt) { return service.listar(UsuarioAtual.de(jwt).empresaId()); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Servico cadastrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ServicoRequest request) {
        return service.cadastrar(UsuarioAtual.de(jwt).empresaId(), request);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Servico atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ServicoRequest request) {
        return service.atualizar(UsuarioAtual.de(jwt).empresaId(), id, request);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void desativar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { service.desativar(UsuarioAtual.de(jwt).empresaId(), id); }
}
