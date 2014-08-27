package net.glowstone.net.handler.handshake;

import com.flowpowered.networking.MessageHandler;
import net.glowstone.GlowServer;
import net.glowstone.net.BungeeData;
import net.glowstone.net.GlowSession;
import net.glowstone.net.message.handshake.HandshakeMessage;
import net.glowstone.net.protocol.ProtocolType;

import java.util.logging.Level;

public class HandshakeHandler implements MessageHandler<GlowSession, HandshakeMessage> {

    @Override
    public void handle(GlowSession session, HandshakeMessage message) {
        ProtocolType protocol = ProtocolType.getById(message.getState());
        if (protocol != ProtocolType.LOGIN && protocol != ProtocolType.STATUS) {
            session.disconnect("Invalid state");
            return;
        }

        session.setHostname(message.getAddress() + ":" + message.getPort());

        // BungeeCord modifies the hostname in the HandshakeMessage to contain
        // the client's UUID and (optionally) properties
        boolean bungee = session.getServer().getProxyParsing();
        if (bungee) {
            try {
                session.setBungeeData(new BungeeData(session, message.getAddress()));
            } catch (IllegalArgumentException ex) {
                session.disconnect("Invalid proxy data provided.");
                return;
            } catch (Exception ex) {
                GlowServer.logger.log(Level.SEVERE, "Error parsing proxy data for " + session, ex);
                session.disconnect("Failed to parse proxy data.");
                return;
            }
        }

        session.setProtocol(protocol);

        if (protocol == ProtocolType.LOGIN) {
            if (message.getVersion() < GlowServer.PROTOCOL_VERSION) {
                session.disconnect("Outdated client!");
            } else if (message.getVersion() > GlowServer.PROTOCOL_VERSION) {
                session.disconnect("Outdated server!");
            }
        }
    }
}
