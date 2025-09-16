package me.bomb.rpack.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;


public final class DefaultPackSource extends PackSource {
	
	private final FileSystemProvider fsp;
	private final Path packdir;
	private final DirectoryStream.Filter<Path> zipfilter;
	
	public DefaultPackSource(Path packdir) {
		this.fsp = packdir.getFileSystem().provider();
		this.packdir = packdir;
		this.zipfilter = new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path path) throws IOException {
				final String name = path.getFileName().toString();
				return name.startsWith(".zip", name.length() - 4);
			}
		};
	}
	@Override
	public String[] ids() {
		String[] ids = null;
		DirectoryStream<Path> ds = null;
		try {
			ds = fsp.newDirectoryStream(packdir, zipfilter);
			ArrayList<String> list = new ArrayList<>();
			Iterator<Path> dsi = ds.iterator();
			while(dsi.hasNext()) {
				try {
					String id = dsi.next().getFileName().toString();
					id = id.substring(0, id.length() - 4);
					list.add(id);
				} catch (NoSuchElementException | IndexOutOfBoundsException | NullPointerException e2) {
				}
			}
			ids = new String[list.size()];
			list.toArray(ids);
		} catch (IOException e) {
			ids = new String[0];
		} finally {
			if(ds != null) {
				try {
					ds.close();
				} catch (IOException e) {
				}
			}
		}
		return ids;
	}

	@Override
	public byte[] get(String id) {
		if(id == null) {
			return null;
		}
		final Path pack = packdir.resolve(id.concat(".zip"));
		final int size;
		try {
			BasicFileAttributes attributes = fsp.readAttributes(pack, BasicFileAttributes.class);
			final long sizel = attributes.size();
			if(attributes.isDirectory() || sizel > 262144000) {
				return null;
			}
			size = (int) sizel;
		} catch (IOException e) {
			return null;
		}
		final byte[] packbuf = new byte[size];
		InputStream is = null;
		boolean ok = false;
		try {
			is = fsp.newInputStream(pack);
			is.read(packbuf, 0, size);
			ok = true;
		} catch (IOException e) {
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return ok ? packbuf : null;
	}

}
