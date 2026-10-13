package br.com.agendamento.security;
import br.com.agendamento.model.Papel;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;

public record UsuarioAtual(UUID id, UUID empresaId, Papel papel) {
    public static UsuarioAtual de(Jwt jwt) {
        return new UsuarioAtual(UUID.fromString(jwt.getSubject()),
            UUID.fromString(jwt.getClaimAsString("empresaId")), Papel.valueOf(jwt.getClaimAsString("papel")));
    }
}
