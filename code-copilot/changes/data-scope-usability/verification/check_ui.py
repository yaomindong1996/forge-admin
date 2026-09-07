import json
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

OUTPUT = Path(__file__).resolve().parent
records = [
    dict(id="101", resourceName="用户管理", resourceCode="system:user:list", enabled=1, userIdColumn="create_by", orgIdColumn="org_id", tenantIdColumn="tenant_id", tableAlias="u", mapperMethod="com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectUserPage", remark="按角色授权限制用户数据"),
    dict(id="102", resourceName="组织管理", resourceCode="system:org:list", enabled=1, userIdColumn="create_by", orgIdColumn="org_id", regionCodeColumn="region_code", tableAlias="t", mapperMethod="com.mdframe.forge.plugin.system.mapper.SysOrgMapper.selectOrgList", remark="支持本组织及下级组织范围"),
    dict(id="103", resourceName="流程监控", resourceCode="flow:monitor:list", enabled=0, userIdColumn="create_by", orgIdColumn="org_id", flowRelatedVisible=1, mapperMethod="com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper.selectMonitorBusinessPage", remark="暂时停用，配置仍保留"),
]
calls = []
fail_refresh = False


def mock_api(route):
    global fail_refresh
    request = route.request
    url = request.url.split("/verification-api", 1)[-1].split("?", 1)[0]
    body = request.post_data_json if request.post_data else {}
    calls.append((url, body))
    print("API", url, flush=True)
    data = []
    if "/dict/data/type/" in url:
        data = [dict(dictLabel="启用", dictValue="1", dictSort=1), dict(dictLabel="禁用", dictValue="0", dictSort=2)]
    elif url.endswith("/resource/tree"):
        data = [dict(resourceType=2, resourceName=row["resourceName"], perms=row["resourceCode"]) for row in records]
    elif url.endswith("/page"):
        data = dict(records=records, total=len(records), current=1, size=10)
    elif url.endswith("/status"):
        row = next(item for item in records if item["id"] == body["id"])
        assert row["enabled"] == body["expectedEnabled"]
        row["enabled"] = body["enabled"]
    elif url.endswith("/getById"):
        data = records[0].copy()
    elif url.endswith("/refreshCache") and fail_refresh:
        route.fulfill(json=dict(code=500, message="刷新数据权限失败，请重试"))
        return
    elif url.endswith("/edit"):
        assert "enabled" not in body, "普通编辑不能覆盖规则状态"
        assert body["mapperMethod"], "折叠技术配置后仍须保留后台查询方法"
        records[0].update(body)
    route.fulfill(json=dict(code=200, data=data))


with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport=dict(width=1440, height=900), device_scale_factor=1)
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.on("pageerror", lambda error: print("PAGE ERROR", str(error), flush=True))
    page.on("console", lambda message: print("CONSOLE", message.text[:1000], flush=True) if message.type == "error" else None)
    page.route("**/verification-api/**", mock_api)
    page.goto("http://127.0.0.1:5187", wait_until="networkidle", timeout=120000)
    page.screenshot(path=str(OUTPUT / "initial.png"), full_page=True)
    print(page.locator("body").inner_text()[:1800])
    switch = page.get_by_role("switch", name="用户管理的数据权限规则")
    expect(switch).to_have_attribute("aria-checked", "true")
    page.screenshot(path=str(OUTPUT / "list-light.png"), full_page=True)
    page.get_by_role("button", name="刷新列表", exact=True).click()
    expect(switch).to_have_attribute("aria-checked", "true")
    assert not any(url.endswith("/refreshCache") for url, _ in calls)
    switch.click()
    expect(page.get_by_text("禁用后，此规则将停止限制数据可见范围", exact=False)).to_be_visible()
    page.screenshot(path=str(OUTPUT / "disable-confirm.png"), full_page=True)
    page.get_by_role("button", name="取消", exact=True).click()
    expect(switch).to_have_attribute("aria-checked", "true")
    assert not any(url.endswith("/status") for url, _ in calls)
    switch.click()
    page.get_by_role("button", name="确认禁用", exact=True).click()
    expect(switch).to_have_attribute("aria-checked", "false")
    switch.click()
    page.get_by_role("button", name="确认启用", exact=True).click()
    expect(switch).to_have_attribute("aria-checked", "true")
    page.get_by_role("button", name="刷新数据权限", exact=True).click()
    expect(page.get_by_role("status").filter(has_text="数据权限已刷新")).to_be_visible()
    fail_refresh = True
    page.get_by_role("button", name="刷新数据权限", exact=True).click()
    expect(page.get_by_role("status").filter(has_text="权限刷新未完成")).to_be_visible()
    fail_refresh = False
    page.get_by_text("编辑", exact=True).first.click()
    expect(page.get_by_text("技术配置", exact=True)).to_be_visible()
    expect(page.get_by_placeholder("如 t，查询无别名可留空")).not_to_be_visible()
    page.get_by_text("技术配置", exact=True).click()
    expect(page.get_by_placeholder("如 t，查询无别名可留空")).to_be_visible()
    page.evaluate("window.$message.destroyAll()")
    page.get_by_placeholder("如 t，查询无别名可留空").scroll_into_view_if_needed()
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUTPUT / "edit-technical.png"), full_page=True)
    page.get_by_role("button", name="确定", exact=True).click()
    expect(page.get_by_text("技术配置", exact=True)).not_to_be_visible()
    expect(page.get_by_role("status").filter(has_text="配置已保存")).to_be_visible()
    page.get_by_role("button", name="切换主题").click()
    page.evaluate("window.$message.destroyAll()")
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUTPUT / "list-dark.png"), full_page=True)
    page.set_viewport_size(dict(width=390, height=844))
    expect(switch).to_be_in_viewport()
    expect(page.get_by_placeholder("搜索页面名称")).to_be_visible()
    assert page.get_by_placeholder("搜索页面名称").bounding_box()["width"] > 180
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUTPUT / "list-mobile.png"), full_page=True)
    assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), "页面存在横向溢出"
    page.get_by_role("button", name="用户管理的更多操作", exact=True).click()
    page.get_by_text("编辑", exact=True).click()
    expect(page.get_by_text("技术配置", exact=True)).to_be_visible()
    page.get_by_role("button", name="取消", exact=True).click()
    page.get_by_role("button", name="新增规则", exact=True).click()
    expect(page.get_by_placeholder("如 t，查询无别名可留空")).to_be_visible()
    page.get_by_role("button", name="确定", exact=True).scroll_into_view_if_needed()
    drawer = page.locator('.n-drawer')
    assert drawer.bounding_box()["x"] >= 0
    assert drawer.bounding_box()["width"] <= 390
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUTPUT / "add-mobile.png"), full_page=True)
    assert not errors, errors
    print(json.dumps(dict(status="passed", browser_errors=errors, request_count=len(calls)), ensure_ascii=False))
    browser.close()
