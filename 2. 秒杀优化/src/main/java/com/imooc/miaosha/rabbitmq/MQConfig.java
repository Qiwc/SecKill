package com.imooc.miaosha.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class MQConfig {

    public static final String MIAOSHAQUEUE = "miaoshaqueue";
    @Bean
    public Queue queue() {
        return new Queue(MIAOSHAQUEUE, true);
    }
//
//    public static final String QUEUE = "queue";
//
//    public static final String TOPIC_EXCHANGE = "topicExchange";
//    public static final String TOPIC_QUEUE1 = "topic.queue1";
//    public static final String TOPIC_QUEUE2 = "topic.queue2";
//    public static final String ROUTING_KEY1 = "topic.key1";
//    public static final String ROUTING_KEY2 = "topic.#";
//
//    public static final String FANOUT_EXCHANGE = "fanoutExchange";
//
//    public static final String HEADERS_EXCHANGE = "headersExchange";
//    public static final String HEADER_QUEUE = "header.queue";
//
//    /*
//		Direct交换机模式
//	*/
//    @Bean
//    public Queue queue() {
//        return new Queue(QUEUE, true);
//    }
//
//    /*
//		Topic交换机模式
//	*/
//    @Bean
//    public TopicExchange topicExchange() {
//        return new TopicExchange(TOPIC_EXCHANGE);
//    }
//    @Bean
//    public Queue topicQueue1() {
//        return new Queue(TOPIC_QUEUE1, true);
//    }
//    @Bean
//    public Queue topicQueue2() {
//        return new Queue(TOPIC_QUEUE2, true);
//    }
//	@Bean
//    public Binding topicBinding1() {
//        return BindingBuilder.bind(topicQueue1()).to(topicExchange()).with(ROUTING_KEY1);
//    }
//    @Bean
//    public Binding topicBinding2() {
//        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with(ROUTING_KEY2);
//    }
//
//    /*
//		广播模式
//	*/
//	@Bean
//	public FanoutExchange fanoutExchage(){
//		return new FanoutExchange(FANOUT_EXCHANGE);
//	}
//	@Bean
//	public Binding FanoutBinding1() {
//		return BindingBuilder.bind(topicQueue1()).to(fanoutExchage());
//	}
//	@Bean
//	public Binding FanoutBinding2() {
//		return BindingBuilder.bind(topicQueue2()).to(fanoutExchage());
//	}
//
//	/*
//		Header模式
//	*/
//	@Bean
//    public HeadersExchange headersExchange() {
//	    return new HeadersExchange(HEADERS_EXCHANGE);
//    }
//    @Bean
//    public Queue headerQueue() {
//        return new Queue(HEADER_QUEUE, true);
//    }
//    @Bean
//    public Binding headerBinding() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("header1", "value1");
//        map.put("header2", "value2");
//        return BindingBuilder.bind(headerQueue()).to(headersExchange()).whereAll(map).match();//都匹配上才能发
//    }
}
