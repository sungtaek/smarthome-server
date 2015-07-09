package org.jinsu.smarthome;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class Server {
	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private SocketIOServer server = null;

	public Server(String host, Integer port) {
		logger.info("create Server[" + host + ":" + port + "]...");

		Configuration config = new Configuration();
		config.setHostname(host);
		config.setPort(port);
		config.getSocketConfig().setReuseAddress(true);
		this.server = new SocketIOServer(config);
		ServiceListener listener = new ServiceListener(server);
		server.addListeners(listener);
	}
	
	public void start() {
		logger.info("start Server!");
		server.start();
	}
	
	public void stop() {
		logger.info("stop Server!");
		server.stop();
	}

	public static void main(String[] args) {
		Server srv = new Server(args[0], Integer.parseInt(args[1]));
		srv.start();
	}
}
