Coupon Management System



Overview
The Coupon Management System is a Spring Boot REST API that allows administrators to create, manage, and apply different types of coupons:
•	Cart-wise Coupons
•	Product-wise Coupons
•	Buy X Get Y (BxGy) Coupons
This project demonstrates robust coupon creation, validation, and persistence using Spring Boot, JPA, and an in-memory H2 Database.
Testing is performed using JUnit 5, Mockito, and Bruno for API validation.
Implemented Cases
1. Cart-wise Coupons
Description: Applies a discount to the entire cart when its total exceeds a threshold.
Logic Implemented:
•	Validates total cart value.
•	Applies a flat or percentage-based discount.
•	Stores threshold and discount details in the database.
2.  Product-wise Coupons
Description: Provides a discount on a specific product.
Logic Implemented:
•	Validates productId presence.
•	Applies discount only to the mentioned product.
3.  Buy X Get Y (BxGy) Coupons
Description: Grants a product for free or discounted when a specific quantity of another product is purchased.
Logic Implemented:
•	Configurable “Buy” and “Get” products with quantities.
•	Stores repetition limit and combinations in the DB.
4.  Coupon Expiry Date 
•	Default expiry = 1 month if not provided.
•	Coupon validity is checked before applying.


5.  Unit Tests 
•	Implemented using JUnit 5 and Mockito.
•	Covers all coupon creation and validation scenarios.
 Sample API Endpoints
1.1: Create Cart-Wise Coupon
POST /api/coupons
Request JSON:
{
  "type": "CART_WISE",
  "details": {
    "threshold": 100,
    "discount": 10
  }
}
1.2: Create Product-Wise Coupon
POST /api/coupons
Request JSON:
{
  "type": "PRODUCT_WISE",
  "details": {
    "productId": 1,
    "discount": 20
  } }
1.3: Create Buy X Get Y (BxGy) Coupon
POST /api/coupons
Request JSON:
{
  "type": "BXGY",
  "details": {
    "buyProducts": [
      { "productId": 1, "quantity": 3 },
      { "productId": 2, "quantity": 3 }
    ],
    "getProducts": [
      { "productId": 3, "quantity": 1 }
    ],
    "repetitionLimit": 2
  } }
2. Fetch all applicable coupons for a given cart and calculate the total discount that will be applied by each coupon
POST / api/coupons/applicable-coupons
Request JSON:
{
  "cart": {
    "items": [  {
        "productId": 1,
        "quantity": 6,
        "price": 50
}   ] } }
3. Apply a specific coupon to the cart and return the updated cart with discounted prices for each item.
POST /api/coupons/apply-coupon/{id}
Request JSON:
{ "cart": {
  "items": [  {
      "productId": 1,
      "quantity": 6,
      "price": 50
   }  ] }}
4. Update a specific coupon by its ID.
PUT /api/coupons/{id}
Request JSON:
{  "type": "CART_WISE",
  "details": {
    "threshold": 200,
    "discount": 15
  } }
 Other Endpoints
Method	Endpoint	Description
GET	/api/coupons	Retrieve all coupons
GET	/api/coupons/{id}	Retrieve a specific coupon by ID
DELETE	/api/coupons/{id}	Delete a coupon
 Unit Testing
Test Case	Description	Result
testAddCouponSuccess()	Verifies successful creation of all coupon types.	 Passed
testAddProductWiseCouponMissingProductId()	Throws exception if product ID is missing.	Passed
testApplyCouponSuccess()	Validates discount logic for all coupon types.	 Passed
testApplyCouponExpiredOrInactive()	Ensures expired/inactive coupons are rejected.	Passed
testApplyCouponInvalidCode()	Handles invalid coupon codes correctly.	Passed
testGetAllCoupons()	Retrieves all coupons via pagination.	Passed
testGetCouponByIdFound()	Fetches coupon details successfully.	Passed
testDeleteCouponSuccess()	Verifies coupon deletion.	Passed
testGetApplicableCoupons_BXGYLogic()	Ensures BxGy logic functions correctly.	Passed
testApplyCouponToCart()	End-to-end coupon application test.	Passed
Unimplemented Cases
Case	Description	Reason
User-specific coupons	Coupons linked to specific users	Authentication not implemented
Coupon usage tracking	Track number of times a coupon is used	Requires transactional linkage
Multiple coupon stacking	Apply more than one coupon per order	Business rule not finalized
Frontend support	UI to manage and apply coupons	Backend-only implementation
 Limitations
1.	No user authentication or authorization.
2.	Coupon validation is stateless.
 Assumptions
•	Product and cart services exist and are accessible.
•	Expiry dates follow yyyy-MM-dd format.
•	Discount values are valid positive numbers.
•	Each coupon type’s logic is independent.
•	Database schema matches entities (Coupon, BxGyDetail).
 Tech Stack
•	Language: Java 17
•	Framework: Spring Boot 3.5.7
•	Database: H2 (in-memory)
•	ORM: Spring Data JPA
•	Testing: JUnit 5, Mockito, Bruno 
•	Build Tool: Maven
 H2 Database Configuration
The project uses an in-memory H2 database for development and testing.
 H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb;
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
Enable H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
Access H2 Console:
http://localhost:8080/h2-console
 How to Run
1.	Clone the repository:
2.	git clone https://github.com/soumyalakshmi06/coupon-management-api.git
3.	Run the project:
4.	mvn spring-boot:run
5.	Access the APIs:
o	Bruno → http://localhost:8080/api/coupons
o	H2 Console → http://localhost:8080/h2-console
 Future Enhancements
•	JWT-based authentication and role-based access control.
•	Coupon usage count, redemption tracking.
•	Integration with e-commerce cart system.
•	Frontend dashboard using React or Angular.

