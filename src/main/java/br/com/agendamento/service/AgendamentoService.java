package br.com.agendamento.service;
import br.com.agendamento.dto.Requests.AgendamentoRequest;
import br.com.agendamento.exception.ApiException;
import br.com.agendamento.model.*;
import br.com.agendamento.repository.*;
import br.com.agendamento.security.UsuarioAtual;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class AgendamentoService {
    private final AgendamentoRepository agendas;
    private final UsuarioRepository usuarios;
    private final ServicoRepository servicos;
    public AgendamentoService(AgendamentoRepository agendas, UsuarioRepository usuarios, ServicoRepository servicos) {
        this.agendas = agendas; this.usuarios = usuarios; this.servicos = servicos;
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Agendamento cadastrar(UsuarioAtual atual, AgendamentoRequest request) {
        if (!request.inicio().isAfter(OffsetDateTime.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "O horario deve estar no futuro");
        if (atual.papel() == Papel.PROFISSIONAL && !atual.id().equals(request.profissionalId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "O profissional pode agendar somente na propria agenda");
        UUID clienteId = atual.papel() == Papel.CLIENTE ? atual.id() : request.clienteId();
        if (atual.papel() == Papel.CLIENTE && request.clienteId() != null && !atual.id().equals(request.clienteId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "O cliente pode agendar somente para si");
        if (clienteId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Informe o clienteId");
        Usuario profissional = usuarios.bloquearProfissional(atual.empresaId(), request.profissionalId()).orElseThrow(ApiException::naoEncontrado);
        Usuario cliente = usuarios.buscarPorId(atual.empresaId(), clienteId).orElseThrow(ApiException::naoEncontrado);
        if (profissional.papel() != Papel.PROFISSIONAL || cliente.papel() != Papel.CLIENTE)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confira os papeis do profissional e do cliente");
        Servico servico = servicos.buscar(atual.empresaId(), request.servicoId()).orElseThrow(ApiException::naoEncontrado);
        if (!servico.ativo()) throw new ApiException(HttpStatus.BAD_REQUEST, "Servico inativo");
        OffsetDateTime fim = request.inicio().plusMinutes(servico.duracaoMinutos());
        if (agendas.temConflito(atual.empresaId(), profissional.id(), request.inicio(), fim))
            throw new ApiException(HttpStatus.CONFLICT, "O profissional ja possui agendamento nesse horario");
        Agendamento agenda = new Agendamento(UUID.randomUUID(), atual.empresaId(), servico.id(), profissional.id(),
            clienteId, request.inicio(), fim, "AGENDADO");
        agendas.salvar(agenda);
        return agenda;
    }
    public List<Agendamento> listar(UsuarioAtual atual, OffsetDateTime de, OffsetDateTime ate) {
        if (!ate.isAfter(de) || Duration.between(de, ate).compareTo(Duration.ofDays(31)) > 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe um intervalo positivo de ate 31 dias");
        return agendas.listar(atual.empresaId(), atual.id(), atual.papel(), de, ate);
    }
    @Transactional
    public void cancelar(UsuarioAtual atual, UUID id) {
        Agendamento agenda = agendas.buscar(atual.empresaId(), id).orElseThrow(ApiException::naoEncontrado);
        if ((atual.papel() == Papel.CLIENTE && !atual.id().equals(agenda.clienteId())) ||
            (atual.papel() == Papel.PROFISSIONAL && !atual.id().equals(agenda.profissionalId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Voce nao pode cancelar esse agendamento");
        agendas.cancelar(atual.empresaId(), id);
    }
}
