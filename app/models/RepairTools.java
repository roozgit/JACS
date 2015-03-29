package models;

import java.util.*;

import play.data.format.Formats;
import play.data.validation.*;

import javax.persistence.*;

import com.avaje.ebean.Expr;
import com.avaje.ebean.Page;

import play.db.ebean.*;

@Entity
@Table(name="repairtools")
public class RepairTools extends Model {

	private static final long serialVersionUID = -8381625674710818751L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long id;
	
	@Constraints.Required
	@Formats.NonEmpty
	public String name;
	
	@ManyToOne
	public Companies manufacturerCompany;
	
	public String manufacturerModelDesignation;
	
	public String serialNo;
	
	@ManyToOne
	public Disciplines owner;
	
	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();
	
	@Column(columnDefinition = "TEXT")
	public String comments;
	
	//Query creation helper
		public static Model.Finder<Long,RepairTools> find = 
				new Model.Finder<Long,RepairTools>(Long.class,RepairTools.class);

		public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(RepairTools rt: find.all()) {
            	options.put(rt.id.toString(), rt.name);
        	}
        return options;
	}
	
		/**
		 * Returns a page of tools for display
		 * 
		 * @param page
		 *            Page number
		 * @param pageSize
		 *            Size of each page
		 * @param sortBy
		 *            parameter to sort the tools by, typically name
		 * @param order
		 *            order of sort
		 * @param filter
		 *            Search filter
		 *            search
		 * @return Page<Companies>: A page of Companies
		 */
		public static Page<RepairTools> page(int page, int pageSize, String sortBy,
				String order, String filter) {
			return find	.where().or(
						Expr.ilike("name", "%" + filter + "%"),
						Expr.ilike("serialNo", "%" + filter + "%"))
					.orderBy(sortBy + " " + order)
					.findPagingList(pageSize)
					.setFetchAhead(false).getPage(page);
		}
}
