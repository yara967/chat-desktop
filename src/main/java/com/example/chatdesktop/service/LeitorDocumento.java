package com.example.chatdesktop.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LeitorDocumento {

    public String lerDocumento() {

        String[] caminhos = {
                "/com/example/chatdesktop/documentos/conhecimento.txt",
                "/com.example.chatdesktop/documentos/conhecimento.txt",
                "/documentos/conhecimento.txt"
        };

        for (String caminho : caminhos) {

            try {

                InputStream arquivo =
                        getClass().getResourceAsStream(caminho);

                if (arquivo != null) {

                    BufferedReader leitor =
                            new BufferedReader(
                                    new InputStreamReader(
                                            arquivo,
                                            StandardCharsets.UTF_8
                                    )
                            );

                    StringBuilder conteudo =
                            new StringBuilder();

                    String linha;

                    while ((linha = leitor.readLine()) != null) {

                        conteudo
                                .append(linha)
                                .append("\n");
                    }

                    leitor.close();

                    return conteudo.toString();
                }

            } catch (Exception e) {

                return "Erro ao ler o documento: "
                        + e.getMessage();
            }
        }

        return "Arquivo conhecimento.txt não encontrado.";
    }

    // =========================================================
    // NOME DA FONTE
    // =========================================================

    public String getNomeFonte() {

        return "conhecimento.txt";
    }

    // =========================================================
    // VERIFICAR SE O DOCUMENTO EXISTE
    // =========================================================

    public boolean documentoExiste() {

        String[] caminhos = {
                "/com/example/chatdesktop/documentos/conhecimento.txt",
                "/com.example.chatdesktop/documentos/conhecimento.txt",
                "/documentos/conhecimento.txt"
        };

        for (String caminho : caminhos) {

            InputStream arquivo =
                    getClass().getResourceAsStream(caminho);

            if (arquivo != null) {

                try {
                    arquivo.close();
                } catch (Exception ignored) {
                }

                return true;
            }
        }

        return false;
    }
}