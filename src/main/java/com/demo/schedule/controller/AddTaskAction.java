package com.demo.schedule.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.demo.schedule.service.TestSchedule;

/**
 * 这里可以通过页面的按钮等等添加任务
 * @author xueyongjun
 *
 */
@Controller
public class AddTaskAction {
	
	@Autowired
	TestSchedule testSchedule;
	
	@RequestMapping("/addTask")
	public String addTask(HttpServletRequest request,HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		List<Map> list=new ArrayList<>();
		Map m=new HashMap<>();
		m.put("id", "3");
		m.put("name", "王五");
		testSchedule.addTask(m);
		
        response.setCharacterEncoding("GBK");
        PrintWriter print;
		try {
			print = response.getWriter();
			print.write("添加任务成功");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
}
