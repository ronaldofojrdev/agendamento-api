package br.com.agendamento.repository;
import br.com.agendamento.model.Usuario;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class UsuarioRepository {
    private final JdbcClient jdbc;
    public UsuarioRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public void salvar(Usuario usuario) {
        jdbc.sql("INSERT INTO usuarios(id, empresa_id, nome, email, senha, papel) VALUES (?, ?, ?, ?, ?, ?)")
            .params(usuario.id(), usuario.empresaId(), usuario.nome(), usuario.email(), usuario.senha(), usuario.papel().name()).update();
    }
    public Optional<Usuario> buscarPorEmail(UUID empresaId, String email) {
        return jdbc.sql("SELECT * FROM usuarios WHERE empresa_id = ? AND email = ?")
            .params(empresaId, email).query(Usuario.class).optional();
    }
    public Optional<Usuario> buscarPorId(UUID empresaId, UUID id) {
        return jdbc.sql("SELECT * FROM usuarios WHERE empresa_id = ? AND id = ?")
            .params(empresaId, id).query(Usuario.class).optional();
    }
    public Optional<Usuario> bloquearProfissional(UUID empresaId, UUID id) {
        // O bloqueio dura ate o fim da transacao e serializa reservas do mesmo profissional.
        return jdbc.sql("SELECT * FROM usuarios WHERE empresa_id = ? AND id = ? FOR UPDATE")
            .params(empresaId, id).query(Usuario.class).optional();
    }
    public List<Usuario> listar(UUID empresaId) {
        return jdbc.sql("SELECT * FROM usuarios WHERE empresa_id = ? ORDER BY nome LIMIT 200")
            .param(empresaId).query(Usuario.class).list();
    }
    public List<Usuario> listarProfissionais(UUID empresaId) {
        return jdbc.sql("SELECT * FROM usuarios WHERE empresa_id = ? AND papel = 'PROFISSIONAL' ORDER BY nome LIMIT 200")
            .param(empresaId).query(Usuario.class).list();
    }
}
