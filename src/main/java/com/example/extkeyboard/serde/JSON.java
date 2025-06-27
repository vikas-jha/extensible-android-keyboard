package com.example.extkeyboard.serde;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSON<T> {

	private static GsonBuilder gsonBuilder = new GsonBuilder();
	private static Gson gson;
	static{
		gson = gsonBuilder.create();
	}
	
	private String json;
	private Class<T> type;
	private Object obj;

	public JSON(String json, Class<T> type) {
		this.json = json;
		this.type = type;
	}

	public JSON(Object obj) {
		this.obj = obj;
	}

	public String toString() {
		if (json == null && obj != null) {
			json = gson.toJson(obj);
		}
		return this.json;
	}

	public T toObject() {
		if (obj == null && json != null && this.type != null) {
			obj = gson.fromJson(json, type);
		}
		return (T) this.obj;
	}

	public Type getType() {
		return this.type;
	}

}
