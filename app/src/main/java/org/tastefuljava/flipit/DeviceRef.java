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

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        DeviceRef other = (DeviceRef) obj;
        return address.equals(other.address);
    }

    @Override
    public String toString() {
        return name + "(" + address + ")";
    }
}
