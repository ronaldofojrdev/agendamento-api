package br.com.agendamento.repository;
import br.com.agendamento.model.Servico;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class ServicoRepository {
    private final JdbcClient jdbc;
    public ServicoRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public void salvar(Servico servico) {
        jdbc.sql("INSERT INTO servicos(id, empresa_id, nome, duracao_minutos, preco, ativo) VALUES (?, ?, ?, ?, ?, true)")
            .params(servico.id(), servico.empresaId(), servico.nome(), servico.duracaoMinutos(), servico.preco()).update();
    }
    public Optional<Servico> buscar(UUID empresaId, UUID id) {
        return jdbc.sql("SELECT * FROM servicos WHERE empresa_id = ? AND id = ?")
            .params(empresaId, id).query(Servico.class).optional();
    }
    public List<Servico> listar(UUID empresaId) {
        return jdbc.sql("SELECT * FROM servicos WHERE empresa_id = ? AND ativo = true ORDER BY nome LIMIT 200")
            .param(empresaId).query(Servico.class).list();
    }
    public void atualizar(Servico servico) {
        jdbc.sql("UPDATE servicos SET nome = ?, duracao_minutos = ?, preco = ? WHERE empresa_id = ? AND id = ?")
            .params(servico.nome(), servico.duracaoMinutos(), servico.preco(), servico.empresaId(), servico.id()).update();
    }
    public int desativar(UUID empresaId, UUID id) {
        return jdbc.sql("UPDATE servicos SET ativo = false WHERE empresa_id = ? AND id = ?")
            .params(empresaId, id).update();
    }
}
