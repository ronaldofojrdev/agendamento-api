package br.com.agendamento.model;
import java.util.UUID;
import java.time.OffsetDateTime;
public record Agendamento(UUID id, UUID empresaId, UUID servicoId, UUID profissionalId,
                          UUID clienteId, OffsetDateTime inicio, OffsetDateTime fim, String status) {}
