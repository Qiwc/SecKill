# SecKill
**一个简单的秒杀系统，从最基本功能版本到秒杀接口优化版本到安全方面优化版本**



## 1.基本秒杀功能版本

基于SpringBoot+MyBatis搭建开发环境，数据库使用Mysql InnoDB，集成Redis与FastJson，集成RabbitMQ

#### 数据库表结构设计如下：

- miaoshaUser表：用户信息
- goods表：所有商品信息
- miaoshaGoods表：秒杀商品信息
- miaoshaOrder表：秒杀订单表
- orderInfo表：订单表

#### 页面设计与流程如下：

- login.html 用户登陆页

  这里会利用Redis做一个分布式Session来缓存用户基本信息

- goods_list.html 商品列表页

  查数据库，展示出所有秒杀商品，点击详情进入可进入商品详情页

- goods_detail.html 商品详情页

  查数据库，展示商品详情，可点击秒杀功能，基础版本秒杀逻辑如下（效率低，而且有bug）

  1.查询数据库，判断库存

  2.查询秒杀订单表，判断这个用户是否已经秒杀过这个商品

  3.进入秒杀事务：减库存、下订单、下秒杀订单

  4.成功后进入订单详情页

- order_detail.html 订单详情页

