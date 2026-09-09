import json
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from playwright.sync_api import expect, sync_playwright


OUTPUT = Path(__file__).resolve().parent
records = [
    dict(id="101", configKey="sys_user_export", configType="EXPORT", exportName="用户列表导出", sheetName="用户列表", fileNameTemplate="用户列表_{date}.xlsx", dataSourceBean="sysUserService", queryMethod="selectUserPage", autoTrans=True, pageable=True, maxRows=50000, sortField="create_time", sortOrder="DESC", status=1, includeSample=False, allowImport=False, remark="提供给人事管理员", createTime="2026-08-20 09:10:00", updateTime="2026-09-06 16:40:00"),
    dict(id="102", configKey="sys_user_import", configType="IMPORT", exportName="用户批量导入模板", sheetName="导入数据", fileNameTemplate="用户导入模板.xlsx", dataSourceBean=None, queryMethod=None, autoTrans=True, pageable=False, maxRows=None, status=1, includeSample=True, allowImport=True, remark="下载后按模板填写", createTime="2026-08-20 09:10:00", updateTime="2026-09-05 10:20:00"),
    dict(id="103", configKey="operation_log_export", configType="BOTH", exportName="操作记录导入导出", sheetName="操作记录", fileNameTemplate="操作记录_{date}.xlsx", dataSourceBean="operationLogService", queryMethod="page", autoTrans=True, pageable=True, maxRows=100000, status=0, includeSample=False, allowImport=True, remark="归档使用", createTime="2026-08-20 09:10:00", updateTime="2026-09-04 14:30:00"),
]
columns = {
    "sys_user_export": [
        dict(id="201", configKey="sys_user_export", fieldName="username", columnName="登录账号", width=18, orderNum=1, export=True, importable=False, required=False, dateFormat=None, numberFormat=None, dictType=None, exampleValue=None, validationRule=None, validationMessage=None),
        dict(id="202", configKey="sys_user_export", fieldName="realName", columnName="用户姓名", width=18, orderNum=2, export=True, importable=False, required=False, dateFormat=None, numberFormat=None, dictType=None, exampleValue=None, validationRule=None, validationMessage=None),
        dict(id="203", configKey="sys_user_export", fieldName="status", columnName="账号状态", width=14, orderNum=3, export=True, importable=False, required=False, dateFormat=None, numberFormat=None, dictType="sys_enable_disable", exampleValue=None, validationRule=None, validationMessage=None),
    ]
}
calls = []


def dict_data(dict_type):
    if dict_type == "sys_excel_config_type":
        return [
            dict(dictLabel="仅导出", dictValue="EXPORT", dictSort=1, listClass="info"),
            dict(dictLabel="仅导入", dictValue="IMPORT", dictSort=2, listClass="warning"),
            dict(dictLabel="导入导出", dictValue="BOTH", dictSort=3, listClass="success"),
        ]
    if dict_type == "sys_enable_disable":
        return [
            dict(dictLabel="启用", dictValue="1", dictSort=1, listClass="success"),
            dict(dictLabel="禁用", dictValue="0", dictSort=2, listClass="default"),
        ]
    if dict_type == "sys_yes_no":
        return [dict(dictLabel="是", dictValue="1", dictSort=1), dict(dictLabel="否", dictValue="0", dictSort=2)]
    return []


def mock_api(route):
    request = route.request
    parsed = urlparse(request.url)
    path = parsed.path.split("/verification-api", 1)[-1]
    query = parse_qs(parsed.query)
    body = request.post_data_json if request.post_data else None
    calls.append((request.method, path, query, body))
    print("API", request.method, path, flush=True)

    if "/system/dict/data/type/" in path:
        route.fulfill(json=dict(code=200, data=dict_data(path.rsplit("/", 1)[-1])))
        return
    if path.endswith("/export-config/page"):
        route.fulfill(json=dict(code=200, data=dict(records=records, total=len(records), current=1, size=10)))
        return
    if path.endswith("/export-config/detail"):
        record_id = query.get("id", ["101"])[0]
        record = next(item for item in records if item["id"] == record_id)
        route.fulfill(json=dict(code=200, data=record))
        return
    if path.endswith("/export-config/status"):
        record_id = query["id"][0]
        next_status = int(query["status"][0])
        next(item for item in records if item["id"] == record_id)["status"] = next_status
        route.fulfill(json=dict(code=200, data=None))
        return
    if path.endswith("/column-config/list"):
        config_key = query.get("configKey", [""])[0]
        route.fulfill(json=dict(code=200, data=columns.get(config_key, [])))
        return
    if path.endswith("/column-config/batch"):
        config_key = query.get("configKey", [""])[0]
        columns[config_key] = body or []
        route.fulfill(json=dict(code=200, data=None))
        return
    if path.endswith("/export-config/copy"):
        route.fulfill(json=dict(code=200, data=dict(id="104", status=0)))
        return
    if "/export-config/test/" in path:
        route.fulfill(status=200, body="mock xlsx", headers={
            "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "Content-Disposition": "attachment;filename*=utf-8''preview.xlsx",
        })
        return
    route.fulfill(json=dict(code=200, data=None))


with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport=dict(width=1440, height=900), device_scale_factor=1)
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.on("console", lambda message: print("CONSOLE", message.text[:1000], flush=True) if message.type == "error" else None)
    page.route("**/verification-api/**", mock_api)
    page.goto("http://127.0.0.1:5188", wait_until="networkidle", timeout=120000)

    expect(page.get_by_text("导出方案", exact=True)).to_be_visible()
    expect(page.get_by_text("用户列表导出", exact=True)).to_be_visible()
    expect(page.get_by_text("数据来源服务", exact=True)).not_to_be_visible()
    page.screenshot(path=str(OUTPUT / "list-light.png"), full_page=True)

    status_switch = page.get_by_role("switch", name="用户列表导出的启用状态")
    expect(status_switch).to_have_attribute("aria-checked", "true")
    status_switch.click()
    expect(page.get_by_text("业务页面将暂时无法使用", exact=False)).to_be_visible()
    page.screenshot(path=str(OUTPUT / "status-confirm.png"), full_page=True)
    page.get_by_role("button", name="取消", exact=True).click()
    assert not any(path.endswith("/export-config/status") for _, path, _, _ in calls)
    status_switch.click()
    page.get_by_role("button", name="确认停用", exact=True).click()
    expect(status_switch).to_have_attribute("aria-checked", "false")

    page.get_by_text("编辑", exact=True).first.click()
    expect(page.get_by_placeholder("让业务人员一眼认出用途，如：用户列表导出")).to_be_visible()
    expect(page.get_by_placeholder("英文、数字或下划线，例如：user_list_export")).to_be_disabled()
    expect(page.get_by_text("高级设置：连接业务数据", exact=True)).to_be_visible()
    expect(page.get_by_placeholder("由技术人员填写，例如：sysUserService")).not_to_be_visible()
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUTPUT / "edit-business.png"), full_page=True)
    page.get_by_text("高级设置：连接业务数据", exact=True).click()
    expect(page.get_by_placeholder("由技术人员填写，例如：sysUserService")).to_be_visible()
    page.wait_for_timeout(250)
    page.screenshot(path=str(OUTPUT / "edit-advanced.png"), full_page=True)
    page.get_by_role("button", name="取消", exact=True).click()

    page.get_by_text("设置文件列", exact=True).first.click()
    expect(page.get_by_text("设置 Excel 中显示的列", exact=True)).to_be_visible()
    expect(page.get_by_text("当前列设置已保存", exact=True)).to_be_visible()
    page.get_by_role("button", name="向下移动登录账号").click()
    expect(page.get_by_text("有未保存的更改", exact=False)).to_be_visible()
    page.screenshot(path=str(OUTPUT / "columns-light.png"), full_page=True)
    page.get_by_role("button", name="保存全部更改", exact=True).click()
    expect(page.get_by_text("当前列设置已保存", exact=True)).to_be_visible()
    page.get_by_role("button", name="添加一列", exact=True).click()
    expect(page.get_by_placeholder("用户在文件中看到的名称，如：用户姓名")).to_be_visible()
    expect(page.get_by_text("格式与校验（高级）", exact=True)).to_be_visible()
    page.screenshot(path=str(OUTPUT / "column-editor.png"), full_page=True)
    page.get_by_role("button", name="取消", exact=True).click()
    page.get_by_role("button", name="关闭", exact=True).click()

    page.get_by_text("更多", exact=True).first.click()
    page.get_by_text("创建副本", exact=True).click()
    expect(page.get_by_text("副本会默认停用", exact=False)).to_be_visible()
    page.screenshot(path=str(OUTPUT / "copy-dialog.png"), full_page=True)
    page.get_by_role("button", name="创建副本", exact=True).click()
    expect(page.get_by_text("副本会默认停用", exact=False)).not_to_be_visible()
    expect(page.get_by_text("副本已创建，默认处于停用状态", exact=True)).to_be_visible()
    expect(page.get_by_text("创建副本失败", exact=False)).not_to_be_visible()

    page.get_by_role("button", name="切换主题", exact=True).click()
    page.wait_for_timeout(200)
    page.screenshot(path=str(OUTPUT / "list-dark.png"), full_page=True)

    page.set_viewport_size(dict(width=390, height=844))
    expect(page.get_by_role("button", name="新建导出方案", exact=True)).to_be_visible()
    page.wait_for_timeout(200)
    page.screenshot(path=str(OUTPUT / "list-mobile.png"), full_page=True)
    assert page.evaluate("document.documentElement.scrollWidth <= innerWidth"), "页面存在横向溢出"
    page.get_by_role("button", name="新建导出方案", exact=True).click()
    expect(page.get_by_placeholder("让业务人员一眼认出用途，如：用户列表导出")).to_be_visible()
    expect(page.get_by_placeholder("由技术人员填写，例如：sysUserService")).not_to_be_visible()
    new_key_input = page.get_by_placeholder("英文、数字或下划线，例如：user_list_export")
    print("NEW_CONFIG_KEY", new_key_input.is_disabled(), new_key_input.get_attribute("disabled"), flush=True)
    expect(new_key_input).to_be_enabled()
    new_key_input.fill("demo_export")
    expect(new_key_input).to_have_value("demo_export")
    new_key_input.fill("")
    page.wait_for_timeout(500)
    drawer = page.locator(".n-drawer").last
    print("MOBILE_DRAWER", drawer.bounding_box(), flush=True)
    assert drawer.bounding_box()["x"] <= 1
    assert drawer.bounding_box()["width"] <= 390
    page.screenshot(path=str(OUTPUT / "add-mobile.png"), full_page=True)

    assert not errors, errors
    print(json.dumps(dict(status="passed", browser_errors=errors, request_count=len(calls)), ensure_ascii=False))
    browser.close()
