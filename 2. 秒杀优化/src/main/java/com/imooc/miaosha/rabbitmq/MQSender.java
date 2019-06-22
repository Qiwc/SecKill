package com.imooc.miaosha.rabbitmq;

import com.imooc.miaosha.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQSender {

	@Autowired
	AmqpTemplate amqpTemplate;

	private static Logger log = LoggerFactory.getLogger(MQSender.class);

//	public void send(Object message) {
//		String msg = RedisService.beanToString(message);
//		log.info("send message: " + msg);
//		amqpTemplate.convertAndSend(MQConfig.QUEUE, msg);
//	}
//
//	public void sendTopic(Object message) {
//		String msg = RedisService.beanToString(message);
//		log.info("send message: " + msg);
//		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, MQConfig.ROUTING_KEY1, msg + "1");//这个是发给了两个queue，因为通配符
//		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, MQConfig.ROUTING_KEY2, msg + "2");
//	}
//
//	public void sendFanout(Object message) {
//		String msg = RedisService.beanToString(message);
//		log.info("send fanout message:"+msg);
//		amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE, "", msg);
//	}
//
//	public void sendHeader(Object message) {
//		String msg = RedisService.beanToString(message);
//		log.info("send fanout message:"+msg);
//		MessageProperties properties = new MessageProperties();
//		properties.setHeader("header1", "value1");
//		properties.setHeader("header2", "value2");
//		Message obj = new Message(msg.getBytes(), properties);
//		amqpTemplate.convertAndSend(MQConfig.HEADERS_EXCHANGE, "", obj);
//	}

    public void sendMiaoshaMessage(MiaoshaMessage miaoshaMessage) {
    	String msg = RedisService.beanToString(miaoshaMessage);
    	log.info("send message: " + msg);
    	amqpTemplate.convertAndSend(MQConfig.MIAOSHAQUEUE, msg);
    }
}
