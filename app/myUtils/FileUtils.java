package myUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import models.Blobs;
import models.Tags;
import models.Tags2;
import models.UserBlobs;
import models.Users;

import org.apache.commons.io.IOUtils;

import play.mvc.Http.MultipartFormData.FilePart;

public class FileUtils {
	
	public static Long upload(FilePart uploadedFile, String userName) throws IOException {
			Long uploadResult = -1L;
		 	if (uploadedFile == null) {
		 		throw new IOException("Null file uploaded");
		 	} else {
				String fileName = uploadedFile.getFilename();
				if (Blobs.find.where().eq("name",fileName).findUnique() != null)
					throw new IllegalStateException("Duplicate file name");
					
				String contentType = uploadedFile.getContentType();
				File file = uploadedFile.getFile();
				FileOutputStream oFile = new FileOutputStream("./files/"+fileName);
				
				Blobs bl = new Blobs();
				InputStream inStream = null;
				try {
					inStream = new FileInputStream(file);
					org.apache.commons.io.IOUtils.copy(inStream, oFile);
					bl.blobFile = "./files/"+fileName;
					bl.name=fileName;
					bl.tag=Tags.DOCUMENT;
					bl.extension=contentType;
					bl.owner= Users.findByUserName(userName);
					bl.creationDate = new Date();
					bl.save();
					uploadResult = bl.id;
				} finally {
						IOUtils.closeQuietly(inStream);
						IOUtils.closeQuietly(oFile);
					}
				return uploadResult;
		 	}
	}
		
	public static Long uploadPersonnel (FilePart uploadedFile, Long userId) throws IOException {
		Long uploadResult = -1L;
	 	if (uploadedFile == null) {
	 		throw new IOException("Null file uploaded");
	 	} else {
			String fileName = uploadedFile.getFilename();
			if (UserBlobs.find.where().eq("name",fileName).findUnique() != null)
				throw new IllegalStateException("Duplicate file name");
				
			String contentType = uploadedFile.getContentType();
			File file = uploadedFile.getFile();
			UserBlobs bl = new UserBlobs();
			InputStream inStream = null;
			try {
				inStream = new FileInputStream(file);
				byte[] bb = org.apache.commons.io.IOUtils.toByteArray(inStream);
				if(bb.length>Math.pow(2,18))
					throw new IOException("Large file size");
				bl.blobFile = bb;
				bl.name=fileName;
				bl.tag=Tags2.PERSONNEL_PHOTO;
				bl.extension=contentType;
				bl.owner= Users.find.byId(userId);
				bl.creationDate = new Date();
				bl.save();
				uploadResult = bl.id;
			} finally {
					IOUtils.closeQuietly(inStream);
				}
			return uploadResult;
	 	}
	}
}
