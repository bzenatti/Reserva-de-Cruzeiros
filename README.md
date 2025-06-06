# 🚢 Sistema de Reserva de Cruzeiros

Este projeto implementa uma aplicação de reserva de cruzeiros baseada em uma arquitetura de microsserviços. A comunicação entre os componentes é realizada de forma assíncrona utilizando o middleware de mensageria **RabbitMQ** com o protocolo **AMQP** e também por meio de comunicação síncrona via REST.

## 🧱 Arquitetura

A arquitetura é composta por:

-   **5 Microsserviços (MS)** independentes: Reserva, Itinerários, Pagamento, Bilhete e Marketing.
-   **1 Middleware RabbitMQ** para o gerenciamento de eventos e comunicação assíncrona.
-   **Comunicação em tempo real** com o cliente através de Server-Sent Events (SSE).
-   **Mensagens assinadas com chaves assimétricas (RSA)** para garantir a autenticidade e a integridade das transações de pagamento.

## 🧩 Microsserviços

### 🛳️ MS Reserva (porta 8080)

É o *backend-for-frontend* (BFF) da aplicação, responsável por orquestrar as operações e servir a interface do usuário.

-   **Interface Web**: Fornece uma página HTML interativa para os usuários.
-   **Consulta de Itinerários**: Busca os cruzeiros disponíveis fazendo uma chamada REST ao MS Itinerários.
-   **Criação de Reservas**: Ao receber um pedido de reserva, publica o evento `booking-created` no RabbitMQ e solicita um link de pagamento ao MS Pagamento via REST.
-   **Cancelamento**: Publica um evento `booking-deleted` para que o MS Itinerários possa liberar as vagas.
-   **Notificações em Tempo Real (SSE)**: Gerencia a comunicação com o cliente, enviando atualizações sobre:
    -   Status do pagamento (`pagamento_aprovado`, `pagamento_recusado`).
    -   Geração do bilhete (`bilhete_gerado`).
    -   Promoções recebidas do MS Marketing.
-   **Segurança**: Verifica a assinatura digital das mensagens de pagamento recebidas do RabbitMQ usando a chave pública do MS Pagamento.

### Schedules MS Itinerários (porta 8081)

Responsável por gerenciar as informações e a disponibilidade dos cruzeiros.

-   **API de Consulta**: Expõe um endpoint REST (`/api/itineraries`) para que outros serviços consultem os itinerários disponíveis.
-   **Gerenciamento de Vagas**: Escuta os eventos:
    -   `booking-created`: Reduz o número de cabines e passageiros disponíveis para um itinerário.
    -   `booking-deleted`: Libera as vagas de uma reserva cancelada.
-   **Dados**: Utiliza uma base de dados mockada para fornecer as informações dos cruzeiros.

### 💳 MS Pagamento (porta 8083)

Processa os pagamentos das reservas.

-   **Geração do Link de Pagamento**: Expõe um endpoint REST (`/api/payment/request-payment`) que, ao ser chamado pelo MS Reserva, gera uma página de pagamento simulada.
-   **Processamento Simulado**: A página de pagamento permite simular a aprovação ou recusa do pagamento.
-   **Publicação de Eventos (com assinatura)**: Publica os eventos de forma segura:
    -   `approved-payment`
    -   `denied-payment`
-   **Segurança**: Assina as mensagens de status de pagamento com sua chave privada (RSA) antes de publicá-las, garantindo que a origem da mensagem é autêntica.

### 🎫 MS Bilhete (porta 8082)

Responsável pela emissão dos bilhetes após a confirmação do pagamento.

-   **Escuta Evento de Pagamento**: Consome o evento `approved-payment` da fila do RabbitMQ.
-   **Geração do Bilhete**: Após receber a confirmação de pagamento, publica o evento `ticket-generated`.
-   **Segurança**: Verifica a assinatura digital da mensagem de pagamento aprovado usando a chave pública do MS Pagamento para garantir que a mensagem não foi adulterada.

### 📢 MS Marketing (porta 8084)

Envia promoções de forma contínua para os clientes interessados.

-   **Publicação de Promoções**: Publica periodicamente mensagens de promoções em um tópico (`promotions-exchange`) no RabbitMQ. As promoções são genéricas e enviadas para todos os serviços que as escutam.

## 🌊 Fluxo Principal: Criação de uma Reserva

1.  **Consulta**: O usuário acessa a interface web (servida pelo MS Reserva) e busca por itinerários. O MS Reserva faz uma chamada **REST** ao MS Itinerários para obter a lista.
2.  **Reserva**: O usuário seleciona um cruzeiro e efetua a reserva.
3.  **Orquestração da Reserva**:
    -   O MS Reserva publica uma mensagem `booking-created` no **RabbitMQ**.
    -   Ao mesmo tempo, faz uma chamada **REST** ao MS Pagamento para obter um link de pagamento.
4.  **Atualização de Vagas**: O MS Itinerários consome a mensagem `booking-created` e atualiza a disponibilidade de vagas do cruzeiro.
5.  **Pagamento**: O usuário recebe o link e acessa a página de pagamento (servida pelo MS Pagamento), onde pode aprovar ou negar a transação.
6.  **Confirmação do Pagamento**:
    -   O MS Pagamento publica o evento `approved-payment` (assinado com sua chave privada) no **RabbitMQ**.
7.  **Emissão do Bilhete**:
    -   O MS Bilhete consome a mensagem `approved-payment`, verifica a assinatura digital e, se válida, publica o evento `ticket-generated`.
8.  **Notificação Final**:
    -   O MS Reserva consome os eventos `approved-payment` e `ticket-generated`, verifica a assinatura do pagamento e notifica o usuário em tempo real via **SSE** sobre o status da reserva e a emissão do bilhete.

## 🔐 Segurança com Assinaturas Digitais

A comunicação crítica (status de pagamento) é protegida com assinaturas digitais RSA para garantir autenticidade e integridade.

-   O **MS Pagamento** possui uma **chave privada** que é usada para assinar as mensagens `approved-payment` e `denied-payment`.
-   Os microsserviços **Reserva** e **Bilhete** possuem a **chave pública** do MS Pagamento para verificar a assinatura das mensagens recebidas. Se a verificação falhar, a mensagem é descartada.

## 🖥️ Interface

O sistema inclui uma interface gráfica simples (`index.html`) que permite:

-   Conectar-se com um nome de cliente para receber notificações.
-   Realizar consultas de cruzeiros por destino, data e porto.
-   Efetuar reservas.
-   Cancelar uma reserva existente.
-   Registrar-se para receber notificações de promoções.
-   Visualizar em tempo real o status de pagamento, a geração de bilhetes e as promoções.

## 📌 Tecnologias Utilizadas

-   **Linguagem**: Java com Spring Boot
-   **Comunicação Assíncrona**: RabbitMQ com protocolo AMQP
-   **Comunicação Síncrona**: RESTful APIs
-   **Comunicação com o Cliente**: Server-Sent Events (SSE)
-   **Segurança**: Criptografia RSA para assinaturas digitais

---

> Projeto desenvolvido para a disciplina de Sistemas Distribuídos - **DAINF/UTFPR**
> Professora: **Ana Cristina Barreiras Kochem Vendramin**
