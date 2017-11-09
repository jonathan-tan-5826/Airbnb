public class CityZip {
	private String _city;
	private String _zipcode;
	
	public CityZip(String city, String zipcode) {
		_city = city;
		_zipcode = zipcode;
	}
	
	public String getCity() {
		return _city;
	}
	
	public String getZipcode() {
		return _zipcode;
	}
	
	public void setCity(String value) {
		_city = value;
	}
	
	public void setZipcode(String value) {
		_zipcode = value;
	}
}
