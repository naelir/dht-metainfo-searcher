package com.naelir.tracker;

import java.util.Map;
import java.util.Set;

import com.naelir.dht.Generator;
import com.naelir.tasks.Sample;

/**
 * A {@link TrackerConnection} that walks through {@link #allHashes} one at a
 * time, issuing a BEP-15 announce request for each info-hash in turn.
 *
 * <p>Each call to {@link #buildNextRequest()} advances an internal cursor and
 * wraps back to the start of the list once the end is reached, so the
 * connection can keep announcing indefinitely as long as it stays alive.
 */
public class AnnounceTrackerConnection extends TrackerConnection {

    private static final short DEFAULT_PORT = 6881;
    private static final int DEFAULT_NUM_WANT = 10;

    /** 20-byte peer id used for every announce issued by this connection. */
    private final byte[] peerId;
    private String currentHash;
    private Map<String, Sample> samples;

    public AnnounceTrackerConnection(String hostAddress, int port, Set<String> hashes, byte[] peerId, Map<String, Sample> samples) {
        super(hostAddress, port, hashes);
        this.peerId = peerId;
        this.samples = samples;
    }

    @Override
    protected boolean hasMoreBatches() {
        return allHashes.isEmpty() == false;
    }
    /**
     * Builds an announce request for the next info-hash in {@link #allHashes},
     * advancing (and wrapping) the internal cursor.
     *
     * @return the encoded announce request, or an empty array if there are no hashes to announce
     */
    @Override
    protected byte[] buildNextRequest() {
        if (allHashes.isEmpty()) {
            return new byte[0];
        }

        currentHash = allHashes.remove(0);

        Sample sample = this.samples.get(currentHash);
        if (sample == null) {
            TrackerUdpManager.logger.error("{}, all {}", samples.keySet(), allHashes);
            return new byte[0];

        }
        sample.check();
        
        byte[] infoHash = Generator.toArray(currentHash);

        AnnounceRequest request = new AnnounceRequest(
                connectionId,
                transactionId,
                infoHash,
                peerId,
                0L,
                0L,
                0L,
                AnnounceRequest.Event.NONE,
                0,
                0,
                DEFAULT_NUM_WANT,
                DEFAULT_PORT);

        return request.encode();
    }
    
    public String getCurrentHash() {
        return currentHash;
    }
}
