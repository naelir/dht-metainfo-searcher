package com.naelir.bt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cdefgah.bencoder4j.model.BencodedDictionary;
import com.naelir.bt.messages.HandshakeMessage;
import com.naelir.bt.messages.HaveNone;
import com.naelir.bt.messages.PeerWireMessage;
import com.naelir.bt.messages.PortMessage;
import com.naelir.bt.messages.ext.ExtendedMessageHandshake;
import com.naelir.bt.messages.ext.TorrentMetadataResponse;
import com.naelir.bt.messages.ext.UtMetadataRequest;
import com.naelir.dht.BDecoder;
import com.naelir.dht.Data;
import com.naelir.dht.Generator;
import com.naelir.fs.FileRecord;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    public static final Logger logger = LogManager.getLogger(ClientHandler.class);
    public static final Path CACHE_FILE = Paths.get(System.getProperty("user.home"), "torrents.info");
    public static final Path CRAP_FILE = Paths.get(System.getProperty("user.home"), "torrents.CRAP");
    private static final List<String> DENIED_PRE = List.of("-XT");
    private String myself;
    private Torrent task;
    private Data data;
    byte[] metadata;
    volatile int piecesExpected;
    volatile int piecesReceived;
    volatile private byte ut_metadata_code;

    public ClientHandler(Data data, Torrent task) {
        this.data = data;
        this.myself = data.getTcpmyself();
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
        logger.debug("received {}", msg.getClass().getSimpleName());
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
            if (hr.peerID != null && DENIED_PRE.contains(hr.peerID.substring(0, 3))) {
                TorrentMeta meta = new TorrentMeta("CRAP");
                this.task.setMeta(meta);
                this.data.fm.create(new FileRecord(this.task.infoHash, "CRAP"));
                logger.error("deny id {}", hr.peerID.substring(0, 3));
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
        } else if (msg instanceof PortMessage) {//
        } else if (msg instanceof PeerWireMessage) {//
        }
    }

    void decode(boolean complete, byte[] addr, int port) {
        Optional<BencodedDictionary> decode = complete ? BDecoder.decode(this.metadata)
                : TorrentMeta.parse(this.metadata);
        Optional<TorrentMeta> torrentMeta = TorrentMeta.of(this.task.infoHash, decode);
        if (torrentMeta.isPresent()) {
            TorrentMeta meta = torrentMeta.get();
            logger.info("resolved {}", meta.getName());
            if (this.task.meta() == null) {
                this.task.setMeta(meta);
                this.data.fm.saveMeta(this.task.infoHash, meta);
                this.data.fm.create(new FileRecord(this.task.infoHash, meta.getName(), meta));
                this.data.repo.insert(TorrentMeta.toEntry(this.task.infoHash, meta));
            }
        } else {
            logger.error("metadata was invalid");
            if (logger.isDebugEnabled()) {
                logger.debug(Arrays.toString(this.metadata));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause);
        ctx.close();
    }
}
