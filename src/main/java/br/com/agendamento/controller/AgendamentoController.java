package br.com.agendamento.controller;
import br.com.agendamento.dto.Requests.AgendamentoRequest;
import br.com.agendamento.model.Agendamento;
import br.com.agendamento.service.AgendamentoService;
import br.com.agendamento.security.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {
    private final AgendamentoService service;
    public AgendamentoController(AgendamentoService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agendamento cadastrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AgendamentoRequest request) {
        return service.cadastrar(UsuarioAtual.de(jwt), request);
    }
    @GetMapping
    public List<Agendamento> listar(@AuthenticationPrincipal Jwt jwt,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return service.listar(UsuarioAtual.de(jwt), de, ate);
    }
    @PatchMapping("/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { service.cancelar(UsuarioAtual.de(jwt), id); }
}
