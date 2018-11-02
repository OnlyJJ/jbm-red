package com.lm.jbm.thread;


import java.net.Socket;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.lm.jbm.service.JmService;
import com.lm.jbm.socket.SocketUtil;
import com.lm.jbm.utils.JsonUtil;
import com.lm.jbm.utils.RandomUtil;




public class GrapRebNoInroomThread implements Runnable {

	private String roomId;
	
	private String userId;
	
	
	public GrapRebNoInroomThread(String roomId, String userId) {
		this.roomId = roomId;
		this.userId = userId;
	}

	public void run() {
		Socket socket = null;
		try {
			socket = SocketUtil.inRoom(roomId, userId);
			while(true) {
				String msg = SocketUtil.recieve(socket);
				JSONObject ret = JsonUtil.strToJsonObject(msg);
				JSONObject data = JsonUtil.strToJsonObject(ret.getString("data"));
				if(data != null) {
					if(data.containsKey("type")) {
						int type = Integer.parseInt(data.get("type").toString());
						JSONObject content = JsonUtil.strToJsonObject(data.get("content").toString());
						switch(type) {
						case 8:
							if(content != null && content.containsKey("id")) {
								String rebId = content.getString("id");
								JmService.grapRebNoSocket(userId, rebId);
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
		} finally {
			if(socket != null) {
				try {
					Thread.sleep(10000);
					socket.close();
					Thread.sleep(3000);
					JmService.outRoom(roomId, userId);
				} catch (Exception e) {
				}
			}
		}
	}


}
