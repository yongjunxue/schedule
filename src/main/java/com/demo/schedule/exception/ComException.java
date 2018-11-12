package com.demo.schedule.exception;

public class ComException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1222370377565251673L;
	public ComException(){
		super();
	}
	public ComException(String msg){
		super(msg);
	}
}
