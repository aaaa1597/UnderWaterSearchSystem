package com.tks.uws.uwsmember;

import java.text.MessageFormat;
import java.util.UUID;

public class Constants {
	/* ServiceUUIDは0000xxxx-0000-1000-8000-00805f9b34fbの形を守る必要がある。CharacteristicUUIDはなんでもOK.*/
	public static final UUID UWS_SERVICE_UUID							= UUID.fromString("00002c00-0000-1000-8000-00805f9b34fb");
	public static final UUID UWS_CHARACTERISTIC_SAMLE_UUID				= UUID.fromString("29292c2c-728c-4a2b-81cb-7b4d884adb04");

	public static String createServiceUuid(int seqno) {
		String ret = MessageFormat.format("00002c{0}-0000-1000-8000-00805f9b34fb", String.format("%02x", (byte)(seqno & 0xff)));
		TLog.d("UUID文字列={0} seqno={1}({2})", ret, seqno, (byte)(seqno & 0xff));
		return ret;
	}

	/* 定義済UUIDに変換する "0000xxxx-0000-1000-8000-00805f9b34fb" */
	private static UUID convertFromInteger(int i) {
		final long MSB = 0x0000000000001000L;
		final long LSB = 0x800000805f9b34fbL;
		long value = i & 0xFFFFFFFF;
		return new UUID(MSB | (value << 32), LSB);
	}
}
