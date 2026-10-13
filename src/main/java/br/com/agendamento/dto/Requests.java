package br.com.agendamento.dto;

import br.com.agendamento.model.Papel;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Requests {
    private Requests() {}

    public record EmpresaRequest(
        @NotBlank @Size(max=120) String nome,
        @NotBlank @Pattern(regexp="[a-z0-9-]{3,80}") String slug,
        @NotBlank @Size(max=120) String adminNome,
        @NotBlank @Email @Size(max=180) String adminEmail,
        @NotBlank @Size(min=8, max=64) String adminSenha) {}

    public record LoginRequest(@NotBlank String empresaSlug,
        @NotBlank @Email String email, @NotBlank @Size(max=64) String senha) {}

    public record UsuarioRequest(@NotBlank @Size(max=120) String nome,
        @NotBlank @Email @Size(max=180) String email,
        @NotBlank @Size(min=8, max=64) String senha, @NotNull Papel papel) {}

    public record ServicoRequest(@NotBlank @Size(max=120) String nome,
        @NotNull @Min(5) @Max(480) Integer duracaoMinutos,
        @NotNull @DecimalMin("0.00") @Digits(integer=8, fraction=2) BigDecimal preco) {}

    public record AgendamentoRequest(@NotNull UUID servicoId,
        @NotNull UUID profissionalId, UUID clienteId,
        @NotNull @Future OffsetDateTime inicio) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
