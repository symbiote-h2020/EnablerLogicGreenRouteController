package eu.h2020.symbiote.smeur.elgrc.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;

import eu.h2020.symbiote.smeur.elgrc.messaging.consumers.PushAirQualityDataConsumer;
import eu.h2020.symbiote.smeur.elgrc.messaging.consumers.RouteRequestConsumer;


/**
 * Bean used to manage internal communication using RabbitMQ.
 * It is responsible for declaring exchanges and using routing keys from centralized config server.
 */
@Component
public class RabbitManager {

	private static Log log = LogFactory.getLog(RabbitManager.class);
	
    @Value("${rabbit.host}")
    private String rabbitHost;
    @Value("${rabbit.username}")
    private String rabbitUsername;
    @Value("${rabbit.password}")
    private String rabbitPassword;
    
    @Value("${rabbit.exchange.greenRouteController.name}")
    private String greenRouteControllerExchangeName;
    @Value("${rabbit.exchange.greenRouteController.type}")
    private String greenRouteControllerExchangeType;
    @Value("${rabbit.exchange.greenRouteController.durable}")
    private boolean greenRouteControllerExchangeDurable;
    @Value("${rabbit.exchange.greenRouteController.autodelete}")
    private boolean greenRouteControllerExchangeAutodelete;
    @Value("${rabbit.exchange.greenRouteController.internal}")
    private boolean greenRouteControllerExchangeInternal;

    @Value("${rabbit.queueName.greenRouteController.pushAirQualityData}")
    private String pushAirQualityDataQueueName;
    @Value("${rabbit.routingKey.greenRouteController.pushAirQualityData}")
    private String pushAirQualityDataRoutingKey;
    
    @Value("${rabbit.queueName.greenRouteController.routeRequest}")
    private String routeRequestQueueName;
    @Value("${rabbit.routingKey.greenRouteController.routeRequest}")
    private String routeRequestRoutingKey;
    
    private Connection connection;

    @Autowired 
    private AutowireCapableBeanFactory beanFactory;
    
    public RabbitManager() {
    }
    
    /**
     * Initiates connection with Rabbit server using parameters from ConfigProperties
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public Connection getConnection() throws IOException, TimeoutException {
        if (connection == null) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(this.rabbitHost);
            factory.setUsername(this.rabbitUsername);
            factory.setPassword(this.rabbitPassword);
            this.connection = factory.newConnection();
        }
        return this.connection;
    }
    
    /**
     * Method creates channel and declares Rabbit exchanges.
     * It triggers start of all consumers used in Registry communication.
     */
    public void init() {
        Channel channel = null;
        log.info("Rabbit is being initialized!");

        try {
            getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        if (connection != null) {
            try {
                channel = this.connection.createChannel();

                channel.exchangeDeclare(this.greenRouteControllerExchangeName,
                        this.greenRouteControllerExchangeType,
                        this.greenRouteControllerExchangeDurable,
                        this.greenRouteControllerExchangeAutodelete,
                        this.greenRouteControllerExchangeInternal,
                        null);

                startConsumers();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeChannel(channel);
            }
        }
    }
    
    /**
     * Cleanup method for rabbit - set on pre destroy
     */
    @PreDestroy
    public void cleanup() {
        //FIXME check if there is better exception handling in @predestroy method
        log.info("Rabbit cleaned!");
        try {
            Channel channel;
            if (this.connection != null && this.connection.isOpen()) {
                channel = connection.createChannel();

                channel.queueUnbind(this.pushAirQualityDataQueueName, this.greenRouteControllerExchangeName, this.pushAirQualityDataRoutingKey);
                channel.queueDelete(this.pushAirQualityDataQueueName);

                channel.queueUnbind(this.routeRequestQueueName, this.greenRouteControllerExchangeName, this.routeRequestRoutingKey);
                channel.queueDelete(this.routeRequestQueueName);

                closeChannel(channel);
                this.connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Method gathers all of the rabbit consumer starter methods
     */
    public void startConsumers() {
        try {
        		startConsumerOfPushAirQualityData();
        		startConsumerOfRouteRequest();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfPushAirQualityData() throws InterruptedException, IOException {

        String queueName = routeRequestQueueName;
        Channel channel;

        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.greenRouteControllerExchangeName, this.pushAirQualityDataRoutingKey);
            //channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for PushAirQualityData messages....");

            Consumer consumer = new PushAirQualityDataConsumer(channel);
            beanFactory.autowireBean(consumer);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Method creates queue and binds it globally available exchange and adequate Routing Key.
     * It also creates a consumer for messages incoming to this queue, regarding to Enabler Logic Wrong Data messages.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void startConsumerOfRouteRequest() throws InterruptedException, IOException {

        String queueName = routeRequestQueueName;
        Channel channel;

        try {
            channel = this.connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, this.greenRouteControllerExchangeName, this.routeRequestRoutingKey);
            // channel.basicQos(1); // to spread the load over multiple servers we set the prefetchCount setting

            log.info("Receiver waiting for UpdateTask messages....");

            Consumer consumer = new RouteRequestConsumer(channel);
            beanFactory.autowireBean(consumer);
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Closes given channel if it exists and is open.
     *
     * @param channel rabbit channel to close
     */
    private void closeChannel(Channel channel) {
        try {
            if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
