package com.imooc.miaosha.controller;

import com.imooc.miaosha.access.AccessLimit;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.*;
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

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
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

	private static HashMap<Long, Integer> stockMap =  new HashMap<Long, Integer>();

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
			stockMap.put(goodsVo.getId(), goodsVo.getStockCount());
			localOverMap.put(goodsVo.getId(), false);
		}
	}

	@RequestMapping(value="/{path}/do_miaosha", method = RequestMethod.POST)
	@ResponseBody
	public Result<Integer> miaosha(MiaoshaUser user,
								   @RequestParam("goodsId")long goodsId,
								   @PathVariable("path") String path) {
		//如果没有登录，返回登录页面
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}

		//验证path
		boolean check = miaoshaService.checkPath(user, goodsId, path);
		if (!check) {
			return Result.error(CodeMsg.REQUEST_ILLEGAL);
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

	@AccessLimit(seconds = 5, maxCount = 5, needLogin = true)
	@RequestMapping(value="/path", method = RequestMethod.GET)
	@ResponseBody
	public Result<String> miaoshaPath(MiaoshaUser user, @RequestParam("goodsId")long goodsId,
									  @RequestParam(value = "verifyCode", defaultValue = "-1") int verifyCode,
									  HttpServletRequest request) {
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}

		//采用拦截器的方式
//		//查询这个用户访问的次数, 5s最多访问5次
//		String uri = request.getRequestURI();
//		String key = uri + "_" + user.getId();
//		Integer count = redisService.get(AccessKey.access, key, Integer.class);
//		if (count == null) {
//			redisService.set(AccessKey.access,  key, 1);
//		} else if (count < 5) {
//			redisService.incr(AccessKey.access,  key);
//		} else {
//			return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
//		}

		boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
		if (!check) {
			return Result.error(CodeMsg.REQUEST_ILLEGAL);
		}
		String path =  miaoshaService.creatMiaoshaPath(user, goodsId);
		return Result.success(path);
	}

	@RequestMapping(value="/verifyCode", method=RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaVerifyCod(HttpServletResponse response, MiaoshaUser user,
											  @RequestParam("goodsId")long goodsId) {
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		try {
			BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
			OutputStream out = response.getOutputStream();
			ImageIO.write(image, "JPEG", out);
			out.flush();
			out.close();
			return null;
		}catch(Exception e) {
			e.printStackTrace();
			return Result.error(CodeMsg.MIAOSHA_FAIL);
		}
	}

	/**获取初始的商品秒杀数量*/
	public static int getGoodsStockOriginal(long goodsId) {
		Integer stock = stockMap.get(goodsId);
		if(stock == null) {
			return 0;
		}
		return stock;
	}
}
