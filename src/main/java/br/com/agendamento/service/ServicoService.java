package br.com.agendamento.service;
import br.com.agendamento.dto.Requests.ServicoRequest;
import br.com.agendamento.model.Servico;
import br.com.agendamento.repository.ServicoRepository;
import br.com.agendamento.exception.ApiException;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ServicoService {
    private final ServicoRepository repository;
    public ServicoService(ServicoRepository repository) { this.repository = repository; }
    public Servico cadastrar(UUID empresaId, ServicoRequest request) {
        Servico servico = new Servico(UUID.randomUUID(), empresaId, request.nome().trim(), request.duracaoMinutos(), request.preco(), true);
        repository.salvar(servico);
        return servico;
    }
    public List<Servico> listar(UUID empresaId) { return repository.listar(empresaId); }
    public Servico atualizar(UUID empresaId, UUID id, ServicoRequest request) {
        Servico atual = repository.buscar(empresaId, id).orElseThrow(ApiException::naoEncontrado);
        Servico servico = new Servico(id, empresaId, request.nome().trim(), request.duracaoMinutos(), request.preco(), atual.ativo());
        repository.atualizar(servico);
        return servico;
    }
    public void desativar(UUID empresaId, UUID id) {
        if (repository.desativar(empresaId, id) == 0) throw ApiException.naoEncontrado();
    }
}
