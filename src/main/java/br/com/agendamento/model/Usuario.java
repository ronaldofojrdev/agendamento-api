package br.com.agendamento.model;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
public record Usuario(UUID id, UUID empresaId, String nome, String email,
                      @JsonIgnore String senha, Papel papel) {}
