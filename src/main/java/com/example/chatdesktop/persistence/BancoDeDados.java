package com.example.chatdesktop.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class BancoDeDados {

    private static final String PASTA_APP =
            System.getProperty("user.home")
                    + File.separator
                    + ".chatdesktop";

    private static final String CAMINHO_BANCO =
            PASTA_APP
                    + File.separator
                    + "chat.db";

    private static final String URL =
            "jdbc:sqlite:" + CAMINHO_BANCO;


    public static Connection conectar() throws SQLException {

        criarPasta();

        return DriverManager.getConnection(URL);
    }


    private static void criarPasta() {

        File pasta =
                new File(PASTA_APP);

        if (!pasta.exists()) {

            boolean criada =
                    pasta.mkdirs();

            if (criada) {

                System.out.println(
                        "Pasta do banco criada em:"
                                + pasta.getAbsolutePath()
                );
            }
        }
    }


    public static void inicializar() {

        criarPasta();

        String sqlConversas = """
            CREATE TABLE IF NOT EXISTS conversas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT NOT NULL,
                data_criacao TEXT NOT NULL
            )
            """;


        String sqlMensagens = """
            CREATE TABLE IF NOT EXISTS mensagens (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversa_id INTEGER NOT NULL,
                tipo TEXT NOT NULL,
                conteudo TEXT NOT NULL,
                origem TEXT,
                fonte TEXT,
                FOREIGN KEY (conversa_id)
                    REFERENCES conversas(id)
                    ON DELETE CASCADE
            )
            """;


        try (
                Connection conexao = conectar();
                Statement statement =
                        conexao.createStatement()
        ) {

            statement.execute(
                    sqlConversas
            );

            statement.execute(
                    sqlMensagens
            );

            System.out.println(
                    "Banco de dados inicializado com sucesso."
            );

            System.out.println(
                    "Banco localizado em:"
                            + CAMINHO_BANCO
            );

        } catch (SQLException e) {

            System.out.println(
                    "Erro ao inicializar o banco de dados: "
                            + e.getMessage()
            );
        }
    }
}