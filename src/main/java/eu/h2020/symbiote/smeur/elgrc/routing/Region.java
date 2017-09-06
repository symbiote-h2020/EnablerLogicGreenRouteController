package eu.h2020.symbiote.smeur.elgrc.routing;

public class Region {
	private String name;
	private String file;
	
	public Region() {
		
	}
	
	public Region(String name, String file) {
		this.setName(name);
		this.setFile(file);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
