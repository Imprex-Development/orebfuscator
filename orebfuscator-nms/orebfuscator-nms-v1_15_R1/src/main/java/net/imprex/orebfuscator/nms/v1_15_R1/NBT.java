/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package net.imprex.orebfuscator.nms.v1_15_R1;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

import com.lishid.orebfuscator.nms.INBT;

import net.minecraft.server.v1_15_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagLongArray;

public class NBT implements INBT {

	private NBTTagCompound nbt = new NBTTagCompound();

	public void reset() {
		this.nbt = new NBTTagCompound();
	}

	public void setInt(String tag, int value) {
		this.nbt.setInt(tag, value);
	}

	public void setLong(String tag, long value) {
		this.nbt.setLong(tag, value);
	}

	public void setByteArray(String tag, byte[] value) {
		this.nbt.setByteArray(tag, value);
	}

	public void setIntArray(String tag, int[] value) {
		this.nbt.setIntArray(tag, value);
	}

	@Override
	public void setLongArray(String tag, long[] value) {
		this.nbt.set(tag, new NBTTagLongArray(value));
	}

	public int getInt(String tag) {
		return this.nbt.getInt(tag);
	}

	public long getLong(String tag) {
		return this.nbt.getLong(tag);
	}

	public byte[] getByteArray(String tag) {
		return this.nbt.getByteArray(tag);
	}

	public int[] getIntArray(String tag) {
		return this.nbt.getIntArray(tag);
	}

	@Override
	public long[] getLongArray(String tag) {
		return this.nbt.getLongArray(tag);
	}

	public void read(DataInput stream) throws IOException {
		this.nbt = NBTCompressedStreamTools.a((DataInputStream) stream);
	}

	public void write(DataOutput stream) throws IOException {
		NBTCompressedStreamTools.a(this.nbt, stream);
	}
}