package br.com.agendamento;

import br.com.agendamento.dto.Requests.*;
import br.com.agendamento.exception.ApiException;
import br.com.agendamento.model.*;
import br.com.agendamento.repository.ServicoRepository;
import br.com.agendamento.security.UsuarioAtual;
import br.com.agendamento.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgendamentoApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthService auth;
    @Autowired ServicoService servicos;
    @Autowired AgendamentoService agendas;
    @Autowired ServicoRepository servicoRepository;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    Empresa empresa;
    Usuario profissional;
    Usuario cliente;
    Servico servico;
    String adminToken;
    String clienteToken;
    String profissionalToken;
    OffsetDateTime inicio;

    @BeforeEach
    void preparar() {
        empresa = auth.cadastrarEmpresa(new EmpresaRequest("Clinica Centro", "empresa-" + UUID.randomUUID(),
            "Ana", "admin@exemplo.com", "SenhaTeste123"));
        profissional = auth.cadastrarUsuario(empresa.id(), new UsuarioRequest("Bruno", "bruno@exemplo.com", "SenhaTeste123", Papel.PROFISSIONAL));
        cliente = auth.cadastrarUsuario(empresa.id(), new UsuarioRequest("Carla", "carla@exemplo.com", "SenhaTeste123", Papel.CLIENTE));
        servico = servicos.cadastrar(empresa.id(), new ServicoRequest("Consulta", 30, new BigDecimal("80.00")));
        adminToken = token("admin@exemplo.com");
        clienteToken = token(cliente.email());
        profissionalToken = token(profissional.email());
        inicio = OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }
    String token(String email) { return auth.login(new LoginRequest(empresa.slug(), email, "SenhaTeste123")).accessToken(); }
    Map<String, Object> reserva(OffsetDateTime horario) {
        return Map.of("servicoId", servico.id(), "profissionalId", profissional.id(), "inicio", horario.toString());
    }
    ResultActions enviar(String path, Object body, String token) throws Exception {
        var request = post(path).contentType("application/json").content(mapper.writeValueAsString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }
    JsonNode criado() throws Exception {
        String json = enviar("/api/agendamentos", reserva(inicio), clienteToken).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json);
    }

    @Test void cadastroPublicoCriaEmpresaEAdmin() throws Exception {
        String slug = "nova-" + UUID.randomUUID();
        enviar("/api/empresas", new EmpresaRequest("Oficina", slug, "Joao", "joao@exemplo.com", "SenhaTeste123"), null)
            .andExpect(status().isCreated()).andExpect(jsonPath("slug").value(slug));
        enviar("/api/auth/login", new LoginRequest(slug, "joao@exemplo.com", "SenhaTeste123"), null)
            .andExpect(status().isOk()).andExpect(jsonPath("accessToken").isString());
    }
    @Test void exigeTokenValido() throws Exception {
        mvc.perform(get("/api/servicos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/servicos").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        String[] partes = clienteToken.split("\\.");
        String adulterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"sub\":\"outro\",\"papel\":\"ADMIN\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + partes[2];
        mvc.perform(get("/api/servicos").header("Authorization", "Bearer " + adulterado)).andExpect(status().isUnauthorized());
    }
    @Test void loginIncorretoRetorna401() throws Exception {
        enviar("/api/auth/login", new LoginRequest(empresa.slug(), cliente.email(), "senhaerrada"), null).andExpect(status().isUnauthorized());
    }
    @Test void rejeitaTokenExpirado() throws Exception {
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("agendamento-api").subject(cliente.id().toString())
            .issuedAt(java.time.Instant.now().minusSeconds(7200))
            .expiresAt(java.time.Instant.now().minusSeconds(3600))
            .claim("empresaId", empresa.id().toString()).claim("papel", "CLIENTE").build();
        var header = org.springframework.security.oauth2.jwt.JwsHeader
            .with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        String expirado = jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims)).getTokenValue();
        mvc.perform(get("/api/servicos").header("Authorization", "Bearer " + expirado)).andExpect(status().isUnauthorized());
    }
    @Test void periodoNaoPodeUltrapassar31Dias() throws Exception {
        mvc.perform(get("/api/agendamentos").param("de", inicio.toString()).param("ate", inicio.plusDays(31).plusSeconds(1).toString())
            .header("Authorization", "Bearer " + adminToken)).andExpect(status().isBadRequest());
    }
    @Test void somenteAdminCadastraUsuariosEServicos() throws Exception {
        enviar("/api/usuarios", new UsuarioRequest("Davi", "davi@exemplo.com", "SenhaTeste123", Papel.ADMIN), clienteToken)
            .andExpect(status().isForbidden());
        enviar("/api/servicos", new ServicoRequest("Corte", 20, BigDecimal.TEN), profissionalToken).andExpect(status().isForbidden());
        enviar("/api/usuarios", new UsuarioRequest("Davi", "davi@exemplo.com", "SenhaTeste123", Papel.CLIENTE), adminToken)
            .andExpect(status().isCreated()).andExpect(jsonPath("senha").doesNotExist());
    }
    @Test void rejeitaDuplicadosEDadosInvalidos() throws Exception {
        enviar("/api/usuarios", new UsuarioRequest("Carla", cliente.email(), "SenhaTeste123", Papel.CLIENTE), adminToken)
            .andExpect(status().isConflict());
        enviar("/api/servicos", new ServicoRequest("", 0, new BigDecimal("-1")), adminToken).andExpect(status().isBadRequest());
        enviar("/api/agendamentos", reserva(inicio.minusDays(4)), clienteToken).andExpect(status().isBadRequest());
    }
    @Test void calculaFimERecusaSobreposicao() throws Exception {
        JsonNode agenda = criado();
        assertThat(OffsetDateTime.parse(agenda.get("fim").asText()).toInstant()).isEqualTo(inicio.plusMinutes(30).toInstant());
        enviar("/api/agendamentos", reserva(inicio.plusMinutes(10)), clienteToken).andExpect(status().isConflict());
        enviar("/api/agendamentos", reserva(inicio.minusMinutes(10)), clienteToken).andExpect(status().isConflict());
        enviar("/api/agendamentos", reserva(inicio), clienteToken).andExpect(status().isConflict());
        enviar("/api/agendamentos", reserva(inicio.plusMinutes(30)), clienteToken).andExpect(status().isCreated());
    }
    @Test void cancelamentoLiberaHorario() throws Exception {
        String id = criado().get("id").asText();
        mvc.perform(patch("/api/agendamentos/{id}/cancelar", id).header("Authorization", "Bearer " + clienteToken))
            .andExpect(status().isNoContent());
        enviar("/api/agendamentos", reserva(inicio), clienteToken).andExpect(status().isCreated());
    }
    @Test void clienteNaoPodeReservarNemCancelarParaOutro() throws Exception {
        Usuario outro = auth.cadastrarUsuario(empresa.id(), new UsuarioRequest("Outro", "outro@exemplo.com", "SenhaTeste123", Papel.CLIENTE));
        Map<String, Object> body = new HashMap<>(reserva(inicio));
        body.put("clienteId", outro.id());
        enviar("/api/agendamentos", body, clienteToken).andExpect(status().isForbidden());
        String id = criado().get("id").asText();
        mvc.perform(patch("/api/agendamentos/{id}/cancelar", id).header("Authorization", "Bearer " + token(outro.email())))
            .andExpect(status().isForbidden());
    }
    @Test void profissionalSomenteOperaPropriaAgenda() throws Exception {
        Usuario outro = auth.cadastrarUsuario(empresa.id(), new UsuarioRequest("Outro", "outro@exemplo.com", "SenhaTeste123", Papel.PROFISSIONAL));
        Map<String, Object> body = new HashMap<>(reserva(inicio));
        body.put("profissionalId", outro.id()); body.put("clienteId", cliente.id());
        enviar("/api/agendamentos", body, profissionalToken).andExpect(status().isForbidden());
        body.put("profissionalId", profissional.id());
        enviar("/api/agendamentos", body, profissionalToken).andExpect(status().isCreated());
    }
    @Test void isolaDadosEntreEmpresas() throws Exception {
        Empresa outra = auth.cadastrarEmpresa(new EmpresaRequest("Outra", "outra-" + UUID.randomUUID(), "Admin", "admin@exemplo.com", "SenhaTeste123"));
        String outroToken = auth.login(new LoginRequest(outra.slug(), "admin@exemplo.com", "SenhaTeste123")).accessToken();
        mvc.perform(get("/api/servicos").header("Authorization", "Bearer " + outroToken)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/servicos/{id}", servico.id()).header("Authorization", "Bearer " + outroToken)).andExpect(status().isNotFound());
        Map<String, Object> body = new HashMap<>(reserva(inicio)); body.put("clienteId", cliente.id());
        enviar("/api/agendamentos", body, outroToken).andExpect(status().isNotFound());
        String id = criado().get("id").asText();
        mvc.perform(patch("/api/agendamentos/{id}/cancelar", id).header("Authorization", "Bearer " + outroToken)).andExpect(status().isNotFound());
    }
    @Test void recusaServicoDeOutraEmpresa() throws Exception {
        Empresa outra = auth.cadastrarEmpresa(new EmpresaRequest("Outra", "outra-" + UUID.randomUUID(), "Admin", "admin@exemplo.com", "SenhaTeste123"));
        Servico externo = servicos.cadastrar(outra.id(), new ServicoRequest("Externo", 30, BigDecimal.TEN));
        Map<String, Object> body = new HashMap<>(reserva(inicio)); body.put("servicoId", externo.id());
        enviar("/api/agendamentos", body, clienteToken).andExpect(status().isNotFound());
    }
    @Test void agendaRespeitaPapelEPeriodo() throws Exception {
        criado();
        Usuario outro = auth.cadastrarUsuario(empresa.id(), new UsuarioRequest("Outro", "outro@exemplo.com", "SenhaTeste123", Papel.CLIENTE));
        for (String token : List.of(adminToken, profissionalToken, clienteToken)) {
            mvc.perform(get("/api/agendamentos").param("de", inicio.toString()).param("ate", inicio.plusDays(1).toString())
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        }
        mvc.perform(get("/api/agendamentos").param("de", inicio.toString()).param("ate", inicio.plusDays(1).toString())
            .header("Authorization", "Bearer " + token(outro.email()))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/agendamentos").param("de", inicio.toString()).param("ate", inicio.minusDays(1).toString())
            .header("Authorization", "Bearer " + adminToken)).andExpect(status().isBadRequest());
    }
    @Test void servicoDesativadoMantemHistoricoMasNaoAceitaReserva() throws Exception {
        criado();
        mvc.perform(delete("/api/servicos/{id}", servico.id()).header("Authorization", "Bearer " + adminToken)).andExpect(status().isNoContent());
        enviar("/api/agendamentos", reserva(inicio.plusHours(1)), clienteToken).andExpect(status().isBadRequest());
        assertThat(servicoRepository.buscar(empresa.id(), servico.id()).orElseThrow().ativo()).isFalse();
    }
    @Test void duasReservasSimultaneasGeramSomenteUmAgendamento() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch pronto = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        Callable<Integer> tarefa = () -> {
            pronto.countDown();
            if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo esgotado");
            try {
                agendas.cadastrar(new UsuarioAtual(cliente.id(), empresa.id(), Papel.CLIENTE),
                    new AgendamentoRequest(servico.id(), profissional.id(), null, inicio));
                return 201;
            } catch (ApiException ex) { return ex.getStatus().value(); }
        };
        try {
            Future<Integer> a = pool.submit(tarefa);
            Future<Integer> b = pool.submit(tarefa);
            assertThat(pronto.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        } finally { iniciar.countDown(); pool.shutdownNow(); }
    }
    @Test void swaggerDisponivelSemLogin() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("components.securitySchemes.bearerAuth.scheme").value("bearer"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
