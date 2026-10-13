CREATE TABLE empresas (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    senha VARCHAR(100) NOT NULL,
    papel VARCHAR(20) NOT NULL CHECK (papel IN ('ADMIN', 'PROFISSIONAL', 'CLIENTE')),
    UNIQUE (empresa_id, email),
    UNIQUE (empresa_id, id)
);

CREATE TABLE servicos (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    nome VARCHAR(120) NOT NULL,
    duracao_minutos INTEGER NOT NULL CHECK (duracao_minutos BETWEEN 5 AND 480),
    preco DECIMAL(10,2) NOT NULL CHECK (preco >= 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, id)
);

CREATE TABLE agendamentos (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    servico_id UUID NOT NULL,
    profissional_id UUID NOT NULL,
    cliente_id UUID NOT NULL,
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fim TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AGENDADO', 'CANCELADO')),
    CHECK (fim > inicio),
    FOREIGN KEY (empresa_id, servico_id) REFERENCES servicos(empresa_id, id),
    FOREIGN KEY (empresa_id, profissional_id) REFERENCES usuarios(empresa_id, id),
    FOREIGN KEY (empresa_id, cliente_id) REFERENCES usuarios(empresa_id, id)
);
CREATE INDEX idx_agenda_profissional ON agendamentos(empresa_id, profissional_id, inicio, fim);
CREATE INDEX idx_agenda_cliente ON agendamentos(empresa_id, cliente_id, inicio);
