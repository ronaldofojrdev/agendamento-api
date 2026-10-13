package br.com.agendamento.repository;
import br.com.agendamento.model.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class AgendamentoRepository {
    private final JdbcClient jdbc;
    public AgendamentoRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public boolean temConflito(UUID empresaId, UUID profissionalId, OffsetDateTime inicio, OffsetDateTime fim) {
        return jdbc.sql("""
            SELECT COUNT(*) FROM agendamentos
            WHERE empresa_id = ? AND profissional_id = ? AND status = 'AGENDADO'
              AND inicio < ? AND fim > ?
            """).params(empresaId, profissionalId, fim, inicio).query(Long.class).single() > 0;
    }
    public void salvar(Agendamento agenda) {
        jdbc.sql("""
            INSERT INTO agendamentos(id, empresa_id, servico_id, profissional_id, cliente_id, inicio, fim, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'AGENDADO')
            """).params(agenda.id(), agenda.empresaId(), agenda.servicoId(), agenda.profissionalId(),
                agenda.clienteId(), agenda.inicio(), agenda.fim()).update();
    }
    public Optional<Agendamento> buscar(UUID empresaId, UUID id) {
        return jdbc.sql("SELECT * FROM agendamentos WHERE empresa_id = ? AND id = ?")
            .params(empresaId, id).query(Agendamento.class).optional();
    }
    public List<Agendamento> listar(UUID empresaId, UUID usuarioId, Papel papel, OffsetDateTime de, OffsetDateTime ate) {
        String filtro = switch (papel) {
            case ADMIN -> "";
            case PROFISSIONAL -> " AND profissional_id = :usuarioId";
            case CLIENTE -> " AND cliente_id = :usuarioId";
        };
        return jdbc.sql("SELECT * FROM agendamentos WHERE empresa_id = :empresaId AND inicio >= :de AND inicio < :ate"
                + filtro + " ORDER BY inicio LIMIT 200")
            .param("empresaId", empresaId).param("usuarioId", usuarioId).param("de", de).param("ate", ate)
            .query(Agendamento.class).list();
    }
    public void cancelar(UUID empresaId, UUID id) {
        jdbc.sql("UPDATE agendamentos SET status = 'CANCELADO' WHERE empresa_id = ? AND id = ?")
            .params(empresaId, id).update();
    }
}
