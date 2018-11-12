package com.demo.schedule.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.springframework.stereotype.Component;

import com.demo.schedule.util.PropertiesHolderUtil;
import com.demo.schedule.util.schedule.ScheduleSupport;
import com.demo.schedule.util.schedule.ScheduleTask;

/**
 * 调度器服务类
 * @author xueyongjun
 *
 */
@Component
public class TestSchedule extends ScheduleSupport{
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> produce_new() {
		List<Map> list=new ArrayList<>();
		Map m=new HashMap<>();
		m.put("id", "1");
		m.put("name", "张三");
		list.add(m);
		m=new HashMap<>();
		m.put("id", "2");
		m.put("name", "李四");
		list.add(m);
		System.out.println("produce:"+list.toString());
		return list;
	}
	
	@Override
	public boolean consume_new(JSONObject msg) {
		System.out.println("consume:"+msg.toString());
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> produce() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void consume(Map mes) {
	}

	@Override
	public ScheduleTask createTask() {
		ScheduleTask task=null;
		String ifStartTestSchedule=PropertiesHolderUtil.getInstance().getProperty("ifStartTestSchedule");//本机器是否启动TestSchedule
		if(ifStartTestSchedule != null && ifStartTestSchedule.equals("1")){
			boolean startProvider=false;
			boolean startConsumer=false;
			String startProviderStr=PropertiesHolderUtil.getInstance().getProperty("testSchedule.ifStartProvider");//本机器是否启动TestSchedule的生产者
			String startComsumerStr=PropertiesHolderUtil.getInstance().getProperty("testSchedule.ifStartConsumer");//本机器是否启动TestSchedule的消费者
			if(startProviderStr != null && startProviderStr.equals("1")){
				startProvider=true;
			}
			if(startComsumerStr != null && startComsumerStr.equals("1")){
				startConsumer=true;
			}
			
			task=new ScheduleTask()
			.setStartProvider(startProvider)
			.setStartConsumer(startConsumer)
			.setComsumerThreadCount(2)
			.setProviderThreadCount(1)
//			.setQueueLimit(20)
			
			.setStartHourOfDay(2)
			.setEndHourOfDay(23)//不包含5点之后
//			.setStartMinute(15)
//			.setEndMinute(20)
			.setTaskPeriod(3)
			.setTaskName("调度器测试");
		}
		return task;
	}

}
