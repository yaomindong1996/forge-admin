import os
from pathlib import Path

from playwright.sync_api import sync_playwright


BASE_URL = os.environ.get("FORGE_UI_BASE_URL", "http://localhost:3001")
TARGET_URL = (
    f"{BASE_URL}/app-center/application/presale_registration_apply/runtime"
    "?pageId=page_page&from=designer"
)
ARTIFACT = Path("/tmp/forge-lowcode-flow-runtime.png")


def visible_texts(locator, limit=30):
    output = []
    for item in locator.all()[:limit]:
        try:
            if item.is_visible():
                text = " ".join(item.inner_text().split())
                if text:
                    output.append(text)
        except Exception:
            continue
    return output


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    console_errors = []
    page.on(
        "console",
        lambda message: console_errors.append(message.text)
        if message.type == "error"
        else None,
    )
    page.on("pageerror", lambda error: console_errors.append(str(error)))

    page.goto(TARGET_URL, wait_until="domcontentloaded", timeout=60_000)
    page.wait_for_timeout(1500)
    if "/login" in page.url:
        username = os.environ.get("FORGE_UI_USERNAME", "")
        password = os.environ.get("FORGE_UI_PASSWORD", "")
        if not username or not password:
            raise RuntimeError("FORGE_UI_USERNAME/FORGE_UI_PASSWORD are required")
        page.locator("#username input").fill(username)
        page.locator("#password input").fill(password)
        page.get_by_role("button", name="登录").click()
        page.wait_for_timeout(3500)
        if "/login" in page.url:
            page.screenshot(path=str(ARTIFACT), full_page=True)
            print("blocked=login did not complete; backend/auth or captcha prevented route access")
            print(f"body={page.locator('body').inner_text()[:1600].replace(chr(10), ' | ')}")
            print(f"console_errors={console_errors[:20]}")
            print(f"screenshot={ARTIFACT}")
            browser.close()
            raise SystemExit(0)
        page.goto(TARGET_URL, wait_until="domcontentloaded", timeout=60_000)
        page.wait_for_timeout(3000)

    page.screenshot(path=str(ARTIFACT), full_page=True)
    print(f"url={page.url}")
    print(f"buttons={visible_texts(page.get_by_role('button'), 50)}")
    print(f"links={visible_texts(page.get_by_role('link'), 50)}")
    print(f"body={page.locator('body').inner_text()[:1600].replace(chr(10), ' | ')}")
    print(f"console_errors={console_errors[:20]}")
    print(f"screenshot={ARTIFACT}")
    browser.close()
