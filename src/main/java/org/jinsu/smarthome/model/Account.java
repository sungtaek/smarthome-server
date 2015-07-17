package org.jinsu.smarthome.model;

public class Account {
	private String home;
	private String agent;

	public String getHome() {
		return home;
	}
	public void setHome(String home) {
		this.home = home;
	}
	public String getAgent() {
		return agent;
	}
	public void setAgent(String agent) {
		this.agent = agent;
	}

    @Override
    public boolean equals(Object o) {
        if(o==this) {
            return true;
        }
        if(!(o instanceof Account)) {
            return false;
        }

        Account c = (Account) o;

        return home.equals(c.home) && agent.equals(c.agent);
    }
}
