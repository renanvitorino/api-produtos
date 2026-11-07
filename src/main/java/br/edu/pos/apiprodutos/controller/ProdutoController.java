
package br.edu.pos.apiprodutos.controller;

import br.edu.pos.apiprodutos.model.Produto;
import br.edu.pos.apiprodutos.service.ProdutoService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.BadRequestResponse;
import io.javalin.openapi.HttpMethod;
import io.javalin.http.HttpStatus;

import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class ProdutoController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProdutoController.class);

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    public void registrarRotas(Javalin app) {

        app.get("/produtos", this::listarProdutos);

        app.get("/produtos/estoque-baixo", this::listarEstoqueBaixo);

        app.post(
                "/produtos/{id}/estoque/entrada",
                this::entradaEstoque
        );

        app.post(
                "/produtos/{id}/estoque/saida",
                this::saidaEstoque
        );

        app.get("/produtos/{id}", this::buscarProduto);

        app.post("/produtos", this::cadastrarProduto);

        app.put("/produtos/{id}", this::atualizarProduto);

        app.delete("/produtos/{id}", this::excluirProduto);

        app.exception(NoSuchElementException.class, (erro, ctx) -> {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new MensagemErro(erro.getMessage()));
        });

        app.exception(IllegalArgumentException.class, (erro, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new MensagemErro(erro.getMessage()));
        });

        app.exception(BadRequestResponse.class, (erro, ctx) -> {
            LOGGER.warn("Requisição JSON inválida: {}", erro.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new MensagemErro("JSON inválido."));
        });

        app.exception(Exception.class, (erro, ctx) -> {
            LOGGER.error("Erro não tratado ao processar requisição.", erro);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new MensagemErro("Erro interno do servidor."));
        });
    }

    @OpenApi(
            path = "/produtos",
            methods = {HttpMethod.GET},
            summary = "Listar todos os produtos",
            tags = {"Produtos"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(from = Produto[].class)
                            }
                    )
            }
    )
    public void listarProdutos(Context ctx) {
        ctx.json(service.listar());
    }


    @OpenApi(
            path = "/produtos/{id}",
            methods = {HttpMethod.GET},
            summary = "Buscar produto por ID",
            tags = {"Produtos"},
            pathParams = {
                    @OpenApiParam(
                            name = "id",
                            type = Long.class,
                            description = "Identificador do produto",
                            required = true
                    )
            },
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(from = Produto.class)
                            }
                    ),
                    @OpenApiResponse(status = "404")
            }
    )
    public void buscarProduto(Context ctx) {

        Long id = parseId(ctx.pathParam("id"));

        Produto produto = service.buscarPorId(id);

        ctx.json(produto);
    }


    @OpenApi(
            path = "/produtos",
            methods = {HttpMethod.POST},
            summary = "Cadastrar um novo produto",
            tags = {"Produtos"},
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = Produto.class)
                    }
            ),
            responses = {
                    @OpenApiResponse(
                            status = "201",
                            content = {
                                    @OpenApiContent(from = Produto.class)
                            }
                    ),
                    @OpenApiResponse(status = "400")
            }
    )
    public void cadastrarProduto(Context ctx) {

        Produto dados = ctx.bodyAsClass(Produto.class);
        if (dados == null) {
            throw new IllegalArgumentException(
                    "O corpo JSON deve conter os dados do produto."
            );
        }

        Produto produto = service.cadastrar(
                dados.getNome(),
                dados.getPreco(),
                dados.getQuantidadeEstoque()
        );

        ctx.status(HttpStatus.CREATED);
        ctx.json(produto);
    }

    @OpenApi(
            path = "/produtos/{id}/estoque/entrada",
            methods = {HttpMethod.POST},
            summary = "Adicionar unidades ao estoque",
            tags = {"Produtos"},
            pathParams = {
                    @OpenApiParam(
                            name = "id",
                            type = Long.class,
                            description = "Identificador do produto",
                            required = true
                    )
            },
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = MovimentacaoEstoque.class)
                    }
            ),
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(from = Produto.class)
                            }
                    ),
                    @OpenApiResponse(status = "400"),
                    @OpenApiResponse(status = "404")
            }
    )
    public void entradaEstoque(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        MovimentacaoEstoque movimentacao =
                ctx.bodyAsClass(MovimentacaoEstoque.class);
        if (movimentacao == null) {
            throw new IllegalArgumentException(
                    "O corpo JSON deve conter a quantidade."
            );
        }

        ctx.json(service.adicionarEstoque(id, movimentacao.quantidade()));
    }

    @OpenApi(
            path = "/produtos/{id}/estoque/saida",
            methods = {HttpMethod.POST},
            summary = "Retirar unidades do estoque",
            tags = {"Produtos"},
            pathParams = {
                    @OpenApiParam(
                            name = "id",
                            type = Long.class,
                            description = "Identificador do produto",
                            required = true
                    )
            },
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = MovimentacaoEstoque.class)
                    }
            ),
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(from = Produto.class)
                            }
                    ),
                    @OpenApiResponse(status = "400"),
                    @OpenApiResponse(status = "404")
            }
    )
    public void saidaEstoque(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        MovimentacaoEstoque movimentacao =
                ctx.bodyAsClass(MovimentacaoEstoque.class);
        if (movimentacao == null) {
            throw new IllegalArgumentException(
                    "O corpo JSON deve conter a quantidade."
            );
        }

        ctx.json(service.retirarEstoque(id, movimentacao.quantidade()));
    }

    @OpenApi(
            path = "/produtos/estoque-baixo",
            methods = {HttpMethod.GET},
            summary = "Listar produtos com estoque baixo",
            tags = {"Produtos"},
            queryParams = {
                    @OpenApiParam(
                            name = "limite",
                            type = Integer.class,
                            description = "Quantidade máxima em estoque",
                            required = true
                    )
            },
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(from = Produto[].class)
                            }
                    ),
                    @OpenApiResponse(status = "400")
            }
    )
    public void listarEstoqueBaixo(Context ctx) {
        String parametro = ctx.queryParam("limite");
        if (parametro == null) {
            throw new IllegalArgumentException(
                    "O parâmetro limite é obrigatório e deve ser um inteiro."
            );
        }

        int limite;
        try {
            limite = Integer.parseInt(parametro);
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException(
                    "O limite deve ser um inteiro.",
                    erro
            );
        }

        ctx.json(service.listarEstoqueBaixo(limite));
    }


    @OpenApi(
            path = "/produtos/{id}",
            methods = {HttpMethod.PUT},
            summary = "Atualizar um produto existente",
            tags = {"Produtos"},
            pathParams = {
                    @OpenApiParam(
                            name = "id",
                            type = Long.class,
                            description = "Identificador do produto",
                            required = true
                    )
            },
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = Produto.class)
                    }
            ),
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {
                                    @OpenApiContent(from = Produto.class)
                            }
                    ),
                    @OpenApiResponse(status = "400"),
                    @OpenApiResponse(status = "404")
            }
    )
    public void atualizarProduto(Context ctx) {

        Long id = parseId(ctx.pathParam("id"));

        Produto dados = ctx.bodyAsClass(Produto.class);
        if (dados == null) {
            throw new IllegalArgumentException(
                    "O corpo JSON deve conter os dados do produto."
            );
        }

        Produto atualizado = service.atualizar(
                id,
                dados.getNome(),
                dados.getPreco()
        );

        ctx.json(atualizado);
    }


    @OpenApi(
            path = "/produtos/{id}",
            methods = {HttpMethod.DELETE},
            summary = "Excluir um produto",
            tags = {"Produtos"},
            pathParams = {
                    @OpenApiParam(
                            name = "id",
                            type = Long.class,
                            description = "Identificador do produto",
                            required = true
                    )
            },
            responses = {
                    @OpenApiResponse(status = "204"),
                    @OpenApiResponse(status = "404")
            }
    )
    public void excluirProduto(Context ctx) {

        Long id = parseId(ctx.pathParam("id"));

        service.excluir(id);

        ctx.status(HttpStatus.NO_CONTENT);
    }

    private Long parseId(String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException erro) {
            throw new IllegalArgumentException(
                    "O ID do produto deve ser numérico.",
                    erro
            );
        }
    }

    public record MovimentacaoEstoque(Integer quantidade) {
    }

    public record MensagemErro(String erro) {
    }
}
