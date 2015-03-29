package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@Table(name="parts")
public class Parts extends Model {
	
	private static final long serialVersionUID = -7570754737515811546L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Required
	@Formats.NonEmpty
	public String name;
	
	public String description;
	
	@ManyToOne
	public Companies originalVendor;
	
	@ManyToMany
	public List<Companies> manufacturerCompanies = new ArrayList<Companies>();
	
	public String mescCode;
	public String vendorCode;
	public String plantCode;
	public String assetCode;
	public String userCode;
	public String vendorDrawingNo;
	
	public String material1;
	public String material2;
	public String material3;
	
	public String storageLocation;
	
	public Float unitPrice;
	
	public Float minimumRequired;
	public Float maximumRequired;
	
	@Enumerated(EnumType.STRING)
	@Required
	public Currencies currency;
	
	@Min(0)
	public Float remainingQuantity=0F;
	
	@ManyToOne
	@Required
	public MeasurementUnits measurementUnit;
	
	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();
	
	public String validate() {
		if(remainingQuantity<0F)
			return "Stock can't be below zero";
		else
			return null;
	}
	
		//Query creation helper
	public static Model.Finder<Long,Parts> find = new Model.Finder<Long,Parts>(Long.class,Parts.class);

		public static List<Parts> findComponentParts(Long componentId) {
		List<Parts> rlist = new ArrayList<Parts>();
		rlist=Parts.find
				.where()
				.eq("components.id",componentId)
				.findList();
			return rlist;
		}

		public static Map<String,String> options() {
        	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        	for(Parts pa: find.where().isNotNull("id").orderBy("name").findList()) {
            	options.put(pa.id.toString(), pa.name);
        	}
        return options;
	}
		
	public static LinkedHashMap<Components,Float> findComponentsForThisPart(Long partId) {
		LinkedHashMap<Components,Float> rList = new LinkedHashMap<Components,Float>();
		for(PartsComponents pc : PartsComponents.find.where()
				.eq("part.id", partId).findList()) {
				rList.put(pc.component,pc.quantity);
		}
		return rList;
	}
		
	/**
	 * Returns a page of parts for display
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
	public static Page<Parts> page(int page, int pageSize, String sortBy,
			String order, String filter) {
		
		String whoql =
		"lower(name) like :filter OR " +
		"lower(description) like :filter OR " +
		"lower(mescCode) like :filter2 OR "+
		"lower(plantCode) like :filter2 OR "+
		"lower(assetCode) like :filter2";
	
		com.avaje.ebean.Query<Parts> query = Ebean.createQuery(Parts.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%").
		setParameter("filter2", filter+"%");
		
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

		
}
