package com.yuhe.american.log_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.yuhe.american.db.log.CommonDB;
import com.yuhe.american.utils.RegUtils;

import net.sf.json.JSONObject;

public class GoldLog extends AbstractLogModule {

	private static final String[] LOG_COLS = { "Uid", "Urs", "Value", "Type", "Remain", "Name" };
	private static final String[] DB_COLS = { "HostID", "Uid", "Urs", "Changes", "Reason", "Remain", "Name", "GoldType",
			"Time" };
	private static String TBL_NAME = "tblGoldLog";
	private static Map<String, String> COL_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("Value", "Changes");
			put("Type", "Reason");
		}
	};

	@Override
	public Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		for (String logStr : logList) {
			JSONObject json = JSONObject.fromObject(logStr);
			if (json != null) {
				String message = json.getString("message");
				String hostID = json.getString("hostid");
				if (StringUtils.isNotBlank(message) && hostMap.containsKey(hostID)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("HostID", hostID);
					String type = json.getString("type");
					boolean isSub = type.indexOf("sub") != -1;
					String time = RegUtils.getLogTime(message);
					map.put("Time", time);
					for (String col : LOG_COLS) {
						String value = RegUtils.getLogValue(message, col, "");
						if (col.equals("Value") && isSub) {
							value = "-" + value;
						}
						col = COL_MAP.getOrDefault(col, col);
						map.put(col, value);
					}
					// 还要加上钻石类型
					String goldType = "1"; // 默认是钻石
					if (type.indexOf("creditgold") != -1)
						goldType = "2"; // 绑钻
					map.put("GoldType", goldType);
					String platformID = hostMap.get(hostID);
					List<Map<String, String>> platformResult = platformResults.get(platformID);
					if (platformResult == null)
						platformResult = new ArrayList<Map<String, String>>();
					platformResult.add(map);
					platformResults.put(platformID, platformResult);
				}
			}
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			CommonDB.batchInsertByDate(platformID, platformResult, DB_COLS, TBL_NAME);
		}
		return platformResults;
	}

	@Override
	public String getStaticsIndex() {
//		return "Gold";
		return null;
	}

	@Override
	public Map<String, List<Map<String, String>>> execute4Kafka(JSONObject json, Map<String, String> staticsHosts) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		String message = json.getString("message");
		String hostID = json.getString("hostid");
		if (StringUtils.isNotBlank(message) && staticsHosts.containsKey(hostID)) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("HostID", hostID);
			String type = json.getString("type");
			boolean isSub = type.indexOf("sub") != -1;
			String time = RegUtils.getLogTime(message);
			map.put("Time", time);
			for (String col : LOG_COLS) {
				String value = RegUtils.getLogValue(message, col, "");
				if (col.equals("Value") && isSub) {
					value = "-" + value;
				}
				col = COL_MAP.getOrDefault(col, col);
				map.put(col, value);
			}
			// 还要加上钻石类型
			String goldType = "1"; // 默认是钻石
			if (type.indexOf("creditgold") != -1)
				goldType = "2"; // 绑钻
			map.put("GoldType", goldType);
			String platformID = staticsHosts.get(hostID);
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			if (platformResult == null)
				platformResult = new ArrayList<Map<String, String>>();
			platformResult.add(map);
			platformResults.put(platformID, platformResult);
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			CommonDB.batchInsertByDate(platformID, platformResult, DB_COLS, TBL_NAME);
		}
		return platformResults;
	}

}
