package com.example.pdflibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import com.example.pdflibrary.element.Bookmark;
import com.example.pdflibrary.element.Link;
import com.example.pdflibrary.element.Meta;
import com.example.pdflibrary.search.FPDFTextSearchContext;
import com.example.pdflibrary.search.TextSearchContext;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.Size;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PdfiumCore {

    static {
        System.loadLibrary("pdfsdk");
        System.loadLibrary("pdfsdk_jni");
    }

    private static final String TAG = PdfiumCore.class.getName();

    private static final Class FD_CLASS = FileDescriptor.class;

    private static final String FD_FIELD_NAME = "descriptor";

    private static final Object lock = new Object();

    private static Field mFdField = null;

    private final int mCurrentDpi;

    public static int getNumFd(ParcelFileDescriptor fdObj) {
        try {
            if (mFdField == null) {
                mFdField = FD_CLASS.getDeclaredField("descriptor");
                mFdField.setAccessible(true);
            }
            return mFdField.getInt(fdObj.getFileDescriptor());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public PdfiumCore(Context ctx) {
//        this.mCurrentDpi = (ctx.getResources().getDisplayMetrics()).densityDpi;
        mCurrentDpi = 72;
        LogUtils.logD(TAG, " mCurrentDpi " + mCurrentDpi);
    }

    public PdfDocument newDocument(ParcelFileDescriptor fd) throws IOException {
        return newDocument(fd, (String) null);
    }

    public PdfDocument newDocument(ParcelFileDescriptor fd, String password) throws IOException {
        PdfDocument document = new PdfDocument();
        document.parcelFileDescriptor = fd;
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenDocument(getNumFd(fd), password);
        }
        return document;
    }

    public PdfDocument newDocument(byte[] data) throws IOException {
        return newDocument(data, (String) null);
    }

    public PdfDocument newDocument(byte[] data, String password) throws IOException {
        PdfDocument document = new PdfDocument();
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenMemDocument(data, password);
        }
        return document;
    }

    public int getPageCount(PdfDocument doc) {
        synchronized (lock) {
            return nativeGetPageCount(doc.mNativeDocPtr);
        }
    }

    /**
     * Prepare information about all characters in a page.
     * Application must call FPDFText_ClosePage to release the text page information.
     *
     * @param pageIndex index of page.
     * @return A handle to the text page information structure. NULL if something goes wrong.
     */
    public long prepareTextInfo(PdfDocument doc, int pageIndex) {
        if (hasTextPage(doc, pageIndex)){
            return doc.mNativeTextPagesPtr.get(pageIndex);
        }
        long textPagePtr;
        textPagePtr = nativeLoadTextPage(doc.mNativeDocPtr, pageIndex);
        if (validPtr(textPagePtr)) {
            doc.mNativeTextPagesPtr.put(Integer.valueOf(pageIndex), Long.valueOf(textPagePtr));
        }
        return textPagePtr;
    }

    /**
     * Prepare information about all characters in a range of pages.
     * Application must call FPDFText_ClosePage to release the text page information.
     *
     * @param fromIndex start index of page.
     * @param toIndex   end index of page.
     * @return list of handles to the text page information structure. NULL if something goes wrong.
     */
    public long[] prepareTextInfo(PdfDocument doc, int fromIndex, int toIndex) {
        long[] textPagesPtr;
        textPagesPtr = nativeLoadTextPages(doc.mNativeDocPtr, fromIndex, toIndex);
        int pageIndex = fromIndex;
        for (long page : textPagesPtr) {
            if (pageIndex > toIndex) break;
            if (validPtr(page)) {
                doc.mNativeTextPagesPtr.put(pageIndex, page);
            }
            pageIndex++;
        }

        return textPagesPtr;
    }

    /**
     * Release all resources allocated for a text page information structure.
     *
     * @param fromIndex start index of page.
     * @param toIndex   end index of page.
     */
    public void releaseTextInfo(PdfDocument doc, int fromIndex, int toIndex) {
        long textPagesPtr;
        for (int i = fromIndex; i < toIndex + 1; i++) {
            textPagesPtr = doc.mNativeTextPagesPtr.get(i);
            if (validPtr(textPagesPtr)) {
                nativeCloseTextPage(textPagesPtr);
            }
        }
    }

    private boolean validPtr(Long ptr) {
        return ptr != null && ptr != -1;
    }

    /**
     * Release all resources allocated for a text page information structure.
     *
     * @param pageIndex index of page.
     */
    public void releaseTextInfo(PdfDocument doc, int pageIndex) {
        long textPagePtr;
        textPagePtr = doc.mNativeTextPagesPtr.get(Integer.valueOf(pageIndex));
        if (validPtr(textPagePtr)) {
            nativeCloseTextPage(textPagePtr);
        }
    }

    private Long ensureTextPage(PdfDocument doc, int pageIndex) {
        Long ptr = doc.mNativeTextPagesPtr.get(pageIndex);
        if (!validPtr(ptr)) {
            return prepareTextInfo(doc, pageIndex);
        }
        return ptr;
    }

    public int countCharactersOnPage(PdfDocument doc, int pageIndex) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            return validPtr(ptr) ? nativeTextCountChars(ptr) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long openPage(PdfDocument doc, int pageIndex) {
        synchronized (lock) {
            long pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex);
            doc.mNativePagesPtr.put(Integer.valueOf(pageIndex), Long.valueOf(pagePtr));
            return pagePtr;
        }
    }

    public long[] openPage(PdfDocument doc, int fromIndex, int toIndex) {
        synchronized (lock) {
            long[] pagesPtr = nativeLoadPages(doc.mNativeDocPtr, fromIndex, toIndex);
            int pageIndex = fromIndex;
            for (long page : pagesPtr) {
                if (pageIndex > toIndex)
                    break;
                doc.mNativePagesPtr.put(Integer.valueOf(pageIndex), Long.valueOf(page));
                pageIndex++;
            }
            return pagesPtr;
        }
    }

    public int getPageWidth(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(Integer.valueOf(index))) != null)
                return nativeGetPageWidthPixel(pagePtr.longValue(), this.mCurrentDpi);
            return 0;
        }
    }

    public int getPageHeight(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(Integer.valueOf(index))) != null)
                return nativeGetPageHeightPixel(pagePtr.longValue(), this.mCurrentDpi);
            return 0;
        }
    }

    public int getPageWidthPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(Integer.valueOf(index))) != null)
                return nativeGetPageWidthPoint(pagePtr.longValue());
            return 0;
        }
    }

    public int getPageHeightPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(Integer.valueOf(index))) != null)
                return nativeGetPageHeightPoint(pagePtr.longValue());
            return 0;
        }
    }

    public Size getPageSize(PdfDocument doc, int index) {
        synchronized (lock) {
            return nativeGetPageSizeByIndex(doc.mNativeDocPtr, index, this.mCurrentDpi);
        }
    }

    /**
     * Get the rotation of page<br>
     */
    public int getPageRotation(PdfDocument doc, int index) {
        Long pagePtr;
        if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
            return nativeGetPageRotation(pagePtr);
        }
        return 0;
    }

    public void renderPage(PdfDocument doc, Surface surface, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPage(doc, surface, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    public void renderPage(PdfDocument doc, Surface surface, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY, boolean renderAnnot) {
        synchronized (lock) {
            try {
                nativeRenderPage(((Long) doc.mNativePagesPtr.get(Integer.valueOf(pageIndex))).longValue(), surface, this.mCurrentDpi, startX, startY, drawSizeX, drawSizeY, renderAnnot);
            } catch (NullPointerException e) {
                LogUtils.logE(TAG, "mContext may be null");
                e.printStackTrace();
            } catch (Exception e) {
                LogUtils.logE(TAG, "Exception throw from native");
                e.printStackTrace();
            }
        }
    }

    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPageBitmap(doc, bitmap, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY, boolean renderAnnot) {
        synchronized (lock) {
            try {
                nativeRenderPageBitmap(((Long) doc.mNativePagesPtr.get(Integer.valueOf(pageIndex))).longValue(), bitmap, this.mCurrentDpi, startX, startY, drawSizeX, drawSizeY, renderAnnot);
            } catch (NullPointerException e) {
                LogUtils.logE(TAG, "mContext may be null");
                e.printStackTrace();
            } catch (Exception e) {
                LogUtils.logE(TAG, "Exception throw from native");
                e.printStackTrace();
            }
        }
    }

    public void closeDocument(PdfDocument doc) {
        synchronized (lock) {
            for (Integer index : doc.mNativePagesPtr.keySet())
                nativeClosePage(((Long) doc.mNativePagesPtr.get(index)).longValue());
            doc.mNativePagesPtr.clear();
            for (Integer ptr : doc.mNativeTextPagesPtr.keySet()) {
                nativeCloseTextPage(doc.mNativeTextPagesPtr.get(ptr));
            }
            doc.mNativeTextPagesPtr.clear();
            nativeCloseDocument(doc.mNativeDocPtr);
            if (doc.parcelFileDescriptor != null) {
                try {
                    doc.parcelFileDescriptor.close();
                } catch (IOException iOException) {
                }
                doc.parcelFileDescriptor = null;
            }
        }
    }

    public Meta getDocumentMeta(PdfDocument doc) {
        synchronized (lock) {
            String title = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Title");
            String author = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Author");
            String subject = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Subject");
            String keywords = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Keywords");
            String creator = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Creator");
            String producer = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Producer");
            String creationDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "CreationDate");
            String modDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "ModDate");
            return new Meta(title, author, subject, keywords, creator, producer, creationDate, modDate);
        }
    }

    public List<Bookmark> getTableOfContents(PdfDocument doc) {
        synchronized (lock) {
            List<Bookmark> topLevel = new ArrayList<>();
            Long first = nativeGetFirstChildBookmark(doc.mNativeDocPtr, null);
            if (first != null)
                recursiveGetBookmark(topLevel, doc, first.longValue());
            return topLevel;
        }
    }

    private void recursiveGetBookmark(List<Bookmark> tree, PdfDocument doc, long bookmarkPtr) {
        Bookmark bookmark = new Bookmark(bookmarkPtr);
        bookmark.setTitle(nativeGetBookmarkTitle(bookmarkPtr));
        bookmark.setPageIdx(nativeGetBookmarkDestIndex(doc.mNativeDocPtr, bookmarkPtr));
        tree.add(bookmark);
        Long child = nativeGetFirstChildBookmark(doc.mNativeDocPtr, Long.valueOf(bookmarkPtr));
        if (child != null)
            recursiveGetBookmark(bookmark.getChildren(), doc, child.longValue());
        Long sibling = nativeGetSiblingBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (sibling != null)
            recursiveGetBookmark(tree, doc, sibling.longValue());
    }

    public List<Link> getPageLinks(PdfDocument doc, int pageIndex) {
        synchronized (lock) {
            List<Link> links = new ArrayList<>();
            Long nativePagePtr = doc.mNativePagesPtr.get(Integer.valueOf(pageIndex));
            if (nativePagePtr == null)
                return links;
            long[] linkPtrs = nativeGetPageLinks(nativePagePtr.longValue());
            for (long linkPtr : linkPtrs) {
                Integer index = nativeGetDestPageIndex(doc.mNativeDocPtr, linkPtr);
                String uri = nativeGetLinkURI(doc.mNativeDocPtr, linkPtr);
                RectF rect = nativeGetLinkRect(linkPtr);
                if (rect != null && (index != null || uri != null))
                    links.add(new Link(rect, index, uri));
            }
            return links;
        }
    }

    public Point mapPageCoordsToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX, int sizeY, int rotate, double pageX, double pageY) {
        long pagePtr = ((Long) doc.mNativePagesPtr.get(Integer.valueOf(pageIndex))).longValue();
        return nativePageCoordinateToDevice(pagePtr, startX, startY, sizeX, sizeY, rotate, pageX, pageY);
    }

    public RectF mapRectToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX, int sizeY, int rotate, RectF coords) {
        Point leftTop = mapPageCoordsToDevice(doc, pageIndex, startX, startY, sizeX, sizeY, rotate, coords.left, coords.top);
        Point rightBottom = mapPageCoordsToDevice(doc, pageIndex, startX, startY, sizeX, sizeY, rotate, coords.right, coords.bottom);
        return new RectF(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y);
    }

    /**
     * Convert the screen coordinates of a point to page coordinates.
     * <p>
     * The page coordinate system has its origin at the left-bottom corner
     * of the page, with the X-axis on the bottom going to the right, and
     * the Y-axis on the left side going up.
     * <p>
     * NOTE: this coordinate system can be altered when you zoom, scroll,
     * or rotate a page, however, a point on the page should always have
     * the same coordinate values in the page coordinate system.
     * <p>
     * The device coordinate system is device dependent. For screen device,
     * its origin is at the left-top corner of the window. However this
     * origin can be altered by the Windows coordinate transformation
     * utilities.
     * <p>
     * You must make sure the start_x, start_y, size_x, size_y
     * and rotate parameters have exactly same values as you used in
     * the FPDF_RenderPage() function call.
     *
     * @param pageIndex index of page
     * @param startX    Left pixel position of the display area in device coordinates.
     * @param startY    Top pixel position of the display area in device coordinates.
     * @param sizeX     Horizontal size (in pixels) for displaying the page.
     * @param sizeY     Vertical size (in pixels) for displaying the page.
     * @param rotate    Page orientation:
     *                  0 (normal)
     *                  1 (rotated 90 degrees clockwise)
     *                  2 (rotated 180 degrees)
     *                  3 (rotated 90 degrees counter-clockwise)
     * @param deviceX   X value in device coordinates to be converted.
     * @param deviceY   Y value in device coordinates to be converted.
     */
    public PointF mapDeviceCoordinateToPage(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX,
                                            int sizeY, int rotate, int deviceX, int deviceY) {
        long pagePtr = doc.mNativePagesPtr.get(pageIndex);
        return nativeDeviceCoordinateToPage(pagePtr, startX, startY, sizeX, sizeY, rotate, deviceX, deviceY);
    }

    /**
     * Extract unicode text string from the page.
     *
     * @param pageIndex  index of page.
     * @param startIndex Index for the start characters.
     * @param length     Number of characters to be extracted.
     * @return Number of characters written into the result buffer, including the trailing terminator.
     */
    public String extractCharacters(PdfDocument doc, int pageIndex, int startIndex, int length) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            if (!validPtr(ptr)) {
                return null;
            }
            short[] buf = new short[length + 1];

            int r = nativeTextGetText(ptr, startIndex, length, buf);

            byte[] bytes = new byte[(r - 1) * 2];
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < r - 1; i++) {
                short s = buf[i];
                bb.putShort(s);
            }

            return new String(bytes, "UTF-16LE");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get Unicode of a character in a page.
     *
     * @param pageIndex index of page.
     * @param index     Zero-based index of the character.
     * @return The Unicode of the particular character. If a character is not encoded in Unicode, the return value will be zero.
     */
    public char extractCharacter(PdfDocument doc, int pageIndex, int index) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            return validPtr(ptr) ? (char) nativeTextGetUnicode(ptr, index) : (char) 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get bounding box of a particular character.
     *
     * @param pageIndex index of page.
     * @param index     Zero-based index of the character.
     * @return the character position measured in PDF "user space".
     */
    public RectF measureCharacterBox(PdfDocument doc, int pageIndex, int index) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            if (!validPtr(ptr)) {
                return null;
            }
            double[] o = nativeTextGetCharBox(ptr, index);
            RectF r = new RectF();
            r.left = (float) o[0];
            r.right = (float) o[1];
            r.bottom = (float) o[2];
            r.top = (float) o[3];
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the index of a character at or nearby a certain position on the page
     *
     * @param pageIndex  index of page.
     * @param x          X position in PDF "user space".
     * @param y          Y position in PDF "user space".
     * @param xTolerance An x-axis tolerance value for character hit detection, in point unit.
     * @param yTolerance A y-axis tolerance value for character hit detection, in point unit.
     * @return The zero-based index of the character at, or nearby the point (x,y). If there is no character at or nearby the point, return value will be -1. If an error occurs, -3 will be returned.
     */
    public int getCharacterIndex(PdfDocument doc, int pageIndex, double x, double y, double xTolerance, double yTolerance) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            return validPtr(ptr) ? nativeTextGetCharIndexAtPos(ptr, x, y, xTolerance, yTolerance) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Count number of rectangular areas occupied by a segment of texts.
     * <p>
     * This function, along with FPDFText_GetRect can be used by applications to detect the position
     * on the page for a text segment, so proper areas can be highlighted or something.
     * FPDFTEXT will automatically merge small character boxes into bigger one if those characters
     * are on the same line and use same font settings.
     *
     * @param pageIndex index of page.
     * @param charIndex Index for the start characters.
     * @param count     Number of characters.
     * @return texts areas count.
     */
    public int countTextRect(PdfDocument doc, int pageIndex, int charIndex, int count) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            return validPtr(ptr) ? nativeTextCountRects(ptr, charIndex, count) : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Get a rectangular area from the result generated by FPDFText_CountRects.
     *
     * @param pageIndex index of page.
     * @param rectIndex Zero-based index for the rectangle.
     * @return the text rectangle.
     */
    public RectF getTextRect(PdfDocument doc, int pageIndex, int rectIndex) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            if (!validPtr(ptr)) {
                return null;
            }
            double[] o = nativeTextGetRect(ptr, rectIndex);
            RectF r = new RectF();
            r.left = (float) o[0];
            r.top = (float) o[1];
            r.right = (float) o[2];
            r.bottom = (float) o[3];
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract unicode text within a rectangular boundary on the page.
     * If the buffer is too small, as much text as will fit is copied into it.
     *
     * @param pageIndex index of page.
     * @param rect      the text rectangle to extract.
     * @return If buffer is NULL or buflen is zero, return number of characters (not bytes) of text
     * present within the rectangle, excluding a terminating NUL.
     * <p>
     * Generally you should pass a buffer at least one larger than this if you want a terminating NUL,
     * which will be provided if space is available. Otherwise, return number of characters copied
     * into the buffer, including the terminating NUL  when space for it is available.
     */
    public String extractText(PdfDocument doc, int pageIndex, RectF rect) {
        try {
            Long ptr = ensureTextPage(doc, pageIndex);
            if (!validPtr(ptr)) {
                return null;
            }

            int length = nativeTextGetBoundedTextLength(ptr, rect.left, rect.top, rect.right, rect.bottom);
            if (length <= 0) {
                return null;
            }

            short[] buf = new short[length + 1];

            int r = nativeTextGetBoundedText(ptr, rect.left, rect.top, rect.right, rect.bottom, buf);

            byte[] bytes = new byte[(r - 1) * 2];
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < r - 1; i++) {
                short s = buf[i];
                bb.putShort(s);
            }
            return new String(bytes, "UTF-16LE");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A handle class for the search context. stopSearch must be called to release this handle.
     *
     * @param pageIndex      index of page.
     * @param query          A unicode match pattern.
     * @param matchCase      match case
     * @param matchWholeWord match the whole word
     * @return A handle for the search context.
     */
    public TextSearchContext newPageSearch(PdfDocument doc, int pageIndex, String query, boolean matchCase, boolean matchWholeWord) {
        return new FPDFTextSearchContext(pageIndex, query, matchCase, matchWholeWord) {

            private Long mSearchHandlePtr;

            @Override
            public void prepareSearch() {

                long textPage = prepareTextInfo(doc, pageIndex);

                if (hasSearchHandle(doc, pageIndex)) {
                    long sPtr = doc.mNativeSearchHandlePtr.get(pageIndex);
                    nativeSearchStop(sPtr);
                }

                this.mSearchHandlePtr = nativeSearchStart(textPage, query, matchCase, matchWholeWord);
            }

            @Override
            public int countResult() {
                if (validPtr(mSearchHandlePtr)) {
                    return nativeCountSearchResult(mSearchHandlePtr);
                }
                return -1;
            }

            @Override
            public RectF searchNext() {
                if (validPtr(mSearchHandlePtr)) {
                    mHasNext = nativeSearchNext(mSearchHandlePtr);
                    if (mHasNext) {
                        int index = nativeGetCharIndexOfSearchResult(mSearchHandlePtr);
                        if (index > -1) {
                            return measureCharacterBox(doc, this.getPageIndex(), index);
                        }
                    }
                }

                mHasNext = false;
                return null;
            }

            @Override
            public RectF searchPrev() {
                if (validPtr(mSearchHandlePtr)) {
                    mHasPrev = nativeSearchPrev(mSearchHandlePtr);
                    if (mHasPrev) {
                        int index = nativeGetCharIndexOfSearchResult(mSearchHandlePtr);
                        if (index > -1) {
                            return measureCharacterBox(doc, this.getPageIndex(), index);
                        }
                    }
                }

                mHasPrev = false;
                return null;
            }

            @Override
            public void stopSearch() {
                super.stopSearch();
                if (validPtr(mSearchHandlePtr)) {
                    nativeSearchStop(mSearchHandlePtr);
                    doc.mNativeSearchHandlePtr.remove(getPageIndex());
                }
            }
        };

    }

    public String getTextFromTextPtr(long textPtr){
        return nativeGetText(textPtr);
    }

    public int getCurrentDpi() {
        return mCurrentDpi;
    }

    public void setCurrentDpi(int d) {
//        mCurrentDpi = d;
    }

    public boolean hasPage(PdfDocument doc, int index) {
        return doc.mNativePagesPtr.containsKey(index);
    }

    public boolean hasTextPage(PdfDocument doc, int index) {
        return doc.mNativeTextPagesPtr.containsKey(index);
    }

    public boolean hasSearchHandle(PdfDocument doc, int index) {
        return doc.mNativeSearchHandlePtr.containsKey(index);
    }


    private native long nativeOpenDocument(int fd, String password);

    private native long nativeOpenMemDocument(byte[] data, String password);

    private native void nativeCloseDocument(long docPtr);

    private native int nativeGetPageCount(long docPtr);

    private native long nativeLoadPage(long docPtr, int pageIndex);

    private native long[] nativeLoadPages(long docPtr, int fromIndex, int toIndex);

    private native void nativeClosePage(long pagePtr);

    private native void nativeClosePages(long[] pagesPtr);

    private native int nativeGetPageWidthPixel(long pagePtr, int dpi);

    private native int nativeGetPageHeightPixel(long pagePtr, int dpi);

    private native int nativeGetPageWidthPoint(long pagePtr);

    private native int nativeGetPageHeightPoint(long pagePtr);

    private native int nativeGetPageRotation(long pagePtr);

    private native void nativeRenderPage(
            long pagePtr, Surface surface, int dpi,
            int startX, int startY,
            int drawSizeHor, int drawSizeVer,
            boolean renderAnnot);

    private native void nativeRenderPageBitmap(
            long pagePtr, Bitmap bitmap, int dpi,
            int startX, int startY,
            int drawSizeHor, int drawSizeVer,
            boolean renderAnnot);

    private native String nativeGetDocumentMetaText(long docPtr, String tag);

    private native Long nativeGetFirstChildBookmark(long docPtr, Long bookmarkPtr);

    private native Long nativeGetSiblingBookmark(long docPtr, long bookmarkPtr);

    private native String nativeGetBookmarkTitle(long bookmarkPtr);

    private native long nativeGetBookmarkDestIndex(long docPtr, long bookmarkPtr);

    private native Size nativeGetPageSizeByIndex(long docPtr, int pageIndex, int dpi);

    private native long[] nativeGetPageLinks(long pagePtr);

    private native Integer nativeGetDestPageIndex(long docPtr, long linkPtr);

    private native String nativeGetLinkURI(long docPtr, long linkPtr);

    private native RectF nativeGetLinkRect(long linkPtr);

    private native Point nativePageCoordinateToDevice(long pagePtr, int startX, int startY, int sizeX,
                                                      int sizeY, int rotate, double pageX, double pageY);

    private native PointF nativeDeviceCoordinateToPage(long pagePtr, int startX, int startY, int sizeX,
                                                       int sizeY, int rotate, int deviceX, int deviceY);

    private native long nativeLoadTextPage(long docPtr, int pageIndex);

    private native long[] nativeLoadTextPages(long docPtr, int fromIndex, int toIndex);

    private native void nativeCloseTextPage(long pagePtr);

    private native void nativeCloseTextPages(long[] pagesPtr);

    private native int nativeTextCountChars(long textPagePtr);

    private native int nativeTextGetText(long textPagePtr, int start_index, int count, short[] result);

    private native int nativeTextGetUnicode(long textPagePtr, int index);

    private native double[] nativeTextGetCharBox(long textPagePtr, int index);

    private native int nativeTextGetCharIndexAtPos(long textPagePtr, double x, double y, double xTolerance, double yTolerance);

    private native int nativeTextCountRects(long textPagePtr, int start_index, int count);

    private native double[] nativeTextGetRect(long textPagePtr, int rect_index);

    private native int nativeTextGetBoundedTextLength(long textPagePtr, double left, double top, double right, double bottom);

    private native int nativeTextGetBoundedText(long textPagePtr, double left, double top, double right, double bottom, short[] arr);

    private native long nativeSearchStart(long textPagePtr, String query, boolean matchCase, boolean matchWholeWord);

    private native void nativeSearchStop(long searchHandlePtr);

    private native boolean nativeSearchNext(long searchHandlePtr);

    private native boolean nativeSearchPrev(long searchHandlePtr);

    private native int nativeGetCharIndexOfSearchResult(long searchHandlePtr);

    private native int nativeCountSearchResult(long searchHandlePtr);

    private native long nativeAddTextAnnotation(long docPtr, int pageIndex, String text, int[] color, int[] bound);

    private native String nativeGetText(long textPtr);

    public native int nativeGetCharIndexAtCoord(long pagePtr, double width, double height, long textPtr, double posX, double posY, double tolX, double tolY);

    public native int nativeCountAndGetRects(long pagePtr, int offsetY, int offsetX, int width, int height, ArrayList<RectF> arr, long tid, int selSt, int selEd);

    ///////////////////////////////////////
    // PDF Native Callbacks
    ///////////

    private void onAnnotationAdded(int pageIndex, long pageNewPtr) {
    }

    private void onAnnotationUpdated(int pageIndex, long pageNewPtr) {
    }

    private void onAnnotationRemoved(int pageIndex, long pageNewPtr) {
    }
}