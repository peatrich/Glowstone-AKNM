package net.glowstone.net.handler.handshake;

import com.flowpowered.networking.MessageHandler;
import net.glowstone.GlowServer;
import net.glowstone.entity.meta.PlayerProperty;
import net.glowstone.net.GlowSession;
import net.glowstone.net.message.handshake.HandshakeMessage;
import net.glowstone.net.protocol.ProtocolType;
import net.glowstone.util.UuidUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

        // BungeeCord modifies the hostname in the HandshakeMessage to contain the clients UUID and (optionally) properties
        String[] parts = message.getAddress().split("\00");
        if (parts.length == 3 || parts.length == 4) {
            if (parts.length == 4) {
                try {
                    // Spoof properties
                    JSONArray jsonProperties = (JSONArray) new JSONParser().parse(parts[3]);

                    final List<PlayerProperty> properties = new ArrayList<>(jsonProperties.size());
                    for (Object obj : jsonProperties) {
                        JSONObject propJson = (JSONObject) obj;
                        String propName = (String) propJson.get("name");
                        String value = (String) propJson.get("value");
                        String signature = (String) propJson.get("signature");
                        properties.add(new PlayerProperty(propName, value, signature));
                    }
                    session.setSpoofedProperties(properties);
                } catch (ParseException | ClassCastException e) {
                    GlowServer.logger.log(Level.SEVERE, "Failed parsing client properties: {0}", e);
                    session.disconnect("Failed reading properties.");
                    return;
                }
            }

            // todo: There is currently no use for the hostname the client connected to
            // because Glowstone uses a deprecated constructor to instantiate the PlayerLoginEvent.
            // Once the hostname is used, this value should replace it if the user connects through BungeeCord
            //String spoofedHostname = parts[0];

            // Spoof client address
            InetSocketAddress spoofedAddress = new InetSocketAddress(parts[1], session.getAddress().getPort());
            session.setAddress(spoofedAddress);

            // Spoof UUID
            UUID spoofedUuid = UuidUtils.fromFlatString(parts[2]);
            session.setSpoofedUUID(spoofedUuid);
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
