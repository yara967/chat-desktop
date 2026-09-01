package com.example.chatdesktop.persistence;

import com.example.chatdesktop.model.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConversaDAO {

    // =========================================================
    // SALVAR MENSAGEM
    // =========================================================

    public static void salvarMensagem(
            int conversaId,
            ChatMessage mensagem
    ) {

        String sql =
                "INSERT INTO mensagens " +
                        "(conversa_id, tipo, conteudo) " +
                        "VALUES (?, ?, ?)";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    conversaId
            );

            statement.setString(
                    2,
                    mensagem.getRole()
            );

            statement.setString(
                    3,
                    mensagem.getContent()
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao salvar mensagem: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // CRIAR CONVERSA
    // =========================================================

    public static int criarConversa(
            String titulo
    ) {

        String sql =
                "INSERT INTO conversas " +
                        "(titulo, data_criacao) " +
                        "VALUES (?, datetime('now'))";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(
                                sql,
                                java.sql.Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(
                    1,
                    titulo
            );

            statement.executeUpdate();

            try (
                    java.sql.ResultSet resultado =
                            statement.getGeneratedKeys()
            ) {

                if (resultado.next()) {

                    return resultado.getInt(1);
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao criar conversa: "
                            + e.getMessage()
            );
        }

        return -1;
    }


    // =========================================================
    // BUSCAR MENSAGENS
    // =========================================================

    public static List<ChatMessage> buscarMensagens(
            int conversaId
    ) {

        List<ChatMessage> mensagens =
                new ArrayList<>();

        String sql =
                "SELECT tipo, conteudo " +
                        "FROM mensagens " +
                        "WHERE conversa_id = ? " +
                        "ORDER BY id ASC";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    conversaId
            );

            try (
                    java.sql.ResultSet resultado =
                            statement.executeQuery()
            ) {

                while (resultado.next()) {

                    mensagens.add(
                            new ChatMessage(
                                    resultado.getString(
                                            "tipo"
                                    ),
                                    resultado.getString(
                                            "conteudo"
                                    )
                            )
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao buscar mensagens: "
                            + e.getMessage()
            );
        }

        return mensagens;
    }


    // =========================================================
    // LISTAR IDS
    // =========================================================

    public static List<Integer> listarIds() {

        List<Integer> ids =
                new ArrayList<>();

        String sql =
                "SELECT id " +
                        "FROM conversas " +
                        "ORDER BY id ASC";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql);

                java.sql.ResultSet resultado =
                        statement.executeQuery()
        ) {

            while (resultado.next()) {

                ids.add(
                        resultado.getInt("id")
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao listar conversas: "
                            + e.getMessage()
            );
        }

        return ids;
    }


    // =========================================================
    // LISTAR TÍTULOS
    // =========================================================

    public static List<String> listarTitulos() {

        List<String> titulos =
                new ArrayList<>();

        String sql =
                "SELECT titulo " +
                        "FROM conversas " +
                        "ORDER BY id ASC";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql);

                java.sql.ResultSet resultado =
                        statement.executeQuery()
        ) {

            while (resultado.next()) {

                titulos.add(
                        resultado.getString(
                                "titulo"
                        )
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao listar títulos: "
                            + e.getMessage()
            );
        }

        return titulos;
    }


    // =========================================================
    // ATUALIZAR TÍTULO
    // =========================================================

    public static void atualizarTitulo(
            int conversaId,
            String titulo
    ) {

        String sql =
                "UPDATE conversas " +
                        "SET titulo = ? " +
                        "WHERE id = ?";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    titulo
            );

            statement.setInt(
                    2,
                    conversaId
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao atualizar título: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // EXCLUIR MENSAGENS
    // =========================================================

    public static void excluirMensagens(
            int conversaId
    ) {

        String sql =
                "DELETE FROM mensagens " +
                        "WHERE conversa_id = ?";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    conversaId
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao excluir mensagens: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // EXCLUIR CONVERSA
    // =========================================================

    public static void excluirConversa(
            int conversaId
    ) {

        /*
         * Primeiro apagamos as mensagens
         * relacionadas à conversa.
         */

        excluirMensagens(
                conversaId
        );

        /*
         * Depois apagamos a conversa.
         */

        String sql =
                "DELETE FROM conversas " +
                        "WHERE id = ?";

        try (
                Connection conexao =
                        BancoDeDados.conectar();

                PreparedStatement statement =
                        conexao.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    conversaId
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao excluir conversa: "
                            + e.getMessage()
            );
        }
    }
}

