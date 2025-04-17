import pika

class RabbitMQSubscriber:

    def __init__(self, username, destinations):
        self.username = username
        self.destinations = destinations
        self.connection = None
        self.channel = None

    def connect(self, host='localhost'):
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host))
        self.channel = self.connection.channel()

    def declare_exchange_and_queues(self, exchange_name='promotions-exchange'):

        self.channel.exchange_declare(exchange=exchange_name, exchange_type='topic', durable=True)

        for destination in self.destinations:
            queue_name = f"{self.username}.{destination}"  
            routing_key = f"promotions.{destination}"     

            self.channel.queue_declare(queue=queue_name, durable=True)
            self.channel.queue_bind(exchange=exchange_name, queue=queue_name, routing_key=routing_key)

            print(f"Queue '{queue_name}' bound to  '{routing_key}'")

    def listen_to_queues(self):

        print(f"Listening to queues for destinations: {', '.join(self.destinations)}")

        def callback(ch, method, properties, body):
            print(f"{body.decode()}")

        for destination in self.destinations:
            queue_name = f"{self.username}.{destination}"
            self.channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)

        self.channel.start_consuming()

    def close_connection(self):
        if self.connection:
            self.connection.close()


if __name__ == "__main__":
    # Get username and destinations from the command line
    username = input("Enter your username: ")
    destinations = input("Enter destinations you want to listen to (comma-separated, e.g., 'rj,sp,ba', or '*' for everything): ").split(',')

    subscriber = RabbitMQSubscriber(username=username, destinations=[dest.strip() for dest in destinations])

    try:
        subscriber.connect()
        subscriber.declare_exchange_and_queues()
        subscriber.listen_to_queues()
    except KeyboardInterrupt:
        print("\nSubscriber closed.")
    finally:
        subscriber.close_connection()
