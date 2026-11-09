package tests;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.ExtentReportManager;

import static io.restassured.RestAssured.given;

/**
 * REST Assured checks next to the UI suite. ApiDemos has no backend of its own, so the
 * public demo API jsonplaceholder.typicode.com stands in for one; in a real project the
 * same pattern verifies the app's own API.
 */
public class ApiIntegratedTests {

    ExtentTest test;
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
    }

    // 1. API Response Status Code যাচাই (basic health check)
    @Test(priority = 1)
    public void api_GetUserReturnsStatus200() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/users/1");

        test.log(Status.INFO, "Status code: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 200, "API did not return 200 OK");
    }

    // 2. API Response এর নির্দিষ্ট field (যেমন username) যাচাই — UI তে দেখানো ডেটার সাথে মিলবে এমন
    @Test(priority = 2)
    public void api_UserResponseContainsExpectedFields() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/users/1");

        String username = response.jsonPath().getString("username");
        String email = response.jsonPath().getString("email");

        test.log(Status.INFO, "Username: " + username + " | Email: " + email);
        Assert.assertEquals(response.jsonPath().getInt("id"), 1);
        Assert.assertNotNull(username, "Username field missing in API response");
        Assert.assertFalse(username.isBlank(), "Username is blank");
        Assert.assertTrue(email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"), "Email field appears invalid: " + email);
    }

    // 3. POST: the API creates a resource and echoes back what was sent
    @Test(priority = 3)
    public void api_CreatePostReturns201WithSentData() {
        Response response = given()
                .baseUri(BASE_URL)
                .contentType("application/json")
                .body("{\"title\": \"ApiDemos check\", \"body\": \"from Appium suite\", \"userId\": 7}")
                .when()
                .post("/posts");

        test.log(Status.INFO, "Create response: " + response.asString());
        Assert.assertEquals(response.getStatusCode(), 201, "Expected 201 Created");
        Assert.assertEquals(response.jsonPath().getString("title"), "ApiDemos check");
        Assert.assertEquals(response.jsonPath().getInt("userId"), 7);
        Assert.assertTrue(response.jsonPath().getInt("id") > 0, "New resource should get an id");
    }

    // 4. Negative API Test: ভুল/অস্তিত্বহীন resource এর জন্য সঠিক error code আসে কিনা
    @Test(priority = 4)
    public void api_NonExistentResourceReturns404() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/users/999999");

        test.log(Status.INFO, "Status code for non-existent user: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 404, "Expected 404 for non-existent resource");
    }
}