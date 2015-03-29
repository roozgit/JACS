package models;

import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;

import java.util.*;

@Entity
@Table(name="user_blobs")
public class UserBlobs extends Model {

	private static final long serialVersionUID = 2108153792158813163L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;

	@Required
	@Formats.NonEmpty
	public String name;
	
	@Enumerated(EnumType.STRING)
	@Required
	public Tags2 tag;

	@Required
	public Date creationDate;
	
	@Required
	public String extension;
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Users owner;
	
	@Lob
	public byte[] blobFile;

	public static Finder<Long,UserBlobs> find = new Finder<Long,UserBlobs>(Long.class,UserBlobs.class);

	public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(UserBlobs bls: UserBlobs.find.all()) {
            	options.put(bls.id.toString(), bls.tag+": "+bls.name);
        	}
        	return options;
	}

}
