package com.naelir.utp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests mirroring the Perl test suite at t/basics.t.
 *
 * <pre>
 * basics.t subtest "Handshake"     → HandshakeTest
 * basics.t subtest "Data Exchange" → DataExchangeTest
 * </pre>
 */
class UTPTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Create a client/server pair with the same conn-id cross-wiring as basics.t. */
    private static UTP[] pair() {
        UTP client = new UTP(100, 101);
        UTP server = new UTP(101, 100);
        return new UTP[]{ client, server };
    }

    // ── Handshake ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Handshake")
    class HandshakeTest {

        @Test
        @DisplayName("Client generates SYN and reaches SYN_SENT")
        void clientGeneratesSyn() {
            UTP[] p   = pair();
            byte[] syn = p[0].connect();

            assertNotNull(syn, "Client generated SYN");
            assertTrue(syn.length >= 20, "SYN is at least one header long");
            assertEquals("SYN_SENT", p[0].getState(), "Client state is SYN_SENT");
        }

        @Test
        @DisplayName("Server receives SYN, replies with STATE, reaches CONNECTED")
        void serverReceivesSyn() {
            UTP[] p    = pair();
            byte[] syn = p[0].connect();
            byte[] ack = p[1].receivePacket(syn);

            assertNotNull(ack, "Server generated STATE (ACK)");
            assertEquals("CONNECTED", p[1].getState(), "Server state is CONNECTED");
        }

        @Test
        @DisplayName("Client receives STATE and reaches CONNECTED")
        void clientReceivesState() {
            UTP[] p    = pair();
            byte[] syn = p[0].connect();
            byte[] ack = p[1].receivePacket(syn);
            p[0].receivePacket(ack);

            assertEquals("CONNECTED", p[0].getState(), "Client state is CONNECTED");
        }

        @Test
        @DisplayName("SYN packet carries correct type byte")
        void synTypeField() {
            UTP    client = new UTP(100, 101);
            byte[] syn    = client.connect();
            UTP.Header h  = UTP.unpackHeader(syn);

            assertNotNull(h);
            assertEquals(UTP.ST_SYN, h.type,    "type == ST_SYN");
            assertEquals(UTP.VERSION, h.version, "version == 1");
        }
    }

    // ── Data Exchange ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Data Exchange")
    class DataExchangeTest {

        /** Shortcut: fully connect a pair. */
        private UTP[] connected() {
            UTP[] p = pair();
            // Server receives SYN and becomes CONNECTED
            p[1].receivePacket(p[0].connect());
            // Client receives a bare ST_STATE to advance to CONNECTED
            // (mirrors "$client->receive_packet( $server->pack_header(2) )" in basics.t)
            p[0].receivePacket(p[1].packHeader(UTP.ST_STATE));
            return p;
        }

        @Test
        @DisplayName("send_data returns non-null packet(s)")
        void sendDataReturnsPacket() {
            UTP[] p    = connected();
            byte[] pkt = p[0].sendData("Hello Standalone μTP".getBytes());

            assertNotNull(pkt, "sendData returned data");
            assertTrue(pkt.length > 0, "Packet is non-empty");
        }

        @Test
        @DisplayName("Server receives correct payload via 'data' event")
        void serverReceivesCorrectData() {
            UTP[] p = connected();

            AtomicReference<byte[]> received = new AtomicReference<>(new byte[0]);
            p[1].on("data", args -> {
                byte[] chunk  = (byte[]) args[0];
                byte[] prev   = received.get();
                byte[] merged = new byte[prev.length + chunk.length];
                System.arraycopy(prev,  0, merged, 0,           prev.length);
                System.arraycopy(chunk, 0, merged, prev.length, chunk.length);
                received.set(merged);
            });

            byte[] msg = "Hello Standalone μTP".getBytes();
            byte[] pkt = p[0].sendData(msg);

            assertNotNull(pkt);
            assertNotNull(p[1].receivePacket(pkt), "Server replies with STATE");
            assertArrayEquals(msg, received.get(), "Server received the exact bytes sent");
        }

        @Test
        @DisplayName("sendData returns null when not CONNECTED")
        void sendDataNullWhenNotConnected() {
            UTP client = new UTP(100, 101);
            assertNull(client.sendData("data".getBytes()), "Must be null before handshake");
        }

        @Test
        @DisplayName("sendData returns null for null or empty input")
        void sendDataNullForEmptyInput() {
            UTP[] p = connected();
            assertNull(p[0].sendData(null),         "null data → null");
            assertNull(p[0].sendData(new byte[0]),  "empty data → null");
        }

        @Test
        @DisplayName("DATA packet carries correct type byte")
        void dataTypeField() {
            UTP[] p    = connected();
            byte[] pkt = p[0].sendData("test".getBytes());
            assertNotNull(pkt);
            UTP.Header h = UTP.unpackHeader(pkt);
            assertNotNull(h);
            assertEquals(UTP.ST_DATA, h.type, "type == ST_DATA");
        }
    }
}
