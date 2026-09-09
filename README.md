# ✦ Chat Desktop com JavaFX e Groq

Aplicativo de chat desktop desenvolvido em **JavaFX**, integrado à **API da Groq**, permitindo realizar conversas com um modelo de inteligência artificial através de uma interface gráfica moderna.

O projeto foi desenvolvido como uma aplicação prática para estudar e aplicar conceitos de **Java**, **JavaFX**, **FXML**, **Maven**, **integração com APIs**, **SQLite** e **persistência de dados**.

---

## 🚀 Funcionalidades

* 💬 Envio de mensagens para a inteligência artificial
* 🤖 Respostas geradas por IA através da API da Groq
* 🖥️ Interface desktop desenvolvida com JavaFX
* 🔐 Sistema de login de usuários
* 📝 Cadastro de novos usuários
* 🗄️ Armazenamento de usuários em banco de dados SQLite
* 💾 Persistência de conversas e mensagens
* 📚 Suporte à utilização de conhecimento local através de RAG
* 🔎 Exibição da origem e fonte da resposta da IA
* ⌨️ Envio de mensagens pelo botão ou tecla Enter
* ⚡ Execução das requisições sem bloquear a interface
* 🎨 Interface personalizada com tema roxo/lilás
* 🌙 Preferência de tema da aplicação
* 🔄 Gerenciamento do histórico de conversas
* 🧠 Integração com modelo de inteligência artificial através da Groq

---

## 🛠️ Tecnologias utilizadas

* **Java 20**
* **JavaFX 20.0.1**
* **FXML**
* **Maven**
* **SQLite**
* **JDBC**
* **API Groq**
* **Java HTTP Client**
* **Gson**
* **IntelliJ IDEA**

### Modelo de IA

O projeto utiliza o modelo:

`openai/gpt-oss-20b`

através da API da Groq.

---

## 🗄️ Banco de dados

O projeto utiliza **SQLite** para realizar a persistência dos dados.

O banco é criado automaticamente na máquina do usuário dentro da pasta:

```text
.chatdesktop/
└── chat.db
```

Entre os dados armazenados estão:

* Usuários
* Conversas
* Mensagens
* Tipo da mensagem
* Origem da resposta
* Fonte utilizada pela IA

### Estrutura principal

```text
usuarios
├── id
├── usuario
└── senha

conversas
├── id
├── titulo
└── data_criacao

mensagens
├── id
├── conversa_id
├── tipo
├── conteudo
├── origem
└── fonte
```

---

## 📚 RAG

O projeto possui suporte à abordagem **RAG (Retrieval-Augmented Generation)**.

O objetivo é permitir que a aplicação utilize informações presentes em documentos locais como fonte de conhecimento para auxiliar na geração das respostas.

Quando uma resposta utiliza esse recurso, a interface pode apresentar informações como:

```text
Origem: RAG
Fonte: conhecimento.txt
```

Isso permite identificar de onde as informações utilizadas na resposta foram obtidas.

---

## 📁 Estrutura do projeto

```text
chat-desktop/
│
├── src/
│   └── main/
│       │
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── chatdesktop/
│       │               │
│       │               ├── Main.java
│       │               ├── LoginController.java
│       │               ├── CadastroController.java
│       │               ├── ChatController.java
│       │               ├── GroqService.java
│       │               ├── LeitorDocumento.java
│       │               │
│       │               └── persistence/
│       │                   └── BancoDeDados.java
│       │
│       └── resources/
│           └── com/
│               └── example/
│                   └── chatdesktop/
│                       │
│                       └── view/
│                           ├── login-view.fxml
│                           ├── cadastro-view.fxml
│                           ├── chat-view.fxml
│                           └── loguin.css
│
├── pom.xml
├── module-info.java
└── README.md
```

---

## 🔐 Sistema de autenticação

A aplicação possui uma tela de login que permite ao usuário acessar o sistema através de um usuário e senha cadastrados.

Também existe uma tela de cadastro para criação de novas contas.

O fluxo principal é:

```text
Cadastro
   ↓
Usuário armazenado no SQLite
   ↓
Tela de Login
   ↓
Validação do usuário e senha
   ↓
Chat IA
```

---

## 💬 Chat com inteligência artificial

Após realizar o login, o usuário é direcionado para a tela principal do Chat IA.

A aplicação envia a pergunta para a API da Groq e apresenta a resposta recebida na interface.

O sistema também possui recursos relacionados ao histórico e gerenciamento das conversas.

---

## 🎨 Interface

A interface foi personalizada utilizando **JavaFX CSS**, buscando um visual moderno inspirado em:

* Glassmorphism
* Tons de roxo e lilás
* Gradientes
* Elementos translúcidos
* Bordas arredondadas
* Sombras suaves
* Estética futurista

A tela de login possui uma identidade visual própria e segue a mesma linguagem visual da aplicação.

---

## ⚙️ Como executar o projeto

### 1. Requisitos

Antes de executar o projeto, é necessário possuir:

* Java JDK 20
* IntelliJ IDEA
* Maven
* Uma chave de API da Groq

### 2. Clonar o projeto

```bash
git clone https://github.com/yara967/chat-desktop.git
```

### 3. Abrir no IntelliJ IDEA

Abra a pasta do projeto no IntelliJ IDEA e aguarde o Maven carregar as dependências.

### 4. Configurar a API

Configure a chave da API da Groq de acordo com a configuração utilizada no projeto.

### 5. Executar

Execute a classe:

```text
Main.java
```

A aplicação será iniciada através da interface JavaFX.

---

## 🔄 Fluxo da aplicação

```text
                 ┌─────────────────┐
                 │    Aplicação    │
                 │   JavaFX Chat   │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │      Login      │
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │     SQLite      │
                 │    Usuários     │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │     Chat IA     │
                 └────────┬────────┘
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
      ┌──────────────┐         ┌──────────────┐
      │   Groq API   │         │     RAG      │
      │      IA      │         │ Conhecimento │
      └──────┬───────┘         └──────┬───────┘
             │                        │
             └────────────┬───────────┘
                          ▼
                 ┌─────────────────┐
                 │     Resposta    │
                 │       IA        │
                 └─────────────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │ SQLite /        │
                 │ Histórico       │
                 └─────────────────┘
```

---

## 🎯 Objetivo do projeto

O principal objetivo do projeto é desenvolver uma aplicação desktop funcional utilizando Java e JavaFX, colocando em prática conceitos de desenvolvimento de software, interface gráfica, banco de dados, integração com APIs e inteligência artificial.

Além da funcionalidade do chat, o projeto busca proporcionar uma experiência de usuário moderna e organizada.

---

## 👩‍💻 Desenvolvimento

Projeto desenvolvido para fins educacionais durante o curso de **Análise e Desenvolvimento de Sistemas (ADS)**.

### Integrantes

* **Yara Vitória da Silva Reis**


---

## 📌 Status do projeto

🚧 **Em desenvolvimento**

Novas funcionalidades, melhorias na interface e ajustes no sistema ainda podem ser adicionados ao projeto.

---

## 📄 Licença

Este projeto foi desenvolvido para fins educacionais.
