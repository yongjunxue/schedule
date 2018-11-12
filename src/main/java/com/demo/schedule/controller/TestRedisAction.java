package com.demo.schedule.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import redis.clients.jedis.Jedis;

import com.demo.schedule.util.RedisUtil;

@Controller
public class TestRedisAction {
	
	@Autowired
	RedisUtil redisUtil;
	@RequestMapping("/test")
	public String testRedis(){
		Jedis j=redisUtil.getRedis();
		j.set("test", "调度器测试");
		System.out.println(j.get("test"));
		return "";
	}
}
