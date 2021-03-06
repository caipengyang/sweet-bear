package com.wanhu.android.shelves.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.TreeSet;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public final class FileUtilities {

	private static final String LOG_TAG = "FileUtilities";
	private static final String BOOK__CACHE_DIRECTORY = "shelves/books";
	private static SimpleDateFormat sLastModifiedFormat;

	public FileUtilities() {
	}

	class FileCompare implements Comparator<File> {
		public int compare(File f1, File f2) {
			return f1.getAbsolutePath().toLowerCase()
					.compareTo(f2.getAbsolutePath().toLowerCase());
		}
	}

	TreeSet<File> mAllPDFs = new TreeSet<File>(new FileCompare());

	private void findAllPDFsHelper(File dir, String endsWith) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null) {
				int sz = files.length;
				for (int i = 0; i < sz; ++i) {
					if (files[i].isDirectory()) {
						findAllPDFsHelper(files[i], endsWith);
					} else if (files[i].isFile()
							&& files[i].getName().toLowerCase()
									.endsWith(endsWith)) {
						mAllPDFs.add(files[i]);
					}
				}
			}
		}
	}

	public TreeSet<File> findAllPDFs(String location, String endsWith) {
		File root = new File(location);
		mAllPDFs.clear();
		findAllPDFsHelper(root, endsWith);
		return mAllPDFs;
	}

	public static class ExpiringFile {
		public File file;
		public Calendar lastModified;
	}

	public static ExpiringFile load(String url, String id) {
		return load(url, null, id);
	}
	
	public static ExpiringFile load(String url, String cookie, String id) {
		ExpiringFile expiring = new ExpiringFile();

		final HttpGet get = new HttpGet(url);
		if (cookie != null)
			get.setHeader("cookie", cookie);

		HttpEntity entity = null;
		try {
			final HttpResponse response = HttpManager.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				setLastModified(expiring, response);

				entity = response.getEntity();

				InputStream in = null;
				OutputStream out = null;

				File booksCacheDirectory;
				try {
					
					booksCacheDirectory = ensureBooksCache();
					in = entity.getContent();
					File file = new File(booksCacheDirectory, id);
					
					// Log.d(file.getName().toString(), "LOAD - FILE NAME > "+id.toString());
					
					out = new FileOutputStream(file);
					
					IOUtilities.copy(in, out,entity.getContentLength());
					out.flush();
					expiring.file = file;

				} catch (IOException e) {
					android.util.Log.e(LOG_TAG, "Could not load file from "
							+ url, e);
				} finally {
					IOUtilities.closeStream(in);
					IOUtilities.closeStream(out);
				}
			}
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not load image from " + url, e);
		} finally {
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					android.util.Log.e(LOG_TAG, "Could not load image from "
							+ url, e);
				}
			}
		}

		return expiring;
	}

	private static void setLastModified(ExpiringFile expiring,
			HttpResponse response) {
		expiring.lastModified = null;

		final Header header = response.getFirstHeader("Last-Modified");
		if (header == null)
			return;

		if (sLastModifiedFormat == null) {
			sLastModifiedFormat = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss z");
		}

		final Calendar calendar = GregorianCalendar.getInstance();
		try {
			calendar.setTime(sLastModifiedFormat.parse(header.getValue()));
			expiring.lastModified = calendar;
		} catch (ParseException e) {
		}
	}

	private static File ensureBooksCache() throws IOException {
		File booksCacheDirectory = getBooksCacheDirectory();
		if (!booksCacheDirectory.exists()) {
			booksCacheDirectory.mkdirs();
			new File(booksCacheDirectory, ".nomedia").createNewFile();
		}
		return booksCacheDirectory;
	}

	public static File getBooksCacheDirectory() {
		return IOUtilities.getExternalFile(BOOK__CACHE_DIRECTORY);
	}
	
	public static void deleteCachedBooks(String id) {
        new File(FileUtilities.getBooksCacheDirectory(), id).delete();
    }
}