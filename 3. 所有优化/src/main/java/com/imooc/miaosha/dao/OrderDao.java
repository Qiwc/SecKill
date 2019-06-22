package com.imooc.miaosha.dao;

import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.OrderInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderDao {
	
	@Select("select * from miaosha_order where user_id=#{userId} and goods_id=#{goodsId}")
	public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(@Param("userId") long userId, @Param("goodsId") long goodsId);

	//插入后的id直接插入到orderInfo实体类中了，并不是返回值返回
	@Insert("insert into order_info(user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)values(" +
			"#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
	@SelectKey(keyColumn="id", keyProperty="id", resultType=long.class, before=false, statement="select last_insert_id()")
	public long insert(OrderInfo orderInfo);

	@Insert("insert into miaosha_order (user_id, goods_id, order_id)values(#{userId}, #{goodsId}, #{orderId})")
	public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);

	@Select("select * from order_info where id = #{orderId}")
    OrderInfo getOrderById(@Param("orderId") long orderId);

	@Delete("delete from order_info")
	public void deleteOrders();

	@Delete("delete from miaosha_order")
	public void deleteMiaoshaOrders();

	@Select("select * from miaosha_order where goods_id=#{goodsId}")
	public List<MiaoshaOrder> listByGoodsId(@Param("goodsId") long goodsId);

}
