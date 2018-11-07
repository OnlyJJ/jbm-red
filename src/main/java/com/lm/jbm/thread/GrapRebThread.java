package com.lm.jbm.thread;


import java.net.Socket;

import com.alibaba.fastjson.JSONObject;
import com.lm.jbm.service.JmService;
import com.lm.jbm.socket.SocketUtil;
import com.lm.jbm.utils.JsonUtil;
import com.lm.jbm.utils.RandomUtil;




public class GrapRebThread implements Runnable {

	private String roomId;
	
	private String userId;
	
	public GrapRebThread(String roomId, String userId) {
		this.roomId = roomId;
		this.userId = userId;
	}

	public void run() {
		Socket socket = null;
		try {
			String ip = RandomUtil.getUserIp(userId);
			socket = SocketUtil.inRoom(roomId, userId);
			String session = JmService.getSessionId(userId);
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
								if(content.containsKey("isOpen")) {
									int isOpen = content.getIntValue("isOpen");
									if(isOpen == 0) {
										System.err.println("本房间红包，不参与！");
										break;
									}
								}
								String rebId = content.getString("id");
								JmService.grapReb(userId, session, rebId, ip);
								break;
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				if(socket != null) {
					Thread.sleep(15000);
					socket.close();
				}
			Thread.sleep(5000);
			JmService.outRoom(roomId, userId);
			
			JmService.grabMap.remove(userId);
			} catch (Exception e) {
			}
		}
	}


	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
