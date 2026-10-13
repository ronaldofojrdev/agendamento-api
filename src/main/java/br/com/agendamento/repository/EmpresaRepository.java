package br.com.agendamento.repository;
import br.com.agendamento.model.Empresa;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class EmpresaRepository {
    private final JdbcClient jdbc;
    public EmpresaRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public void salvar(Empresa empresa) {
        jdbc.sql("INSERT INTO empresas(id, nome, slug) VALUES (?, ?, ?)")
            .params(empresa.id(), empresa.nome(), empresa.slug()).update();
    }
    public Optional<Empresa> buscarPorSlug(String slug) {
        return jdbc.sql("SELECT * FROM empresas WHERE slug = ?").param(slug).query(Empresa.class).optional();
    }
    public Optional<Empresa> buscarPorId(UUID id) {
        return jdbc.sql("SELECT * FROM empresas WHERE id = ?").param(id).query(Empresa.class).optional();
    }
}
