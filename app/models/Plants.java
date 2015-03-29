package models;

import play.data.format.Formats;
import play.data.validation.*;
import javax.persistence.*;

import com.avaje.ebean.Expr;
import com.avaje.ebean.Page;

import play.db.ebean.*;
import java.util.*;

@Entity
@Table(name="plants")
public class Plants extends Model {
	
	private static final long serialVersionUID = 6289835859259509461L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Column(unique=true)
	@Constraints.Required
	@Formats.NonEmpty
	public String name;
	
	public String description;

	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();

	@ManyToOne
	public Installations installation;
	
	@OneToMany(mappedBy="plant")
	public List<Sections> sections = new ArrayList<Sections>();


	//Query creation helper
	public static Model.Finder<Long,Plants> find =
		new Model.Finder<Long,Plants>(Long.class, Plants.class);	
	
	public static Map<String,String> options(Long id) {
    	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
    	for(Plants c: Plants.find.where().eq("installation.id", id).findList()) {
        	options.put(c.id.toString(), c.name);
    	}
    	return options;
	}
		
	/**
	 * Returns a page of plants for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the plants by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for plants, both name and description are used for
	 *            search
	 * @return Page<Plants>: A page of Plants
	 */
	public static Page<Plants> page(int page, int pageSize, String sortBy,
			String order, String filter) {
		return find	.where().or(
					Expr.ilike("name", "%" + filter + "%"),
					Expr.ilike("description", "%" + filter + "%"))
				.orderBy(sortBy + " " + order)
				.findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}
}
