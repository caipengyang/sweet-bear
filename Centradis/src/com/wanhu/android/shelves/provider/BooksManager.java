package com.wanhu.android.shelves.provider;

import java.io.File;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.wanhu.android.shelves.util.FileUtilities;
import com.wanhu.android.shelves.util.IOUtilities;
import com.wanhu.android.shelves.util.ImageUtilities;
import com.wanhu.android.shelves.util.ImportUtilities;
import com.wanhu.android.shelves.util.TextUtilities;

public class BooksManager {

	private static String sBookIdSelection;
	private static String sBookSelection;

	private static String[] sArguments1 = new String[1];
	private static String[] sArguments3 = new String[3];

	private static final String[] PROJECTION_ID_IID = new String[] {
			BooksStore.Book._ID, BooksStore.Book.INTERNAL_ID };
	private static final String[] PROJECTION_ID = new String[] { BooksStore.Book._ID };

	static {
		StringBuilder selection = new StringBuilder();
		selection.append(BooksStore.Book.INTERNAL_ID);
		selection.append("=?");
		sBookIdSelection = selection.toString();

		selection = new StringBuilder();
		selection.append(sBookIdSelection);
		selection.append(" OR ");
		selection.append(BooksStore.Book.EAN);
		selection.append("=? OR ");
		selection.append(BooksStore.Book.ISBN);
		selection.append("=?");
		sBookSelection = selection.toString();
	}

	private BooksManager() {
	}

	public static String findBookId(ContentResolver contentResolver, String id) {
		String internalId = null;
		Cursor c = null;

		try {
			final String[] arguments3 = sArguments3;
			arguments3[0] = arguments3[1] = arguments3[2] = id;
			c = contentResolver.query(BooksStore.Book.CONTENT_URI,
					PROJECTION_ID_IID, sBookSelection, arguments3, null);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					internalId = c
							.getString(c
									.getColumnIndexOrThrow(BooksStore.Book.INTERNAL_ID));
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return internalId;
	}

	public static boolean bookExists(ContentResolver contentResolver, String id) {
		boolean exists;
		Cursor c = null;

		try {
			final String[] arguments3 = sArguments3;
			arguments3[0] = arguments3[1] = arguments3[2] = id;
			c = contentResolver.query(BooksStore.Book.CONTENT_URI,
					PROJECTION_ID, sBookSelection, arguments3, null);
			exists = c.getCount() > 0;
		} finally {
			if (c != null)
				c.close();
		}

		return exists;
	}

	public static BooksStore.Book loadAndAddBook(ContentResolver resolver,
			BooksStore.Book pBookToAdd, Bitmap pBookCoverToAdd,
			BooksStore booksStore) {
		
		final BooksStore.Book book = pBookToAdd;
		if (book != null) {

			File file = book.loadBook(book.getTitle()+".pdf");
			
			if (file == null) {
				return null;
			}

			final Bitmap bitmap = pBookCoverToAdd;
			if (bitmap != null) {
				ImportUtilities.addBookCoverToCache(book, bitmap);
			}
			if (booksStore.downloadBookOk(book.getInternalId())) {
				final Uri uri = resolver.insert(BooksStore.Book.CONTENT_URI,
						book.getContentValues());
				if (uri != null) {
					return book;
				}
			}
		}

		return null;
	}

	public static BooksStore.Book updateBook(ContentResolver resolver,
			String pDocumentId, BooksStore booksStore) {

		final BooksStore.Book book = booksStore.findBook(pDocumentId);

		if (book != null) {
			
			Log.d(book.getTitle().toString(),"BOOK FILENAME HERE ALSO");
			
			String bookId = book.getInternalId();

			File file = book.loadBook(bookId);
			
			if (file == null) {
				return null;
			}

			IOUtilities.mOnPublishProgress = null;
			final Bitmap coverBitMap = book
					.loadCover(BooksStore.ImageSize.TINY);

			if (booksStore.downloadBookOk(bookId)) {
				
				// Log.d(bookId,"UPDATEBOOK ID HERE");

				
				book.mIsbn = CentradisBooksStore.VALUE_USER_ID;
				
				/*
				int a = resolver.update(BooksStore.Book.CONTENT_URI,
						book.getContentValues(), BooksStore.Book.INTERNAL_ID
								+ "=? And " + BooksStore.Book.ISBN + "=?",
						new String[] { pDocumentId,
								CentradisBooksStore.VALUE_USER_ID });
				*/
				//if (a > 0) {

					if (coverBitMap != null) {
						//ImageUtilities.deleteCachedCover(bookId);
						//Log.d(coverBitMap.toString(),"COVERBITMAP");
						ImportUtilities.addBookCoverToCache(book, coverBitMap);
					}
					
					// FileUtilities.deleteCachedBooks(bookId);
					 
					//Log.d(FileUtilities.getBooksCacheDirectory().toString(),"BOOK CACHE DIRECTORY ");
					
					// RENAME TMP FILE (/ID) TO TITLE.PDF LABEL
					// new File(FileUtilities.getBooksCacheDirectory(), bookId+ "temp").renameTo(new File(FileUtilities.getBooksCacheDirectory(),newFilename));
					// new File(FileUtilities.getBooksCacheDirectory(), bookId+ "temp").renameTo(new File(FileUtilities.getBooksCacheDirectory(), bookId));
					final String newFilename = book.getTitle().toString()+".pdf";
					//Log.d(newFilename,"BOOK NEWFILENAME");
					new File(FileUtilities.getBooksCacheDirectory(), bookId).renameTo(new File(FileUtilities.getBooksCacheDirectory(),newFilename));
					
					/*
					Log.d(book.mInternalId.toString(),"BOOK DONE");
					java.io.File fileup = new java.io.File(FileUtilities.getBooksCacheDirectory().toString() , newFilename);
					if (fileup.exists()) {
						Log.d(fileup.getPath().toString(),"FILEUP EXIST DONE");
					}
					*/
					
					return book;
				//}
			}
		}

		return null;
	}

	public static boolean deleteBook(ContentResolver contentResolver,
			String bookId, BooksStore booksStore) {

		if (booksStore.deleteBook(bookId)) {
			final String[] arguments1 = sArguments1;
			arguments1[0] = bookId;
			int count = contentResolver.delete(BooksStore.Book.CONTENT_URI,
					sBookIdSelection, arguments1);
			if (count > 0) {
				ImageUtilities.deleteCachedCover(bookId);
				FileUtilities.deleteCachedBooks(bookId);
				return true;
			}
		}

		return false;
	}
	
	public static boolean deleteLocalBook(ContentResolver contentResolver,
			String bookId,String pdf) {
		final String[] arguments1 = sArguments1;
		arguments1[0] = bookId;
		int count = contentResolver.delete(BooksStore.Book.CONTENT_URI,
				sBookIdSelection, arguments1);
		ImageUtilities.deleteCachedCover(pdf);
		FileUtilities.deleteCachedBooks(bookId);
		return count > 0;
	}

	public static BooksStore.Book findBook(ContentResolver contentResolver,
			String id) {
		BooksStore.Book book = null;
		Cursor c = null;

		try {
			sArguments1[0] = id;
			c = contentResolver.query(BooksStore.Book.CONTENT_URI, null,
					sBookIdSelection, sArguments1, null);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					book = BooksStore.Book.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return book;
	}

	public static BooksStore.Book findBook(ContentResolver contentResolver,
			Uri data) {
		BooksStore.Book book = null;
		Cursor c = null;

		try {
			c = contentResolver.query(data, null, null, null, null);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					book = BooksStore.Book.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return book;
	}
}
