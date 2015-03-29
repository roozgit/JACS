package controllers;

import models.Lounge;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

import com.fasterxml.jackson.databind.JsonNode;

public class WebSocketApplication extends Controller {

	// get the ws.js script
	public static Result wsJs(String username) {
		return ok(views.js.ws.render(
				username
				));
	}

	// Websocket interface
	public static WebSocket<JsonNode> wsInterface(final String username) {
		return new WebSocket<JsonNode>() {
			// called when websocket handshake is done
			public void onReady(WebSocket.In<JsonNode> in,
					WebSocket.Out<JsonNode> out) {
				
				try {
					Lounge.join(username,in, out);
				} catch (Exception e) {
					Logger.error("Websocket handshake error",e.fillInStackTrace());
				}
			}
		};
	}
}
