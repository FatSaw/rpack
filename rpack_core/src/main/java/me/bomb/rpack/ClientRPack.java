package me.bomb.rpack;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.net.SocketFactory;

import me.bomb.rpack.resourceserver.EnumStatus;
import me.bomb.rpack.util.ByteArraysOutputStream;

public final class ClientRPack extends Thread implements RPack {
	
	private final InetAddress hostip, remoteip;
	private final int port, timeout;
	private final SocketFactory socketfactory;
	private final Executor executor;
	private volatile boolean run;
	
	public ClientRPack(Configuration config) {
		this.hostip = config.connectifip;
		this.remoteip = config.connectremoteip;
		this.port = config.connectport;
		this.timeout = config.connecttimeout;
		this.socketfactory = config.connectsocketfactory;
		this.executor = new ThreadPoolExecutor(1, 2, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024));
	}
	
	private byte[] sendPacket(byte packetid, byte[] buf, boolean sendsize, int responsesize, boolean remotesize) {
		Socket socket = null;
		boolean fail = false;
		try {
			ByteArraysOutputStream baos = new ByteArraysOutputStream(3);
			baos.write(new byte[] {'a','m','r','a', 0, 0, 0, 0, packetid}); //PROTOCOLID
			if(buf!=null) {
				if(sendsize) {
					byte[] sizeb = new byte[4];
					int size = buf.length;
					sizeb[0] = (byte)size;
					size>>=8;
					sizeb[1] = (byte)size;
					size>>=8;
					sizeb[2] = (byte)size;
					size>>=8;
					sizeb[3] = (byte)size;
					baos.write(sizeb);
				}
				baos.write(buf);
				buf = null;
			}
			socket = socketfactory.createSocket(remoteip, port, hostip, 0);
			socket.setSoTimeout(timeout);
			baos.writeTo(socket.getOutputStream());
			baos.close();
			InputStream is = socket.getInputStream();
			if(remotesize) {
				buf = new byte[4];
				is.read(buf);
				responsesize = (0xFF & buf[3]) << 24 | (0xFF & buf[2]) << 16 | (0xFF & buf[1]) << 8 | 0xFF & buf[0];
			}
			if(responsesize > 65535) {
				responsesize = 65535;
			}
			
			if(responsesize > 0) {
				buf = new byte[responsesize];
				is.read(buf);
			} else {
				buf = new byte[0];
			}
		} catch (SocketTimeoutException e) {
			fail = true;
		} catch (IOException e) {
			fail = true;
		} finally {
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		return fail ? new byte[0] : buf;
	}
	
	

	@Override
	public void enable() {
		this.run = true;
		start();
	}
	
	@Override
	public void run() {
		while(run) {
			try {
				sleep(1000L);
			} catch (InterruptedException e) {
				interrupted();
			}
		}
	}

	@Override
	public void disable() {
		this.run = false;
		this.interrupt();
	}
	
	public void login(UUID playeruuid, InetAddress address) {
		
	}
	
	public void logout(UUID playeruuid) {
		
	}
	
	private void addToQueue(Runnable run) {
		executor.execute(run);
	}
	
	private AtomicBoolean cachePacksUpdated = new AtomicBoolean(false);
	private String[] cachePacks = null;

	@Override
	public boolean getPacks(Consumer<String[]> resultConsumer) {
		if(cachePacksUpdated.get()) {
			resultConsumer.accept(cachePacks);
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				byte[] buf = ClientRPack.this.sendPacket((byte)0x02, new byte[] {(byte) 1}, false, 0, true);
				if(buf.length == 0) {
					resultConsumer.accept(null);
					return;
				}
				int count = (0xFF & buf[1]) << 8 | 0xFF & buf[0];
				String[] names = new String[count];
				int i = 2 + count, pos = i;
				while(--count > -1) {
					short namelength = (short) (buf[--i] & 0xFF);
					byte[] nameb = new byte[namelength];
					System.arraycopy(buf, pos, nameb, 0, namelength);
					pos+=namelength;
					names[count] = new String(nameb, StandardCharsets.UTF_8);
				}
				String[] namescache = new String[names.length];
				System.arraycopy(names, 0, namescache, 0, names.length);

				cachePacksUpdated.set(true);
				cachePacks = namescache;
				
				resultConsumer.accept(names);
			}
		};
		addToQueue(r);
		return true;
	}
	
	@Override
	public boolean loadPack(UUID[] playeruuid, String name, boolean update, Consumer<EnumStatus> resultConsumer) {
		if(name == null) {
			return false;
		}
		Runnable r = new Runnable() {
			public void run() {
				byte[] nameb = name.getBytes(StandardCharsets.UTF_8);
				int namelength = nameb.length;
				if(namelength > 0xFF) {
					namelength = 0xFF;
					byte[] nnameb = new byte[0xFF];
					System.arraycopy(nameb, 0, nnameb, 0, namelength);
					nameb = nnameb;
				}
				byte[] buf = new byte[4 + namelength + (playeruuid == null ? 0 : playeruuid.length << 4)];
				byte flags = 0;
				if(update) {
					flags |= 0x01;
				}
				final boolean hasstatusreport = resultConsumer != null;
				if(hasstatusreport) {
					flags |= 0x02;
				}
				buf[0] = (byte) namelength;
				buf[3] = flags;
				System.arraycopy(nameb, 0, buf, 4, namelength);
				if(playeruuid == null) {
					buf[1] = 0;
					buf[2] = 0;
				} else {
					int uuidcount = playeruuid.length;
					buf[1] = (byte) uuidcount;
					buf[2] = (byte) (uuidcount >> 8);
					int j = 3 + namelength;
					while(--uuidcount > -1) {
						UUID uuid = playeruuid[uuidcount];
						long msb = uuid.getMostSignificantBits(), lsb = uuid.getLeastSignificantBits();
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						msb>>=8;
						buf[++j] = (byte) msb;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
						lsb>>=8;
						buf[++j] = (byte) lsb;
					}
					
				}
				if(hasstatusreport) {
					buf = ClientRPack.this.sendPacket((byte)0x0B, buf, true, 1, false);
					byte status = buf[0];
					
					switch (status) {
					case 1:
						resultConsumer.accept(EnumStatus.DISPATCHED);
					break;
					case 2:
						resultConsumer.accept(EnumStatus.NOTEXSIST);
					break;
					case 3:
						resultConsumer.accept(EnumStatus.PACKED);
						cachePacksUpdated.set(false);
					break;
					case 4:
						resultConsumer.accept(EnumStatus.REMOVED);
						cachePacksUpdated.set(false);
					break;
					case 5:
						resultConsumer.accept(EnumStatus.UNAVILABLE);
					break;
					}
				} else {
					buf = ClientRPack.this.sendPacket((byte)0x0B, buf, true, 0, false);
					cachePacksUpdated.set(false);
				}
			}
		};
		addToQueue(r);
		return true;
	}

}
