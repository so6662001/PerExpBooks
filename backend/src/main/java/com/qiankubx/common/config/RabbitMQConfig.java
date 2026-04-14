package com.qiankubx.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_QIANKU = "qianku.exchange";
    public static final String QUEUE_EMAIL = "qianku.queue.email";
    public static final String QUEUE_PDF = "qianku.queue.pdf";
    public static final String ROUTING_EMAIL = "qianku.routing.email";
    public static final String ROUTING_PDF = "qianku.routing.pdf";

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange qiankuExchange() {
        return new DirectExchange(EXCHANGE_QIANKU, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(QUEUE_EMAIL, true);
    }

    @Bean
    public Queue pdfQueue() {
        return new Queue(QUEUE_PDF, true);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange qiankuExchange) {
        return BindingBuilder.bind(emailQueue).to(qiankuExchange).with(ROUTING_EMAIL);
    }

    @Bean
    public Binding pdfBinding(Queue pdfQueue, DirectExchange qiankuExchange) {
        return BindingBuilder.bind(pdfQueue).to(qiankuExchange).with(ROUTING_PDF);
    }
}
