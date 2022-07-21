package qa.avasilev;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import qa.avasilev.config.Project;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byTagAndText;
import static com.codeborne.selenide.Selenide.*;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static qa.avasilev.helpers.AllureRestAssuredFilter.withCustomTemplates;

public class DemoWebShopTests extends TestBase {

    static String login = Project.config.login(),
            password = Project.config.password(),
            authCookieName = "NOPCOMMERCE.AUTH",
            authCookieValue,
            productId;

    @Test
    @DisplayName("Successful authorization to some demowebshop (UI)")
    void loginTest() {
        step("Open login page", () ->
                open("/login"));

        step("Fill login form", () -> {
            $("#Email").setValue(login);
            $("#Password").setValue(password)
                    .pressEnter();
        });

        step("Verify successful authorization", () ->
                $(".account").shouldHave(text(login)));
    }

    @Test
    @DisplayName("Successful authorization to some demowebshop (API + UI)")
    void loginWithApiTest() {

        step("Authorize via API and get the cookie", () -> {
            authCookieValue = given()
                    .filter(withCustomTemplates())
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("Email", login)
                    .formParam("Password", password)
                    .log().all()
                    .when()
                    .post("/login")
                    .then()
                    .log().all()
                    .statusCode(302)
                    .extract().cookie(authCookieName);
        });

        step("Open minimal content, because cookie can be set when site is opened", () ->
                open("/Themes/DefaultClean/Content/images/logo.png"));

        step("Set cookie to to browser", () -> {
            Cookie authCookie = new Cookie(authCookieName, authCookieValue);
            WebDriverRunner.getWebDriver().manage().addCookie(authCookie);
        });

        step("Open main page", () ->
                open(""));

        step("Verify successful authorization", () ->
                $(".account").shouldHave(text(login)));
    }

    @Test
    @DisplayName("User is able to add product to cart and then delete it")
    void addToCart() {

        SelenideElement productLink = $$(byTagAndText("a", "Computing and Internet"))
                .findBy(cssClass("product-name"));

        step("Authorize via API and get the cookie", () -> {
            authCookieValue = given()
                    .filter(withCustomTemplates())
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("Email", login)
                    .formParam("Password", password)
                    .log().all()
                    .when()
                    .post("/login")
                    .then()
                    .log().all()
                    .statusCode(302)
                    .extract().cookie(authCookieName);
        });

        step("Add product to cart via API", () -> {
            given()
                    .filter(withCustomTemplates())
                    .log().all()
                    .cookie(authCookieName, authCookieValue)
                    .when()
                    .post("/addproducttocart/catalog/13/1/1")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("success", is(true));
        });

        step("Open cart and check, that the product is there, also get it's id", () -> {

            step("Open minimal content, because cookie can be set when site is opened", () ->
                    open("/Themes/DefaultClean/Content/images/logo.png"));

            step("Set cookie to to browser", () -> {
                Cookie authCookie = new Cookie(authCookieName, authCookieValue);
                WebDriverRunner.getWebDriver().manage().addCookie(authCookie);
            });

            open("/cart");
            assertNotEquals("(0)", $(".cart-qty").text());
            productId = productLink.parent().parent().$(".remove-from-cart").$("input").getValue();
            closeWebDriver();
        });

        step("Remove added product via API", () -> {
            given()
                    .filter(withCustomTemplates())
                    .cookie(authCookieName, authCookieValue)
                    .config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs("multipart/form-data", ContentType.TEXT)))
                    .urlEncodingEnabled(true)
                    .log().all()
                    .formParam("removefromcart", productId)
                    .formParam("itemquantity" + productId, 1)
                    .formParam("updatecart", "Update shopping cart")
                    .when()
                    .post("/cart")
                    .then()
                    .log().all()
                    .statusCode(200);
        });

        step("Open browser again and check, that the product is deleted", () -> {

            step("Open minimal content, because cookie can be set when site is opened", () ->
                    open("/Themes/DefaultClean/Content/images/logo.png"));

            step("Set cookie to to browser", () -> {
                Cookie authCookie = new Cookie(authCookieName, authCookieValue);
                WebDriverRunner.getWebDriver().manage().addCookie(authCookie);
            });

            open("/cart");
            productLink.shouldNot(exist);
        });
    }
}
