
package br.edu.pos.apiprodutos.repository;

import br.edu.pos.apiprodutos.config.Database;
import br.edu.pos.apiprodutos.model.Produto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositoryJDBC implements ProdutoRepository {

    @Override
    public void salvar(Produto produto) {

        if (produto.getId() == null) {
            inserir(produto);
        } else {
            atualizar(produto);
        }
    }

    private void inserir(Produto produto) {

        String sql = """
                INSERT INTO produtos (nome, preco, quantidade_estoque)
                VALUES (?, ?, ?)
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {

            comando.setString(1, produto.getNome());
            comando.setDouble(2, produto.getPreco());
            comando.setInt(3, produto.getQuantidadeEstoque());

            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {

                if (chaves.next()) {
                    produto.setId(chaves.getLong(1));
                } else {
                    throw new SQLException(
                            "O banco não retornou o ID do produto"
                    );
                }
            }

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao inserir produto",
                    erro
            );
        }
    }

    private void atualizar(Produto produto) {

        String sql = """
                UPDATE produtos
                SET nome = ?, preco = ?
                WHERE id = ?
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setString(1, produto.getNome());
            comando.setDouble(2, produto.getPreco());
            comando.setLong(3, produto.getId());

            comando.executeUpdate();

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao atualizar produto",
                    erro
            );
        }
    }

    @Override
    public List<Produto> listarTodos() {

        List<Produto> produtos = new ArrayList<>();

        String sql = """
                SELECT id, nome, preco, quantidade_estoque
                FROM produtos
                ORDER BY id
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()
        ) {

            while (resultado.next()) {
                produtos.add(converterProduto(resultado));
            }

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao listar produtos",
                    erro
            );
        }

        return produtos;
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {

        String sql = """
                SELECT id, nome, preco, quantidade_estoque
                FROM produtos
                WHERE id = ?
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setLong(1, id);

            try (ResultSet resultado = comando.executeQuery()) {

                if (resultado.next()) {
                    return Optional.of(converterProduto(resultado));
                }
            }

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao buscar produto",
                    erro
            );
        }

        return Optional.empty();
    }

    @Override
    public boolean adicionarEstoque(Long id, int quantidade) {

        String sql = """
                UPDATE produtos
                SET quantidade_estoque = quantidade_estoque + ?
                WHERE id = ? AND quantidade_estoque <= ?
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setInt(1, quantidade);
            comando.setLong(2, id);
            comando.setInt(3, Integer.MAX_VALUE - quantidade);

            return comando.executeUpdate() == 1;

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao adicionar estoque",
                    erro
            );
        }
    }

    @Override
    public boolean retirarEstoque(Long id, int quantidade) {

        String sql = """
                UPDATE produtos
                SET quantidade_estoque = quantidade_estoque - ?
                WHERE id = ? AND quantidade_estoque >= ?
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setInt(1, quantidade);
            comando.setLong(2, id);
            comando.setInt(3, quantidade);

            return comando.executeUpdate() == 1;

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao retirar estoque",
                    erro
            );
        }
    }

    @Override
    public List<Produto> listarEstoqueBaixo(int limite) {

        List<Produto> produtos = new ArrayList<>();

        String sql = """
                SELECT id, nome, preco, quantidade_estoque
                FROM produtos
                WHERE quantidade_estoque <= ?
                ORDER BY id
                """;

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setInt(1, limite);

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    produtos.add(converterProduto(resultado));
                }
            }

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao listar produtos com estoque baixo",
                    erro
            );
        }

        return produtos;
    }

    @Override
    public void excluir(Long id) {

        String sql = "DELETE FROM produtos WHERE id = ?";

        try (
                Connection conexao = Database.conectar();
                PreparedStatement comando = conexao.prepareStatement(sql)
        ) {

            comando.setLong(1, id);
            comando.executeUpdate();

        } catch (SQLException erro) {
            throw new RuntimeException(
                    "Erro ao excluir produto",
                    erro
            );
        }
    }

    private Produto converterProduto(ResultSet resultado)
            throws SQLException {

        return new Produto(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getDouble("preco"),
                resultado.getInt("quantidade_estoque")
        );
    }
}
