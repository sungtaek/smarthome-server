package org.jinsu.smarthome;

import java.util.List;
import java.util.ArrayList;

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

        List<Account> accounts = client.get("accounts");
		if(accounts != null) {
            for(Account account: accounts) {
                emitToHome(client, account.getHome(), "leave", account);
            }
		}
	}

	@OnEvent("join")
	public void onJoin(SocketIOClient client, Account account, AckRequest ack) {
		logger.info(client.getSessionId() + "] join!!");

        releasePrevAccount(client, account);
        addAccount(client, account);
		emitToHome(client, account.getHome(), "join", account);
	}

	@OnEvent("leave")
	public void onLeave(SocketIOClient client, Account account, AckRequest ack) {
		logger.info(client.getSessionId() + "] leave!!");

        removeAccount(client, account);
        emitToHome(client, account.getHome(), "leave", account);
	}


	@OnEvent("action")
	public void onAction(SocketIOClient client, Action action, AckRequest ack) {
		logger.info(client.getSessionId() + "] recv action!!");
        List<Account> accounts = client.get("accounts");
        
        if(accounts != null && accounts.size() > 0) {
            int count = 0;
            count = emitToAgent(client, accounts.get(0).getHome(), action.getTarget(), "action", action);
            if(count <= 0) {
                Result result = new Result();
                result.setSource(action.getSource());
                result.setTarget(action.getTarget());
                result.setCode(403);
                result.setMessage("not found agent");
                client.sendEvent("result", result);
            }
        }
	}

	@OnEvent("result")
	public void onResult(SocketIOClient client, Result result, AckRequest ack) {
		logger.info(client.getSessionId() + "] recv result!!");
		List<Account> accounts = client.get("accounts");

        if(accounts != null && accounts.size() > 0) {
            emitToAgent(client, accounts.get(0).getHome(), result.getSource(), "result", result);
        }
    }


    private int addAccount(SocketIOClient client, Account account) {
        List<Account> accounts = null;
        accounts = client.get("accounts");
        if(accounts == null) {
            accounts = new ArrayList<Account>();
		    client.set("accounts", accounts);
        }
        else {
            for(Account prev: accounts) {
                if(prev == account) {
                    return accounts.size();
                }
            }
        }
        accounts.add(account);
        return accounts.size();
    }

    private int removeAccount(SocketIOClient client, Account account) {
        int i;
        List<Account> accounts = null;
        accounts = client.get("accounts");
        if(accounts != null && accounts.size() > 0) {
            for(i=0; i<accounts.size(); i++) {
                Account prev = accounts.get(i);
                if(prev == account) {
                    accounts.remove(i--);
                }
            }
            return accounts.size();
        }
        return 0;
    }

	private int emitToHome(SocketIOClient me, String home, String event, Object data) {
		int count = 0;
		for(SocketIOClient client: server.getAllClients()) {
			if(client != me) {
				List<Account> accounts = client.get("accounts");
                if(accounts != null && accounts.size() > 0) {
                    for(Account account: accounts) {
                        if(home.equals(account.getHome())) {
                            logger.info(" -> emit to " + client.getSessionId());
                            client.sendEvent(event, data);
                            count++;
                            break;
                        }
                    }
                }
			}
		}
		return count;
	}
	
	private int emitToAgent(SocketIOClient me, String home, String agent, String event, Object data) {
		int count = 0;
		for(SocketIOClient client: server.getAllClients()) {
			if(client != me) {
				List<Account> accounts = client.get("accounts");
                if(accounts != null && accounts.size() > 0) {
                    for(Account account: accounts) {
                        if(home.equals(account.getHome()) && agent.equals(account.getAgent())) {
                            logger.info(" -> emit to " + client.getSessionId());
                            client.sendEvent(event, data);
                            count++;
                            break;
                        }
                    }
                }
			}
		}
		return count;
	}

    private void releasePrevAccount(SocketIOClient me, Account account) {
        for(SocketIOClient client: server.getAllClients()) {
			if(client != me) {
                if(removeAccount(client, account) == 0) {
                    client.disconnect();
                }
			}
		}
    }
}
