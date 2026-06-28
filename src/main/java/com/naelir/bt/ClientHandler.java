package com.naelir.bt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naelir.bt.messages.ChokeMessage;
import com.naelir.bt.messages.HandshakeMessage;
import com.naelir.bt.messages.HaveNone;
import com.naelir.bt.messages.NotInterestedMessage;
import com.naelir.bt.messages.PeerWireMessage;
import com.naelir.bt.messages.PortMessage;
import com.naelir.bt.messages.ext.ExtendedMessageHandshake;
import com.naelir.bt.messages.ext.TorrentMetadataResponse;
import com.naelir.bt.messages.ext.UtMetadataRequest;
import com.naelir.dht.BDecoder;
import com.naelir.dht.Data;
import com.naelir.dht.Generator;
import com.naelir.dht.MetaTorrentTask;
import com.naelir.dht.Node;
import com.naelir.http.RemoteClient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    public static final Logger logger = LogManager.getLogger(ClientHandler.class);
    public static final Path CACHE_FILE = Paths.get(System.getProperty("user.home"), "torrents.info");
    public static final Path CRAP_FILE = Paths.get(System.getProperty("user.home"), "torrents.CRAP");
    private static final List<String> ALLOWED_PRE = List.of("-BC", "-DE", "-LT", "-lt", "-qB", "-TR", "-UT", "-BT",
            "TIX");
    private static final List<String> DENIED_PRE = List.of("-XT");
    
    private String myself;
    private Torrent task;
    // private int serverPort;
    private Data data;
    byte[] metadata;
    volatile int piecesExpected;
    volatile int piecesReceived;
    volatile private byte ut_metadata_code;
    private String peerID;

    public ClientHandler(Data data, Torrent task) {
        this.data = data;
        this.myself = data.getTcpmyself();
//        this.serverPort = serverPort;
        this.task = Objects.requireNonNull(task);
        this.metadata = new byte[0];
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        byte[] hex = Generator.toArray(this.task.infoHash);
        channel.writeAndFlush(new HandshakeMessage(hex, this.myself));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        int port = remoteAddress.getPort();
        InetAddress address = remoteAddress.getAddress();
        byte[] addr = address.getAddress();
        logger.debug("channel {} closed", channel);
        if (this.piecesReceived > 0 && this.piecesReceived < this.piecesExpected) {
            decode(false, addr, port);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("received {}, to string {}", msg.getClass().getSimpleName(), msg);
        Channel channel = ctx.channel();
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        int port = remoteAddress.getPort();
        InetAddress address = remoteAddress.getAddress();
        byte[] addr = address.getAddress();
        if (msg instanceof HandshakeMessage hr) {
            logger.debug("peer {} responded, supports meta {}", hr.peerID, hr.isExtended());
            if (hr.isExtended() == false) {
                ctx.close();
            }
            this.peerID = hr.peerID;
            if (hr.peerID != null && DENIED_PRE.contains(hr.peerID.substring(0, 3))) {
                this.task.setMeta(TorrentMeta.SCAM);
                this.data.fm.saveMeta(this.task.infoHash, TorrentMeta.SCAM);
                logger.error("deny id {}", hr.peerID.substring(0, 3));
//                this.data.denied.add(address);
//                this.data.pingTasks.forEach(e -> e.getNodes().removeIf(f -> address.equals(f.address())));
                ctx.close();
            }
        } else if (msg instanceof ExtendedMessageHandshake eh) {
            channel.write(new ExtendedMessageHandshake(addr));
            channel.flush();
            logger.debug("peer {} responded: {}", eh.version, eh);
            if (eh.metadata_size == 0) {
                logger.warn("will close peer {} metadata size {}", eh.version, eh.metadata_size);
                ctx.close();
            }
            this.metadata = new byte[eh.metadata_size];
            this.piecesExpected = eh.metadata_size / TorrentMetadataResponse.METADATA_PIECE_SIZE + 1;
            this.ut_metadata_code = (byte) eh.ut_metadata;

            for (int i = 0; i < this.piecesExpected; i++) {
                channel.write(new UtMetadataRequest(0, this.ut_metadata_code, i));
            }
            channel.flush();
            
            channel.write(new HaveNone());
            channel.flush();
        } else if (msg instanceof TorrentMetadataResponse r) {
            if (r.msg.type == 2) {
                logger.warn("will close channel {} receiving error {}", channel, r.msg.type);
                ctx.close();
                return;
            }
            System.arraycopy(r.meta, 0, this.metadata, r.msg.piece * TorrentMetadataResponse.METADATA_PIECE_SIZE,
                    r.meta.length);
            this.piecesReceived++;
            logger.debug("piece {} received from expected {}", this.piecesReceived, this.piecesExpected);
            if (this.piecesReceived == this.piecesExpected) {
                decode(true, addr, port);
                ctx.close();
            }
        } else if (msg instanceof PortMessage pm) {//
        } else if (msg instanceof PeerWireMessage ud) {
            //
        }
    }

    void decode(boolean complete, byte[] addr, int port) {
        Optional<BencodedDictionary> decode = complete ? BDecoder.decode(this.metadata)
                : TorrentMeta.parse(this.metadata);
        Optional<TorrentMeta> torrentMeta = TorrentMeta.of(decode);
        if (torrentMeta.isPresent()) {
            TorrentMeta meta = torrentMeta.get();
            logger.warn(meta);
            this.data.fm.saveMeta(this.task.infoHash, meta);
            this.task.setMeta(TorrentMeta.RESOLVED);
            this.data.remoteClient.saveMeta(this.task.infoHash, meta);
        } else {
            logger.error("metadata seems invalid");
            logger.debug(Arrays.toString(this.metadata));
//            this.data.tasks.offer(new MetaTorrentTask(new Node(addr, port), this.task));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause);
        ctx.close();
    }

    private boolean islt(String peer) {
        return peer != null && (peer.startsWith("-lt") || peer.startsWith("-LT"));
    }

}
