package cn.lsg.twofactor;


public class Item {

    private Integer id;

    private String name;

    private String user;

    private String secretKey;

    public Item(Integer id, String name, String user, String secretKey) {
        this.id = id;
        this.name = name;
        this.user = user;
        this.secretKey = secretKey;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
