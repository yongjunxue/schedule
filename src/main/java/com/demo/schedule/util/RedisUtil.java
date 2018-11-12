package com.demo.schedule.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 这里可能获取不到
 * @author xueyongjun
 *
 */
@Component
public class RedisUtil {
	@Autowired
	private JedisPool jedisPool;
	
	public Jedis getRedis(){
		Jedis j=null;
		try{
			if(jedisPool != null){
				j=jedisPool.getResource();
			}else{
				WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
				jedisPool = (JedisPool) wac.getBean("jedisPool");
				j=jedisPool.getResource();
			}
		}catch(Exception e){
			System.out.println(jedisPool.getNumActive());
			throw e;
		}
		return j;
	}

}
