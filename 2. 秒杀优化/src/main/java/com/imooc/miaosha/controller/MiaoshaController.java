package com.imooc.miaosha.controller;

import com.imooc.miaosha.domain.Goods;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.MiaoshaKey;
import com.imooc.miaosha.redis.OrderKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean{

	@Autowired
    MiaoshaUserService userService;
	
	@Autowired
    RedisService redisService;
	
	@Autowired
    GoodsService goodsService;

	@Autowired
	OrderService orderService;

	@Autowired
	MiaoshaService miaoshaService;

	@Autowired
	MQSender sender;

	private Map<Long, Boolean> localOverMap = new HashMap<>();

    /*
    系统初始化
     */
	@Override
	public void afterPropertiesSet() {
		List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
		if (goodsVoList == null) {
			return;
		}
		for (GoodsVo goodsVo : goodsVoList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goodsVo.getId(), goodsVo.getStockCount());
			localOverMap.put(goodsVo.getId(), false);
		}
	}

	@RequestMapping(value="/do_miaosha", method = RequestMethod.POST)
	@ResponseBody
	public Result<Integer> miaosha(MiaoshaUser user, @RequestParam("goodsId")long goodsId) {
		//如果没有登录，返回登录页面
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}

		//1.预减库存，直接减，原子操作，返回值是减后的值
		boolean over = localOverMap.get(goodsId);
		if (over) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
		if (stock < 0) {
			localOverMap.put(goodsId, true);
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		//2.判断这个用户是否已经秒杀过这个商品,因为秒杀完都会有一个订单的，直接去查有没有这个订单就可以
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
		if (order != null) {
			return Result.error(CodeMsg.REPEATE_MIAOSHA);
		}
		//3.异步，入队
		MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
		miaoshaMessage.setUser(user);
		miaoshaMessage.setGoodsId(goodsId);
		sender.sendMiaoshaMessage(miaoshaMessage);

		//直接返回排队中
		return Result.success(0);


		/*
		//1.判断库存
		GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
		int stock = goodsVo.getStockCount();
		if (stock <= 0) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		//2.判断这个用户是否已经秒杀过这个商品,因为秒杀完都会有一个订单的，直接去查有没有这个订单就可以
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
		if (order != null) {
			return Result.error(CodeMsg.REPEATE_MIAOSHA);
		}
		//3.进入秒杀事务:减库存、下订单
		OrderInfo orderInfo = miaoshaService.miaosha(user, goodsVo);

		return Result.success(orderInfo);
		*/
	}

	@RequestMapping(value="/result", method = RequestMethod.GET)
	@ResponseBody
	public Result<Long> miaoshaResult(MiaoshaUser user, @RequestParam("goodsId")long goodsId) {
		//判断用户有没有秒杀到商品
		long orderId = miaoshaService.getMiaoshaResult(user.getId(), goodsId);
		return Result.success(orderId);
	}

	@RequestMapping(value="/reset", method=RequestMethod.GET)
	@ResponseBody
	public Result<Boolean> reset(Model model) {
		List<GoodsVo> goodsList = goodsService.listGoodsVo();
		for(GoodsVo goods : goodsList) {
			goods.setStockCount(10);
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), 10);
			localOverMap.put(goods.getId(), false);
		}
		redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
		redisService.delete(MiaoshaKey.isGoodsOver);
		miaoshaService.reset(goodsList);
		return Result.success(true);
	}
}
