package models;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@Table(name="blobs")
public class Blobs extends Model {
	
	private static final long serialVersionUID = 4103230299829984408L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;
	
	@Formats.NonEmpty
	public String name;
	
	@Required
	@Enumerated(EnumType.STRING)
	public Tags tag;

	public Date creationDate;
	
	@Constraints.Required
	public String extension;
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Users owner;
	
	@Required
	public String blobFile;

	public static Finder<Long,Blobs> find = new Finder<Long,Blobs>(Long.class,Blobs.class);
	
	public static Boolean isBlobOwner(Long blobId, String userName) {
		 return find.where()
		            .eq("owner.userName", userName)
		            .eq("id", blobId)
		            .findRowCount() > 0;
	}

	public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(Blobs bls: Blobs.find.all()) {
            	options.put(bls.id.toString(), bls.tag+": "+bls.name);
        	}
        	return options;
	}
	
	/**
	 * Returns a page of Blobs for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter
	 *            search
	 * @return Page<Blobs>
	 */
	public static Page<Blobs> page(int page, int pageSize, String sortBy,
			String order, String filter) {
		
		String whoql =
		"lower(name) like :filter OR " +
		"lower(extension) like :filter2 OR " +
		"lower(owner.userName) like :filter2 OR " +
		"lower(tag) like :filter";
	
		com.avaje.ebean.Query<Blobs> query = Ebean.createQuery(Blobs.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%").
		setParameter("filter2", filter+"%");
		
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

}
