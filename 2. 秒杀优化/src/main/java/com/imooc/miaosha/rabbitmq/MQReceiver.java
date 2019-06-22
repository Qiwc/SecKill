package com.imooc.miaosha.rabbitmq;

import com.imooc.miaosha.domain.Goods;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {

	@Autowired
	RedisService redisService;

	@Autowired
	GoodsService goodsService;

	@Autowired
	OrderService orderService;

	@Autowired
	MiaoshaService miaoshaService;

	private static Logger log = LoggerFactory.getLogger(MQReceiver.class);

	@RabbitListener(queues = MQConfig.MIAOSHAQUEUE)
	public void receive(String message) {
		log.info("receive message: " + message);
		MiaoshaMessage mm = RedisService.stringToBean(message, MiaoshaMessage.class);
		MiaoshaUser user = mm.getUser();
		long goodsId = mm.getGoodsId();

		//1.查数据库看看还有没有库存
		GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
		int stock = goodsVo.getStockCount();
		if (stock <= 0) {
			return;
		}
		//2.判断这个用户是否已经秒杀过这个商品,因为秒杀完都会有一个订单的，直接去查有没有这个订单就可以
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
		if (order != null) {
			return;
		}
		//3.进入秒杀事务:减库存、下订单
		miaoshaService.miaosha(user, goodsVo);
	}

//	@RabbitListener(queues = MQConfig.QUEUE)
//	public void receive(String message) {
//		log.info("receive message: " + message);
//	}
//
//	@RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
//	public void receiveTopic1(String message) {
//		log.info("receive topic queue1 message: " + message);
//	}
//
//	@RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
//	public void receiveTopic2(String message) {
//		log.info("receive topic queue2 message: " + message);
//	}
//
//	@RabbitListener(queues = MQConfig.HEADER_QUEUE)
//	public void receiveHeader(byte[] message) {
//		log.info("receive header queue message: " + new String(message));
//	}

}
