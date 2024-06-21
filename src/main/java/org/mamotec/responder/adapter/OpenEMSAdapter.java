package org.mamotec.responder.adapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class OpenEMSAdapter {
	private static final String BASE_URL = "http://x:admin@";
	private static final String MODBUS_TCP_IP = "0.0.0.0"; // Stelle sicher, dass die `variables` Klasse und `modbus_tcp_ip` Variable vorhanden sind

	public static Integer getCurrentPower(String inverterId) {
		try {
			String url = BASE_URL + MODBUS_TCP_IP + ":8084/rest/channel/" + inverterId + "/ActivePower";
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(url))
					.GET()
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			JSONObject responseObject = new JSONObject(response.body());
			return responseObject.getInt("value");
		} catch (Exception e) {
			System.out.println("Error getting current power: " + e.getMessage());
			return null;
		}
	}

	public static Integer getPeakPower(String inverterId) {
		try {
			String url = BASE_URL + MODBUS_TCP_IP + ":8084/rest/channel/" + inverterId + "/MaxApparentPower";
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(url))
					.GET()
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			JSONObject responseObject = new JSONObject(response.body());
			return responseObject.getInt("value");
		} catch (Exception e) {
			System.out.println("Error getting peak power: " + e.getMessage());
			return null;
		}
	}

	public static Integer writeChannelValue(String inverterId, String channel, int value) {
		try {
			String url = BASE_URL + MODBUS_TCP_IP + ":8084/rest/channel/" + inverterId + "/" + channel;
			HttpClient client = HttpClient.newHttpClient();
			JSONObject json = new JSONObject();
			json.put("value", value);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(url))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				JSONObject responseObject = new JSONObject(response.body());
				return responseObject.getInt("value");
			} else {
				System.out.println("Error updating channel value: " + response.body());
				return null;
			}
		} catch (Exception e) {
			System.out.println("Error updating channel value: " + e.getMessage());
			return null;
		}
	}
}
