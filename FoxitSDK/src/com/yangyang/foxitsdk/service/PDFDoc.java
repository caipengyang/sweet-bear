package com.yangyang.foxitsdk.service;

import java.nio.ByteBuffer;

import FoxitEMBSDK.EMBJavaSupport;
import FoxitEMBSDK.EMBJavaSupport.CPDFFormFillerInfo;
import FoxitEMBSDK.EMBJavaSupport.CPDFJsPlatform;
import FoxitEMBSDK.EMBJavaSupport.CPDFPSI;
import FoxitEMBSDK.EMBJavaSupport.PointF;
import FoxitEMBSDK.EMBJavaSupport.Rectangle;
import FoxitEMBSDK.EMBJavaSupport.RectangleF;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import com.yangyang.foxitsdk.exception.memoryException;
import com.yangyang.foxitsdk.exception.parameterException;
import com.yangyang.foxitsdk.view.IPDFView;

public class PDFDoc {

	/* state variables */
	private boolean initFlag = false;
	private int fileAccessHandle = 0;
	private int nPDFDocHandler = 0;
	protected int[] pageHandlers;
	protected int pageCount = 0;
	private int currentPageNumber = 0;
	private static String TAG = "FoxitDoc";
	private static final String strFontFilePath = "/mnt/sdcard/DroidSansFallback.ttf";
	private int mode;

	/** form */
	private CPDFFormFillerInfo formFillerInfo = null;
	private int nPDFFormFillerInfo = 0;
	private CPDFJsPlatform jsPlatform = null;
	private int nPDFJsPlatform = 0;
	private int nPDFFormHandler = 0;
	private IPDFView view;

	/** psi */
	private CPDFPSI fxPsi = null;
	private int nPSICallback = 0;
	private int nPSIHandle = 0;

	/** search */
	private int nPDFCurTextPageHandler;
	private int nFindHandler;
	private int nFindRects = -1;
	private EMBJavaSupport.RectangleF[] rcFind = null;
	private int nCurFindIndex = -1;

	public enum Mode {
		Read(1), // 只读模式（默认）
		Annotation(1 << 1), // 注释（可以修改文件添加注释)
		Form(1 << 2), // 填表（可以填写表单）
		PSI(1 << 3), // 自动绘图（可一触摸屏绘任意形状）
		;
		private int type;

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		Mode(int type) {
			this.type = type;
		}
	}

	/**
	 * note类型
	 * 
	 * @author yangyang
	 * 
	 */
	public enum AnnotationType {
		NONE, // 无操作
		NOTE, // 注释
		HIGHLIGHT, // 高亮显示
		PENCIL, // 铅笔
		STAMP, // 邮戳
		ERASER, // 橡皮擦
	}

	/**
	 * if you want to allow a specified memory block,you must call this function
	 * yourself.Otherwides,the SDK will initialize a 5M memory block when
	 * creating a PDF document.
	 * 
	 * @param memorySize
	 */
	public void initFoxitSDK(int memorySize) {
		try {
			EMBJavaSupport.FSMemInitFixedMemory(memorySize);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			postToLog(e.getMessage());
			return;
		}
		EMBJavaSupport.FSInitLibrary(0);
		EMBJavaSupport.FSUnlock("SDKEDFZ1101",
				"67F4682D93E2DEC7D70457CBB6866EE1762DFD45");

		// ///////formfiller implemention///////
		if ((this.mode & Mode.Form.getType()) > 0) {
			if (view == null)
				return;
			formFillerInfo = new EMBJavaSupport().new CPDFFormFillerInfo(view);
			if (formFillerInfo == null)
				return;
			nPDFFormFillerInfo = EMBJavaSupport
					.FPDFFormFillerInfoAlloc(formFillerInfo);
			if (nPDFFormFillerInfo == 0)
				return;

			jsPlatform = new EMBJavaSupport().new CPDFJsPlatform();
			if (jsPlatform == null)
				return;
			nPDFJsPlatform = EMBJavaSupport.FPDFJsPlatformAlloc(jsPlatform);
			if (nPDFJsPlatform == 0)
				return;

			EMBJavaSupport.FPDFFormFillerInfoSetJsPlatform(nPDFFormFillerInfo,
					nPDFJsPlatform);
			EMBJavaSupport.FPDFJsPlatformSetFormFillerInfo(nPDFJsPlatform,
					nPDFFormFillerInfo);
			// ////////////////////////////
		}

		LoadJbig2Decoder();
		LoadJpeg2000Decoder();
		LoadCNSFontCMap();
		LoadKoreaFontCMap();
		LoadJapanFontCMap();
		SetFontFileMap();
		this.initFlag = true;
	}

	/** Load jbig2 decoder. */
	private void LoadJbig2Decoder() {
		EMBJavaSupport.FSLoadJbig2Decoder();
	}

	/** Load jpeg2000 decoder. */
	private void LoadJpeg2000Decoder() {
		EMBJavaSupport.FSLoadJpeg2000Decoder();
	}

	/** */
	private void LoadJapanFontCMap() {
		EMBJavaSupport.FSFontLoadJapanCMap();
		EMBJavaSupport.FSFontLoadJapanExtCMap();
	}

	/** */
	private void LoadCNSFontCMap() {
		EMBJavaSupport.FSFontLoadGBCMap();
		EMBJavaSupport.FSFontLoadGBExtCMap();
		EMBJavaSupport.FSFontLoadCNSCMap();
	}

	/** */
	private void LoadKoreaFontCMap() {
		EMBJavaSupport.FSFontLoadKoreaCMap();
	}

	private void SetFontFileMap() {
		try {
			EMBJavaSupport.FSSetFileFontmap(strFontFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PDFDoc(String filePath, String password, IPDFView view, int mode) {
		try {
			this.mode = mode;
			this.view = view;
			if (!initFlag)
				this.initFoxitSDK(5 * 1024 * 1024);
			fileAccessHandle = EMBJavaSupport.FSFileReadAlloc(filePath);
			nPDFDocHandler = EMBJavaSupport.FPDFDocLoad(fileAccessHandle,
					password);
			nPDFFormHandler = EMBJavaSupport.FPDFDocInitFormFillEnviroument(
					nPDFDocHandler, nPDFFormFillerInfo);
			this.pageHandlers = new int[this.getPageCounts()];
		} catch (memoryException e) {
			EMBJavaSupport.FSFileReadRelease(fileAccessHandle);
			nPDFDocHandler = 0;
			fileAccessHandle = 0;
			postToLog(e.getMessage());
			return;
		} catch (Exception e) {
			postToLog(e.getMessage());
			return;
		}

		// /form info///
		/**
		 * formFillerInfo = new EMBJavaSupport().new CPDFFormFillerInfo(view);
		 * if (formFillerInfo == null) return; nPDFFormFillerInfo =
		 * EMBJavaSupport .FPDFFormFillerInfoAlloc(formFillerInfo); if
		 * (nPDFFormFillerInfo == 0) return;
		 * 
		 * jsPlatform = new EMBJavaSupport().new CPDFJsPlatform(); if
		 * (jsPlatform == null) return; nPDFJsPlatform =
		 * EMBJavaSupport.FPDFJsPlatformAlloc(jsPlatform); if (nPDFJsPlatform ==
		 * 0) return;
		 * EMBJavaSupport.FPDFFormFillerInfoSetJsPlatform(nPDFFormFillerInfo,
		 * nPDFJsPlatform);
		 * EMBJavaSupport.FPDFJsPlatformSetFormFillerInfo(nPDFJsPlatform,
		 * nPDFFormFillerInfo);
		 */
		// /form info//
	}

	/*
	 * public void updateMode(Mode mode) { this.mode = mode; // switch
	 * (this.mode) { // case Form: // nPDFFormHandler =
	 * EMBJavaSupport.FPDFDocInitFormFillEnviroument( // nPDFDocHandler,
	 * nPDFFormFillerInfo); // break; // case Read: // if (nPDFFormHandler > 0)
	 * { // EMBJavaSupport.FPDFFormFillOnBeforeClosePage(nPDFFormHandler, //
	 * this.getCurrentPageHandler()); //
	 * EMBJavaSupport.FPDFDocExitFormFillEnviroument(nPDFFormHandler); //
	 * nPDFFormHandler = 0; // } // break; // default: // break; // } }
	 */

	/** Count PDF page. */
	public int getPageCounts() {
		if (nPDFDocHandler == 0) {
			return EMBJavaSupport.EMBJavaSupport_RESULT_ERROR;
		}
		if (pageCount <= 0)
			try {
				pageCount = EMBJavaSupport.FPDFDocGetPageCount(nPDFDocHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return pageCount;
	}

	/**
	 * return a specified page handler.
	 * 
	 * @param pageNumber
	 * @return
	 */
	public int getPageHandler(int pageNumber) {
		if (pageHandlers.length <= pageNumber) {
			postToLog("pageNumber is bigger than max page count!");
		} else if (pageHandlers[pageNumber] <= 0) {
			try {
				pageHandlers[pageNumber] = EMBJavaSupport.FPDFPageLoad(
						nPDFDocHandler, pageNumber);
				EMBJavaSupport.FPDFPageStartParse(pageHandlers[pageNumber], 0,
						0);
				if ((this.mode & Mode.Form.getType()) > 0) {
					// /formfiller implemention
					EMBJavaSupport.FPDFFormFillOnAfterLoadPage(nPDFFormHandler,
							pageHandlers[pageNumber]);
					EMBJavaSupport.FPDFFormFillUpdatForm(this
							.getPDFFormHandler());
					// //////////////////////////
				}

			} catch (memoryException e) {
				postToLog(e.getMessage());
			} catch (Exception e) {
				postToLog(e.getMessage());
			}
		} else {
			// if ((this.mode & Mode.Form.getType()) > 0) {
			// // /formfiller implemention
			// EMBJavaSupport.FPDFFormFillOnAfterLoadPage(nPDFFormHandler,
			// pageHandlers[pageNumber]);
			// EMBJavaSupport.FPDFFormFillUpdatForm(this.getPDFFormHandler());
			// // //////////////////////////
			// }
		}
		return pageHandlers[pageNumber];
	}

	/**
	 * jump to the next page
	 */
	public void nextPage() {
		this.currentPageNumber++;
		if (this.currentPageNumber >= this.pageCount)
			this.currentPageNumber = this.pageCount - 1;
		if ((this.mode & Mode.Form.getType()) > 0) {
			EMBJavaSupport.FPDFFormFillOnKillFocus(nPDFFormHandler);
		}
	}

	/**
	 * jump to the previous page
	 */
	public void previoutPage() {
		this.currentPageNumber--;
		if (this.currentPageNumber < 0)
			this.currentPageNumber = 0;
		if ((this.mode & Mode.Form.getType()) > 0) {
			EMBJavaSupport.FPDFFormFillOnKillFocus(nPDFFormHandler);
		}
	}

	public int getCurrentPage() {
		return this.currentPageNumber;
	}

	public int getCurrentPageHandler() {
		if (this.currentPageNumber < 0) {
			postToLog("error current page number:" + this.currentPageNumber);
			return -1;
		}
		return this.getPageHandler(this.currentPageNumber);
	}

	/**
	 * jump to a specified page number.
	 * 
	 * @param pageNumber
	 */
	public void gotoPage(int pageNumber) {
		if (pageNumber >= this.getPageCounts()) {
			postToLog("pdf document has no page:" + pageNumber);
		} else {
			this.currentPageNumber = pageNumber;
		}
	}

	/** Render pdf to bitmap. */
	public Bitmap getPageBitmap(int displayWidth, int displayHeight) {
		int nPDFCurPageHandler = this.getCurrentPageHandler();
		if (nPDFCurPageHandler == 0) {
			return null;
		}
		Bitmap bm;
		bm = Bitmap.createBitmap(displayWidth, displayHeight,
				Bitmap.Config.ARGB_8888);
		int dib;
		try {
			dib = EMBJavaSupport.FSBitmapCreate(displayWidth, displayHeight, 7,
					null, 0);
			EMBJavaSupport.FSBitmapFillColor(dib, 0xff);
			EMBJavaSupport.FPDFRenderPageStart(dib, nPDFCurPageHandler, 0, 0,
					displayWidth, displayHeight, 0,
					(mode & Mode.Annotation.getType()) > 0 ? 1 : 0, null, 0);

			// /formfiller implemention
			if ((mode & Mode.Form.getType()) > 0) {
				EMBJavaSupport.FPDFFormFillDraw(nPDFFormHandler, dib,
						nPDFCurPageHandler, 0, 0, displayWidth, displayHeight,
						0, 0);
			}
			// ////////////////////////////
			byte[] bmpbuf = EMBJavaSupport.FSBitmapGetBuffer(dib);
			ByteBuffer bmBuffer = ByteBuffer.wrap(bmpbuf);
			bm.copyPixelsFromBuffer(bmBuffer);

			EMBJavaSupport.FSBitmapDestroy(dib);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bm;
	}

	public Bitmap getDirtyBitmap(Rect rect, int displayWidth, int displayHeight) {
		Bitmap bm = null;
		int nPDFCurPageHandler = this.getCurrentPageHandler();
		if (nPDFCurPageHandler == 0) {
			return null;
		}

		bm = Bitmap.createBitmap(rect.width(), rect.height(),
				Bitmap.Config.ARGB_8888);
		int dib;
		try {
			dib = EMBJavaSupport.FSBitmapCreate(rect.width(), rect.height(), 7,
					null, 0);

			EMBJavaSupport.FSBitmapFillColor(dib, 0xff);
			EMBJavaSupport.FPDFRenderPageStart(dib, nPDFCurPageHandler,
					-rect.left, -rect.top, displayWidth, displayHeight, 0, 0,
					null, 0);

			// /formfiller implemention//
			if (nPDFFormHandler == 0)
				return null;
			EMBJavaSupport.FPDFFormFillDraw(nPDFFormHandler, dib,
					nPDFCurPageHandler, -rect.left, -rect.top, displayWidth,
					displayHeight, 0, 0);
			// //////////////////////////////

			byte[] bmpbuf = EMBJavaSupport.FSBitmapGetBuffer(dib);
			ByteBuffer bmBuffer = ByteBuffer.wrap(bmpbuf);
			bm.copyPixelsFromBuffer(bmBuffer);

			EMBJavaSupport.FSBitmapDestroy(dib);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bm;
	}

	public void lock() {

	}

	/**
	 * 文件保存或者另存为
	 * 
	 * @param fileName
	 *            新的文件名
	 * @return 0：成功
	 */
	public int save(String fileName) {
		try {
			int filewrite = EMBJavaSupport.FSFileWriteAlloc(fileName);
			EMBJavaSupport.FPDFDocSaveAs(nPDFDocHandler,
					EMBJavaSupport.EMBJavaSupport_SAVEFLAG_INCREMENTAL, 0,
					filewrite);
			EMBJavaSupport.FSFileWriteRelease(filewrite);
			return EMBJavaSupport.EMBJavaSupport_RESULT_SUCCESS;
		} catch (memoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	// clean up unmanaged resources
	public void close() {
		if (this.nPDFCurTextPageHandler > 0) {
			this.CloseTextPage();
		}
		// /formfiller implemention
		if (this.nPDFFormHandler > 0) {
			EMBJavaSupport.FPDFDocExitFormFillEnviroument(nPDFFormHandler);
			nPDFFormHandler = 0;
		}
		if (nPDFFormFillerInfo > 0) {
			EMBJavaSupport.FPDFFormFillerInfoRelease(nPDFFormFillerInfo);
			nPDFFormFillerInfo = 0;
			EMBJavaSupport.FPDFJsPlatformRelease(nPDFJsPlatform);
			nPDFJsPlatform = 0;

		}
		// ///

		for (int i = 0; i < pageHandlers.length; i++) {
			if (pageHandlers[i] > 0) {// if page handle exist for that page
				try {
					EMBJavaSupport.FPDFPageClose(pageHandlers[i]);
					pageHandlers[i] = 0;
				} catch (parameterException e) {
					// Not handling parameter exception for now
				}
			}
		}
		if (fileAccessHandle != 0) {
			EMBJavaSupport.FSFileReadRelease(fileAccessHandle);
		}
	}

	public float GetPageSizeX(int pageIndex) {
		if (this.pageHandlers[pageIndex] == 0) {
			this.getPageHandler(pageIndex);
		}

		try {
			return EMBJavaSupport.FPDFPageGetSizeX(pageHandlers[pageIndex]);
		} catch (parameterException e) {
			postToLog(e.getMessage());
			return 0;
		}
	}

	public float GetPageSizeY(int pageIndex) {
		if (pageHandlers[pageIndex] == 0) {
			this.getPageHandler(pageIndex);
		}

		try {
			return EMBJavaSupport.FPDFPageGetSizeY(pageHandlers[pageIndex]);
		} catch (parameterException e) {
			postToLog(e.getMessage());
			return 0;
		}
	}

	private void postToLog(String msg) {
		Log.v(TAG, msg);
	}

	public int getDocumentHandler() {
		// TODO Auto-generated method stub
		return this.nPDFDocHandler;
	}

	public int addAnnot(AnnotationType annotationType, Object data)
			throws memoryException {

		int result = EMBJavaSupport.EMBJavaSupport_RESULT_ERROR;
		switch (annotationType) {

		case NONE: {
			break;
		}

		case NOTE: {
			int nNoteInfoItem = EMBJavaSupport.FPDFNoteInfoAlloc("James",
					0x0000ff, 80, (RectangleF) data, "I like note",
					this.getCurrentPageHandler());
			int nIndex = EMBJavaSupport
					.FPDFAnnotAdd(this.getCurrentPageHandler(),
							EMBJavaSupport.EMBJavaSupport_ANNOTTYPE_NOTE,
							nNoteInfoItem);
			EMBJavaSupport.FPDFNoteInfoRelease(nNoteInfoItem);
			if (nIndex >= 0) {
				result = nIndex;
			}
			break;
		}

		case PENCIL: {
			int line_count = 1;
			int nPencilInfoItem = EMBJavaSupport.FPDFPencilInfoAlloc("James",
					0xff0000, 80, true, true, 5, line_count);
			int nLineInfo = EMBJavaSupport.FPDFLineInfoAlloc(line_count);
			float[] points = (float[]) data;
			EMBJavaSupport.FPDFLineInfoSetPointInfo(nLineInfo, 0, 2, points);
			EMBJavaSupport
					.FPDFPencilInfoSetLineInfo(nPencilInfoItem, nLineInfo);
			int nIndex = EMBJavaSupport.FPDFAnnotAdd(
					this.getCurrentPageHandler(),
					EMBJavaSupport.EMBJavaSupport_ANNOTTYPE_PENCIL,
					nPencilInfoItem);
			EMBJavaSupport.FPDFLineInfoRelease(nLineInfo);
			EMBJavaSupport.FPDFPencilInfoRelease(nPencilInfoItem);
			if (nIndex >= 0) {
				result = nIndex;
			}

			break;
		}

		case STAMP: {
			String path = "/mnt/sdcard/FoxitLog.jpg";
			int nStampInfo = EMBJavaSupport.FPDFStampInfoAlloc("James",
					0xffff00, 80, (RectangleF) data, "Stamp_Test", path);
			int nIndex = EMBJavaSupport.FPDFAnnotAdd(
					this.getCurrentPageHandler(),
					EMBJavaSupport.EMBJavaSupport_ANNOTTYPE_STAMP, nStampInfo);
			EMBJavaSupport.FPDFStampInfoRelease(nStampInfo);
			if (nIndex >= 0) {
				result = nIndex;
			}
			break;
		}
		default:
			break;
		}

		// int filewrite =
		// EMBJavaSupport.FSFileWriteAlloc("/data/data/com.foxitsample.annotations/FoxitSaveAnnotation.pdf");
		// EMBJavaSupport.FPDFDocSaveAs(nPDFDocHandler,
		// EMBJavaSupport.EMBJavaSupport_SAVEFLAG_INCREMENTAL,0, filewrite);
		// EMBJavaSupport.FSFileWriteRelease(filewrite);
		return result;
	}

	public int getAnnotationIndex(int x, int y) {
		return EMBJavaSupport.FPDFAnnotGetIndexAtPos(
				this.getCurrentPageHandler(), x, y);
	}

	public boolean deleteAnnotation(int annotationIndex) {
		if (annotationIndex > 0) {
			int nRet = EMBJavaSupport.FPDFAnnotDelete(
					this.getCurrentPageHandler(), annotationIndex);
			if (nRet == 0)
				return true;
		}
		return false;
	}

	public int deleteAnnotation(int x, int y) {
		int annot_index = this.getAnnotationIndex(x, y);
		if (deleteAnnotation(annot_index))
			return annot_index;
		return -1;
	}

	public int getPDFFormHandler() {
		return nPDFFormHandler;
	}

	public int getCurPSIHandle() {
		return nPSIHandle;
	}

	public int getMode() {
		// TODO Auto-generated method stub
		return this.mode;
	}

	public int FindPrev() {
		if (nPDFCurTextPageHandler == 0 || nFindHandler == 0)
			return 0;
		int ret = EMBJavaSupport.FPDFTextFindPrev(nFindHandler);
		if (ret != 1)
			return 0;
		nCurFindIndex = EMBJavaSupport.FPDFTextGetSchResultIndex(nFindHandler);
		int nCount = EMBJavaSupport.FPDFTextGetSchCount(nFindHandler);
		nFindRects = EMBJavaSupport.FPDFTextCountRects(nPDFCurTextPageHandler,
				nCurFindIndex, nCount);
		rcFind = new EMBJavaSupport.RectangleF[nFindRects];
		for (int i = 0; i < nFindRects; i++) {
			EMBJavaSupport.RectangleF rcFindTemp = (new EMBJavaSupport()).new RectangleF();
			rcFindTemp = EMBJavaSupport.FPDFTextGetRect(nPDFCurTextPageHandler,
					i);
			rcFind[i] = (new EMBJavaSupport()).new RectangleF();
			rcFind[i].left = rcFindTemp.left;
			rcFind[i].top = rcFindTemp.top;
			rcFind[i].right = rcFindTemp.right;
			rcFind[i].bottom = rcFindTemp.bottom;
		}
		return nFindRects;
	}

	public boolean InitPDFTextPage() {
		nPDFCurTextPageHandler = EMBJavaSupport.FPDFTextLoadPage(this
				.getCurrentPageHandler());
		// if(nPDFCurTextPageHandler == 0)
		// return false;//throw later
		return true;
	}

	public void CloseTextPage() {
		EMBJavaSupport.FPDFTextCloseTextPage(nPDFCurTextPageHandler);
		nPDFCurTextPageHandler = 0;
	}

	public int SearchStart(String strFindWhat) {
		if (nPDFCurTextPageHandler > 0) {
			this.CloseTextPage();
		}
		this.InitPDFTextPage();
		nFindHandler = EMBJavaSupport.FPDFTextFindStart(nPDFCurTextPageHandler,
				strFindWhat, 4, 0);
		if (nFindHandler == 0)
			return 0;
		int rectnum = FindNext();
		return rectnum;
	}

	public int FindNext() {
		if (nPDFCurTextPageHandler == 0 || nFindHandler == 0)
			return 0;
		int ret = EMBJavaSupport.FPDFTextFindNext(nFindHandler);
		EMBJavaSupport.FPDFTextGetText(nPDFCurTextPageHandler, 0, 100);
		if (ret != 1)
			return 0;
		nCurFindIndex = EMBJavaSupport.FPDFTextGetSchResultIndex(nFindHandler);
		int nCount = EMBJavaSupport.FPDFTextGetSchCount(nFindHandler);
		nFindRects = EMBJavaSupport.FPDFTextCountRects(nPDFCurTextPageHandler,
				nCurFindIndex, nCount);
		rcFind = new EMBJavaSupport.RectangleF[nFindRects];
		for (int i = 0; i < nFindRects; i++) {
			EMBJavaSupport.RectangleF rcFindTemp = (new EMBJavaSupport()).new RectangleF();
			rcFindTemp = EMBJavaSupport.FPDFTextGetRect(nPDFCurTextPageHandler,
					i);
			rcFind[i] = (new EMBJavaSupport()).new RectangleF();
			rcFind[i].left = rcFindTemp.left;
			rcFind[i].top = rcFindTemp.top;
			rcFind[i].right = rcFindTemp.right;
			rcFind[i].bottom = rcFindTemp.bottom;
		}
		return nFindRects;
	}

	public Bitmap GetHighLightMarkedRectBitmap(int width, int height,
			float stride) {
		int[] colors = new int[width * height];
		for (int i = 0; i < width * height; i++)
			colors[i] = 0;
		int r = 0;
		int g = 0;
		int b = 255;
		int a = 50;
		int color_blue = Color.argb(a, r, g, b);
		for (int j = 0; j < width * height; j++)
			colors[j] = color_blue;
		Bitmap map = Bitmap.createBitmap(colors, width, height,
				Bitmap.Config.ARGB_8888);
		return map;
	}

	public RectangleF GetHighLightMarkedRect(int index, int displayWidth,
			int displayHeight) {
		if (nFindRects <= 0)
			return null;
		float left = rcFind[index].left;
		float bottom = rcFind[index].bottom;
		float right = rcFind[index].right;
		float top = rcFind[index].top;
		EMBJavaSupport.PointF point = EMBJavaSupport.instance.new PointF();
		EMBJavaSupport.RectangleF rect = EMBJavaSupport.instance.new RectangleF();
		point.x = left;
		point.y = top;
		EMBJavaSupport.FPDFPagePageToDevicePointF(this.getCurrentPageHandler(),
				0, 0, displayWidth, displayHeight, 0, point);
		rect.left = point.x;
		rect.top = point.y;
		point.x = right;
		point.y = bottom;
		EMBJavaSupport.FPDFPagePageToDevicePointF(this.getCurrentPageHandler(),
				0, 0, displayWidth, displayHeight, 0, point);
		rect.right = point.x;
		rect.bottom = point.y;

		return rect;
	}
}
