package br.edu.pos.apiprodutos.service;

import br.edu.pos.apiprodutos.model.Produto;
import br.edu.pos.apiprodutos.repository.ProdutoRepository;

import java.util.List;
import java.util.NoSuchElementException;

public class ProdutoService {

    private final ProdutoRepository repository;

    // Injeção de dependência pelo construtor
    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public Produto cadastrar(String nome, double preco) {
        return cadastrar(nome, preco, 0);
    }

    public Produto cadastrar(
            String nome,
            double preco,
            Integer quantidadeEstoque
    ) {

        validarProduto(nome, preco);
        validarEstoqueInicial(quantidadeEstoque);

        Produto produto = new Produto(
                null,
                nome,
                preco,
                quantidadeEstoque
        );

        repository.salvar(produto);

        return produto;
    }

    public Produto adicionarEstoque(Long id, Integer quantidade) {
        validarQuantidade(quantidade);

        if (!repository.adicionarEstoque(id, quantidade)) {
            buscarPorId(id);
            throw new IllegalArgumentException(
                    "A quantidade excede o limite máximo do estoque."
            );
        }

        return buscarPorId(id);
    }

    public Produto retirarEstoque(Long id, Integer quantidade) {
        validarQuantidade(quantidade);

        if (!repository.retirarEstoque(id, quantidade)) {
            buscarPorId(id);
            throw new IllegalArgumentException(
                    "Estoque insuficiente para realizar a saída."
            );
        }

        return buscarPorId(id);
    }

    public List<Produto> listarEstoqueBaixo(int limite) {
        if (limite < 0) {
            throw new IllegalArgumentException(
                    "O limite deve ser maior ou igual a zero."
            );
        }
        return repository.listarEstoqueBaixo(limite);
    }

    public List<Produto> listar() {
        return repository.listarTodos();
    }

    public Produto buscarPorId(Long id) {

        return repository.buscarPorId(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Produto não encontrado: " + id
                        )
                );
    }

    public Produto atualizar(Long id, String nome, double preco) {

        validarProduto(nome, preco);

        Produto produto = buscarPorId(id);

        produto.setNome(nome);
        produto.setPreco(preco);

        repository.salvar(produto);

        return produto;
    }

    public void excluir(Long id) {

        buscarPorId(id);

        repository.excluir(id);
    }

    private void validarProduto(String nome, double preco) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do produto é obrigatório."
            );
        }

        if (!Double.isFinite(preco) || preco <= 0) {
            throw new IllegalArgumentException(
                    "O preço deve ser maior que zero."
            );
        }
    }

    private void validarEstoqueInicial(Integer quantidadeEstoque) {
        if (quantidadeEstoque == null || quantidadeEstoque < 0) {
            throw new IllegalArgumentException(
                    "A quantidade inicial em estoque deve ser maior ou igual a zero."
            );
        }
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero."
            );
        }
    }
}