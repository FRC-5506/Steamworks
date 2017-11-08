package com.midcoastmaineiacs.Steamworks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardServer extends WebSocketTableServer {
	private static final int PORT = 5800;
	private Map<String, Serializable> indicators = new HashMap<>();
	private List<String> keys = new ArrayList<>();

	public DashboardServer() {
		super("DashboardServer", PORT);
	}

	@Override
	protected void setDefaults() {
		setBoolean("competition", false);
		setBoolean("enabled", true);
		setDouble("pos", 2);
		setString("auto", "playdead");
		setDouble("heading", 0);
		setString("name", "MMDashboard");
		setString("layout", "{\"toggles\":{\"competition\":\"orange\\u0000Practice\\u0000Competition\",\"power\":\"orange\\u0000Half speed\\u0000Full speed\"},\"compassHeight\":2,\"fieldList\":[\"competition\",\"power\"]}");
	}

	public void addIndicator(String key, String on, String off, String offColor, boolean value) {
		indicators.put(key, offColor + "\0" + off + "\0" + on);
		keys.add(key);
		setBoolean(key, value);
	}

	public void addReadout(String key, String text, String color) {
		keys.add(key);
		setColoredString(key, text, color);
	}

	public void addReadout(String key, String text) {
		keys.add(key);
		setString(key, text);
	}

	public void setColoredString(String key, String text, String color) {
		setString(key, color + "\0" + text);
	}

	public void applyLayout(int minCompassHeight) {
		Map<String, Object> map = new HashMap<>();
		map.put("toggles", indicators);
		if (indicators.size() + minCompassHeight % 2 == 0)
			map.put("compassHeight", minCompassHeight);
		else
			map.put("compassHeight", minCompassHeight + 1);
		map.put("fieldList", keys);
		ObjectMapper m = new ObjectMapper();
		try {
			setString("layout", m.writeValueAsString(map));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		System.out.println(getString("layout"));
	}
}
