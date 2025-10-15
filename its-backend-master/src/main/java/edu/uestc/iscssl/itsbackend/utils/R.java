package edu.uestc.iscssl.itsbackend.utils;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class R extends ResponseEntity<Map<String,Object>> {

	private static final long serialVersionUID = 1L;

	public R(HttpStatus status) {
		super(status);
	}

	public R(Map<String, Object> body, HttpStatus status) {
		super(body, status);
	}


	public static R error() {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "未知异常，请联系管理员");
	}
	
	public static R error(String msg) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, msg);
	}
	
	public static R error(HttpStatus status, String msg) {
		Map<String,Object> map =new HashMap<>();
		map.put("msg",msg);
		return new R(map,status);
	}

	public static R ok(String msg) {
		Map<String,Object> map =new HashMap<>();
		map.put("msg",msg);
		return new R(map,HttpStatus.OK);
	}

	public static R ok(Map<String, Object> map) {
		return new R(map,HttpStatus.OK);
	}

}
