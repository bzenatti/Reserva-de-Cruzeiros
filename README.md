# 🚢 Sistema de Reserva de Cruzeiros

Este projeto visa implementar uma aplicação de reserva de cruzeiros baseada em arquitetura de microsserviços, utilizando o middleware de mensageria **RabbitMQ** com o protocolo **AMQP** para comunicação assíncrona entre os componentes.

## 🧱 Arquitetura

A arquitetura é composta por:

- **4 Microsserviços (MS)** independentes
- **1 Middleware RabbitMQ** para gerenciamento de eventos
- **Subscribers** interessados em promoções por destino
- **Mensagens assinadas com chaves assimétricas** para garantir autenticidade


## 🧩 Microsserviços

### 🛳️ MS Reserva

- Consulta itinerários com base no destino, data e porto de embarque.
- Efetua reservas e publica o evento `reserva_criada`.
- Escuta os eventos:
  - `pagamento_aprovado`
  - `pagamento_recusado`
  - `bilhete_gerado`
- Verifica assinaturas digitais com a chave pública do MS Pagamento.
- Cancela reservas se o pagamento for recusado.

### 💳 MS Pagamento

- Escuta o evento `reserva_criada` para processar o pagamento.
- Publica:
  - `pagamento_aprovado` (com assinatura digital via chave privada)
  - `pagamento_recusado` (também assinado digitalmente)

### 🎫 MS Bilhete

- Escuta o evento `pagamento_aprovado`.
- Gera o bilhete e publica o evento `bilhete_gerado`.
- Verifica a assinatura digital usando a chave pública do MS Pagamento.

### 📢 MS Marketing

- Publica promoções específicas nas filas:
  - `promocoes_destinoX`
  - `promocoes_destinoY`
- Apenas os assinantes interessados em destinos específicos recebem essas mensagens.

### 👥 Assinantes

- Deve ouvir apenas as promoções a qual assinou, consumindo de forma que outros assinantes consigam ouvir da mesma fila.

## 📨 Comunicação via RabbitMQ

Foi utilizado o **RabbitMQ** para:
- Orquestrar eventos
- Garantir comunicação assíncrona e desacoplada
- Escalar microsserviços de forma independente
- Melhorar performance e modularidade

## 🔐 Segurança com Assinaturas Digitais

As mensagens entre MS Pagamento e os outros serviços são **assinadas digitalmente**. Cada MS possui:
- **Chave pública do MS Pagamento** (para verificação)
- **Chave privada (apenas o MS Pagamento)** usada para assinar mensagens

## 🖥️ Interface

O sistema inclui uma interface gráfica/interativa simples para:
- Realizar consultas de cruzeiros
- Efetuar reservas
- Visualizar status de pagamento e bilhete


## 📌 Tecnologias utilizadas

- Linguagem: (ex: Python, Node.js, etc.)
- RabbitMQ
- Protocolo AMQP
- Criptografia com RSA (assinaturas digitais)
- Docker (opcional, para deploy dos serviços)
- Biblioteca de Interface: (ex: Flask, React, etc.)

---

> Projeto desenvolvido para a disciplina de Sistemas Distribuídos - **DAINF/UTFPR**  
> Professora: **Ana Cristina Barreiras Kochem Vendramin**
