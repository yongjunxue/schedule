package com.demo.schedule.util.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.demo.schedule.util.LockUtil;
import com.demo.schedule.util.RedisUtil;



/**
 * 调度器
 * 
 * 优势:1.同一个任务，支持多个生产者，也支持多个消费者。
 * 		2.生产者和消费者可以部署在同一台机器，也可以部署在不同的机器。
 * 		3.无数据丢失
 * 缺陷:1.有极低概率重复消费（此时，需要在消费时在数据库中确认一下是否消费过，即可避免）
 * 		2.占用空间较大，一个任务维护了两套数据
 * 		3.定义任务的时候，需要留出一部分时间用来处理旧数据
 * @author xueyongjun
 *
 */
public abstract class ScheduleSupport implements InitializingBean{
	
	public static Logger logger = Logger.getLogger(ScheduleSupport.class);
	
	private RedisUtil redisUtil=new RedisUtil();
	
	private boolean debug=true;
	
	private LockUtil lock;
	private String redisListKey;//任务list；
	private String redisSetKey;//任务set
	private String oldDataMapKey;//旧数据
	private String maxHandleTimeKey;//最长的处理时间。默认值为5秒
	private long defaultMaxHandleTime=5;
	
	private ScheduleTask task;//具体的任务信息
	private ScheduledExecutorService provider;
	private ScheduledExecutorService consumer;
	
	@Deprecated
	private BlockingQueue<Map<String,Object>> paperIdQueue;
	@Deprecated
	private boolean pauseProvider=false;//是否暂停生产者

	private static List<ScheduleTask> taskList=new ArrayList<>();//任务列表
/*	//暂时保留
//	private static List<ScheduledExecutorService> providerList=new CopyOnWriteArrayList<ScheduledExecutorService>();
//	private static List<ScheduledExecutorService> consumerList=new CopyOnWriteArrayList<ScheduledExecutorService>();
//	private static List<BlockingQueue<Map<String,Object>>> queueList=new CopyOnWriteArrayList<BlockingQueue<Map<String,Object>>>();//任务队列
 */	
	@Deprecated
	private static ConcurrentHashMap<Map<String,Object>,String> workIngMap=new ConcurrentHashMap<>();//正在执行的任务Map
	
	private void start(){
		taskList.add(task);
		provider=new ScheduledThreadPoolExecutor(task.getProviderThreadCount());
		consumer=new ScheduledThreadPoolExecutor(task.getComsumerThreadCount());
		paperIdQueue =new LinkedBlockingDeque<>(task.getQueueLimit());
		
/*		//暂时保留
		providerList.add(provider);
		consumerList.add(consumer);
		queueList.add(paperIdQueue);
		*/
//		startProvider();
//		startConsumer();
		
		//
		if(task.isStartProvider()){
			for(int i=1;i<=task.getProviderThreadCount();i++){
				startProvider_new();
			}
			logger.info("【"+task.getTaskName()+"】"+task.getProviderThreadCount()+"个生产者线程启动成功");
			System.out.println("【"+task.getTaskName()+"】"+task.getProviderThreadCount()+"个生产者线程启动成功");
		}
		if(task.isStartConsumer()){
			for(int i=1;i<=task.getComsumerThreadCount();i++){
				startConsumer_new();
			}
			logger.info("【"+task.getTaskName()+"】"+task.getComsumerThreadCount()+"个消费者线程启动成功");
			System.out.println("【"+task.getTaskName()+"】"+task.getComsumerThreadCount()+"个消费者线程启动成功");
		}
	}
	
	/**
	 * 启动消费者(单机环境)
	 */
	@Deprecated
	private void startConsumer() {
		consumer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
					if(ifWorkTime()){//是否在执行任务时间段
							Map<String, Object> msg = new HashMap<>();
							try {
								msg = paperIdQueue.take();
								consume(msg);
								logger.info("【"+task.getTaskName()+"】消费者线程"+Thread.currentThread().getName()+"任务执行成功。"+msg.toString());
							} catch(Exception e1) {
								e1.printStackTrace();
								logger.info("【"+task.getTaskName()+"】消费者线程"+Thread.currentThread().getName()+"任务执行失败。"+msg.toString());
							}
							workIngMap.remove(msg);
							synchronized (paperIdQueue) {
								if(paperIdQueue.size()==0){
									pauseProvider=false;
									if(debug){
										logger.info("【"+task.getTaskName()+"】生产者线程继续工作");
									}
								}
							}
					}
			}
		}, 5, task.getTaskPeriod(), task.getTimeUnit());
	}
	
	/**
	 * 启动生产者(单机环境)
	 */
	@Deprecated
	private void startProvider() {
		provider.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
					if(ifWorkTime()){//是否在执行任务时间段
							if(!pauseProvider){
								@SuppressWarnings("rawtypes")
								List<Map> msgList=produce();
								if(msgList.size()>0){
									for(Map<String,Object> msg : msgList){
										String v=workIngMap.putIfAbsent(msg, "true");
										if(v==null){
											try {
												paperIdQueue.put(msg);
												logger.info("【"+task.getTaskName()+"】生产者线程"+Thread.currentThread().getName()+"添加任务成功。"+msg.toString());
											} catch (InterruptedException e) {
												e.printStackTrace();
												logger.info("【"+task.getTaskName()+"】生产者线程"+Thread.currentThread().getName()+"添加任务失败。"+msg.toString());
											}
										}
									}
								}
								synchronized (paperIdQueue) {
									if(paperIdQueue.size()>0){
										pauseProvider=true;
										logger.info("【"+task.getTaskName()+"】生产者线程"+Thread.currentThread().getName()+"暂停工作。");
									}
								}
							}
					}
			}
		}, 2, task.getTaskPeriod(),task.getTimeUnit() );
	}
	
	
	/**
	 * 启动生产者(单机集群都支持)
	 */
	private void startProvider_new() {
		provider.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if(ifWorkTime()){//是否在执行任务时间段
					long count=getListSize();
					if(count==0){
						@SuppressWarnings("rawtypes")
						List<Map> msgList=produce_new();
						addTask(msgList);
					}
				}else{
					if(lock.tryLock()){
						handleOldData();
						lock.unLock();
					}
				}
			}
		}, getRandom(), task.getTaskPeriod(),task.getTimeUnit() );
	}

	/**
	 * 获取list中任务个数
	 * @return
	 */
	private long getListSize() {
		Jedis jedis=redisUtil.getRedis();
		long count=jedis.llen(redisListKey);
		jedis.close();
		return count;
	}

	/**
	 * 添加任务
	 * 直接将任务添加到任务队列中
	 * @param msgList
	 */
	@SuppressWarnings("rawtypes")
	private void addTask(List<Map> msgList) {
		if(msgList != null && msgList.size()>0){
			for(Map<String,Object> msg : msgList){
				addTask(msg);
			}
		}
	}
	
	/**
	 * TODO 在list和set里面添加任务
	 * @param taskMap
	 */
	@SuppressWarnings("rawtypes")
	public void addTask(Map taskMap){
		if(taskMap!=null){
			try{
				Jedis jedis=redisUtil.getRedis();
				JSONObject json = JSONObject.fromObject(taskMap);
				if(jedis.sadd(redisSetKey, json.toString())>0){
					jedis.lpush(redisListKey, json.toString());
					logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"添加任务成功。"+taskMap.toString());
				}
				jedis.close();
			}catch(Exception e){
				logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"添加任务失败。"+taskMap.toString());
			}
		}
	}
	/**
	 * 启动消费者((单机集群都支持))
	 */
	private void startConsumer_new() {
		Random r=new Random();
		consumer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if(ifWorkTime()){//是否在执行任务时间段
					long start=System.currentTimeMillis();
					JSONObject json=getTask();
					if(json != null){
						if(consume_new(json)){
							if(removeTask(json)){
								logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"执行任务成功。"+json.toString());
								long end=System.currentTimeMillis();
								
								if(lock.tryLock()){
									saveMaxHandleTime(end-start);
									lock.unLock();
								}
							}else{
								//-----重复消费1------这里有极低的可能造成重复消费的问题。
								//不过不影响，只要消费方在消费消息的时候确认一下就可以
								logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"任务移除失败，请在消费时确认是否重复消费。"+json.toString());
							}
						}
					}
				}
			}
		},getRandom() , task.getTaskPeriod(), task.getTimeUnit());
	}
	
	private long getRandom() {
		Random r=new Random();
		return r.nextInt(10)+1;
	}
	
	public static void main(String[] args) {
		for(int i=1;i<=10;i++){
			System.out.println(new Random().nextInt(10)+1);
		}
	}
	
	private void saveMaxHandleTime(long time) {
		Jedis jedis=redisUtil.getRedis();
		String str=jedis.get(maxHandleTimeKey);
		if(str != null && !str.equals("")){
			long oldTime=Long.parseLong(str);
			if(oldTime<time){
				jedis.set(maxHandleTimeKey, time+"");
				lock.setOutTime((int) (time*1.5/1000));
			}
		}else{
			if(time > (defaultMaxHandleTime*1000)){
				jedis.set(maxHandleTimeKey, time+"");
				lock.setOutTime((int) (time*1.5/1000));
			}else{
				jedis.set(maxHandleTimeKey, (defaultMaxHandleTime*1000)+"");
				lock.setOutTime((int) (time*1.5/1000));
			}
		}
		jedis.close();
	}

	/**
	 * 任务执行成功后，将任务从set里面移除
	 * @param taskJson
	 */
	private boolean removeTask(JSONObject taskJson) {
		try {
			Jedis jedis=redisUtil.getRedis();
			jedis.srem(redisSetKey, taskJson.toString());
			
			//有可能，消费者还没有执行完成的数据被添加到旧数据中了；所以当其执行完成时，需要删除，以免重新添加到任务list
			jedis.hdel(oldDataMapKey, taskJson.toString());
			jedis.close();
			return true;
		} catch(Exception e1) {
			e1.printStackTrace();
			logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"获取任务失败。"+taskJson.toString());
		}
		return false;
	}

	/**
	 * 从队列中获取一个任务
	 * 说明：如果是手动获取任务，则在处理完成任务后必须执行removeTask(JSONObject taskJson)
	 * @return
	 */
	private JSONObject getTask() {
		JSONObject json = null;
		try {
			Jedis jedis=redisUtil.getRedis();
			String value = jedis.rpop(redisListKey);
			if(value != null && !value.equals("")){
				json=JSONObject.fromObject(value);
			}
			jedis.close();
		} catch(Exception e1) {
			e1.printStackTrace();
			logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"获取任务失败。"+json.toString());
		}
		return json;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		task=createTask();
		if(task != null){
			lock=new LockUtil(task.getTaskName()+"_LOCK");
			redisListKey=task.getTaskName()+"_LIST";
			redisSetKey=task.getTaskName()+"_SET";
			oldDataMapKey=task.getTaskName()+"_OLD_DATA_MAP";
			maxHandleTimeKey=task.getTaskName()+"_MAX_HANDLE_TIME";
			
			start();
			logger.info("【"+task.getTaskName()+"】任务启动成功");
		}
	}
	
	/**
	 * -------------处理旧数据-------------
	 * 在非工作时间，如果list的set中的任务是一致的，就表示没有旧数据。否则，表示有旧数据。
	 */
	private void handleOldData() {
		Jedis jedis=redisUtil.getRedis();
		long len=jedis.scard(redisSetKey);
		if(len>0){
			//1.获取旧数据Map
			Map<String, String> oldDataMap=jedis.hgetAll(oldDataMapKey); //key为具体的任务map,value为保存时间戳
			for(String taskMap : oldDataMap.keySet()){
				long saveTime=Long.parseLong(oldDataMap.get(taskMap));
				long maxHandleTime=getMaxHandleTime();
				long currentTime=System.currentTimeMillis();
				
				if(currentTime>(saveTime+maxHandleTime*2)){//给消费者足够的处理时间，如果在这段时间这个任务还在Map中，则说明是旧数据
					//2.满足旧数据的要求
					Transaction tx=jedis.multi();
					//2.1从map中删除此数据
					tx.hdel(oldDataMapKey, taskMap);
					//2.2将此数据重新添加到队列
					tx.lpush(redisListKey, taskMap);
					tx.exec();
					logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"旧数据重新添加到队列。"+taskMap);
				}
			}
			//3.获取当前任务set中的数据
			Set<String> set=jedis.smembers(redisSetKey);
			//4.获取当前任务队列list中的数据
			List<String> list=jedis.lrange(redisListKey,0,-1);
			//5.添加时间戳，存入map
			for(String setValue : set){
				boolean exit=false;
				for(String listValue : list){
					if(setValue.equals(listValue)){
						exit=true;
						break;
					}
				}
				if(!exit){
					if(!jedis.hexists(oldDataMapKey, setValue)){
						//走到这里，表明这个数据可能是旧数据。
						jedis.hset(oldDataMapKey, setValue, System.currentTimeMillis()+"");
						logger.info("【"+task.getTaskName()+"】"+Thread.currentThread().getName()+"可能的旧数据。"+setValue);
					}
				}
			}
		}
		jedis.close();
	}

	private long getMaxHandleTime() {
		long time=(defaultMaxHandleTime*1000);
		Jedis jedis=redisUtil.getRedis();
		String maxTimeStr=jedis.get(maxHandleTimeKey);
		if(maxTimeStr != null && !maxTimeStr.equals("")){
			time=Long.parseLong(maxTimeStr);
		}
		return time;
	}

	private boolean ifWorkTime() {
		Calendar calendar =Calendar.getInstance();
		int current_hour_of_day=calendar.get(Calendar.HOUR_OF_DAY);
		int current_min=calendar.get(Calendar.MINUTE);
		if(current_hour_of_day>=task.getStartHourOfDay() && current_hour_of_day<task.getEndHourOfDay()){
			if(current_min>=task.getStartMinute() && current_min<=task.getEndMinute()){
				return true;
			}
		}
    	return false;
    }
	
	/**
	 * 生产消息接口
	 * @param limit 生产者一次性从数据库中读取的记录条数，可以不采用这个
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated
	public abstract List<Map> produce();
	
	/**
	 * 生产消息接口
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public abstract List<Map> produce_new();
	/**
	 * 消费消息接口
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated
	public abstract void consume(Map mes);
	/**
	 * 消费消息接口
	 */
	public abstract boolean consume_new(JSONObject msg);
	/**
	 * 定义任务
	 */
	public abstract ScheduleTask createTask();
}

