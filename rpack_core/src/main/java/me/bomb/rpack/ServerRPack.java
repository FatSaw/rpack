package me.bomb.rpack;

import static me.bomb.rpack.util.NameFilter.filterName;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;

import me.bomb.rpack.resourceserver.EnumStatus;
import me.bomb.rpack.source.PackSource;
import me.bomb.rpack.util.ByteArraysOutputStream;

public final class ServerRPack extends LocalRPack implements Runnable {
	private final InetAddress hostip, remoteip;
	private final int port, backlog, timeout;
	private final ServerSocketFactory connectserverfactory;
	private volatile boolean run;
	private ServerSocket server;

	public ServerRPack(Configuration config, PackSource packsource, PackSender packsender, ConcurrentHashMap<UUID,InetAddress> playerips) {
		super(config, packsource, packsender, playerips);
		this.hostip = config.connectifip;
		this.remoteip = config.connectremoteip;
		this.port = config.connectport;
		this.backlog = config.connectbacklog;
		this.timeout = config.connecttimeout;
		this.connectserverfactory = config.connectserverfactory;
	}
	
	@Override
	public void enable() {
		super.enable();
		this.run = true;
		new Thread(this).start();
	}

	@Override
	public void disable() {
		super.disable();
		run = false;
		if(server==null) return;
		try {
			server.close();
		} catch (IOException e) {
		}
	}
	
	public final byte[] getPacksBytes(byte[] packedb) {
		if(packedb.length != 1) {
			return new byte[0];
		}
		String[] packs = packsource.ids();
		int packcount = packs.length;
		if(packcount > 65535) {
			packcount = 65535;
		}
		int i = packcount;
		int totallengths = 0;
		byte[] lengths = new byte[i];
		byte[][] anames = new byte[i][];
		while(--i > -1) {
			byte[] namebytes = packs[i].getBytes(StandardCharsets.UTF_8);
			int length = namebytes.length;
			if(length > 0xFF) {
				length = 0xFF;
				byte[] nnamebytes = new byte[0xFF];
				System.arraycopy(namebytes, 0, nnamebytes, 0, length);
				namebytes = nnamebytes;
			}
			lengths[i] = (byte) length;
			anames[i] = namebytes;
			totallengths += length;
		}
		i = packcount;
		byte[] response = new byte[2 + i + totallengths];
		response[0] = (byte)i;
		response[1] = (byte) (i>>>8);
		System.arraycopy(lengths, 0, response, 2, i);
		int pos = 2 + i;
		while(--i > -1) {
			byte[] name = anames[i];
			int length = name.length;
			System.arraycopy(name, 0, response, pos, length);
			pos+=length;
		}
		return response;
	}
	
	public final byte[] loadPackBytes(byte[] playeruuidnameupdatestatusb) {
		if(playeruuidnameupdatestatusb.length < 4) {
			return new byte[0];
		}
		int namesize = playeruuidnameupdatestatusb[0] & 0xFF, targetcount = (playeruuidnameupdatestatusb[1] & 0xFF | playeruuidnameupdatestatusb[2]<<8), flags = playeruuidnameupdatestatusb[3];
		
		final boolean update = (flags & 0x01) == 0x01, reportstatus = (flags & 0x02) == 0x02;
		
		int i = (targetcount << 4) + namesize + 4;
		if(playeruuidnameupdatestatusb.length != i) {
			return new byte[0];
		}

		byte[] nameb = new byte[namesize];
		System.arraycopy(playeruuidnameupdatestatusb, 4, nameb, 0, namesize);
		String id = new String(nameb, StandardCharsets.UTF_8);
		String filteredId = filterName(id);
		UUID[] playeruuids = targetcount == 0 ? null : new UUID[targetcount];
		while(--targetcount > -1) {
			long lsb = 0L, msb = 0L;
			lsb = playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			lsb<<=8;
			lsb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb = playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			msb<<=8;
			msb |= playeruuidnameupdatestatusb[--i] & 0xFF;
			final UUID playeruuid = new UUID(msb, lsb);
			playeruuids[targetcount] = playeruuid;
		}
		if(reportstatus) {
			byte[] statusb = new byte[1];
			Consumer<EnumStatus> statusreport = new Consumer<EnumStatus>() {

				@Override
				public void accept(EnumStatus status) {
					switch(status) {
					case DISPATCHED : 
						statusb[0] = 1;
					break;
					case NOTEXSIST : 
						statusb[0] = 2;
					break;
					case PACKED : 
						statusb[0] = 3;
					break;
					case REMOVED : 
						statusb[0] = 4;
					break;
					case UNAVILABLE : 
						statusb[0] = 5;
					break;
					}
				}
				
			};
			byte[] resource;
			if(update) {
				resource = packsource.get(filteredId);
			} else {
				resource = resourcemanager.getCached(filteredId);
				if(resource == null) {
					resource = packsource.get(filteredId);
				}
			}
			if(resource == null) {
				statusreport.accept(update && resourcemanager.removeCache(filteredId) ? EnumStatus.REMOVED : EnumStatus.NOTEXSIST);
			} else {
				resourcemanager.putCache(filteredId, resource);
				if(playeruuids != null) {
					dispatcher.dispatch(filteredId, playeruuids, resource);
					statusreport.accept(EnumStatus.DISPATCHED);
				} else {
					statusreport.accept(update ? EnumStatus.PACKED : EnumStatus.UNAVILABLE);
				}
			}
			return statusb;
		}
		
		byte[] resource;
		if(update) {
			resource = packsource.get(filteredId);
		} else {
			resource = resourcemanager.getCached(filteredId);
			if(resource == null) {
				resource = packsource.get(filteredId);
			}
		}
		if(resource == null) {
			resourcemanager.removeCache(filteredId);
		} else {
			resourcemanager.putCache(filteredId, resource);
			dispatcher.dispatch(filteredId, playeruuids, resource);
		}
		return new byte[0];
	}
	
	@Override
	public void run() {
		while (run) {
			try {
				server = connectserverfactory.createServerSocket(port, backlog, hostip);
				server.setSoTimeout(timeout);
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			while (!server.isClosed()) {
				Socket connected = null;
				try {
					connected = server.accept();
					InetAddress connectedaddress = connected.getInetAddress();
					if (this.remoteip == null || this.remoteip.equals(connectedaddress)) {
						final Socket fconnected = connected;
						new Thread() {
							@Override
							public void run() {
								try {
									processConnection(fconnected);
									fconnected.close();
								} catch(SocketException | SSLException e) {
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}.start();
					}
				} catch (SocketTimeoutException e) {
					continue;
				} catch (SocketException e) {
					break;
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
				}
			}
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private final void processConnection(Socket connected) throws IOException {
		InputStream is = connected.getInputStream();
		byte[] ibuf = new byte[9];
		final byte packetid;
		if(is.read(ibuf) != 9 || ibuf[0] != 'a' || ibuf[1] != 'm' || ibuf[2] != 'r' || ibuf[3] != 'a' || ibuf[4] != 0 || ibuf[7] != 0 || (packetid = ibuf[8]) < 0 || packetid > 0x13 ) {
			return;
		}
		ibuf = null;
		
		if(packetid == 0x07 || packetid == 0x13) {
			ibuf = new byte[0x11];
			is.read(ibuf);
		} else if(packetid == 0x04 || packetid == 0x06 || packetid == 0x08 || packetid == 0x09 || packetid == 0x0A || packetid == 0x0C || packetid == 0x0D || packetid == 0x0E) {
			ibuf = new byte[0x10];
			is.read(ibuf);
		} else if(packetid == 0x02) {
			ibuf = new byte[0x01];
			is.read(ibuf);
		} else if(packetid != 0x12) {
			ibuf = new byte[4];
			is.read(ibuf);
			int length = (0xFF & ibuf[3]) << 24 | (0xFF & ibuf[2]) << 16 | (0xFF & ibuf[1]) << 8 | 0xFF & ibuf[0];
			ibuf = new byte[length];
			if(length != is.read(ibuf)) {
				return;
			}
		}
		ByteArraysOutputStream baos = new ByteArraysOutputStream(2);
		byte[] sizeb,obuf = null;
		int size = 0;
		switch(packetid) {
		case 0x02:
			obuf = this.getPacksBytes(ibuf);
			size = obuf.length;
			sizeb = new byte[4];
			sizeb[0] = (byte)size;
			size>>>=8;
			sizeb[1] = (byte)size;
			size>>>=8;
			sizeb[2] = (byte)size;
			size>>>=8;
			sizeb[3] = (byte)size;
			baos.write(sizeb);
			baos.write(obuf);
		break;
		case 0x0B:
			baos.write(this.loadPackBytes(ibuf));
		break;
		}
		baos.writeTo(connected.getOutputStream());
		baos.close();
	}

}
