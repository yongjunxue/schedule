任务调度器
使用方法：（可以参考TestSchedule）
1.继承ScheduleSupport，实现produce_new()、consume_new()、createTask()三个方法
2.具体的任务使用ScheduleTask创建
