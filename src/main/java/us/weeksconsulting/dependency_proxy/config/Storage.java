package us.weeksconsulting.dependency_proxy.config;

public class Storage {
    private final String type;
    private final String location;

    public Storage(String type, String location) {
        this.type = type;
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Storage [type=" + type + ", location=" + location + "]";
    }

}
