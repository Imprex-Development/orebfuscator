/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface INBT {
	void reset();

	void setInt(String tag, int value);

	void setLong(String tag, long value);

	void setByteArray(String tag, byte[] value);

	void setIntArray(String tag, int[] value);

	default void setLongArray(String tag, long[] value) {}

	int getInt(String tag);

	long getLong(String tag);

	byte[] getByteArray(String tag);

	int[] getIntArray(String tag);

	default long[] getLongArray(String tag) { return new long[0]; }

	void read(DataInput stream) throws IOException;

	void write(DataOutput stream) throws IOException;
}
