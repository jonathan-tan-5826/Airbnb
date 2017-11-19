# Airbnb Crawler

A multi-threaded crawler written in Java to obtain the average price per month per zipcode for renting homes through Airbnb.

Technologies used:
Selenium-Java 2.53
Maven 3.0.2
MySQL-Connector-Java 5.1.6

General explanation:
1) Pass in a state abbreviation or group number (1-10), month, year, and desired number of threads (Ex: java -jar airbnb_crawler_v2.jar CA 10 2017 10)
2) Queries the database for all zipcodes in that state, along with their corresponding cities, and distributes the CityZip amongst X buckets (X = passed in desired number of threads).
3) For each bucket with CityZips, a worker thread is assigned to crawl Airbnb for each zipcode in the bucket for the month and year passed in.

Inside the worker threads:
1) Worker calculates the check-in date and check-out date based on the month and year passed in.
2) For each zipcode the worker performs the following...
3) Opens a Firefox Web Driver
4) Constructs the URL & loads the page in the driver
5) Verifies there are at least 5 listings on the page. If not, the worker closes the driver and continues to the next zipcode.
6) Saves the page source
7) Waits for price range button to appear. If it does not appear in time, program errors out and exits. Otherwise, the button is pressed.
8) Waits for price-text to appear to obtain the average price.
9) Verifies the period (Makes sure the average price is for monthly). If the average price is not for the month, the worker closes the driver and continues to the next zipcode.
10) Verifies the city listed on the webpage matches the zipcode's city. If not, the worker closes the driver and continues to the next zipcode.
11) Saves information to database.
12) Closes the Firefox Web Driver and restarts at step 3 with the next zipcode.

