package br.com.agendamento.model;
import java.util.UUID;
import java.math.BigDecimal;
public record Servico(UUID id, UUID empresaId, String nome, int duracaoMinutos,
                      BigDecimal preco, boolean ativo) {}
