package org.tastefuljava.flipit;

public class DeviceRef {
    private final String name;
    private final String address;

    public DeviceRef(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
