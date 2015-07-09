package org.jinsu.smarthome;

import org.jinsu.smarthome.model.Account;
import org.jinsu.smarthome.model.Action;
import org.jinsu.smarthome.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

public class ServiceListener {
	private final Logger logger = LoggerFactory.getLogger(ServiceListener.class);
	private SocketIOServer server;

	public ServiceListener(SocketIOServer server) {
		this.server = server;
	}

	@OnConnect
	public void onConnect(SocketIOClient client) {
		logger.info("connect!! " + client.getSessionId()
				+ "[" + client.getRemoteAddress().toString() + "]");
	}

	@OnDisconnect
	public void onDisconnect(SocketIOClient client) {
		logger.info("disconnect!! " + client.getSessionId()
				+ "[" + client.getRemoteAddress().toString() + "]");

		Account account = client.get("account");
		if(account != null) {
			emitToHome(client, account.getHome(), "leave", account);
		}
	}

	@OnEvent("join")
	public void onJoin(SocketIOClient client, Account account, AckRequest ack) {
		logger.info(client.getSessionId() + "] join!!");

		client.set("account", account);
		emitToHome(client, account.getHome(), "join", account);
	}

	@OnEvent("leave")
	public void onLeave(SocketIOClient client, Account account, AckRequest ack) {
		logger.info(client.getSessionId() + "] leave!!");

		client.set("account", null);
		emitToHome(client, account.getHome(), "leave", account);
	}


	@OnEvent("action")
	public void onAction(SocketIOClient client, Action action, AckRequest ack) {
		logger.info(client.getSessionId() + "] recv action!!");
		Account account = client.get("account");

		emitToAgent(client, account.getHome(), action.getTarget(), "action", action);
	}

	@OnEvent("result")
	public void onResult(SocketIOClient client, Result result, AckRequest ack) {
		logger.info(client.getSessionId() + "] recv result!!");
		Account account = client.get("account");

		emitToAgent(client, account.getHome(), result.getSource(), "result", result);
	}

	
	private int emitToHome(SocketIOClient me, String home, String event, Object data) {
		int count = 0;
		for(SocketIOClient client: server.getAllClients()) {
			if(client != me) {
				Account account = client.get("account");
				if(account != null && home.equals(account.getHome())) {
					logger.info(" -> emit to " + client.getSessionId());
					client.sendEvent(event, data);
					count++;
				}
			}
		}
		return count;
	}
	
	private int emitToAgent(SocketIOClient me, String home, String agent, String event, Object data) {
		int count = 0;
		for(SocketIOClient client: server.getAllClients()) {
			if(client != me) {
				Account account = client.get("account");
				if(account != null && home.equals(account.getHome()) && agent.equals(account.getAgent())) {
					logger.info(" -> emit to " + client.getSessionId());
					client.sendEvent(event, data);
					count++;
				}
			}
		}
		return count;
	}
}
