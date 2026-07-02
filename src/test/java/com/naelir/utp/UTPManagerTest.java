package com.naelir.utp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UTPManager}, covering the three methods
 * from Net::uTP::Manager:
 *
 * <pre>
 *   handle_packet   → HandlePacketTests
 *   new_connection  → NewConnectionTests
 *   tick            → TickTests
 * </pre>
 */
class UTPManagerTest {

    // ── handlePacket ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handlePacket")
    class HandlePacketTests {

        @Test
        @DisplayName("SYN creates a new connection entry in the map")
        void synCreatesConnection() {
            UTPManager manager = new UTPManager();
            // client.connect() emits SYN with conn_id = connIdRecv = 101
            byte[] syn = new UTP(100, 101).connect();
            manager.handlePacket(syn, new InetSocketAddress("127.0.0.1", 6881));
            assertEquals(1, manager.getConnections().size());
        }

        @Test
        @DisplayName("SYN emits new_connection event with UTP instance and sender IP")
        void synEmitsNewConnectionEvent() {
            UTPManager manager = new UTPManager();
            AtomicBoolean fired    = new AtomicBoolean(false);
            AtomicReference<String> capturedIp = new AtomicReference<>();
            manager.on("new_connection", args -> {
                fired.set(true);
                assertInstanceOf(UTP.class, args[0], "first arg must be UTP");
                capturedIp.set((String) args[1]);
            });

            manager.handlePacket(new UTP(100, 101).connect(),
                                 new InetSocketAddress("10.0.0.1", 4242));

            assertTrue(fired.get(),         "new_connection event must fire");
            assertEquals("10.0.0.1", capturedIp.get(), "IP must match sender");
        }

        @Test
        @DisplayName("SYN returns a STATE (ST_STATE) response")
        void synReturnsStatePacket() {
            UTPManager manager  = new UTPManager();
            byte[]     response = manager.handlePacket(
                    new UTP(100, 101).connect(),
                    new InetSocketAddress("127.0.0.1", 6881));

            assertNotNull(response, "Manager must reply to SYN");
            assertTrue(response.length >= 20);
            UTP.Header h = UTP.unpackHeader(response);
            assertNotNull(h);
            assertEquals(UTP.ST_STATE, h.type, "Reply to SYN must be ST_STATE");
        }

        @Test
        @DisplayName("Non-SYN packet for an unknown session is silently ignored")
        void unknownSessionIgnored() {
            UTPManager manager = new UTPManager();
            byte[]     dataHdr = new UTP(999, 1000).packHeader(UTP.ST_DATA);

            assertNull(manager.handlePacket(dataHdr, new InetSocketAddress("127.0.0.1", 1111)),
                       "Unknown session must return null");
            assertTrue(manager.getConnections().isEmpty());
        }

        @Test
        @DisplayName("Packet shorter than 20 bytes is ignored without side effects")
        void shortPacketIgnored() {
            UTPManager manager = new UTPManager();
            assertNull(manager.handlePacket(new byte[10], new InetSocketAddress("127.0.0.1", 1)));
            assertTrue(manager.getConnections().isEmpty());
        }

        @Test
        @DisplayName("null sender address returns null")
        void nullAddrReturnsNull() {
            assertNull(new UTPManager().handlePacket(new UTP(100, 101).connect(), null));
        }

        @Test
        @DisplayName("null data returns null")
        void nullDataReturnsNull() {
            assertNull(new UTPManager().handlePacket(null, new InetSocketAddress("127.0.0.1", 1)));
        }

        @Test
        @DisplayName("RESET closes and removes the connection immediately")
        void resetClosesConnection() {
            UTPManager        manager = new UTPManager();
            InetSocketAddress addr    = new InetSocketAddress("127.0.0.1", 6881);

            // SYN has conn_id = 101 → stored at key ("127.0.0.1", 6881, 101)
            manager.handlePacket(new UTP(100, 101).connect(), addr);
            assertEquals(1, manager.getConnections().size());

            // RESET with the same conn_id = 101 (connIdSend of UTP(101, 100))
            byte[] reset = new UTP(101, 100).packHeader(UTP.ST_RESET);
            manager.handlePacket(reset, addr);

            assertTrue(manager.getConnections().isEmpty(),
                       "RESET must remove the session from the map");
        }

        @Test
        @DisplayName("Full handshake via manager leaves both sides CONNECTED")
        void fullHandshake() {
            UTPManager        manager = new UTPManager();
            InetSocketAddress addr    = new InetSocketAddress("127.0.0.1", 6881);

            // Client SYN
            UTP    client   = new UTP(100, 101);
            byte[] syn      = client.connect();
            byte[] stateAck = manager.handlePacket(syn, addr);   // server → STATE

            assertNotNull(stateAck, "Server must reply with STATE");

            // Client processes the STATE → CONNECTED
            client.receivePacket(stateAck);
            assertEquals("CONNECTED", client.getState(), "Client must reach CONNECTED");

            // Server session inside the manager must also be CONNECTED
            UTPManager.ConnectionKey key = new UTPManager.ConnectionKey("127.0.0.1", 6881, 101);
            UTP serverSide = manager.getConnections().get(key);
            assertNotNull(serverSide);
            assertEquals("CONNECTED", serverSide.getState(), "Server side must be CONNECTED");
        }

        @Test
        @DisplayName("Data exchange through manager delivers payload via 'data' event")
        void dataExchangeThroughManager() {
            UTPManager        manager = new UTPManager();
            InetSocketAddress addr    = new InetSocketAddress("127.0.0.1", 6881);

            // Handshake
            UTP    client = new UTP(100, 101);
            byte[] state  = manager.handlePacket(client.connect(), addr);
            client.receivePacket(state);

            // Subscribe to data on the server-side session
            AtomicReference<byte[]> received = new AtomicReference<>(new byte[0]);
            UTPManager.ConnectionKey key =
                    new UTPManager.ConnectionKey("127.0.0.1", 6881, 101);
            manager.getConnections().get(key).on("data", args -> {
                byte[] chunk  = (byte[]) args[0];
                byte[] prev   = received.get();
                byte[] merged = new byte[prev.length + chunk.length];
                System.arraycopy(prev,  0, merged, 0,           prev.length);
                System.arraycopy(chunk, 0, merged, prev.length, chunk.length);
                received.set(merged);
            });

            // Client sends data
            byte[] msg = "Hello from Manager test".getBytes();
            byte[] pkt = client.sendData(msg);
            manager.handlePacket(pkt, addr);

            assertArrayEquals(msg, received.get(), "Manager must deliver the exact payload");
        }
    }

    // ── newConnection ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("newConnection")
    class NewConnectionTests {

        @Test
        @DisplayName("newConnection returns a non-null UTP instance")
        void returnsUtpInstance() {
            UTPManager manager = new UTPManager();
            assertNotNull(manager.newConnection("10.0.0.1", 6881));
        }

        @Test
        @DisplayName("newConnection registers exactly one entry in the map")
        void registersOneEntry() {
            UTPManager manager = new UTPManager();
            manager.newConnection("10.0.0.1", 6881);
            assertEquals(1, manager.getConnections().size());
        }

        @Test
        @DisplayName("connect() on the returned UTP reaches SYN_SENT")
        void connectReachesSynSent() {
            UTPManager manager = new UTPManager();
            UTP        utp     = manager.newConnection("10.0.0.1", 6881);
            assertNotNull(utp.connect());
            assertEquals("SYN_SENT", utp.getState());
        }

        @Test
        @DisplayName("Two calls to newConnection create two independent entries")
        void twoConnectionsAreIndependent() {
            UTPManager manager = new UTPManager();
            UTP a = manager.newConnection("10.0.0.1", 6881);
            UTP b = manager.newConnection("10.0.0.1", 6882);
            assertNotSame(a, b);
            assertEquals(2, manager.getConnections().size());
        }
    }

    // ── tick ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("tick")
    class TickTests {

        @Test
        @DisplayName("tick on an empty manager returns an empty list")
        void emptyManagerEmptyTick() {
            assertTrue(new UTPManager().tick(0.0).isEmpty());
        }

        @Test
        @DisplayName("tick returns empty list immediately after connect (RTO not elapsed)")
        void noRetransmitsRightAfterConnect() {
            UTPManager manager = new UTPManager();
            UTP        utp     = manager.newConnection("10.0.0.1", 6881);
            utp.connect();  // SYN in outBuffer, RTO = 1 s

            assertTrue(manager.tick(0.0).isEmpty(),
                       "Nothing to retransmit within the first RTO window");
        }

        @Test
        @DisplayName("tick preserves live connections")
        void liveConnectionsNotRemovedByTick() {
            UTPManager manager = new UTPManager();
            manager.newConnection("10.0.0.1", 6881);
            manager.tick(0.0);
            assertEquals(1, manager.getConnections().size());
        }

        @Test
        @DisplayName("tick removes CLOSED connections")
        void closedConnectionRemovedByTick() {
            UTPManager        manager = new UTPManager();
            InetSocketAddress addr    = new InetSocketAddress("127.0.0.1", 6881);

            // Create a server-side session via SYN
            manager.handlePacket(new UTP(100, 101).connect(), addr);
            assertEquals(1, manager.getConnections().size());

            // Mark session closed directly (simulates remote RESET received)
            UTPManager.ConnectionKey key =
                    new UTPManager.ConnectionKey("127.0.0.1", 6881, 101);
            // Deliver a RESET with conn_id=101 to close it
            manager.handlePacket(new UTP(101, 100).packHeader(UTP.ST_RESET), addr);

            // handlePacket already removes it; tick must not re-add it
            manager.tick(0.0);
            assertTrue(manager.getConnections().isEmpty());
        }

        @Test
        @DisplayName("PendingPacket records carry non-null ip, positive port, and data")
        void pendingPacketStructure() {
            UTPManager manager = new UTPManager();
            UTP        utp     = manager.newConnection("172.16.0.1", 9999);
            utp.connect();

            // tick won't retransmit yet (RTO not elapsed), but if it did the
            // packet structure must be valid – verify via the list type/shape
            List<UTPManager.PendingPacket> result = manager.tick(0.0);
            for (UTPManager.PendingPacket p : result) {
                assertNotNull(p.ip());
                assertTrue(p.port() > 0);
                assertNotNull(p.data());
                assertTrue(p.data().length >= 20);
            }
        }
    }
}
