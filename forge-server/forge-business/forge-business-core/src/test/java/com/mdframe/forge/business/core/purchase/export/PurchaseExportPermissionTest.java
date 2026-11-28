package com.mdframe.forge.business.core.purchase.export;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.fun.strategy.SaRouteMatchFunction;
import cn.dev33.satoken.spring.pathmatch.SaPathPatternParserUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.mdframe.forge.starter.auth.config.StpInterfaceImpl;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.mdframe.forge.business.core.purchase.export.PurchaseExportFixtureController.BASE_PATH;
import static com.mdframe.forge.business.core.purchase.export.PurchaseExportFixtureController.EXPORT_PERMISSION;
import static com.mdframe.forge.business.core.purchase.export.PurchaseExportFixtureController.XLSX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PurchaseExportFixture.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PurchaseExportPermissionTest {

    private static final String EXPORT_PATH = BASE_PATH + "/export";
    private static final String PAGE_PATH = BASE_PATH + "/page";
    private static final String PC_MENU = "/business/purchase-order-test";
    private static final String UPGRADE_SCRIPT = "V2__purchase_export_permission.sql";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;
    private SaTokenContext originalContext;
    private SaTokenDao originalDao;
    private StpInterface originalPermissions;
    private SaRouteMatchFunction originalRouteMatcher;

    @BeforeAll
    void enableLocalSessions() {
        originalContext = SaManager.getSaTokenContext();
        originalDao = SaManager.getSaTokenDao();
        originalPermissions = SaManager.getStpInterface();
        originalRouteMatcher = SaStrategy.instance.routeMatcher;
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
        SaManager.setStpInterface(new StpInterfaceImpl());
        SaStrategy.instance.routeMatcher = SaPathPatternParserUtil::match;
    }

    @AfterAll
    void restoreSessionConfiguration() {
        SaStrategy.instance.routeMatcher = originalRouteMatcher;
        SaManager.setStpInterface(originalPermissions);
        SaManager.setSaTokenDao(originalDao);
        SaManager.setSaTokenContext(originalContext);
    }

    @Test
    void procurementUserExportsTheSameRowsAsThePagedList() throws Exception {
        DocumentContext login = login(10);
        String token = login.read("$.token");
        assertMenu(token);
        assertThat(buttonPermissions(token)).containsExactly(EXPORT_PERMISSION);
        assertThat(strings(login, "$.apiPermissions")).containsExactlyInAnyOrder("GET " + PAGE_PATH, "POST " + EXPORT_PATH);

        DocumentContext first = json(getPage(token, 1, ""));
        DocumentContext second = json(getPage(token, 2, ""));
        assertThat(first.<Integer>read("$.total")).isEqualTo(2);
        assertThat(second.<Integer>read("$.total")).isEqualTo(2);
        List<String> orders = List.of(first.read("$.records[0].orderNo"), second.read("$.records[0].orderNo"));
        assertThat(orders).containsExactly("PO-A-02", "PO-A-01");
        assertThat(workbookOrders(post(EXPORT_PATH, token, "{}"))).containsExactlyElementsOf(orders);

        DocumentContext filtered = json(getPage(token, 1, "&supplierName=%E5%8D%8E%E5%8C%97"));
        assertThat(filtered.<Integer>read("$.total")).isEqualTo(1);
        assertThat(workbookOrders(post(EXPORT_PATH, token, "{\"supplierName\":\"华北\"}")))
                .containsExactly(filtered.<String>read("$.records[0].orderNo"));
    }

    @Test
    void anotherOrganizationWithTheSameRoleOnlyExportsItsOwnRows() throws Exception {
        String token = login(11).read("$.token");
        DocumentContext page = json(getPage(token, 1, ""));
        assertThat(page.<Integer>read("$.total")).isEqualTo(1);
        assertThat(page.<String>read("$.records[0].orderNo")).isEqualTo("PO-B-01");
        assertThat(workbookOrders(post(EXPORT_PATH, token, "{}"))).containsExactly("PO-B-01");
    }

    @ParameterizedTest
    @CsvSource({"12, false, false, PO-A-02", "13, true, false, PO-A-02",
            "14, false, true, PO-A-02", "20, false, false, PO-C-01"})
    void listAccessDoesNotGrantExport(long userId, boolean buttonGrant, boolean apiGrant, String firstOrder) throws Exception {
        DocumentContext login = login(userId);
        String token = login.read("$.token");
        assertMenu(token);
        assertThat(buttonPermissions(token).contains(EXPORT_PERMISSION)).isEqualTo(buttonGrant);
        assertThat(strings(login, "$.apiPermissions").contains("POST " + EXPORT_PATH)).isEqualTo(apiGrant);
        assertThat(json(getPage(token, 1, "")).<String>read("$.records[0].orderNo")).isEqualTo(firstOrder);
        assertDenied(post(EXPORT_PATH, token, "{}"), 403);
    }

    @Test
    void exportRequiresALoggedInUser() throws Exception {
        assertDenied(post(EXPORT_PATH, null, "{}"), 401);
    }

    @Test
    void exportDoesNotAcceptTheGetMethod() throws Exception {
        String token = login(10).read("$.token");
        HttpResponse<byte[]> response = send(request(EXPORT_PATH, token).GET());
        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Content-Disposition")).isEmpty();
    }

    @Test
    void flywayUpgradePreservesExistingGrantsAndMatchesFreshInstall() {
        EmbeddedDatabase installed = PurchaseExportFixture.emptyDatabase();
        EmbeddedDatabase clean = PurchaseExportFixture.emptyDatabase();
        try {
            Flyway baseline = PurchaseExportFixture.migration(installed).target("1").load();
            assertThat(baseline.migrate().migrationsExecuted).isEqualTo(1);
            Integer baselineChecksum = baseline.info().current().getChecksum();
            assertThat(grantCount(installed)).isEqualTo(15);
            Flyway upgrade = PurchaseExportFixture.migration(installed).load();
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(upgrade.info().applied()[0].getChecksum()).isEqualTo(baselineChecksum);

            Flyway fresh = PurchaseExportFixture.migration(clean).load();
            assertThat(fresh.migrate().migrationsExecuted).isEqualTo(2);
            for (Flyway migration : List.of(upgrade, fresh)) {
                migration.validate();
                assertThat(migration.migrate().migrationsExecuted).isZero();
                assertThat(migration.info().pending()).isEmpty();
            }

            new ResourceDatabasePopulator(new ClassPathResource(PurchaseExportFixture.MIGRATION_LOCATION
                    + "/" + UPGRADE_SCRIPT)).execute(installed);
            assertThat(grantCount(installed)).isEqualTo(17);
            assertThat(grantCount(clean)).isEqualTo(17);
            assertThat(exportGrantees(installed)).containsExactly(100L, 100L).isEqualTo(exportGrantees(clean));
        } finally {
            installed.shutdown();
            clean.shutdown();
        }
    }

    private int grantCount(EmbeddedDatabase database) {
        return new JdbcTemplate(database).queryForObject("SELECT COUNT(*) FROM sys_role_resource", Integer.class);
    }

    private List<Long> exportGrantees(EmbeddedDatabase database) {
        return new JdbcTemplate(database).queryForList("""
                SELECT grant_row.role_id FROM sys_role_resource grant_row
                JOIN sys_resource resource ON resource.id = grant_row.resource_id
                WHERE resource.perms IN ('business:purchaseOrderTest:export', 'business:purchaseOrderTest:exportApi')
                ORDER BY grant_row.role_id
                """, Long.class);
    }

    private DocumentContext login(long userId) throws IOException, InterruptedException {
        DocumentContext login = json(post("/auth/login?userId=" + userId, null, "{}"));
        assertThat(login.<String>read("$.token")).isNotBlank();
        return login;
    }

    private void assertMenu(String token) throws IOException, InterruptedException {
        DocumentContext menu = json(send(request("/auth/current/menu", token).GET()));
        assertThat(strings(menu, "$.data[*].path")).containsExactly("/business");
        assertThat(strings(menu, "$.data[0].children[*].path")).containsExactly(PC_MENU);
    }

    private List<String> buttonPermissions(String token) throws IOException, InterruptedException {
        return strings(json(send(request("/auth/current/permissions", token).GET())), "$.data");
    }

    private HttpResponse<byte[]> getPage(String token, int pageNum, String filters) throws IOException, InterruptedException {
        return send(request(PAGE_PATH + "?pageNum=" + pageNum + filters, token).GET());
    }

    private HttpResponse<byte[]> post(String path, String token, String body) throws IOException, InterruptedException {
        return send(request(path, token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private HttpRequest.Builder request(String path, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (token != null) {
            request.header(StpUtil.getTokenName(), token);
        }
        return request;
    }

    private HttpResponse<byte[]> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private DocumentContext json(HttpResponse<byte[]> response) {
        String body = new String(response.body(), StandardCharsets.UTF_8);
        assertThat(response.statusCode()).as(body).isEqualTo(200);
        return JsonPath.parse(body);
    }

    private List<String> strings(DocumentContext document, String path) {
        return document.read(path);
    }

    private void assertDenied(HttpResponse<byte[]> response, int businessCode) {
        assertThat(json(response).<Integer>read("$.code")).isEqualTo(businessCode);
        assertThat(response.headers().firstValue("Content-Disposition")).isEmpty();
    }

    private List<String> workbookOrders(HttpResponse<byte[]> response) throws IOException {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(value -> assertThat(value).startsWith(XLSX));
        assertThat(response.headers().firstValue("Content-Disposition")).hasValueSatisfying(value -> assertThat(value).startsWith("attachment"));
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(response.body()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("采购单号");
            List<String> orderNumbers = new ArrayList<>();
            for (int row = 1; row <= sheet.getLastRowNum(); row++) {
                orderNumbers.add(sheet.getRow(row).getCell(0).getStringCellValue());
            }
            return orderNumbers;
        }
    }
}
