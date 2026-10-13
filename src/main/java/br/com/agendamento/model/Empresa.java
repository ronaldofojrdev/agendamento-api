package br.com.agendamento.model;
import java.util.UUID;
public record Empresa(UUID id, String nome, String slug) {}
