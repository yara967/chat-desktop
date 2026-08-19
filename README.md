# 💬 Chat Desktop com JavaFX e Groq

Aplicação de chat desktop desenvolvida em JavaFX, integrada à API da Groq para permitir conversas com um modelo de inteligência artificial.

O projeto foi desenvolvido como uma primeira versão simples, com o objetivo de aprender na prática como criar uma interface JavaFX e realizar uma comunicação com uma API de IA.

## 🚀 Funcionalidades

- 💬 Envio de mensagens para a IA
- 🤖 Respostas geradas por inteligência artificial
- 🖥️ Interface desktop com JavaFX
- ⌨️ Envio da mensagem pelo botão ou pela tecla Enter
- 🔄 Comunicação com a API da Groq
- ⚡ Execução das requisições sem travar a interface

## 🛠️ Tecnologias utilizadas

- Java 21
- JavaFX 21.0.6
- Maven
- Groq API
- HTTP Client do Java
- Modelo `openai/gpt-oss-20b`

## 📁 Estrutura do projeto

```text
chat-desktop/
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java
│       │   └── com.example.chatdesktop/
│       │       └── HelloApplication.java
│       │
│       └── resources/
│
├── pom.xml
└── README.md