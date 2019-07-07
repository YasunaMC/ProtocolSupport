package protocolsupport.zplatform;

import org.spigotmc.SpigotConfig;

import net.minecraft.server.v1_13_R2.NetworkManager;
import protocolsupport.api.ServerPlatformIdentifier;
import protocolsupport.zplatform.impl.spigot.SpigotMiscUtils;
import protocolsupport.zplatform.impl.spigot.SpigotPacketFactory;

public class ServerPlatform {

	private static ServerPlatform current;

	public static boolean detect() {
		if (current != null) {
			throw new IllegalStateException("Implementation already detected");
		}
		try {
			NetworkManager.class.getDeclaredFields();
			SpigotConfig.class.getDeclaredFields();
			current = new ServerPlatform(ServerPlatformIdentifier.SPIGOT, new SpigotMiscUtils(), new SpigotPacketFactory());
		} catch (Throwable t) {
		}
		try {
//TODO: update to glowstone 1.13 when it becomes available
//			GlowServer.class.getDeclaredFields();
//			current = new ServerPlatform(ServerPlatformIdentifier.GLOWSTONE, new GlowstonePlatformInjector(), new GlowStoneMiscUtils(), new GlowStonePacketFactory(), new GlowStoneWrapperFactory());
		} catch (Throwable t) {
		}
		return current != null;
	}

	public static ServerPlatform get() {
		if (current == null) {
			throw new IllegalStateException("Access to implementation before detect");
		}
		return current;
	}

	private final ServerPlatformIdentifier identifier;
	private final PlatformUtils utils;
	private final PlatformPacketFactory packetfactory;
	private ServerPlatform(ServerPlatformIdentifier identifier, PlatformUtils miscutils, PlatformPacketFactory packetfactory) {
		this.identifier = identifier;
		this.utils = miscutils;
		this.packetfactory = packetfactory;
	}

	public ServerPlatformIdentifier getIdentifier() {
		return identifier;
	}

	public PlatformUtils getMiscUtils() {
		return utils;
	}

	public PlatformPacketFactory getPacketFactory() {
		return packetfactory;
	}

}
