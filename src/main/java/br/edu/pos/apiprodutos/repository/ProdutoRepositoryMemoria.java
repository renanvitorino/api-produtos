package br.edu.pos.apiprodutos.repository;

import br.edu.pos.apiprodutos.model.Produto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositoryMemoria implements ProdutoRepository {

    private final List<Produto> produtos = new ArrayList<>();

    @Override
    public void salvar(Produto produto) {

        produtos.removeIf(p -> p.getId().equals(produto.getId()));

        produtos.add(produto);
    }

    @Override
    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos);
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {

        return produtos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    @Override
    public boolean adicionarEstoque(Long id, int quantidade) {
        Optional<Produto> produto = buscarPorId(id);
        if (produto.isEmpty()
                || produto.get().getQuantidadeEstoque()
                > Integer.MAX_VALUE - quantidade) {
            return false;
        }
        produto.get().setQuantidadeEstoque(
                produto.get().getQuantidadeEstoque() + quantidade
        );
        return true;
    }

    @Override
    public boolean retirarEstoque(Long id, int quantidade) {
        Optional<Produto> produto = buscarPorId(id);
        if (produto.isEmpty()
                || produto.get().getQuantidadeEstoque() < quantidade) {
            return false;
        }
        produto.get().setQuantidadeEstoque(
                produto.get().getQuantidadeEstoque() - quantidade
        );
        return true;
    }

    @Override
    public List<Produto> listarEstoqueBaixo(int limite) {
        return produtos.stream()
                .filter(produto -> produto.getQuantidadeEstoque() <= limite)
                .toList();
    }

    @Override
    public void excluir(Long id) {

        produtos.removeIf(p -> p.getId().equals(id));
    }
}