package com.example.chatdesktop.service;

public class TesteDocumento {

    public static void main(String[] args) {

        LeitorDocumento leitor = new LeitorDocumento();

        String conteudo = leitor.lerDocumento();

        System.out.println("===== CONTEÚDO DO DOCUMENTO =====");
        System.out.println(conteudo);
        System.out.println("=================================");
    }
}
