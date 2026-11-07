package br.edu.pos.apiprodutos.repository;

import br.edu.pos.apiprodutos.model.Produto;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository {

    void salvar(Produto produto);

    List<Produto> listarTodos();

    Optional<Produto> buscarPorId(Long id);

    boolean adicionarEstoque(Long id, int quantidade);

    boolean retirarEstoque(Long id, int quantidade);

    List<Produto> listarEstoqueBaixo(int limite);

    void excluir(Long id);
}