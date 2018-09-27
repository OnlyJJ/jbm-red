package com.lm.jbm.service;


import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lm.jbm.thread.GrapRebNoInroomThread;
import com.lm.jbm.thread.GrapRebThread;
import com.lm.jbm.thread.ThreadManager;
import com.lm.jbm.utils.DateUtil;
import com.lm.jbm.utils.HttpUtils;
import com.lm.jbm.utils.JsonUtil;
import com.lm.jbm.utils.PropertiesUtil;
import com.lm.jbm.utils.RandomUtil;


public class JmService {
	public static ConcurrentHashMap<String, String> serssionMap = new ConcurrentHashMap<String, String>(512);
	public static ConcurrentHashMap<String, String> grabMap = new ConcurrentHashMap<String, String>(512);
	public static final String U1 = PropertiesUtil.getValue("U1");
	public static final String U9 = PropertiesUtil.getValue("U9");
	public static final String U15 = PropertiesUtil.getValue("U15");
	public static final String U16 = PropertiesUtil.getValue("U16");
	public static final String U32 = PropertiesUtil.getValue("U32");

	public static String getSessionId(String userId) {
		String sessionId = serssionMap.get(userId);
		if(StringUtils.isEmpty(sessionId)) {
			sessionId = login(userId, RandomUtil.getPwd(), RandomUtil.getIp());
			if(sessionId != null && !StringUtils.isEmpty(sessionId)) {
				serssionMap.put(userId, sessionId);
			}
		}
		return sessionId;
	}
	
	public static String login(String userId, String pwd, String ip) {
		try {
			JSONObject json = new JSONObject();
			JSONObject userbaseinfo = new JSONObject();
			userbaseinfo.put("a", userId);
			userbaseinfo.put("b", pwd);
			userbaseinfo.put("j", ip);
			json.put("userbaseinfo", userbaseinfo);
			String str = json.toString();
			String strRes = HttpUtils.post(U1, str);
			JSONObject res = JsonUtil.strToJsonObject(strRes);
			String sessionId = null;
			if (res != null) {
				JSONObject session = JsonUtil.strToJsonObject(res
						.getString("session"));
				if (session != null && session.containsKey("b")) {
					sessionId = session.get("b").toString();
					serssionMap.put(userId, sessionId);
				}
			}
			return sessionId;
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	public static int findOnline(String roomId) throws Exception{
		try {
			JSONObject json = new JSONObject();
			JSONObject roomonlineinfo = new JSONObject();
			roomonlineinfo.put("b", roomId);
			JSONObject page = new JSONObject();
			page.put("b", "1");
			page.put("c", "50");
			json.put("roomonlineinfo", roomonlineinfo);
			json.put("page", page);
			String str = json.toString();
			String res = HttpUtils.post(U15, str);
			int real = 0;
			int total = 0;
			if(StringUtils.isNotEmpty(res)) {
				JSONObject data = JsonUtil.strToJsonObject(res);
				if(data != null) {
					if(data.containsKey("page")) {
						JSONObject pageData = JsonUtil.strToJsonObject(data.getString("page"));
						total = Integer.parseInt(pageData.getString("a"));
					}
					if(total >0) {
						if(data.containsKey("onlineuserinfo")) {
							String[] configs = RandomUtil.getUserIds();
							List<String> userids = null;
							if(configs != null && configs.length >0) {
								userids = Arrays.asList(configs);
							}
							JSONArray array = JsonUtil.strToJSONArray(data.getString("onlineuserinfo"));
							if(array != null && array.size() >0) {
								int size = array.size();
								for(int i=0; i<size; i++) {
									JSONObject obj = array.getJSONObject(i);
									String userId = obj.getString("a");
									if(userId.indexOf("robot") != -1 || userId.indexOf("pesudo") != -1) {
										break;
									}
									if(userids != null && userids.contains(userId)) {
										continue;
									}
									real++;
								}
							}
						}
					}
				}
			}
			System.err.println("房间：" + roomId + "，当前真实用户数：" + real);
			return real;
		} catch(Exception e) {
			System.err.println(e.getMessage());
//			throw new Exception("查询房间信息错误，退出当前循环!");
			return 0;
		}
	}

	public static String inRoom(String roomId, String userId) {
		try {
			JSONObject json = new JSONObject();
			JSONObject roomonlineinfo = new JSONObject();
			roomonlineinfo.put("a", 1);
			roomonlineinfo.put("b", roomId);
			JSONObject onlineUserInfo = new JSONObject();
			onlineUserInfo.put("a", userId);
			roomonlineinfo.put("c", onlineUserInfo);
			json.put("roomonlineinfo", roomonlineinfo);
			String str = json.toString();
			return HttpUtils.post(U16, str);
		} catch(Exception e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	public static String outRoom(String roomId, String userId) {
		try {
			JSONObject json = new JSONObject();
			JSONObject roomonlineinfo = new JSONObject();
			roomonlineinfo.put("a", 2);
			roomonlineinfo.put("b", roomId);
			JSONObject onlineUserInfo = new JSONObject();
			onlineUserInfo.put("a", userId);
			roomonlineinfo.put("c", onlineUserInfo);
			json.put("roomonlineinfo", roomonlineinfo);
			String str = json.toString();
			return HttpUtils.post(U16, str);
		} catch(Exception e) {
			System.err.println(e.getMessage());
		}
		return null;
	}
	
	
	
	
	
	public static boolean checkFreeTime() {
		try {
			Date now = new Date();
			String str1 = DateUtil.format2Str(now, "yyyy-MM-dd") + " 01:30:00";
			String str2 = DateUtil.format2Str(now, "yyyy-MM-dd") + " 08:00:00";
			Date d = DateUtil.parse(str1, "yyyy-MM-dd HH:mm:ss");
			Date d2 = DateUtil.parse(str2, "yyyy-MM-dd HH:mm:ss");
			if(now.after(d) && now.before(d2)) {
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}
	
	public static void grapReb(String userId, String sessionId, String rebId, String ip) throws Exception {
		try {
			grabMap.remove(userId);
			JSONObject json = new JSONObject();
			JSONObject session = new JSONObject();
			session.put("b", sessionId);
			
			JSONObject userbaseinfo = new JSONObject();
			userbaseinfo.put("a", userId);
			userbaseinfo.put("j", ip);
			
			JSONObject redpacketsendvo = new JSONObject();
			redpacketsendvo.put("a", rebId);
			
			json.put("session", session);
			json.put("userbaseinfo", userbaseinfo);
			json.put("redpacketsendvo", redpacketsendvo);
			
			String str = json.toString();
			String res = HttpUtils.post3(U32, str, ip);
			if(StringUtils.isNotEmpty(res)) {
				JSONObject data = JsonUtil.strToJsonObject(res);
				if(data != null && data.containsKey("redpacketreceivevo")) {
					JSONObject peachvoJson = JsonUtil.strToJsonObject(data.getString("redpacketreceivevo"));
					int gold = peachvoJson.getIntValue("d");
					StringBuilder msg = new StringBuilder();
					msg.append(userId).append("抢红包，抢到：").append("X").append(gold).append("个金币");
					System.err.println(msg.toString());
				}
			}
		} catch(Exception e) {
			throw e;
		}
		throw new Exception("结束当前用户socket！");
	}
	
	public static void grapRebNoSocket(String userId, String rebId) throws Exception {
		grabMap.remove(userId);
		String[] userIds = RandomUtil.getUserIds();
		List<String> list = Arrays.asList(userIds);
		Collections.shuffle(list);
		int size = list.size();
		int index = 0;
		int total = 0;
		JSONObject redpacketsendvo = new JSONObject();
		redpacketsendvo.put("a", rebId);
		for(int i=0; i<size; i++) {
			try {
				if(index > 20) {
					return;
				}
				String uid = list.get(i);
				String ip = RandomUtil.getUserIp(uid);
				String sessionId = getSessionId(uid);
				JSONObject json = new JSONObject();
				JSONObject session = new JSONObject();
				session.put("b", sessionId);
				
				JSONObject userbaseinfo = new JSONObject();
				userbaseinfo.put("a", uid);
				userbaseinfo.put("j", ip);
				
				json.put("session", session);
				json.put("userbaseinfo", userbaseinfo);
				json.put("redpacketsendvo", redpacketsendvo);
				
				String str = json.toString();
				String res = HttpUtils.post3(U32, str, ip);
				if(StringUtils.isNotEmpty(res)) {
					JSONObject data = JsonUtil.strToJsonObject(res);
					if(data != null && data.containsKey("redpacketreceivevo")) {
						JSONObject peachvoJson = JsonUtil.strToJsonObject(data.getString("redpacketreceivevo"));
						int gold = peachvoJson.getIntValue("d");
						total += gold;
						StringBuilder msg = new StringBuilder();
						msg.append(uid).append("抢红包，抢到：").append("X").append(gold).append("个金币");
						System.err.println(msg.toString());
					}
				}
				index++;
			} catch(Exception e) {
				
			}
		}
		System.err.println("抢红包，总共抢到金币：" + total);
		throw new Exception("退出监听用户socket");
	}
	
	public static void grapReb(String roomId) {
		try {
			if(checkFreeTime()) {
				System.err.println("凌晨时段，不参与抢红包！");
				return;
			}
			Thread.sleep(10000);
			int real = findOnline(roomId);
			boolean socketInroom = true;
//			if(real < 45) {
//				socketInroom = true;
//			}
			String[] userIds = RandomUtil.getUserIds();
			List<String> list = Arrays.asList(userIds);
			Collections.shuffle(list);
			int index = 1;
			int size = list.size();
			if(socketInroom) {
				for(int i=0; i<size; i++) {
					if(index > 17) {
						return;
					}
					String userId = list.get(i);
					if(grabMap.containsKey(userId)) {
						continue;
					}
					grabMap.put(userId, roomId);
					Thread.sleep(RandomUtil.getRandom(500, 1000));
					GrapRebThread reb = new GrapRebThread(roomId, userId);
					ThreadManager.getInstance().execute(reb);
					index++;
				}
			} else {
				String userId = list.get(RandomUtil.getRandom(0, list.size()));
				if(grabMap.containsKey(userId)) {
					userId = list.get(RandomUtil.getRandom(0, list.size()));
				}
				grabMap.put(userId, roomId);
				GrapRebNoInroomThread t = new GrapRebNoInroomThread(roomId, userId);
				ThreadManager.getInstance().execute(t);
			}
		} catch(Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
