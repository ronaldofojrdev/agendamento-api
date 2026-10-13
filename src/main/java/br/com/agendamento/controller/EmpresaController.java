package br.com.agendamento.controller;
import br.com.agendamento.dto.Requests.EmpresaRequest;
import br.com.agendamento.model.Empresa;
import br.com.agendamento.repository.EmpresaRepository;
import br.com.agendamento.service.AuthService;
import br.com.agendamento.security.UsuarioAtual;
import br.com.agendamento.exception.ApiException;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {
    private final AuthService auth;
    private final EmpresaRepository empresas;
    public EmpresaController(AuthService auth, EmpresaRepository empresas) { this.auth = auth; this.empresas = empresas; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    public Empresa cadastrar(@Valid @RequestBody EmpresaRequest request) { return auth.cadastrarEmpresa(request); }
    @GetMapping("/atual")
    public Empresa atual(@AuthenticationPrincipal Jwt jwt) {
        return empresas.buscarPorId(UsuarioAtual.de(jwt).empresaId()).orElseThrow(ApiException::naoEncontrado);
    }
}
