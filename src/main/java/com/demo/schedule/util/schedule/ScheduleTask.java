package com.demo.schedule.util.schedule;

import java.util.concurrent.TimeUnit;
/**
 * 定时任务 的定义
 * 任务名称，任务的开始时间、结束时间、间隔时间等
 * @author xueyongjun
 *
 */
public class ScheduleTask{
	
	private String taskName;//任务名称
	private int startHourOfDay=0;//任务开始时间点，默认为0点
	private int endHourOfDay=24;//任务结束时间点，默认为24点
	private int startMinute=0;//每个小时内，任务开始的分钟数
	private int endMinute=60;//每个小时内，任务结束的分钟数
	private int taskPeriod=1;//处理试卷间隔时间,默认为妙
	private TimeUnit timeUnit=TimeUnit.SECONDS;//时间单位定位秒
	
	private boolean startProvider=false;//是否启动生成者（集群）
	private boolean startConsumer=false;//是否启动消费者（集群）
	private int providerThreadCount=1;//生产者线程数
	private int comsumerThreadCount=1;//消费者线程数
	
	@Deprecated
	private int queueLimit=16;//队列中任务最大值
	
	public String getTaskName() {
		return taskName;
	}

	public ScheduleTask setTaskName(String taskName) {
		this.taskName = taskName;
		return this;
	}

	public int getStartHourOfDay() {
		return startHourOfDay;
	}

	public ScheduleTask setStartHourOfDay(int startHourOfDay) {
		this.startHourOfDay = startHourOfDay;
		return this;
	}

	public int getEndHourOfDay() {
		return endHourOfDay;
	}

	public ScheduleTask setEndHourOfDay(int endHourOfDay) {
		this.endHourOfDay = endHourOfDay;
		return this;
	}

	public int getStartMinute() {
		return startMinute;
	}

	public ScheduleTask setStartMinute(int startMinute) {
		this.startMinute = startMinute;
		return this;
	}

	public int getEndMinute() {
		return endMinute;
	}

	public ScheduleTask setEndMinute(int endMinute) {
		this.endMinute = endMinute;
		return this;
	}

	public int getTaskPeriod() {
		return taskPeriod;
	}

	public ScheduleTask setTaskPeriod(int taskPeriod) {
		if(taskPeriod<1){
			throw new RuntimeException("定时器任务间隔时间必须大于0");
		}
		this.taskPeriod = taskPeriod;
		return this;
	}

	public int getProviderThreadCount() {
		return providerThreadCount;
	}

	public ScheduleTask setProviderThreadCount(int providerThreadCount) {
		this.providerThreadCount = providerThreadCount;
		return this;
	}

	public int getComsumerThreadCount() {
		return comsumerThreadCount;
	}

	public ScheduleTask setComsumerThreadCount(int comsumerThreadCount) {
		this.comsumerThreadCount = comsumerThreadCount;
		return this;
	}

	public int getQueueLimit() {
		return queueLimit;
	}

	public ScheduleTask setQueueLimit(int queueLimit) {
		this.queueLimit = queueLimit;
		return this;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public boolean isStartProvider() {
		return startProvider;
	}

	public ScheduleTask setStartProvider(boolean startProvider) {
		this.startProvider = startProvider;
		return this;
	}

	public boolean isStartConsumer() {
		return startConsumer;
	}

	public ScheduleTask setStartConsumer(boolean startConsumer) {
		this.startConsumer = startConsumer;
		return this;
	}

}

