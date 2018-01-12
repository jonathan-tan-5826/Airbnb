import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Worker implements Runnable {
	private Connection _connection;
	private String _name;
	private String _checkInDate;
	private String _checkOutDate;
	private String _state;
	private int _month;
	private int _year;
	private List<CityZip> _cityZip;
	private String _proxyAddress;
	private String _version;
	private String _suffix;
	private static final String LISTING_XPATH = "//div[@class='_1788tsr0']";
	private static final String PRICE_BUTTON_CSS_SELECTOR = "button[aria-controls='menuItemComponent-price_range']";
	private static final String PRICE_BUTTON_CSS_SELECTOR2 = "button[aria-controls='menuItemComponent-price']";
	private static final String PRICE_TEXT_CLASS = "_150a3jym";
	private static final String PER_MONTH_SPAN_XPATH = "//span[.='Per month']";
	private static final String CITY_XPATH = "//*[@id=\"english-canonical-url\"]";
	private static final String PREFIX = "/s/";
	private static final char TOTAL_RETRIES = 2;
	private boolean _testIp;
	private boolean _didTestIp = false;

	public Worker(String name, String state, List<CityZip> cityZip, String proxyAddress, String checkInDate,
			String checkOutDate, int month, int year, String version, boolean testIp) {
		_name = name;
		_state = state;
		_cityZip = cityZip;
		_checkInDate = checkInDate;
		_checkOutDate = checkOutDate;
		_proxyAddress = proxyAddress;
		_month = month;
		_year = year;
		_version = version;
		_testIp = testIp;
		_suffix = "--" + _state;
	}

	public void run() {
		System.out.println("[" + _version + "] Running " + _name);
		try {
			System.out.println("[" + _name + "]: Using proxy address: " + _proxyAddress);
			DesiredCapabilities cap = GetDesiredCapabilities();
			WebDriver driver = new FirefoxDriver(cap);
			_connection = getConnection();

			for (int count = 0; count < _cityZip.size(); ++count) {
				if (!_didTestIp)
				{
					if (_testIp)
					{
						TestIpAddress(driver);
					}
					_didTestIp = true;
				}
				
				String city = _cityZip.get(count).getCity();
				String zipcode = _cityZip.get(count).getZipcode();
				crawlZipcode(driver, city, zipcode);
				
				System.out.println(_cityZip.size() - count - 1 + " zipcodes remaining.");
			}
			_connection.close();
			driver.quit();
		} catch (Exception e) {
			System.err.println("[" + _name + "] ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("[" + _name + "]: exiting.");
	}

	public void TestIpAddress(WebDriver driver) {
		driver.get("https://ipinfo.io");
		WebElement test = (new WebDriverWait(driver, 30)).until(
				ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/header/div/div/div[2]/div[1]/p")));
		System.out.println("[" + _name + "]: IpAddress = " + test.getText());
	}

	public DesiredCapabilities GetDesiredCapabilities() {
		Proxy proxy = new Proxy();
		proxy.setAutodetect(false);
		proxy.setProxyType(Proxy.ProxyType.MANUAL);
		proxy.setHttpProxy(_proxyAddress).setFtpProxy(_proxyAddress).setSslProxy(_proxyAddress);

		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference(FirefoxProfile.ALLOWED_HOSTS_PREFERENCE, "localhost");

		DesiredCapabilities cap = new DesiredCapabilities();
		cap.setCapability(CapabilityType.PROXY, proxy);
		cap.setCapability(FirefoxDriver.PROFILE, profile);

		return cap;
	}

	public void crawlZipcode(WebDriver driver, String city, String zipcode) throws Exception {
		boolean didCrawl = false;
		char crawlAttempts = 0;
		boolean isOldPriceText = false;
		String searchParameter = _state + " " + zipcode + ", United-States";
		String url = "https://www.airbnb.com/s/" + searchParameter + "/homes?checkin=" + _checkInDate + "&checkout="
				+ _checkOutDate;
		
		while (!didCrawl && (crawlAttempts < TOTAL_RETRIES))
		{
			try {
				System.out.println("[" + _name + "]: Crawling " + zipcode + ", " + _checkInDate + " - " + _checkOutDate);
				System.out.println(url);
	
				// Load Page
				driver.get(url);
				
				// Verify city
				(new WebDriverWait(driver, 15)).until(ExpectedConditions.presenceOfElementLocated(By.xpath(CITY_XPATH)));
				List<WebElement> cityTexts = driver.findElements(By.xpath(CITY_XPATH));
				if (cityTexts.size() > 0)
				{
					String foundCity = StringUtils.substringBetween(cityTexts.get(0).getAttribute("content").toString(), PREFIX, _suffix);
					if ((foundCity != null) && (foundCity.toLowerCase().replaceAll("-", " ").equals(city.toLowerCase()))) {
						System.out.println("[" + _name + "]:[Success] City verified.");
						
						// Verify there are at least 5 listings
						(new WebDriverWait(driver, 15))
								.until(ExpectedConditions.presenceOfElementLocated(By.xpath(LISTING_XPATH)));
						if (driver.findElements(By.xpath(LISTING_XPATH)).size() >= 5) {
							System.out.println("[" + _name + "]:[Success] Sufficient listings");
			
							// Verify period
							List<WebElement> foundPeriods = driver.findElements(By.xpath(PER_MONTH_SPAN_XPATH));
							if (foundPeriods.size() > 0) {
								System.out.println("[" + _name + "]:[Success] Period verified.");
									
								WebElement priceRangeButton = null;
								try {
									// Wait for Price Range Button Element
									priceRangeButton = GetWebElementByCSS(driver, PRICE_BUTTON_CSS_SELECTOR, 15);
								} catch (WebDriverException e) {
									// Price button did not show up on page despite there being listings. Try other CSS selector.
									System.err.println("[" + _name + "]: ERROR GETTING PRICE RANGE BUTTON1: " + url);
									System.err.println(e.getMessage());
									
									try {
										priceRangeButton = GetWebElementByCSS(driver, PRICE_BUTTON_CSS_SELECTOR2, 15);
										isOldPriceText = true;
									} catch (WebDriverException e2) {
										// Both CSS selectors failed, try again if crawlAttempts < TOTAL_RETRIES.
										System.err.println("[" + _name + "]: ERROR GETTING PRICE RANGE BUTTON2: " + url);
										System.err.println(e2.getMessage());
										
										crawlAttempts++;
										didCrawl = false;
										continue;
									}
								}
								
								// Click Price Button Element
								priceRangeButton.click();
								System.out.println("[" + _name + "]: Price button clicked.");
				
								// Wait for Price Text
								WebElement priceText = GetWebElementByClassName(driver, PRICE_TEXT_CLASS, 15);
								System.out.println("[" + _name + "]: priceText = " + priceText.getText());
								
								// Save PageSource
								String yearMonthDirectory = _year + "_" + String.format("%02d", _month);
								String modifiedCity = city.replaceAll(" ", "-");
								String fileName = zipcode + "_" + modifiedCity + "_" + _month + "_" + _year + ".txt";
								String directory = "./pagesources/" + yearMonthDirectory + "/" + _state + "/";
								writeStringToFile(directory, fileName, driver.getPageSource());
								
								// If its the old price text, verify the period
								if (isOldPriceText)
								{
									String foundPeriod = StringUtils.substringBetween(priceText.getText(), "per ", " for");
									if (!((foundPeriod != null) && (foundPeriod.toLowerCase().equals("month")))) {
										System.out.println("[" + _name + "][Error] Incorrect period, old text.");
										didCrawl = true;
										continue;
									} else {
										System.out.println("[" + _name + "][Success] Correct period, old text.");
									}
								}
						
								// Save to Database
								Airbnb airbnb = new Airbnb();
								airbnb.setCrawlTime(getCurrentTimestamp());
								airbnb.setZipcode(convertToInt(zipcode));
								airbnb.setCity(city);
								airbnb.setState(_state);
								airbnb.setUrl(url);
								airbnb.setMonth(_month);
								airbnb.setYear(_year);
								airbnb.setAveragePrice(convertToInt(getNumericalCharacters(priceText.getText())));
								airbnb.print();
				
								saveAirbnbToDatabase(airbnb);
							} else {
								System.out.println("[" + _name + "]:[Error] Incorrect period.");
							}
						} else {
							System.out.println("[" + _name + "]:[Error] Insufficient listings.");
						}
					} else {
						System.out.println("[" + _name + "]:[Error] Incorrect city.");
					}
				} else {
					System.out.println("[" + _name + "]:[Error] No city found.");
				}
	
				System.out.println("[" + _name + "]: Finished crawling " + zipcode + ", " + _checkInDate + " - " + _checkOutDate);
			} catch (WebDriverException e) {
				System.err.println("[" + _name + "]: ERROR: " + e.getMessage());
				System.err.println(url);
				return;
			}
			didCrawl = true;
		}
	}

	public WebElement GetWebElementByClassName(WebDriver driver, String locator, int time) {
		return (new WebDriverWait(driver, 15))
				.ignoring(NoSuchElementException.class)
				.ignoring(StaleElementReferenceException.class)
				.until(ExpectedConditions.presenceOfElementLocated(By.className(locator)));
	}

	public WebElement GetWebElementByCSS(WebDriver driver, String locator, int time) {
		return (new WebDriverWait(driver, 15))
				.ignoring(NoSuchElementException.class)
				.ignoring(StaleElementReferenceException.class)
				.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(locator)));
	}

	/**
	 * @title getNumericalCharacters
	 * @param value<String>
	 * @return value<String> with numerical characters only
	 */
	public String getNumericalCharacters(String value) {
		return value.replaceAll("[^0-9]", "");
	}

	/**
	 * @title convertToInt
	 * @param value<String>
	 * @return Converts value<String> to an integer after removing all non-numerical
	 *         characters
	 */
	public int convertToInt(String value) {
		String filteredValue = getNumericalCharacters(value);

		if (filteredValue.length() > 0) {
			return Integer.parseInt(filteredValue);
		} else {
			return 0;
		}
	}

	/**
	 * @title writeStringToFile
	 * @param fileName<String>,
	 *            text<String>
	 * @return
	 * @desc Creates fileName, if it does not exist, in /pagesources and writes text
	 *       to file.
	 */
	public void writeStringToFile(String directory, String fileName, String text) throws Exception {
		System.out.println("[" + _name + "]: writeStringToFile.");
		String fullFileName = directory + fileName;

		File file = new File(fullFileName);
		FileUtils.writeStringToFile(file, text, "UTF-8");
	}

	/**
	 * @title saveAirbnbToDatabase
	 * @param airbnb<Airbnb>
	 * @throws Exception
	 * @desc Inserts airbnb's data to database
	 */
	public void saveAirbnbToDatabase(Airbnb airbnb) throws Exception {
		System.out.println("[" + _name + "] Entered saveAirbnbToDatabase");

		String sqlStatement = "INSERT INTO airbnb(zipcode, average_price, month, year, url, crawl_time) VALUES(?,?,?,?,?,?)";
		PreparedStatement preparedStatement = _connection.prepareStatement(sqlStatement);
		// 1
		preparedStatement.setInt(1, airbnb.getZipcode());
		// 2
		preparedStatement.setInt(2, airbnb.getAveragePrice());
		// 3
		preparedStatement.setInt(3, airbnb.getMonth());
		// 4
		preparedStatement.setInt(4, airbnb.getYear());
		// 5
		preparedStatement.setString(5, airbnb.getUrl());
		// 6
		preparedStatement.setTimestamp(6, airbnb.getCrawlTime());
		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	/**
	 * @title getConnection
	 * @param
	 * @return connection<Connection> to MySQL database where data will be stored
	 */
	private Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String urldb = "jdbc:mysql://localhost/homeDB";
		String user = "jonathan";
		String password = "password";

		Connection connection = DriverManager.getConnection(urldb, user, password);
		return connection;
	}

	/**
	 * @title getCurrentTimestamp
	 * @param
	 * @return currentTimestamp<Timestamp>
	 */
	private Timestamp getCurrentTimestamp() {
		System.out.println("[" + _name + "] Entered getCurrentTimestamp");

		Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		return currentTimestamp;
	}
}