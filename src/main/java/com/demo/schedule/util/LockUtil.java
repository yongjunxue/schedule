package com.demo.schedule.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

/**
 * 锁
 * @author xueyongjun
 *
 */
public class LockUtil {
	
	private RedisUtil redisUtil=new RedisUtil();
	
	private String lockKey;
	
	private boolean spin=false;
	
	private int maxSpinCount=10;
	
	private int outTime=5;//锁过期时间，默认为5秒
	
	private final int threadSleepTimeMills=10;//10毫秒
	
	public LockUtil(String lockKey){
		this.lockKey=lockKey;
	}
	
	@SuppressWarnings("static-access")
	public boolean tryLock(){
		boolean flag=false;
		if(spin){
			for(int i=1;i<=maxSpinCount;i++){
				flag=lock();
				if(flag){
					break;
				}
				try {
					Thread.currentThread().sleep(threadSleepTimeMills);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}else{
			flag=lock();
		}
		return flag;
	}
	
	/**
	 * 
	 * @param outTime
	 * @return
	 */
	@SuppressWarnings("static-access")
	public boolean tryLock(final int outTime){
		final StopFlag stopFlag=new StopFlag();
		boolean flag=false;
			Thread stopThread=new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.currentThread().sleep(outTime*1000);
						stopFlag.stop(true);
					} catch (InterruptedException e) {
					}
				}
			});
			stopThread.setDaemon(true);
			stopThread.start();
			while(!stopFlag.isStop()){
				flag=lock();
				if(flag){
					stopThread.interrupt();
					break;
				}else{
					try {
						Thread.currentThread().sleep(threadSleepTimeMills);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		return flag;
	}
	
	class StopFlag{
		
		private boolean stopFlag=false;
		
		public boolean isStop() {
			return stopFlag;
		}
		
		public void stop(boolean stopFlag) {
			this.stopFlag= stopFlag;
		}
	}
	
	private boolean lock() {
		boolean flag=false;
		Jedis jedis=redisUtil.getRedis();
		JSONObject lockJson=createLock();
		Transaction tx=jedis.multi();
		tx.setnx(lockKey, lockJson.toString());
		tx.expire(lockKey,outTime );
		List<Response<?>> res=tx.execGetResponse();
		if(res != null && res.size()>0){
			Response<?>re=res.get(0);
			if(re.get().equals("1") || re.get().equals("OK")){
				flag=true;
			}else{
				String str=jedis.get(lockKey);
				lockJson=JSONObject.fromObject(str);
				String ip=getIp();
				String threadId=Thread.currentThread().getId()+"";
				if(lockJson.getString("ip") != null && lockJson.getString("ip").equals(ip)){
					if(lockJson.get("threadId") != null && lockJson.getString("threadId").equals(threadId)){
						long count=lockJson.getLong("count");
						lockJson.put("count", ++count);
						tx=jedis.multi();
						tx.set(lockKey, lockJson.toString());
						tx.expire(lockKey,outTime );
						res=tx.execGetResponse();
						if(res != null && res.size()>0){
							re=res.get(0);
							if(re.get().equals("1") || re.get().equals("OK")){
								flag=true;
							}
						}
					}
				}
			}
		}
		if(flag){
//			System.out.println("锁+1："+getLockInfo());
		}
		jedis.close();
		return flag;
	}

	public void unLock(){
		Jedis jedis=redisUtil.getRedis();
		String str=jedis.get(lockKey);
		if(str != null){
			JSONObject lockJson=JSONObject.fromObject(str);
			
			String ip=getIp();
			String threadId=Thread.currentThread().getId()+"";
			if(lockJson.getString("ip") != null && lockJson.getString("ip").equals(ip)){
				if(lockJson.get("threadId") != null && lockJson.getString("threadId").equals(threadId)){
					int count=lockJson.getInt("count");
					count--;
					if(count<0){
						count=0;
					}
					if(count==0){
						jedis.del(lockKey);
//						System.out.println("锁释放");
					}else{
						lockJson.put("count", count);
						Transaction tx=jedis.multi();
						tx=jedis.multi();
						tx.set(lockKey, lockJson.toString());
						tx.expire(lockKey,outTime );
						tx.execGetResponse();
//						if(res != null && res.size()>0){
//							Response<?>re=res.get(0);
//							if(re.get().equals("1") || re.get().equals("OK")){
//								System.out.println("锁-1:"+getLockInfo());
//							}
//						}
					}
				}
			}
		}else{
			//锁还没有释放，就失效了
		}
		jedis.close();
	}
	
	private JSONObject createLock() {
		JSONObject lockJson=new JSONObject();
		String ip=getIp();
		String threadId=Thread.currentThread().getId()+"";
		long count=0;
		lockJson.put("ip", ip);
		lockJson.put("threadId", threadId);
		lockJson.put("count", count);
//		lockJson.put("lockKey", lockKey);
//		lockJson.put("createTime", System.currentTimeMillis());
		return lockJson;
	}
	
	private String getIp() {
		String ip=null;
		try {
			InetAddress ia=InetAddress.getLocalHost();
			ip=ia.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ip;
	}
	
	public String getLockInfo(){
		Jedis jedis=redisUtil.getRedis();
		String info=jedis.get(lockKey);
		jedis.close();
		return info;
	}
	
	public int getOutTime() {
		return outTime;
	}

	public void setOutTime(int outTime) {
		this.outTime = outTime;
	}
}
