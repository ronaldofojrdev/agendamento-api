package br.com.agendamento.service;
import br.com.agendamento.dto.Requests.*;
import br.com.agendamento.model.*;
import br.com.agendamento.repository.*;
import br.com.agendamento.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuthService {
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final long expiration;
    private final String senhaFicticia;
    public AuthService(EmpresaRepository empresas, UsuarioRepository usuarios, PasswordEncoder encoder,
                       JwtEncoder jwtEncoder, @Value("${app.jwt.expiration}") long expiration) {
        this.empresas = empresas; this.usuarios = usuarios; this.encoder = encoder;
        this.jwtEncoder = jwtEncoder; this.expiration = expiration;
        this.senhaFicticia = encoder.encode(UUID.randomUUID().toString());
    }
    @Transactional
    public Empresa cadastrarEmpresa(EmpresaRequest request) {
        Empresa empresa = new Empresa(UUID.randomUUID(), request.nome().trim(), request.slug());
        empresas.salvar(empresa);
        cadastrarUsuario(empresa.id(), new UsuarioRequest(request.adminNome(), request.adminEmail(), request.adminSenha(), Papel.ADMIN));
        return empresa;
    }
    public Usuario cadastrarUsuario(UUID empresaId, UsuarioRequest request) {
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha deve ter no maximo 72 bytes");
        Usuario usuario = new Usuario(UUID.randomUUID(), empresaId, request.nome().trim(),
            request.email().trim().toLowerCase(Locale.ROOT), encoder.encode(request.senha()), request.papel());
        usuarios.salvar(usuario);
        return usuario;
    }
    public TokenResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = empresas.buscarPorSlug(request.empresaSlug())
            .flatMap(empresa -> usuarios.buscarPorEmail(empresa.id(), request.email().trim().toLowerCase(Locale.ROOT)));
        boolean senhaValida = encoder.matches(request.senha(), encontrado.map(Usuario::senha).orElse(senhaFicticia));
        if (encontrado.isEmpty() || !senhaValida) throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        Usuario usuario = encontrado.get();
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("agendamento-api").subject(usuario.id().toString())
            .issuedAt(agora).expiresAt(agora.plusSeconds(expiration))
            .claim("empresaId", usuario.empresaId().toString()).claim("papel", usuario.papel().name()).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiration);
    }
}
