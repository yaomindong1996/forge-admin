import json
import mimetypes
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from playwright.sync_api import sync_playwright


BASE_URL = "http://forge-ui.local/forge"
DIST_DIR = Path(__file__).resolve().parents[3] / "forge-admin-ui" / "dist"
SCREENSHOTS = {
    "desktop": "/private/tmp/job-workbench-1366.png",
    "wide": "/private/tmp/job-workbench-1920.png",
    "mobile": "/private/tmp/job-workbench-390.png",
    "list": "/private/tmp/job-list-1366.png",
}


def response(data=None, code=200, msg="success"):
    return {"code": code, "data": data, "msg": msg}


def menu_data():
    return [
        {
            "id": 10,
            "resourceType": 1,
            "resourceName": "系统管理",
            "path": "/system",
            "visible": 1,
            "menuStatus": 1,
            "sort": 1,
            "children": [
                {
                    "id": 20,
                    "parentId": 10,
                    "resourceType": 2,
                    "resourceName": "定时任务",
                    "path": "/system/job-config",
                    "component": "system/job-config",
                    "visible": 1,
                    "menuStatus": 1,
                    "sort": 1,
                    "children": [
                        {
                            "id": 21,
                            "parentId": 20,
                            "resourceType": 2,
                            "resourceName": "新建定时任务",
                            "path": "/system/job-config/editor",
                            "component": "system/job-config.editor",
                            "visible": 0,
                            "menuStatus": 1,
                            "sort": 90,
                        },
                        {
                            "id": 22,
                            "parentId": 20,
                            "resourceType": 2,
                            "resourceName": "编辑定时任务",
                            "path": "/system/job-config/editor/:id",
                            "component": "system/job-config.editor.[id]",
                            "visible": 0,
                            "menuStatus": 1,
                            "sort": 91,
                        },
                    ],
                }
            ],
        }
    ]


def dict_data(dict_type):
    values = {
        "sys_job_status": [("停用", "0", "warning"), ("启用", "1", "success")],
        "sys_job_run_mode": [
            ("任务处理器", "HANDLER", "success"),
            ("本地服务方法", "BEAN", "info"),
            ("远程服务", "RPC", "warning"),
        ],
        "sys_job_sync_status": [
            ("等待同步", "PENDING", "warning"),
            ("同步成功", "SYNCED", "success"),
            ("同步失败", "FAILED", "error"),
        ],
    }
    return [
        {
            "dictLabel": label,
            "dictValue": value,
            "dictSort": index,
            "listClass": style,
            "dictStatus": 1,
        }
        for index, (label, value, style) in enumerate(values.get(dict_type, []), 1)
    ]


def mock_api(request_log, fail_update=False):
    def handler(route, request):
        parsed = urlparse(request.url)
        path = parsed.path
        query = parse_qs(parsed.query)
        status = 200
        payload = response()

        if path.endswith("/crypto/config"):
            payload = response(
                {
                    "enabled": False,
                    "enableApiCrypto": False,
                    "enableFieldCrypto": False,
                    "enableDynamicKey": False,
                    "enableReplay": False,
                }
            )
        elif path.endswith("/auth/userInfo"):
            payload = response(
                {
                    "userId": 1,
                    "username": "admin",
                    "realName": "管理员",
                    "tenantId": 1,
                    "tenantName": "默认租户",
                    "tenantIds": [1],
                    "activeOrgId": 1,
                    "activeOrgName": "平台管理部",
                    "roleIds": [1],
                    "roleKeys": ["admin"],
                    "permissions": ["*:*:*"],
                    "admin": True,
                    "tenantAdmin": True,
                }
            )
        elif path.endswith("/system/tenant/userTenantConfig"):
            payload = response(
                {
                    "tenantId": 1,
                    "systemName": "Forge Admin",
                    "browserTitle": "Forge Admin",
                    "systemLayout": "top-menu",
                    "systemTheme": "#2563eb",
                }
            )
        elif path.endswith("/auth/current/menu"):
            payload = response(menu_data())
        elif path.endswith("/system/dict/data/list"):
            payload = response(dict_data(query.get("dictType", [""])[0]))
        elif path.endswith("/job/config/executors"):
            payload = response(
                [
                    {
                        "code": "inventoryCloseHandler",
                        "displayName": "库存日结处理器",
                        "description": "每日汇总库存流水并生成日结结果",
                        "group": "INVENTORY",
                        "source": "JobHandler",
                        "executeMode": "HANDLER",
                    },
                    {
                        "code": "orderSyncHandler",
                        "displayName": "订单同步处理器",
                        "description": "同步待处理订单到业务系统",
                        "group": "ORDER",
                        "source": "ScheduledJob",
                        "executeMode": "HANDLER",
                    },
                ]
            )
        elif path.endswith("/job/config/cron/preview"):
            expression = (request.post_data_json or {}).get("cronExpression", "")
            payload = response(
                {
                    "cronExpression": expression,
                    "description": "工作日每天 02:15 执行"
                    if expression == "0 15 2 ? * MON-FRI"
                    else "每天 02:00 执行",
                    "nextFireTimes": [
                        "2026-07-20T02:00:00",
                        "2026-07-21T02:00:00",
                        "2026-07-22T02:00:00",
                        "2026-07-23T02:00:00",
                        "2026-07-24T02:00:00",
                    ],
                }
            )
        elif path.endswith("/job/config/page"):
            payload = response(
                {
                    "records": [
                        {
                            "id": 9,
                            "jobName": "库存日结",
                            "jobGroup": "INVENTORY",
                            "description": "每日汇总库存流水",
                            "executeMode": "HANDLER",
                            "executorHandler": "inventoryCloseHandler",
                            "executionSummary": "库存日结处理器",
                            "cronExpression": "0 0 2 * * ?",
                            "scheduleSummary": "每天 02:00 执行",
                            "status": 1,
                            "syncStatus": "SYNCED",
                            "nextFireTime": "2026-07-20T02:00:00",
                            "lastExecutionStatus": 1,
                            "lastExecutionTime": "2026-07-19T02:00:03",
                        }
                    ],
                    "total": 1,
                }
            )
        elif path.endswith("/job/config/9") and request.method == "GET":
            payload = response(
                {
                    "id": 9,
                    "jobName": "库存日结",
                    "jobGroup": "INVENTORY",
                    "description": "每日汇总库存流水",
                    "executeMode": "HANDLER",
                    "executorHandler": "inventoryCloseHandler",
                    "cronExpression": "0 15 2 ? * MON-FRI",
                    "jobParam": "{\"warehouseId\":1001}",
                    "status": 1,
                    "retryCount": 0,
                    "version": 3,
                    "syncStatus": "SYNCED",
                }
            )
        elif path.endswith("/job/config") and request.method == "POST":
            request_log.append({"createPayload": request.post_data_json})
            payload = response({"id": 10})
        elif path.endswith("/job/config") and request.method == "PUT":
            request_log.append({"updatePayload": request.post_data_json})
            if fail_update:
                status = 500
                payload = response(None, 500, "配置已保存，但调度同步失败：调度服务暂不可用")
        elif path.endswith("/job/log/clean"):
            payload = response(12)
        else:
            payload = response(None)

        route.fulfill(
            status=status,
            content_type="application/json; charset=utf-8",
            body=json.dumps(payload, ensure_ascii=False),
        )

    return handler


def new_page(browser, viewport, request_log, fail_update=False):
    context = browser.new_context(viewport=viewport, locale="zh-CN")
    context.add_init_script(
        "localStorage.setItem('prod_auth', JSON.stringify({accessToken: 'ui-test-token'}));"
    )
    page = context.new_page()
    api_handler = mock_api(request_log, fail_update=fail_update)

    def app_handler(route, request):
        path = urlparse(request.url).path
        if path.startswith("/forge-api/"):
            api_handler(route, request)
            return
        relative_path = path.removeprefix("/forge/")
        target = DIST_DIR / relative_path
        if not target.is_file():
            target = DIST_DIR / "index.html"
        content_type = mimetypes.guess_type(target.name)[0] or "application/octet-stream"
        route.fulfill(status=200, content_type=content_type, body=target.read_bytes())

    page.route("http://forge-ui.local/**", app_handler)
    return context, page


def wait_for_app(page, selector):
    page.wait_for_load_state("networkidle")
    page.locator(selector).wait_for(state="visible", timeout=30000)


def layout_metrics(page):
    return page.evaluate(
        """
        () => {
          const visible = element => {
            const style = getComputedStyle(element)
            const rect = element.getBoundingClientRect()
            return style.display !== 'none' && style.visibility !== 'hidden'
              && rect.width > 0 && rect.height > 0
          }
          return {
            viewportWidth: innerWidth,
            rootScrollWidth: document.documentElement.scrollWidth,
            bodyScrollWidth: document.body.scrollWidth,
            horizontalOverflow: document.documentElement.scrollWidth > innerWidth + 1,
            clippedButtons: [...document.querySelectorAll('button')]
              .filter(visible)
              .filter(element => element.scrollWidth > element.clientWidth + 1)
              .map(element => element.textContent.trim()),
          }
        }
        """
    )


def run():
    results = {"screenshots": SCREENSHOTS, "checks": {}}
    request_log = []
    console_errors = []
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)

        context, page = new_page(browser, {"width": 1366, "height": 768}, request_log)
        page.on(
            "console",
            lambda message: console_errors.append(message.text)
            if message.type == "error"
            else None,
        )
        page.goto(f"{BASE_URL}/system/job-config/editor", wait_until="domcontentloaded")
        wait_for_app(page, ".job-workbench")
        assert page.locator(".job-config-page").count() == 0
        page.locator('input[placeholder="例如：库存日结"]').fill("库存日结")
        page.locator('input[placeholder="例如：INVENTORY"]').fill("INVENTORY")
        page.locator('textarea[placeholder="简要说明这个任务负责什么"]').fill(
            "每日汇总库存流水并生成日结结果"
        )
        page.get_by_text("搜索并选择任务处理器", exact=True).click()
        page.get_by_text("库存日结处理器", exact=True).last.click()
        page.get_by_text("每天 02:00 执行", exact=True).first.wait_for()
        assert page.get_by_text("保存后停用", exact=True).count() >= 1
        page.screenshot(path=SCREENSHOTS["desktop"], full_page=True)
        results["checks"]["desktop1366"] = layout_metrics(page)
        page.get_by_role("button", name="保存任务").first.click()
        page.wait_for_url(f"{BASE_URL}/system/job-config")
        wait_for_app(page, ".job-config-page")
        page.get_by_text("立即运行", exact=True).wait_for()
        page.get_by_text("查看日志", exact=True).wait_for()
        assert page.get_by_text("0 0 2 * * ?", exact=True).count() == 0
        page.get_by_role("button", name="更多").click()
        page.get_by_text("清空全部日志", exact=True).click()
        page.get_by_text("确定清空全部任务运行日志吗？此操作不可恢复。", exact=True).wait_for()
        page.get_by_role("button", name="取消").last.click()
        page.screenshot(path=SCREENSHOTS["list"], full_page=True)
        create_payload = next(
            item["createPayload"] for item in request_log if "createPayload" in item
        )
        assert create_payload["status"] == 0
        assert create_payload["executorHandler"] == "inventoryCloseHandler"
        assert create_payload["cronExpression"] == "0 0 2 * * ?"
        results["checks"]["createPayload"] = create_payload
        context.close()

        context, page = new_page(
            browser, {"width": 1920, "height": 1080}, request_log, fail_update=True
        )
        page.goto(f"{BASE_URL}/system/job-config/editor/9", wait_until="domcontentloaded")
        wait_for_app(page, ".job-workbench")
        assert page.locator(".job-config-page").count() == 0
        cron_input = page.locator('input[placeholder^="Quartz 6 段表达式"]')
        assert cron_input.input_value() == "0 15 2 ? * MON-FRI"
        assert page.locator('input[placeholder="例如：库存日结"]').is_disabled()
        assert page.locator(".future-list li").count() == 5
        page.screenshot(path=SCREENSHOTS["wide"], full_page=True)
        results["checks"]["wide1920"] = layout_metrics(page)
        page.locator('textarea[placeholder="简要说明这个任务负责什么"]').fill(
            "每日汇总库存流水和库存余额"
        )
        page.get_by_role("button", name="取消").first.click()
        page.get_by_text("未保存变更", exact=True).wait_for()
        page.get_by_role("button", name="继续编辑").click()
        assert page.url.endswith("/system/job-config/editor/9")
        page.get_by_role("button", name="保存任务").first.click()
        page.get_by_text("配置已保存，调度同步失败", exact=True).last.wait_for()
        assert page.get_by_role("button", name="保存任务").first.is_disabled()
        results["checks"]["complexCronPreserved"] = cron_input.input_value()
        results["checks"]["partialSaveVisible"] = True
        context.close()

        context, page = new_page(browser, {"width": 390, "height": 844}, request_log)
        page.goto(f"{BASE_URL}/system/job-config/editor", wait_until="domcontentloaded")
        wait_for_app(page, ".job-workbench")
        assert page.locator(".mobile-actions").is_visible()
        assert not page.get_by_role("button", name="保存任务").first.is_visible()
        assert page.locator(".section-nav button").count() == 4
        page.get_by_role("button", name="执行计划").click()
        page.screenshot(path=SCREENSHOTS["mobile"], full_page=True)
        results["checks"]["mobile390"] = layout_metrics(page)
        context.close()
        browser.close()

    results["consoleErrors"] = console_errors
    print(json.dumps(results, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    run()
