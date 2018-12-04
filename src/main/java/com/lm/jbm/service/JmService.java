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
import com.lm.jbm.utils.LogUtil;
import com.lm.jbm.utils.PropertiesUtil;
import com.lm.jbm.utils.RandomUtil;


public class JmService {
	public static ConcurrentHashMap<String, Long> goldMap = new ConcurrentHashMap<String, Long>(512);
	public static ConcurrentHashMap<String, String> serssionMap = new ConcurrentHashMap<String, String>(512);
	public static ConcurrentHashMap<String, String> grabMap = new ConcurrentHashMap<String, String>(512);
	public static final String U1 = PropertiesUtil.getValue("U1");
	public static final String U9 = PropertiesUtil.getValue("U9");
	public static final String U11 = PropertiesUtil.getValue("U11");
	public static final String U15 = PropertiesUtil.getValue("U15");
	public static final String U16 = PropertiesUtil.getValue("U16");
	public static final String U32 = PropertiesUtil.getValue("U32");
	public static final String U48 = PropertiesUtil.getValue("U48");
	public static final String U50 = PropertiesUtil.getValue("U50");
	public static final String IP = "192.168.200.16";
	public static final String TOTLE_KEY = "gold";
	public static final String NUMBER_KEY = "number";

	public static String getSessionId(String userId) {
		String sessionId = serssionMap.get(userId);
		if(StringUtils.isEmpty(sessionId)) {
			String ip = RandomUtil.getUserIp(userId);
			sessionId = login(userId, RandomUtil.getPwd(), ip);
			if(sessionId != null && !StringUtils.isEmpty(sessionId)) {
				serssionMap.put(userId, sessionId);
				sign(userId, sessionId, ip);
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
	
	public static void sign(String userId, String sessionId, String ip) {
		JSONObject json = new JSONObject();
		JSONObject session = new JSONObject();
		session.put("b", sessionId);
		
		JSONObject userbaseinfo = new JSONObject();
		userbaseinfo.put("a", userId);
		userbaseinfo.put("j", ip);
		
		json.put("session", session);
		json.put("userbaseinfo", userbaseinfo);
		
		// 是否签到
		boolean flag = false;
		String res = HttpUtils.post3(U50, json.toString(), ip);
		if(StringUtils.isNotEmpty(res)) {
			JSONObject data = JsonUtil.strToJsonObject(res);
			if(data != null && data.containsKey("signinfovo")) {
				JSONObject ret = JsonUtil.strToJsonObject(data.getString("signinfovo"));
				String signFlag = ret.getString("e");
				if(signFlag.equalsIgnoreCase("n")) {
					flag = true;
				}
			}
		}
		if(flag) {
			System.err.println("签到：" + userId);
			HttpUtils.post3(U48, json.toString(), ip);
		}
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
			return HttpUtils.post3(U16, str, IP);
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
			return HttpUtils.post3(U16, str, IP);
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
	
	public static void grapReb(String roomId, String userId, String sessionId, String rebId, String ip) throws Exception {
		try {
			JSONObject json = new JSONObject();
			JSONObject info = new JSONObject();
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
			
			boolean success = false;
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
					LogUtil.log.info("抢红包：" + userId + "，抢到：" + gold);
					long total = gold;
					long num = 1;
					long nowTotal = gold;
					if(goldMap.containsKey(TOTLE_KEY)) {
						total += goldMap.get(TOTLE_KEY);
					}  
					if(goldMap.containsKey(NUMBER_KEY)) {
						num += goldMap.get(NUMBER_KEY);
					}
					if(goldMap.containsKey(roomId)) {
						nowTotal += goldMap.get(roomId);
					}
					goldMap.put(TOTLE_KEY, total);
					goldMap.put(NUMBER_KEY, num);
					goldMap.put(roomId, nowTotal);
					success = true;
				}
			}
			
			// 摘桃成功后，触发修改昵称
			if(success && RandomUtil.isTrue()) {
				info.put("session", session);
				JSONObject userbaseinfo1 = new JSONObject();
				userbaseinfo1.put("a", userId);
				info.put("userbaseinfo", userbaseinfo1);
				JSONObject anchorinfo = new JSONObject();
				String nickname = RandomUtil.getNickname(userId);
				anchorinfo.put("d", nickname);
				String remark = RandomUtil.getRemark(userId);
				if(StringUtils.isNotEmpty(remark)) {
					anchorinfo.put("h", remark);
				} else {
					anchorinfo.put("h", "暂无");
				}
				anchorinfo.put("y", "");
				anchorinfo.put("x", "");
				anchorinfo.put("z", "");
				String brith = RandomUtil.getBrithday(userId);
				if(StringUtils.isNotEmpty(brith)) {
					anchorinfo.put("m", brith);
				}
				anchorinfo.put("l", "男");
				info.put("anchorinfo", anchorinfo);
				String resp = HttpUtils.post3(U11, info.toString(), ip);
				if(StringUtils.isNotEmpty(resp)) {
					JSONObject data = JsonUtil.strToJsonObject(resp);
					if(data != null && data.containsKey("result")) {
						JSONObject result = data.getJSONObject("result");
						int a = result.getIntValue("a");
						if(a == 2020) { // 昵称被占用
							nickname = RandomUtil.reSetNickname(nickname);
							anchorinfo.put("d", nickname);
							info.put("anchorinfo", anchorinfo);
							HttpUtils.post3(U11, info.toString(), ip);
						}
					}
				}
				System.err.println("抢红包成功，修改昵称：" + userId + "，nickname：" + nickname);
			}
		} catch(Exception e) {
			throw e;
		}
		throw new Exception("结束当前用户socket！");
	}
	
	public static void grapRebNoSocket(String userId, String rebId) throws Exception {
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
	
	public static void grapReb(String roomId, int num) {
		try {
			if(checkFreeTime()) {
				System.err.println("凌晨时段，不参与抢红包！");
				return;
			}
			Thread.sleep(8000);
			int real = findOnline(roomId);
			boolean socketInroom = true;
			String[] userIds = RandomUtil.getUserIds();
			List<String> list = Arrays.asList(userIds);
			Collections.shuffle(list);
			int index = 1;
			int size = list.size();
			if(socketInroom) {
				for(int i=0; i<size; i++) {
					if(num >= 45) { // 红包个数区间
						if(index > 23) { // 抢红包总个数
							break;
						} 
					} else if(num >= 35) {
						if(index > 20) {
							break;
						} 
					} else if(num >= 30) {
						if(index > 16) {
							break;
						} 
					} else if(num >= 25) {
						if(index > 13) {
							break;
						} 
					} else if(num >= 20) {
						if(index > 10) {
							break;
						} 
					} else if(num >= 15) {
						if(index > 8) {
							break;
						} 
					} else if(num >= 10) {
						if(index > 5) {
							break;
						} 
					} else if(num >= 5) {
						if(index > 3) {
							break;
						} 
					} else {
						if(index > 2) {
							return;
						} 
					}
					String userId = list.get(i);
					if(grabMap.containsKey(userId)) {
						continue;
					}
					grabMap.put(userId, roomId);
					Thread.sleep(1000);
					GrapRebThread reb = new GrapRebThread(roomId, userId);
					ThreadManager.getInstance().execute(reb);
					index++;
				}
				System.err.println("本次参与抢红包总人数：" + index);
				Thread.sleep(60000);
				LogUtil.log.info("抢红包房间：" + roomId 
						+ "，参与人数: " + index 
						+ "，抢成功人数：" + goldMap.get(NUMBER_KEY)
						+ "，本房间总共抢到：" + goldMap.get(roomId)
						+ "，当天总共抢到：" +  goldMap.get(TOTLE_KEY)
						);
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
