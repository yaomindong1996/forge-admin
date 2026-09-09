import json
import os
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from playwright.sync_api import sync_playwright


BASE_URL = os.environ.get("FORGE_VISUAL_SMOKE_BASE_URL", "http://127.0.0.1:3000")
APPLICATION_CODE = "IA_VISUAL_SMOKE"
OBJECT_ID = "1910000000000000001"
ARTIFACTS_DIR = Path(__file__).with_name("artifacts")

private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
public_key_pem = private_key.public_key().public_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PublicFormat.SubjectPublicKeyInfo,
).decode("ascii")

application_options = {
    "inAppBuilder": {
        "schemaVersion": 2,
        "homePageId": "page_home",
        "nodes": [{
            "id": "page_home",
            "type": "page",
            "pageType": "home",
            "title": "工作台首页",
            "parentId": None,
            "sort": 0,
        }],
        "pages": {
            "page_home": {
                "title": "工作台首页",
                "layout": {
                    "gridLayout": {"cols": 12, "rowHeight": 32, "gap": 8, "items": []},
                },
            },
        },
        "formAssets": [],
        "flowInteraction": {
            "approvalActions": [{
                "actionId": "approve",
                "operation": "approve",
                "label": "同意",
                "enabled": True,
            }],
            "timeline": {"enabled": True, "title": "审批记录"},
            "nodePermissions": [{
                "nodeKey": "financeReview",
                "visibleSectionIds": [],
                "readonlySectionIds": ["payment"],
            }],
            "callbacks": {"approvedActionCode": "complete_order"},
        },
    },
}

application = {
    "id": "101",
    "applicationCode": APPLICATION_CODE,
    "applicationName": "预售业务应用",
    "suiteCode": "PRESALE",
    "status": 1,
    "options": json.dumps(application_options, ensure_ascii=False),
}
business_object = {
    "objectId": OBJECT_ID,
    "objectCode": "PRESALE_ORDER",
    "objectName": "预售登记",
    "objectRole": "PRIMARY",
}
workspace_entries = [{
    "id": "entry-1",
    "applicationId": application["id"],
    "appCode": "PRESALE_MOBILE",
    "appName": "预售移动端",
    "appType": "MOBILE",
    "mountTarget": "MOBILE",
    "entryMode": "RUNTIME",
    "objectCode": business_object["objectCode"],
    "runtimeOpenMode": "LIST",
    "targetPageKey": "list",
    "status": 1,
}]
workspace_extensions = [{
    "id": "extension-1",
    "applicationId": application["id"],
    "extensionCode": "PRESALE_PAGE_JS",
    "extensionName": "预售页面校验增强",
    "extensionType": "PAGE_JS",
    "hookCode": "BEFORE_SUBMIT",
    "scopeType": "OBJECT",
    "scopeRefId": OBJECT_ID,
    "objectName": business_object["objectName"],
    "status": "ENABLED",
    "draftVersion": 3,
    "enabledVersion": 2,
    "failurePolicy": "FAIL_CLOSED",
    "riskLevel": "MEDIUM",
}]
page_sections = [
    {"sectionId": "base", "title": "基础信息"},
    {"sectionId": "member", "title": "会员信息"},
    {"sectionId": "payment", "title": "付款信息"},
]
designer = {
    **business_object,
    "fields": [{
        "fieldCode": "customerName",
        "fieldName": "客户姓名",
        "formVisible": True,
        "listVisible": True,
    }],
    "modelSchema": {"fields": [{"field": "customerName", "label": "客户姓名"}]},
    "pageSchema": {"zones": [
        {"zoneKey": "edit", "fieldRefs": ["customerName"]},
        {"zoneKey": "table", "fieldRefs": ["customerName"]},
    ]},
    "formDesignerSchema": {
        "formKey": "main",
        "formName": "预售登记",
        "components": [{
            "id": "customerName",
            "field": "customerName",
            "label": "客户姓名",
            "componentKey": "input",
        }],
        "pageSections": page_sections,
        "settings": {
            "governance": {
                "fieldEvents": [{"eventType": "change", "fieldCode": "customerName"}],
            },
        },
    },
    "viewSchema": {
        "list": {
            "enabled": True,
            "columns": [{"fieldCode": "customerName", "label": "客户姓名"}],
        },
    },
    "designerOptions": {
        "actions": [{"actionCode": "complete_order", "actionName": "完成订单"}],
    },
    "documentConfig": {
        "documentEnabled": True,
        "statusField": "status",
        "mainFlowSummary": {
            "configured": True,
            "flowModelKey": "presaleApproval",
        },
    },
}
flow_config = {
    "flowBinding": {
        "flowModelKey": "presaleApproval",
        "flowModelName": "预售审批",
        "titleTemplate": "${customerName}-预售审批",
        "startMode": "MANUAL",
        "variableMapping": [{
            "formField": "customerName",
            "flowVariable": "customerName",
            "label": "客户姓名",
        }],
        "businessBinding": {
            "mode": "LOWCODE_OBJECT",
            "primaryKeyField": "id",
            "tenantField": "tenant_id",
            "statusField": "status",
        },
        "nodeForms": [],
        "conditionFlows": [],
        "options": {},
    },
    "documentConfig": designer["documentConfig"],
    "formAssets": {"formAssets": [], "warnings": []},
    "summary": {"statusField": "status"},
}


def fulfill_json(route, data):
    route.fulfill(
        status=200,
        content_type="application/json",
        body=json.dumps({"code": 200, "msg": "success", "data": data}, ensure_ascii=False),
    )


def mock_api(route):
    path = urlparse(route.request.url).path.removeprefix("/dev-api")
    if path == "/crypto/public-key":
        return fulfill_json(route, {"publicKey": public_key_pem, "algorithm": "RSA"})
    if path == "/crypto/exchange":
        return fulfill_json(route, True)
    if path == "/auth/userInfo":
        return fulfill_json(route, {
            "userId": "1",
            "username": "admin",
            "realName": "管理员",
            "tenantId": "1",
            "admin": True,
            "permissions": ["*:*:*"],
            "apiPermissions": ["*:*:*"],
        })
    if path == "/auth/current/menu":
        return fulfill_json(route, [{
            "id": "designer-runtime",
            "resourceName": "应用设计器",
            "resourceType": 2,
            "path": "/app-center/application/:applicationCode/runtime",
            "component": "app-center/application-runtime.[applicationCode]",
            "visible": 1,
            "menuStatus": 1,
        }])
    if path == "/system/tenant/userTenantConfig":
        return fulfill_json(route, {"systemName": "Forge Admin", "systemLayout": "empty"})
    if path == "/system/dict/data/list":
        return fulfill_json(route, [])
    if path == f"/ai/business/application/by-code/{APPLICATION_CODE}/workspace":
        return fulfill_json(route, {
            "application": application,
            "objects": [business_object],
            "extensions": workspace_extensions,
            "entries": workspace_entries,
        })
    if path == f"/ai/business/object/{OBJECT_ID}/designer":
        return fulfill_json(route, designer)
    if path == "/ai/business/flow-app/config/PRESALE_ORDER":
        return fulfill_json(route, flow_config)
    if path == "/ai/business/flow/form-assets/PRESALE_ORDER":
        return fulfill_json(route, {"formAssets": [], "warnings": []})
    if path == "/ai/business/flow/model/presaleApproval/variables":
        return fulfill_json(route, {
            "flowVariables": [{"variableName": "customerName", "displayName": "客户姓名"}],
            "fieldCandidates": [],
            "userTasks": [
                {
                    "taskDefKey": "managerApproval",
                    "taskName": "经理审批",
                    "candidateGroups": ["manager"],
                },
                {
                    "taskDefKey": "financeReview",
                    "taskName": "财务复核",
                    "candidateGroups": ["finance"],
                },
            ],
        })
    if "/api/flow" in path and "/model" in path:
        return fulfill_json(route, [{
            "id": "model-1",
            "modelKey": "presaleApproval",
            "modelName": "预售审批",
            "status": 1,
        }])
    if path == f"/ai/business/object/{OBJECT_ID}/actions":
        return fulfill_json(route, designer["designerOptions"]["actions"])
    if path == "/ai/business/trigger/page":
        return fulfill_json(route, {
            "records": [{"id": "trigger-1", "objectCode": business_object["objectCode"]}],
            "total": 1,
        })
    if path == "/ai/business/extension/page":
        return fulfill_json(route, {"records": workspace_extensions, "total": 1})
    return fulfill_json(route, {})


ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    console_errors = []
    page.on(
        "console",
        lambda message: console_errors.append(
            f"{message.text} [{message.location.get('url', '')}]"
        ) if message.type == "error" else None,
    )
    page.on("pageerror", lambda error: console_errors.append(str(error)))
    page.route("**/dev-api/**", mock_api)
    page.add_init_script("localStorage.setItem('prod_auth', JSON.stringify({ accessToken: 'visual-smoke-token' }))")

    base_designer_url = f"{BASE_URL}/app-center/application/{APPLICATION_CODE}/runtime?edit=1"
    page.goto(f"{base_designer_url}&designSection=events", wait_until="networkidle")
    page.get_by_role("button", name="预售登记（表单页）").wait_for()
    mapping_background = page.locator(".table-mapping").evaluate(
        "element => getComputedStyle(element).backgroundColor"
    )
    if mapping_background != "rgb(255, 255, 255)":
        raise RuntimeError(f"数据库映射摘要背景非中性白色：{mapping_background}")
    if "designSection=events" not in page.url:
        raise RuntimeError("旧 events 深链未保留")
    for canvas_view in ["表单布局", "页面分区", "详情设置"]:
        if not page.get_by_role("radio", name=canvas_view).is_visible():
            raise RuntimeError(f"{canvas_view} 视图未显示")
    page.locator(".property-tab-configured-dot").wait_for()

    if page.locator(".application-design-section .designer-nav").count():
        raise RuntimeError("内嵌表单页仍显示重复对象导航")
    if page.locator(".application-design-section .closure-steps").count():
        raise RuntimeError("内嵌表单页仍显示单据闭环配置")
    embedded_workbench_columns = page.locator(
        ".application-design-section .designer-workbench"
    ).evaluate("element => getComputedStyle(element).gridTemplateColumns")
    if len(embedded_workbench_columns.split()) != 1:
        raise RuntimeError(f"内嵌对象工作台不是单列：{embedded_workbench_columns}")

    expected_group_counts = {
        "页面": "3/3",
        "数据": "1/1",
        "自动化": "3/3",
        "流程": "1/1",
        "设置": "1/1",
    }
    actual_group_counts = {}
    for group_label, expected_count in expected_group_counts.items():
        group = page.locator(".resource-group", has=page.locator(
            ".resource-group-toggle", has_text=group_label
        ))
        actual_count = group.locator(".resource-group-toggle small").inner_text()
        actual_group_counts[group_label] = actual_count
        if actual_count != expected_count:
            raise RuntimeError(
                f"{group_label}分组配置计数错误：{actual_count} != {expected_count}"
            )

    page.locator(".page-view-controls .n-radio-button", has_text="页面分区").click()
    section_title_input = page.locator(
        ".inline-page-section-editor .settings-grid .n-form-item"
    ).nth(0).locator("input")
    section_title_input.wait_for()
    section_title_input.fill("基础信息（烟测）")
    page.locator(".page-view-controls .n-radio-button", has_text="表单布局").click()
    page.locator(".page-view-controls .n-radio-button", has_text="页面分区").click()
    if section_title_input.input_value() != "基础信息（烟测）":
        raise RuntimeError("画布视图切换后分区草稿丢失")
    section_title_input.fill("基础信息")
    page.locator(".page-view-controls .n-radio-button", has_text="表单布局").click()

    for width in [1024, 1280, 1440]:
        page.set_viewport_size({"width": width, "height": 1000})
        layout = page.evaluate("""() => ({
          scrollWidth: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
          tree: document.querySelector('.application-resource-tree')?.getBoundingClientRect().toJSON(),
          content: document.querySelector('.application-design-section')?.getBoundingClientRect().toJSON(),
        })""")
        if layout["scrollWidth"] > layout["clientWidth"]:
            raise RuntimeError(
                f"{width}px 存在横向溢出：{layout['scrollWidth']} > {layout['clientWidth']}"
            )
        if not layout["tree"] or not layout["content"]:
            raise RuntimeError(f"{width}px 缺少资源树或内容区")
        if round(layout["tree"]["right"]) != round(layout["content"]["left"]):
            raise RuntimeError(f"{width}px 资源树与内容区未对齐")
        page.screenshot(path=ARTIFACTS_DIR / f"resource-tree-{width}.png")

    page.goto(
        f"{base_designer_url}&designSection=automation-enhancements",
        wait_until="networkidle",
    )
    page.locator(".resource-node.active", has_text="动作增强").wait_for()
    extension_row = page.locator(
        ".extension-row:not(.extension-row-head)",
        has_text=workspace_extensions[0]["extensionName"],
    )
    extension_row.wait_for()
    if page.locator(".extensions-panel > .panel-heading").count():
        raise RuntimeError("内嵌动作增强仍显示重复页面标题")
    page.locator(".extensions-panel > .embedded-actionbar").wait_for()
    page.screenshot(path=ARTIFACTS_DIR / "extensions-1440.png")

    page.goto(
        f"{base_designer_url}&designResource=page-custom:page_home",
        wait_until="networkidle",
    )
    page.locator(".resource-node.active", has_text="工作台首页（自由编排）").wait_for()
    if page.locator(".runtime-navigation").count():
        raise RuntimeError("自由编排编辑态仍显示第二套运行时导航")
    runtime_columns = page.locator(".runtime-body.designer-resource-active").evaluate(
        "element => getComputedStyle(element).gridTemplateColumns"
    )
    if len(runtime_columns.split()) != 1:
        raise RuntimeError(f"自由编排工作台隐藏导航后仍保留空白列：{runtime_columns}")

    page.get_by_role("button", name="新建页面").click()
    page.wait_for_function(
        "() => new URLSearchParams(location.search).get('designResource')?.startsWith('page-custom:')"
        " && new URLSearchParams(location.search).get('designResource') !== 'page-custom:page_home'"
    )
    created_resource_key = parse_qs(urlparse(page.url).query).get("designResource", [""])[0]
    if not created_resource_key.startswith("page-custom:"):
        raise RuntimeError(f"新建页面未写入 designResource：{page.url}")
    created_resource = page.locator(".resource-node.active")
    created_resource.wait_for()
    if "自由编排" not in created_resource.inner_text():
        raise RuntimeError("新建页面未在资源树中立即显示并选中")
    page.screenshot(path=ARTIFACTS_DIR / "free-page-created-1440.png")

    page.goto(f"{base_designer_url}&designResource=flow:{OBJECT_ID}", wait_until="networkidle")
    page.locator(".flow-card h4", has_text="选择流程模型").wait_for()
    interaction_panel = page.locator(".flow-interaction-panel")
    interaction_panel.get_by_text("经理审批", exact=True).wait_for()
    interaction_panel.get_by_text("财务复核", exact=True).wait_for()
    interaction_panel.get_by_text("审批操作", exact=True).wait_for()
    page.wait_for_function(
        "() => !document.querySelector('.flow-interaction-panel .n-spin-body')"
    )

    finance_row = interaction_panel.locator(".node-table-row", has_text="财务复核")
    visible_section_select = finance_row.locator(
        ".node-section-grid .n-form-item"
    ).nth(0).locator(".n-base-selection")
    visible_section_select.click()
    page.locator(".n-base-select-option", has_text="基础信息").click()
    page.locator(".n-base-select-option", has_text="会员信息").click()
    page.keyboard.press("Escape")
    interaction_panel.locator(".panel-heading").click()
    page.wait_for_timeout(300)
    finance_row.get_by_text("2/3 选中", exact=True).wait_for()
    page.screenshot(path=ARTIFACTS_DIR / "flow-designer-1440.png")

    flow_layout = page.evaluate("""() => ({
      columns: getComputedStyle(document.querySelector('.application-flow-workbench')).gridTemplateColumns,
      bindingBottom: document.querySelector('.application-flow-workbench')?.firstElementChild?.getBoundingClientRect().bottom,
      interactionTop: document.querySelector('.flow-interaction-panel')?.getBoundingClientRect().top,
    })""")
    if len(flow_layout["columns"].split()) != 1:
        raise RuntimeError(f"流程工作台不是单列：{flow_layout['columns']}")
    if flow_layout["interactionTop"] < flow_layout["bindingBottom"]:
        raise RuntimeError("流程节点配置与绑定区发生重叠")

    flow_config["flowBinding"]["flowModelKey"] = ""
    flow_config["flowBinding"]["flowModelName"] = ""
    page.reload(wait_until="networkidle")
    page.locator(".flow-interaction-panel").get_by_text(
        "先在上方绑定流程模型，节点会自动显示在这里",
        exact=True,
    ).wait_for()

    relevant_errors = [
        message for message in console_errors
        if all(token not in message for token in ["WebSocket", "STOMP 错误", "/ws/info", "favicon", "ResizeObserver"])
    ]
    if relevant_errors:
        raise RuntimeError("浏览器控制台错误：\n" + "\n".join(relevant_errors))

    print(json.dumps({
        "viewports": [1024, 1280, 1440],
        "legacyEventsDeepLink": "passed",
        "databaseSummaryBackground": mapping_background,
        "formCanvasViews": ["layout", "sections", "detail"],
        "formCanvasDraftPersistence": "passed",
        "configuredEventIndicator": "passed",
        "embeddedObjectWorkbenchColumns": embedded_workbench_columns,
        "duplicateEmbeddedNavigation": "absent",
        "duplicateClosureSteps": "absent",
        "resourceGroupCounts": actual_group_counts,
        "embeddedExtensions": workspace_extensions[0]["extensionCode"],
        "duplicateExtensionsHeading": "absent",
        "freePageRuntimeNavigation": "absent",
        "createdPageResource": created_resource_key,
        "flowVerticalLayout": "passed",
        "parsedFlowNode": "financeReview",
        "flowPolicyConfiguredWithoutIdentifiers": "passed",
        "unboundFlowGuide": "passed",
        "screenshots": [
            "resource-tree-1024.png",
            "resource-tree-1280.png",
            "resource-tree-1440.png",
            "extensions-1440.png",
            "free-page-created-1440.png",
            "flow-designer-1440.png",
        ],
    }, ensure_ascii=False, indent=2))
    browser.close()
