package models;

import play.data.format.Formats;
import play.data.validation.*;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.Min;

import javax.persistence.*;

import play.db.ebean.*;

import java.util.*;

@Entity
@Table(name = "installations")
public class Installations extends Model {

	private static final long serialVersionUID = 4900189982494649675L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Column(unique=true)
	@Constraints.Required
	@Formats.NonEmpty
	public String name;
	
	public String description;
	
	public String soilType;

	@Enumerated(EnumType.STRING)
	public Corrosiveness soilCorrosiveness;

	public String earthquakeZone;	
	
	@Min(0)
	@Max(100)
	public Float minHumidity;
	
	@Min(0)
	@Max(100)
	public Float maxHumidity;
	public Float minTemperature;
	public Float maxTemperature;
	
	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();

	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@OneToMany(mappedBy="installation")
	public List<Plants> plants = new ArrayList<Plants>();
	
	
	// Query creation helper
	public static Model.Finder<Long, Installations> find = new Model.Finder<Long, Installations>(
			Long.class, Installations.class);

	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Installations c : Installations.find.all()) {
			options.put(c.id.toString(), c.name);
		}
		return options;
	}

}
