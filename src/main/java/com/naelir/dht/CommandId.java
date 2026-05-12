package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class CommandId {
    byte[] ip;
    int port;
    ByteBuffer tid;

    public CommandId(ByteBuffer tid, byte[] ip, int port) {
        super();
        this.tid = tid;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandId other = (CommandId) obj;
        return Arrays.equals(this.ip, other.ip) && this.port == other.port && Objects.equals(this.tid, other.tid);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.ip);
        result = prime * result + Objects.hash(this.port, this.tid);
        return result;
    }
}
