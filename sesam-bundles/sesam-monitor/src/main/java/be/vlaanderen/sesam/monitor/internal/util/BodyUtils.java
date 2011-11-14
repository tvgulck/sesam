/*
 * Copyright 2011 Vlaams Gewest
 *
 * This file is part of SESAM, the Service Endpoint Security And Monitoring framework.
 *
 * SESAM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SESAM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SESAM.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.vlaanderen.sesam.monitor.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

public class BodyUtils {

	private static final Logger log = LoggerFactory.getLogger(BodyUtils.class);

	private BodyUtils() {
	}

	public static String getMd5Sum(File f) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			String res = DigestUtils.md5Hex(fis);
			log.debug("Hashed file: {} -- {}", f.getName(), res);
			return res;
		} catch (IOException e) {
			log.warn("Failed calculating MD5sum. " + e.getMessage());
			return null;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Inputstream is not closed.
	 * 
	 * @param f
	 * @return
	 */
	public static String getMd5Sum(InputStream is) {
		try {
			String res = DigestUtils.md5Hex(is);
			log.debug("Hashed a stream: {}", res);
			return res;
		} catch (IOException e) {
			log.warn("Failed calculating MD5sum. " + e.getMessage());
			return null;
		}
	}

	public static ChannelBuffer fileToBuffer(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		byte[] buf = FileCopyUtils.copyToByteArray(fis);
		ChannelBuffer wcb = ChannelBuffers.wrappedBuffer(buf);
		return wcb;
	}
}