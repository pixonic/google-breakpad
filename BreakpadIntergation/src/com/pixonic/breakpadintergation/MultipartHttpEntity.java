// (C) Pixonic, 2013

package com.pixonic.breakpadintergation;

import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.SequenceInputStream;
import java.io.StringBufferInputStream;
import java.io.IOException;

/// позволяет отправлять в теле post-запроса формы с отправкой файлов
/// можно атачить несколько файлов
class MultipartHttpEntity implements HttpEntity
{
	public String BoundaryTag = "7F5D2195-CDF6-40E2-A0BD-3525A7C0007A";

	private ArrayList < InputStream > inputChuncks = new ArrayList< InputStream >(5);
	private long totalLength = 0;
	private boolean ready = false;

	MultipartHttpEntity()
	{
		BoundaryTag = UUID.randomUUID().toString();
	}

	/// добавить в запрос поста строковое значение `value` с именем `name`
	public void addValue(String name, String value)
	{
		String data = "\n--" + BoundaryTag + "\n" +
			String.format("Content-Disposition: form-data; name=\"%s\"\n\n%s", name, value);

		totalLength += data.length();

		inputChuncks.add(new StringBufferInputStream(data));
	}

	/// добавить файл `f` как поле формы `name` с именем файла `filename`
	public void addFile(String name, String filename, File f) throws IOException
	{
		long l = f.length();
		try{

			String data = "\n--" + BoundaryTag + "\n" +
				String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\nContent-Type: application/octet-stream\n\n", name, filename);

			totalLength += l + data.length();
			inputChuncks.add(new StringBufferInputStream(data));
			inputChuncks.add(new FileInputStream(f));
		} catch(IOException e) {
			android.util.Log.e("TAG", "Can't use input file " + filename, e);
			throw e;
		}
	}

	/// завершить тело поста
	public void end()
	{
		String data = "\n--" + BoundaryTag + "--\n";
		totalLength += data.length();
		inputChuncks.add(new StringBufferInputStream(data));

		ready = true;
	}

	/// //////////////////////////////////// ///
	///  INTERFACE FOR HTTPCLIENT            ///
	/// //////////////////////////////////// ///

	public void consumeContent()
	{
		totalLength = 0;
		inputChuncks.clear();

		ready = false;
	}
	public InputStream getContent()
	{
		return new SequenceInputStream(Collections.enumeration(inputChuncks));
	}
	public Header getContentEncoding()
	{
		return null;
	}
	public long getContentLength()
	{
		return totalLength;
	}
	public Header getContentType()
	{
		return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + BoundaryTag);
	}
	public boolean isChunked()
	{
		return false;
	}
	public boolean isRepeatable()
	{
		return false;
	}
	public boolean isStreaming()
	{
		return ready;
	}
	public void writeTo(OutputStream outstream)
	{
		for(InputStream inp : inputChuncks)
			writeFromInputToOutput(inp, outstream);
	}

	private static final int BUFFER_SIZE = 2048;
	private static final int EOF_MARK = -1;
	public int writeFromInputToOutput(InputStream source, OutputStream dest) {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = EOF_MARK;
		int count = 0;
		try {
			while ((bytesRead = source.read(buffer)) != EOF_MARK) {
				dest.write(buffer, 0, bytesRead);
				count += bytesRead;
			}
		} catch (IOException e) {
			android.util.Log.e("TAG", "IOException", e);
		}
		return count;
	}
}