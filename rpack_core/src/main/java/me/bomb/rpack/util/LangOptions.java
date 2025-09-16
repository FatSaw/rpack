package me.bomb.rpack.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.bomb.rpack.MessageSender;

public enum LangOptions {
loadpack_usage, loadpack_nopermission, loadpack_nopermissionother, loadpack_noconsoleselector, loadpack_targetoffline, loadpack_processing, loadpack_nopack, loadpack_loaderunavilable, loadpack_success_dispatched, loadpack_success_updated, loadpack_success_deleted, loadpack_unavilableselector_near, loadpack_unavilableselector_random, loadpack_unavilableselector_all;
	private static MessageSender messagesender;
	
	private final static String defaultlang = new String();
	
	public static void loadLang(MessageSender messagesender, Path langfile, final String resourcename) {
		LangOptions.messagesender = messagesender;
		byte[] buf = null;
		InputStream is = null;
		FileSystemProvider fs = langfile.getFileSystem().provider();
		try {
			BasicFileAttributes attributes = fs.readAttributes(langfile, BasicFileAttributes.class);
			is = fs.newInputStream(langfile);
			long filesize = attributes.size();
			if(filesize > 0x7FFFFFFD) {
				filesize = 0x7FFFFFFD;
			}
			buf = new byte[(int)filesize];
			int size = is.read(buf);
			if(size < filesize) {
				buf = Arrays.copyOf(buf, size);
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
				is = LangOptions.class.getClassLoader().getResourceAsStream(resourcename);
				buf = new byte[0x3000];
				buf = Arrays.copyOf(buf, is.read(buf));
				is.close();
				OutputStream os = null;
				try {
					os = fs.newOutputStream(langfile);
					os.write(buf);
					os.close();
				} catch (IOException e3) {
					if(os != null) {
						try {
							os.close();
						} catch (IOException e4) {
						}
					}
				}
			} catch (IOException e3) {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e4) {
					}
				}
			}
		}
		SimpleConfiguration sc = new SimpleConfiguration(buf);
		String replacelangkey = "replacelang\0", localisationkey = "localisation\0", localisation = "default", localisation0 = localisationkey.concat(localisation).concat("\0");
		LangOptions[] values = values();
		final int valuescount = values.length;
		int i = valuescount;
		while (--i > -1) {
			LangOptions lang = values[i];
			lang.text.clear();
			String langname = lang.name();
			String msg = sc.getStringOrDefault(localisation0.concat(langname.replace("_", "\0")), langname);
			msg = msg.replace("\\n", "\n");
			lang.text.put(defaultlang, msg);
		}
		String[] localisationkeys = sc.getSubKeys(localisationkey);
		i = localisationkeys.length;
		while(--i > -1) {
			localisation = localisationkeys[i];
			if(localisation.equals("default")) continue;
			localisation0 = localisationkey.concat(localisation).concat("\0");
			int j = valuescount;
			while (--j > -1) {
				LangOptions lang = values[j];
				String msg = sc.getStringOrDefault(localisation0.concat(lang.name().replace("_", "\0")), null);
				if(msg==null) continue;
				msg = msg.replace("\\n", "\n");
				lang.text.put(localisation, msg);
			}
		}
		String[] replacelangkeys = sc.getSubKeys(replacelangkey);
		i = replacelangkeys.length;
		while(--i > -1) {
			String to = replacelangkeys[i], replacekey0 = replacelangkey.concat(to);
			String from = sc.getStringOrDefault(replacekey0, to);
			int j = valuescount;
			while (--j > -1) {
				LangOptions lang = values[j];
				lang.text.put(to, lang.text.get(from));
			}
		}
	}
	
	private final Map<String, String> text = new HashMap<String, String>();

	public void sendMsg(final Object target, final Placeholder... placeholders) {
		String msg = null;
		final String locale = messagesender.getLocale(target);
		if (locale != null) {
			msg = text.get(locale);
		}
		if(msg==null) {
			msg = text.get(defaultlang);
		}
		if (msg==null || msg.isEmpty()) {
			return;
		}
		int i = placeholders.length;
		while(--i > -1) {
			Placeholder placeholder = placeholders[i];
			msg = msg.replace(placeholder.placeholder, placeholder.value);
		}
		messagesender.send(target, msg);
	}
	
	public static class Placeholder {
		
		private static final byte[] HEX = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
		
		public final String placeholder;
		public final String value;

		public Placeholder(String placeholder, String value) {
			this.placeholder = placeholder;
			this.value = filterValue(value);
		}
		
		private static String filterValue(String value) {
			char[] chars = value.toCharArray();
			int i = chars.length;
			int j = (i << 2) + (i << 1);
			byte[] filtered = new byte[j];
			while(--i > -1) {
				int ch = chars[i];
				filtered[--j] = HEX[ch & 0x0F];
				byte r = 3;
				while(--r > -1) {
					ch >>>= 4;
					filtered[--j] = HEX[ch & 0x0F];
				}
				filtered[--j] = 'u';
				filtered[--j] = '\\';
			}
			return new String(filtered, StandardCharsets.US_ASCII);
		}
	}
}
