package me.bomb.rpack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import me.bomb.rpack.http.SimpleServerSocketFactory;
import me.bomb.rpack.http.SimpleSocketFactory;
import me.bomb.rpack.util.SimpleConfiguration;

public final class Configuration {
	
	public final String errors;
	public final Path musicdir;
	
	public final boolean use, usecmd, connectuse, connecttls;
	
	public final String sendpackhost;
	public final InetAddress sendpackifip, connectifip, connectremoteip;
	public final int sendpackport, connectport;
	public final int sendpackbacklog, connectbacklog;
	public final int sendpacktimeout, connecttimeout;
	
	public final boolean sendpackstrictaccess;
	
	public final boolean servercache, clientcache, waitacception;
	
	protected final byte[] tokensalt;
	protected final ServerSocketFactory sendpackserverfactory, connectserverfactory;
	protected final SocketFactory connectsocketfactory;
	
	public Configuration(Path musicdir, boolean usecmd, boolean uploaduse, boolean sendpackuse, boolean connectuse, boolean encoderuse, boolean uploadhttps, boolean connecthttps, String uploadhost, String sendpackhost, InetAddress sendpackifip, InetAddress uploadifip, InetAddress connectifip, InetAddress connectremoteip, int sendpackport, int uploadport, int connectport, int sendpackbacklog, int uploadbacklog, int connectbacklog, int sendpacktimeout, int connecttimeout, int uploadtimeout, boolean uploadstrictaccess, boolean sendpackstrictaccess, Path encoderbinary, boolean processpack, boolean servercache, boolean clientcache, boolean waitacception, int uploadlifetime, int uploadlimitsize, int uploadlimitcount, short packthreadlimitcount, float packthreadcoefficient, byte encoderchannels, int encoderbitrate, int encodersamplingrate, byte[] tokensalt, ServerSocketFactory sendpackserverfactory, ServerSocketFactory uploadserverfactory, ServerSocketFactory connectserverfactory, SocketFactory connectsocketfactory) {
		this.errors = new String();
		this.use = true;
		this.musicdir = musicdir;
		this.usecmd = usecmd;
		this.connectuse = connectuse;
		this.connecttls = connecthttps;
		this.sendpackhost = sendpackhost;
		this.sendpackifip = sendpackifip;
		this.connectifip = connectifip;
		this.connectremoteip = connectremoteip;
		this.sendpackport = sendpackport;
		this.connectport = connectport;
		this.sendpackbacklog = sendpackbacklog;
		this.connectbacklog = connectbacklog;
		this.sendpacktimeout = sendpacktimeout;
		this.connecttimeout = connecttimeout;
		this.sendpackstrictaccess = sendpackstrictaccess;
		this.servercache = servercache;
		this.clientcache = clientcache;
		this.waitacception = waitacception;
		this.tokensalt = tokensalt;
		this.sendpackserverfactory = sendpackserverfactory;
		this.connectserverfactory = connectserverfactory;
		this.connectsocketfactory = connectsocketfactory;
	}
	
	public Configuration(FileSystem fs, final Path configfile, final Path musicdir, final boolean defaultwaitacception, final boolean defaultremoteclient, final String resourcename) {
		byte[] bytes = null;
		final StringBuilder errors = new StringBuilder();
		InputStream is = null;
		FileSystemProvider fsp = fs.provider();
		try {
			BasicFileAttributes attributes = fsp.readAttributes(configfile, BasicFileAttributes.class);
			is = fsp.newInputStream(configfile);
			long filesize = attributes.size();
			if(filesize > 0x00010000) {
				filesize = 0x00010000;
			}
			bytes = new byte[(int)filesize];
			int size = is.read(bytes);
			if(size < filesize) {
				bytes = Arrays.copyOf(bytes, size);
			}
			is.close();
		} catch (IOException e1) {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e2) {
				}
			}
			try {
				is = Configuration.class.getClassLoader().getResourceAsStream(resourcename);
				bytes = new byte[0x0800];
				bytes = Arrays.copyOf(bytes, is.read(bytes));
				is.close();
				OutputStream os = null;
				try {
					os = fsp.newOutputStream(configfile);
					os.write(bytes);
					os.close();
				} catch (IOException e3) {
					appendError("Filed to write default config", errors);
					if(os != null) {
						try {
							os.close();
						} catch (IOException e4) {
						}
					}
				}
			} catch (IOException e3) {
				appendError("Filed to read default config", errors);
				if(is != null) {
					try {
						is.close();
					} catch (IOException e4) {
					}
				}
			}
		}
		SimpleConfiguration sc;
		if(bytes != null && (sc = new SimpleConfiguration(bytes)).getBooleanOrError("rpack\0use", errors)) {
			this.use = true;
			this.musicdir = musicdir;
			this.usecmd = sc.getBooleanOrError("rpack\0usecmd", errors);
			this.connectuse = sc.getBooleanOrError("rpack\0server\0connect\0use", errors);

			this.sendpackhost = sc.getStringOrError("rpack\0host", errors);
			InetAddress sendpackifip = null;
			String sendpackipstr = sc.getStringOrError("rpack\0server\0sendpack\0ip", errors);
			if(sendpackipstr!=null) {
				try {
					sendpackifip = InetAddress.getByName(sendpackipstr);
				} catch (UnknownHostException e) {
					appendError("Filed to get sendpack local interface ip", errors);
				}
			}
			this.sendpackifip = sendpackifip;
			this.sendpackport = sc.getIntOrError("rpack\0server\0sendpack\0port", errors);
			this.sendpackbacklog = sc.getIntOrError("rpack\0server\0sendpack\0backlog", errors);
			this.sendpacktimeout = sc.getIntOrError("rpack\0server\0sendpack\0timeout", errors);
			this.sendpackstrictaccess = sc.getBooleanOrError("rpack\0server\0sendpack\0strictaccess", errors);
			this.waitacception = sc.getBooleanOrDefault("rpack\0server\0sendpack\0waitacception", defaultwaitacception);
			this.tokensalt = sc.getBytesBase64OrError("rpack\0server\0sendpack\0tokensalt", errors);
			this.servercache = sc.getBooleanOrError("rpack\0server\0sendpack\0cache\0server", errors);
			this.clientcache = sc.getBooleanOrError("rpack\0server\0sendpack\0cache\0client", errors);
			
			this.sendpackserverfactory = new SimpleServerSocketFactory();
			if(this.connectuse) {
				InetAddress connectifip = null;
				String connectipstr = sc.getStringOrError("rpack\0server\0connect\0ip", errors);
				if(connectipstr!=null) {
					try {
						connectifip = InetAddress.getByName(connectipstr);
					} catch (UnknownHostException e) {
						appendError("Filed to get connect local interface ip", errors);
					}
				}
				this.connectifip = connectifip;

				this.connecttls = sc.getBooleanOrError("rpack\0server\0connect\0tls\0use", errors);
				
				if(defaultremoteclient) {
					InetAddress connectserverip = null;
					String connectserveripstr = sc.getStringOrError("rpack\0server\0connect\0client\0serverip", errors);
					if(connectserveripstr != null && !connectserveripstr.equals("0.0.0.0")) {
						try {
							connectserverip = InetAddress.getByName(connectserveripstr);
						} catch (UnknownHostException e) {
							appendError("Filed to get connect server ip", errors);
						}
					}
					this.connectremoteip = connectserverip;
					this.connectport = sc.getIntOrError("rpack\0server\0connect\0client\0port", errors);
					this.connectbacklog = 0;
					this.connecttimeout = sc.getIntOrError("rpack\0server\0connect\0client\0timeout", errors);
					this.connectserverfactory = null;
					if(connecttls) {
						KeyStore keystore = null;
						SSLSocketFactory sslsocketfactory = null;
						final String connectcertpath = sc.getStringOrError("rpack\0server\0connect\0tls\0path", errors);
						Path certfile = null;
						try {
							certfile = fs.getPath(connectcertpath);
						} catch (InvalidPathException e) {
							appendError("Filed to read connect tls certificate file (path invalid)", errors);
						}
						
						final String certpassword;
						if(certfile != null && (certpassword = sc.getStringOrError("rpack\0server\0connect\0tls\0password", errors)) != null) {
							is = null;
							try {
								is = fs.provider().newInputStream(certfile);
							} catch (SecurityException e1) {
								if(is != null) {
									try {
										is.close();
									} catch (IOException e2) {
									}
								}
								appendError("Filed to read connect tls certificate file (no permission)", errors);
							} catch (IOException e) {
								appendError("Filed to read connect tls certificate file (not found)", errors);
							}
							try {
								keystore = KeyStore.getInstance("PKCS12");
							} catch (KeyStoreException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate (filed to get PKCS12 instance)", errors);
							}
							try {
								keystore.load(is, certpassword.toCharArray());
							} catch (CertificateException | NoSuchAlgorithmException | IOException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate", errors);
							}
							try {
								TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
								trustManagerFactory.init(keystore);
								TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
								KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
								keyManagerFactory.init(keystore, certpassword.toCharArray());
								KeyManager[] keymanagers = keyManagerFactory.getKeyManagers();
								SSLContext tlscontext = SSLContext.getInstance("TLSv1.2");
								tlscontext.init(keymanagers, trustmanagers, SecureRandom.getInstanceStrong());
								sslsocketfactory = tlscontext.getSocketFactory();
							} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
								sslsocketfactory = null;
							}
						}
						this.connectsocketfactory = sslsocketfactory;
					} else {
						this.connectsocketfactory = new SimpleSocketFactory();
					}
				} else {
					InetAddress connectclientip = null;
					String connectclientipstr = sc.getStringOrError("rpack\0server\0connect\0server\0clientip", errors);
					if(connectclientipstr != null && !connectclientipstr.equals("0.0.0.0")) {
						try {
							connectclientip = InetAddress.getByName(connectclientipstr);
						} catch (UnknownHostException e) {
							appendError("Filed to get connect client ip", errors);
						}
					}
					this.connectremoteip = connectclientip;
					this.connectport = sc.getIntOrError("rpack\0server\0connect\0server\0port", errors);
					this.connectbacklog = sc.getIntOrError("rpack\0server\0connect\0server\0backlog", errors);
					this.connecttimeout = sc.getIntOrError("rpack\0server\0connect\0server\0timeout", errors);
					this.connectsocketfactory = null;
					if(connecttls) {
						KeyStore keystore = null;
						SSLServerSocketFactory sslserverfactory = null;
						final String connectcertpath = sc.getStringOrError("rpack\0server\0connect\0tls\0path", errors);
						Path certfile = null;
						try {
							certfile = fs.getPath(connectcertpath);
						} catch (InvalidPathException e) {
							appendError("Filed to read connect tls certificate file (path invalid)", errors);
						}
						
						final String certpassword;
						if(certfile != null && (certpassword = sc.getStringOrError("rpack\0server\0connect\0tls\0password", errors)) != null) {
							is = null;
							try {
								is = fs.provider().newInputStream(certfile);
							} catch (SecurityException e1) {
								if(is != null) {
									try {
										is.close();
									} catch (IOException e2) {
									}
								}
								appendError("Filed to read connect tls certificate file (no permission)", errors);
							} catch (IOException e) {
								appendError("Filed to read connect tls certificate file (not found)", errors);
							}
							try {
								keystore = KeyStore.getInstance("PKCS12");
							} catch (KeyStoreException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate (filed to get PKCS12 instance)", errors);
							}
							try {
								keystore.load(is, certpassword.toCharArray());
							} catch (CertificateException | NoSuchAlgorithmException | IOException e) {
								keystore = null;
								appendError("Filed to initialize connect tls certificate", errors);
							}
							try {
								TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
								trustManagerFactory.init(keystore);
								TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
								KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
								keyManagerFactory.init(keystore, certpassword.toCharArray());
								KeyManager[] keymanagers = keyManagerFactory.getKeyManagers();
								SSLContext tlscontext = SSLContext.getInstance("TLSv1.2");
								tlscontext.init(keymanagers, trustmanagers, SecureRandom.getInstanceStrong());
								sslserverfactory = tlscontext.getServerSocketFactory();
							} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
								sslserverfactory = null;
							}
						}
						this.connectserverfactory = sslserverfactory;
					} else {
						this.connectserverfactory = new SimpleServerSocketFactory();
					}
				}
			} else {
				this.connectifip = null;
				this.connecttls = false;
				this.connectremoteip = null;
				this.connectport = 0;
				this.connectbacklog = 0;
				this.connecttimeout = 0;
				this.connectserverfactory = null;
				this.connectsocketfactory = null;
			}
		} else {
			this.use = false;
			this.usecmd = false;
			this.musicdir = null;
			this.connectuse = false;
			this.sendpackserverfactory = null;
			this.sendpackhost = null;
			this.sendpackifip = null;
			this.sendpackport = 0;
			this.sendpackbacklog = 0;
			this.sendpacktimeout = 0;
			this.sendpackstrictaccess = false;
			this.waitacception = false;
			this.tokensalt = null;
			this.connectifip = null;
			this.connecttls = false;
			this.connectremoteip = null;
			this.connectport = 0;
			this.connectbacklog = 0;
			this.connecttimeout = 0;
			this.connectserverfactory = null;
			this.connectsocketfactory = null;
			this.servercache = false;
			this.clientcache = false;
		}
		this.errors = errors.toString();
	}
	
	private static void appendError(String error, StringBuilder sb) {
		sb.append(error);
		sb.append('\n');
	}

}
