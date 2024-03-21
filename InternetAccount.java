package user;

public class InternetAccount extends Account {

private String domain;

public InternetAccount(String domain, String username, String password) {super(username, password);
      this.domain = domain;
    }

public String getDomain() {
      return domain;
    }

  
public void changeDomain(String domain) { this.domain = domain; }
}
